package com.stefanosdemetriou.solactive.domain;

import com.stefanosdemetriou.solactive.web.dto.Stats;

public class InstrumentStatsCalculator {

	public static final int STATS_MILIS_TTL = 60 * 1000;

	private Stats calculatedStats = new Stats();

	private InstrumentTickListNode head;
	private InstrumentTickListNode tail;

	public synchronized void addTick(InstrumentTick tick) {
		long minTime = System.currentTimeMillis() - STATS_MILIS_TTL;

		// while we are here, clear expired ticks
		cleanExpiredTicks(minTime);

		// new tick is older than time allowed, so ignore it
		if (tick.getTimestamp() < minTime) {
			return;
		}

		// list empty (either first addition, or no addition for TTL)
		if (tail == null) {
			tail = new InstrumentTickListNode(tick, null, null);
			head = tail;
			calculatedStats = new Stats(tick.getPrice(), tick.getPrice(), tick.getPrice(), 1L);
			return;
		}

		// oldest tick we got, should be added first to the list
		if (head.getTick().getTimestamp() > tick.getTimestamp()) {
			InstrumentTickListNode newNode = new InstrumentTickListNode(tick, head, null);
			head.setPrev(newNode);
			head = newNode;
			recalculate();
			return;
		}

		// ticks should mostly come in sorted by time, so traverse list from last to
		// first to find where new tick should be placed
		InstrumentTickListNode current = tail;
		while (current != null && current.getTick().getTimestamp() > tick.getTimestamp()) {
			current = current.getPrev();
		}

		InstrumentTickListNode newNode = new InstrumentTickListNode(tick, current.getNext(), current);
		if (current.getNext() != null) {
			current.getNext().setPrev(newNode);
		}
		current.setNext(newNode);

		// force recalculation
		recalculate();
	}

	public synchronized Stats getCachedStats() {
		// recalculate if needed
		long minTime = System.currentTimeMillis() - STATS_MILIS_TTL;
		if (head != null && minTime > head.getTick().getTimestamp()) {
			cleanExpiredTicks(minTime);
			recalculate();
		}
		return calculatedStats;
	}

	private void cleanExpiredTicks(long minTime) {
		while (head != null && head.getTick().getTimestamp() < minTime) {
			head = head.getNext();
		}
		if (head == null) {
			tail = null;
		}
	}

	private void recalculate() {
		calculatedStats = new Stats();
		if (head == null) {
			return;
		}

		InstrumentTickListNode current = head;
		double total = 0;
		while (current != null) {
			InstrumentTick tick = current.getTick();
			total += tick.getPrice();
			if (calculatedStats.getCount() == 0 || tick.getPrice() < calculatedStats.getMin()) {
				calculatedStats.setMin(tick.getPrice());
			}
			if (calculatedStats.getCount() == 0 || tick.getPrice() > calculatedStats.getMax()) {
				calculatedStats.setMax(tick.getPrice());
			}
			calculatedStats.setCount(calculatedStats.getCount() + 1);
			current = current.getNext();
		}
		if (calculatedStats.getCount() > 0) {
			calculatedStats.setAvg(total / calculatedStats.getCount());
		}
	}

}
