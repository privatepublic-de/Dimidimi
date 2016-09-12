package de.privatepublic.midiutils.events;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.privatepublic.midiutils.NoteRun;

public class Event {

	private static final List<ClockReceiver> clockReceivers = new CopyOnWriteArrayList<ClockReceiver>();
	private static final List<NoteReceiver> noteReceivers = new CopyOnWriteArrayList<NoteReceiver>();
	private static final List<StorageReceiver> storageReceivers = new CopyOnWriteArrayList<StorageReceiver>();
	private static final List<SettingsUpdateReceiver> settingsReceivers = new CopyOnWriteArrayList<SettingsUpdateReceiver>();

	public static void registerClockReceiver(ClockReceiver cr) {
		if (!clockReceivers.contains(cr)) {
			clockReceivers.add(cr);
		}
	}

	public static void registerNoteReceiver(NoteReceiver nr) {
		if (!noteReceivers.contains(nr)) {
			noteReceivers.add(nr);
		}
	}
	
	public static void registerStorageReceiver(StorageReceiver sr) {
		if (!storageReceivers.contains(sr)) {
			storageReceivers.add(sr);
		}
	}
	
	public static void registerSettingsUpdateReceiver(SettingsUpdateReceiver sr) {
		if (!settingsReceivers.contains(sr)) {
			settingsReceivers.add(sr);
		}
	}

	public static void noteOn(int noteNumber, int velocity, int pos) {
		for (NoteReceiver nr : noteReceivers) {
			nr.receiveNoteOn(noteNumber, velocity, pos);
		}
	}

	public static void noteOff(int noteNumber, int pos) {
		for (NoteReceiver nr : noteReceivers) {
			nr.receiveNoteOff(noteNumber, pos);
		}
	}

	public static void sendClock(int pos) {
		for (ClockReceiver cr : clockReceivers) {
			cr.receiveClock(pos);
		}
	}
	
	public static void sendActive(boolean active, int pos) {
		for (ClockReceiver cr : clockReceivers) {
			cr.receiveActive(active, pos);
		}
	}

	private static final List<LoopUpdateReceiver> loopUpdateReceivers = new CopyOnWriteArrayList<LoopUpdateReceiver>();

	public static void registerLoopUpdateReceiver(LoopUpdateReceiver lr) {
		if (!loopUpdateReceivers.contains(lr)) {
			loopUpdateReceivers.add(lr);
		}
	}

	public static void sendLoopUpdate(List<NoteRun> cycleList) {
		for (LoopUpdateReceiver lr : loopUpdateReceivers) {
			lr.loopUpdated(cycleList);
		}
	}
	
	public static void sendLoopDisplayRefresh() {
		for (LoopUpdateReceiver lr : loopUpdateReceivers) {
			lr.refreshLoopDisplay();;
		}
	}

	private static final List<ClearReceiver> clearReceivers = new CopyOnWriteArrayList<ClearReceiver>();

	public static void registerClearReceiver(ClearReceiver cr) {
		if (!clearReceivers.contains(cr)) {
			clearReceivers.add(cr);
		}
	}

	public static void sendClear() {
		for (ClearReceiver cr : clearReceivers) {
			cr.clearPattern();
		}
	}
	
	public static void sendSave(File file) throws JsonGenerationException, JsonMappingException, IOException {
		for (StorageReceiver sr : storageReceivers) {
			sr.saveRequest(file);
		}
	}
	
	public static void sendLoad(File file) throws JsonParseException, JsonMappingException, IOException {
		for (StorageReceiver sr : storageReceivers) {
			sr.loadRequest(file);
		}
	}
	

	public static void sendSettingsUpdate() {
		for (SettingsUpdateReceiver sr : settingsReceivers) {
			sr.settingsUpdated();
		}
	}
	
}
