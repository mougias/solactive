package com.stefanosdemetriou.solactive.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Stats {

	private double avg;

	private double max;

	private double min;

	private long count;
}
