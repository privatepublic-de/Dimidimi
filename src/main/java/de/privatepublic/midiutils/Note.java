package de.privatepublic.midiutils;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class Note {

	private int posStart;
	private int posEnd = -1;
	private int noteNumber;
	private int velocity;
	private boolean isCompleted;
	private boolean isPlayed;

	private int qoffset = 0;
	
	
	public Note() {
		
	}
	
	public Note(int noteNumber, int velocity, int posStart) {
		this.noteNumber = noteNumber;
		this.velocity = velocity;
		this.posStart = posStart;
	}
	
	public Note(Note other, int posOffset) { // clone constructor
		this.velocity = other.velocity;
		this.posStart = other.posStart + posOffset;
		this.posEnd = other.posEnd + posOffset;
		this.noteNumber = other.noteNumber;
		this.isCompleted = other.isCompleted;
	}
	
	@JsonIgnore
	public int getTransformedPosEnd(int maxTicks, int quantizationIndex) {
		return (posEnd + qoffset)%maxTicks;
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
	public int getTransformedPosStart(int maxTicks, int quantizationIndex) {
		if (quantizationIndex>0) {
			int stepsize = Q_STEPS[quantizationIndex];
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
		return (posStart + qoffset)%maxTicks;
	}
	
	@JsonIgnore
	public int getTransformedNoteNumber(int transposeIndex) {
		int result = noteNumber+T_STEPS[transposeIndex];
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
	}
	
	@JsonIgnore
	public String getNoteName(int transposeIndex) {
		return NOTE_NAMES[getTransformedNoteNumber(transposeIndex)%12]+(getTransformedNoteNumber(transposeIndex)/12-1);
	}
	
	@Override
	public String toString() {
		return "<NoteRun #"+noteNumber+", "+velocity+">";
	}
	
	public static String getConcreteNoteName(int number) { // TODO Name
		return NOTE_NAMES[number%12];
	}
	
	private static final int[] Q_STEPS = new int[]{ 0, 48, 24, 12, 6, 3, 48/3, 24/3, 12/3};
	private static final int[] T_STEPS = new int[]{ 24, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -24};
	// private static final String[] NOTE_NAMES = new String[] {"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};
	private static final String[] NOTE_NAMES = new String[] {"C","Db","D","Eb","E","F","Gb","G","Ab","A","Bb","B"};
	
}
