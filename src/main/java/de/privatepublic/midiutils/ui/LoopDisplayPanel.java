package de.privatepublic.midiutils.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.privatepublic.midiutils.MidiHandler;
import de.privatepublic.midiutils.Note;
import de.privatepublic.midiutils.events.LoopUpdateReceiver;
import de.privatepublic.midiutils.events.ManipulateReceiver;

public class LoopDisplayPanel extends JPanel implements LoopUpdateReceiver {

	private static final long serialVersionUID = -592444184016477559L;
	private static final Logger LOG = LoggerFactory.getLogger(LoopDisplayPanel.class);
	
	private int pos = 0;
	
	private List<Note> noteList = new ArrayList<Note>();
	private List<Line2D.Float> notePositionList = new ArrayList<Line2D.Float>();
	private boolean listsAreSynced = false;
	
	private Note hitNote;
	private Note selectedNote;
	private Point dragStart;
	
	private int dragStartNoteNumber;
	private int dragStartPosStart;
	private int dragStartPosEnd;
	
	private float noteHeight;
	private static final int bufferSemis = 5;
	private int highestNote = 96-bufferSemis;
	private int lowestNote = 12+bufferSemis;
	
	public LoopDisplayPanel() {
		super();
		addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					if (selectedNote!=null && selectedNote.isCompleted()) {
						openPopUp(e);
					}
				}
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				selectedNote = hitNote;				
				if (e.isPopupTrigger()) {
					if (selectedNote!=null && selectedNote.isCompleted()) {
						openPopUp(e);
					}
				}
				else {
					dragStart = e.getPoint();
					if (selectedNote!=null && selectedNote.isCompleted()) {
						dragStartNoteNumber = selectedNote.getNoteNumber();
						dragStartPosStart = selectedNote.getPosStart();
						dragStartPosEnd = selectedNote.getPosEnd();
					}
				}
				repaint();
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
			}
			
			private void openPopUp(MouseEvent e){
				PopUpMenu menu = new PopUpMenu();
		        menu.show(e.getComponent(), e.getX(), e.getY());
		    }
			
		});
		addMouseMotionListener(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent e) {
				if (!listsAreSynced) {
					return;
				}
				Note hitNoteBefore = hitNote;
				synchronized(noteList) {
					int mx = e.getX();
					int my = e.getY();
					int i = 0;
					hitNote = null;
					for (Note note:noteList) {
						Line2D.Float noteline = notePositionList.get(i);
						if (noteline.getX1()<=noteline.getX2()) {
							if (mx<=noteline.getX2() && mx>=noteline.getX1() && Math.abs(my-noteline.getY1())<noteHeight) {
								hitNote = note;
								break;
							}
						}
						else {
							if ((mx<=noteline.getX2() || mx>=noteline.getX1()) && Math.abs(my-noteline.getY1())<noteHeight) {
								hitNote = note;
								break;
							}
						}
						i++;
					}
					if (hitNote!=null) {
						Cursor cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR); 
					    setCursor(cursor);
					}
					else {
					    setCursor(Cursor.getDefaultCursor());
					}
					if (hitNote!=hitNoteBefore) {
						repaint();
					}
				}
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				if (selectedNote!=null && selectedNote.isCompleted()) {
					int distY = dragStart.y-e.getY();
					int distX = e.getX()-dragStart.x;
					
					int noteOffset = (int)(distY/noteHeight);
					selectedNote.setNoteNumber(dragStartNoteNumber+noteOffset);
					
					float tickwidth = getWidth()/(float)MidiHandler.instance().getMaxTicks();
					int ticksOffset = (int)(distX/tickwidth);
					selectedNote.setPosStart((dragStartPosStart+ticksOffset+MidiHandler.instance().getMaxTicks())%MidiHandler.instance().getMaxTicks());
					selectedNote.setPosEnd((dragStartPosEnd+ticksOffset+MidiHandler.instance().getMaxTicks())%MidiHandler.instance().getMaxTicks());
					
					repaint();
				}
			}
		});
	}
	
	@Override
	public void paint(Graphics go) {
		Graphics2D g = (Graphics2D)go;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setFont(Theme.fontNotes);
		int width = getWidth();
		int height = getHeight();
		int displayNoteCount = highestNote-lowestNote + 2*bufferSemis;
		noteHeight = height*(1f/displayNoteCount);
		g.setColor(Theme.colorBackground);
		g.fillRect(0, 0, width, height);
		
		// draw grid
		g.setColor(Theme.colorOctaves);
		for (int i=0;i<11;i++) {
			float colorhue = (96-i*12)/96f;
			g.setColor(Color.getHSBColor(colorhue, Theme.octaveColorSaturation, Theme.octaveColorBrightness));
			int y = (int)(((highestNote+bufferSemis)-i*12)*noteHeight);
			g.drawLine(0, y, width, y);
		}
		boolean is3based = MidiHandler.instance().getNumberQuarters()%3==0;
		int activeQuarter = pos/24;
		float quarterwidth = width*(24f/MidiHandler.instance().getMaxTicks());
		float sixthwidth = quarterwidth/4f; 
		for (int i=0;i<MidiHandler.instance().getNumberQuarters()*4;i++) {
			int xpos = (int)(i*sixthwidth);
			if (i/4 == activeQuarter) {
				g.setColor(Theme.colorActiveQuarter);
				g.fillRect(xpos, height-height/30, Math.round(sixthwidth), height);
				g.fillRect(xpos, 0, Math.round(sixthwidth), height/30);
			}
			g.setColor(Theme.colorGrid);
			g.drawLine(xpos, 0, xpos, height);
			if (i%4==0) { 
				// highlight quarters
				g.setColor(Theme.colorGridIntense);
				g.drawLine(xpos+1, 0, xpos+1, height);
			}
			if ((is3based && i%12==0) || (!is3based && i%16==0) && i>0) {
				// highlight first beat
				g.setColor(Theme.colorGridIntense);
				g.drawLine(xpos+3, 0, xpos+3, height);
				g.drawLine(xpos-1, 0, xpos-1, height);
				g.drawLine(xpos-3, 0, xpos-3, height);
			}
			
		}
		
		// draw playhead
		g.setColor(Theme.colorPlayhead);
		float playheadx = width*((float)pos/MidiHandler.instance().getMaxTicks());
		float tickwidth = width*(1f/MidiHandler.instance().getMaxTicks());
		g.fillRect((int)(playheadx-tickwidth/2), 0, (int)tickwidth, height);

		// draw notes
		//g.setStroke(new BasicStroke(tickwidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
		Note selectedNoteRun = selectedNote;
		if (selectedNoteRun==null) {
			selectedNoteRun = hitNote;
		}
		synchronized(noteList) {
			notePositionList.clear();
			for (Note dc:noteList) {
				int no = (highestNote+bufferSemis)-dc.getTransformedNoteNumber();
				float colorhue = (96-dc.getTransformedNoteNumber())/96f; 
				Color noteColor = Color.getHSBColor(colorhue, Theme.noteColorSaturation, Theme.noteColorBrightness);
				if (dc.isPlayed()) {
					g.setColor(Theme.colorPlayedNote);
				}
				else {
					g.setColor(noteColor);
				}
				float notey = no*noteHeight;
				float notestartx = dc.getTransformedPosStart()*tickwidth;
				float noteendx = dc.isCompleted()?dc.getTransformedPosEnd()*tickwidth:pos*tickwidth;
				float veloHeight = Math.max(dc.getVelocity()/127f * noteHeight*2, noteHeight/3f);
				
				int lineCap = (dc==selectedNoteRun)?BasicStroke.CAP_BUTT:BasicStroke.CAP_ROUND;
				
				if (noteendx>=notestartx) {
					if (dc==selectedNoteRun) {
						g.setColor(Theme.colorSelectedNoteOutline);
						g.setStroke(new BasicStroke(veloHeight*2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
						g.drawLine((int)notestartx, (int)notey, (int)noteendx, (int)notey);
						g.setColor(noteColor);
					}
					g.setStroke(new BasicStroke(veloHeight, lineCap, BasicStroke.JOIN_MITER));
					g.drawLine((int)notestartx, (int)notey, (int)noteendx, (int)notey);
				}
				else {
					if (dc==selectedNoteRun) {
						g.setColor(Theme.colorSelectedNoteOutline);
						g.setStroke(new BasicStroke(veloHeight*2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
						g.drawLine((int)0, (int)notey, (int)noteendx, (int)notey);
						g.drawLine((int)notestartx, (int)notey, (int)width, (int)notey);
						g.setColor(noteColor);
					}
					g.setStroke(new BasicStroke(veloHeight, lineCap, BasicStroke.JOIN_MITER));
					g.drawLine((int)0, (int)notey, (int)noteendx, (int)notey);
					g.drawLine((int)notestartx, (int)notey, (int)width, (int)notey);
				}
				notePositionList.add(new Line2D.Float(notestartx, notey, noteendx, notey));
				
				if (selectedNoteRun!=null) {
					String notetext = dc.getNoteName();
					FontMetrics fm = g.getFontMetrics();
	                Rectangle2D rect = fm.getStringBounds(notetext, g);
	                int y = (int)(notey);
	                if (dc==selectedNoteRun) {
	                	y -= veloHeight;	
	                }
	                g.setColor(noteColor);
	                g.fill(new RoundRectangle2D.Float(notestartx-1, (float)(y-fm.getAscent()), (float)rect.getWidth()+2, (float)rect.getHeight(), 7, 7));
//	                g.fillRect((int)notestartx-1,
//	                           y - fm.getAscent(),
//	                           (int) rect.getWidth()+2,
//	                           (int) rect.getHeight());
					g.setColor(Theme.colorSelectedNoteText);
					g.drawString(notetext, notestartx, y);
				}
				
			}
			listsAreSynced = true;
		}
		
	}

	public void updateLoopPosition(int pos) {
		this.pos = pos;
		repaint();
	}

	@Override
	public void loopUpdated(List<Note> list) {
		synchronized(noteList) {
			listsAreSynced = false;
			noteList.clear();
			for (Note dc:list) {
				noteList.add(dc);
			}
			calculateNoteExtents();
		}
		repaint();
	}

	@Override
	public void refreshLoopDisplay() {
		calculateNoteExtents();
		repaint();
	}
	
	private void calculateNoteExtents() {
		int maxNote = 12;
		int minNote = 96+12; 
		synchronized(noteList) {
			for (Note dc:noteList) {
				minNote = Math.min(dc.getTransformedNoteNumber(), minNote);
				maxNote = Math.max(dc.getTransformedNoteNumber(), maxNote);
			}
			if (noteList.size()==0) {
				minNote = 12 + bufferSemis;
				maxNote = 96+12 - bufferSemis;
			}
			highestNote = maxNote;
			lowestNote = minNote;
		}
	}
	
	class PopUpMenu extends JPopupMenu {
		private static final long serialVersionUID = 9049098677413712819L;

		public PopUpMenu(){
	    	JMenuItem delete = new JMenuItem("Delete");
	        delete.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					LOG.info("Delete Note Called");
					ManipulateReceiver.Dispatcher.sendClearNote(selectedNote);
					selectedNote = null;
					hitNote = null;
				}
			});
	        JMenu velocity = new JMenu("Velocity");
	        JSlider veloslider = new JSlider();
	        veloslider.setOrientation(SwingConstants.VERTICAL);
	        veloslider.setMaximum(127);
	        veloslider.setMinimum(1);
	        veloslider.setValue(selectedNote.getVelocity());
	        Dictionary<Integer, JLabel> labels = new Hashtable<Integer, JLabel>(); // TODO static
	        labels.put(1, new JLabel("1"));
	        labels.put(32, new JLabel("32"));
	        labels.put(64, new JLabel("64"));
	        labels.put(96, new JLabel("96"));
	        labels.put(127, new JLabel("127")); 
	        veloslider.setLabelTable(labels);
	        veloslider.setMajorTickSpacing(32);
	        veloslider.setMinorTickSpacing(8);
	        veloslider.setPaintTicks(true);
	        veloslider.setPaintLabels(true);
	        veloslider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					selectedNote.setVelocity(veloslider.getValue());
					LoopUpdateReceiver.Dispatcher.sendRefreshLoopDisplay();;
				}
			});
	        velocity.add(veloslider);
	        
	        JMenu length = new JMenu("Length");
	        JSlider lengthslider = new JSlider();
	        lengthslider.setPreferredSize(new Dimension(MidiHandler.instance().getMaxTicks(), 48));
	        lengthslider.setOrientation(SwingConstants.HORIZONTAL);
	        lengthslider.setMajorTickSpacing(24);
	        lengthslider.setMinorTickSpacing(6);
	        lengthslider.setPaintTicks(true);
	        lengthslider.setMaximum(MidiHandler.instance().getMaxTicks()-1);
	        lengthslider.setMinimum(1);
	        int len;
			if (selectedNote.getPosEnd()>selectedNote.getPosStart()) {
				len = selectedNote.getPosEnd()-selectedNote.getPosStart();
			}
			else {
				len = selectedNote.getPosEnd()+(selectedNote.getPosStart()-MidiHandler.instance().getMaxTicks());
			}
	        lengthslider.setValue(len);
	        lengthslider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					selectedNote.setPosEnd((selectedNote.getPosStart()+lengthslider.getValue())%MidiHandler.instance().getMaxTicks());
					LoopUpdateReceiver.Dispatcher.sendRefreshLoopDisplay();
				}
			});
	        length.add(lengthslider);
	        add(velocity);
	        add(length);
	        addSeparator();
	        add(delete);
	    }
	}
	
//	private void fixOverlappingNotes() {
//		synchronized(noteList) {
//			for (NoteRun check:noteList) {
//				for (NoteRun other:noteList) {
//					if (other!=check && check.getNoteNumber()==other.getNoteNumber()) {
//						
//					}
//				}				
//			}
//		}
//	}
	
}
