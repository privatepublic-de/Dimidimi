package de.privatepublic.midiutils;

import java.awt.Color;
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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.privatepublic.midiutils.Note.TransformationProvider;
import de.privatepublic.midiutils.events.DimidimiEventReceiver;
import de.privatepublic.midiutils.events.NotesUpdatedReceiver;
import de.privatepublic.midiutils.events.PerformanceReceiver;
import de.privatepublic.midiutils.events.SettingsUpdateReceiver;
import de.privatepublic.midiutils.ui.Theme;
import de.privatepublic.midiutils.ui.UIWindow;

public class Loop implements TransformationProvider, PerformanceReceiver, SettingsUpdateReceiver {

	private static final Logger LOG = LoggerFactory.getLogger(Loop.class);
	
	private static int SOLOCOUNT = 0;
	
	public Loop() {
		midiChannelIn = Prefs.get(Prefs.MIDI_IN_CHANNEL, 0);
		setMidiChannelOut(Prefs.get(Prefs.MIDI_OUT_CHANNEL, 1));
		updateColors();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					window = new UIWindow(Loop.this);
					emitLoopUpdated();
					registerAsReceiver(Loop.this);
					window.setVisible(true);
				} catch (Exception e) {
					LOG.error("Could not create UIWindow", e);
				}
			}
		});
	}
	
	public Loop(StorageContainer data, String name) {
		setName(name);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					window = new UIWindow(Loop.this);
					applyStorageData(data);
					updateColors();
					emitLoopUpdated();
					registerAsReceiver(Loop.this);
					window.setVisible(true);
				} catch (Exception e) {
					LOG.error("Could not create UIWindow", e);
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
			emitSettingsUpdated();
			emitRefreshLoopDisplay();
		}
		updateColors();
	}


	public boolean isMidiInputOn() {
		return midiInputOn;
	}


	public void setMidiInputOn(boolean midiInputOn) {
		this.midiInputOn = midiInputOn;
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
	
	public int[] getCcList() {
		return ccList;
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
		emitRefreshLoopDisplay();
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
			emitLoopUpdated();
		}
	}

	public boolean isSoloed() {
		return isSoloed;
	}

	public void setSoloed(boolean isSoloed) {
		if (isSoloed!=this.isSoloed) {
			this.isSoloed = isSoloed;
			SOLOCOUNT += isSoloed?1:-1;
			DiMIDImi.updateNotesOnAllLoops();
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
			return isSoloed;	
		}
		return true;
	}

	public UIWindow getWindow() {
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
	

	public void clearPattern() {
		for (Note dc:getNotesList()) {
			MidiHandler.instance().sendNoteOffMidi(this, dc.getNoteNumber(this));
		}
		getNotesList().clear();
		for (int i=0;i<MAX_NUMBER_OF_QUARTERS*TICK_COUNT_BASE;++i) {
			ccList[i] = 0;
			pitchBendList[i] = 0;
		}
		MidiHandler.instance().sendPitchBend(this, 0);
		MidiHandler.instance().sendCC(this, 0);
		emitLoopUpdated();
	}
	
	public void clearModWheel() {
		for (int i=0;i<MAX_NUMBER_OF_QUARTERS*TICK_COUNT_BASE;++i) {
			ccList[i] = 0;
		}
		MidiHandler.instance().sendCC(this, 0);
	}
	
	public void clearPitchBend() {
		for (int i=0;i<MAX_NUMBER_OF_QUARTERS*TICK_COUNT_BASE;++i) {
			pitchBendList[i] = 0;
		}
		MidiHandler.instance().sendPitchBend(this, 0);
	}


	public void saveLoop(File file) throws JsonGenerationException, JsonMappingException, IOException {
		StorageContainer data = new StorageContainer(this);		
		mapper.writeValue(file, data);
		LOG.info("Saved file {}", file.getPath());
	}

	public void loadLoop(File file) throws JsonParseException, JsonMappingException, IOException {
		StorageContainer data = mapper.readValue(file, StorageContainer.class);
		LOG.info("Loaded file {}", file.getPath());
		applyStorageData(data);
		emitSettingsUpdated();
		emitLoopUpdated();
		MidiHandler.instance().sendAllNotesOffMidi(this);
	}
	
	private void applyStorageData(StorageContainer data) {
		setQuantizationIndex(data.getQuantization());
		setTransposeIndex(data.getTranspose());
		setLengthQuarters(data.getLength());
		setMidiChannelIn(data.getMidiChannelIn());
		setMidiChannelOut(data.getMidiChannelOut());
		setMidiInputOn(data.isMidiChannelInActive());
		setDrums(data.isDrums());
		clearPattern();
		for (Note n: data.getNotes()) {
			getNotesList().add(n);
		}
		data.copyList(data.getPitchBend(), pitchBendList);
		data.copyList(data.getModWheel(), ccList);
		Map<String, Integer> wpos = data.getWindowPos();
		if (wpos!=null) {
			Rectangle windowBounds = new Rectangle(wpos.get("x"), wpos.get("y"), wpos.get("w"), wpos.get("h"));
			window.setScreenPosition(windowBounds);
		}
	}

	public void clearNote(Note note) {
		getNotesList().remove(note);
		MidiHandler.instance().sendNoteOffMidi(this, note.getNoteNumber(this));
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
	
	public void doubleSpeed() {
		setLengthQuarters(getLengthQuarters()/2);
		for (Note note: getNotesList()) {
			if (note.isCompleted()) {
				note.setPosStart(note.getPosStart()/2);
				note.setPosEnd(note.getPosEnd()/2);
			}
		}
		emitLoopUpdated();
		emitSettingsUpdated();
	}
	
	public void halfSpeed() {
		setLengthQuarters(getLengthQuarters()*2);
		for (Note note: getNotesList()) {
			if (note.isCompleted()) {
				note.setPosStart(note.getPosStart()*2);
				note.setPosEnd(note.getPosEnd()*2);
			}
		}
		emitLoopUpdated();
		emitSettingsUpdated();
	}


	public void destroy() {
		setSoloed(false);
		MidiHandler.instance().sendAllNotesOffMidi(this);
		notesList.clear();
		loopUpdateReceivers.clear();
		performanceReceivers.clear();
		settingsUpdateReceivers.clear();
		window = null;
	}








	public void registerAsReceiver(DimidimiEventReceiver receiver) {
		if (receiver instanceof NotesUpdatedReceiver) {
			loopUpdateReceivers.add((NotesUpdatedReceiver)receiver);
		}
		if (receiver instanceof PerformanceReceiver) {
			performanceReceivers.add((PerformanceReceiver)receiver);
		}
		if (receiver instanceof SettingsUpdateReceiver) {
			settingsUpdateReceivers.add((SettingsUpdateReceiver)receiver);
		}
	}


	public void emitLoopUpdated() {
		for (NotesUpdatedReceiver receiver: loopUpdateReceivers) {
			receiver.onNotesUpdated();
		}
	}

	public void emitRefreshLoopDisplay() {
		for (NotesUpdatedReceiver receiver: loopUpdateReceivers) {
			receiver.onRefreshLoopDisplay();
		}
	}


	public void emitNoteOn(int noteNumber, int velocity, int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.onNoteOn(noteNumber, velocity, pos);
		}
	}

	public void emitNoteOff(int notenumber, int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.onNoteOff(notenumber, pos);;
		}
	}
	
	public void emitCC(int cc, int val, int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.onReceiveCC(cc, val, pos);
		}
	}
	
	public void emitPitchBend(int val, int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.onReceivePitchBend(val, pos);
		}
	}
	

	public void emitClock(int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.onClock(pos);
		}
	}

	public void emitActive(boolean active, int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.onActivityChange(active, pos);
		}
	}
	
	public void emitState() {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.onStateChange(isMuted, isSoloed, queuedMuteState, queuedSoloState);
		}
	}

	public void emitSettingsUpdated() {
		for (SettingsUpdateReceiver receiver: settingsUpdateReceivers) {
			receiver.onSettingsUpdated();
		}
	}
	
	
	
	@Override
	public void onNoteOn(int noteNumber, int velocity, int pos) {
		if (MidiHandler.ACTIVE && isMidiInputOn()) {
			Note note = new Note(noteNumber, velocity, pos);
			lastStarted[noteNumber] = note;
			getNotesList().add(note);
			Note overlap = findOverlappingNote(note, pos);
			if (overlap!=null) {
				getNotesList().remove(overlap);
			}
			emitLoopUpdated();
		}
	}
	
	@Override
	public void onNoteOff(int notenumber, int pos) {
		if (MidiHandler.ACTIVE && isMidiInputOn()) {
			Note reference = lastStarted[notenumber];
			if (reference!=null) {
				reference.setPosEnd(pos);
			}
			emitLoopUpdated();
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
			overrideCC = overridePitchBend = false;
		}
		if (overridePitchBend) {
			pitchBendList[pos] = currentPitchBend;	
		}
		if (overrideCC) {
			ccList[pos] = currentCC;
		}
		int prevpos = (pos+getMaxTicks()-1)%getMaxTicks();
		if (pitchBendList[pos]!=pitchBendList[prevpos]) {
			MidiHandler.instance().sendPitchBend(this, pitchBendList[pos]);
		}
		if (ccList[pos]!=ccList[prevpos]) {
			MidiHandler.instance().sendCC(this, ccList[pos]);
		}
		
		boolean isAudible = isAudible();
		
		for (Note note:getNotesList()) {
			if (!note.isCompleted()) {
				Note overlap = findOverlappingNote(note, pos);
				if (overlap!=null) {
					getNotesList().remove(overlap);
				}
				emitLoopUpdated();
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
			boolean hasChanges = queuedMuteState!=QueuedState.NO_CHANGE || queuedSoloState!=QueuedState.NO_CHANGE;
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
				setSoloed(false);
				break;
			case ON:
				setSoloed(true);
				break;
			default:
				break;
			}
			queuedSoloState = QueuedState.NO_CHANGE;
			if (hasChanges) {
				emitState();
				emitLoopUpdated();
			}
			
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
		if (isMidiInputOn()) {
			if (cc==1) {
				currentCC = val;
				ccList[pos] = val;
				overrideCC = true;
			}
		}
	}

	@Override
	public void onReceivePitchBend(int val, int pos) {
		if (isMidiInputOn()) {
			currentPitchBend = val;
			pitchBendList[pos] = val;
			overridePitchBend = true;
		}
	}
	
	


	private int lengthQuarters = 8;
	private int maxTicks = lengthQuarters*TICK_COUNT_BASE;
	private int midiChannelIn = -1; // 0 - based
	private int midiChannelOut = -1;// 0 - based
	private boolean midiInputOn = true;
	private boolean isMuted = false;
	private boolean isSoloed = false;
	private boolean isDrums = false;
	private boolean isMetronomeEnabled = false;
	private QueuedState queuedMuteState = QueuedState.NO_CHANGE;
	private QueuedState queuedSoloState = QueuedState.NO_CHANGE;
	private int quantizationIndex = 0;
	private int transposeIndex = 13;
	private UIWindow window;
	private int currentCC = 0;
	private int currentPitchBend = 0;
	private List<Note> notesList = new CopyOnWriteArrayList<Note>();
	private boolean overrideCC = false;
	private boolean overridePitchBend = false;
	private int[] ccList = new int[MAX_NUMBER_OF_QUARTERS*TICK_COUNT_BASE];
	private int[] pitchBendList = new int[MAX_NUMBER_OF_QUARTERS*TICK_COUNT_BASE];
	private String name;
	
	private Color colorNote;
	private Color colorNoteBright;
	private Color colorNoteSelected;
	private Color colorNotePlayed;
	private Color colorNoteBrightSelected;
	private Color colorChannel;

	private List<NotesUpdatedReceiver> loopUpdateReceivers = new CopyOnWriteArrayList<NotesUpdatedReceiver>();
	private List<PerformanceReceiver> performanceReceivers = new CopyOnWriteArrayList<PerformanceReceiver>();
	private List<SettingsUpdateReceiver> settingsUpdateReceivers = new CopyOnWriteArrayList<SettingsUpdateReceiver>();

	private Note[] lastStarted = new Note[128];
	
	public static final int TICK_COUNT_BASE = 24;
	public static final int MAX_NUMBER_OF_QUARTERS = 64;
	
	public static enum QueuedState { NO_CHANGE, ON, OFF }

	@Override
	public void onStateChange(boolean mute, boolean solo, QueuedState queuedMute, QueuedState queuedSolo) {
		
	}
	
	private static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
	}
	
}
