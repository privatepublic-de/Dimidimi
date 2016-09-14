package de.privatepublic.midiutils.events;

import de.privatepublic.midiutils.NoteRun;

public interface ClearReceiver {

	public void clearPattern();
	public void clearNote(NoteRun note);
	
}
