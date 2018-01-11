package de.privatepublic.midiutils.ui;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.privatepublic.midiutils.Prefs;

public class Theme {
	
	private static final Logger LOG = LoggerFactory.getLogger(Theme.class);
	
	public static final Theme DARK = new Theme("/theme_dark.properties");
	public static final Theme BRIGHT = new Theme("/theme_bright.properties");
	public static Theme APPLY = Prefs.get(Prefs.THEME, 0)>0?Theme.DARK:Theme.BRIGHT;
	
	public static boolean isBright() {
		return APPLY == BRIGHT;
	}
	
	
	private Font fontNotes = new Font(Font.SANS_SERIF, Font.BOLD, 12); 
	private Font fontMidiBig = new Font(Font.SANS_SERIF, Font.BOLD, 12);
	private Color colorBackground = Color.decode("#222222");
	private Color colorBackgroundController = Color.decode("#33333f");
	private Color colorForeground = Color.decode("#ffffff");
	private Color colorGrid = Color.decode("#444444");
	private Color colorGridIntense = Color.decode("#494949");
	private Color colorActiveQuarter = Color.decode("#666666");
	private Color colorPlayhead = Color.decode("#cccccc");
	private Color colorPlayedNote = colorGridIntense;
	private Color colorNoteLabels = colorGrid;
	private Color colorSelectedNoteOutline = new Color(.6f, .6f, .6f, .6f);
	private Color colorSelectedNoteText = Color.WHITE;
	private Color colorMidiOutBig = colorGrid;
	private Color colorGridHighlight = Color.RED;
	private Color colorSelectionRectangle = Color.WHITE;
	private Color colorMuted = new Color(0x80ffffff,true);
	private float noteColorSaturation = .6f;
	private float noteColorBrightness = 1f;
	private Color colorModWheel = Color.WHITE;
	private Color colorPitchBend = Color.GREEN;
	private float noteLightColorBrightnessFactor = .6f;
	private float colorChannelSaturation=.1f;
	private float colorChannelBrightness=.2f;
	
	public Theme(String fileName) {
		try {
			LineIterator iter = IOUtils.lineIterator(Theme.class.getResourceAsStream(fileName), "utf8");
			while(iter.hasNext()) {
				try {
					String line = iter.next();
					String[] parts = line.split("=");
					Field field = Theme.class.getDeclaredField(parts[0]);
					field.setAccessible(true);
					if (parts.length==2) {
						if (parts[1].startsWith("#")) {
							// color
							Color colorVal;
							if (parts[1].indexOf(',')>-1) {
								String[] cparts = parts[1].split(",");
								colorVal = new Color(
										Integer.parseInt(cparts[0].substring(1, 3), 16),
										Integer.parseInt(cparts[0].substring(3, 5), 16),
										Integer.parseInt(cparts[0].substring(5, 7), 16),
										Integer.parseInt(cparts[1], 16)
										);
							}
							else {
								colorVal = Color.decode(parts[1]);
							}
							field.set(this, colorVal);
						}
						else if (parts[1].startsWith("font:")) {
							// font
							field.set(this, Font.decode(parts[1].substring(5)));
						}
						else {
							// try float value
							field.set(this, Float.parseFloat(parts[1]));
						}
					}
				} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
					LOG.error("Error reading theme properties", e);
				} 
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Font fontNotes() {
		return fontNotes;
	}
	public Font fontMidiBig() {
		return fontMidiBig;
	}
	public Color colorBackground() {
		return colorBackground;
	}
	public Color colorBackgroundController() {
		return colorBackgroundController;
	}

	public Color colorForeground() {
		return colorForeground;
	}
	public Color colorGrid() {
		return colorGrid;
	}
	public Color colorGridIntense() {
		return colorGridIntense;
	}
	public Color colorActiveQuarter() {
		return colorActiveQuarter;
	}
	public Color colorPlayhead() {
		return colorPlayhead;
	}
	public Color colorPlayedNote() {
		return colorPlayedNote;
	}
	public Color colorNoteLabels() {
		return colorNoteLabels;
	}
	public Color colorSelectedNoteOutline() {
		return colorSelectedNoteOutline;
	}
	public Color colorSelectedNoteText() {
		return colorSelectedNoteText;
	}
	public Color colorMidiOutBig() {
		return colorMidiOutBig;
	}
	public Color colorModWheel() {
		return colorModWheel;
	}
	public Color colorPitchBend() {
		return colorPitchBend;
	}
	public Color colorGridHighlight() {
		return colorGridHighlight;
	}

	public Color colorSelectionRectangle() {
		return colorSelectionRectangle;
	}

	public Color colorMuted() {
		return colorMuted;
	}

	public float noteColorSaturation() {
		return noteColorSaturation;
	}
	public float noteColorBrightness() {
		return noteColorBrightness;
	}
	public float noteLightColorBrightnessFactor() {
		return noteLightColorBrightnessFactor;
	}

	public float colorChannelSaturation() {
		return colorChannelSaturation;
	}

	public float getColorChannelBrightness() {
		return colorChannelBrightness;
	}
		
}