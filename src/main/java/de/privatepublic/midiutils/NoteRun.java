package de.privatepublic.midiutils;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class NoteRun {

	private int posStart;
	private int posEnd = -1;
	private int noteNumber;
	private int velocity;
	private boolean isCompleted;
	private boolean isPlayed;

	private int playedNoteNumber;
	private int qoffset = 0;
	
	public static int APPLY_QUANTIZATION = 0;
	public static int APPLY_TRANSPOSE = 13;
	
	public NoteRun() {
		
	}
	
	public NoteRun(int noteNumber, int velocity, int posStart) {
		this.noteNumber = noteNumber;
		this.velocity = velocity;
		this.posStart = posStart;
	}
	
	public NoteRun(NoteRun other, int posOffset) { // clone constructor
		this.velocity = other.velocity;
		this.posStart = other.posStart + posOffset;
		this.posEnd = other.posEnd + posOffset;
		this.noteNumber = other.noteNumber;
		this.isCompleted = other.isCompleted;
	}
	
	@JsonIgnore
	public int getTransformedPosEnd() {
		return (posEnd + qoffset)%MidiHandler.instance().getMaxTicks();
	}
	public void setPosEnd(int posEnd) {
		this.posEnd = posEnd;
		isCompleted = true;
	}
	
	public int getPosEnd() {
		return posEnd;
	}
	
	public int getPosStart() {
		return posStart;
	}
	
	public void setPosStart(int posStart) {
		this.posStart = posStart;
	}
	
	@JsonIgnore
	public int getTransformedPosStart() {
		if (APPLY_QUANTIZATION>0) {
			int stepsize = Q_STEPS[APPLY_QUANTIZATION];
			int offset = posStart % stepsize;
			if (offset<stepsize/2) {
				qoffset = -offset;
			}
			else {
				qoffset = (stepsize-offset);
			}
		}
		else {
			qoffset = 0;
		}
		return (posStart + qoffset)%MidiHandler.instance().getMaxTicks();
	}
	
	@JsonIgnore
	public int getTransformedNoteNumber() {
		int result = noteNumber+T_STEPS[APPLY_TRANSPOSE];
		return result<0?0:(result>127?127:result);
	}
	
	
	public int getNoteNumber() {
		return noteNumber;
	}
	
	public void setNoteNumber(int notenumber) {
		this.noteNumber = notenumber;;
	}
	
	public int getVelocity() {
		return velocity;
	}
	
	public void setVelocity(int velocity) {
		this.velocity = velocity;
	}
	
	@JsonIgnore
	public boolean isCompleted() {
		return isCompleted;
	}
	
	@JsonIgnore
	public boolean isPlayed() {
		return isPlayed;
	}

	public void setPlayed(boolean isPlayed) {
		this.isPlayed = isPlayed;
		if (isPlayed) {
			playedNoteNumber = getTransformedNoteNumber();
		}
	}

	@JsonIgnore
	public int getPlayedNoteNumber() {
		return playedNoteNumber;
	}
	
	@JsonIgnore
	public String getNoteName() {
		return NOTE_NAMES[getTransformedNoteNumber()%12]+(getTransformedNoteNumber()/12-1);
	}
	
	
	@Override
	public String toString() {
		return "<NoteRun #"+noteNumber+", "+velocity+">";
	}
	
	private static final int[] Q_STEPS = new int[]{ 0, 48, 24, 12, 6, 3, 48/3, 24/3, 12/3};
	private static final int[] T_STEPS = new int[]{ 24, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -24};
	private static final String[] NOTE_NAMES = new String[] {"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};
	
}
