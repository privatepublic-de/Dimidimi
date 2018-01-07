package de.privatepublic.midiutils.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.SystemColor;
import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import de.privatepublic.midiutils.Prefs;

public class Theme {
	
	public static final Theme DARK = new Theme("/theme_dark.properties");
	public static final Theme BRIGHT = new Theme("/theme_bright.properties");
	
	public static Theme CURRENT = Prefs.get(Prefs.THEME, 0)>0?Theme.DARK:Theme.BRIGHT;
	
	private Font fontNotes = new Font(Font.SANS_SERIF, Font.BOLD, 12); 
	private Font fontMidiBig = new Font(Font.SANS_SERIF, Font.BOLD, 12);
	private Color colorBackground = Color.decode("#222222");
	private Color colorForeground = Color.decode("#ffffff");
	private Color colorGrid = Color.decode("#444444");
	private Color colorGridIntense = Color.decode("#494949");
	private Color colorActiveQuarter = Color.decode("#666666");
	private Color colorPlayhead = Color.decode("#cccccc");
	private Color colorPlayedNote = colorGridIntense;
	private Color colorNoteLabels = colorGrid;
	private Color colorSelectedNoteOutline = new Color(.6f, .6f, .6f, .6f);
	private Color colorSelectedNoteText = Color.WHITE;
	private Color colorClockOn = Color.getHSBColor(.58f, .8f, 1f);
	private Color colorClockOff = SystemColor.window;
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
					
					Field field = Theme.class.getDeclaredField(parts[0]); //NoSuchFieldException
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
				} catch (NoSuchFieldException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Font getFontNotes() {
		return fontNotes;
	}
	public Font getFontMidiBig() {
		return fontMidiBig;
	}
	public Color getColorBackground() {
		return colorBackground;
	}
	public Color getColorForeground() {
		return colorForeground;
	}
	public Color getColorGrid() {
		return colorGrid;
	}
	public Color getColorGridIntense() {
		return colorGridIntense;
	}
	public Color getColorActiveQuarter() {
		return colorActiveQuarter;
	}
	public Color getColorPlayhead() {
		return colorPlayhead;
	}
	public Color getColorPlayedNote() {
		return colorPlayedNote;
	}
	public Color getNoteLabels() {
		return colorNoteLabels;
	}
	public Color getColorSelectedNoteOutline() {
		return colorSelectedNoteOutline;
	}
	public Color getColorSelectedNoteText() {
		return colorSelectedNoteText;
	}
	public Color getColorClockOn() {
		return colorClockOn;
	}
	public Color getColorClockOff() {
		return colorClockOff;
	}
	public Color getColorMidiOutBig() {
		return colorMidiOutBig;
	}
	public Color getColorModWheel() {
		return colorModWheel;
	}
	public Color getColorPitchBend() {
		return colorPitchBend;
	}
	public Color getColorGridHighlight() {
		return colorGridHighlight;
	}

	public Color getColorSelectionRectangle() {
		return colorSelectionRectangle;
	}

	public Color getColorMuted() {
		return colorMuted;
	}

	public float getNoteColorSaturation() {
		return noteColorSaturation;
	}
	public float getNoteColorBrightness() {
		return noteColorBrightness;
	}
	public float getNoteLightColorBrightnessFactor() {
		return noteLightColorBrightnessFactor;
	}

	public float getColorChannelSaturation() {
		return colorChannelSaturation;
	}

	public void setColorChannelSaturation(float colorChannelSaturation) {
		this.colorChannelSaturation = colorChannelSaturation;
	}

	public float getColorChannelBrightness() {
		return colorChannelBrightness;
	}

	public void setColorChannelBrightness(float colorChannelBrightness) {
		this.colorChannelBrightness = colorChannelBrightness;
	}
		
}