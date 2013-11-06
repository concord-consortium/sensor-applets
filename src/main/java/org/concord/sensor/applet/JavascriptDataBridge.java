package org.concord.sensor.applet;

import java.applet.Applet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import netscape.javascript.JSException;
import netscape.javascript.JSObject;

import org.concord.sensor.ExperimentConfig;
import org.concord.sensor.SensorConfig;
import org.json.JSONStringer;
import org.json.JSONWriter;

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
		handleCallback("\"initSensorInterfaceComplete\"", new String[] { Boolean.toString(booleanValue) });
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

	public void destroy() {
		jsBridgeExecutor.shutdownNow();
	}
	
	public String asArgs(float[] arr) {
		return arrayAsString(arr, arr.length, 1);
	}
	
	public void handleCallback(final String idx, final String[] args) {
		jsBridgeExecutor.schedule(new Runnable() {
			public void run() {
		        StringBuffer buf = new StringBuffer();
		        buf.append(handlerPath);
		        buf.append(".handleCallback(");
		        buf.append(idx);
		        if (args != null && args.length > 0) {
			        buf.append(", [");
			        for (int i = 0; i < args.length; i++) {
			            buf.append(args[i]);
			            if (i != args.length-1) {
			                buf.append(",");
			            }
			        }
			        buf.append("]");
		        }
		        buf.append(");");
		        try {
		            window.eval(buf.toString());
		        } catch (JSException e) {
		            System.err.println("Javascript error: " + e.getMessage());
		            e.printStackTrace();
		        }
			}
		}, 0, TimeUnit.MILLISECONDS);
	}

	public static JSONWriter toJSON(ExperimentConfig config) {
		JSONWriter writer = new JSONStringer();
		writer.object();
		writer.key("valid").value(config.isValid());
		writer.key("invalidReason").value(config.getInvalidReason());
		writer.key("period").value(config.getPeriod());
		writer.key("exactPeriod").value(config.getExactPeriod());
		writer.key("dataReadPeriod").value(config.getDataReadPeriod());
		writer.key("deviceName").value(config.getDeviceName());
		writer.key("sensorConfigs").array();
		for (SensorConfig cfg : config.getSensorConfigs()) {
			toJSON(cfg, writer);
		}
		writer.endArray();
		writer.endObject();
		return writer;
	}

	public static JSONWriter toJSON(SensorConfig config) {
		JSONWriter writer = new JSONStringer();
		toJSON(config, writer);
		return writer;
	}
	
	public static JSONWriter toJSON(SensorConfig config, JSONWriter writer) {
		writer.object();
		writer.key("confirmed").value(config.isConfirmed());
		writer.key("type").value(config.getType());
		writer.key("stepSize").value(config.getStepSize());
		writer.key("port").value(config.getPort());
		writer.key("portName").value(config.getPortName());
		writer.key("name").value(config.getName());
		writer.key("unit").value(config.getUnit());
		writer.endObject();
		return writer;
	}

	public static JSONWriter toJSON(SensorConfig[] configs) {
		JSONWriter writer = new JSONStringer();
		writer.array();
		for (SensorConfig cfg : configs) {
			toJSON(cfg, writer);
		}
		writer.endArray();
		return writer;
	}

}
