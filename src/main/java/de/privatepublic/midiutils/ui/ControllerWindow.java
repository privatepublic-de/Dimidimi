package de.privatepublic.midiutils.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import de.privatepublic.midiutils.DiMIDImi;
import de.privatepublic.midiutils.Session;
import de.privatepublic.midiutils.Session.QueuedState;
import de.privatepublic.midiutils.events.PerformanceReceiver;
import de.privatepublic.midiutils.events.SettingsUpdateReceiver;

public class ControllerWindow extends JFrame implements SettingsUpdateReceiver {

	private static final long serialVersionUID = 3196404892575349167L;
	private JPanel windowPane;
	private JPanel contentPane;
	private Map<Integer, PanelComponent> panelComponents = new HashMap<Integer, PanelComponent>();


	public ControllerWindow() {
		setBounds(100, 100, 450, 300);
		setTitle("dimidimi Control");
		
		
		windowPane = new JPanel();
		windowPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		setContentPane(windowPane);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBorder(null);
		GroupLayout gl_windowPane = new GroupLayout(windowPane);
		gl_windowPane.setHorizontalGroup(
			gl_windowPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_windowPane.createSequentialGroup()
					.addComponent(scrollPane)
					)
		);
		gl_windowPane.setVerticalGroup(
			gl_windowPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_windowPane.createSequentialGroup()
					.addComponent(scrollPane)
					)
		);
		
		contentPane = new JPanel();
		scrollPane.setViewportView(contentPane);
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		windowPane.setLayout(gl_windowPane);
		
		for (Session session:DiMIDImi.getSessions()) {
			session.registerAsReceiver(this);
		}
	}

	@Override
	public void settingsUpdated() {
		if (panelComponents.size()<=DiMIDImi.getSessions().size()) {
			for (Session session:DiMIDImi.getSessions()) {
				PanelComponent panel = panelComponents.get(session.hashCode());
				if (panel==null) {
					panel = new PanelComponent(session);
					contentPane.add(panel.getPanel());
					panelComponents.put(session.hashCode(), panel);
					int targetWidth = (int)panel.getPanel().getPreferredSize().getWidth()+WIDTH_PADDING;
					int targetHeight = (int)panel.getPanel().getPreferredSize().getHeight()+HEIGHT_PADDING;
					Dimension currSize = getMaximumSize();
				    setMaximumSize(new Dimension(targetWidth, currSize.height));
				    setMinimumSize(new Dimension(targetWidth, targetHeight));
				    currSize = getSize();
				    setSize(new Dimension(targetWidth, currSize.height));
					session.registerAsReceiver(this);
				}
				panel.updateLabelText();
			}
			refreshView();
		}
		else if (panelComponents.size()>DiMIDImi.getSessions().size()) {
			Integer removeKey = null;
			for (Integer key:panelComponents.keySet()) {
				boolean exists = false;
				for (Session session:DiMIDImi.getSessions()) {
					if (session.hashCode()==key) {
						exists = true;
						break;
					}
				}
				if (!exists) {
					removeKey = key;
					break;
				}
			}
			if (removeKey!=null) {
				contentPane.remove(panelComponents.get(removeKey).getPanel());
				panelComponents.get(removeKey).destroy();
				panelComponents.remove(removeKey);
				refreshView();
			}
		}
	}

	private void refreshView() {
		invalidate();
		validate();
		repaint();
	}
	
	
	private static class PanelComponent implements PerformanceReceiver {
		
		static final CopyOnWriteArrayList<BlinkToggleButton> BLINKERS = new CopyOnWriteArrayList<BlinkToggleButton>(); 
		static {
			javax.swing.Timer flashTimer = new javax.swing.Timer(334, new ActionListener() {
				private boolean blinkState;
				@Override
				public void actionPerformed(ActionEvent e) {
					blinkState=!blinkState;
					for (BlinkToggleButton btn:BLINKERS) {
						btn.blink(blinkState);
					}
				}
			});
		    flashTimer.setCoalesce(true);
		    flashTimer.setRepeats(true);
		    flashTimer.setInitialDelay(0);
		    flashTimer.start();
		}
		
		private static enum Toggle { MUTE, SOLO }
		
		private JPanel panel;
		private Session session;
		private JLabel label;
		private BlinkToggleButton btnMute;
		private BlinkToggleButton btnSolo;
		private boolean onNextCycle = false;
		
		public PanelComponent(Session session) {
			this.session = session;
			
			panel = new JPanel();
			panel.setBorder(new MatteBorder(0,0,1,0, Color.gray));
			label = new JLabel();
			label.setPreferredSize(new Dimension(30, 24));
			panel.add(label);
			
			btnMute = new BlinkToggleButton("Mute");
			btnMute.setIcon(IC_EMPTY);
			btnMute.setSelectedIcon(IC_CHECKED);
			panel.add(btnMute);
			btnMute.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ev) {
					if (ev.getStateChange()==ItemEvent.SELECTED || ev.getStateChange()==ItemEvent.DESELECTED){
						toggleState(Toggle.MUTE, ev.getStateChange()==ItemEvent.SELECTED);
					}
				}
			});
			
			btnSolo = new BlinkToggleButton("Solo");
			btnSolo.setIcon(IC_EMPTY);
			btnSolo.setSelectedIcon(IC_CHECKED);
			panel.add(btnSolo);
			btnSolo.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ev) {
					if (ev.getStateChange()==ItemEvent.SELECTED || ev.getStateChange()==ItemEvent.DESELECTED){
						toggleState(Toggle.SOLO, ev.getStateChange()==ItemEvent.SELECTED);
					}
				}
			});
			
			JCheckBox chckbxTriggerOnEnd = new JCheckBox("Next cycle");
			panel.add(chckbxTriggerOnEnd);
			chckbxTriggerOnEnd.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ev) {
					if(ev.getStateChange()==ItemEvent.SELECTED){
						onNextCycle = true;
					} else if(ev.getStateChange()==ItemEvent.DESELECTED){
						onNextCycle = false;
					}
				}
			});
			panel.setMaximumSize(panel.getPreferredSize());
			BLINKERS.add(btnMute);
			BLINKERS.add(btnSolo);

			session.registerAsReceiver(this);
		}
		
		
		public void destroy() {
			BLINKERS.remove(btnMute);
			BLINKERS.remove(btnSolo);
		}


		private void toggleState(Toggle toggle, boolean on) {		
				switch (toggle) {
				case MUTE:
					if (onNextCycle) {
						if (on) {
							btnMute.startBlinking(IC_NEXT_CYCLE, IC_EMPTY, true);
							session.setQueuedMute(QueuedState.ON);
						}
						else {
							btnMute.startBlinking(IC_OFF_NEXT_CYCLE, IC_EMPTY, false);
							session.setQueuedMute(QueuedState.OFF);
						}
					}
					else {
						if (on) {
							btnMute.stopBlinking();
							session.setMuted(true);
						}
						else {
							session.setMuted(false);
						}
					}
					break;
				case SOLO:
					if (onNextCycle) {
						if (on) {
							btnSolo.startBlinking(IC_NEXT_CYCLE, IC_EMPTY, true);
							session.setQueuedSolo(QueuedState.ON);
						}
						else {
							btnSolo.startBlinking(IC_OFF_NEXT_CYCLE, IC_EMPTY, false);
							session.setQueuedSolo(QueuedState.OFF);
						}
					}
					else {
						if (on) {
							btnSolo.stopBlinking();
							session.setSoloed(true);
						}
						else {
							session.setSoloed(false);
						}
					}
					break;
				}
		}
		
		
		public void updateLabelText() {
			label.setText("#"+(session.getMidiChannelOut()+1));
		}
		
		public JPanel getPanel() {
			return panel;
		}
		
		
		@Override
		public void stateChange(boolean mute, boolean solo, QueuedState queuedMute, QueuedState queuedSolo) {
			
			switch (queuedMute) {
			case OFF:
				btnMute.startBlinking(IC_OFF_NEXT_CYCLE, IC_EMPTY, false);
				break;
			case ON:
				btnMute.startBlinking(IC_NEXT_CYCLE, IC_EMPTY, true);
				break;
			default:
				btnMute.stopBlinking();
				if (mute) {
					btnMute.setSelected(true);
				}
				else {
					btnMute.setSelected(false);
				}
				break;
			}
			switch (queuedSolo) {
			case OFF:
				btnSolo.startBlinking(IC_OFF_NEXT_CYCLE, IC_EMPTY, false);
				break;
			case ON:
				btnSolo.startBlinking(IC_NEXT_CYCLE, IC_EMPTY, true);
				break;
			default:
				btnSolo.stopBlinking();
				if (solo) {
					btnSolo.setSelected(true);
				}
				else {
					btnSolo.setSelected(false);
				}
				break;
			}

		};
		

		@Override
		public void noteOn(int noteNumber, int velocity, int pos) {
		}


		@Override
		public void noteOff(int notenumber, int pos) {
		}


		@Override
		public void receiveClock(int pos) {
		}


		@Override
		public void receiveActive(boolean active, int pos) {
		}


		@Override
		public void receiveCC(int cc, int val, int pos) {
		}


		@Override
		public void receivePitchBend(int val, int pos) {
		}
		
	}
	
	private static class BlinkToggleButton extends JToggleButton {
		
		private static final long serialVersionUID = 7707283219651661189L;
		
		ImageIcon blinkIcon1;
		ImageIcon blinkIcon2;
		boolean blinkUseSelectedIcon;
		boolean blinkOn;
		
		public BlinkToggleButton(String string) {
			super(string);
		}

		public void blink(boolean blinkState) {
			if (blinkOn) {
				ImageIcon useIcon = blinkState?blinkIcon2:blinkIcon1;
				if (blinkUseSelectedIcon) {
					setSelectedIcon(useIcon);
				}
				else {
					setIcon(useIcon);
				}
			}
		}
		
		public void startBlinking(ImageIcon icon1, ImageIcon icon2, boolean useSelectedIcon) {
			blinkIcon1 = icon1;
			blinkIcon2 = icon2;
			blinkUseSelectedIcon = useSelectedIcon;
			if (useSelectedIcon) {
				setSelectedIcon(icon1);
			}
			blinkOn = true;
		}
		
		public void stopBlinking() {
			blinkOn = false;
			setIcon(IC_EMPTY);
			setSelectedIcon(IC_CHECKED);
		}
		
	}
	
	private static final ImageIcon IC_EMPTY = new ImageIcon(PanelComponent.class.getResource("/ic_empty_circle.png"));
	private static final ImageIcon IC_CHECKED = new ImageIcon(PanelComponent.class.getResource("/ic_check.png"));
	private static final ImageIcon IC_NEXT_CYCLE = new ImageIcon(PanelComponent.class.getResource("/ic_next_cycle.png"));
	private static final ImageIcon IC_OFF_NEXT_CYCLE = new ImageIcon(PanelComponent.class.getResource("/ic_off_next_cycle.png"));
	
	private static final int WIDTH_PADDING = 48;
	private static final int HEIGHT_PADDING = 48;
	
}
