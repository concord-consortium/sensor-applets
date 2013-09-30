package org.concord.sensor.applet;

import org.concord.sensor.SensorRequest;
import org.concord.sensor.impl.SensorRequestImpl;

public interface SensorAppletAPI {

	public abstract void initSensorInterface(String listenerPath, String deviceType, SensorRequest[] sensors);

	public abstract void startCollecting();

	public abstract void stopCollecting();
	
	public abstract void isInterfaceConnected(String deviceType, String callbackIndex);

	public abstract void getAttachedSensors(String deviceType, String callbackIndex);

	public abstract void getDeviceConfiguration(String deviceType, String callbackIndex);

	public abstract SensorRequestImpl getSensorRequest(String sensorType);

	public abstract String getTypeConstantName(int type);

}