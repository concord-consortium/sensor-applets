package org.concord.sensor.applet;

public interface SensorAppletAPI {

	public abstract boolean initSensorInterface(String listenerPath);

	public abstract void startCollecting();

	public abstract void stopCollecting();

}