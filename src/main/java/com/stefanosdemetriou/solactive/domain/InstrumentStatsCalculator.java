package com.stefanosdemetriou.solactive.domain;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.stefanosdemetriou.solactive.web.dto.Stats;

public class InstrumentStatsCalculator {

	public static final int STATS_MILIS_TTL = 60 * 1000;

	private Stats calculatedStats;

	private final List<InstrumentTick> ticks = new LinkedList<>();

	public synchronized void addTick(InstrumentTick tick) {
		long minTime = System.currentTimeMillis() - STATS_MILIS_TTL;
		if (tick.getTimestamp() > minTime) {
			ticks.add(tick);
		}
	}

	public synchronized Stats getCachedStats() {
		return calculatedStats != null ? calculatedStats : new Stats();
	}

	public synchronized Stats cleanListAndCalculate() {
		// remove elements older than STATS_MILIS_TIME and calculate stats for the rest
		calculatedStats = new Stats();
		double total = 0;
		long minTime = System.currentTimeMillis() - STATS_MILIS_TTL;
		Iterator<InstrumentTick> it = ticks.iterator();
		while (it.hasNext()) {
			InstrumentTick tick = it.next();
			if (tick.getTimestamp() < minTime) {
				it.remove();
				continue;
			}
			total += tick.getPrice();
			if (calculatedStats.getCount() == 0 || tick.getPrice() < calculatedStats.getMin()) {
				calculatedStats.setMin(tick.getPrice());
			}
			if (calculatedStats.getCount() == 0 || tick.getPrice() > calculatedStats.getMax()) {
				calculatedStats.setMax(tick.getPrice());
			}
			calculatedStats.setCount(calculatedStats.getCount() + 1);
		}
		if (calculatedStats.getCount() > 0) {
			calculatedStats.setAvg(total / calculatedStats.getCount());
		}
		return calculatedStats;
	}

}
