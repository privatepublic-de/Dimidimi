package de.privatepublic.midiutils;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MidiHandler {

	private static final Logger LOG = LoggerFactory.getLogger(MidiHandler.class);
	
	public static boolean ACTIVE = false;
	private ArrayList<MidiDeviceWrapper> outputDeviceList = new ArrayList<MidiDeviceWrapper>();
	private ArrayList<MidiDeviceWrapper> inputDeviceList = new ArrayList<MidiDeviceWrapper>();
	private int pos;
	private ShortMessage resetPositionMessage = new ShortMessage();
	private Session session;
	
	public MidiHandler(Session session, int pos) {
		
		this.session = session;
		this.pos = pos;
		
		List<String> prefInIds = Prefs.getPrefIdentifierList(Prefs.MIDI_IN_DEVICES);
		List<String> prefOutIds = Prefs.getPrefIdentifierList(Prefs.MIDI_OUT_DEVICES);
		
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
				return o1.getInfo().getVendor().compareTo(o2.getInfo().getVendor());
			}
		});
		Collections.sort(inputDeviceList, new Comparator<MidiDeviceWrapper>() {
			@Override
			public int compare(MidiDeviceWrapper o1, MidiDeviceWrapper o2) {
				return o1.getInfo().getVendor().compareTo(o2.getInfo().getVendor());
			}
		});
		session.emitSettingsUpdated();
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
				ACTIVE = false;
				session.emitActive(false, pos);
				pos = 0;
				sendMessage(resetPositionMessage);
				sendAllNotesOffMidi();
				break;
			case ShortMessage.START:
				LOG.info("Received START");
				ACTIVE = true;
				session.emitActive(true, pos);
				break;
			case ShortMessage.CONTINUE:
				LOG.info("Received CONTINUE");
				ACTIVE = true;
				session.emitActive(true, pos);
				break;
			case ShortMessage.TIMING_CLOCK:
				session.emitClock(pos);
				pos += session.getClockIncrement();
				pos = pos%session.getMaxTicks();
				break;
			}
			if (message instanceof ShortMessage && device.isActiveForInput() && session.isMidiInputOn()) {
				final ShortMessage msg = (ShortMessage)message;
				final int channel = msg.getChannel();
				if (channel==session.getMidiChannelIn()) {
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
		session.emitNoteOn(noteNumber, velocity, pos);
		sendNoteOnMidi(noteNumber, velocity);
	}

	private void noteOff(int noteNumber) {
		session.emitNoteOff(noteNumber, pos);
		sendNoteOffMidi(noteNumber);
	}

	public void sendNoteOnMidi(int noteNumber, int velocity) {
		if (session.isMidiOutputOn()) {
			try {
				ShortMessage message = new ShortMessage();
				message.setMessage(ShortMessage.NOTE_ON, session.getMidiChannelOut(), noteNumber, velocity);
				sendMessage(message);
			} catch (InvalidMidiDataException e) {
				e.printStackTrace();
			}
		}
	}

	public void sendNoteOffMidi(int noteNumber) {
		if (session.isMidiOutputOn()) {
			try {
				ShortMessage message = new ShortMessage();
				message.setMessage(ShortMessage.NOTE_OFF, session.getMidiChannelOut(), noteNumber, 0);
				sendMessage(message);
			} catch (InvalidMidiDataException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendMessage(MidiMessage msg) {
		for (MidiDeviceWrapper d:outputDeviceList) {
			if (d.isActiveForOutput()) {
				d.getReceiver().send(msg, -1);
			}
		}
	}
	
	
	public void sendAllNotesOffMidi() {
		sendAllNotesOffMidi(session.getMidiChannelOut());
	}
	
	
	public void sendAllNotesOffMidi(int channel) {
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
	
	public int getPos() {
		return pos;
	}
	

}
