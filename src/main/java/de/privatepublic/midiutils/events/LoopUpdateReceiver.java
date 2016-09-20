package de.privatepublic.midiutils.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import de.privatepublic.midiutils.Note;

public interface LoopUpdateReceiver extends DimidimiEventReceiver {

	public void loopUpdated(List<Note> list);
	public void refreshLoopDisplay();
	
	
}
