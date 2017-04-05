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

@RestController
public class ChallengeResponseController {
	/*
	 * Conigure a maximum number of pending responses to help defend against a
	 * malicious client running us out of memory.
	 */
	private static final int MAX_PENDING = 1000;

	/*
	 * A generator of random numbers for our challenges.  SecureRandom is threadsafe.
	 */
	private Random rnd = new SecureRandom();

	/*
	 * Keep a map of pending challenges and expected responses.  Challenges
	 * will be inactivated after MAX_PENDING is reached.
	 */
	private Map<String, String> challenges = new LinkedHashMap<String, String>() {
		public boolean removeEldestEntry(Map.Entry<String, String> e) {
			return size() > MAX_PENDING;
		}
	};

	/*
	 * Randomly generate a number for the challenge.
	 */
	private int generateNumber() {
		return rnd.nextInt(10) + 1;
	}

	/**
	 * Request a challenge from the service.
	 */
    @RequestMapping(value="/", method=RequestMethod.POST, produces="application/json")
    public String requestChallenge() {
    	int a = generateNumber();
    	int b = generateNumber();
    	int c = generateNumber();
    	String challenge = "Please sum the numbers " + a + ", " + b + ", " + c;
    	synchronized(challenges) {
    		challenges.put(challenge, Integer.toString(a + b + c));
    	}
    	return JSONObject.quote(challenge);
    }

    /**
     * Give a response to a particular challenge from the service.
     */
    @RequestMapping(value="/", method=RequestMethod.GET, produces="application/json")
    public ResponseEntity<String> validateResponse(@RequestParam("challenge") String challenge, @RequestParam("response") String response) {
    	String expectedResponse;
    	synchronized(challenges) {
    		expectedResponse = challenges.get(challenge);
    	}
    	if (expectedResponse == null || !expectedResponse.equals(response)) {
    		return new ResponseEntity<>(JSONObject.quote("No"), HttpStatus.BAD_REQUEST);
    	}
    	/*
    	 * On success, remove the pending challenge.
    	 */
    	synchronized(challenges) {
    		challenges.remove(challenge);
    	}
    	return new ResponseEntity<>(JSONObject.quote("Yes"), HttpStatus.OK);
    }
}
