package de.privatepublic.midiutils.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.ImageIcon;
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

import de.privatepublic.midiutils.Loop;
import de.privatepublic.midiutils.MidiHandler;
import de.privatepublic.midiutils.Note;
import de.privatepublic.midiutils.Note.TransformationProvider;
import de.privatepublic.midiutils.events.NotesUpdatedReceiver;

public class LoopDisplayPanel extends JPanel implements NotesUpdatedReceiver {

	private static final long serialVersionUID = -592444184016477559L;
	private static final Logger LOG = LoggerFactory.getLogger(LoopDisplayPanel.class);
	
	private Loop loop;
	private int pos = 0;
	private boolean animate;
	private Note resizeNote;
	private boolean isDragging;
	private boolean isDragButtonPressed;
	private boolean isSelectionDrag;
	private Point dragStart;
	private Point dragEnd;
	private Point insertNotePos;
	private Rectangle selectRectangle = new Rectangle();
	private Note draggedNote = null;
	private boolean insertNoteMode = false;
	private final Note insertNote = new Note();
	private float noteHeight;
	private float tickwidth;
	private static final int MARGIN_SEMIS = 1;
	private static final int META_KEY = InputEvent.ALT_DOWN_MASK;
	private int highestNote = 96-MARGIN_SEMIS;
	private int lowestNote = 12+MARGIN_SEMIS;
	private CopyOnWriteArrayList<Note> selectedNotes = new CopyOnWriteArrayList<Note>();
	private Map<TextAttribute, Object> textAttributes = new HashMap<TextAttribute, Object>();
	
	public LoopDisplayPanel(Loop loop) {
		super();
		textAttributes.put(TextAttribute.TRACKING, -0.1f);
		this.loop = loop;
		
		addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
				if ((e.getModifiersEx()&META_KEY)==0) {
					insertNoteMode = false;
					setCursor(Cursor.getDefaultCursor());
					repaint();
				}
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				if ((e.getModifiersEx() & META_KEY)!=0) {
					Point mouse = MouseInfo.getPointerInfo().getLocation();
					Point comp = getLocationOnScreen();
					updateInsertNote(new Point(mouse.x-comp.x, mouse.y-comp.y));
					insertNoteMode = true;
					setCursor(PEN_CURSOR);
					repaint();
				}
				if (e.getKeyCode()==KeyEvent.VK_BACK_SPACE) {
					deleteSelectedNotes();
				}
				if (e.getKeyCode()==KeyEvent.VK_M) {
					loop.setMuted(!loop.isMuted());
				}
				if (e.getKeyCode()==KeyEvent.VK_S) {
					loop.setSolo(!loop.isSolo());
				}
				
			}
		});
		setFocusable(true);
		
		addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				isDragging = false;
				isDragButtonPressed = false;
				isSelectionDrag = false;
				draggedNote = null;
				
				if (insertNoteMode) {
					setCursor(Cursor.getDefaultCursor());
					if (findHitNote(loop.getNotesList(), e.getPoint())==null) {
						updateInsertNote(e.getPoint());
						Note n = new Note(insertNote, 0);
						n.setVelocity(100);
						loop.getNotesList().add(n);
						selectedNotes.clear();
						n.stashOrigin();
						draggedNote = n;
						selectedNotes.add(n);
					}
				}
				else {
					repaint();
					if (e.isPopupTrigger()) {
						handlePopupTrigger(e);
					}
				}
				onRefreshLoopDisplay();
			}
			
			private void handlePopupTrigger(MouseEvent e) {
				if (selectedNotes.size()>1) {
					Note hitNote = findHitNote(selectedNotes, e.getPoint());
					if (hitNote!=null) {
						openPopUp(e);
					}
				}
				else {
					Note hitNote = findHitNote(loop.getNotesList(), e.getPoint());
					if (hitNote!=null) {
						selectedNotes.clear();
						selectedNotes.add(hitNote);
						openPopUp(e);
					}
				}
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				requestFocus();
				if (e.isPopupTrigger()) {
					handlePopupTrigger(e);
				}
				else if (e.getButton()==MouseEvent.BUTTON1) {
					isDragButtonPressed = true;
					dragStart = e.getPoint();
					if (resizeNote==null) {
						Note hitNote = findHitNote(selectedNotes, dragStart);
						if (hitNote!=null) {
							// is within already selected notelist
							draggedNote = hitNote;
							hitNote.stashOrigin();
							onRefreshLoopDisplay();
						}
						else {
							if (!e.isShiftDown()) {
								// multiple select
								selectedNotes.clear();
							}
							hitNote = findHitNote(loop.getNotesList(), dragStart);
							if (hitNote!=null) {
								// found a note to select
								if (!selectedNotes.contains(hitNote)) {
									selectedNotes.add(hitNote);
								}
								hitNote.stashOrigin();
								draggedNote = hitNote;
							}
							if (selectedNotes.size()>0) {
								isSelectionDrag = false;
							}
							else {
								// clicked outside note, start selection
								isSelectionDrag = true;
								selectedNotes.clear();
								selectRectangle.x = dragStart.x;
								selectRectangle.y = dragStart.y;
								selectRectangle.width = 0;
								selectRectangle.height = 0;
								dragEnd = e.getPoint();
							}
						}
					}
					else {
						selectedNotes.clear();
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
				requestFocusInWindow();
			}
			
			private void openPopUp(MouseEvent e){
				PopUpMenu menu = new PopUpMenu();
		        menu.show(e.getComponent(), e.getX(), e.getY());
		    }
			
		});
		addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseMoved(MouseEvent e) {
				if (!isDragging) {
					int mx = e.getX();
					int my = e.getY();
					Note found2resize = null;
					for (Note note:loop.getNotesList()) {
						if (note.isCompleted()) {
							Rectangle[] rects = getNotePositionsRect(note);
							int posy = Math.round(rects[0].y+noteHeight/2);
							// find length resizable hit
							int rightx = rects[rects.length-1].x+rects[rects.length-1].width;
							if (mx<rightx+tickwidth && mx>rightx && Math.abs(my-posy)<noteHeight) {
								found2resize = note;
								found2resize.stashOrigin();
								break;
							}
						}
					}
					boolean repaint = found2resize!=resizeNote;
					resizeNote = found2resize;
					if (repaint) {
						repaint();
					}
				}
				if (resizeNote!=null) {
					setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
				}
				else if (isDragging || isListHit(loop.getNotesList(), e.getPoint())) {
					setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				}
				else if (insertNoteMode) {
					setCursor(PEN_CURSOR);
					updateInsertNote(e.getPoint());
					repaint();
				}
				else {
					setCursor(Cursor.getDefaultCursor());
				}
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				isDragging = isDragButtonPressed && !isSelectionDrag;
				if (resizeNote!=null) {
					int distX = e.getX()-dragStart.x;
					int ticksOffset = (int)(distX/tickwidth);
					resizeNote.setPosEnd((resizeNote.getOriginPosEnd()+ticksOffset+loop.getMaxTicks())%loop.getMaxTicks());
					MidiHandler.instance().sendNoteOffMidi(loop, resizeNote.getNoteNumber());
					onRefreshLoopDisplay();
					return;
				}
				if (isSelectionDrag) {
					dragEnd = e.getPoint();
					
					int rw = dragEnd.x-dragStart.x;
					int rh = dragEnd.y-dragStart.y;
					int rx = dragStart.x;
					int ry = dragStart.y;
					if (rw<0) {
						rx += rw;
						rw = -rw;
					}
					if (rh<0) {
						ry += rh;
						rh = -rh;
					}
					selectRectangle.x = rx;
					selectRectangle.y =ry;
					selectRectangle.width = rw;
					selectRectangle.height = rh;
					selectedNotes.clear();
					for (Note note: loop.getNotesList()) {
						for (Rectangle nr: getNotePositionsRect(note)) {
							if (selectRectangle.intersects(nr)) {
								selectedNotes.add(note);
								note.stashOrigin();
								break;
							}		
						}
					}
					repaint();
				}
				else {
					if (selectedNotes.size()>0) {
						for (Note note:selectedNotes) {
							
							int distY = dragStart.y-e.getY();
							int distX = e.getX()-dragStart.x;
							
							int noteOffset = Math.round(distY/noteHeight);
							note.setNoteNumber(note.getOriginNoteNumber()+noteOffset);

							int ticksOffset = (int)(distX/tickwidth);
							note.setPosStart((note.getOriginPosStart()+ticksOffset+loop.getMaxTicks())%loop.getMaxTicks());
							note.setPosEnd((note.getOriginPosEnd()+ticksOffset+loop.getMaxTicks())%loop.getMaxTicks());
						}
						onRefreshLoopDisplay();
					}
				}
			}
		});
		addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (selectedNotes.size()>0) {
					int wheelinc = e.getWheelRotation();
					for (Note note:selectedNotes) {
						int vel = note.getVelocity();
						vel = Math.min(Math.max(vel-wheelinc, 0), 127);
						note.setVelocity(vel);
					}
					onRefreshLoopDisplay();
				}
			}
		});
		
		if (loop!=null) {
			loop.registerReceiver(this);
		}
		
		LOG.debug("Created LoopDisplayPanel");
	}
	
	
	@Override
	public void paint(Graphics go) {
		
		
		Graphics2D g = (Graphics2D)go;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		int width = getWidth();
		int height = getHeight() + 1 /* one more because of render artefacts in swing panel */;
		int displayNoteCount = highestNote-lowestNote + 2*MARGIN_SEMIS;
		noteHeight = height*(1f/displayNoteCount);
		g.setColor(Theme.APPLY.colorBackground());
		g.fillRect(0, 0, width, height);
		
		// draw midi out channel text
		String channelText = ""+((loop!=null?loop.getMidiChannelOut():0)+1);
		g.setColor(loop!=null?loop.getChannelColor():Theme.APPLY.colorMidiOutBig());  
		float fontSize = 20.0f;
	    Font font = Theme.APPLY.fontMidiBig().deriveFont(fontSize);
	    int fheight = g.getFontMetrics(font).getHeight();
	    fontSize = (height / (float)fheight ) * fontSize;
	    textAttributes.put(TextAttribute.SIZE, fontSize);
	    g.setFont(Theme.APPLY.fontMidiBig().deriveFont(textAttributes));
	    int fwidth = g.getFontMetrics(g.getFont()).stringWidth(channelText);
	    fheight = g.getFontMetrics(g.getFont()).getHeight();
	    g.drawString(channelText, width/2-fwidth/2, height-height/5);
	    
	    
	    g.setFont(Theme.APPLY.fontNotes());
	    
		// draw pitchbend and mod
	    if (loop!=null) {
		    g.setStroke(STROKE_3);
		    final int centery = height/2;
		    for (int i=0;i<loop.getLengthQuarters()*Loop.TICK_COUNT_BASE;++i) {
			    	int xpos = Math.round(i*tickwidth);
			    	int mod = loop.getModWheelList()[i];
			    	int pressure = loop.getPressureList()[i];
			    	int pb = (-loop.getPitchBendList()[i])/129;
			    	g.setColor(Theme.APPLY.colorPitchBend());
			    	if (pb<0) {
			    		g.fillRect(xpos, centery+pb, Math.round(tickwidth)+1, Math.abs(pb));
			    	}
			    	else if (pb>0) {
			    		g.fillRect(xpos, centery, Math.round(tickwidth)+1, pb);
			    	}
			    	g.setColor(Theme.APPLY.colorModWheel());
			    	g.drawLine(xpos, height-mod, xpos, height);
			    	g.drawLine(xpos, 0, xpos, pressure);
		    }
	    }

	    
		// draw grid
	    g.setStroke(STROKE_1);
		if (loop==null) {
			return;
		}
		
		boolean is3based = loop.getLengthQuarters()%3==0;
		int activeQuarter = pos/Loop.TICK_COUNT_BASE;
		float quarterwidth = width*((float)Loop.TICK_COUNT_BASE/loop.getMaxTicks());
		float sixthwidth = quarterwidth/4f; 
		tickwidth = (float)width/loop.getMaxTicks();
		
		FontMetrics fm = g.getFontMetrics();
		for (int i=lowestNote;i<highestNote+1;i++) {
			int index = (highestNote+MARGIN_SEMIS)-i;
			int notey = Math.round(index*noteHeight-noteHeight/2);
			String notetext = loop.isDrums()?Note.getDrumName(i):Note.getName(i)+" "+(i/12-2);
			int ty = (int)(notey+((noteHeight - fm.getHeight()) / 2) + fm.getAscent());
			g.setColor(Theme.APPLY.colorGrid());
			g.drawLine(0, notey, width, notey);
			g.setColor(Theme.APPLY.colorNoteLabels());
			g.drawString(notetext, 5, ty);
		}
		
		
		for (int i=0;i<loop.getLengthQuarters()*4;i++) {
			int xpos = (int)(i*sixthwidth);
			g.setColor(Theme.APPLY.colorGrid());
			g.drawLine(xpos, 0, xpos, height);
			if (i%4==0) { 
				// highlight quarters
				g.setColor(Theme.APPLY.colorGridIntense());
				g.drawLine(xpos+1, 0, xpos+1, height);
			}
			if ((is3based && i%12==0) || (!is3based && i%16==0) && i>0) {
				// highlight first beat
				g.setColor(Theme.APPLY.colorGridIntense());
				g.drawLine(xpos+3, 0, xpos+3, height);
				g.drawLine(xpos-1, 0, xpos-1, height);
				g.drawLine(xpos-3, 0, xpos-3, height);
			}
			if (MidiHandler.ACTIVE && (i/4 == activeQuarter)) {
				g.setColor(Theme.APPLY.colorActiveQuarter());
				g.fillRect(xpos, height-(int)noteHeight/2, Math.round(sixthwidth)+1, height);
				g.fillRect(xpos, 0, Math.round(sixthwidth)+1, (int)noteHeight/2);
			}
		}
		
		// draw playhead
		if (MidiHandler.ACTIVE) {
			g.setColor(Theme.APPLY.colorPlayhead());
			float playheadx = width*((float)pos/loop.getMaxTicks());
			g.fillRect((int)(playheadx-tickwidth/2), 0, (int)tickwidth, height);
		}
		
		// draw notes
		int halfheight = Math.round(noteHeight/2);
		int quartheight = halfheight/2;
		
		boolean isSingleSelection = selectedNotes.size()==1;
		
		List<Note> useList = MidiHandler.ACTIVE?loop.getNotesListPlayback():loop.getNotesList(); 
		
		for (Note note: useList) {
			boolean isSelected = selectedNotes.contains(note);
			
			Color noteColor = loop.getNoteColor(isSelected);
			Color noteColorLight = loop.getNoteColorHighlighted(isSelected);
			
			g.setStroke(STROKE_1);
			
			if (note.isPlayed()) {
				noteColor = loop.getNoteColorPlayed();
				noteColorLight = noteColor;
			}
			
			Rectangle[] rects = getNotePositionsRect(note);
			
			if (animate && MidiHandler.ACTIVE && loop.isAudible()) {
				int start = note.getPosStart(loop);
				int end = note.getPosEnd(loop);
				int length = start>end?end+loop.getMaxTicks()-start:end-start;
				int position = start>end?pos-start+loop.getMaxTicks():pos-start;
				if (position<0) {
					position += loop.getMaxTicks();
				}
				float percent = position/(float)length;
				if (!note.isPlayed()) {
					percent *= percent;
				}
				float offset = ((1-(percent-1)*(percent-1))*noteHeight*.5f);
				g.setColor(loop.getNoteColorHighlighted(false));
				for (Rectangle rect:rects) {
//					g.drawRect(rect.x-offset, rect.y-offset, rect.width+offset*2, rect.height+offset*2);
					g.draw(new RoundRectangle2D.Float(rect.x-offset, rect.y-offset, rect.width+offset*2.0f, rect.height+offset*2.0f, 20, 20));
				}
			}
			
			for (Rectangle rect: rects) {
				if (isSelected) {
					g.setColor(Theme.APPLY.colorSelectedNoteOutline());
					g.fillRect(rect.x-quartheight, rect.y-quartheight, rect.width+halfheight, rect.height+halfheight);
				}
				
				g.setColor(noteColor);
				g.fillRect(rect.x, rect.y, rect.width, rect.height);
//				RoundRectangle2D roundrect = new RoundRectangle2D.Float(rect.x, rect.y, rect.width, rect.height, 6, 6);
//				g.fill(roundrect);
				
				int veloHeight = Math.round(note.getVelocity()/127f * (noteHeight-1));
				g.setColor(noteColorLight);
				g.fillRect(rect.x, rect.y, rect.width, rect.height-veloHeight);
//				g.fill(new RoundRectangle2D.Float(rect.x, rect.y, rect.width, rect.height-veloHeight, 6, 6));
				
//				g.setColor(noteColor);
				g.drawRect(rect.x, rect.y, rect.width, rect.height);
//				g.draw(roundrect);
				
			}
			int rightindex = rects.length-1;			
			if ((isSingleSelection && isSelected && isDragging) || (isDragging && note==draggedNote)) {
				g.setColor(Theme.APPLY.colorGridHighlight());
				int ybottom = rects[0].y+rects[0].height;
				int xright = rects[rightindex].x+rects[rightindex].width;
				g.drawLine(0, rects[0].y, width, rects[0].y);
				g.drawLine(0, ybottom, width, ybottom);
				g.drawLine(rects[0].x, 0, rects[0].x, height);
				g.drawLine(xright, 0, xright, height);
			}
			if (note==resizeNote) {
				g.setColor(Theme.APPLY.colorGridHighlight());
				g.setStroke(new BasicStroke(tickwidth));
				g.drawLine(rects[rightindex].x+rects[rightindex].width, rects[rightindex].y-(int)(noteHeight/2), rects[rightindex].x+rects[rightindex].width, rects[rightindex].y+rects[rightindex].height+(int)(noteHeight/2));
			}
			
		}
		
	    if (!loop.isAudible()) {
	    	g.setColor(Theme.APPLY.colorMuted());
			g.fillRect(0, 0, width, height);
	    }
		
		if ((selectedNotes.size()>0 && isDragging)) {
			// draw scale
			int x = getNotePositionsRect(selectedNotes.get(0))[0].x;;
			int lasty = -10;
			for (int i=lowestNote;i<highestNote+1;i++) {
				int index = (highestNote+MARGIN_SEMIS)-i;
				int notey = Math.round(index*noteHeight-noteHeight/2);
				String notetext = loop.isDrums()?Note.getDrumName(i):Note.getName(i);
				Rectangle2D rect = fm.getStringBounds(notetext, g);
				int y = (int)(notey+((noteHeight - fm.getHeight()) / 2) + fm.getAscent());
				if (Math.abs(y-lasty)>=rect.getHeight()) {
					g.setColor(Theme.APPLY.colorBackground());
					g.fillRect(x+(int)rect.getX()+2, y+(int)rect.getY(), (int)rect.getWidth()+6, (int)rect.getHeight());
					g.setColor(Theme.APPLY.colorNoteLabels());	
					g.drawString(notetext, x+4, y);
					lasty = y;
				}
			}
		}
		
		if ((insertNotePos!=null && insertNoteMode)) {
			
			Rectangle noteRect = getNotePositionsRect(insertNote)[0];
			g.setStroke(STROKE_1);
			g.setColor(Theme.APPLY.colorSelectionRectangle());
			g.drawRect(0, noteRect.y, width, (int)noteHeight);
			g.setColor(loop.getNoteColorHighlighted(false));
			g.fillRect(noteRect.x, noteRect.y, noteRect.width, noteRect.height);
			
			int value = insertNote.getNoteNumber();			
			String notetext = loop.isDrums()?Note.getDrumName(value):Note.getName(value);
			Rectangle2D rect = fm.getStringBounds(notetext, g);
			int x = noteRect.x-32; // cursor size
			if (x<0) {
				x = noteRect.x+24;
			}
			int y = (int) (noteRect.y + noteHeight/2);
			g.setColor(Theme.APPLY.colorBackground());
			g.fillRect(x+(int)rect.getX()+2, y+(int)rect.getY(), (int)rect.getWidth()+6, (int)rect.getHeight());
			g.setColor(Theme.APPLY.colorNoteLabels());	
			g.drawString(notetext, x+4, y);
		}
		
		if (isSelectionDrag) {
			g.setStroke(STROKE_3);
			g.setColor(Theme.APPLY.colorSelectionRectangle());
			g.drawRect(selectRectangle.x, selectRectangle.y, selectRectangle.width, selectRectangle.height);
		}
	}
	
	
	public void setAnimate(boolean animate) {
		this.animate = animate;
	}


	private Rectangle[] getNotePositionsRect(Note note) {
		int index = (highestNote+MARGIN_SEMIS)-note.getNoteNumber(loop);
		float height = noteHeight*.8f;
		int notey = Math.round(index*noteHeight-noteHeight/2+(noteHeight-height)/2);
		int notestartx = Math.round(note.getPosStart(loop)*tickwidth);
		int noteendx = Math.round(note.isCompleted()?note.getPosEnd(loop)*tickwidth:pos*tickwidth);
		if (noteendx>=notestartx) {
			return new Rectangle[] {new Rectangle(notestartx, notey, noteendx-notestartx, (int) height) };
		}
		else {
			return new Rectangle[] {
					new Rectangle(notestartx, notey, getWidth()-notestartx, (int) height),
					new Rectangle(0, notey, noteendx, (int) height)
					};
		}
	}
	
	private boolean isListHit(List<Note> notelist, Point hitPoint) {
		for (Note note:notelist) {
			Rectangle[] rects = getNotePositionsRect(note);
			for (Rectangle rect:rects) {
				if (rect.contains(hitPoint)) {
					return true;
				}
			}
			
		}
		return false;
	}
	
	private Note findHitNote(List<Note> notelist, Point hitPoint) {
		for (Note note: notelist) {
			for (Rectangle rect: getNotePositionsRect(note)) {
				if (rect.contains(hitPoint)) {
					return note;
				}
			}
		}
		return null;
	}
	
	public void updateInsertNote(Point p) {
		int value = (int)(highestNote+MARGIN_SEMIS-(p.getY()-noteHeight/2)/noteHeight);
		value = value - TransformationProvider.T_STEPS[loop.getTransposeIndex()];
		int pos = (int)((p.getX()-16)/tickwidth);
		insertNotePos = new Point(p); // cloned p
		insertNote.setNoteNumber(value);
		insertNote.setPosStart(pos);
		int length = Note.TransformationProvider.Q_STEPS[loop.getQuantizationIndex()];
		if (length>0) {
			length--;
		}
		else {
			length=5;
		}
		insertNote.setPosEnd((pos+length)%loop.getMaxTicks());
		int qstart = insertNote.getPosStart(loop);
		int qend = insertNote.getPosEnd(loop);
		insertNote.setPosStart(qstart);
		insertNote.setPosEnd(qend);
	}
	
	public void updateLoopPosition(int pos) {
		this.pos = pos;
		repaint();
	}

	@Override
	public void onNotesUpdated() {
		calculateNoteExtents();
		repaint();
	}

	@Override
	public void onRefreshLoopDisplay() {
		calculateNoteExtents();
		repaint();
	}
	
	private void calculateNoteExtents() {
		if (loop!=null && loop.isDrums()) {
			lowestNote = 35;
			highestNote = 49; 
			return;
		}
		int maxNote = 12;
		int minNote = 96+12; 
		for (Note dc:loop.getNotesList()) {
			minNote = Math.min(dc.getNoteNumber(loop), minNote);
			maxNote = Math.max(dc.getNoteNumber(loop), maxNote);
		}
		if (loop.getNotesList().size()==0) {
			minNote = 12*3 - MARGIN_SEMIS;
			maxNote = 12*5 + MARGIN_SEMIS;
		}
		if (maxNote-minNote<12 && !isDragButtonPressed) {
			int add = (12-(maxNote-minNote))/2;
			maxNote += add;
			minNote -= add;
		}
		highestNote = maxNote;
		lowestNote = minNote;
	}
	
	private void deleteSelectedNotes() {
		for (Note selectedNote: selectedNotes) {
			loop.clearNote(selectedNote);
		}
		selectedNotes.clear();
	}
	
	class PopUpMenu extends JPopupMenu {
		private static final long serialVersionUID = 9049098677413712819L;

		public PopUpMenu(){
	    	JMenuItem delete = new JMenuItem("Delete");
	        delete.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					deleteSelectedNotes();
				}
			});
	        JSlider veloslider = new JSlider();
	        veloslider.setOrientation(SwingConstants.VERTICAL);
	        veloslider.setMaximum(127);
	        veloslider.setMinimum(1);
	        int avrvelo = 0;
	        for (Note selectedNote: selectedNotes) {
	        	avrvelo += selectedNote.getVelocity();
	        }
	        avrvelo /= selectedNotes.size();
	        veloslider.setValue(avrvelo);
	        veloslider.setLabelTable(VELOCITY_LABELS);
	        veloslider.setMajorTickSpacing(32);
	        veloslider.setMinorTickSpacing(8);
	        veloslider.setPaintTicks(true);
	        veloslider.setPaintLabels(true);
	        veloslider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					for (Note selectedNote: selectedNotes) {
						selectedNote.setVelocity(veloslider.getValue());
					}
					loop.triggerRefreshLoopDisplay();;
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
	private static Cursor PEN_CURSOR;
	
	static {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		PEN_CURSOR = toolkit.createCustomCursor(new ImageIcon(Res.IMAGE_CURSOR_EDIT()).getImage() , new Point(1, 30), "custom cursor");
	}

	public void selectAll() {
		selectedNotes.clear();
		selectedNotes.addAll(loop.getNotesList());
	}

}
