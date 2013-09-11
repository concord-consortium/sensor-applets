package org.concord.sensor.applet;

import java.applet.Applet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import netscape.javascript.JSException;
import netscape.javascript.JSObject;

public class JavascriptDataBridge {
    private JSObject window;
    private String handlerPath;
	private ScheduledExecutorService jsBridgeExecutor;

    public JavascriptDataBridge(String javascriptObjectPath, Applet applet) {
        window = JSObject.getWindow(applet);
        handlerPath = javascriptObjectPath;
		jsBridgeExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public void notifyDeviceUnplugged() {
		jsBridgeExecutor.schedule(new Runnable() {
			public void run() {
				System.err.println("Notifying device was unplugged.");
		        window.eval(handlerPath + ".deviceUnplugged()");
			}
		}, 0, TimeUnit.MILLISECONDS);
    }

    public void notifySensorUnplugged() {
		jsBridgeExecutor.schedule(new Runnable() {
			public void run() {
				System.err.println("Notifying sensor was unplugged.");
		        window.eval(handlerPath + ".sensorUnplugged()");
			}
		}, 0, TimeUnit.MILLISECONDS);
    }
    
	public void initSensorInterfaceComplete(final boolean booleanValue) {
		jsBridgeExecutor.schedule(new Runnable() {
			public void run() {
		        window.eval(handlerPath + ".initSensorInterfaceComplete(" + booleanValue + ")");
			}
		}, 0, TimeUnit.MILLISECONDS);
	}

	// We're using JSObject.eval() instead of using JSObject.call() because Firefox has problems with call()
    public void handleData(final int numSamples, final int numSensors, final float[] data) {
		jsBridgeExecutor.schedule(new Runnable() {
			public void run() {
		        String evalString = getJsEventCall(numSamples, numSensors, data);
		        try {
		            window.eval(evalString);
		        } catch (JSException e) {
		            System.err.println("Javascript error: " + e.getMessage());
		            e.printStackTrace();
		        }
			}
		}, 0, TimeUnit.MILLISECONDS);
    }

    private String getJsEventCall(int numSamples, int numSensors, float[] data) {
        StringBuffer buf = new StringBuffer();
        buf.append(handlerPath);
        buf.append(".dataReceived(");
        buf.append(1000); // 1000 is DataStreamEvent.DATA_RECEIVED
        buf.append(", " + numSamples);
        buf.append(", " + arrayAsString(data, numSamples, numSensors));
        buf.append(");");
        return buf.toString();
    }
    
    private String arrayAsString(float[] arr, int numSamples, int numSensors) {
        if (arr == null) {
            return "null";
        }
        StringBuffer buf = new StringBuffer();
        buf.append("[");
        int last = numSamples * numSensors;
        for (int i = 0; i < last; i++) {
            buf.append(arr[i]);
            if (i != last-1) {
                buf.append(",");
            }
        }
        buf.append("]");
        return buf.toString();
    }

}
