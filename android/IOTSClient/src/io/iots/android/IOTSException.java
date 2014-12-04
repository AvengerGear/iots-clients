package io.iots.android;

public class IOTSException extends Exception {
	/**
	 * Serial Version 1
	 */
	private static final long serialVersionUID = 1L;
	
	public int status;
	
	public IOTSException(String message){
		super(message);
	}
	
	public IOTSException(String message, int status){
		super(message);
		this.status = status;
	}
}
