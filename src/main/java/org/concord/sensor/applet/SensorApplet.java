package org.concord.sensor.applet;

import java.awt.EventQueue;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JApplet;

import netscape.javascript.JSObject;

import org.concord.sensor.ExperimentConfig;
import org.concord.sensor.SensorConfig;
import org.concord.sensor.SensorRequest;
import org.concord.sensor.applet.exception.ConfigureDeviceException;
import org.concord.sensor.applet.exception.SensorAppletException;
import org.concord.sensor.impl.SensorRequestImpl;
import org.concord.sensor.impl.SensorUtilJava;

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

	private boolean notifiedJavascript;
	
    private ExperimentConfig latestExperimentConfig = null;
    
    private SensorConfig[] latestSensorConfig = null;
    
    public enum State {
        READY, RUNNING, STOPPED, UNKNOWN
    }
    
	@Override
	public void start() {
		// we only want to notify javascript once
		// previously this code was in the init method which would only run once
		// but that caused problems sometimes on IE.
		if (notifiedJavascript) {
			return;
		}
		notifiedJavascript = true;
		String codeToEval = getParameter("evalOnInit");
		if (codeToEval == null || codeToEval.length() == 0){
			return;
		}
	    JSObject window = JSObject.getWindow(this);
	    System.out.println("SensorApplet running evalOnInit code");
	    window.eval(codeToEval);
	}

    @Override
    public void destroy() {
    	for (Map.Entry<String, SensorUtil> entry : sensorUtils.entrySet()) {
    		SensorUtil util = entry.getValue();
		    util.destroy();
    	}
    	jsBridge.destroy();
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
    
    public void initSensorInterface(final String listenerPath, final String deviceType, final SensorRequest[] sensors) {
    	EventQueue.invokeLater(new Runnable() {
			public void run() {
		    	AccessController.doPrivileged(new PrivilegedAction<Void>() {
		    		public Void run() {
		    			try {
		    				// Create the data bridge
		    				logger.info("Setting things up: " + listenerPath + ", " + deviceType + ", " + sensors);
		    				jsBridge = new JavascriptDataBridge(listenerPath, SensorApplet.this);

		    				
		    				SensorUtil util = findOrCreateUtil(deviceType);
		    				util.initSensorInterface(sensors, jsBridge);
		    			} catch (Throwable t) {
		    				System.err.println("Caught unexpected runtime exception...");
		    				t.printStackTrace();
		    				jsBridge.initSensorInterfaceComplete(false);
		    			}
		    			return null;
		    		}
		    	});
			}
    	});
	}
    
    // XXX
    public void isInterfaceConnected(final String deviceType, final String callbackIndex) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
		    	Boolean b = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
		    		public Boolean run() {
						SensorUtil util = findOrCreateUtil(deviceType);
						if (util.isDeviceAttached()) {
							return Boolean.TRUE;
						}
		    			return Boolean.FALSE;
		    		}
				});
		    	
		    	jsBridge.handleCallback(callbackIndex, new String[] {b.toString()});
			}
		});
    }
    
    public ExperimentConfig getCachedDeviceConfiguration() {
    	return latestExperimentConfig;
    }
    
    public void getDeviceConfiguration(final String deviceType, final String callbackIndex) {
    	EventQueue.invokeLater(new Runnable() {
    		public void run() {
    	    	ExperimentConfig c = AccessController.doPrivileged(new PrivilegedAction<ExperimentConfig>() {
    	    		public ExperimentConfig run() {
    					SensorUtil util = findOrCreateUtil(deviceType);
    					try {
    						return util.getDeviceConfig();
    					} catch (ConfigureDeviceException e) {
    						e.printStackTrace();
    						return null;
    					}
    	    		}
    			});
    	    	latestExperimentConfig = c;
    	    	
    	    	jsBridge.handleCallback(callbackIndex, null);
    		}
    	});
    }
    
    public SensorConfig[] getCachedAttachedSensors() {
    	return latestSensorConfig;
    }
    
    // XXX
    public void getAttachedSensors(final String deviceType, final String callbackIndex) {
    	EventQueue.invokeLater(new Runnable() {
			public void run() {
		    	SensorConfig[] c = AccessController.doPrivileged(new PrivilegedAction<SensorConfig[]>() {
		    		public SensorConfig[] run() {
						SensorUtil util = findOrCreateUtil(deviceType);
						try {
							ExperimentConfig config = util.getDeviceConfig();
							if (config != null) {
								return config.getSensorConfigs();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
						return null;
		    		}
				});
    	    	latestSensorConfig = c;
    	    	
    	    	jsBridge.handleCallback(callbackIndex, null);
			}
		});
    }
    
    // XXX
    public void getAttachedSensorsValues(final String deviceType, final String callbackIndex) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				float[] values = getSensorsValues(deviceType, true);
				String args = jsBridge.asArgs(values);
				jsBridge.handleCallback(callbackIndex, new String[] {args});
			}
		});
    }
    
    // XXX
    public void getConfiguredSensorsValues(final String deviceType, final String callbackIndex) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				float[] values = getConfiguredSensorsValues(deviceType);
				String args = jsBridge.asArgs(values);
				jsBridge.handleCallback(callbackIndex, new String[] {args});
			}
		});
    }
    
    private float[] getConfiguredSensorsValues(String deviceType) {
    	SensorUtil util = findOrCreateUtil(deviceType);
		if (util.isActualConfigValid()) {
	    	return getSensorsValues(deviceType, false);
		} else {
			jsBridge.notifySensorUnplugged();
			util.reconfigureNextTime();
			return null;
		}
    }
    
    private float[] getSensorsValues(final String deviceType, final boolean allSensors) {
    	float[] out = AccessController.doPrivileged(new PrivilegedAction<float[]>() {
    		public float[] run() {
				SensorUtil util = findOrCreateUtil(deviceType);
				try {
					return util.readSingleValue(jsBridge, allSensors);
				} catch (SensorAppletException e) {
					e.printStackTrace();
				}
				return null;
    		}
		});
    	
    	return out;
    }

	public void stopCollecting() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
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
		});
	}
    
    public void startCollecting() {
		stopCollecting();
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
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
		});
    }
    
    // Doesn't need to be async
    public SensorRequestImpl getSensorRequest(String sensorType) {
    	return SensorUtil.getSensorRequest(sensorType);
    }
    
    // Doesn't need to be async
    public String getTypeConstantName(int type) {
    	return SensorUtilJava.getTypeConstantName(type).toLowerCase();
    }

}
