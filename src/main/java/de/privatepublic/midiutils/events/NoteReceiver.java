package de.privatepublic.midiutils.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public interface NoteReceiver { // TODO combine clock and note receiver to one midieventreceiver!

	public void noteOn(int noteNumber, int velocity, int pos);
	public void noteOff(int notenumber, int pos);


	public static class Dispatcher {

		private static final List<NoteReceiver> receivers = new CopyOnWriteArrayList<NoteReceiver>();

		public static void register(NoteReceiver receiver) {
			if (!receivers.contains(receiver)) {
				receivers.add(receiver);
			}
		}

		public static void sendNoteOn(int noteNumber, int velocity, int pos) {
			for (NoteReceiver receiver: receivers) {
				receiver.noteOn(noteNumber, velocity, pos);
			}
		}

		public static void sendNoteOff(int notenumber, int pos) {
			for (NoteReceiver receiver: receivers) {
				receiver.noteOff(notenumber, pos);;
			}
		}

	}


}
