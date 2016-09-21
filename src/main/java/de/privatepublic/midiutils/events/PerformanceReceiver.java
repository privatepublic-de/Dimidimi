package de.privatepublic.midiutils.events;

public interface PerformanceReceiver extends DimidimiEventReceiver { 

	public void noteOn(int noteNumber, int velocity, int pos);
	public void noteOff(int notenumber, int pos);
	public void receiveClock(int pos);
	public void receiveActive(boolean active, int pos);


}
