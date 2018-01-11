package de.privatepublic.midiutils;

import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.privatepublic.midiutils.ui.ControllerWindow;
import de.privatepublic.midiutils.ui.UIWindow;



public class DiMIDImi {

	private static final Logger LOG = LoggerFactory.getLogger(DiMIDImi.class);
	
	private static final List<Loop> LOOPS = new CopyOnWriteArrayList<Loop>();
	
	private static ControllerWindow controllerWindow;
	
	public static boolean DISABLE_SPACEBAR_TOGGLE = false;
	
	public static void main(String[] args) {
		LOG.info("DiMIDImi Looper starting ...");
		
		// create controller window
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
					controllerWindow = new ControllerWindow();
					createLoop();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
		    	LOG.info("Shutting down ...");
		    	if (controllerWindow!=null) {
		    		Rectangle pos = controllerWindow.getBounds();
		    		boolean visible = controllerWindow.isVisible();
		    		boolean topmost = controllerWindow.isAlwaysOnTop();
		    		Prefs.put(Prefs.CONTROLLER_POS, pos.x+","+pos.y+","+pos.width+","+pos.height+","+topmost+","+visible);
		    	}
		    }
		 });
	}
	
	public static void updateSettingsOnAllLoops() {
		LOG.info("Updating settings for all loops");
		for (Loop loop: LOOPS) {
			loop.emitSettingsUpdated();
		}
	}
	
	public static void updateNotesOnAllLoops() {
		LOG.info("Updating notes for all loops");
		for (Loop loop: LOOPS) {
			loop.emitLoopUpdated();
		}
	}
	
	
	public static void updateStateOnAllLoops() {
		LOG.info("Updating state for all loops");
		for (Loop loop: LOOPS) {
			loop.emitState();
		}
	}
	
	public static void focusLoops(Loop focusLoop) {
		for (Loop loop: LOOPS) {
			if (loop==focusLoop) {
				loop.emitFocus(focusLoop);
			}
		}
	}
	
	
	public static Loop createLoop() {
		Loop loop = new Loop();
		LOOPS.add(loop);
		loop.registerAsReceiver(controllerWindow);
		DiMIDImi.updateSettingsOnAllLoops();
		LOG.info("Created new loop {}", loop.hashCode());
		return loop;
	}
	
	public static Loop createLoop(StorageContainer data, String name) {
		Loop loop = new Loop(data, name);
		LOOPS.add(loop);
		loop.registerAsReceiver(controllerWindow);
		DiMIDImi.updateSettingsOnAllLoops();
		LOG.info("Created new loop {}", loop.hashCode());
		return loop;
	}
	
	public static void removeLoop(Loop loop) {
		if (LOOPS.contains(loop)) {
			LOOPS.remove(loop);
			loop.destroy();
			DiMIDImi.updateSettingsOnAllLoops();
			LOG.info("Removed loop {}", loop.hashCode());
			if (LOOPS.size()==0) {
				System.exit(0);
			}
		}
	}
	
	public static void removeAllLoops() {
		LOG.info("Removing all loops");
		for (Loop loop: LOOPS) {
			loop.getWindow().closeWindow();
		}
	}
	
	public static void saveLoop(File file) throws JsonGenerationException, JsonMappingException, IOException {
		List<StorageContainer> dataList = new ArrayList<StorageContainer>();
		String name = FilenameUtils.getBaseName(file.getName());
		for (Loop loop: LOOPS) {
			StorageContainer data = new StorageContainer(loop);
			dataList.add(data);
			loop.setName(name);
		}
		mapper.writeValue(file, dataList);
		LOG.info("Saved loop {}", file.getPath());
	}
	
	
	public static void loadLoop(File file) throws JsonParseException, JsonMappingException, IOException {
		List<Loop> loopsToClose = new ArrayList<Loop>(LOOPS);
		List<StorageContainer> list = Arrays.asList(mapper.readValue(file, StorageContainer[].class));
		String name = FilenameUtils.getBaseName(file.getName());
		for (StorageContainer data:list) {
			createLoop(data, name);
		}
		for (Loop loop:loopsToClose) {
			loop.getWindow().closeWindow();
		}
		LOG.info("Loaded loop {}", file.getPath());
	}
	
	public static List<Loop> getLoops() {
		return LOOPS;
	}

	public static void arrangeLoopWindows() {
		Rectangle rect = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int width = rect.width;
		int height = rect.height;
		
		LOG.info("Arranging windows in {}x{}", width, height);		
		int minwidth = UIWindow.WINDOW_MAX_WIDTH-40;
		int minheight = UIWindow.WINDOW_MAX_HEIGHT;
		int maxcols = width / minwidth;
		int maxrows = height / minheight;
		int numcols = maxcols;
		int numrows = maxrows;
		int number = LOOPS.size();
		switch(number) {
		case 1:
			// maximize single window
			numcols = 1;
			numrows = 1;
			break;
		case 2:
			numcols = 1;
			numrows = Math.min(2, maxcols);
			break;
		case 3:
		case 4:
			numcols = Math.min(2, maxcols);
			numrows = Math.min(2, maxrows);
			break;
		case 5:
		case 6:
			numcols = Math.min(2, maxcols);
			numrows = Math.min(3, maxrows);
			break;
		}
		int wwidth = width/numcols;
		int wheight = height/numrows;
		int row = 0;
		int col = 0;
		int rowiteration = 0;
		for (Loop loop: LOOPS) {
			Rectangle pos = new Rectangle(rect.x+20*rowiteration+col*wwidth, rect.y+20*rowiteration+row*wheight, wwidth-20, wheight-20);
			loop.getWindow().setScreenPosition(pos);
			col = (col+1)%numcols;
			if (col==0) {
				row = (row+1)%numrows;
				if (row==0) {
					rowiteration++;
				}
			}
		}
	}
	
	public static ControllerWindow getControllerWindow() {
		return controllerWindow;
	}
	
	private static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	
}
