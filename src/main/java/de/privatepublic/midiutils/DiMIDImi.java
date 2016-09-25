package de.privatepublic.midiutils;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;



public class DiMIDImi {

	private static final Logger LOG = LoggerFactory.getLogger(DiMIDImi.class);
	
	private static final List<Session> SESSIONS = new CopyOnWriteArrayList<Session>();
	
	public static void main(String[] args) {
		LOG.info("DiMIDImi Looper starting ...");
		createSession();
	}
	
	public static void updateSettingsOnAllSessions() {
		LOG.info("Updating sessions");
		for (Session session: SESSIONS) {
			session.emitSettingsUpdated();
		}
	}
	
	public static Session createSession() {
		Session session = new Session(SESSIONS.size()>0?SESSIONS.get(0).getMidiHandler().getPos():0);
		SESSIONS.add(session);
		LOG.info("Created new session {}", session.hashCode());
		return session;
	}
	
	public static Session createSession(StorageContainer data) {
		Session session = new Session(data);
		SESSIONS.add(session);
		LOG.info("Created new session {}", session.hashCode());
		return session;
	}
	
	public static void removeSession(Session session) {
		SESSIONS.remove(session);
		session.destroy();
		LOG.info("Removed session {}", session.hashCode());
		if (SESSIONS.size()==0) {
			System.exit(0);
		}
	}
	
	public static void removeAllSessions() {
		LOG.info("Removing all sessions");
		for (Session session: SESSIONS) {
			session.getWindow().closeWindow();
		}
	}
	
	public static void saveSession(File file) throws JsonGenerationException, JsonMappingException, IOException {
		List<StorageContainer> dataList = new ArrayList<StorageContainer>();
		for (Session session: SESSIONS) {
			StorageContainer data = new StorageContainer(session);
			dataList.add(data);
		}
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(file, dataList);
		LOG.info("Saved session {}", file.getPath());
	}
	
	
	public static void loadSession(File file) throws JsonParseException, JsonMappingException, IOException {
		List<Session> sessionsToClose = new ArrayList<Session>(SESSIONS);
		ObjectMapper mapper = new ObjectMapper();
		List<StorageContainer> list = mapper.readValue(file, mapper.getTypeFactory().constructCollectionType(List.class, StorageContainer.class));
		for (StorageContainer data:list) {
			createSession(data);
		}
		for (Session session:sessionsToClose) {
			session.getWindow().closeWindow();
		}
		LOG.info("Loaded session {}", file.getPath());
	}

	public static void arrangeSessionWindows() {
		Rectangle rect = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int width = rect.width;
		int height = rect.height;
		
		LOG.info("Arranging windows in {}x{}", width, height);		
		int minwidth = 740;
		int minheight = 300; // TODO constants for uiwindow, too
		int maxcols = width / minwidth;
		int maxrows = height / minheight;
		int numcols = maxcols;
		int numrows = maxrows;
		int number = SESSIONS.size();
		switch(number) {
		case 1:
			// maximize single window
			numcols = 1;
			numrows = 1;
			break;
		case 2:
			numcols = Math.min(2, maxcols);
			numrows = 1;
			break;
		case 3:
		case 4:
			numcols = Math.min(2, maxcols);
			numrows = Math.min(2, maxrows);
			break;
		case 5:
		case 6:
			numcols = Math.min(3, maxcols);
			numrows = Math.min(2, maxrows);
			break;
		}
		int wwidth = width/numcols;
		int wheight = height/numrows;
		int row = 0;
		int col = 0;
		int rowiteration = 0;
		for (Session session: SESSIONS) {
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
	
}
