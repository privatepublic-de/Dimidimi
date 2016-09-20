package de.privatepublic.midiutils;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.privatepublic.midiutils.events.ManipulateReceiver;
import de.privatepublic.midiutils.events.PerformanceReceiver;
import de.privatepublic.midiutils.events.StorageReceiver;

public class PerformanceHandler implements PerformanceReceiver, ManipulateReceiver, StorageReceiver { // TODO Storage and Manipulate go to session and not as events!
	
	private static final Logger LOG = LoggerFactory.getLogger(PerformanceHandler.class);
	
	private Note[] lastStarted = new Note[128];
	private boolean recordActive = false;
	private Session session;
	
	public PerformanceHandler(Session session) {
		this.session = session;
		session.registerAsReceiver(this);
	}
	
	@Override
	public void noteOn(int noteNumber, int velocity, int pos) {
		if (recordActive) {
			Note dc = new Note(noteNumber, velocity, pos);
			lastStarted[noteNumber] = dc;
			session.getNotesList().add(dc);
			session.emitLoopUpdated();
		}
	}
	
	@Override
	public void noteOff(int notenumber, int pos) {
		if (recordActive) {
			Note reference = lastStarted[notenumber];
			if (reference!=null) {
				reference.setPosEnd(pos);
			}
			session.emitLoopUpdated();
		}
	}
	
	@Override
	public void receiveClock(int pos) {
		
			for (Note dc:session.getNotesList()) {
				if (!dc.isCompleted()) {
					continue;
				}
				if (pos==dc.getTransformedPosStart(session.getMaxTicks())) {
					dc.setPlayed(true);
					session.getMidiHandler().sendNoteOn(dc.getTransformedNoteNumber(), dc.getVelocity());
				}
				if (pos==dc.getTransformedPosEnd(session.getMaxTicks())) {
					session.getMidiHandler().sendNoteOff(dc.getPlayedNoteNumber());
					dc.setPlayed(false);
				}
			}
		
	}

	@Override
	public void clearPattern() {
		for (Note dc:session.getNotesList()) {
			session.getMidiHandler().sendNoteOff(dc.getPlayedNoteNumber());
		}
		session.getNotesList().clear();

		session.emitLoopUpdated();
	}

	@Override
	public void receiveActive(boolean active, int pos) {
		recordActive = active;
		if (!recordActive) {
			// find still uncompleted notes
			for (Note nr:session.getNotesList()) {
				if (!nr.isCompleted()) {
					nr.setPosEnd(pos);
				}
			}
			
		}
	}

	@Override
	public void saveRequest(File file) throws JsonGenerationException, JsonMappingException, IOException {
		StorageContainer data = new StorageContainer(session.getNotesList(), Note.APPLY_TRANSPOSE, Note.APPLY_QUANTIZATION, session.getLengthQuarters());		
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
		
		for (Note n: data.getNotes()) {
			session.getNotesList().add(n);
		}
		Note.APPLY_QUANTIZATION = data.getQuantization();
		Note.APPLY_TRANSPOSE = data.getTranspose();
		session.setLengthQuarters(data.getLength());
		
		session.emitLoopUpdated();
		session.emitSettingsUpdated();
		session.getMidiHandler().sendAllNotesOff();
	}

	@Override
	public void clearNote(Note note) {
		session.getNotesList().remove(note);
		session.getMidiHandler().sendAllNotesOff();
		session.emitLoopUpdated();
	}

	@Override
	public void doublePattern() {
		
		ArrayList<Note> addNotes = new ArrayList<Note>();
		int posOffset = session.getMaxTicks();
		session.setLengthQuarters(session.getLengthQuarters()*2);
		for (Note note: session.getNotesList()) {
			if (note.isCompleted()) {
				addNotes.add(new Note(note, posOffset));
			}
		}
		session.getNotesList().addAll(addNotes);
		
		session.emitLoopUpdated();
		session.emitSettingsUpdated();
	}
	
	
	
}
