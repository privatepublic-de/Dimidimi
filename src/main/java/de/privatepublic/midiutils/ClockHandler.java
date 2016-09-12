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

import de.privatepublic.midiutils.events.ClearReceiver;
import de.privatepublic.midiutils.events.ClockReceiver;
import de.privatepublic.midiutils.events.Event;
import de.privatepublic.midiutils.events.NoteReceiver;
import de.privatepublic.midiutils.events.StorageReceiver;

public class ClockHandler implements ClockReceiver, NoteReceiver, ClearReceiver, StorageReceiver {
	
	private static final Logger LOG = LoggerFactory.getLogger(ClockHandler.class);
	
	private List<NoteRun> cycleList = new ArrayList<NoteRun>();
	private NoteRun[] lastStarted = new NoteRun[128];
	private boolean recordActive = false;
	
	public ClockHandler() {
		Event.registerClearReceiver(this);
		Event.registerStorageReceiver(this);
	}
	
	public void receiveNoteOn(int noteNumber, int velocity, int pos) {
		if (recordActive) {
			NoteRun dc = new NoteRun(noteNumber, velocity, pos);
			lastStarted[noteNumber] = dc;
			synchronized (cycleList) {
				cycleList.add(dc);
			}
			Event.sendLoopUpdate(cycleList);
		}
	}
	
	public void receiveNoteOff(int notenumber, int pos) {
		if (recordActive) {
			NoteRun reference = lastStarted[notenumber];
			if (reference!=null) {
				reference.setPosEnd(pos);
			}
			Event.sendLoopUpdate(cycleList);
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
		Event.sendLoopUpdate(cycleList);
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
	}

	@Override
	public void loadRequest(File file) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		StorageContainer data = mapper.readValue(file, StorageContainer.class);
		clearPattern();
		synchronized (cycleList) {
			for (NoteRun n: data.getNotes()) {
				cycleList.add(n);
			}
			NoteRun.APPLY_QUANTIZATION = data.getQuantization();
			NoteRun.APPLY_TRANSPOSE = data.getTranspose();
			MidiHandler.instance().updateSettings(MidiHandler.instance().getMidiChannelIn(), MidiHandler.instance().getMidiChannelOut(), data.getLength());
		}
		Event.sendLoopUpdate(cycleList);
		Event.sendSettingsUpdate();
		MidiHandler.instance().sendAllNotesOff();
	}
	
	
	
}
