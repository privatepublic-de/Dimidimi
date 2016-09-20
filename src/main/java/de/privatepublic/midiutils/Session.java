package de.privatepublic.midiutils;

import java.awt.EventQueue;
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

import de.privatepublic.midiutils.events.DimidimiEventReceiver;
import de.privatepublic.midiutils.events.LoopUpdateReceiver;
import de.privatepublic.midiutils.events.PerformanceReceiver;
import de.privatepublic.midiutils.events.SettingsUpdateReceiver;
import de.privatepublic.midiutils.ui.UIWindow;

public class Session {

	private static final Logger LOG = LoggerFactory.getLogger(Session.class);

	public Session() {
		midiHandler = new MidiHandler(this);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIWindow window = new UIWindow(Session.this);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}


	public int getClockDivision() {
		return ppqdiv;
	}

	public void setClockDivision(int div) {
		ppqdiv = div;
	}

	public int getLengthQuarters() {
		return lengthQuarters;
	}


	public void setLengthQuarters(int lengthQuarters) {
		this.lengthQuarters = lengthQuarters;
		this.maxTicks = lengthQuarters*TICK_COUNT_BASE;
	}

	public int getMaxTicks() {
		return maxTicks;
	}

	// 0 - based
	public int getMidiChannelIn() {
		return midiChannelIn;
	}

	// 0 - based
	public void setMidiChannelIn(int midiChannelIn) {
		this.midiChannelIn = midiChannelIn;
		Prefs.put(Prefs.MIDI_IN_CHANNEL, midiChannelIn);
	}

	// 0 - based
	public int getMidiChannelOut() {
		return midiChannelOut;
	}

	// 0 - based
	public void setMidiChannelOut(int midiChannelOut) {
		int oldOut = midiChannelOut;
		this.midiChannelOut = midiChannelOut;
		getMidiHandler().sendAllNotesOff(oldOut);
		Prefs.put(Prefs.MIDI_OUT_CHANNEL, midiChannelOut);
	}


	public MidiHandler getMidiHandler() {
		return midiHandler;
	}


	public boolean isMidiInputOn() {
		return midiInputOn;
	}


	public void setMidiInputOn(boolean midiInputOn) {
		this.midiInputOn = midiInputOn;
	}


	public boolean isMidiOutputOn() {
		return midiOutputOn;
	}


	public void setMidiOutputOn(boolean midiOutputOn) {
		if (this.midiOutputOn!=midiOutputOn && !midiOutputOn) {
			getMidiHandler().sendAllNotesOff();
		}
		this.midiOutputOn = midiOutputOn;
	}


	public int getQuantization() {
		return quantization;
	}


	public void setQuantization(int quantization) {
		this.quantization = quantization;
	}


	public int getTransposeBy() {
		return transposeBy;
	}


	public void setTransposeBy(int transposeBy) {
		this.transposeBy = transposeBy;
	}


	public List<Note> getNotesList() {
		return notesList;
	}


	public void setNotesList(List<Note> notesList) {
		this.notesList = notesList;
	}




	public void clearPattern() {
		for (Note dc:getNotesList()) {
			getMidiHandler().sendNoteOff(dc.getPlayedNoteNumber());
		}
		getNotesList().clear();
		emitLoopUpdated();
	}


	public void saveLoop(File file) throws JsonGenerationException, JsonMappingException, IOException {
		StorageContainer data = new StorageContainer(getNotesList(), Note.APPLY_TRANSPOSE, Note.APPLY_QUANTIZATION, getLengthQuarters());		
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(file, data);
		LOG.info("Saved file {}", file.getPath());
	}

	public void loadLoop(File file) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		StorageContainer data = mapper.readValue(file, StorageContainer.class);
		LOG.info("Loaded file {}", file.getPath());
		clearPattern();
		for (Note n: data.getNotes()) {
			getNotesList().add(n);
		}
		Note.APPLY_QUANTIZATION = data.getQuantization();
		Note.APPLY_TRANSPOSE = data.getTranspose();
		setLengthQuarters(data.getLength());

		emitLoopUpdated();
		emitSettingsUpdated();
		getMidiHandler().sendAllNotesOff();
	}

	public void clearNote(Note note) {
		getNotesList().remove(note);
		getMidiHandler().sendNoteOff(note.getTransformedNoteNumber());
		emitLoopUpdated();
	}

	public void doublePattern() {
		ArrayList<Note> addNotes = new ArrayList<Note>();
		int posOffset = getMaxTicks();
		setLengthQuarters(getLengthQuarters()*2);
		for (Note note: getNotesList()) {
			if (note.isCompleted()) {
				addNotes.add(new Note(note, posOffset));
			}
		}
		getNotesList().addAll(addNotes);
		emitLoopUpdated();
		emitSettingsUpdated();
	}











	public void registerAsReceiver(DimidimiEventReceiver receiver) {
		if (receiver.getClass().isAssignableFrom(LoopUpdateReceiver.class)) {
			loopUpdateReceivers.add((LoopUpdateReceiver)receiver);
		}
		if (receiver.getClass().isAssignableFrom(PerformanceReceiver.class)) {
			performanceReceivers.add((PerformanceReceiver)receiver);
		}
		if (receiver.getClass().isAssignableFrom(SettingsUpdateReceiver.class)) {
			settingsUpdateReceivers.add((SettingsUpdateReceiver)receiver);
		}
	}


	public void emitLoopUpdated() {
		for (LoopUpdateReceiver receiver: loopUpdateReceivers) {
			receiver.loopUpdated(getNotesList());
		}
	}

	public void emitRefreshLoopDisplay() {
		for (LoopUpdateReceiver receiver: loopUpdateReceivers) {
			receiver.refreshLoopDisplay();
		}
	}


	public void emitNoteOn(int noteNumber, int velocity, int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.noteOn(noteNumber, velocity, pos);
		}
	}

	public void emitNoteOff(int notenumber, int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.noteOff(notenumber, pos);;
		}
	}

	public void emitClock(int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.receiveClock(pos);
		}
	}

	public void emitActive(boolean active, int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.receiveActive(active, pos);
		}
	}

	public void emitSettingsUpdated() {
		for (SettingsUpdateReceiver receiver: settingsUpdateReceivers) {
			receiver.settingsUpdated();
		}
	}



	private int lengthQuarters = 8;
	private int maxTicks = lengthQuarters*TICK_COUNT_BASE;
	private MidiHandler midiHandler;
	private int midiChannelIn = 0; // 0 - based
	private int midiChannelOut = 1;// 0 - based
	private boolean midiInputOn = true;
	private boolean midiOutputOn = true;
	private int ppqdiv = 2;
	private int quantization;
	private int transposeBy;
	private List<Note> notesList = new CopyOnWriteArrayList<Note>();

	private List<LoopUpdateReceiver> loopUpdateReceivers = new CopyOnWriteArrayList<LoopUpdateReceiver>();
	private List<PerformanceReceiver> performanceReceivers = new CopyOnWriteArrayList<PerformanceReceiver>();
	private List<SettingsUpdateReceiver> settingsUpdateReceivers = new CopyOnWriteArrayList<SettingsUpdateReceiver>();

	private static final int TICK_COUNT_BASE = 48;


}
