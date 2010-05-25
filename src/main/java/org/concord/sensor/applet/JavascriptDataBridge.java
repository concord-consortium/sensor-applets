package org.concord.sensor.applet;

import java.applet.Applet;

import netscape.javascript.JSException;

import org.concord.framework.data.stream.DataListener;
import org.concord.framework.data.stream.DataStreamEvent;

import netscape.javascript.JSObject;

public class JavascriptDataBridge implements DataListener {
    private JSObject window;
    private String handlerPath;
    
    public JavascriptDataBridge(String javascriptObjectPath, Applet applet) {
        window = JSObject.getWindow(applet);
        handlerPath = javascriptObjectPath;
    }
    
    public void dataReceived(DataStreamEvent dataEvent) {
        handleEvent(dataEvent, "dataReceived");
    }
    
    public void dataStreamEvent(DataStreamEvent dataEvent) {
        handleEvent(dataEvent, "dataStreamEvent");
    }
    
    // We're using JSObject.eval() instead of using JSObject.call() because Firefox has problems with call()
    private void handleEvent(DataStreamEvent event, String method) {
        String evalString = getJsEventCall(event, method);
        try {
            window.eval(evalString);
        } catch (JSException e) {
            System.err.println("Javascript error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getJsEventCall(DataStreamEvent dataEvent, String method) {
        StringBuffer buf = new StringBuffer();
        buf.append(handlerPath);
        buf.append(".");
        buf.append(method);
        buf.append("(");
        buf.append(dataEvent.getType());
        buf.append(", " + dataEvent.getNumSamples());
        buf.append(", " + arrayAsString(dataEvent.getData(), dataEvent.getNumSamples()));
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

}
