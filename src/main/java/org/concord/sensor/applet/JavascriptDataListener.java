package org.concord.sensor.applet;

public interface JavascriptDataListener {
    /**
     * Called when sensor data is received.
     * @param dataEventType -- See org.concord.framework.data.stream.DataStreamEvent
     * @param numberOfSamples -- Number of data points in this event
     * @param data -- float array of actual data. Length with match numberOfSamples
     */
    public void dataReceived(int dataEventType, int numberOfSamples, float[] data);
    
    /**
     * Called when the applet has initialized and the sensors are ready to go
     */
    public void sensorsReady();
    
    /**
     * Called when a device is unexpectedly unplugged (eg during data collection)
     */
    public void deviceUnplugged();
    
    /**
     * Called when a sensor is unexpectedly unplugged, but the device is still attached (eg during data collection)
     */
    public void sensorUnplugged();
}
