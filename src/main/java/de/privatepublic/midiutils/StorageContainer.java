package de.privatepublic.midiutils;

import java.util.List;

public class StorageContainer {

	private List<Note> notes;
	private int transpose;
	private int quantization;
	private int length;
	private int midiChannelIn;
	private int midiChannelOut;
	
	public StorageContainer() {
		
	}
	
	
	public StorageContainer(Session session) {
		this.notes = session.getNotesList();
		this.transpose = session.getTransposeIndex();
		this.quantization = session.getQuantizationIndex();
		this.length = session.getLengthQuarters();
		this.midiChannelIn = session.getMidiChannelIn();
		this.midiChannelOut = session.getMidiChannelOut();
	}

	public List<Note> getNotes() {
		return notes;
	}

	public void setNotes(List<Note> notes) {
		this.notes = notes;
	}

	public int getTranspose() {
		return transpose;
	}

	public void setTranspose(int transpose) {
		this.transpose = transpose;
	}

	public int getQuantization() {
		return quantization;
	}

	public void setQuantization(int quantization) {
		this.quantization = quantization;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}


	public int getMidiChannelIn() {
		return midiChannelIn;
	}


	public void setMidiChannelIn(int midiChannelIn) {
		this.midiChannelIn = midiChannelIn;
	}


	public int getMidiChannelOut() {
		return midiChannelOut;
	}


	public void setMidiChannelOut(int midiChannelOut) {
		this.midiChannelOut = midiChannelOut;
	}
	
	
	
}
