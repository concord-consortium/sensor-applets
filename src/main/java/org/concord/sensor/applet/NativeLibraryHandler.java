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
    private URL origJarUrl;

    public NativeLibraryHandler(URL nativeLibJarUrl) throws IOException {
        super();
        this.origJarUrl = nativeLibJarUrl;
    }
    
    public void initializeLibrary() throws MalformedURLException, IOException {
        // Download vernier native lib to system
        File nativeLibJar = downloadLibJar();
        File nativeLibDir = unpackLibJar(nativeLibJar);
        // load the native lib
        if (System.getProperty("os.name").startsWith("Windows")) {
            // don't rename the GoIO_DLL, as apparently vernier_ccsd needs to find it by name
            // it doesn't seem to have the same problem as the mac library
            File lib = new File(nativeLibDir, "GoIO_DLL.dll");
            File lib2 = renameLib("vernier_ccsd", ".dll", nativeLibDir);
            loadLib(lib);
            loadLib(lib2);
        } else if (System.getProperty("os.name").startsWith("Mac")) {
        	File lib;
        	if(System.getProperty("os.arch").startsWith("ppc")) {
        		lib = renameLib("libvernier_ccsd_ppc7400", ".jnilib", nativeLibDir);
        		loadLib(lib);
        	}else if(System.getProperty("os.arch").startsWith("i386")) {
        		lib = renameLib("libvernier_ccsd_i386", ".jnilib", nativeLibDir);
        		loadLib(lib);
        	}else  {
        		lib = renameLib("libvernier_ccsd_x86_64", ".jnilib", nativeLibDir);
        		loadLib(lib);
        	}
        } else {
            throw new RuntimeException("Unsupported Operating System: " + System.getProperty("os.name"));
        }
       System.setProperty(NativeVernierSensorDevice.VERNIER_NATIVE_LIB_LOADED, "true");
    }
    
    // On Mac, at least, only one instance of a native lib can be loaded per JVM. By renaming the library,
    // we can get around this restriction.
    private File renameLib(String prefix, String suffix, File nativeLibDir) throws IOException {
        File origNativeLib = new File(nativeLibDir, prefix + suffix);
        File tempNativeLib = File.createTempFile(prefix + "-", suffix, nativeLibDir);
        System.out.println("Created temp file: " + tempNativeLib);
        
        if (tempNativeLib.delete() && origNativeLib.renameTo(tempNativeLib)) {
            return tempNativeLib;
        } else {
            throw new RuntimeException("Unable to rename " + origNativeLib + " to " + tempNativeLib);
        }
    }

    private void loadLib(File lib) {
        System.out.println("loading: " + lib.getAbsolutePath());
        System.load(lib.getAbsolutePath());
    }

    private File unpackLibJar(File nativeLibJar) throws FileNotFoundException, IOException {
        File endDir = nativeLibJar.getParentFile();
        JarInputStream zis = new JarInputStream(new FileInputStream(nativeLibJar));
        JarEntry fileEntry = zis.getNextJarEntry();
        while (fileEntry != null) {
            String entryName = fileEntry.getName();
            File file = new File(endDir, entryName);
            
            if (fileEntry.isDirectory()) {
                file.mkdirs();
            } else {
                // make sure the parent directories exist
                File parent = file.getParentFile();
                if (parent != null && ! parent.exists()) {
                    parent.mkdirs();
                }
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
        return endDir;
    }

    private File downloadLibJar() throws MalformedURLException, IOException {
        URL nativeLibUrl = origJarUrl;
        // put it in it's own directory, since on windows we can't guarantee the temp dir will be unique per jvm
        File outDir = File.createTempFile("vsl", "").getAbsoluteFile();
        // createTempFile creates the file for us, so delete it and make the directory, throw an exception if either return false
        if (! outDir.delete() || ! outDir.mkdirs()) {
            throw new RuntimeException("failed to create temp dir: " + outDir);
        }
        File outFile = File.createTempFile("vernier-goio-nar", ".jar", outDir);
        InputStream stream = nativeLibUrl.openStream();
        byte[] buffer = new byte[1024];
        FileOutputStream fos = new FileOutputStream(outFile);
        downloadStream(buffer, stream, fos);
        return outFile;
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
