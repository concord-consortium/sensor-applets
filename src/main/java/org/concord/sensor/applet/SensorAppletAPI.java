package org.concord.sensor.applet;

import org.concord.sensor.ExperimentConfig;
import org.concord.sensor.SensorConfig;
import org.concord.sensor.SensorRequest;
import org.concord.sensor.impl.SensorRequestImpl;

public interface SensorAppletAPI {

	public abstract boolean initSensorInterface(String listenerPath, String deviceType, SensorRequest[] sensors);

	public abstract void startCollecting();

	public abstract void stopCollecting();
	
	public abstract boolean isInterfaceConnected(String deviceType);

	public abstract SensorConfig[] getAttachedSensors(String deviceType);

	public abstract ExperimentConfig getDeviceConfiguration(String deviceType);

	public abstract SensorRequestImpl getSensorRequest(String sensorType);

	public abstract String getTypeConstantName(int type);

}