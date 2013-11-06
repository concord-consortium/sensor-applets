package org.concord.sensor.applet;

import java.awt.Button;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Logger;

import javax.swing.JApplet;

public class HighLevelTestApplet extends JApplet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(HighLevelTestApplet.class.getName());
	
	@Override
	public void start() {
		super.start();
		
		Button readButton = new Button("Read");
		readButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				readData();
			}
		});
		this.add(readButton);
	}
	
	public void readData() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				AccessController.doPrivileged(new PrivilegedAction<Void>() {
					public Void run() {
						doRead();
						return null;
					}
				});
			}
		});
	}
	
	private void doRead() {
		SensorUtil util = new SensorUtil(this, "golink");
		JavascriptDataBridge jsBridge = new JavascriptDataBridge("window", this);
		try {
			float[] data = util.readSingleValue(jsBridge, true);
			logger.info("got data: " + jsBridge.asArgs(data));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
