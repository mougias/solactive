package com.stefanosdemetriou.solactive.web.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "There is no such instrument.")
public class NoSuchInstrumentException extends Exception {

	private static final long serialVersionUID = 4081639427882459397L;

}
