package org.concord.sensor.applet;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.concord.framework.data.stream.DataListener;
import org.concord.framework.data.stream.DataStreamEvent;
import org.concord.framework.otrunk.OTControllerService;
import org.concord.framework.otrunk.view.OTControllerServiceFactory;
import org.concord.otrunk.applet.OTAppletViewer;
import org.concord.otrunk.datamodel.OTIDFactory;
import org.concord.sensor.nativelib.NativeVernierSensorDevice;
import org.concord.sensor.state.OTSensorDataProxy;
import org.concord.sensor.state.SensorDataProxy;

public class OTSensorApplet extends OTAppletViewer implements DataListener {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(OTSensorApplet.class.getName());
    private SensorDataProxy sensorProxy;
    private File nativeLibJar;
    private File nativeLib;

    private JPanel mainPanel;
    private JButton startButton;
    private JButton stopButton;

    @Override
    public void init() {
        try {
            nativeLibJar = File.createTempFile("vernier-goio-macosx-nar", ".jar");
            // Download vernier native lib to system
            downloadLibJar();
            unpackLibJar();
            // load the native lib
            renameLib();
            loadLib();
        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Native sensor lib url invalid!", e);
        } catch (FileNotFoundException e) {
            // we should never hit this...
            logger.log(Level.SEVERE, "Couldn't find native lib jar file to extract!", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Couldn't create temp files to hold native library!", e);
        }

        super.init();
    }

    private void renameLib() throws IOException {
        File origNativeLib = new File(nativeLibJar.getParentFile(), "libvernier_ccsd.jnilib");
//        nativeLib = origNativeLib;
        File tempNativeLib = File.createTempFile("libvernier_ccsd-", ".jnilib");
        System.out.println("Created temp file: " + tempNativeLib);
        origNativeLib.renameTo(tempNativeLib);
        nativeLib = tempNativeLib;
        System.setProperty(NativeVernierSensorDevice.VERNIER_NATIVE_LIB_LOADED, "true");
    }

    private void loadLib() {
        System.load(nativeLib.getAbsolutePath());
    }

    private void unpackLibJar() throws FileNotFoundException, IOException {
        File endDir = nativeLibJar.getParentFile();
        JarInputStream zis = new JarInputStream(new FileInputStream(nativeLibJar));
        JarEntry fileEntry = zis.getNextJarEntry();
        while (fileEntry != null) {
            String entryName = fileEntry.getName();
            int lastSlash = entryName.lastIndexOf(File.separatorChar);
            File file;
            if (lastSlash > -1) {
                File parent = new File(endDir, entryName.substring(0, lastSlash));
                parent.mkdirs();
                file = new File(parent, entryName.substring(lastSlash));
            } else {
                file = new File(endDir, entryName);
            }
            if (fileEntry.isDirectory()) {
                file.mkdirs();
            } else {
                // extract the file
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buf = new byte[1024];
                while (zis.available() > 0) {
                    int len = zis.read(buf, 0, buf.length);
                    if (len <= 0) {
                        break;
                    }
                    fos.write(buf, 0, len);
                }
                fos.flush();
                fos.close();
            }
            zis.closeEntry();
            fileEntry = zis.getNextJarEntry();
        }
        nativeLibJar.deleteOnExit();
    }

    private void downloadLibJar() throws MalformedURLException, IOException {
        URL nativeLibUrl = new URL(getCodeBase(), "vernier-goio-macosx-nar.jar");
        InputStream stream = nativeLibUrl.openStream();
        byte[] buffer = new byte[1024];
        FileOutputStream fos = new FileOutputStream(nativeLibJar);
        downloadStream(buffer, stream, fos);
    }

    private void downloadStream(byte[] buffer, InputStream inputStream, OutputStream fileOutputStream)
            throws IOException {
        int len;
        while ((len = inputStream.read(buffer)) >= 0) {
            fileOutputStream.write(buffer, 0, len);
        }

        inputStream.close();
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    protected void initDataProxy() throws Exception {
        OTSensorDataProxy otSensorProxy = (OTSensorDataProxy) getOTrunk().getRoot();
        OTControllerService controllerService = otSensorProxy.getOTObjectService().createControllerService();
        sensorProxy = (SensorDataProxy) controllerService.getRealObject(otSensorProxy);

        sensorProxy.addDataListener(this);
    }

    public void startCollecting() {
        sensorProxy.start();
    }

    public void stopCollecting() {
        sensorProxy.stop();
    }

    public void dataReceived(DataStreamEvent dataEvent) {
        printEvent(dataEvent, "Data received");
    }

    public void dataStreamEvent(DataStreamEvent dataEvent) {
        printEvent(dataEvent, "Data stream event");
    }

    private void printEvent(DataStreamEvent event, String prefix) {
        System.out.println(prefix);
        System.out.print("  type(" + typeToString(event.getType()) + ")");
        System.out.println(", numSamples(" + event.getNumSamples() + ")");
        if (event.getData() != null && event.getNumSamples() > 0) {
            System.out.println("    samples("
                    + sampleStr(Arrays.copyOfRange(event.getData(), 0, event.getNumSamples())) + ")");
        }
    }

    private String typeToString(int type) {
        switch (type) {
        case DataStreamEvent.DATA_DESC_CHANGED:
            return "DataDesc Changed";
        case DataStreamEvent.DATA_DESC_ERROR:
            return "DataDesc Error";
        case DataStreamEvent.DATA_DESC_RESET:
            return "DataDesc Reset";
        case DataStreamEvent.DATA_RECEIVED:
            return "Data Received";
        case DataStreamEvent.DATA_REPLACED:
            return "Data Replaced";

        default:
            return "Unknown type: " + type;
        }
    }

    private String sampleStr(float[] samples) {
        StringBuffer buf = new StringBuffer();
        for (float f : samples) {
            buf.append(f);
            buf.append(", ");
        }

        buf.delete(buf.length() - 2, buf.length());
        return buf.toString();
    }
    
    @Override
    public void start() {
        super.start();
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
        nativeLibJar = null;
        nativeLib = null;
        sensorProxy = null;
        super.destroy();
    }

}
