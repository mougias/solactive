package com.stefanosdemetriou.solactive.web.controllers;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.stefanosdemetriou.solactive.StatsManager;
import com.stefanosdemetriou.solactive.web.dto.Stats;
import com.stefanosdemetriou.solactive.web.dto.Tick;
import com.stefanosdemetriou.solactive.web.exceptions.NoSuchInstrumentException;
import com.stefanosdemetriou.solactive.web.exceptions.TickTooOldException;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
public class TicksController {

	private final StatsManager statsManager;

	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping("/ticks")
	public void addTick(@RequestBody @Valid Tick tick) throws TickTooOldException {
		statsManager.addTick(tick);
	}

	@GetMapping("/statistics")
	public Stats getGlobalStats() {
		return statsManager.getTotalStats();
	}

	@GetMapping("/statistics/{instrument}")
	public Stats getInstrumentStats(@PathVariable String instrument) throws NoSuchInstrumentException {
		return statsManager.getStatsForInstrument(instrument);
	}
}
