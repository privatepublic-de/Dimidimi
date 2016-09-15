package de.privatepublic.midiutils.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public interface SettingsUpdateReceiver {

	public void settingsUpdated();
	
	
	public static class Dispatcher {

		private static final List<SettingsUpdateReceiver> receivers = new CopyOnWriteArrayList<SettingsUpdateReceiver>();

		public static void register(SettingsUpdateReceiver receiver) {
			if (!receivers.contains(receiver)) {
				receivers.add(receiver);
			}
		}

		public static void sendSettingsUpdated() {
			for (SettingsUpdateReceiver receiver: receivers) {
				receiver.settingsUpdated();
			}
		}


	}
	
}
