package de.privatepublic.midiutils.events;


public interface NotesUpdatedReceiver extends DimidimiEventReceiver {

	public void onNotesUpdated();
	public void onRefreshLoopDisplay();
	
}
