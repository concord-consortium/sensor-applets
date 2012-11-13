package org.concord.sensor.applet;

import java.awt.EventQueue;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.swing.JApplet;

import org.concord.sensor.ExperimentConfig;
import org.concord.sensor.device.SensorDevice;

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
	private SensorDevice device;
	private JavascriptDataBridge jsBridge;
	private boolean deviceIsRunning = false;
	private ExperimentConfig actualConfig;
    
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

		deviceIsRunning = device.start();		
		System.out.println("started device");

		final float [] data = new float [1024];
		Thread t = new Thread() {
			public void run() {
				while(deviceIsRunning){
					final int numSamples = device.read(data, 0, 1, null);
					if(numSamples > 0) {
						final float[] dataCopy = Arrays.copyOfRange(data, 0, numSamples);
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								jsBridge.handleData(numSamples, dataCopy);
							}
						});
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

		device = util.getDevice();
		actualConfig = util.configureDevice(device);
	}

	private void tearDownDevice() {
		if(device != null){
			device.close();
			device = null;
		}
	}

}
