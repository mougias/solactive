package com.stefanosdemetriou.solactive;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.stefanosdemetriou.solactive.domain.InstrumentStatsCalculator;
import com.stefanosdemetriou.solactive.domain.InstrumentTick;
import com.stefanosdemetriou.solactive.web.dto.Stats;
import com.stefanosdemetriou.solactive.web.dto.Tick;
import com.stefanosdemetriou.solactive.web.exceptions.NoSuchInstrumentException;
import com.stefanosdemetriou.solactive.web.exceptions.TickTooOldException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StatsManager {

	private final Map<String, InstrumentStatsCalculator> instrumentStats = new ConcurrentHashMap<>();

	private Stats globalStats;

	public void addTick(Tick tick) throws TickTooOldException {
		if (tick == null
				|| tick.getTimestamp() < System.currentTimeMillis() - InstrumentStatsCalculator.STATS_MILIS_TTL) {
			throw new TickTooOldException();
		}

		long startTime = System.currentTimeMillis();

		instrumentStats.putIfAbsent(tick.getInstrument(), new InstrumentStatsCalculator());
		instrumentStats.get(tick.getInstrument()).addTick(new InstrumentTick(tick.getTimestamp(), tick.getPrice()));
		recalculateGlobalStats();

		long elapsedTime = System.currentTimeMillis() - startTime;
		if (elapsedTime > 1000) {
			log.warn("Recalculating stats took more than 1 second to complete");
		}
		log.trace("Stats recalculation time {}ms", elapsedTime);
	}

	public Stats getTotalStats() {
		recalculateGlobalStats();
		return globalStats;
	}

	public Stats getStatsForInstrument(String instrument) throws NoSuchInstrumentException {
		InstrumentStatsCalculator calc = instrumentStats.get(instrument);
		if (calc == null) {
			throw new NoSuchInstrumentException();
		}
		return calc.getCachedStats();
	}

	private void recalculateGlobalStats() {
		// you can't diff recalculate min and max, so need to do this fully
		Stats newGlobalStats = new Stats();
		double total = 0;
		for (InstrumentStatsCalculator calc : instrumentStats.values()) {
			Stats stats = calc.getCachedStats();
			if (newGlobalStats.getCount() == 0 || (stats.getCount() > 0 && stats.getMax() > newGlobalStats.getMax())) {
				newGlobalStats.setMax(stats.getMax());
			}
			if (newGlobalStats.getCount() == 0 || (stats.getCount() > 0 && stats.getMin() < newGlobalStats.getMin())) {
				newGlobalStats.setMin(stats.getMin());
			}
			newGlobalStats.setCount(newGlobalStats.getCount() + stats.getCount());
			total += stats.getAvg() * stats.getCount();
		}
		if (newGlobalStats.getCount() > 0) {
			newGlobalStats.setAvg(total / newGlobalStats.getCount());
		}
		globalStats = newGlobalStats;
	}

}
