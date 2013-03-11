package org.concord.sensor.applet.exception;

public class CreateDeviceException extends SensorAppletException {
	private static final long serialVersionUID = 1L;
	
	public CreateDeviceException() {
		super();
	}
	
	public CreateDeviceException(String msg) {
		super(msg);
	}
	
	public CreateDeviceException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	public CreateDeviceException(Throwable cause) {
		super(cause);
	}
	
	public CreateDeviceException(String msg, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(msg, cause, enableSuppression, writableStackTrace);
	}

}
