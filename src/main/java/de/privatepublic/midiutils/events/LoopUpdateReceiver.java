package de.privatepublic.midiutils.events;

import java.util.List;

import de.privatepublic.midiutils.NoteRun;

public interface LoopUpdateReceiver {

	public void loopUpdated(List<NoteRun> list);
	public void refreshLoopDisplay();
	
}
