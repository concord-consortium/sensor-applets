package org.concord.sensor.applet;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JApplet;

import org.concord.sensor.SensorRequest;
import org.concord.sensor.applet.exception.SensorAppletException;
import org.concord.sensor.impl.SensorRequestImpl;

/**
 * This applet expects the following params:
 *   device: the device name to use (supports: golink, labquest, pseudo, manual)
 *     - if 'manual', you will also need to specify:
 *       deviceId (int): the device id you want to use
 *       openString (String; optional): the open string needed to be passed to the device (some devices don't need this)
 *   probeType: The probe type to use (supports: temperature, light, distance, co2, force 5n, force 50n, manual)
 *     - if 'manual', you will also need to specify:
 *       period (float): how often to take a sample
 *       precision (int): how many significant digits to report
 *       min (float): min supported value
 *       max (float): max supported value
 *       sensorPort (int): which port the sensor is attached to the device
 *       stepSize (float): the maximum step size between values
 *       sensorType (int): one of the constants from SensorConfig (eg SensorConfig.QUANTITY_TEMPERATURE)
 * @author aunger
 *
 */
public class SensorApplet extends JApplet implements SensorAppletAPI {
    private static final long serialVersionUID = 1L;
    
    private static final Logger logger = Logger.getLogger(SensorApplet.class.getName());
    
	private JavascriptDataBridge jsBridge;

	private HashMap<String, SensorUtil> sensorUtils = new HashMap<String, SensorUtil>();
    
    public enum State {
        READY, RUNNING, STOPPED, UNKNOWN
    }
    
    @Override
    public void destroy() {
    	for (Map.Entry<String, SensorUtil> entry : sensorUtils.entrySet()) {
    		SensorUtil util = entry.getValue();
		    util.destroy();
    	}
    	sensorUtils.clear();
    	super.destroy();
    }
    
    private SensorUtil findOrCreateUtil(String deviceType) {
    	SensorUtil util = sensorUtils.get(deviceType);
		if (util == null) {
			logger.info("Creating new util...");
			util = new SensorUtil(this, deviceType);
			sensorUtils.put(deviceType, util);
		}
		return util;
    }
    
    public boolean initSensorInterface(final String listenerPath, final String deviceType, final SensorRequest[] sensors) {
    	Boolean b = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
    		public Boolean run() {
    			try {
    				// Create the data bridge
    				logger.info("Setting things up: " + listenerPath + ", " + deviceType + ", " + sensors);
    				jsBridge = new JavascriptDataBridge(listenerPath, SensorApplet.this);

    				
    				findOrCreateUtil(deviceType).setupDevice(sensors);

    				jsBridge.sensorsReady();
    			} catch (SensorAppletException e) {
    				e.printStackTrace();
    				return Boolean.FALSE;
    			}
    			
    			return Boolean.TRUE;
    		}
    	});
        
		return b.booleanValue();
	}
    
    public boolean isInterfaceConnected(final String deviceType) {
    	Boolean b = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
    		public Boolean run() {
				SensorUtil util = findOrCreateUtil(deviceType);
				if (util.isCollectable()) {
					return Boolean.TRUE;
				}
    			return Boolean.FALSE;
    		}
		});
    	return b.booleanValue();
    }

	public void stopCollecting() {
		AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
			public Boolean run() {
				for (Map.Entry<String, SensorUtil> entry : sensorUtils.entrySet()) {
					try {
						SensorUtil util = entry.getValue();
						if (util.isRunning()) {
							util.stopDevice();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				return Boolean.TRUE;
			}
		});
	}
    
    public void startCollecting() {
		stopCollecting();
		
		AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
			public Boolean run() {
				for (Map.Entry<String, SensorUtil> entry : sensorUtils.entrySet()) {
					SensorUtil util = entry.getValue();
					if (util.isCollectable()) {
						try {
							util.startDevice(jsBridge);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				return Boolean.TRUE;
			}
		});
    }
    
    public SensorRequestImpl getSensorRequest(String sensorType) {
    	return SensorUtil.getSensorRequest(sensorType);
    }

}
