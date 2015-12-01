package com.wang.resource.exceptions;

public class NotModifiedException extends RuntimeException {
	private static final long serialVersionUID = -1575358021535691374L;
	int errorCode = 304;

	public int getErrorCode() {
		return errorCode;
	}
}
