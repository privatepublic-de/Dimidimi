package de.privatepublic.midiutils.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import de.privatepublic.midiutils.Note;

public interface ManipulateReceiver extends DimidimiEventReceiver {

	public void clearPattern();
	public void clearNote(Note note);
	public void doublePattern();
	
	
}
