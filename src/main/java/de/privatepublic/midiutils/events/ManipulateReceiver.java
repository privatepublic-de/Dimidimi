package de.privatepublic.midiutils.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import de.privatepublic.midiutils.NoteRun;

public interface ManipulateReceiver {

	public void clearPattern();
	public void clearNote(NoteRun note);
	
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
		
		public static void sendClearNote(NoteRun note) {
			for (ManipulateReceiver receiver: receivers) {
				receiver.clearNote(note );
			}
		}
		
	}
	
	
}
