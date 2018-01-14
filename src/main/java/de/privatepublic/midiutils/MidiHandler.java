package de.privatepublic.midiutils;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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

import uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider;

public class MidiHandler {

	private static final Logger LOG = LoggerFactory.getLogger(MidiHandler.class);
	
	public static boolean ACTIVE = false;
	
	private static final MidiHandler instance = new MidiHandler();
	
	public static MidiHandler instance() {
		return instance;
	}
	
	
	
	private ArrayList<MidiDeviceWrapper> outputDeviceList = new ArrayList<MidiDeviceWrapper>();
	private ArrayList<MidiDeviceWrapper> inputDeviceList = new ArrayList<MidiDeviceWrapper>();
	private int pos;
	private ShortMessage resetPositionMessage = new ShortMessage();
	
	private MIDIReceiver internalReceiver = new MIDIReceiver(new MidiDeviceWrapper());
	private ShortMessage msgClock = new ShortMessage();
	private ShortMessage msgStart = new ShortMessage();
	private ShortMessage msgStop = new ShortMessage();
	
	
	private MidiHandler() {

		try {
			msgClock.setMessage(ShortMessage.TIMING_CLOCK, 0 ,0);
			msgStart.setMessage(ShortMessage.START, 0, 0);
			msgStop.setMessage(ShortMessage.STOP, 0, 0);
		} catch (InvalidMidiDataException e1) {
			e1.printStackTrace();
		}
		
		
		List<String> prefInIds = Prefs.getPrefIdentifierList(Prefs.MIDI_IN_DEVICES);
		List<String> prefOutIds = Prefs.getPrefIdentifierList(Prefs.MIDI_OUT_DEVICES);
		
		MidiDevice device;
		MidiDevice.Info[] infos = CoreMidiDeviceProvider.getMidiDeviceInfo();
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
//					LOG.info("Output device {}", infos[i].getDescription());
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
//					LOG.info("Input device {}", infos[i].getDescription());
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
		LOG.info("Available MIDI devices: {} in, {} out", inputDeviceList.size(), outputDeviceList.size());
	}
	
	private Timer internalClockTimer;
	
	public void startInternalClock(double bpm) {
		long interval = Math.round(((60000/bpm)/4)/6);
		boolean alreadyRunning = (internalClockTimer!=null);
		if (alreadyRunning) {
			internalClockTimer.cancel();
		}
		internalClockTimer = new Timer();
		if (!alreadyRunning) {
			internalReceiver.send(msgStart, 0);
			sendMessage(msgStart);
		}
		internalClockTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				internalReceiver.send(msgClock, 0);
				sendMessage(msgClock);
			}
		}, interval, interval);
	}
	
	public void setInteralClockSpeed(double bpm) {
		if (internalClockTimer!=null) { // if it's running, restart with new speed
			startInternalClock(bpm);
		}
	}
	
	public void stopInternalClock() {
		if (internalClockTimer!=null) {
			internalClockTimer.cancel();
			internalClockTimer = null;
		}
		internalReceiver.send(msgStop, 0);
		sendMessage(msgStop);
	}
	
	public void toggleInternalClock() {
		if (internalClockTimer!=null) {
			stopInternalClock();
		}
		else {
			startInternalClock(DiMIDImi.getControllerWindow().getBPM());
		}
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
				pos = 0;
				sendMessage(resetPositionMessage);
				break;
			case ShortMessage.START:
				LOG.info("Received START");
				ACTIVE = true;
				break;
			case ShortMessage.CONTINUE:
				LOG.info("Received CONTINUE");
				ACTIVE = true;
				break;
//			case ShortMessage.TIMING_CLOCK:
//				pos++;
//				break;
			}
			for (Loop loop:Loop.getLoops()) {
				switch(status) {
				case ShortMessage.STOP:
					sendAllNotesOffMidi(loop, false);
					loop.triggerActivityChange(false, pos);
					loop.triggerRefreshLoopDisplay();
					break;
				case ShortMessage.START:
					loop.triggerActivityChange(true, pos);
					break;
				case ShortMessage.CONTINUE:
					loop.triggerActivityChange(true, pos);
					break;
				case ShortMessage.TIMING_CLOCK:
					if (ACTIVE) {
						loop.triggerClock(pos%loop.getMaxTicks());
					}
					break;
				}
				if (message instanceof ShortMessage && device.isActiveForInput() && loop.isRecordOn()) {
					final ShortMessage msg = (ShortMessage)message;
					final int channel = msg.getChannel();
					if (channel==loop.getMidiChannelIn()) {
						int command = msg.getCommand();
						final int data1 = msg.getData1();
						final int data2 = msg.getData2();
						switch(command) {
						case ShortMessage.NOTE_ON:
							if (data2==0) {
								noteOff(loop, data1);
							}
							else {
								noteOn(loop, data1, data2);
							}
							break;
						case ShortMessage.NOTE_OFF:
							noteOff(loop, data1);
							break;
						case ShortMessage.CONTROL_CHANGE:
							loop.triggerReceiveCC(data1, data2, pos);
							break;
						case ShortMessage.PITCH_BEND:
							int val = ((data1 & 0x7f) + ((data2 & 0x7f)<<7)) - 0x2000;
							loop.triggerReceivePitchBend(val, pos);
							break;
						}
					}
				}
			}
			if (status==ShortMessage.TIMING_CLOCK) {
				pos++;
			}
		}

		@Override
		public void close() {

		}
	}

	private void noteOn(Loop loop, int noteNumber, int velocity) {
		loop.triggerNoteOn(noteNumber, velocity, pos);
		sendNoteOnMidi(loop, noteNumber, velocity);
	}

	private void noteOff(Loop loop, int noteNumber) {
		loop.triggerNoteOff(noteNumber, pos);
		sendNoteOffMidi(loop, noteNumber);
	}

	public void sendNoteOnMidi(Loop loop, int noteNumber, int velocity) {
		try {
			ShortMessage message = new ShortMessage();
			message.setMessage(ShortMessage.NOTE_ON, loop.getMidiChannelOut(), noteNumber, velocity);
			sendMessage(message);
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	public void sendNoteOffMidi(Loop loop, int noteNumber) {
		try {
			ShortMessage message = new ShortMessage();
			message.setMessage(ShortMessage.NOTE_OFF, loop.getMidiChannelOut(), noteNumber, 0);
			sendMessage(message);
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}
	
	public void sendCC(Loop loop, int val) {
		try {
			ShortMessage message = new ShortMessage();
			message.setMessage(ShortMessage.CONTROL_CHANGE, loop.getMidiChannelOut(), 1, val);
			sendMessage(message);
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}

	public void sendPitchBend(Loop loop, int val) {
		try {
			ShortMessage message = new ShortMessage();
			val = Math.min(val + 0x2000, 0x3fff);
			message.setMessage(ShortMessage.PITCH_BEND, loop.getMidiChannelOut(), val & 0x7f, (val>>7) & 0x7f);
			sendMessage(message);
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
	}
	

	private void sendMessage(MidiMessage msg) {
		for (MidiDeviceWrapper d:outputDeviceList) {
			if (d.isActiveForOutput()) {
				d.getReceiver().send(msg, -1);
			}
		}
	}
	
	
	public void sendAllNotesOffMidi(Loop loop) {
		sendAllNotesOffMidi(loop, loop.getMidiChannelOut(), false);
	}
	
	public void sendAllNotesOffMidi(Loop loop, boolean panic) {
		sendAllNotesOffMidi(loop, loop.getMidiChannelOut(), panic);
	}
	
	public void sendAllNotesOffMidi(Loop loop, int channel) {
		sendAllNotesOffMidi(loop, channel, false);
	}
	
	public void sendAllNotesOffMidi(Loop loop, int channel, boolean panic) {
		ShortMessage message = new ShortMessage();		
		try {
			for (Note note: loop.getNotesList()) {
				if (note.isPlayed()) {
					message.setMessage(ShortMessage.NOTE_OFF, channel, note.getPlayedNoteNumber(), 0);
					note.setUnPlayed();
					sendMessage(message);
				}
			}
			message.setMessage(ShortMessage.CONTROL_CHANGE, channel, 1, 0);
			sendMessage(message);
			message.setMessage(ShortMessage.PITCH_BEND, channel, 0, 0x40);
			sendMessage(message);
			if (panic) {
				message.setMessage(ShortMessage.CONTROL_CHANGE, channel, 123, 0);
				sendMessage(message);
				message.setMessage(ShortMessage.CONTROL_CHANGE, channel, 120, 0);
				sendMessage(message);
				message.setMessage(ShortMessage.CONTROL_CHANGE, channel, 121, 0);
				sendMessage(message);
			}
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
		}
		
	}
	
	public int getPos() {
		return pos;
	}

	
}
