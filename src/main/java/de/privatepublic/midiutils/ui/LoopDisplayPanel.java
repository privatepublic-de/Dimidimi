package de.privatepublic.midiutils.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
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
import java.util.ArrayList;
import java.util.List;

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
import de.privatepublic.midiutils.NoteRun;
import de.privatepublic.midiutils.events.Event;
import de.privatepublic.midiutils.events.LoopUpdateReceiver;

public class LoopDisplayPanel extends JPanel implements LoopUpdateReceiver {

	private static final long serialVersionUID = -592444184016477559L;
	private static final Logger LOG = LoggerFactory.getLogger(LoopDisplayPanel.class);
	
	private int pos = 0;
	
	private List<NoteRun> noteList = new ArrayList<NoteRun>();
	private List<Line2D.Float> hitrects = new ArrayList<Line2D.Float>();
	private boolean listsaresynced = false;
	private Color colorActiveQuarter = Color.decode("#ff9900");
	private Color colorOctaves = Color.decode("#dddddd");
	
	private NoteRun hitNote;
	private NoteRun selectedNote;
	private Point dragStart;
	
	private int dragStartNoteNumber;
	private int dragStartPosStart;
	private int dragStartPosEnd;
	
	private float noteheight;
	
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
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			private void openPopUp(MouseEvent e){
				PopUpMenu menu = new PopUpMenu();
		        menu.show(e.getComponent(), e.getX(), e.getY());
		    }
			
		});
		addMouseMotionListener(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent e) {
				if (!listsaresynced) {
					return;
				}
				NoteRun hitNoteBefore = hitNote;
				synchronized(noteList) {
					int mx = e.getX();
					int my = e.getY();
					int i = 0;
					hitNote = null;
					for (NoteRun note:noteList) {
						Line2D.Float noteline = hitrects.get(i);
						if (noteline.getX1()<=noteline.getX2()) {
							if (mx<=noteline.getX2() && mx>=noteline.getX1() && Math.abs(my-noteline.getY1())<noteheight) {
								hitNote = note;
								break;
							}
						}
						else {
							if ((mx<=noteline.getX2() || mx>=noteline.getX1()) && Math.abs(my-noteline.getY1())<noteheight) {
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
				// TODO Auto-generated method stub
				//LOG.info("Dragged {}", e);
				if (selectedNote!=null && selectedNote.isCompleted()) {
					int distY = dragStart.y-e.getY();
					int distX = e.getX()-dragStart.x;
					
					int noteOffset = (int)(distY/noteheight);
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
		int width = getWidth();
		int height = getHeight();
		noteheight = height*(1f/127);
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
				g.setColor(Color.DARK_GRAY);
				g.drawLine(xpos+1, 0, xpos+1, height);
				g.setColor(Color.GRAY);
			}
			if ((is3based && i%12==0) || (!is3based && i%16==0)) {
				g.setColor(Color.DARK_GRAY);
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
		NoteRun selectedNoteRun = selectedNote;
		if (selectedNoteRun==null) {
			selectedNoteRun = hitNote;
		}
		synchronized(noteList) {
			hitrects.clear();
			for (NoteRun dc:noteList) {
				int no = 127 - dc.getTransformedNoteNumber();
				float colorhue = no/127f; 
				Color noteColor = Color.getHSBColor(colorhue, .9f, .7f);
				if (dc.isPlayed()) {
					g.setColor(Color.ORANGE);
				}
				else {
					g.setColor(noteColor);
				}
				float notey = no*noteheight;
				float notestartx = dc.getTransformedPosStart()*tickwidth;
				float noteendx = dc.isCompleted()?dc.getTransformedPosEnd()*tickwidth:pos*tickwidth;
				float velo = Math.max(dc.getVelocity()/127f * tickwidth*2,1);
				
				if (noteendx>=notestartx) {
					if (dc==selectedNoteRun) {
						g.setColor(Color.LIGHT_GRAY);
						g.setStroke(new BasicStroke(velo*4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
						g.drawLine((int)notestartx, (int)notey, (int)noteendx, (int)notey);
						g.setColor(noteColor);
					}
					g.setStroke(new BasicStroke(velo, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
					g.drawLine((int)notestartx, (int)notey, (int)noteendx, (int)notey);
				}
				else {
					if (dc==selectedNoteRun) {
						g.setColor(Color.LIGHT_GRAY);
						g.setStroke(new BasicStroke(velo*4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
						g.drawLine((int)0, (int)notey, (int)noteendx, (int)notey);
						g.drawLine((int)notestartx, (int)notey, (int)width, (int)notey);
						g.setColor(noteColor);
					}
					g.setStroke(new BasicStroke(velo, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER));
					g.drawLine((int)0, (int)notey, (int)noteendx, (int)notey);
					g.drawLine((int)notestartx, (int)notey, (int)width, (int)notey);
				}
				hitrects.add(new Line2D.Float(notestartx, notey, noteendx, notey));
				
				if (selectedNoteRun!=null) {
					String notetext = dc.getNoteName();
					FontMetrics fm = g.getFontMetrics();
	                Rectangle2D rect = fm.getStringBounds(notetext, g);
	                int y = (int)(notey);
	                g.setColor(noteColor);
	                g.fillRect((int)notestartx,
	                           y - fm.getAscent(),
	                           (int) rect.getWidth(),
	                           (int) rect.getHeight());
					g.setColor(Color.WHITE);
					g.drawString(notetext, notestartx, y);
				}
				
			}
			listsaresynced = true;
		}
		
	}

	public void updateLoopPosition(int pos) {
		this.pos = pos;
		repaint();
	}

	@Override
	public void loopUpdated(List<NoteRun> list) {
		synchronized(noteList) {
			listsaresynced = false;
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
	
	
	
	class PopUpMenu extends JPopupMenu {
		private static final long serialVersionUID = 9049098677413712819L;

		public PopUpMenu(){
	    	JMenuItem delete = new JMenuItem("Delete");
	        delete.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					LOG.info("Delete Note Called");
					Event.sendClearNote(selectedNote);
					selectedNote = null;
					hitNote = null;
				}
			});
	        JMenu velocity = new JMenu("Velocity");
	        JSlider veloslider = new JSlider();
	        veloslider.setOrientation(SwingConstants.HORIZONTAL);
	        veloslider.setMaximum(127);
	        veloslider.setMinimum(1);
	        veloslider.setValue(selectedNote.getVelocity());
	        veloslider.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					selectedNote.setVelocity(veloslider.getValue());
					Event.sendLoopDisplayRefresh();
				}
			});
	        velocity.add(veloslider);
	        
	        JMenu length = new JMenu("Length");
	        JSlider lengthslider = new JSlider();
	        lengthslider.setOrientation(SwingConstants.HORIZONTAL);
	        lengthslider.setMaximum(MidiHandler.instance().getMaxTicks());
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
					Event.sendLoopDisplayRefresh();
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
