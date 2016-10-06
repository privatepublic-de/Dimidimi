package de.privatepublic.midiutils.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
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
import java.awt.font.TextAttribute;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

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
	private Note resizeNote;
	private Point dragStart;
	
	private int dragStartNoteNumber;
	private int dragStartPosStart;
	private int dragStartPosEnd;
	
	private float noteHeight;
	private float tickwidth;
	private static final int bufferSemis = 5;
	private int highestNote = 96-bufferSemis;
	private int lowestNote = 12+bufferSemis;
	
	private Map<TextAttribute, Object> textAttributes = new HashMap<TextAttribute, Object>();
	
	public LoopDisplayPanel(Session session) {
		super();
		textAttributes.put(TextAttribute.TRACKING, -0.1f);
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
					Note storeStartDataNote = null;
					if (selectedNote!=null && selectedNote.isCompleted()) {
						storeStartDataNote = selectedNote;
					}
					if (resizeNote!=null) {
						storeStartDataNote = resizeNote;
					}
					if (storeStartDataNote!=null) {
						dragStartNoteNumber = storeStartDataNote.getNoteNumber();
						dragStartPosStart = storeStartDataNote.getPosStart();
						dragStartPosEnd = storeStartDataNote.getPosEnd();
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
				Note hitNoteBefore = hitNote;
				Note resizeNoteBefore = resizeNote;
				int mx = e.getX();
				int my = e.getY();
				hitNote = null;
				Note potentialResizeNote = null;
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
					// find length resizable hit
					if (mx<noteline.getX2()+tickwidth && mx>noteline.getX2() && Math.abs(my-noteline.getY1())<noteHeight) {
						potentialResizeNote = note;
					}
				}
				if (hitNote!=null) {
					setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				}
				else {
					if (potentialResizeNote!=null) {
						setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
					}
					else {
						setCursor(Cursor.getDefaultCursor());						
					}
				}
				resizeNote = potentialResizeNote;
				if (hitNote!=hitNoteBefore || resizeNote!=resizeNoteBefore) {
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
					
					int ticksOffset = (int)(distX/tickwidth);
					selectedNote.setPosStart((dragStartPosStart+ticksOffset+session.getMaxTicks())%session.getMaxTicks());
					selectedNote.setPosEnd((dragStartPosEnd+ticksOffset+session.getMaxTicks())%session.getMaxTicks());
					
					repaint();
				}
				else {
					if (resizeNote!=null) {
						int distX = e.getX()-dragStart.x;
						int ticksOffset = (int)(distX/tickwidth);
						resizeNote.setPosEnd((dragStartPosEnd+ticksOffset+session.getMaxTicks())%session.getMaxTicks());
						repaint();
					}
				}
			}
		});
		if (session!=null) {
			session.registerAsReceiver(this);
		}
	}
	
	
	@Override
	public void paint(Graphics go) {
		Graphics2D g = (Graphics2D)go;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		int width = getWidth();
		int height = getHeight();
		int displayNoteCount = highestNote-lowestNote + 2*bufferSemis;
		noteHeight = height*(1f/displayNoteCount);
		g.setColor(Theme.CURRENT.getColorBackground());
		g.fillRect(0, 0, width, height);
		// draw midi out channel text
		String channelText = "#"+((session!=null?session.getMidiChannelOut():0)+1);
		g.setColor(Theme.CURRENT.getColorMidiOutBig());
		float fontSize = 20.0f;
	    Font font = Theme.CURRENT.getFontMidiBig().deriveFont(fontSize);
	    int fheight = g.getFontMetrics(font).getHeight();
	    fontSize = (height / fheight ) * fontSize;
	    textAttributes.put(TextAttribute.SIZE, fontSize);
	    g.setFont(Theme.CURRENT.getFontMidiBig().deriveFont(textAttributes));
	    int fwidth = g.getFontMetrics(g.getFont()).stringWidth(channelText);
	    g.drawString(channelText, width-fwidth, height);
	    g.setFont(Theme.CURRENT.getFontNotes());
	    
	    // draw pitchbend and mod
	    g.setStroke(new BasicStroke(3));
	    tickwidth = (float)width/session.getMaxTicks();
	    final int centery = height/2;//-0x2000/129;
	    g.setColor(Theme.CURRENT.getColorPitchBend());
	    g.drawLine(0, centery, width, centery);
	    for (int i=0;i<session.getLengthQuarters()*Session.TICK_COUNT_BASE;++i) {
	    	int xpos = Math.round(i*tickwidth);
	    	int mod = session.getCcList()[i];
	    	int pb = (-session.getPitchBendList()[i])/129;
	    	g.setColor(Theme.CURRENT.getColorPitchBend());
	    	if (pb<0) {
	    		g.fillRect(xpos, centery+pb, Math.round(tickwidth)+1, Math.abs(pb));
	    	}
	    	else {
	    		g.fillRect(xpos, centery, Math.round(tickwidth)+1, pb);
	    	}
	    	g.setColor(Theme.CURRENT.getColorModWheel());
	    	g.drawLine(xpos, height-mod, xpos, height);
	    }
		
		// draw grid
	    g.setStroke(new BasicStroke(1));
		g.setColor(Theme.CURRENT.getColorOctaves());
		for (int i=0;i<11;i++) {
			float colorhue = (96-i*12)/96f;
			g.setColor(Color.getHSBColor(colorhue, Theme.CURRENT.getOctaveColorSaturation(), Theme.CURRENT.getOctaveColorBrightness()));
			int y = (int)(((highestNote+bufferSemis)-i*12)*noteHeight);
			g.drawLine(0, y, width, y);
		}
		if (session==null) {
			return;
		}
		boolean is3based = session.getLengthQuarters()%3==0;
		int activeQuarter = pos/Session.TICK_COUNT_BASE;
		float quarterwidth = width*((float)Session.TICK_COUNT_BASE/session.getMaxTicks());
		float sixthwidth = quarterwidth/4f; 
		for (int i=0;i<session.getLengthQuarters()*4;i++) {
			int xpos = (int)(i*sixthwidth);
			if (i/4 == activeQuarter) {
				g.setColor(Theme.CURRENT.getColorActiveQuarter());
				g.fillRect(xpos, height-height/30, Math.round(sixthwidth), height);
				g.fillRect(xpos, 0, Math.round(sixthwidth), height/30);
			}
			g.setColor(Theme.CURRENT.getColorGrid());
			g.drawLine(xpos, 0, xpos, height);
			if (i%4==0) { 
				// highlight quarters
				g.setColor(Theme.CURRENT.getColorGridIntense());
				g.drawLine(xpos+1, 0, xpos+1, height);
			}
			if ((is3based && i%12==0) || (!is3based && i%16==0) && i>0) {
				// highlight first beat
				g.setColor(Theme.CURRENT.getColorGridIntense());
				g.drawLine(xpos+3, 0, xpos+3, height);
				g.drawLine(xpos-1, 0, xpos-1, height);
				g.drawLine(xpos-3, 0, xpos-3, height);
			}
		}
		
		// draw playhead
		g.setColor(Theme.CURRENT.getColorPlayhead());
		float playheadx = width*((float)pos/session.getMaxTicks());
		
		g.fillRect((int)(playheadx-tickwidth/2), 0, (int)tickwidth, height);

		// draw notes
		Note selectedNoteRun = selectedNote;
		if (selectedNoteRun==null) {
			selectedNoteRun = hitNote;
		}
		for (Note note:session.getNotesList()) {
			float colorhue = (96-note.getTransformedNoteNumber(session.getTransposeIndex()))/96f; 
			Color noteColor = Color.getHSBColor(colorhue, Theme.CURRENT.getNoteColorSaturation(), Theme.CURRENT.getNoteColorBrightness());
			if (note.isPlayed()) {
				g.setColor(Theme.CURRENT.getColorPlayedNote());
			}
			else {
				g.setColor(noteColor);
			}
			Line2D.Float notepos = getNotePosition(note);
			float notey = notepos.y1;
			float notestartx = notepos.x1;
			float noteendx = notepos.x2;
			float veloHeight = Math.max(note.getVelocity()/127f * noteHeight*2, noteHeight/3f);

			if (noteendx>=notestartx) {
				if (note==selectedNoteRun) {
					g.setColor(Theme.CURRENT.getColorSelectedNoteOutline());
					g.setStroke(new BasicStroke(veloHeight*2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
					g.drawLine((int)notestartx, (int)notey, (int)noteendx, (int)notey);
					g.setColor(noteColor);
				}
				g.setStroke(new BasicStroke(veloHeight, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
				g.drawLine((int)notestartx, (int)notey, (int)noteendx, (int)notey);
			}
			else {
				if (note==selectedNoteRun) {
					g.setColor(Theme.CURRENT.getColorSelectedNoteOutline());
					g.setStroke(new BasicStroke(veloHeight*2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
					g.drawLine((int)0, (int)notey, (int)noteendx, (int)notey);
					g.drawLine((int)notestartx, (int)notey, (int)width, (int)notey);
					g.setColor(noteColor);
				}
				g.setStroke(new BasicStroke(veloHeight, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
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
				g.setColor(Theme.CURRENT.getColorSelectedNoteText());
				g.drawString(notetext, notestartx, y);
			}
		}
		if (resizeNote!=null) {
			Line2D.Float npos = getNotePosition(resizeNote);
			g.setColor(Color.RED);
			g.setStroke(new BasicStroke(tickwidth));
			g.drawLine((int)npos.x2, (int)(npos.y2 - noteHeight/2), (int)npos.x2, (int)(npos.y2 + noteHeight/2));
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
		calculateNoteExtents();
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
