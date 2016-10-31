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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.privatepublic.midiutils.events.DimidimiEventReceiver;
import de.privatepublic.midiutils.events.LoopUpdateReceiver;
import de.privatepublic.midiutils.events.PerformanceReceiver;
import de.privatepublic.midiutils.events.SettingsUpdateReceiver;
import de.privatepublic.midiutils.ui.Theme;
import de.privatepublic.midiutils.ui.UIWindow;

public class Session implements PerformanceReceiver {

	private static final Logger LOG = LoggerFactory.getLogger(Session.class);
	
	private static int SOLOCOUNT = 0;
	
	public Session() {
		midiChannelIn = Prefs.get(Prefs.MIDI_IN_CHANNEL, 0);
		midiChannelOut = Prefs.get(Prefs.MIDI_OUT_CHANNEL, 1);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					window = new UIWindow(Session.this);
					// new PerformanceHandler(Session.this);
					registerAsReceiver(Session.this);
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
					// new PerformanceHandler(Session.this);
					registerAsReceiver(Session.this);
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
		MidiHandler.instance().sendAllNotesOffMidi(this, oldOut);
		
		colorNote = Color.getHSBColor(midiChannelOut/16f, Theme.CURRENT.getNoteColorSaturation(), Theme.CURRENT.getNoteColorBrightness());
	    colorNoteBright = Color.getHSBColor(midiChannelOut/16f, Theme.CURRENT.getNoteColorSaturation(), Theme.CURRENT.getNoteColorBrightness()*Theme.CURRENT.getNoteLightColorBrightnessFactor());
	    colorNoteSelected = Color.getHSBColor(midiChannelOut/16f, Theme.CURRENT.getNoteColorSaturation()*.5f, Theme.CURRENT.getNoteColorBrightness());
	    colorNoteBrightSelected = Color.getHSBColor(midiChannelOut/16f, Theme.CURRENT.getNoteColorSaturation()*.5f, Theme.CURRENT.getNoteColorBrightness()*Theme.CURRENT.getNoteLightColorBrightnessFactor());
		
		Prefs.put(Prefs.MIDI_OUT_CHANNEL, midiChannelOut);
		emitSettingsUpdated();
		emitRefreshLoopDisplay();
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
			DiMIDImi.updateLoopsOnAllSessions();
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
	

	public void clearPattern() {
		for (Note dc:getNotesList()) {
			MidiHandler.instance().sendNoteOffMidi(this, dc.getTransformedNoteNumber(getTransposeIndex()));
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
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(file, data);
		LOG.info("Saved file {}", file.getPath());
	}

	public void loadLoop(File file) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		StorageContainer data = mapper.readValue(file, StorageContainer.class);
		LOG.info("Loaded file {}", file.getPath());
		applyStorageData(data);
		emitSettingsUpdated();
		emitLoopUpdated();
		MidiHandler.instance().sendAllNotesOffMidi(this);
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
		MidiHandler.instance().sendNoteOffMidi(this, note.getTransformedNoteNumber(getTransposeIndex()));
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
			receiver.loopUpdated();
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
	
	public void emitCC(int cc, int val, int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.receiveCC(cc, val, pos);
		}
	}
	
	public void emitPitchBend(int val, int pos) {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.receivePitchBend(val, pos);
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
	
	public void emitState() {
		for (PerformanceReceiver receiver: performanceReceivers) {
			receiver.stateChange(isMuted, isSoloed, queuedMuteState, queuedSoloState);
		}
	}

	public void emitSettingsUpdated() {
		for (SettingsUpdateReceiver receiver: settingsUpdateReceivers) {
			receiver.settingsUpdated();
		}
	}
	
	
	
	@Override
	public void noteOn(int noteNumber, int velocity, int p) {
		if (MidiHandler.ACTIVE && isMidiInputOn()) {
			int pos = p%getMaxTicks();
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
	public void noteOff(int notenumber, int p) {
		int pos = p%getMaxTicks();
		if (MidiHandler.ACTIVE && isMidiInputOn()) {
			Note reference = lastStarted[notenumber];
			if (reference!=null) {
				reference.setPosEnd(pos);
			}
			emitLoopUpdated();
		}
	}
	
	@Override
	public void receiveClock(int p) {
		int pos = p%getMaxTicks();
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
			if (isAudible && pos==note.getTransformedPosStart(getMaxTicks(), getQuantizationIndex())) {
				note.setPlayed(true);
				MidiHandler.instance().sendNoteOnMidi(this, note.getTransformedNoteNumber(getTransposeIndex()), note.getVelocity());
			}
			if (pos==note.getTransformedPosEnd(getMaxTicks(), getQuantizationIndex()) && note.isPlayed()) {
				MidiHandler.instance().sendNoteOffMidi(this, note.getTransformedNoteNumber(getTransposeIndex()));
				note.setPlayed(false);
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
	public void receiveActive(boolean active, int p) {
		if (!active) {
			int pos = p%getMaxTicks();
			// find still uncompleted notes
			for (Note nr:getNotesList()) {
				if (!nr.isCompleted()) {
					nr.setPosEnd(pos);
				}
			}
			
		}
	}

	private Note findOverlappingNote(Note note, int p) {
		int pos = p%getMaxTicks();
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
	public void receiveCC(int cc, int val, int p) {
		if (isMidiInputOn()) {
			int pos = p%getMaxTicks();
			if (cc==1) {
				currentCC = val;
				ccList[pos] = val;
				overrideCC = true;
			}
		}
	}

	@Override
	public void receivePitchBend(int val, int p) {
		if (isMidiInputOn()) {
			int pos = p%getMaxTicks();
			currentPitchBend = val;
			pitchBendList[pos] = val;
			overridePitchBend = true;
		}
	}
	
	


	private int lengthQuarters = 8;
	private int maxTicks = lengthQuarters*TICK_COUNT_BASE;
	private int midiChannelIn = 0; // 0 - based
	private int midiChannelOut = 0;// 0 - based
	private boolean midiInputOn = true;
	private boolean isMuted = false;
	private boolean isSoloed = false;
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
	
	private Color colorNote;
	private Color colorNoteBright;
	private Color colorNoteSelected;
	private Color colorNoteBrightSelected;

	private List<LoopUpdateReceiver> loopUpdateReceivers = new CopyOnWriteArrayList<LoopUpdateReceiver>();
	private List<PerformanceReceiver> performanceReceivers = new CopyOnWriteArrayList<PerformanceReceiver>();
	private List<SettingsUpdateReceiver> settingsUpdateReceivers = new CopyOnWriteArrayList<SettingsUpdateReceiver>();

	private Note[] lastStarted = new Note[128];
	
	public static final int TICK_COUNT_BASE = 24;
	public static final int MAX_NUMBER_OF_QUARTERS = 64;
	
	public static enum QueuedState { NO_CHANGE, ON, OFF }

	@Override
	public void stateChange(boolean mute, boolean solo, QueuedState queuedMute, QueuedState queuedSolo) {
		
	}

}
