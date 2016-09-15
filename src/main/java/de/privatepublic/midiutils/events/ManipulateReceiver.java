package de.privatepublic.midiutils.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import de.privatepublic.midiutils.Note;

public interface ManipulateReceiver {

	public void clearPattern();
	public void clearNote(Note note);
	public void doublePattern();
	
	public static class Dispatcher {
		
		private static final List<ManipulateReceiver> receivers = new CopyOnWriteArrayList<ManipulateReceiver>();
		
		public static void register(ManipulateReceiver receiver) {
			if (!receivers.contains(receiver)) {
				receivers.add(receiver);
			}
		}
		
		public static void sendClearPattern() {
			for (ManipulateReceiver receiver: receivers) {
				receiver.clearPattern();
			}
		}
		
		public static void sendClearNote(Note note) {
			for (ManipulateReceiver receiver: receivers) {
				receiver.clearNote(note);
			}
		}
		
		public static void sendDoublePattern() {
			for (ManipulateReceiver receiver: receivers) {
				receiver.doublePattern();
			}
		}
		
	}
	
	
}
