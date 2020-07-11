package com.stefanosdemetriou.solactive;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.stefanosdemetriou.solactive.domain.InstrumentStatsCalculator;
import com.stefanosdemetriou.solactive.domain.InstrumentTick;
import com.stefanosdemetriou.solactive.web.dto.Stats;
import com.stefanosdemetriou.solactive.web.dto.Tick;
import com.stefanosdemetriou.solactive.web.exceptions.NoSuchInstrumentException;
import com.stefanosdemetriou.solactive.web.exceptions.TickTooOldException;

@Component
public class StatsManager extends Thread {

	private final Map<String, InstrumentStatsCalculator> instrumentStats = new ConcurrentHashMap<>();

	private final ExecutorService executor = Executors
			.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

	private AtomicReference<Stats> globalStats = new AtomicReference<>();

	@PostConstruct
	public void init() {
		this.start();
	}

	@Override
	public void run() {
		while (!Thread.interrupted()) {
			List<Callable<Void>> callables = new ArrayList<>();
			Iterator<InstrumentStatsCalculator> it = instrumentStats.values().iterator();
			while (it.hasNext()) {
				InstrumentStatsCalculator calc = it.next();
				callables.add(new Callable<Void>() {
					@Override
					public Void call() throws Exception {
						calc.cleanExpiredTicks();
						return null;
					}
				});
			}

			// wait for cleanup of expired ticks on all instruments
			try {
				List<Future<Void>> futures = executor.invokeAll(callables);
				for (Future<Void> future : futures) {
					future.get();
				}
			} catch (InterruptedException | ExecutionException e) {
				Thread.currentThread().interrupt();
			}

			recalculateGlobalStats();
		}
	}

	public void addTick(Tick tick) throws TickTooOldException {
		if (tick == null
				|| tick.getTimestamp() < System.currentTimeMillis() - InstrumentStatsCalculator.STATS_MILIS_TTL) {
			throw new TickTooOldException();
		}

		instrumentStats.putIfAbsent(tick.getInstrument(), new InstrumentStatsCalculator());
		instrumentStats.get(tick.getInstrument()).addTick(new InstrumentTick(tick.getTimestamp(), tick.getPrice()));

		recalculateGlobalStats();
	}

	public synchronized Stats getTotalStats() {
		Stats stats = globalStats.get();
		stats = (stats == null) ? new Stats() : stats;
		return stats;
	}

	public Stats getStatsForInstrument(String instrument) throws NoSuchInstrumentException {
		InstrumentStatsCalculator calc = instrumentStats.get(instrument);
		if (calc == null) {
			throw new NoSuchInstrumentException();
		}
		return calc.getCachedStats();
	}

	private synchronized void recalculateGlobalStats() {
		// recalculate global stats
		// strictly speaking we could do diff calc but this is an O(n) operation, where
		// n is just the number of instruments, so we'll leave it for now
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
		globalStats.set(newGlobalStats);
	}

}
