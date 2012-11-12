package org.concord.sensor.applet;

import java.applet.Applet;

import netscape.javascript.JSException;

import netscape.javascript.JSObject;

public class JavascriptDataBridge {
    private JSObject window;
    private String handlerPath;
    
    public JavascriptDataBridge(String javascriptObjectPath, Applet applet) {
        window = JSObject.getWindow(applet);
        handlerPath = javascriptObjectPath;
    }
    
    // We're using JSObject.eval() instead of using JSObject.call() because Firefox has problems with call()
    public void handleData(int numSamples, float[] data) {
        String evalString = getJsEventCall(numSamples, data);
        try {
            window.eval(evalString);
        } catch (JSException e) {
            System.err.println("Javascript error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getJsEventCall(int numSamples, float[] data) {
        StringBuffer buf = new StringBuffer();
        buf.append(handlerPath);
        buf.append(".dataReceived(");
        buf.append(1000); // 1000 is DataStreamEvent.DATA_RECEIVED
        buf.append(", " + numSamples);
        buf.append(", " + arrayAsString(data, numSamples));
        buf.append(");");
        return buf.toString();
    }
    
    private String arrayAsString(float[] arr, int numSamples) {
        if (arr == null) {
            return "null";
        }
        StringBuffer buf = new StringBuffer();
        buf.append("[");
        for (int i = 0; i < numSamples; i++) {
            buf.append(arr[i]);
            if (i != numSamples-1) {
                buf.append(",");
            }
        }
        buf.append("]");
        return buf.toString();
    }

    public void sensorsReady() {
        window.eval(handlerPath + ".sensorsReady();");
    }

}
