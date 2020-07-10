package com.stefanosdemetriou.solactive.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class InstrumentTickListNode {
	private InstrumentTick tick;
	private InstrumentTickListNode next;
	private InstrumentTickListNode prev;
}
