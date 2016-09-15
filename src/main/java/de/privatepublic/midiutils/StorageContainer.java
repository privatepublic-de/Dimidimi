package de.privatepublic.midiutils;

import java.util.List;

public class StorageContainer {

	private List<Note> notes;
	private int transpose;
	private int quantization;
	private int length;
	
	public StorageContainer() {
		
	}
	
	public StorageContainer(List<Note> notes, int transpose, int quantization, int length) {
		this.notes = notes;
		this.transpose = transpose;
		this.quantization = quantization;
		this.length = length;
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
	
	
	
}
