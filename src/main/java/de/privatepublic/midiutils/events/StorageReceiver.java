package de.privatepublic.midiutils.events;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface StorageReceiver {

	public void saveRequest(File file) throws JsonGenerationException, JsonMappingException, IOException;
	public void loadRequest(File file) throws JsonParseException, JsonMappingException, IOException;
	
	
	public static class Dispatcher {

		private static final List<StorageReceiver> receivers = new CopyOnWriteArrayList<StorageReceiver>();

		public static void register(StorageReceiver receiver) {
			if (!receivers.contains(receiver)) {
				receivers.add(receiver);
			}
		}

		public static void sendSaveRequest(File file) throws JsonGenerationException, JsonMappingException, IOException {
			for (StorageReceiver receiver: receivers) {
				receiver.saveRequest(file);
			}
		}
		
		public static void sendLoadRequest(File file) throws JsonGenerationException, JsonMappingException, IOException {
			for (StorageReceiver receiver: receivers) {
				receiver.loadRequest(file);
			}
		}


	}
	
	
}
