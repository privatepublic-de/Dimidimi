package de.privatepublic.midiutils.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public interface PerformanceReceiver { 

	public void noteOn(int noteNumber, int velocity, int pos);
	public void noteOff(int notenumber, int pos);
	public void receiveClock(int pos);
	public void receiveActive(boolean active, int pos);


	public static class Dispatcher {

		private static final List<PerformanceReceiver> receivers = new CopyOnWriteArrayList<PerformanceReceiver>();

		public static void register(PerformanceReceiver receiver) {
			if (!receivers.contains(receiver)) {
				receivers.add(receiver);
			}
		}

		public static void sendNoteOn(int noteNumber, int velocity, int pos) {
			for (PerformanceReceiver receiver: receivers) {
				receiver.noteOn(noteNumber, velocity, pos);
			}
		}

		public static void sendNoteOff(int notenumber, int pos) {
			for (PerformanceReceiver receiver: receivers) {
				receiver.noteOff(notenumber, pos);;
			}
		}
		
		public static void sendClock(int pos) {
			for (PerformanceReceiver receiver: receivers) {
				receiver.receiveClock(pos);
			}
		}

		public static void sendActive(boolean active, int pos) {
			for (PerformanceReceiver receiver: receivers) {
				receiver.receiveActive(active, pos);
			}
		}

	}


}
