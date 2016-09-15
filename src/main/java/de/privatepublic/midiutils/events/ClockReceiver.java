package de.privatepublic.midiutils.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public interface ClockReceiver { // TODO combine clock and note receiver to one midieventreceiver!

	public void receiveClock(int pos);
	public void receiveActive(boolean active, int pos);
	
	
	public static class Dispatcher {

		private static final List<ClockReceiver> receivers = new CopyOnWriteArrayList<ClockReceiver>();

		public static void register(ClockReceiver receiver) {
			if (!receivers.contains(receiver)) {
				receivers.add(receiver);
			}
		}

		public static void sendClock(int pos) {
			for (ClockReceiver receiver: receivers) {
				receiver.receiveClock(pos);
			}
		}

		public static void sendActive(boolean active, int pos) {
			for (ClockReceiver receiver: receivers) {
				receiver.receiveActive(active, pos);
			}
		}

	}
	
	
}
