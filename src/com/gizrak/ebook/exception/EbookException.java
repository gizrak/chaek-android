package com.gizrak.ebook.exception;

public class EbookException extends Exception {

	private static final long serialVersionUID = -6408954401840213609L;
	
	private String message;
	
	public EbookException(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
