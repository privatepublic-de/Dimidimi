package de.privatepublic.midiutils.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import de.privatepublic.midiutils.Note;

public interface LoopUpdateReceiver {

	public void loopUpdated(List<Note> list);
	public void refreshLoopDisplay();
	
	
	public static class Dispatcher {

		private static final List<LoopUpdateReceiver> receivers = new CopyOnWriteArrayList<LoopUpdateReceiver>();

		public static void register(LoopUpdateReceiver receiver) {
			if (!receivers.contains(receiver)) {
				receivers.add(receiver);
			}
		}

		public static void sendLoopUpdated(List<Note> notelist) {
			for (LoopUpdateReceiver receiver: receivers) {
				receiver.loopUpdated(notelist);
			}
		}
		
		public static void sendRefreshLoopDisplay() {
			for (LoopUpdateReceiver receiver: receivers) {
				receiver.refreshLoopDisplay();
			}
		}


	}
	
	
}
