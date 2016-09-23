package de.privatepublic.midiutils;

import java.awt.EventQueue;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
	
	private static int CLOCK_INCREMENT = Prefs.get(Prefs.MIDI_CLOCK_INCREMENT, 2);
	
	public Session(int pos) {
		midiChannelIn = Prefs.get(Prefs.MIDI_IN_CHANNEL, 0);
		midiChannelOut = Prefs.get(Prefs.MIDI_OUT_CHANNEL, 1);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					midiHandler = new MidiHandler(Session.this, pos);
					window = new UIWindow(Session.this);
					new PerformanceHandler(Session.this);
				} catch (Exception e) {
					LOG.error("Could not create UIWindow", e);
				}
			}
		});
	}
	
	public Session(StorageContainer data) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					window = new UIWindow(Session.this);
					applyStorageData(data);
					emitLoopUpdated();
					emitSettingsUpdated();
					new PerformanceHandler(Session.this);
				} catch (Exception e) {
					LOG.error("Could not create UIWindow", e);
				}
			}
		});
		midiHandler = new MidiHandler(this, 0);
	}


	public int getClockIncrement() {
		return CLOCK_INCREMENT;
	}

	public void setClockIncrement(int steps) {
		if (steps != CLOCK_INCREMENT) {
			CLOCK_INCREMENT = steps;
			Prefs.put(Prefs.MIDI_CLOCK_INCREMENT, steps);
			DiMIDImi.updateSettingsOnAllSessions();
		}
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
		getMidiHandler().sendAllNotesOffMidi(oldOut);
		Prefs.put(Prefs.MIDI_OUT_CHANNEL, midiChannelOut);
		emitRefreshLoopDisplay();
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
			getMidiHandler().sendAllNotesOffMidi();
		}
		this.midiOutputOn = midiOutputOn;
	}


	public int getQuantizationIndex() {
		return quantizationIndex;
	}


	public void setQuantizationIndex(int quantization) {
		this.quantizationIndex = quantization;
	}


	public int getTransposeIndex() {
		return transposeIndex;
	}


	public void setTransposeIndex(int transposeBy) {
		this.transposeIndex = transposeBy;
	}


	public List<Note> getNotesList() {
		return notesList;
	}


	public void setNotesList(List<Note> notesList) {
		this.notesList = notesList;
	}
	
	public UIWindow getWindow() {
		return window;
	}




	public void clearPattern() {
		for (Note dc:getNotesList()) {
			getMidiHandler().sendNoteOffMidi(dc.getTransformedNoteNumber(getTransposeIndex()));
		}
		getNotesList().clear();
		emitLoopUpdated();
	}


	public void saveLoop(File file) throws JsonGenerationException, JsonMappingException, IOException {
		StorageContainer data = new StorageContainer(this);		
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(file, data);
		LOG.info("Saved file {}", file.getPath());
	}

	public void loadLoop(File file) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		StorageContainer data = mapper.readValue(file, StorageContainer.class);
		LOG.info("Loaded file {}", file.getPath());
		applyStorageData(data);
		emitLoopUpdated();
		emitSettingsUpdated();
		getMidiHandler().sendAllNotesOffMidi();
	}
	
	private void applyStorageData(StorageContainer data) {
		clearPattern();
		for (Note n: data.getNotes()) {
			getNotesList().add(n);
		}
		setQuantizationIndex(data.getQuantization());
		setTransposeIndex(data.getTranspose());
		setLengthQuarters(data.getLength());
		setMidiChannelIn(data.getMidiChannelIn());
		setMidiChannelOut(data.getMidiChannelOut());
		setMidiInputOn(data.isMidiChannelInActive());
		setMidiOutputOn(data.isMidiChannelOutActive());
		Map<String, Integer> wpos = data.getWindowPos();
		Rectangle windowBounds = new Rectangle(wpos.get("x"), wpos.get("y"), wpos.get("w"), wpos.get("h"));
		window.setScreenPosition(windowBounds);
	}

	public void clearNote(Note note) {
		getNotesList().remove(note);
		getMidiHandler().sendNoteOffMidi(note.getTransformedNoteNumber(getTransposeIndex()));
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


	public void destroy() {
		midiHandler.sendAllNotesOffMidi();
		notesList.clear();
		loopUpdateReceivers.clear();
		performanceReceivers.clear();
		settingsUpdateReceivers.clear();
		midiHandler = null;
		window = null;
	}








	public void registerAsReceiver(DimidimiEventReceiver receiver) {
		if (receiver instanceof LoopUpdateReceiver) {
			loopUpdateReceivers.add((LoopUpdateReceiver)receiver);
		}
		if (receiver instanceof PerformanceReceiver) {
			performanceReceivers.add((PerformanceReceiver)receiver);
		}
		if (receiver instanceof SettingsUpdateReceiver) {
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
	private int quantizationIndex = 0;
	private int transposeIndex = 13;
	private UIWindow window;
	private List<Note> notesList = new CopyOnWriteArrayList<Note>();

	private List<LoopUpdateReceiver> loopUpdateReceivers = new CopyOnWriteArrayList<LoopUpdateReceiver>();
	private List<PerformanceReceiver> performanceReceivers = new CopyOnWriteArrayList<PerformanceReceiver>();
	private List<SettingsUpdateReceiver> settingsUpdateReceivers = new CopyOnWriteArrayList<SettingsUpdateReceiver>();

	public static final int TICK_COUNT_BASE = 48;

}
