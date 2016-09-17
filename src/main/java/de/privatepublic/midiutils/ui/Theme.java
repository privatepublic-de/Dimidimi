package de.privatepublic.midiutils.ui;

import java.awt.Color;
import java.awt.Font;

public class Theme {
	
	
	
	
	
	
	static final Font fontNotes = new Font(Font.SANS_SERIF, Font.BOLD, 12); 
	
//	static final Color colorBackground = Color.WHITE;
//	static final Color colorGrid = Color.LIGHT_GRAY;
//	static final Color colorGridIntense = Color.GRAY;
//	static final Color colorActiveQuarter = Color.decode("#ff6666");
//	static final Color colorPlayhead = Color.ORANGE;
//	static final Color colorPlayedNote = Color.ORANGE;
//	static final Color colorOctaves = Color.decode("#dddddd");
//	static final Color colorSelectedNoteOutline = new Color(.7f, .7f, .7f, .6f);
//	static final Color colorSelectedNoteText = Color.WHITE;
//	
//	static final Color colorClockOn = colorActiveQuarter;
//	static final Color colorClockOff = Color.WHITE;
//	
//	
//	static final float noteColorSaturation = .9f;
//	static final float noteColorBrightness = .7f;
//	
//	static final float octaveColorSaturation = .9f;
//	static final float octaveColorBrightness = .6f;
	
	
	static final Color colorBackground = Color.decode("#222222");
	static final Color colorGrid = Color.decode("#444444");
	static final Color colorGridIntense = Color.decode("#494949");
	static final Color colorActiveQuarter = Color.decode("#666666");
	static final Color colorPlayhead = Color.decode("#cccccc");
	static final Color colorPlayedNote = colorGridIntense;
	static final Color colorOctaves = colorGrid;
	static final Color colorSelectedNoteOutline = new Color(.6f, .6f, .6f, .6f);
	static final Color colorSelectedNoteText = Color.BLACK;
	
	static final Color colorClockOn = Color.getHSBColor(.58f, .8f, 1f);
	static final Color colorClockOff = colorBackground;
	
	
	static final float noteColorSaturation = .6f;
	static final float noteColorBrightness = 1f;
	
	static final float octaveColorSaturation = .6f;
	static final float octaveColorBrightness = .5f;
	
}