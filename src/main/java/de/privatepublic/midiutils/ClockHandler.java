package de.privatepublic.midiutils;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.privatepublic.midiutils.events.ClockReceiver;
import de.privatepublic.midiutils.events.LoopUpdateReceiver;
import de.privatepublic.midiutils.events.ManipulateReceiver;
import de.privatepublic.midiutils.events.NoteReceiver;
import de.privatepublic.midiutils.events.SettingsUpdateReceiver;
import de.privatepublic.midiutils.events.StorageReceiver;

public class ClockHandler implements ClockReceiver, NoteReceiver, ManipulateReceiver, StorageReceiver {
	
	private static final Logger LOG = LoggerFactory.getLogger(ClockHandler.class);
	
	private List<NoteRun> cycleList = new ArrayList<NoteRun>();
	private NoteRun[] lastStarted = new NoteRun[128];
	private boolean recordActive = false;
	
	public ClockHandler() {
		ManipulateReceiver.Dispatcher.register(this);
		StorageReceiver.Dispatcher.register(this);
	}
	
	public void noteOn(int noteNumber, int velocity, int pos) {
		if (recordActive) {
			NoteRun dc = new NoteRun(noteNumber, velocity, pos);
			lastStarted[noteNumber] = dc;
			synchronized (cycleList) {
				cycleList.add(dc);
			}
			LoopUpdateReceiver.Dispatcher.sendLoopUpdated(cycleList);
		}
	}
	
	public void noteOff(int notenumber, int pos) {
		if (recordActive) {
			NoteRun reference = lastStarted[notenumber];
			if (reference!=null) {
				reference.setPosEnd(pos);
			}
			LoopUpdateReceiver.Dispatcher.sendLoopUpdated(cycleList);
		}
	}
	
	public void receiveClock(int pos) {
		synchronized (cycleList) {
			for (NoteRun dc:cycleList) {
				if (!dc.isCompleted()) {
					continue;
				}
				if (pos==dc.getTransformedPosStart()) {
					dc.setPlayed(true);
					MidiHandler.instance().sendNoteOn(dc.getTransformedNoteNumber(), dc.getVelocity());
				}
				if (pos==dc.getTransformedPosEnd()) {
					MidiHandler.instance().sendNoteOff(dc.getPlayedNoteNumber());
					dc.setPlayed(false);
				}
			}
		}
	}

	@Override
	public void clearPattern() {
		synchronized (cycleList) {
			for (NoteRun dc:cycleList) {
				MidiHandler.instance().sendNoteOff(dc.getPlayedNoteNumber());
			}
			cycleList.clear();
		}
		LoopUpdateReceiver.Dispatcher.sendLoopUpdated(cycleList);
	}

	@Override
	public void receiveActive(boolean active, int pos) {
		recordActive = active;
		if (!recordActive) {
			// find still uncompleted notes
			for (NoteRun nr:cycleList) {
				if (!nr.isCompleted()) {
					nr.setPosEnd(pos);
				}
			}
			
		}
	}

	@Override
	public void saveRequest(File file) throws JsonGenerationException, JsonMappingException, IOException {
		StorageContainer data = new StorageContainer(cycleList, NoteRun.APPLY_TRANSPOSE, NoteRun.APPLY_QUANTIZATION, MidiHandler.instance().getNumberQuarters());		
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(file, data);
		LOG.info("Saved file {}", file.getPath());
	}

	@Override
	public void loadRequest(File file) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		StorageContainer data = mapper.readValue(file, StorageContainer.class);
		LOG.info("Loaded file {}", file.getPath());
		clearPattern();
		synchronized (cycleList) {
			for (NoteRun n: data.getNotes()) {
				cycleList.add(n);
			}
			NoteRun.APPLY_QUANTIZATION = data.getQuantization();
			NoteRun.APPLY_TRANSPOSE = data.getTranspose();
			MidiHandler.instance().updateLength(data.getLength());
		}
		LoopUpdateReceiver.Dispatcher.sendLoopUpdated(cycleList);
		SettingsUpdateReceiver.Dispatcher.sendSettingsUpdated();
		MidiHandler.instance().sendAllNotesOff();
	}

	@Override
	public void clearNote(NoteRun note) {
		synchronized (cycleList) {
			cycleList.remove(note);
		}
		MidiHandler.instance().sendAllNotesOff();
		LoopUpdateReceiver.Dispatcher.sendLoopUpdated(cycleList);
	}

	@Override
	public void doublePattern() {
		synchronized(cycleList) {
			ArrayList<NoteRun> addNotes = new ArrayList<NoteRun>();
			int posOffset = MidiHandler.instance().getMaxTicks();
			MidiHandler.instance().updateLength(MidiHandler.instance().getNumberQuarters()*2);
			for (NoteRun note: cycleList) {
				if (note.isCompleted()) {
					addNotes.add(new NoteRun(note, posOffset));
				}
			}
			cycleList.addAll(addNotes);
		}
		LoopUpdateReceiver.Dispatcher.sendLoopUpdated(cycleList);
		SettingsUpdateReceiver.Dispatcher.sendSettingsUpdated();
	}
	
	
	
}
