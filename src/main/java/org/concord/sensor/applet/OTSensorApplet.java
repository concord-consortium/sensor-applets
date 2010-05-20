package org.concord.sensor.applet;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.concord.framework.otrunk.OTControllerService;
import org.concord.otrunk.applet.OTAppletViewer;
import org.concord.sensor.state.OTSensorDataProxy;
import org.concord.sensor.state.SensorDataProxy;

public class OTSensorApplet extends OTAppletViewer {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(OTSensorApplet.class.getName());
    private SensorDataProxy sensorProxy;

    private JPanel mainPanel;
    private JButton startButton;
    private JButton stopButton;

    @Override
    public void init() {
        try {
            // FIXME We need to handle win32 as well
            URL nativeJarUrl = new URL(getCodeBase(), "vernier-goio-macosx-nar.jar");
            NativeLibraryHandler handler = new NativeLibraryHandler(nativeJarUrl);
            handler.initializeLibrary();
        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Native sensor lib url invalid!", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Couldn't create temp files to hold native library!", e);
        }

        super.init();
    }



    protected void initDataProxy() throws Exception {
        OTSensorDataProxy otSensorProxy = (OTSensorDataProxy) getOTrunk().getRoot();
        OTControllerService controllerService = otSensorProxy.getOTObjectService().createControllerService();
        sensorProxy = (SensorDataProxy) controllerService.getRealObject(otSensorProxy);

        sensorProxy.addDataListener(new DefaultDataListener());
    }

    public void startCollecting() {
        sensorProxy.start();
    }

    public void stopCollecting() {
        sensorProxy.stop();
    }

    @Override
    public void setupView() {
        try {
            System.out.println("Starting to set up view.");
            initDataProxy();
            System.out.println("Finished init'ing sensor proxy");
            mainPanel = new JPanel(new FlowLayout());
            startButton = new JButton("Start");
            stopButton = new JButton("Stop");
            mainPanel.add(startButton);
            mainPanel.add(stopButton);

            getContentPane().add(mainPanel);

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
            System.out.println("Finished view setup.");
        } catch (Exception e1) {
            logger.log(Level.SEVERE, "Failed to set up sensor proxy!", e1);
            getContentPane().add(new JLabel("Setup failure."));
        }
        
        getContentPane().validate();
        getContentPane().repaint();
    }

    @Override
    public void destroy() {
        sensorProxy = null;
        super.destroy();
    }

}
