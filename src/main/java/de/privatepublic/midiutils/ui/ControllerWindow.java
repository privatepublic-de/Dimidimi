package de.privatepublic.midiutils.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.privatepublic.midiutils.DiMIDImi;
import de.privatepublic.midiutils.Loop;
import de.privatepublic.midiutils.Loop.QueuedState;
import de.privatepublic.midiutils.MidiHandler;
import de.privatepublic.midiutils.Prefs;
import de.privatepublic.midiutils.events.PerformanceReceiver;
import de.privatepublic.midiutils.events.SettingsUpdateReceiver;
import javax.swing.ScrollPaneConstants;

public class ControllerWindow extends JFrame implements SettingsUpdateReceiver, PerformanceReceiver {

	private static final long serialVersionUID = 3196404892575349167L;
	
	private static final Logger LOG = LoggerFactory.getLogger(ControllerWindow.class);
	
	private static final int STANDARD_WIDTH = 250;
	
	private JPanel windowPane;
	private JPanel contentPane;
	private Map<Integer, PanelComponent> panelComponents = new HashMap<Integer, PanelComponent>();
	private int bpm = 100;
	
	private List<JComponent> updateBackgroundComponents = new ArrayList<JComponent>();
	private List<JComponent> updateForegroundComponents = new ArrayList<JComponent>();

	public ControllerWindow() {
		setFocusableWindowState(false);
		setType(Type.UTILITY);
		
		String posprefs = Prefs.get(Prefs.CONTROLLER_POS, null);
		if (posprefs!=null) {
			String[] parts = posprefs.split(",");
			setBounds(Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),STANDARD_WIDTH,Integer.parseInt(parts[3]));
			setAlwaysOnTop("true".equals(parts[4]));
			setVisible("true".equals(parts[5]));
		}
		else {
			setBounds(100, 100, STANDARD_WIDTH, 300);
		}
		setTitle("dimidimi Control");
		
		windowPane = new JPanel();
		windowPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		setContentPane(windowPane);
		windowPane.setLayout(new BorderLayout(0, 0));
		
		panel_1 = new JPanel();
		
		windowPane.add(panel_1, BorderLayout.NORTH);
		
		lblAll = new JLabel("All:");
		panel_1.add(lblAll);
		
		btnAllMuteOff = new JButton("Mute");
		btnAllMuteOff.setIcon(IC_NEXT_CYCLE_OFF);
		panel_1.add(btnAllMuteOff);
		btnAllMuteOff.setToolTipText("All Mute Off");
		
		btnAllSoloOff = new JButton("Solo");
		btnAllSoloOff.setIcon(IC_NEXT_CYCLE_OFF);
		panel_1.add(btnAllSoloOff);
		btnAllSoloOff.setToolTipText("All Solo Off");
		
		btnNext = new JCheckBox("Next cycle");
		btnNext.setOpaque(false);
		// btnNext.setIcon(IC_EMPTY);
		btnNext.setToolTipText("Toggle All Next Cycle");
		panel_1.add(btnNext);
		btnAllSoloOff.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				allOff(Toggle.SOLO);
			}
		});
		
		btnAllMuteOff.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				allOff(Toggle.MUTE);
			}
			
		});
		btnNext.addActionListener(new ActionListener() {
			boolean value = false;
			@Override
			public void actionPerformed(ActionEvent e) {
				value = !value;
				for (PanelComponent comp:panelComponents.values()) {
					comp.onNextCycle = value;
					comp.chckbxTriggerOnEnd.setSelected(value);
				}
			}
		});
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(new LineBorder(Color.DARK_GRAY));
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		contentPane = new JPanel();
		contentPane.setBackground(Theme.APPLY.colorBackground());
		scrollPane.setViewportView(contentPane);
		contentPane.setBorder(new EmptyBorder(5, 0, 5, 0));
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		windowPane.add(scrollPane);
		panel_2 = new JPanel();
		windowPane.add(panel_2, BorderLayout.SOUTH);
		panel_2.setLayout(new BorderLayout(0, 0));
		
		panel_3 = new JPanel();
		panel_2.add(panel_3, BorderLayout.NORTH);
		
		btnStart = new JButton("▶");
		btnStart.setToolTipText("Start");
		
		btnStop = new JButton("◼");
		btnStop.setToolTipText("Stop");
		btnStop.setEnabled(false);
		
		toggleAlwaysOnTop = new JCheckBox("Stay on top");
		toggleAlwaysOnTop.setOpaque(false);
		toggleAlwaysOnTop.setAlignmentX(Component.RIGHT_ALIGNMENT);
		toggleAlwaysOnTop.setMargin(new Insets(1, 24, 0, 1));
		toggleAlwaysOnTop.setToolTipText("Controller window always on top");
		toggleAlwaysOnTop.setHorizontalTextPosition(SwingConstants.LEFT);
		toggleAlwaysOnTop.setSelected(isAlwaysOnTop());
		toggleAlwaysOnTop.setFont(toggleAlwaysOnTop.getFont().deriveFont(toggleAlwaysOnTop.getFont().getSize() - 2f));
		panel_3.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		panel_3.add(btnStart);
		panel_3.add(btnStop);
		panel_3.add(toggleAlwaysOnTop);
		toggleAlwaysOnTop.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				setAlwaysOnTop(toggleAlwaysOnTop.isSelected());
			}
		});
		btnStop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MidiHandler.instance().stopInternalClock();
			}
		});
		
		btnStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MidiHandler.instance().startInternalClock(slider.getValue());
			}
		});
		
		panel_4 = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel_4.getLayout();
		flowLayout.setVgap(0);
		panel_2.add(panel_4, BorderLayout.SOUTH);
		
		slider = new JSlider();
		slider.setOpaque(false);
		slider.setMinorTickSpacing(10);
		slider.setPaintTicks(true);
		panel_4.add(slider);
		
		slider.setValue(100);
		slider.setMinimum(20);
		slider.setMaximum(180);
		slider.setToolTipText("Tempo BPM");
		
		lblBpm = new JLabel("100 BPM");
		panel_4.add(lblBpm);
		
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				bpm = slider.getValue();
				lblBpm.setText((bpm<100?"0":"")+bpm+" BPM");
				MidiHandler.instance().setInteralClockSpeed(slider.getValue());	
			}
		});
		
		updateBackgroundComponents.add(contentPane);
		updateBackgroundComponents.add(panel_1);
		updateBackgroundComponents.add(panel_3);
		updateBackgroundComponents.add(panel_4);
		
		updateForegroundComponents.add(lblAll);
		updateForegroundComponents.add(lblBpm);
		updateForegroundComponents.add(btnNext);
		updateForegroundComponents.add(toggleAlwaysOnTop);
		
		LOG.debug("Created controller window.");
	}
	
	private void allOff(Toggle what) {
		for (PanelComponent comp:panelComponents.values()) {
			boolean isSelected = false;
			switch (what) {
			case SOLO:
				isSelected = comp.btnSolo.isSelected();
				break;
			case MUTE:
				isSelected = comp.btnMute.isSelected();
				break;
			};
			if (isSelected) {
				comp.toggleState(what, false);
			}
		}
	}

	public int getBPM() {
		return bpm;
	}
	
	@Override
	public void onSettingsUpdated() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					for (JComponent cp: updateBackgroundComponents) {
						cp.setBackground(Theme.APPLY.colorBackground());	
					}
					for (JComponent cp: updateForegroundComponents) {
						cp.setForeground(Theme.APPLY.colorForeground());	
					}
					
					if (panelComponents.size()<=DiMIDImi.getLoops().size()) {
						for (Loop loop:DiMIDImi.getLoops()) {
							PanelComponent panel = panelComponents.get(loop.hashCode());
							if (panel==null) {
								panel = new PanelComponent(loop);
								panelComponents.put(loop.hashCode(), panel);
								contentPane.add(panel.getPanel(), gbc);
								panel.getPanel().revalidate();
								int targetWidth = (int)panel.getPanel().getPreferredSize().getWidth()+WIDTH_PADDING;
								targetWidth = Math.max(targetWidth, STANDARD_WIDTH);
								int targetHeight = (int)panel.getPanel().getPreferredSize().getHeight()+HEIGHT_PADDING;
								Dimension currSize = getMaximumSize();
							    setMaximumSize(new Dimension(targetWidth, currSize.height));
							    setMinimumSize(new Dimension(targetWidth, targetHeight));
							    currSize = getSize();
							    setSize(new Dimension(targetWidth, currSize.height));
								loop.registerAsReceiver(ControllerWindow.this);
							}
							panel.updateLabelText();
						}
						refreshView();
					}
					else if (panelComponents.size()>DiMIDImi.getLoops().size()) {
						Integer removeKey = null;
						for (Integer key:panelComponents.keySet()) {
							boolean exists = false;
							for (Loop loop:DiMIDImi.getLoops()) {
								if (loop.hashCode()==key) {
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
					for (PanelComponent pc: panelComponents.values()) {
						pc.updateColors();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
	}

	private void refreshView() {
		revalidate();
//		invalidate();
//		validate();
		repaint();
	}
	
	
	private static class PanelComponent implements PerformanceReceiver {
		
		static final CopyOnWriteArrayList<BlinkToggleButton> BLINKERS = new CopyOnWriteArrayList<BlinkToggleButton>(); 
		static {
			javax.swing.Timer flashTimer = new javax.swing.Timer(150, new ActionListener() {
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
		
		private JPanel panel;
		private Loop loop;
		private JLabel label;
		private BlinkToggleButton btnMute;
		private BlinkToggleButton btnSolo;
		private JCheckBox chckbxTriggerOnEnd;
		private boolean onNextCycle = false;
		
		public PanelComponent(Loop loop) {
			this.loop = loop;
			
			panel = new JPanel();
			panel.setPreferredSize(null);
			panel.setBorder(BorderFactory.createLineBorder(Theme.APPLY.colorBackground(), 2, false));
			label = new JLabel("", SwingConstants.RIGHT);
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
			
			chckbxTriggerOnEnd = new JCheckBox("Next cycle");
			panel.add(chckbxTriggerOnEnd);
			chckbxTriggerOnEnd.setOpaque(false);
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

			loop.registerAsReceiver(this);
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
							btnMute.startBlinking(IC_NEXT_CYCLE_ON, IC_EMPTY, true);
							loop.setQueuedMute(QueuedState.ON);
						}
						else {
							btnMute.startBlinking(IC_NEXT_CYCLE_OFF, IC_EMPTY, false);
							loop.setQueuedMute(QueuedState.OFF);
						}
					}
					else {
						if (on) {
							btnMute.stopBlinking();
							loop.setMuted(true);
						}
						else {
							loop.setMuted(false);
						}
					}
					if (btnMute.isSelected()!=on) {
						btnMute.setSelected(on);
					}
					break;
				case SOLO:
					if (onNextCycle) {
						if (on) {
							btnSolo.startBlinking(IC_NEXT_CYCLE_ON, IC_EMPTY, true);
							loop.setQueuedSolo(QueuedState.ON);
						}
						else {
							btnSolo.startBlinking(IC_NEXT_CYCLE_OFF, IC_EMPTY, false);
							loop.setQueuedSolo(QueuedState.OFF);
						}
					}
					else {
						if (on) {
							btnSolo.stopBlinking();
							loop.setSoloed(true);
						}
						else {
							loop.setSoloed(false);
						}
					}
					if (btnSolo.isSelected()!=on) {
						btnSolo.setSelected(on);
					}
					break;
				}
		}
		
		
		public void updateLabelText() {
			label.setText("#"+(loop.getMidiChannelOut()+1));
		}
		
		public void updateColors() {
			panel.setBackground(Theme.isBright()?loop.getNoteColorPlayed():loop.getNoteColor(false));
			panel.setBorder(BorderFactory.createLineBorder(Theme.APPLY.colorBackground(), 2, false));
		}
		
		public JPanel getPanel() {
			return panel;
		}
		
		
		@Override
		public void onStateChange(boolean mute, boolean solo, QueuedState queuedMute, QueuedState queuedSolo) {
			
			switch (queuedMute) {
			case OFF:
				btnMute.startBlinking(IC_NEXT_CYCLE_OFF, IC_EMPTY, false);
				break;
			case ON:
				btnMute.startBlinking(IC_NEXT_CYCLE_ON, IC_EMPTY, true);
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
				btnSolo.startBlinking(IC_NEXT_CYCLE_OFF, IC_EMPTY, false);
				break;
			case ON:
				btnSolo.startBlinking(IC_NEXT_CYCLE_ON, IC_EMPTY, true);
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
		public void onNoteOn(int noteNumber, int velocity, int pos) {
		}


		@Override
		public void onNoteOff(int notenumber, int pos) {
		}


		@Override
		public void onClock(int pos) {
		}


		@Override
		public void onActivityChange(boolean active, int pos) {
		}


		@Override
		public void onReceiveCC(int cc, int val, int pos) {
		}


		@Override
		public void onReceivePitchBend(int val, int pos) {
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
	private static final ImageIcon IC_NEXT_CYCLE_ON = new ImageIcon(PanelComponent.class.getResource("/ic_next_cycle.png"));
	private static final ImageIcon IC_NEXT_CYCLE_OFF = new ImageIcon(PanelComponent.class.getResource("/ic_off_next_cycle.png"));

	private static enum Toggle { MUTE, SOLO };
	
	private static GridBagConstraints gbc = new GridBagConstraints();
	static {
	    gbc.weightx = 1;
	    gbc.fill = GridBagConstraints.HORIZONTAL;
	    gbc.gridwidth = GridBagConstraints.REMAINDER;
	    gbc.insets = new Insets(1, 0, 1, 0);
	}
	
	private static final int WIDTH_PADDING = 34;
	private static final int HEIGHT_PADDING = 48;
	private JButton btnAllSoloOff;
	private JButton btnAllMuteOff;
	private JCheckBox toggleAlwaysOnTop;
	private JPanel panel_1;
	private JCheckBox btnNext;
	private JPanel panel_2;
	private JLabel lblAll;
	private JButton btnStart;
	private JButton btnStop;
	private JSlider slider;
	private JLabel lblBpm;
	private JPanel panel_3;
	private JPanel panel_4;


	@Override
	public void onNoteOn(int noteNumber, int velocity, int pos) {
	}

	@Override
	public void onNoteOff(int notenumber, int pos) {
	}

	@Override
	public void onClock(int pos) {
	}

	@Override
	public void onActivityChange(boolean active, int pos) {
		btnStart.setEnabled(!active);
		btnStop.setEnabled(active);
	}

	@Override
	public void onReceiveCC(int cc, int val, int pos) {
	}

	@Override
	public void onReceivePitchBend(int val, int pos) {
	}

	@Override
	public void onStateChange(boolean mute, boolean solo, QueuedState queuedMute,
			QueuedState queuedSolo) {
	}
	
}
