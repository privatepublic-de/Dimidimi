package de.privatepublic.midiutils.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.JLabel;
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
	private boolean isDragging;
	private Point dragStart;
	private int dragStartNoteNumber;
	private int dragStartPosStart;
	private int dragStartPosEnd;
	
	private float noteHeight;
	private float tickwidth;
	private static final int MARGIN_SEMIS = 1;
	private int highestNote = 96-MARGIN_SEMIS;
	private int lowestNote = 12+MARGIN_SEMIS;
	
	private Map<TextAttribute, Object> textAttributes = new HashMap<TextAttribute, Object>();
	
	public LoopDisplayPanel(Session session) {
		super();
		textAttributes.put(TextAttribute.TRACKING, -0.1f);
		this.session = session;
		addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				isDragging = false;
				if (e.isPopupTrigger()) {
					if (selectedNote!=null && selectedNote.isCompleted()) {
						openPopUp(e);
					}
				}
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				isDragging = true;
				selectedNote = hitNote;				
				if (e.isPopupTrigger()) {
					if (selectedNote!=null) {
						openPopUp(e);
					}
				}
				else {
					dragStart = e.getPoint();
					Note storeStartDataNote = null;
					if (selectedNote!=null) {
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
				if (!isDragging) {
					hitNote = null;
				}
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
					if (note.isCompleted()) {
						Rectangle[] rects = getNotePositionsRect(note);
						int posy = Math.round(rects[0].y+noteHeight/2);
						for (Rectangle rect:rects) {
							if (mx<=rect.x+rect.width && mx>=rect.x && Math.abs(my-posy)<noteHeight) {
								hitNote = note;
								break;
							}
						}
						// find length resizable hit
						int rightx = rects[rects.length-1].x+rects[rects.length-1].width;
						if (mx<rightx+tickwidth && mx>rightx && Math.abs(my-posy)<noteHeight) {
							potentialResizeNote = note;
						}
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
					refreshLoopDisplay();
				}
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				if (selectedNote!=null) {
					int distY = dragStart.y-e.getY();
					int distX = e.getX()-dragStart.x;
					
					int noteOffset = (int)(distY/noteHeight);
					selectedNote.setNoteNumber(dragStartNoteNumber+noteOffset);
					
					int ticksOffset = (int)(distX/tickwidth);
					selectedNote.setPosStart((dragStartPosStart+ticksOffset+session.getMaxTicks())%session.getMaxTicks());
					selectedNote.setPosEnd((dragStartPosEnd+ticksOffset+session.getMaxTicks())%session.getMaxTicks());
					refreshLoopDisplay();
				}
				else {
					if (resizeNote!=null) {
						int distX = e.getX()-dragStart.x;
						int ticksOffset = (int)(distX/tickwidth);
						resizeNote.setPosEnd((dragStartPosEnd+ticksOffset+session.getMaxTicks())%session.getMaxTicks());
						refreshLoopDisplay();
					}
				}
			}
		});
		addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int wheelinc = e.getWheelRotation();
				Note note = selectedNote;
				if (note==null) {
					note = hitNote;
				}
				if (note!=null) {
					int vel = note.getVelocity();
					vel = Math.min(Math.max(vel-wheelinc, 0), 127);
					note.setVelocity(vel);
					refreshLoopDisplay();
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
		int displayNoteCount = highestNote-lowestNote + 2*MARGIN_SEMIS;
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
	    
		// draw grid
	    g.setStroke(STROKE_1);
		g.setColor(Theme.CURRENT.getColorOctaves());
		for (int i=0;i<11;i++) {
			float colorhue = (96-i*12)/96f;
			g.setColor(Color.getHSBColor(colorhue, Theme.CURRENT.getOctaveColorSaturation(), Theme.CURRENT.getOctaveColorBrightness()));
			int y = (int)(((highestNote+MARGIN_SEMIS)-i*12)*noteHeight);
			g.drawLine(0, y, width, y);
		}
		if (session==null) {
			return;
		}
		boolean is3based = session.getLengthQuarters()%3==0;
		int activeQuarter = pos/Session.TICK_COUNT_BASE;
		float quarterwidth = width*((float)Session.TICK_COUNT_BASE/session.getMaxTicks());
		float sixthwidth = quarterwidth/4f; 
		tickwidth = (float)width/session.getMaxTicks();
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
		int halfheight = Math.round(noteHeight/2);
		int quartheight = halfheight/2;
		for (Note note:session.getNotesList()) {
			float colorhue = (96-note.getTransformedNoteNumber(session.getTransposeIndex()))/96f; 
			Color noteColor = Color.getHSBColor(colorhue, Theme.CURRENT.getNoteColorSaturation(), Theme.CURRENT.getNoteColorBrightness());
			Color noteColorLight = Color.getHSBColor(colorhue, Theme.CURRENT.getNoteColorSaturation(), Theme.CURRENT.getNoteColorBrightness()*Theme.CURRENT.getNoteLightColorBrightnessFactor());
			
			g.setStroke(STROKE_1);
			
			if (note.isPlayed()) {
				noteColor = Theme.CURRENT.getColorPlayedNote();
				noteColorLight = noteColor;
			}
			
			Rectangle[] rects = getNotePositionsRect(note);
			
			for (Rectangle rect: rects) {
				if (note==selectedNoteRun) {
					g.setColor(Theme.CURRENT.getColorSelectedNoteOutline());
					g.fillRect(rect.x-quartheight, rect.y-quartheight, rect.width+halfheight, rect.height+halfheight);
				}
				g.setColor(noteColor);
				g.fillRect(rect.x, rect.y, rect.width, rect.height);
				
				int veloHeight = Math.round(note.getVelocity()/127f * (noteHeight-1));
				g.setColor(noteColorLight);
				g.fillRect(rect.x, rect.y, rect.width, rect.height-veloHeight);
				
				g.setColor(noteColor);
				g.drawRect(rect.x, rect.y, rect.width, rect.height);
			}
			int rightindex = rects.length-1;			
			if (note==selectedNoteRun) {
				g.setColor(Color.RED);
				int ybottom = rects[0].y+rects[0].height;
				int xright = rects[rightindex].x+rects[rightindex].width;
				g.drawLine(0, rects[0].y, width, rects[0].y);
				g.drawLine(0, ybottom, width, ybottom);
				g.drawLine(rects[0].x, 0, rects[0].x, height);
				g.drawLine(xright, 0, xright, height);
			}
			if (note==resizeNote) {
				g.setColor(Color.RED);
				g.setStroke(new BasicStroke(tickwidth));
				g.drawLine(rects[rightindex].x+rects[rightindex].width, 0, rects[rightindex].x+rects[rightindex].width, height);
			}
		}
		
		// draw pitchbend and mod
	    g.setStroke(STROKE_3);
	    final int centery = 64;//-0x2000/129;
	    for (int i=0;i<session.getLengthQuarters()*Session.TICK_COUNT_BASE;++i) {
	    	int xpos = Math.round(i*tickwidth);
	    	int mod = session.getCcList()[i];
	    	int pb = (-session.getPitchBendList()[i])/129;
	    	g.setColor(Theme.CURRENT.getColorPitchBend());
	    	if (pb<0) {
	    		g.fillRect(xpos, centery+pb, Math.round(tickwidth)+1, Math.abs(pb));
	    	}
	    	else if (pb>0) {
	    		g.fillRect(xpos, centery, Math.round(tickwidth)+1, pb);
	    	}
	    	g.setColor(Theme.CURRENT.getColorModWheel());
	    	g.drawLine(xpos, height-mod, xpos, height);
	    }
		
		if (selectedNoteRun!=null) {
			Rectangle pos = getNotePositionsRect(selectedNoteRun)[0];
			final int keywidth = (int)(tickwidth*12);
			int x = 0;
			if (pos.x<keywidth+quartheight) {
				x = width-keywidth;
			}
			g.setColor(Theme.CURRENT.getColorBackground());
			g.fillRect(x>0?x-1:x, 0, keywidth+2, height);
			for (int i=lowestNote;i<highestNote+1;i++) {
				int index = (highestNote+MARGIN_SEMIS)-i;
				int notey = Math.round(index*noteHeight-noteHeight/2);
				String notetext = Note.getConcreteNoteName(i);
				boolean blackkey = notetext.length()>1;
				FontMetrics fm = g.getFontMetrics();
				Rectangle2D rect = fm.getStringBounds(notetext, g);
				int y = notey+halfheight+quartheight;
				if (blackkey) {
					g.setColor(Color.BLACK);	
				}
				else {
					g.setColor(Color.WHITE);
				}
				g.fillRect(x, notey+1, keywidth, (int)(noteHeight*.85f)-1);
				if (blackkey) {
					g.setColor(Color.WHITE);	
				}
				else {
					g.setColor(Color.BLACK);
				}
				g.drawString(notetext, blackkey?x:x+keywidth-(int)rect.getWidth()-2, y);	
			}
		}
	}
	
	
	private Rectangle[] getNotePositionsRect(Note note) {
		int index = (highestNote+MARGIN_SEMIS)-note.getTransformedNoteNumber(session.getTransposeIndex());
		int notey = Math.round(index*noteHeight-noteHeight/2);
		int notestartx = Math.round(note.getTransformedPosStart(session.getMaxTicks(), session.getQuantizationIndex())*tickwidth);
		int noteendx = Math.round(note.isCompleted()?note.getTransformedPosEnd(session.getMaxTicks(), session.getQuantizationIndex())*tickwidth:pos*tickwidth);
		int height = Math.round(noteHeight*.85f);
		if (noteendx>=notestartx) {
			return new Rectangle[] {new Rectangle(notestartx, notey, noteendx-notestartx, height) };
		}
		else {
			return new Rectangle[] {
					new Rectangle(notestartx, notey, getWidth()-notestartx, height),
					new Rectangle(0, notey, noteendx, height)
					};
		}
		
	}
	

	public void updateLoopPosition(int pos) {
		this.pos = pos;
		repaint();
	}

	@Override
	public void loopUpdated() {
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
			minNote = 12 + MARGIN_SEMIS;
			maxNote = 96+12 - MARGIN_SEMIS;
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
	        JSlider veloslider = new JSlider();
	        veloslider.setOrientation(SwingConstants.VERTICAL);
	        veloslider.setMaximum(127);
	        veloslider.setMinimum(1);
	        veloslider.setValue(selectedNote.getVelocity());
	        veloslider.setLabelTable(VELOCITY_LABELS);
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
	        add(veloslider);
	        addSeparator();
	        add(delete);
	    }
	}
	
	private static final Dictionary<Integer, JLabel> VELOCITY_LABELS = new Hashtable<Integer, JLabel>();
	
	static {
        VELOCITY_LABELS.put(1, new JLabel("1"));
        VELOCITY_LABELS.put(32, new JLabel("32"));
        VELOCITY_LABELS.put(64, new JLabel("64"));
        VELOCITY_LABELS.put(96, new JLabel("96"));
        VELOCITY_LABELS.put(127, new JLabel("127")); 
	}
	
	private static final Stroke STROKE_1 = new BasicStroke(1);
	private static final Stroke STROKE_3 = new BasicStroke(3);
	
}
