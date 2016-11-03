package de.privatepublic.midiutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Prefs {

	
	private static final Logger LOG = LoggerFactory.getLogger(Prefs.class);
	
	private static final Preferences PREFS = Preferences.userNodeForPackage(DiMIDImi.class);
	public static final String MIDI_IN_DEVICES = "midiindev";
	public static final String MIDI_OUT_DEVICES = "midioutdev";
	public static final String MIDI_IN_CHANNEL = "midiinch";
	public static final String MIDI_OUT_CHANNEL = "midioutch";
	public static final String FILE_LOOP_LAST_USED_NAME = "lastfile";
	public static final String FILE_SESSION_LAST_USED_NAME = "lastsession";
	public static final String THEME = "theme";
	public static final String ANIMATE = "animate";
	public static final String RECENT_SESSION_LIST="recentsessions";
	public static final String RECENT_LOOP_LIST="recentloops";
	public static final String CONTROLLER_POS="ctrlpos";
	
	private static int MAX_RECENT_FILE_COUNT = 10;
	public static String LIST_ENTRY_EMPTY_MARKER = "(--empty--)";
	
	public static void put(String key, String val) {
		PREFS.put(key, val);
		flushPrefsSilently();
	}
	
	public static void put(String key, int val) {
		PREFS.putInt(key, val);
		flushPrefsSilently();
	}
	
	public static String get(String key, String def) {
		return PREFS.get(key, def);
	}
	
	public static int get(String key, int def) {
		return PREFS.getInt(key, def);
	}
	
	private static void flushPrefsSilently() {
		try {
			PREFS.flush();
		} catch (BackingStoreException e) {
			LOG.error("Couldn't write prefs:", e);
		}
	}
	
	public static List<String> getPrefIdentifierList(String key) {
		return new ArrayList<String>(Arrays.asList(PREFS.get(key, "").split(",")));
	}
	
	public static void putPrefIdentfierList(String key, List<? extends Identifiable> list) {
		StringBuilder sb = new StringBuilder();
		for (Identifiable id:list) {
			if (sb.length()>0) {
				sb.append(',');
			}
			sb.append(id.getIdentifier());
		}
		put(key, sb.toString());
	}
	
	public static void pushToList(String listKey, String value) {
		List<String> list = getList(listKey);
		if (list.contains(value)) {
			list.remove(value);
		}
		list.add(0, value);
		for (int i=0;i<MAX_RECENT_FILE_COUNT;i++) {
			if (i>=list.size() || list.get(i)==null) {
				put(listKey+i, LIST_ENTRY_EMPTY_MARKER);
			}
			else {
				put(listKey+i, list.get(i));
			}
		}
	}
	
	public static List<String> getList(String listKey) {
		String[] result = new String[MAX_RECENT_FILE_COUNT];
		for (int i=0;i<MAX_RECENT_FILE_COUNT;i++) {
			result[i] = get(listKey+i, null);
			if (result[i]==null) {
				result[i] = LIST_ENTRY_EMPTY_MARKER;
			}
		}
		return new ArrayList<String>(Arrays.asList(result));
	}
	
	
	
	public static interface Identifiable {
		public String getIdentifier();
	} 
}
