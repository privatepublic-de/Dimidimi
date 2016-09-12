package de.privatepublic.midiutils;

import java.awt.EventQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.privatepublic.midiutils.events.Event;
import de.privatepublic.midiutils.ui.UIWindow;



public class DiMIDImi {

	private static final Logger LOG = LoggerFactory.getLogger(DiMIDImi.class);
	
	
	public static void main(String[] args) {
		LOG.info("DiMIDImi Looper starting ...");
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIWindow window = new UIWindow();
					Event.registerClockReceiver(window);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		MidiHandler.instance();
	}
	
}
