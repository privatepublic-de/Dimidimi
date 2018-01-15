package de.privatepublic.midiutils;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.privatepublic.midiutils.Note.TransformationProvider;
import de.privatepublic.midiutils.events.DimidimiEventReceiver;
import de.privatepublic.midiutils.events.FocusReceiver;
import de.privatepublic.midiutils.events.NotesUpdatedReceiver;
import de.privatepublic.midiutils.events.PerformanceReceiver;
import de.privatepublic.midiutils.events.SettingsUpdateReceiver;
import de.privatepublic.midiutils.ui.Theme;
import de.privatepublic.midiutils.ui.LoopWindow;

public class Loop implements TransformationProvider, PerformanceReceiver, SettingsUpdateReceiver, Comparable<Loop> {

	public static enum QueuedState { NO_CHANGE, ON, OFF }
	public static final int TICK_COUNT_BASE = 24;
	public static final int MAX_NUMBER_OF_QUARTERS = 64;
	
	private static final Logger LOG = LoggerFactory.getLogger(Loop.class);
	private static int SOLOCOUNT = 0;
	private static final List<Loop> LOOPS = new CopyOnWriteArrayList<Loop>();
	private static final ObjectMapper MAPPER = new ObjectMapper();
	static {
		MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	private int lengthQuarters = 8;
	private int maxTicks = lengthQuarters*TICK_COUNT_BASE;
	private int midiChannelIn = -1; // 0 - based
	private int midiChannelOut = -1;// 0 - based
	private boolean isRecordOn = true;
	private boolean isMuted = false;
	private boolean isSolo = false;
	private boolean isDrums = false;
	private boolean isMetronomeEnabled = false;
	private QueuedState queuedMuteState = QueuedState.NO_CHANGE;
	private QueuedState queuedSoloState = QueuedState.NO_CHANGE;
	private int quantizationIndex = 0;
	private int transposeIndex = 13;
	private LoopWindow window;
	private int currentModWheel = 0;
	private int currentPressure = 0;
	private int currentPitchBend = 0;
	private List<Note> notesList = new CopyOnWriteArrayList<Note>();
	private boolean overrideModWheel = false;
	private boolean overridePressure = false;
	private boolean overridePitchBend = false;
	private int[] modWheelList = new int[MAX_NUMBER_OF_QUARTERS*TICK_COUNT_BASE];
	private int[] pressureList = new int[MAX_NUMBER_OF_QUARTERS*TICK_COUNT_BASE];
	private int[] pitchBendList = new int[MAX_NUMBER_OF_QUARTERS*TICK_COUNT_BASE];
	private String name;
	
	private Color colorNote;
	private Color colorNoteBright;
	private Color colorNoteSelected;
	private Color colorNotePlayed;
	private Color colorNoteBrightSelected;
	private Color colorChannel;
	private Color colorDimmedBackground;

	private List<NotesUpdatedReceiver> loopUpdateReceivers = new CopyOnWriteArrayList<NotesUpdatedReceiver>();
	private List<PerformanceReceiver> performanceReceivers = new CopyOnWriteArrayList<PerformanceReceiver>();
	private List<SettingsUpdateReceiver> settingsUpdateReceivers = new CopyOnWriteArrayList<SettingsUpdateReceiver>();
	private List<FocusReceiver> focusReceivers = new CopyOnWriteArrayList<FocusReceiver>();

	private Note[] lastStarted = new Note[128];
	
	public Loop() {
		midiChannelIn = Prefs.get(Prefs.MIDI_IN_CHANNEL, 0);
		setMidiChannelOut(Prefs.get(Prefs.MIDI_OUT_CHANNEL, 1));
		updateColors();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					window = new LoopWindow(Loop.this);
					triggerNotesUpdated();
					registerReceiver(Loop.this);
					window.setVisible(true);
				} catch (Exception e) {
					LOG.error("Could not create LoopWindow", e);
				}
			}
		});
	}
	
	public Loop(StorageContainer data, String name) {
		setName(name);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					window = new LoopWindow(Loop.this);
					applyStorageData(data);
					updateColors();
					triggerNotesUpdated();
					registerReceiver(Loop.this);
					window.setVisible(true);
				} catch (Exception e) {
					LOG.error("Could not create LoopWindow", e);
				}
			}
		});
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
		if (this.midiChannelIn!=midiChannelIn) {
			this.midiChannelIn = midiChannelIn;
			Prefs.put(Prefs.MIDI_IN_CHANNEL, midiChannelIn);
		}
	}

	// 0 - based
	public int getMidiChannelOut() {
		return midiChannelOut;
	}

	// 0 - based
	public void setMidiChannelOut(int midiChannelOut) {
		int oldOut = this.midiChannelOut;
		if (oldOut!=midiChannelOut) {
			this.midiChannelOut = midiChannelOut;
			Prefs.put(Prefs.MIDI_OUT_CHANNEL, midiChannelOut);
			MidiHandler.instance().sendAllNotesOffMidi(this, oldOut>-1?oldOut:midiChannelOut);
			triggerSettingsUpdated();
			triggerRefreshLoopDisplay();
		}
		updateColors();
	}


	public boolean isRecordOn() {
		return isRecordOn;
	}


	public void setRecordOn(boolean recordOn) {
		this.isRecordOn = recordOn;
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
	
	public int[] getModWheelList() {
		return modWheelList;
	}
	
	public int[] getPressureList() {
		return pressureList;
	}

	public int[] getPitchBendList() {
		return pitchBendList;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public boolean isDrums() {
		return isDrums;
	}
	
	public void setDrums(boolean drummode) {
		isDrums = drummode;
		triggerRefreshLoopDisplay();
	}

	public boolean isMetronomeEnabled() {
		return isMetronomeEnabled;
	}

	public void setMetronomeEnabled(boolean isMetronomeEnabled) {
		this.isMetronomeEnabled = isMetronomeEnabled;
	}

	public boolean isMuted() {
		return isMuted;
	}

	public void setMuted(boolean isMuted) {
		if (isMuted!=this.isMuted) {
			this.isMuted = isMuted;
			queuedMuteState = QueuedState.NO_CHANGE;
			triggerNotesUpdated();
			triggerStateChange();
		}
	}

	public boolean isSolo() {
		return isSolo;
	}

	public void setSolo(boolean solo) {
		if (solo!=this.isSolo) {
			queuedSoloState = QueuedState.NO_CHANGE;
			this.isSolo = solo;
			SOLOCOUNT += solo?1:-1;
			Loop.updateNotesOnAllLoops();
			Loop.updateStateOnAllLoops();
			triggerStateChange();
		}
	}

	public QueuedState getQueuedMute() {
		return queuedMuteState;
	}

	public void setQueuedMute(QueuedState isQueuedMute) {
		this.queuedMuteState = isQueuedMute;
	}

	public QueuedState getQueuedSolo() {
		return queuedSoloState;
	}

	public void setQueuedSolo(QueuedState isQueuedSolo) {
		this.queuedSoloState = isQueuedSolo;
	}
	
	public boolean isAudible() {
		if (isMuted) {
			return false;
		}
		if (SOLOCOUNT>0) {
			return isSolo;	
		}
		return true;
	}

	public LoopWindow getWindow() {
		return window;
	}

	public Color getNoteColor(boolean selected) {
		return selected?colorNoteSelected:colorNote;
	}
	
	public Color getNoteColorHighlighted(boolean selected) {
		return selected?colorNoteBrightSelected:colorNoteBright;
	}
	
	public Color getNoteColorPlayed() {
		return colorNotePlayed;
	}
	
	public Color getChannelColor() {
		return colorChannel;
	}
	

	public Color getColorBackground() {
		if (isAudible()) {
			return Theme.isBright()?getNoteColorPlayed():getNoteColor(false);
		}
		else {
			return colorDimmedBackground;
		}
	}

	public void clearPattern() {
		for (Note dc:getNotesList()) {
			MidiHandler.instance().sendNoteOffMidi(this, dc.getNoteNumber(this));
		}
		getNotesList().clear();
		for (int i=0;i<MAX_NUMBER_OF_QUARTERS*TICK_COUNT_BASE;++i) {
			modWheelList[i] = 0;
			pitchBendList[i] = 0;
		}
		MidiHandler.instance().sendPitchBend(this, 0);
		MidiHandler.instance().sendCC(this, 0);
		triggerNotesUpdated();
	}
	
	public void clearModWheel() {
		for (int i=0;i<MAX_NUMBER_OF_QUARTERS*TICK_COUNT_BASE;++i) {
			modWheelList[i] = 0;
		}
		MidiHandler.instance().sendCC(this, 0);
	}
	
	public void clearPitchBend() {
		for (int i=0;i<MAX_NUMBER_OF_QUARTERS*TICK_COUNT_BASE;++i) {
			pitchBendList[i] = 0;
		}
		MidiHandler.instance().sendPitchBend(this, 0);
	}
	
	public void clearPressure() {
		for (int i=0;i<MAX_NUMBER_OF_QUARTERS*TICK_COUNT_BASE;++i) {
			pressureList[i] = 0;
		}
		MidiHandler.instance().sendChannelPressure(this, 0);
	}
	
	


	public void saveToFile(File file) throws JsonGenerationException, JsonMappingException, IOException {
		StorageContainer data = new StorageContainer(this);		
		MAPPER.writeValue(file, data);
		LOG.info("Saved file {}", file.getPath());
	}

	public void loadFromFile(File file) throws JsonParseException, JsonMappingException, IOException {
		StorageContainer data = MAPPER.readValue(file, StorageContainer.class);
		LOG.info("Loaded file {}", file.getPath());
		applyStorageData(data);
		triggerSettingsUpdated();
		triggerNotesUpdated();
		MidiHandler.instance().sendAllNotesOffMidi(this);
	}
	
	private void applyStorageData(StorageContainer data) {
		setQuantizationIndex(data.getQuantization());
		setTransposeIndex(data.getTranspose());
		setLengthQuarters(data.getLength());
		setMidiChannelIn(data.getMidiChannelIn());
		setMidiChannelOut(data.getMidiChannelOut());
		setRecordOn(data.isMidiChannelInActive());
		setDrums(data.isDrums());
		clearPattern();
		for (Note n: data.getNotes()) {
			getNotesList().add(n);
		}
		data.copyList(data.getPitchBend(), pitchBendList);
		data.copyList(data.getModWheel(), modWheelList);
		data.copyList(data.getChannelPressure(), pressureList);
		Map<String, Integer> wpos = data.getWindowPos();
		if (wpos!=null) {
			Rectangle windowBounds = new Rectangle(wpos.get("x"), wpos.get("y"), wpos.get("w"), wpos.get("h"));
			window.setScreenPosition(windowBounds);
		}
	}

	public void clearNote(Note note) {
		getNotesList().remove(note);
		MidiHandler.instance().sendNoteOffMidi(this, note.getNoteNumber(this));
		triggerNotesUpdated();
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
		triggerNotesUpdated();
		triggerSettingsUpdated();
	}
	
	public void doubleSpeed() {
		setLengthQuarters(getLengthQuarters()/2);
		for (Note note: getNotesList()) {
			if (note.isCompleted()) {
				note.setPosStart(note.getPosStart()/2);
				note.setPosEnd(note.getPosEnd()/2);
			}
		}
		triggerNotesUpdated();
		triggerSettingsUpdated();
	}
	
	public void halfSpeed() {
		setLengthQuarters(getLengthQuarters()*2);
		for (Note note: getNotesList()) {
			if (note.isCompleted()) {
				note.setPosStart(note.getPosStart()*2);
				note.setPosEnd(note.getPosEnd()*2);
			}
		}
		triggerNotesUpdated();
		triggerSettingsUpdated();
	}
	
	
	public void applyQuantization() {
		for (Note note : getNotesList()) {
			note.setPosStart(note.getPosStart(this));
			note.setPosEnd(note.getPosEnd(this));
		}
		triggerRefreshLoopDisplay();
	}
	
	public void applyTransposition() {
		for (Note note : getNotesList()) { 
			note.setNoteNumber(note.getNoteNumber(this));
		}
		triggerRefreshLoopDisplay();
	}
	


	public void destroy() {
		setSolo(false);
		MidiHandler.instance().sendAllNotesOffMidi(this);
		notesList.clear();
		loopUpdateReceivers.clear();
		performanceReceivers.clear();
		settingsUpdateReceivers.clear();
		window = null;
	}


	public void registerReceiver(DimidimiEventReceiver receiver) {
		if (receiver instanceof NotesUpdatedReceiver) {
			loopUpdateReceivers.add((NotesUpdatedReceiver)receiver);
		}
		if (receiver instanceof PerformanceReceiver) {
			performanceReceivers.add((PerformanceReceiver)receiver);
		}
		if (receiver instanceof SettingsUpdateReceiver) {
			settingsUpdateReceivers.add((SettingsUpdateReceiver)receiver);
		}
		if (receiver instanceof FocusReceiver) {
			focusReceivers.add((FocusReceiver)receiver);
		}
	}

	public void triggerNotesUpdated() {
		for (NotesUpdatedReceiver receiver: loopUpdateReceivers) {
			receiver.onNotesUpdated();
		}
	}

	public void triggerRefreshLoopDisplay() {
		for (NotesUpdatedReceiver receiver: loopUpdateReceivers) {
			receiver.onRefreshLoopDisplay();
		}
	}

	public void triggerNoteOn(int noteNumber, int velocity, int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.onNoteOn(noteNumber, velocity, pos);
		}
	}

	public void triggerNoteOff(int notenumber, int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.onNoteOff(notenumber, pos);;
		}
	}
	
	public void triggerReceiveCC(int cc, int val, int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.onReceiveCC(cc, val, pos);
		}
	}
	
	public void triggerReceivePressure(int val, int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.onReceivePressure(val, pos);
		}
	}
	
	public void triggerReceivePitchBend(int val, int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.onReceivePitchBend(val, pos);
		}
	}

	public void triggerClock(int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.onClock(pos);
		}
	}

	public void triggerActivityChange(boolean active, int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.onActivityChange(active, pos);
		}
	}
	
	public void triggerStateChange() {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.onStateChange(isMuted, isSolo, queuedMuteState, queuedSoloState);
		}
	}

	public void triggerSettingsUpdated() {
		for (SettingsUpdateReceiver receiver: settingsUpdateReceivers) {
			receiver.onSettingsUpdated();
		}
	}

	public void triggerFocusLoop(Loop focusLoop) {
		for (FocusReceiver receiver: focusReceivers) {
			receiver.onFocusLoop(focusLoop);
		}
	}

	
	
	@Override
	public void onNoteOn(int noteNumber, int velocity, int pos) {
		if (MidiHandler.ACTIVE && isRecordOn()) {
			Note note = new Note(noteNumber, velocity, pos);
			lastStarted[noteNumber] = note;
			getNotesList().add(note);
			Note overlap = findOverlappingNote(note, pos);
			if (overlap!=null) {
				getNotesList().remove(overlap);
			}
			triggerNotesUpdated();
		}
	}
	
	@Override
	public void onNoteOff(int notenumber, int pos) {
		if (MidiHandler.ACTIVE && isRecordOn()) {
			Note reference = lastStarted[notenumber];
			if (reference!=null) {
				reference.setPosEnd(pos);
			}
			triggerNotesUpdated();
		}
	}
	
	int lastMetronomeNote = 0;
	
	@Override
	public void onClock(int pos) {
		
		if (lastMetronomeNote>0) {
			MidiHandler.instance().sendNoteOffMidi(this, lastMetronomeNote);
		}
		
		if (isMetronomeEnabled() && pos%24==0) { // is a quarter note
			boolean is3based = getLengthQuarters()%3==0;
			int q = pos/6;
			boolean accented = ((is3based && q%12==0) || (!is3based && q%16==0));
			if (isDrums()) {
				lastMetronomeNote = accented?42:37;				
			}
			else {
				lastMetronomeNote = accented?81:69;	
			}
			MidiHandler.instance().sendNoteOnMidi(this, lastMetronomeNote, 127); 
		}
		
		if (pos==0) {
			overrideModWheel = overridePitchBend = overridePressure = false;
		}
		if (overridePitchBend) {
			pitchBendList[pos] = currentPitchBend;	
		}
		if (overrideModWheel) {
			modWheelList[pos] = currentModWheel;
		}
		if (overridePressure) {
			pressureList[pos] = currentPressure;
		}
		int prevpos = (pos+getMaxTicks()-1)%getMaxTicks();
		if (pitchBendList[pos]!=pitchBendList[prevpos]) {
			MidiHandler.instance().sendPitchBend(this, pitchBendList[pos]);
		}
		if (modWheelList[pos]!=modWheelList[prevpos]) {
			MidiHandler.instance().sendCC(this, modWheelList[pos]);
		}
		
		if (pressureList[pos]!=pressureList[prevpos]) {
			MidiHandler.instance().sendChannelPressure(this, pressureList[pos]);
		}
		
		boolean isAudible = isAudible();
		
		for (Note note:getNotesList()) {
			if (!note.isCompleted()) {
				Note overlap = findOverlappingNote(note, pos);
				if (overlap!=null) {
					getNotesList().remove(overlap);
				}
				triggerNotesUpdated();
				continue;
			}
			if (isAudible && pos==note.getPosStart(this)) {
				int playnumber = note.getNoteNumber(this);
				note.setPlayed(playnumber);
				MidiHandler.instance().sendNoteOnMidi(this, playnumber, note.getVelocity());
			}
			if (pos==note.getPosEnd(this) && note.isPlayed()) {
				MidiHandler.instance().sendNoteOffMidi(this, note.getPlayedNoteNumber());
				note.setUnPlayed();
			}
		}
		
		if (pos==getMaxTicks()-1) {
			// check for queued mutes and solos
			switch (queuedMuteState) {
			case OFF:
				setMuted(false);
				break;
			case ON:
				setMuted(true);
				break;
			default:
				break;
			}
			queuedMuteState = QueuedState.NO_CHANGE;
			switch (queuedSoloState) {
			case OFF:
				setSolo(false);
				break;
			case ON:
				setSolo(true);
				break;
			default:
				break;
			}
			queuedSoloState = QueuedState.NO_CHANGE;
		}
		
	}


	@Override
	public void onActivityChange(boolean active, int pos) {
		if (!active) {
			// find still uncompleted notes
			for (Note nr:getNotesList()) {
				if (!nr.isCompleted()) {
					nr.setPosEnd(pos);
				}
			}
			
		}
	}

	private Note findOverlappingNote(Note note, int pos) {
		for (Note ln:getNotesList()) {
			if (ln!=note && ln.getNoteNumber()==note.getNoteNumber()) {
				if (ln.getPosStart()<ln.getPosEnd()) {
					if (ln.getPosStart()<=pos && ln.getPosEnd()>pos) {
						return ln;
					}
				}
				else {
					if (ln.getPosStart()<=pos && ln.getPosEnd()>0) {
						return ln;
					}
				}
			}
		}
		return null;
	}

	@Override
	public void onReceiveCC(int cc, int val, int pos) {
		if (isRecordOn()) {
			if (cc==1) { // mod wheel
				currentModWheel = val;
				modWheelList[pos] = val;
				overrideModWheel = true;
			}
		}
	}
	

	@Override
	public void onReceivePressure(int val, int pos) {
		if (isRecordOn()) {
			currentPressure = val;
			pressureList[pos] = val;
			overridePressure = true;
		}
	}


	@Override
	public void onReceivePitchBend(int val, int pos) {
		if (isRecordOn()) {
			currentPitchBend = val;
			pitchBendList[pos] = val;
			overridePitchBend = true;
		}
	}
	
	


	
	@Override
	public void onStateChange(boolean mute, boolean solo, QueuedState queuedMute, QueuedState queuedSolo) {
		
	}
	
	
	@Override
	public void onSettingsUpdated() {
		updateColors();
	}

	private void updateColors() {
		float hue = midiChannelOut/18f;
		colorNote = Color.getHSBColor(hue, Theme.APPLY.noteColorSaturation(), Theme.APPLY.noteColorBrightness());
		colorNotePlayed = Color.getHSBColor(hue, .25f, 1);
	    colorNoteBright = Color.getHSBColor(hue, Theme.APPLY.noteColorSaturation(), Theme.APPLY.noteColorBrightness()*Theme.APPLY.noteLightColorBrightnessFactor());
	    colorNoteSelected = Color.getHSBColor(hue, Theme.APPLY.noteColorSaturation()*.5f, Theme.APPLY.noteColorBrightness());
	    colorNoteBrightSelected = Color.getHSBColor(hue, Theme.APPLY.noteColorSaturation()*.5f, Theme.APPLY.noteColorBrightness()*Theme.APPLY.noteLightColorBrightnessFactor());
	    colorChannel = Color.getHSBColor(hue, Theme.APPLY.colorChannelSaturation(), Theme.APPLY.getColorChannelBrightness());
	    colorDimmedBackground = Color.getHSBColor(hue, .1f, Theme.APPLY.noteColorBrightness()*Theme.APPLY.noteLightColorBrightnessFactor());
	}

	@Override
	public int compareTo(Loop o) {
		return Integer.compare(midiChannelOut, o.midiChannelOut);
	}

	public static void updateSettingsOnAllLoops() {
		LOG.info("Updating settings for all loops");
		for (Loop loop: LOOPS) {
			loop.triggerSettingsUpdated();
		}
	}

	public static void updateNotesOnAllLoops() {
		LOG.info("Updating notes for all loops");
		for (Loop loop: LOOPS) {
			loop.triggerNotesUpdated();
		}
	}

	public static void updateStateOnAllLoops() {
		LOG.info("Updating state for all loops");
		for (Loop loop: LOOPS) {
			loop.triggerStateChange();
		}
	}

	public static Loop createLoop() {
		Loop loop = new Loop();
		LOOPS.add(loop);
		loop.registerReceiver(DiMIDImi.getControllerWindow());
		Loop.updateSettingsOnAllLoops();
		LOG.info("Created new loop {}", loop.hashCode());
		return loop;
	}

	public static Loop createLoop(StorageContainer data, String name) {
		Loop loop = new Loop(data, name);
		LOOPS.add(loop);
		loop.registerReceiver(DiMIDImi.getControllerWindow());
		Loop.updateSettingsOnAllLoops();
		LOG.info("Created new loop {}", loop.hashCode());
		return loop;
	}

	public static void removeLoop(Loop loop) {
		if (LOOPS.contains(loop)) {
			LOOPS.remove(loop);
			loop.destroy();
			Loop.updateSettingsOnAllLoops();
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

	public static void saveSession(File file) throws JsonGenerationException, JsonMappingException, IOException {
		List<StorageContainer> dataList = new ArrayList<StorageContainer>();
		String name = FilenameUtils.getBaseName(file.getName());
		for (Loop loop: LOOPS) {
			StorageContainer data = new StorageContainer(loop);
			dataList.add(data);
			loop.setName(name);
		}
		MAPPER.writeValue(file, dataList);
		LOG.info("Saved loop {}", file.getPath());
	}

	public static void loadSession(File file) throws JsonParseException, JsonMappingException, IOException {
		List<Loop> loopsToClose = new ArrayList<Loop>(LOOPS);
		List<StorageContainer> list = Arrays.asList(MAPPER.readValue(file, StorageContainer[].class));
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

	
}
