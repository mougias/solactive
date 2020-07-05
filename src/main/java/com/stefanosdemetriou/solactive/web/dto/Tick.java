package com.stefanosdemetriou.solactive.web.dto;

import javax.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Tick {

	@NotBlank
	private String instrument;

	private double price;
	
	private long timestamp;
}
