package de.privatepublic.midiutils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class Note {

	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(Note.class);
	
	public static String getName(int number) { 
		return NOTE_NAMES[number%12];
	}
	
	public static String getDrumName(int number) { 
		if (number<35 || number>49) {
			return "?? " + NOTE_NAMES[number%12];	
		} else {
			return DRUM_NAMES[number-35];
		}
	}
	
	private int posStart;
	private int posEnd = -1;
	private int noteNumber;
	private int velocity;
	private boolean isCompleted;
	private boolean isPlayed;
	private int playedNoteNumber;
	private int originPosStart;
	private int originPosEnd;
	private int originNoteNumber;
	
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
	public int getPosEnd(TransformationProvider tp) {
		int quantizationOffset = getQuantizationOffset(tp);
		return (posEnd + quantizationOffset)%tp.getMaxTicks();
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
	public int getPosStart(TransformationProvider tp) {
		int quantizationOffset = getQuantizationOffset(tp);
		return (posStart + quantizationOffset)%tp.getMaxTicks();
	}
	
	private int getQuantizationOffset(TransformationProvider tp) {
		if (tp.getQuantizationIndex()>0) {
			int stepsize = TransformationProvider.Q_STEPS[tp.getQuantizationIndex()];
			int offset = posStart % stepsize;
			if (offset<stepsize/2) {
				return -offset;
			}
			else {
				return (stepsize-offset);
			}
		}
		else {
			return 0;
		}
	}
	
	@JsonIgnore
	public int getNoteNumber(TransformationProvider tp) {
		int result = noteNumber+TransformationProvider.T_STEPS[tp.getTransposeIndex()];
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

	public void setPlayed(int withNumber) {
		this.isPlayed = true;
		this.playedNoteNumber = withNumber;
	}
	
	public void setUnPlayed() {
		this.isPlayed = false;
	}
	
	@JsonIgnore
	public int getPlayedNoteNumber() {
		return playedNoteNumber;
	}
	
	@JsonIgnore
	public String getNoteName(TransformationProvider tp) {
		return NOTE_NAMES[getNoteNumber(tp)%12]+(getNoteNumber(tp)/12-1);
	}
	
	@Override
	public String toString() {
		return "<Note "+hashCode()+", "+"#"+noteNumber+", "+velocity+">";
	}
	
	public void stashOrigin() {
		originPosStart = posStart;
		originPosEnd = posEnd;
		originNoteNumber = noteNumber;
	}
	
	@JsonIgnore
	public int getOriginPosEnd() {
		return originPosEnd;
	}
	
	@JsonIgnore
	public int getOriginPosStart() {
		return originPosStart;
	}
	
	@JsonIgnore
	public int getOriginNoteNumber() {
		return originNoteNumber;
	}
	
	@JsonIgnore
	public boolean isOverlapping(Note n, TransformationProvider tp) {
		if (n.getNoteNumber(tp)==getNoteNumber(tp)) {
			int mystart = getPosStart(tp);
			if (mystart>=n.getPosStart(tp) && mystart<n.getPosEnd(tp)) {
				return true;
			}
		}
		return false;
	}
	
	private static final String[] NOTE_NAMES = new String[] {"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};
	private static final String[] DRUM_NAMES = new String[] {"BD0", "BD1", "Rim", "SD", "Cl1", "Cl2", "Cow", "HH", "Clv", "HH2", "TM1", "OH", "TM2", "TM3", "Cym", "TM4"};

	
	public static interface TransformationProvider {
		
		public static final int[] Q_STEPS = new int[]{ 0, 48, 24, 12, 6, 3, 48/3, 24/3, 12/3};
		public static final int[] T_STEPS = new int[]{ 24, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -24};
		
		public static final String[] QUANTIZE_LABEL = new String[]{"free","1/2","1/4","1/8","1/16","1/32","1/4t", "1/8t", "1/16t"};
		public static final String[] TRANSPOSE_LABEL = new String[]{
				" ↑ 2 oct", " ↑ 1 oct"," ↑ 11"," ↑ 10"," ↑ 9"," ↑ 8"," ↑ 7"," ↑ 6"," ↑ 5"," ↑ 4"," ↑ 3"," ↑ 2"," ↑ 1",
				"↓trans↑",
				"↓ 1 ","↓ 2 ","↓ 3 ","↓ 4 ","↓ 5 ","↓ 6 ","↓ 7 ","↓ 8 ","↓ 9 ","↓ 10 ","↓ 11 ","↓ 1 oct ","↓ 2 oct "};
		
		public int getTransposeIndex();
		public int getQuantizationIndex();
		public int getMaxTicks();
	}
	
}
