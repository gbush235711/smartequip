package com.smartequip.demo;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class ChallengeResponseControllerTest {
	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Before
	public void setup() {
		this.mockMvc = webAppContextSetup(webApplicationContext).build();
	}

	/*
	 * Reqeust a challenge from the service.
	 */
	private String[] requestChallenge() throws Exception {
		String result = mockMvc.perform(post("/")).andExpect(status().isOk()).andExpect(content().
			contentType("application/json;charset=UTF-8")).andReturn().getResponse().getContentAsString();
		Pattern p = Pattern.compile("\"Please sum the numbers (\\d+), (\\d+), (\\d+)\"");
		Matcher m = p.matcher(result);
		Assert.assertTrue(m.matches());
		return new String[] {m.group(0), m.group(1), m.group(2), m.group(3)};
	}

	/*
	 * Give a response to a particular challenge and check that the status matches what we expect.
	 */
	private void giveResponse(String[] result, int expectedStatus) throws Exception {
		String challenge = result[0];
		challenge = challenge.substring(1, challenge.length() - 1);
		int sum = Integer.parseInt(result[1]) + Integer.parseInt(result[2]) + Integer.parseInt(result[3]);

		mockMvc.perform(get("/").param("challenge", challenge).param("response", Integer.toString(sum))).
		    andExpect(status().is(expectedStatus));
	}

	/**
	 * Request a challenge, give a successful response, then re-submit the response and confirm failure
	 * to verify it was removed from the pending responses.
	 */
	@Test
	public void successfulChallenge() throws Exception {
		String[] result = requestChallenge();
		giveResponse(result, 200);
		giveResponse(result, 400);
	}

	/**
	 * Request a challenge, respond with a correct challenge but the wrong response and confirm
	 * failure.
	 */
	@Test
	public void failChallengeOnSum() throws Exception {
		String[] result = requestChallenge();
		result[1] = "99";
		giveResponse(result, 400);
	}

	/**
	 * Respond with a challenge that hasn't been offered yet and confirm failure.
	 */
	@Test
	public void failChallengeOnUnrequested() throws Exception {
		giveResponse(new String[] {"What is your quest?", "1", "2", "3"}, 400);
	}
}
