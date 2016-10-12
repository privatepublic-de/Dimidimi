package de.privatepublic.midiutils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;
import javax.sound.midi.MidiDevice.Info;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import de.privatepublic.midiutils.Prefs.Identifiable;

public class MidiDeviceWrapper implements Identifiable {
	private Info info;
	private MidiDevice device;
	private Receiver receiver;
	private Transmitter transmitter;
	private boolean isActiveForOutput;
	private boolean isActiveForInput;
	private String identifier;

	public MidiDeviceWrapper(Info info, MidiDevice device) {
		this.info = info;
		this.device = device;
		try {
			this.receiver = device.getReceiver();
		} catch (Exception e) {
			// has no receiver
		}
		try {
			transmitter = device.getTransmitter();
		} catch (MidiUnavailableException e) {
			// has no transmitter
		}
		getIdentifier();
	}
	
	@Override
	/**
	 * Human readable device description.
	 */
	public String toString() {
		return info.getDescription()+" ("+info.getName()+")";
	}
	
	public Info getInfo() {
		return info;
	}
	
	public String getIdentifier() {
		if (identifier==null) {
			try {
				MessageDigest md5 = MessageDigest.getInstance("MD5");
				identifier = (new HexBinaryAdapter()).marshal(md5.digest(toString().getBytes()));
			} catch (NoSuchAlgorithmException e) {
				// shouldn't happen;
			}
		}
		return identifier;
	}
	
	public MidiDevice getDevice() {
		return device;
	}
	
	public Receiver getReceiver() {
		return receiver;
	}
	
	public boolean isActiveForOutput() {
		return isActiveForOutput && receiver!=null;
	}

	public void setActiveForOutput(boolean isActiveForOutput) {
		this.isActiveForOutput = isActiveForOutput;
	}
	
	public boolean isActiveForInput() {
		return isActiveForInput && transmitter!=null;
	}

	public void setActiveForInput(boolean isActiveForInput) {
		this.isActiveForInput = isActiveForInput;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MidiDeviceWrapper) {
			return ((MidiDeviceWrapper)obj).device==this.device;
		}
		return false;
	}
	
}