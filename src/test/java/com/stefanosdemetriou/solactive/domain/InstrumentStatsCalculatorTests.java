package com.stefanosdemetriou.solactive.domain;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.stefanosdemetriou.solactive.web.dto.Stats;

class InstrumentStatsCalculatorTests {

	@Test
	void cachedStatsOnInitAreEmpty() {
		InstrumentStatsCalculator calc = new InstrumentStatsCalculator();
		Stats stats = calc.getCachedStats();

		assertThat(stats, is(not(nullValue())));
		assertThat(stats.getCount(), equalTo(0L));
		assertThat(stats.getAvg(), equalTo(0.0));
		assertThat(stats.getMax(), equalTo(0.0));
		assertThat(stats.getMin(), equalTo(0.0));
	}

	@Test
	void cachedStatsAreEmptyBeforeCalculate() {
		InstrumentStatsCalculator calc = new InstrumentStatsCalculator();
		calc.addTick(new InstrumentTick(System.currentTimeMillis(), 12.3));

		Stats stats = calc.getCachedStats();

		assertThat(stats, is(not(nullValue())));
		assertThat(stats.getCount(), equalTo(0L));
		assertThat(stats.getAvg(), equalTo(0.0));
		assertThat(stats.getMax(), equalTo(0.0));
		assertThat(stats.getMin(), equalTo(0.0));
	}

	@Test
	void calculationOfSingleElementIsCorrect() {
		InstrumentStatsCalculator calc = new InstrumentStatsCalculator();
		calc.addTick(new InstrumentTick(System.currentTimeMillis(), 12.3));

		Stats stats = calc.cleanListAndCalculate();

		assertThat(stats, is(not(nullValue())));
		assertThat(stats.getCount(), equalTo(1L));
		assertThat(stats.getAvg(), equalTo(12.3));
		assertThat(stats.getMax(), equalTo(12.3));
		assertThat(stats.getMin(), equalTo(12.3));
	}

	@Test
	void cachedStatsAreCorrectAfterCalculation() {
		InstrumentStatsCalculator calc = new InstrumentStatsCalculator();
		calc.addTick(new InstrumentTick(System.currentTimeMillis(), 12.3));

		calc.cleanListAndCalculate();
		Stats stats = calc.getCachedStats();

		assertThat(stats, is(not(nullValue())));
		assertThat(stats.getCount(), equalTo(1L));
		assertThat(stats.getAvg(), equalTo(12.3));
		assertThat(stats.getMax(), equalTo(12.3));
		assertThat(stats.getMin(), equalTo(12.3));
	}

	@Test
	void calculationOfMultipleTicksAreCorrect() {
		InstrumentStatsCalculator calc = new InstrumentStatsCalculator();
		calc.addTick(new InstrumentTick(System.currentTimeMillis(), 1.0));
		calc.addTick(new InstrumentTick(System.currentTimeMillis(), 2.0));
		calc.addTick(new InstrumentTick(System.currentTimeMillis(), 6.0));

		Stats stats = calc.cleanListAndCalculate();

		assertThat(stats, is(not(nullValue())));
		assertThat(stats.getCount(), equalTo(3L));
		assertThat(stats.getAvg(), equalTo(3.0));
		assertThat(stats.getMax(), equalTo(6.0));
		assertThat(stats.getMin(), equalTo(1.0));
	}

	@Test
	void parallelTicksAreSynchronized() throws InterruptedException {
		InstrumentStatsCalculator calc = new InstrumentStatsCalculator();
		ExecutorService executor = Executors.newFixedThreadPool(100);

		for (int i = 0; i < 100; i++) {
			executor.submit(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < 100000; i++) {
						calc.addTick(new InstrumentTick(System.currentTimeMillis(), i));
					}
				}
			});
		}

		double expectedAvg = 0;
		for (int i = 0; i < 100000; i++) {
			expectedAvg += i;
		}
		expectedAvg /= 100000;

		executor.shutdown();
		boolean timeout = !executor.awaitTermination(59, TimeUnit.SECONDS);
		assertThat(timeout, is(false));

		Stats stats = calc.cleanListAndCalculate();
		assertThat(stats.getCount(), equalTo(100000L * 100));
		assertThat(stats.getAvg(), equalTo(expectedAvg));
		assertThat(stats.getMin(), equalTo(0.0));
		assertThat(stats.getMax(), equalTo(99999.0));

	}

	@Test
	void willNotAddTickOlderThanOneMinute() {
		InstrumentStatsCalculator calc = new InstrumentStatsCalculator();
		calc.addTick(new InstrumentTick(System.currentTimeMillis() - 600001, 12.3));

		Stats stats = calc.getCachedStats();

		assertThat(stats, is(not(nullValue())));
		assertThat(stats.getCount(), equalTo(0L));
		assertThat(stats.getAvg(), equalTo(0.0));
		assertThat(stats.getMax(), equalTo(0.0));
		assertThat(stats.getMin(), equalTo(0.0));
	}

	@Test
	void willCleanOldTicks() throws InterruptedException {
		InstrumentStatsCalculator calc = new InstrumentStatsCalculator();
		calc.addTick(new InstrumentTick(System.currentTimeMillis(), 1.0));

		Thread.sleep(60 * 1000);
		calc.addTick(new InstrumentTick(System.currentTimeMillis(), 2.0));
		calc.addTick(new InstrumentTick(System.currentTimeMillis(), 6.0));

		Stats stats = calc.cleanListAndCalculate();
		assertThat(stats, is(not(nullValue())));
		assertThat(stats.getCount(), equalTo(2L));
		assertThat(stats.getAvg(), equalTo(4.0));
		assertThat(stats.getMax(), equalTo(6.0));
		assertThat(stats.getMin(), equalTo(2.0));
	}
}
