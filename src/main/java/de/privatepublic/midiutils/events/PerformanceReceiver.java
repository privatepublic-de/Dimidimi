package de.privatepublic.midiutils.events;

import de.privatepublic.midiutils.Loop;

public interface PerformanceReceiver extends DimidimiEventReceiver { 

	public void onNoteOn(int noteNumber, int velocity, int pos);
	public void onNoteOff(int notenumber, int pos);
	public void onClock(int pos);
	public void onActivityChange(boolean active, int pos);
	public void onReceiveCC(int cc, int val, int pos);
	public void onReceivePitchBend(int val, int pos);
	public void onStateChange(boolean mute, boolean solo, Loop.QueuedState queuedMute, Loop.QueuedState queuedSolo);
	
}
