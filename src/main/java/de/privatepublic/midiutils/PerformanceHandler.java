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

import de.privatepublic.midiutils.events.LoopUpdateReceiver;
import de.privatepublic.midiutils.events.ManipulateReceiver;
import de.privatepublic.midiutils.events.PerformanceReceiver;
import de.privatepublic.midiutils.events.SettingsUpdateReceiver;
import de.privatepublic.midiutils.events.StorageReceiver;

public class PerformanceHandler implements PerformanceReceiver, ManipulateReceiver, StorageReceiver {
	
	private static final Logger LOG = LoggerFactory.getLogger(PerformanceHandler.class);
	
	private List<Note> cycleList = new ArrayList<Note>();
	private Note[] lastStarted = new Note[128];
	private boolean recordActive = false;
	
	public PerformanceHandler() {
		ManipulateReceiver.Dispatcher.register(this);
		StorageReceiver.Dispatcher.register(this);
		PerformanceReceiver.Dispatcher.register(this);
	}
	
	public void noteOn(int noteNumber, int velocity, int pos) {
		if (recordActive) {
			Note dc = new Note(noteNumber, velocity, pos);
			lastStarted[noteNumber] = dc;
			synchronized (cycleList) {
				cycleList.add(dc);
			}
			LoopUpdateReceiver.Dispatcher.sendLoopUpdated(cycleList);
		}
	}
	
	public void noteOff(int notenumber, int pos) {
		if (recordActive) {
			Note reference = lastStarted[notenumber];
			if (reference!=null) {
				reference.setPosEnd(pos);
			}
			LoopUpdateReceiver.Dispatcher.sendLoopUpdated(cycleList);
		}
	}
	
	public void receiveClock(int pos) {
		synchronized (cycleList) {
			for (Note dc:cycleList) {
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
			for (Note dc:cycleList) {
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
			for (Note nr:cycleList) {
				if (!nr.isCompleted()) {
					nr.setPosEnd(pos);
				}
			}
			
		}
	}

	@Override
	public void saveRequest(File file) throws JsonGenerationException, JsonMappingException, IOException {
		StorageContainer data = new StorageContainer(cycleList, Note.APPLY_TRANSPOSE, Note.APPLY_QUANTIZATION, MidiHandler.instance().getNumberQuarters());		
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
			for (Note n: data.getNotes()) {
				cycleList.add(n);
			}
			Note.APPLY_QUANTIZATION = data.getQuantization();
			Note.APPLY_TRANSPOSE = data.getTranspose();
			MidiHandler.instance().updateLength(data.getLength());
		}
		LoopUpdateReceiver.Dispatcher.sendLoopUpdated(cycleList);
		SettingsUpdateReceiver.Dispatcher.sendSettingsUpdated();
		MidiHandler.instance().sendAllNotesOff();
	}

	@Override
	public void clearNote(Note note) {
		synchronized (cycleList) {
			cycleList.remove(note);
		}
		MidiHandler.instance().sendAllNotesOff();
		LoopUpdateReceiver.Dispatcher.sendLoopUpdated(cycleList);
	}

	@Override
	public void doublePattern() {
		synchronized(cycleList) {
			ArrayList<Note> addNotes = new ArrayList<Note>();
			int posOffset = MidiHandler.instance().getMaxTicks();
			MidiHandler.instance().updateLength(MidiHandler.instance().getNumberQuarters()*2);
			for (Note note: cycleList) {
				if (note.isCompleted()) {
					addNotes.add(new Note(note, posOffset));
				}
			}
			cycleList.addAll(addNotes);
		}
		LoopUpdateReceiver.Dispatcher.sendLoopUpdated(cycleList);
		SettingsUpdateReceiver.Dispatcher.sendSettingsUpdated();
	}
	
	
	
}