package com.stefanosdemetriou.solactive.domain;

import com.stefanosdemetriou.solactive.web.dto.Stats;

public class InstrumentStatsCalculator {

	public static final int STATS_MILIS_TTL = 60 * 1000;

	private Stats calculatedStats = new Stats();

	private InstrumentTickListNode head;
	private InstrumentTickListNode tail;

	public synchronized void addTick(InstrumentTick tick) {
		final long minTime = System.currentTimeMillis() - STATS_MILIS_TTL;

		// new tick is older than time allowed, so ignore it
		if (tick.getTimestamp() < minTime) {
			return;
		}

		// list empty (either first addition, or no addition for TTL)
		if (tail == null) {
			tail = new InstrumentTickListNode(tick, null, null);
			head = tail;
			addSingleTickToStats(tick);
			return;
		}

		// oldest tick we got, should be added first to the list
		if (head.getTick().getTimestamp() > tick.getTimestamp()) {
			InstrumentTickListNode newNode = new InstrumentTickListNode(tick, head, null);
			head.setPrev(newNode);
			head = newNode;
			addSingleTickToStats(tick);
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

		addSingleTickToStats(tick);
	}

	public synchronized Stats getCachedStats() {
		return calculatedStats;
	}

	public void cleanExpiredTicks() {
		final long minTime = System.currentTimeMillis() - STATS_MILIS_TTL;

		boolean fullRecalc = false;
		long removedCount = 0;
		double removedAmount = 0;
		while (head != null && head.getTick().getTimestamp() < minTime) {
			removedCount++;
			removedAmount += head.getTick().getPrice();
			fullRecalc = fullRecalc || head.getTick().getPrice() == calculatedStats.getMax()
					|| head.getTick().getPrice() == calculatedStats.getMin();
			head = head.getNext();
		}
		if (head == null) {
			tail = null;
			calculatedStats = new Stats();
			return;
		}

		// if we lost min or max then we need full traversal to find the new ones
		// otherwise we can just diff calc avg and count
		if (fullRecalc) {
			recalculate();
		} else {
			double newAmount = calculatedStats.getAvg() * calculatedStats.getCount() - removedAmount;
			long newCount = calculatedStats.getCount() - removedCount;
			calculatedStats.setAvg(newAmount / newCount);
			calculatedStats.setCount(newCount);
		}
	}

	private void addSingleTickToStats(InstrumentTick tick) {
		double newAvg = (calculatedStats.getAvg() * calculatedStats.getCount() + tick.getPrice())
				/ (calculatedStats.getCount() + 1);
		double min = calculatedStats.getCount() == 0 ? tick.getPrice()
				: Math.min(tick.getPrice(), calculatedStats.getMin());
		double max = calculatedStats.getCount() == 0 ? tick.getPrice()
				: Math.max(tick.getPrice(), calculatedStats.getMax());

		calculatedStats.setAvg(newAvg);
		calculatedStats.setCount(calculatedStats.getCount() + 1);
		calculatedStats.setMin(min);
		calculatedStats.setMax(max);
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
