package com.smartequip.demo;

public class Challenge {
	private final String key;
	private final String challenge;

	public Challenge(String key, String challenge) {
		this.key = key;
		this.challenge = challenge;
	}

	public String getKey() {
		return key;
	}

	public String getChallenge() {
		return challenge;
	}
}
