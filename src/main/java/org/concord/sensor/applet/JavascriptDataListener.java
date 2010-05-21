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
     * Called when a stream event is received
     * @param dataEventType -- See org.concord.framework.data.stream.DataStreamEvent
     * @param numberOfSamples -- Number of data points in this event
     * @param data -- float array of actual data. Length with match numberOfSamples
     */
    public void dataStreamEvent(int dataEventType, int numberOfSamples, float[] data);
}
