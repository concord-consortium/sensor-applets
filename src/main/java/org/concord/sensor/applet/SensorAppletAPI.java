package org.concord.sensor.applet;

import org.concord.sensor.SensorRequest;

public interface SensorAppletAPI {

	public abstract boolean initSensorInterface(String listenerPath, String deviceType, SensorRequest[] sensors);

	public abstract void startCollecting();

	public abstract void stopCollecting();

}