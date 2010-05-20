package org.concord.sensor.applet;

import org.concord.framework.data.stream.DataListener;
import org.concord.framework.data.stream.DataStreamEvent;

public class DefaultDataListener implements DataListener {

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
            float[] arr = new float[event.getNumSamples()];
            System.arraycopy(event.getData(), 0, arr, 0, event.getNumSamples());
            System.out.println("    samples("
                    + sampleStr(arr) + ")");
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

}
