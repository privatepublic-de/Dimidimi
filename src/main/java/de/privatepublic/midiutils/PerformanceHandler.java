package de.privatepublic.midiutils;


import de.privatepublic.midiutils.events.PerformanceReceiver;

public class PerformanceHandler implements PerformanceReceiver { 
	
	private Note[] lastStarted = new Note[128];
	private Session session;
	
	public PerformanceHandler(Session session) {
		this.session = session;
		session.registerAsReceiver(this);
	}
	
	@Override
	public void noteOn(int noteNumber, int velocity, int pos) {
		if (MidiHandler.ACTIVE) {
			Note note = new Note(noteNumber, velocity, pos);
			lastStarted[noteNumber] = note;
			session.getNotesList().add(note);
			session.emitLoopUpdated();
		}
	}
	
	@Override
	public void noteOff(int notenumber, int pos) {
		if (MidiHandler.ACTIVE) {
			Note reference = lastStarted[notenumber];
			if (reference!=null) {
				reference.setPosEnd(pos);
			}
			session.emitLoopUpdated();
		}
	}
	
	@Override
	public void receiveClock(int pos) {
			for (Note note:session.getNotesList()) {
				if (!note.isCompleted()) {
					continue;
				}
				if (pos==note.getTransformedPosStart(session.getMaxTicks(), session.getQuantizationIndex())) {
					note.setPlayed(true);
					session.getMidiHandler().sendNoteOnMidi(note.getTransformedNoteNumber(session.getTransposeIndex()), note.getVelocity());
				}
				if (pos==note.getTransformedPosEnd(session.getMaxTicks(), session.getQuantizationIndex())) {
					session.getMidiHandler().sendNoteOffMidi(note.getTransformedNoteNumber(session.getTransposeIndex()));
					note.setPlayed(false);
				}
			}
	}


	@Override
	public void receiveActive(boolean active, int pos) {
		if (!active) {
			// find still uncompleted notes
			for (Note nr:session.getNotesList()) {
				if (!nr.isCompleted()) {
					nr.setPosEnd(pos);
				}
			}
			
		}
	}

	
	
}
