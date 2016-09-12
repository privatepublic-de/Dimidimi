package de.privatepublic.midiutils.events;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface StorageReceiver {

	public void saveRequest(File file) throws JsonGenerationException, JsonMappingException, IOException;
	
	public void loadRequest(File file) throws JsonParseException, JsonMappingException, IOException;
	
}
