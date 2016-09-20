package de.privatepublic.midiutils.events;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface StorageReceiver extends DimidimiEventReceiver {

	public void saveRequest(File file) throws JsonGenerationException, JsonMappingException, IOException;
	public void loadRequest(File file) throws JsonParseException, JsonMappingException, IOException;
	
	
}
