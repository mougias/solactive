package com.stefanosdemetriou.solactive.web.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NO_CONTENT, reason = "Tick is older than 60 seconds.")
public class TickTooOldException extends Exception {

	private static final long serialVersionUID = 5618157301355397144L;

}
