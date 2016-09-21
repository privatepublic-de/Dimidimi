package de.privatepublic.midiutils;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class DiMIDImi {

	private static final Logger LOG = LoggerFactory.getLogger(DiMIDImi.class);
	
	private static final List<Session> SESSIONS = new ArrayList<Session>();
	
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
		Session session = new Session();
		SESSIONS.add(session);
		LOG.info("Created new session {}", session.hashCode());
		return session;
	}
	
	public static void removeSession(Session session) {
		SESSIONS.remove(session);
		LOG.info("Removed session {}", session.hashCode());
		if (SESSIONS.size()==0) {
			System.exit(0);
		}
	}
	
}
