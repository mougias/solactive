package com.stefanosdemetriou.solactive.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InstrumentTick {
	private long timestamp;
	private double price;
}
