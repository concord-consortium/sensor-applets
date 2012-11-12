package org.concord.sensor.applet;

import java.awt.EventQueue;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Logger;

import javax.swing.JApplet;

import org.concord.sensor.ExperimentConfig;
import org.concord.sensor.SensorConfig;
import org.concord.sensor.SensorRequest;
import org.concord.sensor.device.SensorDevice;
import org.concord.sensor.device.impl.DeviceConfigImpl;
import org.concord.sensor.device.impl.DeviceID;
import org.concord.sensor.device.impl.JavaDeviceFactory;
import org.concord.sensor.impl.ExperimentRequestImpl;
import org.concord.sensor.impl.SensorRequestImpl;
import org.concord.sensor.impl.SensorUtilJava;

/**
 * This applet expects the following params:
 *   device: the device name to use (supports: golink, labquest, pseudo)
 *   probeType: The probe type to use (supports: temperature, light, distance)
 * @author aunger
 *
 */
public class SensorApplet extends JApplet implements SensorAppletAPI {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(SensorApplet.class.getName());
    
	private JavaDeviceFactory deviceFactory;
	private SensorDevice device;
	private JavascriptDataBridge jsBridge;
	private boolean deviceIsRunning = false;
    
    public enum State {
        READY, RUNNING, STOPPED, UNKNOWN
    }
    
    @Override
    public void destroy() {
    	super.destroy();
    	
		tearDownDevice();
    }
    
    public boolean initSensorInterface(final String listenerPath) {
    	AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
    		public Boolean run() {
    			try {
    				setupInterface(listenerPath);
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    			
    			return Boolean.TRUE;
    		}
    	});
        
		return true;
	}

	private void setupInterface(final String listenerPath) {
		// Create the data bridge
		jsBridge = new JavascriptDataBridge(listenerPath, SensorApplet.this);

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				setupDevice();

				jsBridge.sensorsReady();
			}
		});
	}

	public void stopCollecting() {
		AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
			public Boolean run() {
				try {
					stopDevice();
				} catch (Exception e) {
					e.printStackTrace();
				}

				return Boolean.TRUE;
			}
		});
	}

	private void stopDevice() {
		if (device != null && deviceIsRunning) {
			device.stop(true);
			deviceIsRunning = false;
		}
	}
    
    public void startCollecting() {
		stopCollecting();
		
    	AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
    		public Boolean run() {
    			try {
    				startDevice();
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    			return Boolean.TRUE;
    		}
    	});
    }

	private void startDevice() {
		if (device == null) {
			setupDevice();
		}
		// Check what is attached, this isn't necessary if you know what you want
		// to be attached.  But sometimes you want the user to see what is attached
		ExperimentConfig currentConfig = device.getCurrentConfig();
		SensorUtilJava.printExperimentConfig(currentConfig);


		ExperimentRequestImpl request = new ExperimentRequestImpl();

		SensorRequest sensor = getSensorRequest(request);

		request.setSensorRequests(new SensorRequest [] { sensor });

		final ExperimentConfig actualConfig = device.configure(request);

		deviceIsRunning = device.start();		
		System.out.println("started device");

		final float [] data = new float [1024];
		Thread t = new Thread() {
			public void run() {
				while(deviceIsRunning){
					int numSamples = device.read(data, 0, 1, null);
					if(numSamples > 0) {
						jsBridge.handleData(numSamples, data);
					}
					try {
						Thread.sleep((long)(actualConfig.getDataReadPeriod()*1000));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		t.start();
	}

	private void setupDevice() {
		tearDownDevice();
		deviceFactory = new JavaDeviceFactory();
		
		int deviceId = getDeviceId();
		device = deviceFactory.createDevice(new DeviceConfigImpl(deviceId, getOpenString(deviceId)));
	}

	private void tearDownDevice() {
		if(device != null){
			device.close();
			device = null;
		}
	}
    
    private int getDeviceId() {
    	String id = getParameter("device");
    	logger.info("Got device of: " + id);
    	if (id.equals("golink")) {
			return DeviceID.VERNIER_GO_LINK_JNA;
    	} else if (id.equals("labquest")) {
    		return DeviceID.VERNIER_LAB_QUEST;
    	} else {
    		return DeviceID.PSEUDO_DEVICE;
    	}
    }
    
    private String getOpenString(int deviceId) {
    	switch (deviceId) {
		case DeviceID.VERNIER_GO_LINK_JNA:
		case DeviceID.VERNIER_LAB_QUEST:
			return null;
		default:
			return null;
		}
    }
    
    private SensorRequest getSensorRequest(ExperimentRequestImpl experiment) {
    	String type = getParameter("probeType");
    	logger.info("Got probeType of: " + type);
    	if (type == null) { type = "temperature"; }
    	type = type.toLowerCase();
    	
		SensorRequestImpl sensor = new SensorRequestImpl();
		
		if (type.equals("light")) {
			experiment.setPeriod(0.1f);
			configureSensorRequest(sensor, 0, 0.0f, 4000.0f, 0, 0.1f, SensorConfig.QUANTITY_LIGHT);
		} else if (type.equals("position") || type.equals("distance")) {
			experiment.setPeriod(0.1f);
			configureSensorRequest(sensor, -2, 0.0f, 4.0f, 0, 0.1f, SensorConfig.QUANTITY_DISTANCE);
		} else {
			// fall back to temperature
			experiment.setPeriod(0.1f);
			configureSensorRequest(sensor, -1, 0.0f, 40.0f, 0, 0.1f, SensorConfig.QUANTITY_TEMPERATURE);
		}
		experiment.setNumberOfSamples(-1);
		
		return sensor;
    }
    
    private void configureSensorRequest(SensorRequestImpl sensor, int precision, float min, float max, int port, float step, int type) {
		sensor.setDisplayPrecision(precision);
		sensor.setRequiredMin(min);
		sensor.setRequiredMax(max);
		sensor.setPort(port);
		sensor.setStepSize(step);
		sensor.setType(type);
    }
}
