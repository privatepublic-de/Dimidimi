package de.privatepublic.midiutils;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageContainer {

	private List<Note> notes;
	private int transpose;
	private int quantization;
	private int length;
	private int midiChannelIn;
	private int midiChannelOut;
	private boolean midiChannelInActive;
	private boolean midiChannelOutActive;
	private Map<String, Integer> windowPos;
	
	public StorageContainer() {
		
	}
	
	
	public StorageContainer(Session session) {
		this.notes = session.getNotesList();
		this.transpose = session.getTransposeIndex();
		this.quantization = session.getQuantizationIndex();
		this.length = session.getLengthQuarters();
		this.midiChannelIn = session.getMidiChannelIn();
		this.midiChannelOut = session.getMidiChannelOut();
		this.midiChannelInActive = session.isMidiInputOn();
		this.midiChannelOutActive = session.isMidiOutputOn();
		this.windowPos = new HashMap<String, Integer>();
		Rectangle bounds = session.getWindow().getScreenPosition();
		windowPos.put("x", bounds.x);
		windowPos.put("y", bounds.y);
		windowPos.put("w", bounds.width);
		windowPos.put("h", bounds.height);
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


	public boolean isMidiChannelInActive() {
		return midiChannelInActive;
	}


	public void setMidiChannelInActive(boolean midiChannelInActive) {
		this.midiChannelInActive = midiChannelInActive;
	}


	public boolean isMidiChannelOutActive() {
		return midiChannelOutActive;
	}


	public void setMidiChannelOutActive(boolean midiChannelOutActive) {
		this.midiChannelOutActive = midiChannelOutActive;
	}


	public Map<String, Integer> getWindowPos() {
		return windowPos;
	}


	public void setWindowPos(Map<String, Integer> windowPos) {
		this.windowPos = windowPos;
	}
	
	
	
}
