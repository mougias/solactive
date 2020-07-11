package com.stefanosdemetriou.solactive.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Stats {

	private double avg;

	private double max;

	private double min;

	private long count;
}
