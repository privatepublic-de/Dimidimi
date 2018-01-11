package de.privatepublic.midiutils.events;

import de.privatepublic.midiutils.Loop;

public interface FocusReceiver extends DimidimiEventReceiver {
	public void onFocusLoop(Loop loop);
}
