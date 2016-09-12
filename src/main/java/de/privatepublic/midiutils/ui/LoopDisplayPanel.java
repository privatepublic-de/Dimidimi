package de.privatepublic.midiutils.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import de.privatepublic.midiutils.MidiHandler;
import de.privatepublic.midiutils.NoteRun;
import de.privatepublic.midiutils.events.LoopUpdateReceiver;

public class LoopDisplayPanel extends JPanel implements LoopUpdateReceiver {

	private static final long serialVersionUID = -592444184016477559L;
	
	private int pos = 0;
	
	private List<NoteRun> noteList = new ArrayList<NoteRun>();
	private Color colorActiveQuarter = Color.decode("#ff9900");
	private Color colorOctaves = Color.decode("#dddddd");
	
	@Override
	public void paint(Graphics go) {
		Graphics2D g = (Graphics2D)go;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		int width = getWidth();
		int height = getHeight();
		float noteheight = height*(1f/127);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width, height);
		// draw grid
		g.setColor(colorOctaves);
		for (int i=1;i<11;i++) {
			g.drawLine(0, height-(int)(i*12*noteheight), width, height-(int)(i*12*noteheight));
		}
		g.setColor(Color.GRAY);
		boolean is3based = MidiHandler.instance().getNumberQuarters()%3==0;
		int activeQuarter = pos/24;
		float quarterwidth = width*(24f/MidiHandler.instance().getMaxTicks());
		float sixthwidth = quarterwidth/4f; 
		for (int i=0;i<MidiHandler.instance().getNumberQuarters()*4;i++) {
			int xpos = (int)(i*sixthwidth);
			if (i/4 == activeQuarter) {
				g.setColor(colorActiveQuarter);
				g.fillRect(xpos, height-height/30, (int)sixthwidth, height);
				g.fillRect(xpos, 0, (int)sixthwidth, height/30);
				g.setColor(Color.GRAY);
			}
			g.drawLine(xpos, 0, xpos, height);
			if (i%4==0) { // highlight 16ths
				g.setColor(Color.BLACK);
				g.drawLine(xpos+1, 0, xpos+1, height);
				g.setColor(Color.GRAY);
			}
			if ((is3based && i%12==0) || (!is3based && i%16==0)) {
				g.setColor(Color.BLACK);
				g.drawLine(xpos+3, 0, xpos+3, height);
				g.drawLine(xpos-1, 0, xpos-1, height);
				g.drawLine(xpos-3, 0, xpos-3, height);
				g.setColor(Color.GRAY);
			}
			
		}
		
		// draw playhead
		g.setColor(Color.ORANGE);
		float playheadx = width*((float)pos/MidiHandler.instance().getMaxTicks());
		float tickwidth = width*(1f/MidiHandler.instance().getMaxTicks());
		g.fillRect((int)(playheadx-tickwidth/2), 0, (int)tickwidth, height);

		// draw notes
		g.setStroke(new BasicStroke(tickwidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
		synchronized(noteList) {
			for (NoteRun dc:noteList) {
				int no = 127 - dc.getTransformedNoteNumber();
				float colorhue = no/127f; 
				if (dc.isPlayed()) {
					g.setColor(Color.ORANGE);
				}
				else {
					g.setColor(Color.getHSBColor(colorhue, .9f, .7f));
				}
				float notey = no*noteheight;
				float notestartx = dc.getTransformedPosStart()*tickwidth;
				float noteendx = dc.isCompleted()?dc.getTransformedPosEnd()*tickwidth:pos*tickwidth;
				if (true) {
					float velo = Math.max(dc.getVelocity()/127f * tickwidth*2,1);
					g.setStroke(new BasicStroke(velo, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
//					g.drawLine((int)notestartx, (int)notey, (int)notestartx, (int)notey-velo);
//					g.setStroke(new BasicStroke(tickwidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));			
				}
				if (noteendx>=notestartx) {
					g.drawLine((int)notestartx, (int)notey, (int)noteendx, (int)notey);
				}
				else {
					g.drawLine((int)0, (int)notey, (int)noteendx, (int)notey);
					g.drawLine((int)notestartx, (int)notey, (int)width, (int)notey);
				}
			}
		}
		
	}

	public void updateLoopPosition(int pos) {
		this.pos = pos;
		repaint();
	}

	@Override
	public void loopUpdated(List<NoteRun> list) {
		synchronized(noteList) {
			noteList.clear();
			for (NoteRun dc:list) {
				noteList.add(dc);
			}
		}
		repaint();
	}

	@Override
	public void refreshLoopDisplay() {
		repaint();
	}
	
	
	
}
