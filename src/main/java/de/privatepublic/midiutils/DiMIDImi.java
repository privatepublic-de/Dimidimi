package de.privatepublic.midiutils;

import java.awt.EventQueue;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.privatepublic.midiutils.ui.ControllerWindow;



public class DiMIDImi {

	private static final Logger LOG = LoggerFactory.getLogger(DiMIDImi.class);
	
	private static ControllerWindow CONTROLLER_WINDOW;
	
	public static boolean DISABLE_SPACEBAR_TOGGLE = false;
	
	public static void main(String[] args) {
		LOG.info("DiMIDImi Looper starting ...");
		
		// create controller window and one empty loop
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					KeyboardFocusManager.getCurrentKeyboardFocusManager()
					  .addKeyEventDispatcher(new KeyEventDispatcher() {
					      @Override
					      public boolean dispatchKeyEvent(KeyEvent e) {
						    	  if (!DISABLE_SPACEBAR_TOGGLE && ' '==e.getKeyChar() && e.getID()==KeyEvent.KEY_PRESSED) {
						    		  MidiHandler.instance().toggleInternalClock();
						    		  return true;
						    	  }
						    	  return false;
					      }
					});
					CONTROLLER_WINDOW = new ControllerWindow();
					Loop.createLoop();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		    	LOG.info("Shutting down ...");
		    	if (CONTROLLER_WINDOW!=null) {
		    		Rectangle pos = CONTROLLER_WINDOW.getBounds();
		    		boolean visible = CONTROLLER_WINDOW.isVisible();
		    		boolean topmost = CONTROLLER_WINDOW.isAlwaysOnTop();
		    		Prefs.put(Prefs.CONTROLLER_POS, pos.x+","+pos.y+","+pos.width+","+pos.height+","+topmost+","+visible);
		    	}
		    }
		 });
	}
	
	public static ControllerWindow getControllerWindow() {
		return CONTROLLER_WINDOW;
	}
	
}
