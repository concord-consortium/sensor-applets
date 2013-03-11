package org.concord.sensor.applet.exception;

public class ConfigureDeviceException extends SensorAppletException {
	private static final long serialVersionUID = 1L;
	
	public ConfigureDeviceException() {
		super();
	}
	
	public ConfigureDeviceException(String msg) {
		super(msg);
	}
	
	public ConfigureDeviceException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	public ConfigureDeviceException(Throwable cause) {
		super(cause);
	}
	
	public ConfigureDeviceException(String msg, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(msg, cause, enableSuppression, writableStackTrace);
	}

}
