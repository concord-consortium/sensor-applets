package org.concord.sensor.applet.exception;

public class SensorAppletException extends Exception {
	private static final long serialVersionUID = 1L;
	
	public SensorAppletException() {
		super();
	}
	
	public SensorAppletException(String msg) {
		super(msg);
	}
	
	public SensorAppletException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	public SensorAppletException(Throwable cause) {
		super(cause);
	}
	
	public SensorAppletException(String msg, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(msg, cause, enableSuppression, writableStackTrace);
	}

}
