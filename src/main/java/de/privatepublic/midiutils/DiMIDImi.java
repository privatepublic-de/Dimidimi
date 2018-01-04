package de.privatepublic.midiutils;

import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
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
	
	public static void main(String[] args) {
		LOG.info("DiMIDImi Looper starting ...");
		
		// create controller window
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					controllerWindow = new ControllerWindow();
					createSession();
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
	
	public static void updateSettingsOnAllSessions() {
		LOG.info("Updating settings for all sessions");
		for (Loop session: LOOPS) {
			session.emitSettingsUpdated();
		}
	}
	
	public static void updateLoopsOnAllSessions() {
		LOG.info("Updating loops for all sessions");
		for (Loop session: LOOPS) {
			session.emitLoopUpdated();
		}
	}
	
	public static Loop createSession() {
		Loop session = new Loop();
		LOOPS.add(session);
		session.registerAsReceiver(controllerWindow);
		DiMIDImi.updateSettingsOnAllSessions();
		LOG.info("Created new session {}", session.hashCode());
		return session;
	}
	
	public static Loop createSession(StorageContainer data, String sessionName) {
		Loop session = new Loop(data, sessionName);
		LOOPS.add(session);
		session.registerAsReceiver(controllerWindow);
		DiMIDImi.updateSettingsOnAllSessions();
		LOG.info("Created new session {}", session.hashCode());
		return session;
	}
	
	public static void removeSession(Loop session) {
		if (LOOPS.contains(session)) {
			LOOPS.remove(session);
			session.destroy();
			DiMIDImi.updateSettingsOnAllSessions();
			LOG.info("Removed session {}", session.hashCode());
			if (LOOPS.size()==0) {
				System.exit(0);
			}
		}
	}
	
	public static void removeAllSessions() {
		LOG.info("Removing all sessions");
		for (Loop session: LOOPS) {
			session.getWindow().closeWindow();
		}
	}
	
	public static void saveSession(File file) throws JsonGenerationException, JsonMappingException, IOException {
		List<StorageContainer> dataList = new ArrayList<StorageContainer>();
		String sessionName = FilenameUtils.getBaseName(file.getName());
		for (Loop session: LOOPS) {
			StorageContainer data = new StorageContainer(session);
			dataList.add(data);
			session.setSessionName(sessionName);
		}
		mapper.writeValue(file, dataList);
		LOG.info("Saved session {}", file.getPath());
	}
	
	
	public static void loadSession(File file) throws JsonParseException, JsonMappingException, IOException {
		List<Loop> sessionsToClose = new ArrayList<Loop>(LOOPS);
		List<StorageContainer> list = Arrays.asList(mapper.readValue(file, StorageContainer[].class));
		String sessionName = FilenameUtils.getBaseName(file.getName());
		for (StorageContainer data:list) {
			createSession(data, sessionName);
		}
		for (Loop session:sessionsToClose) {
			session.getWindow().closeWindow();
		}
		LOG.info("Loaded session {}", file.getPath());
	}
	
	public static List<Loop> getSessions() {
		return LOOPS;
	}

	public static void arrangeSessionWindows() {
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
		for (Loop session: LOOPS) {
			Rectangle pos = new Rectangle(rect.x+20*rowiteration+col*wwidth, rect.y+20*rowiteration+row*wheight, wwidth-20, wheight-20);
			session.getWindow().setScreenPosition(pos);
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
