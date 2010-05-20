package org.concord.sensor.applet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.concord.sensor.nativelib.NativeVernierSensorDevice;

public class NativeLibraryHandler {
    
    private File nativeLibJar;
    private File nativeLib;
    private URL origJarUrl;

    public NativeLibraryHandler(URL nativeLibJarUrl) throws IOException {
        super();
        this.nativeLibJar = File.createTempFile("vernier-goio-nar", ".jar");
        this.origJarUrl = nativeLibJarUrl;
    }
    
    public void initializeLibrary() throws MalformedURLException, IOException {
        // Download vernier native lib to system
        downloadLibJar();
        unpackLibJar();
        // load the native lib
        renameLib();
        loadLib();
    }
    
    private void renameLib() throws IOException {
        // FIXME We need to handle Win32 as well
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
        URL nativeLibUrl = origJarUrl;
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

}
