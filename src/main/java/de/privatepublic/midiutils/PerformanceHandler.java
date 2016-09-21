package de.privatepublic.midiutils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.privatepublic.midiutils.events.PerformanceReceiver;

public class PerformanceHandler implements PerformanceReceiver { 
	
	private static final Logger LOG = LoggerFactory.getLogger(PerformanceHandler.class);
	
	private Note[] lastStarted = new Note[128];
//	private boolean recordActive = false;
	private Session session;
	
	public PerformanceHandler(Session session) {
		this.session = session;
		session.registerAsReceiver(this);
	}
	
	@Override
	public void noteOn(int noteNumber, int velocity, int pos) {
		if (MidiHandler.ACTIVE) {
			Note dc = new Note(noteNumber, velocity, pos);
			lastStarted[noteNumber] = dc;
			session.getNotesList().add(dc);
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
		
			for (Note dc:session.getNotesList()) {
				if (!dc.isCompleted()) {
					continue;
				}
				if (pos==dc.getTransformedPosStart(session.getMaxTicks(), session.getQuantizationIndex())) {
					dc.setPlayed(true);
					session.getMidiHandler().sendNoteOn(dc.getTransformedNoteNumber(session.getTransposeIndex()), dc.getVelocity());
				}
				if (pos==dc.getTransformedPosEnd(session.getMaxTicks(), session.getQuantizationIndex())) {
					session.getMidiHandler().sendNoteOff(dc.getTransformedNoteNumber(session.getTransposeIndex()));
					dc.setPlayed(false);
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
