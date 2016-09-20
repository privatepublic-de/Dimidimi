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

import de.privatepublic.midiutils.Note;
import de.privatepublic.midiutils.Session;
import de.privatepublic.midiutils.events.LoopUpdateReceiver;

public class LoopDisplayPanel extends JPanel implements LoopUpdateReceiver {

	private static final long serialVersionUID = -592444184016477559L;
	private static final Logger LOG = LoggerFactory.getLogger(LoopDisplayPanel.class);
	
	private Session session;
	
	private int pos = 0;
	
	private Note hitNote;
	private Note selectedNote;
	private Point dragStart;
	
	private int dragStartNoteNumber;
	private int dragStartPosStart;
	private int dragStartPosEnd;
	
	private float noteHeight;
	private float tickwidth;
	private static final int bufferSemis = 5;
	private int highestNote = 96-bufferSemis;
	private int lowestNote = 12+bufferSemis;
	
	public LoopDisplayPanel(Session session) {
		super();
		this.session = session;
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
//				if (!listsAreSynced) {
//					return;
//				}
				Note hitNoteBefore = hitNote;
				int mx = e.getX();
				int my = e.getY();
				hitNote = null;
				for (Note note:session.getNotesList()) {
					Line2D.Float noteline = getNotePosition(note);
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
			
			@Override
			public void mouseDragged(MouseEvent e) {
				if (selectedNote!=null && selectedNote.isCompleted()) {
					int distY = dragStart.y-e.getY();
					int distX = e.getX()-dragStart.x;
					
					int noteOffset = (int)(distY/noteHeight);
					selectedNote.setNoteNumber(dragStartNoteNumber+noteOffset);
					
					float tickwidth = getWidth()/(float)session.getMaxTicks();
					int ticksOffset = (int)(distX/tickwidth);
					selectedNote.setPosStart((dragStartPosStart+ticksOffset+session.getMaxTicks())%session.getMaxTicks());
					selectedNote.setPosEnd((dragStartPosEnd+ticksOffset+session.getMaxTicks())%session.getMaxTicks());
					
					repaint();
				}
			}
		});
		session.registerAsReceiver(this);
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
		boolean is3based = session.getLengthQuarters()%3==0;
		int activeQuarter = pos/Session.TICK_COUNT_BASE;
		float quarterwidth = width*((float)Session.TICK_COUNT_BASE/session.getMaxTicks());
		float sixthwidth = quarterwidth/4f; 
		for (int i=0;i<session.getLengthQuarters()*4;i++) {
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
		float playheadx = width*((float)pos/session.getMaxTicks());
		tickwidth = width*(1f/session.getMaxTicks());
		g.fillRect((int)(playheadx-tickwidth/2), 0, (int)tickwidth, height);

		// draw notes
		//g.setStroke(new BasicStroke(tickwidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
		Note selectedNoteRun = selectedNote;
		if (selectedNoteRun==null) {
			selectedNoteRun = hitNote;
		}
		for (Note note:session.getNotesList()) {
			float colorhue = (96-note.getTransformedNoteNumber(session.getTransposeIndex()))/96f; 
			Color noteColor = Color.getHSBColor(colorhue, Theme.noteColorSaturation, Theme.noteColorBrightness);
			if (note.isPlayed()) {
				g.setColor(Theme.colorPlayedNote);
			}
			else {
				g.setColor(noteColor);
			}
			Line2D.Float notepos = getNotePosition(note);
			float notey = notepos.y1;
			float notestartx = notepos.x1;
			float noteendx = notepos.x2;
			float veloHeight = Math.max(note.getVelocity()/127f * noteHeight*2, noteHeight/3f);

			int lineCap = (note==selectedNoteRun)?BasicStroke.CAP_BUTT:BasicStroke.CAP_ROUND;

			if (noteendx>=notestartx) {
				if (note==selectedNoteRun) {
					g.setColor(Theme.colorSelectedNoteOutline);
					g.setStroke(new BasicStroke(veloHeight*2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
					g.drawLine((int)notestartx, (int)notey, (int)noteendx, (int)notey);
					g.setColor(noteColor);
				}
				g.setStroke(new BasicStroke(veloHeight, lineCap, BasicStroke.JOIN_MITER));
				g.drawLine((int)notestartx, (int)notey, (int)noteendx, (int)notey);
			}
			else {
				if (note==selectedNoteRun) {
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
			
			if (selectedNoteRun!=null) {
				String notetext = note.getNoteName(session.getTransposeIndex());
				FontMetrics fm = g.getFontMetrics();
				Rectangle2D rect = fm.getStringBounds(notetext, g);
				int y = (int)(notey);
				if (note==selectedNoteRun) {
					y -= veloHeight;	
				}
				g.setColor(noteColor);
				g.fill(new RoundRectangle2D.Float(notestartx-1, (float)(y-fm.getAscent()), (float)rect.getWidth()+2, (float)rect.getHeight(), 7, 7));
				g.setColor(Theme.colorSelectedNoteText);
				g.drawString(notetext, notestartx, y);
			}

		}
	}
	
	private Line2D.Float getNotePosition(Note note) {
		int no = (highestNote+bufferSemis)-note.getTransformedNoteNumber(session.getTransposeIndex());
		float notey = no*noteHeight;
		float notestartx = note.getTransformedPosStart(session.getMaxTicks(), session.getQuantizationIndex())*tickwidth;
		float noteendx = note.isCompleted()?note.getTransformedPosEnd(session.getMaxTicks(), session.getQuantizationIndex())*tickwidth:pos*tickwidth;
		return new Line2D.Float(notestartx, notey, noteendx, notey);
	}

	public void updateLoopPosition(int pos) {
		this.pos = pos;
		repaint();
	}

	@Override
	public void loopUpdated(List<Note> list) {
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
		for (Note dc:session.getNotesList()) {
			minNote = Math.min(dc.getTransformedNoteNumber(session.getTransposeIndex()), minNote);
			maxNote = Math.max(dc.getTransformedNoteNumber(session.getTransposeIndex()), maxNote);
		}
		if (session.getNotesList().size()==0) {
			minNote = 12 + bufferSemis;
			maxNote = 96+12 - bufferSemis;
		}
		highestNote = maxNote;
		lowestNote = minNote;
	}
	
	class PopUpMenu extends JPopupMenu {
		private static final long serialVersionUID = 9049098677413712819L;

		public PopUpMenu(){
	    	JMenuItem delete = new JMenuItem("Delete");
	        delete.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					LOG.info("Delete Note Called");
					session.clearNote(selectedNote);
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
					session.emitRefreshLoopDisplay();;
				}
			});
	        velocity.add(veloslider);
	        
	        JMenu length = new JMenu("Length");
	        JSlider lengthslider = new JSlider();
	        lengthslider.setPreferredSize(new Dimension(session.getMaxTicks(), 48));
	        lengthslider.setOrientation(SwingConstants.HORIZONTAL);
	        lengthslider.setMajorTickSpacing(Session.TICK_COUNT_BASE);
	        lengthslider.setMinorTickSpacing(Session.TICK_COUNT_BASE/4);
	        lengthslider.setPaintTicks(true);
	        lengthslider.setMaximum(session.getMaxTicks()-1);
	        lengthslider.setMinimum(1);
	        int len;
			if (selectedNote.getPosEnd()>selectedNote.getPosStart()) {
				len = selectedNote.getPosEnd()-selectedNote.getPosStart();
			}
			else {
				len = selectedNote.getPosEnd()+(selectedNote.getPosStart()-session.getMaxTicks());
			}
	        lengthslider.setValue(len);
	        lengthslider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					selectedNote.setPosEnd((selectedNote.getPosStart()+lengthslider.getValue())%session.getMaxTicks());
					session.emitRefreshLoopDisplay();
				}
			});
	        length.add(lengthslider);
	        add(velocity);
	        add(length);
	        addSeparator();
	        add(delete);
	    }
	}
	
	
}
