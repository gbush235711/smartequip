package com.smartequip.demo;

import java.util.Random;
import java.security.SecureRandom;
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.json.JSONObject;

import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


@RestController
public class ChallengeResponseController {
    /*
     * A generator of random numbers for our challenges.  SecureRandom is threadsafe.
     */
    private final Random rnd = new SecureRandom();

    /*
     * A timeout value after which the challenge expires.  (5 minutes)
     */
    private final int TIMEOUT = 5 * 60 * 1000;

    private final Key key = generateRandomKey();

    private Key generateRandomKey() {
        // 128-bit AES key
        byte[] k = new byte[16];
        for (int i = 0; i < k.length; i++) {
            k[i] = (byte) rnd.nextInt();
        }
        return new SecretKeySpec(k, "AES");
    }

    /*
     * Encrpyt a string using AES.  Not sure if Key is threadsafe, so synchronized.
     */
    private synchronized String encrypt(String s) {
        try {
            final Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, key);
            final byte[] encValue = c.doFinal(s.getBytes("UTF-8"));
            return Base64.getUrlEncoder().encodeToString(encValue);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /*
     * Decrpyt a string using AES.  Not sure if Key is threadsafe, so synchronized.
     */
    private synchronized String decrypt(String s) {
        try {
            final Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, key);
            return new String(c.doFinal(Base64.getUrlDecoder().decode(s)), "UTF-8");
        } catch (Exception ex) {
            return null;
        }
    }

    /*
     * Randomly generate a number for the challenge.
     */
    private int generateNumber() {
        return rnd.nextInt(10) + 1;
    }

    /**
     * Request a challenge from the service.  An encoded key and challenge question will
     * be generated.  The encoded key is salted to prevent creating a dictionary attack for
     * challenge questions.
     */
    @RequestMapping(value="/", method=RequestMethod.POST, produces="application/json")
    public Challenge requestChallenge() {
        int a = generateNumber();
        int b = generateNumber();
        int c = generateNumber();
        long timestamp = System.currentTimeMillis();
        int salt = rnd.nextInt();
        String challenge = "Please sum the numbers " + a + ", " + b + ", " + c;
        String response = Integer.toString(a + b + c);
        return new Challenge(encrypt(challenge + "|" + response + "|" + timestamp + "|" + salt), challenge);
    }

    /**
     * Give a response to a particular challenge from the service.  The client must send
     * the same key and challenge that he previously received.
     *
     * The encrypted key will be decoded using our private key, and the client's response
     * will be compared to its contents.
     */
    @RequestMapping(value="/", method=RequestMethod.GET, produces="application/json")
    public ResponseEntity<String> validateResponse(@RequestParam("key") String k, @RequestParam("challenge") String challenge,
        @RequestParam("response") String response) {
        String dk = decrypt(k);
        if (dk != null) {
            String[] fields = dk.split("\\|");
            if (fields.length == 4) {
                String expectedChallenge = fields[0];
                String expectedResponse = fields[1];
                Long timestamp = Long.parseLong(fields[2]);
                if (expectedChallenge.equals(challenge) && expectedResponse.equals(response) &&
                    (System.currentTimeMillis() - timestamp < TIMEOUT)) {
                    return new ResponseEntity<>(JSONObject.quote("Yes"), HttpStatus.OK);
                }
            }
        }
        return new ResponseEntity<>(JSONObject.quote("No"), HttpStatus.BAD_REQUEST);
    }
}
