package de.privatepublic.midiutils.events;

public interface ClockReceiver {

	public void receiveClock(int pos);
	public void receiveActive(boolean active, int pos);
	
}
