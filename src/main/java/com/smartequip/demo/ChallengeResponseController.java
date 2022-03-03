package com.smartequip.demo;

import java.io.ByteArrayOutputStream;
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
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
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
            int hash = s.hashCode();
            final Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv;
            {
                int n = c.getBlockSize();
                iv = new byte[n];
                rnd.nextBytes(iv);
            }
            c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(c.getIV());
            baos.write(c.doFinal((s.hashCode() + "@" + s).getBytes("UTF-8")));
            return Base64.getUrlEncoder().encodeToString(baos.toByteArray());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /*
     * Decrpyt a string using AES.  Not sure if Key is threadsafe, so synchronized.
     */
    private synchronized String decrypt(String s) {
        try {
            byte[] data = Base64.getUrlDecoder().decode(s);
            final Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            int n = c.getBlockSize();
            c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(data, 0, n));
            String decoded = new String(c.doFinal(data, n, data.length - n), "UTF-8");
            String[] fields = decoded.split("@");
            String hashString = fields[0];
            String result = fields[1];
            return hashString.equals(Integer.toString(result.hashCode())) ? result : null;
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
     * be generated.  The encoded key is generated using a random initial value (IV) and
     * a timestamp and marked with a hash in order to counter replay/dictionary/forgery attacks.
     * This means that even the exact same challenge will encode to a different key almost every time,
     * and any key you have seen before expires in 5 minutes - i.e. you can't just program your bot
     * to replay the same solved challenge indefinitely, and it should be very hard to fake
     * a key by looking for patterns in prior challenges or modifying a prior key.
     */
    @RequestMapping(value="/", method=RequestMethod.POST, produces="application/json")
    public Challenge requestChallenge() {
        int a = generateNumber();
        int b = generateNumber();
        int c = generateNumber();
        long timestamp = System.currentTimeMillis();
        String challenge = "Please sum the numbers " + a + ", " + b + ", " + c;
        String response = Integer.toString(a + b + c);
        return new Challenge(encrypt(challenge + "|" + response + "|" + timestamp), challenge);
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
            if (fields.length == 3) {
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
