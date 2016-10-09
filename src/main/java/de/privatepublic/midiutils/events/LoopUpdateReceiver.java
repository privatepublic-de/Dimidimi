package de.privatepublic.midiutils.events;


public interface LoopUpdateReceiver extends DimidimiEventReceiver {

	public void loopUpdated();
	public void refreshLoopDisplay();
	
	
}
