package org.concord.sensor.applet;

import java.awt.EventQueue;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Logger;

import javax.swing.JApplet;

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
    
    private SensorUtil util;
	private JavascriptDataBridge jsBridge;
    
    public enum State {
        READY, RUNNING, STOPPED, UNKNOWN
    }
    
    @Override
    public void init() {
    	super.init();
    	util = new SensorUtil(this);
    }
    
    @Override
    public void destroy() {
    	super.destroy();
    	
		util.tearDownDevice();
		util.destroy();
		util = null;
    }
    
    public boolean initSensorInterface(final String listenerPath) {
    	AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
    		public Boolean run() {
    			try {
    				// Create the data bridge
    				jsBridge = new JavascriptDataBridge(listenerPath, SensorApplet.this);

    				util.setupDevice();

    				jsBridge.sensorsReady();
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    			
    			return Boolean.TRUE;
    		}
    	});
        
		return true;
	}

	public void stopCollecting() {
		AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
			public Boolean run() {
				try {
					util.stopDevice();
				} catch (Exception e) {
					e.printStackTrace();
				}

				return Boolean.TRUE;
			}
		});
	}
    
    public void startCollecting() {
		stopCollecting();
		
    	AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
    		public Boolean run() {
    			try {
    				util.startDevice(jsBridge);
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    			return Boolean.TRUE;
    		}
    	});
    }

}
