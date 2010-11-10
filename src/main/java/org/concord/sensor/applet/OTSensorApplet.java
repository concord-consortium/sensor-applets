package org.concord.sensor.applet;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import netscape.javascript.JSObject;

import org.concord.framework.data.stream.DataListener;
import org.concord.framework.otrunk.OTControllerService;
import org.concord.otrunk.applet.OTAppletViewer;
import org.concord.sensor.state.OTSensorDataProxy;
import org.concord.sensor.state.SensorDataProxy;

/**
 * This applet is an extension of the OTAppletViewer class. It is intended to be used specifically for
 * reading sensor data and passing that off to a listener. Listeners can be created in Java or Javascript.
 * 
 * Basic usage:
 * 1) Create a javascript listener that implements the org.concord.sensor.applet.JavascriptDataListener interface
 * 2a) set the applet param "listenerPath" to the string object name for your listener
 *   '<param name="listenerPath" value="jsListener" />'
 *   OR
 * 2b) create the listener bridge through the applet and register it in the applet
 *   var bridge = applet.createJavascriptBridge("jsListener");
 *   applet.addDataListener(bridge);
 *   
 * @author aunger
 *
 */
public class OTSensorApplet extends OTAppletViewer {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(OTSensorApplet.class.getName());
    private SensorDataProxy sensorProxy;

    private JPanel mainPanel;
    private JButton startButton;
    private JButton stopButton;
    
    private boolean sensorSetupSucceeded = false;
    
    private DefaultDataListener defaultListener = new DefaultDataListener();
    private JavascriptDataBridge jsListener;
    private Timer timer;
    
    public enum State {
        READY, RUNNING, STOPPED, UNKNOWN
    }

    @Override
    public void init() {
        try {
            String nativeJarPath = new String("/jnlp/org/concord/sensor/vernier/vernier-goio/" +  getNativeJarName());
            // /jnlp/org/concord/sensor/vernier/vernier-goio/vernier-goio-<os-and-arch>-nar.jar
            // URL nativeJarUrl = new URL("http://jnlp.concord.org/dev/org/concord/sensor/vernier/vernier-goio/" + getNativeJarName());
            NativeLibraryHandler handler = new NativeLibraryHandler(nativeJarPath);

            handler.initializeLibrary();
        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Native sensor lib url invalid!", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Couldn't create temp files to hold native library!", e);
        }

        super.init();
    }
    
    @Override
    protected void loadState() {
        super.loadState();
//        try {
//            initDataProxy();
//            sensorSetupSucceeded = true;
//            notifyListenerOfStartup();
//            initStateListening();
//        } catch (Exception e) {
//            logger.log(Level.SEVERE, "Failed to set up sensor proxy!", e);
//            sensorSetupSucceeded = false;
//        }
    }
    
    public void initSensorInterface(String listenerPath) {
        try {
        	System.out.println("calling: initDataProxy()");
            initDataProxy(listenerPath);
            sensorSetupSucceeded = true;
        	System.out.println("calling: notifyListenerOfStartup()");
            notifyListenerOfStartup();
        	System.out.println("calling: initStateListening()");
            initStateListening();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to set up sensor proxy!", e);
            sensorSetupSucceeded = false;
        }
    }
 

    private void notifyListenerOfStartup() {
        if (jsListener != null) {
            jsListener.sensorsReady();
        }
    }
    
    private void initStateListening() {
        final String statePath = getParameter("sensorStatePath");
        final JSObject window = JSObject.getWindow(this);
    	if (window == null) {
    		System.out.println("*** initStateListening: JSObject.getWindow request to browser returning null  ...");
    	}

        // if statePath is defined, and it's an object
        if (statePath != null && window.eval(statePath) != null) {
            // poll for changes in state
            TimerTask stateChangePoll = new TimerTask() {
                private State currentState = State.READY;
                
                @Override
                public void run() {
                    State newState = getState((String)window.eval(statePath));
                    if (currentState != newState) {
                        switch (newState) {
                        case READY:
                            // reset sensor
                            stopCollecting();
                            break;
                        case RUNNING:
                            // start collecting
                            startCollecting();
                            break;
                        case STOPPED:
                            // stop collecting
                            stopCollecting();
                            break;
                        case UNKNOWN:
                            // how do we handle this? do nothing for now
                            break;
                        default:
                            // do nothing
                            break;
                        }
                        currentState = newState;
                    }
                }
                
                private State getState(String strState) {
                    if ("ready".equals(strState)) {
                        return State.READY;
                    } else if ("running".equals(strState)) {
                        return State.RUNNING;
                    } else if ("stopped".equals(strState)) {
                        return State.STOPPED;
                    }
                    return State.UNKNOWN;
                }
            };
            
            timer = new Timer();
            timer.schedule(stateChangePoll, 500, 200);
        }
    }

    private String getNativeJarName() {
        if(System.getProperty("os.name").startsWith("Windows")) {
            return "vernier-goio-win32-nar.jar";
        }else if(System.getProperty("os.name").startsWith("Mac")) {
        	if(System.getProperty("os.arch").startsWith("ppc")) {
        		return "vernier-goio-macosx-ppc-nar.jar";
        	}else if(System.getProperty("os.arch").startsWith("i386")) {
        		return "vernier-goio-macosx-i386-nar.jar";
        	}else if(System.getProperty("os.arch").startsWith("x86_64")) {
        		return "vernier-goio-macosx-x86_64-nar.jar";
        	}      
        }
        return "vernier-goio-macosx-nar.jar?version-id=1.4.0";
    }

    protected void initDataProxy(String listenerPath) throws Exception {
        OTSensorDataProxy otSensorProxy = (OTSensorDataProxy) getOTrunk().getRoot();
        OTControllerService controllerService = otSensorProxy.getOTObjectService().createControllerService();
        sensorProxy = (SensorDataProxy) controllerService.getRealObject(otSensorProxy);

        if (listenerPath != null) {
            jsListener = createJavascriptBridge(listenerPath);
            addDataListener(jsListener);
        } else {
            addDataListener(defaultListener);
        }
    }

    public void startCollecting() {
    	logger.log(Level.INFO, "starting collection: ");
    	try {
            AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                public Boolean run() {
                    sensorProxy.start();
                    return Boolean.TRUE;
                }
            });    		
    	} catch(Exception e) {
            logger.log(Level.SEVERE, "error starting collection: ", e);
        }
    }

    public void stopCollecting() {
        AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                sensorProxy.stop();
                return Boolean.TRUE;
            }
        });
    }

    @Override
    public void setupView() {
        mainPanel = new JPanel(new FlowLayout());
        if (getParameter("hideButtons") == null || getParameter("hideButtons").matches("(false|no)")) {
            if (sensorSetupSucceeded) {
                startButton = new JButton("Start");
                stopButton = new JButton("Stop");
                mainPanel.add(startButton);
                mainPanel.add(stopButton);

                startButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        startCollecting();
                    }
                });
    
                stopButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        stopCollecting();
                    }
                });
            } else {
                mainPanel.add(new JLabel("Sensor setup failure."));
            }
        }
        getContentPane().add(mainPanel);  
        
        getContentPane().validate();
        getContentPane().repaint();
    }
    
    public boolean readyToCollectData() {
        return (isMasterLoaded() && sensorSetupSucceeded);
    }

    @Override
    public void destroy() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (sensorProxy != null) {
            sensorProxy.stop();
            sensorProxy.removeDataListener(defaultListener);
            sensorProxy = null;
        }
        super.destroy();
    }
    
    public void addDataListener(DataListener listener) {
        sensorProxy.addDataListener(listener);
    }
    
    public void removeDataListener(DataListener listener) {
        sensorProxy.removeDataListener(listener);
    }
    
    public JavascriptDataBridge createJavascriptBridge(String jsObjectPath) {
        return new JavascriptDataBridge(jsObjectPath, this);
    }
}
