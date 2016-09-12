package de.privatepublic.midiutils.events;

public interface NoteReceiver {

	public void receiveNoteOn(int noteNumber, int velocity, int pos);
	public void receiveNoteOff(int notenumber, int pos);
	
}
