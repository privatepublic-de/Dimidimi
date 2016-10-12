package de.privatepublic.midiutils.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.SystemColor;

public class Theme {
	
	public static final Theme DARK = new Theme(
			new Font(Font.SANS_SERIF, Font.PLAIN, 12) /*fontNotes*/, 
			new Font(Font.SANS_SERIF, Font.BOLD, 12)/*fontMidiBig*/, 
			Color.decode("#222222") /*colorBackground*/, 
			Color.decode("#444444") /*colorGrid*/, 
			Color.decode("#494949") /*colorGridIntense*/, 
			Color.decode("#666666") /*colorActiveQuarter*/, 
			Color.decode("#cccccc") /*colorPlayhead*/, 
			Color.decode("#494949") /*colorPlayedNote*/, 
			Color.decode("#444444") /*colorOctaves*/, 
			new Color(.6f, .6f, .6f, .6f) /*colorSelectedNoteOutline*/, 
			Color.WHITE /*colorSelectedNoteText*/, 
			Color.getHSBColor(.58f, .8f, 1f) /*colorClockOn*/, 
			SystemColor.window /*colorClockOff*/, 
			Color.decode("#333333") /*colorMidiOutBig*/,
			new Color(.06f, .26f, .34f, .7f), /*colorModWheel*/
			//Color.decode("#114455") /*colorModWheel*/, 
			new Color(.34f, .06f, .06f, .7f) /*colorPitchBend*/,
			//Color.decode("#551111") /*colorPitchBend*/,
			.6f /*noteColorSaturation*/, 
			1f /*noteColorBrightness*/, 
			.8f, /*noteLightColorBrightnessFactor*/
			.6f, /*octaveColorSaturation*/ 
			.5f/*octaveColorBrightness*/
		);
	
	public static final Theme BRIGHT = new Theme(
			new Font(Font.SANS_SERIF, Font.PLAIN, 12) /*fontNotes*/, 
			new Font(Font.SANS_SERIF, Font.BOLD, 12)/*fontMidiBig*/, 
			Color.WHITE /*colorBackground*/, 
			Color.decode("#eeeeee") /*colorGrid*/, 
			Color.decode("#dddddd") /*colorGridIntense*/, 
			Color.decode("#ff6666") /*colorActiveQuarter*/, 
			Color.ORANGE /*colorPlayhead*/, 
			Color.ORANGE /*colorPlayedNote*/, 
			Color.decode("#dddddd") /*colorOctaves*/, 
			new Color(.7f, .7f, .7f, .6f) /*colorSelectedNoteOutline*/, 
			Color.BLACK /*colorSelectedNoteText*/, 
			Color.getHSBColor(.58f, .8f, 1f) /*colorClockOn*/, 
			SystemColor.window /*colorClockOff*/, 
			Color.decode("#eeeeee") /*colorMidiOutBig*/, 
			Color.decode("#D4F4FF") /*colorModWheel*/, 
			Color.decode("#D99E9F") /*colorPitchBend*/, 
			.8f /*noteColorSaturation*/, 
			.8f /*noteColorBrightness*/, 
			.8f, /*noteLightColorBrightnessFactor*/
			.9f, /*octaveColorSaturation*/ 
			.6f/*octaveColorBrightness*/
		);
	
	public static Theme CURRENT = Theme.DARK;
	
	private Font fontNotes = new Font(Font.SANS_SERIF, Font.BOLD, 12); 
	private Font fontMidiBig = new Font(Font.SANS_SERIF, Font.BOLD, 12);
	private Color colorBackground = Color.decode("#222222");
	private Color colorGrid = Color.decode("#444444");
	private Color colorGridIntense = Color.decode("#494949");
	private Color colorActiveQuarter = Color.decode("#666666");
	private Color colorPlayhead = Color.decode("#cccccc");
	private Color colorPlayedNote = colorGridIntense;
	private Color colorOctaves = colorGrid;
	private Color colorSelectedNoteOutline = new Color(.6f, .6f, .6f, .6f);
	private Color colorSelectedNoteText = Color.WHITE;
	private Color colorClockOn = Color.getHSBColor(.58f, .8f, 1f);
	private Color colorClockOff = SystemColor.window;
	private Color colorMidiOutBig = colorGrid;
	private float noteColorSaturation = .6f;
	private float noteColorBrightness = 1f;
	private float octaveColorSaturation = .6f;
	private float octaveColorBrightness = .5f;
	private Color colorModWheel = Color.WHITE;
	private Color colorPitchBend = Color.GREEN;
	private float noteLightColorBrightnessFactor = .6f;
	
	public Theme(Font fontNotes, Font fontMidiBig, Color colorBackground, Color colorGrid,
			Color colorGridIntense, Color colorActiveQuarter, Color colorPlayhead, Color colorPlayedNote,
			Color colorOctaves, Color colorSelectedNoteOutline, Color colorSelectedNoteText, Color colorClockOn, Color colorClockOff,
			Color colorMidiOutBig, 
			Color colorModWheel, Color colorPitchBend, float noteColorSaturation, float noteColorBrightness,
			float noteLightColorBrightnessFactor,
			float octaveColorSaturation,
			float octaveColorBrightness) {
		this.fontNotes = fontNotes;
		this.fontMidiBig = fontMidiBig;
		this.colorBackground = colorBackground;
		this.colorGrid = colorGrid;
		this.colorGridIntense = colorGridIntense;
		this.colorActiveQuarter = colorActiveQuarter;
		this.colorPlayhead = colorPlayhead;
		this.colorPlayedNote = colorPlayedNote;
		this.colorOctaves = colorOctaves;
		this.colorSelectedNoteOutline = colorSelectedNoteOutline;
		this.colorSelectedNoteText = colorSelectedNoteText;
		this.colorClockOn = colorClockOn;
		this.colorClockOff = colorClockOff;
		this.colorMidiOutBig = colorMidiOutBig;
		this.colorModWheel = colorModWheel;
		this.colorPitchBend = colorPitchBend;
		this.noteColorSaturation = noteColorSaturation;
		this.noteColorBrightness = noteColorBrightness;
		this.noteLightColorBrightnessFactor = noteLightColorBrightnessFactor;
		this.octaveColorSaturation = octaveColorSaturation;
		this.octaveColorBrightness = octaveColorBrightness;
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
	public Color getColorOctaves() {
		return colorOctaves;
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
	public float getNoteColorSaturation() {
		return noteColorSaturation;
	}
	public float getNoteColorBrightness() {
		return noteColorBrightness;
	}
	public float getNoteLightColorBrightnessFactor() {
		return noteLightColorBrightnessFactor;
	}
	public float getOctaveColorSaturation() {
		return octaveColorSaturation;
	}
	public float getOctaveColorBrightness() {
		return octaveColorBrightness;
	}
		
		
	
}