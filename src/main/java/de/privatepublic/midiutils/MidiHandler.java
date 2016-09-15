package de.privatepublic.midiutils;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.privatepublic.midiutils.Prefs.Identifiable;
import de.privatepublic.midiutils.events.ClockReceiver;
import de.privatepublic.midiutils.events.NoteReceiver;

public class MidiHandler {

	private static final Logger LOG = LoggerFactory.getLogger(MidiHandler.class);
	
	
	private static MidiHandler INSTANCE = new MidiHandler();

	public static MidiHandler instance() {
		return INSTANCE;
	}
	
	
	
	public static class MidiDeviceWrapper implements Identifiable {
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
	
	private ArrayList<MidiDeviceWrapper> outputDeviceList = new ArrayList<MidiDeviceWrapper>();
	private ArrayList<MidiDeviceWrapper> inputDeviceList = new ArrayList<MidiDeviceWrapper>();
	private int settingsChannelIn = 5 -1;
	private int settingsChannelOut = 3 -1;
	private int settingsNumberQuarters = 8;
	
	
	private MidiHandler() {
		
		List<String> prefInIds = Prefs.getPrefIdentifierList(Prefs.MIDI_IN_DEVICES);
		List<String> prefOutIds = Prefs.getPrefIdentifierList(Prefs.MIDI_OUT_DEVICES);
		
		settingsChannelIn  = Prefs.get(Prefs.MIDI_IN_CHANNEL, settingsChannelIn);
		settingsChannelOut  = Prefs.get(Prefs.MIDI_OUT_CHANNEL, settingsChannelOut);
		
		ppqdiv = Prefs.get(Prefs.MIDI_48PPQ, 2);
		
		MidiDevice device;
		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
		try {
			resetPositionMessage.setMessage(ShortMessage.SONG_POSITION_POINTER, 0, 0);
		} catch (InvalidMidiDataException e1) {
			e1.printStackTrace();
		}
		LOG.info("Opening MIDI devices ...");
		for (int i=0; i<infos.length; i++) {
			try {
				device = MidiSystem.getMidiDevice(infos[i]);
				String info = infos[i].getName();
				if ("Gervill".equalsIgnoreCase(info)) {
					continue;
				}
				
				if (device.getMaxReceivers()!=0) {
					// output device
					MidiDeviceWrapper dev = new MidiDeviceWrapper(infos[i], device);
					if (!outputDeviceList.contains(dev)) {
						outputDeviceList.add(dev);
						if (prefOutIds.contains(dev.getIdentifier())) {
							dev.setActiveForOutput(true);
						}
					}
					device.open();
					LOG.info("Output device {}", infos[i].getDescription());
				}
				if (device.getMaxTransmitters()!=0) {
					// input device
					MidiDeviceWrapper dev = new MidiDeviceWrapper(infos[i], device);
					if (!inputDeviceList.contains(dev)) {
						inputDeviceList.add(dev);
						if (prefInIds.contains(dev.getIdentifier())) {
							dev.setActiveForInput(true);
						}
					}
					Transmitter trans = device.getTransmitter();
					Receiver receiver = new MIDIReceiver(dev);
					trans.setReceiver(receiver);
					device.open();
					LOG.info("Input device {}", infos[i].getDescription());
				}
				
			} catch (MidiUnavailableException e) {
				LOG.warn("Error opening device {}", infos[i].getDescription());
			}
		}
		Collections.sort(outputDeviceList, new Comparator<MidiDeviceWrapper>() {
			@Override
			public int compare(MidiDeviceWrapper o1, MidiDeviceWrapper o2) {
				return o1.info.getVendor().compareTo(o2.info.getVendor());
			}
		});
		Collections.sort(inputDeviceList, new Comparator<MidiDeviceWrapper>() {
			@Override
			public int compare(MidiDeviceWrapper o1, MidiDeviceWrapper o2) {
				return o1.info.getVendor().compareTo(o2.info.getVendor());
			}
		});
		ClockHandler clockHandler = new ClockHandler();
		ClockReceiver.Dispatcher.register(clockHandler);
		NoteReceiver.Dispatcher.register(clockHandler);
	}
	
	public void storeSelectedOutDevices() {
		ArrayList<MidiDeviceWrapper> list = new ArrayList<MidiDeviceWrapper>();
		for (MidiDeviceWrapper dev:getOutputDevices()) {
			if (dev.isActiveForOutput()) {
				list.add(dev);
			}
		}
		Prefs.putPrefIdentfierList(Prefs.MIDI_OUT_DEVICES, list);
	}
	
	public void storeSelectedInDevices() {
		ArrayList<MidiDeviceWrapper> list = new ArrayList<MidiDeviceWrapper>();
		for (MidiDeviceWrapper dev:getInputDevices()) {
			if (dev.isActiveForInput()) {
				list.add(dev);
			}
		}
		Prefs.putPrefIdentfierList(Prefs.MIDI_IN_DEVICES, list);
	}

	private int pos_in;
	private int pos;
	private int ppqdiv = 1;
//	List<Receiver> receivers = new ArrayList<Receiver>();
	ShortMessage resetPositionMessage = new ShortMessage();
	
	public List<MidiDeviceWrapper> getOutputDevices() {
		return outputDeviceList;
	}
	
	public List<MidiDeviceWrapper> getInputDevices() {
		return inputDeviceList;
	}

	private class MIDIReceiver implements Receiver { 
		
		private MidiDeviceWrapper device;
		
		public MIDIReceiver(MidiDeviceWrapper device) {
			this.device = device;
		}
		
		@Override
		public void send(MidiMessage message, long timeStamp) {
			final int status = message.getStatus();
			switch(status) {
			case ShortMessage.STOP:
				LOG.info("Received STOP - Setting song position zero");
				ClockReceiver.Dispatcher.sendActive(false, pos);
				pos = 0;
				pos_in = 0;
				sendMessage(resetPositionMessage);
				sendAllNotesOff();
				break;
			case ShortMessage.START:
				LOG.info("Received START");
				ClockReceiver.Dispatcher.sendActive(true, pos);
				break;
			case ShortMessage.CONTINUE:
				LOG.info("Received CONTINUE");
				ClockReceiver.Dispatcher.sendActive(true, pos);
				break;
			case ShortMessage.TIMING_CLOCK:
				ClockReceiver.Dispatcher.sendClock(pos);
				pos_in++;
				pos = (pos_in/ppqdiv)%getMaxTicks();
				break;
			}
			if (message instanceof ShortMessage && device.isActiveForInput) {
				final ShortMessage msg = (ShortMessage)message;
				final int channel = msg.getChannel();
				if (channel==settingsChannelIn) {
					int command = msg.getCommand();
					final int data1 = msg.getData1();
					final int data2 = msg.getData2();
					switch(command) {
					case ShortMessage.NOTE_ON:
						if (data2==0) {
							noteOff(data1);
						}
						else {
							noteOn(data1, data2);
						}
						break;
					case ShortMessage.NOTE_OFF:
						noteOff(data1);
						break;
					}
				}
			}
		}

		@Override
		public void close() {

		}
	}

	private void noteOn(int noteNumber, int velocity) {
		NoteReceiver.Dispatcher.sendNoteOn(noteNumber, velocity, pos);
		sendNoteOn(noteNumber, velocity);
	}

	private void noteOff(int noteNumber) {
		NoteReceiver.Dispatcher.sendNoteOff(noteNumber, pos);
		sendNoteOff(noteNumber);
	}

	public void sendNoteOn(int noteNumber, int velocity) {
		try {
			ShortMessage message = new ShortMessage();
			message.setMessage(ShortMessage.NOTE_ON, settingsChannelOut, noteNumber, velocity);
			sendMessage(message);
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	public void sendNoteOff(int noteNumber) {
		try {
			ShortMessage message = new ShortMessage();
			message.setMessage(ShortMessage.NOTE_OFF, settingsChannelOut, noteNumber, 0);
			sendMessage(message);
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	private void sendMessage(MidiMessage msg) {
//		if (selectedDevice!=null) {
//			selectedDevice.getReceiver().send(msg, -1);
//		}
		for (MidiDeviceWrapper d:outputDeviceList) {
			if (d.isActiveForOutput()) {
				d.getReceiver().send(msg, -1);
			}
		}
	}
	
	
	public void sendAllNotesOff() {
		sendAllNotesOff(settingsChannelOut);
	}
	
	
	private void sendAllNotesOff(int channel) {
		for (int i=0;i<128;i++) {
			try {
				ShortMessage message = new ShortMessage();
				message.setMessage(ShortMessage.NOTE_OFF, channel, i, 0);
				sendMessage(message);
			} catch (InvalidMidiDataException e) {
				e.printStackTrace();
			}
		}
	}
	
	public int getMaxTicks() {
		return getNumberQuarters() * 24;
	}
	
	public int getNumberQuarters() {
		return settingsNumberQuarters;
	}
	
	public int getMidiChannelOut() {
		return settingsChannelOut;
	}
	
	public int getMidiChannelIn() {
		return settingsChannelIn;
	}
	
	public void set48PPQ(boolean ppq48) {
		ppqdiv = ppq48?2:1;
	}
	
	public int getPPQDiv() {
		return ppqdiv;
	}

//	public void updateSettings(int midiIn, int midiOut, int numberQuarters) {
//		if (midiIn!=settingsChannelIn || midiOut!=settingsChannelOut) {
//			int oldOut = settingsChannelOut;
//			settingsChannelIn = midiIn;
//			settingsChannelOut = midiOut;
//			sendAllNotesOff(oldOut);
//			Prefs.put(Prefs.MIDI_IN_CHANNEL, settingsChannelIn);
//			Prefs.put(Prefs.MIDI_OUT_CHANNEL, settingsChannelOut);
//		}
//		if (numberQuarters!=settingsNumberQuarters) {
//			settingsNumberQuarters = numberQuarters;
//			//ClockReceiver.Dispatcher.sendClock(pos);
//		}
//	}
	
	public void setMidiChannelIn(int midiChannel) {
		settingsChannelIn = midiChannel;
		Prefs.put(Prefs.MIDI_IN_CHANNEL, settingsChannelIn);
	}
	
	public void setMidiChannelOut(int midiChannel) {
		int oldOut = settingsChannelOut;
		settingsChannelOut = midiChannel;
		sendAllNotesOff(oldOut);
		Prefs.put(Prefs.MIDI_OUT_CHANNEL, settingsChannelOut);
	}
	
	
	public void updateLength(int numberQuarters) {
		settingsNumberQuarters = numberQuarters;
		//ClockReceiver.Dispatcher.sendClock(pos);
	}
	
	
	

}
