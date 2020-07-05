package com.stefanosdemetriou.solactive.web;

import static org.hamcrest.CoreMatchers.equalTo;

import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import com.stefanosdemetriou.solactive.web.dto.Stats;
import com.stefanosdemetriou.solactive.web.dto.Tick;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class TicksControllerTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void testGetGlobalStats() {
		ResponseEntity<Stats> response = restTemplate.getForEntity("/statistics", Stats.class);
		assertThat(response.getStatusCodeValue(), equalTo(200));
	}

	@Test
	void testGetUnknownInstrumentStats() {
		ResponseEntity<Stats> response = restTemplate.getForEntity("/statistics/NOSUCHINSTRUMENT", Stats.class);
		assertThat(response.getStatusCodeValue(), equalTo(404));
	}

	@Test
	void testPostTick() {
		Tick tick = new Tick();
		tick.setInstrument("IBM.N");
		tick.setPrice(123.2);
		tick.setTimestamp(System.currentTimeMillis());

		ResponseEntity<Void> response = restTemplate.postForEntity("/ticks", tick, Void.class);
		assertThat(response.getStatusCodeValue(), equalTo(201));
	}

	@Test
	void testPostExpiredTick() {
		Tick tick = new Tick();
		tick.setInstrument("IBM.N");
		tick.setPrice(123.2);
		tick.setTimestamp(System.currentTimeMillis() - 60001);

		ResponseEntity<Void> response = restTemplate.postForEntity("/ticks", tick, Void.class);
		assertThat(response.getStatusCodeValue(), equalTo(204));
	}

	@Test
	void testPostTickAndGetInstrument() {
		Tick tick = new Tick();
		tick.setInstrument("IBM.N");
		tick.setPrice(123.2);
		tick.setTimestamp(System.currentTimeMillis());

		restTemplate.postForEntity("/ticks", tick, Void.class);
		ResponseEntity<Stats> response = restTemplate.getForEntity("/statistics/IBM.N", Stats.class);

		assertThat(response.getStatusCodeValue(), equalTo(200));
	}
}
