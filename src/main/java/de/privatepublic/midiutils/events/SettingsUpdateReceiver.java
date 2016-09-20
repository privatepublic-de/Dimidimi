package de.privatepublic.midiutils.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public interface SettingsUpdateReceiver extends DimidimiEventReceiver {

	public void settingsUpdated();
	
	
}
