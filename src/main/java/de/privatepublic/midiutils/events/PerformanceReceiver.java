package de.privatepublic.midiutils.events;

import de.privatepublic.midiutils.Session;

public interface PerformanceReceiver extends DimidimiEventReceiver { 

	public void noteOn(int noteNumber, int velocity, int pos);
	public void noteOff(int notenumber, int pos);
	public void receiveClock(int pos);
	public void receiveActive(boolean active, int pos);
	public void receiveCC(int cc, int val, int pos);
	public void receivePitchBend(int val, int pos);
	public void stateChange(boolean mute, boolean solo, Session.QueuedState queuedMute, Session.QueuedState queuedSolo);
	
}
