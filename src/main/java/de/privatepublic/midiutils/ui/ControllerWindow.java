package de.privatepublic.midiutils.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import de.privatepublic.midiutils.DiMIDImi;
import de.privatepublic.midiutils.Session;
import de.privatepublic.midiutils.events.SettingsUpdateReceiver;

public class ControllerWindow extends JFrame implements SettingsUpdateReceiver {

	private static final long serialVersionUID = 3196404892575349167L;
	private JPanel contentPane;
	private Map<Integer, PanelComponent> panelComponents = new HashMap<Integer, PanelComponent>();


	public ControllerWindow() {
		setBounds(100, 100, 450, 300);
		setTitle("dimidimi Control");
		
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
//		JPanel panel = new JPanel();
//		contentPane.add(panel);
//		
//		JLabel label = new JLabel("#1");
//		panel.add(label);
//		
//		
//		JToggleButton btnMute = new JToggleButton("Mute");
//		panel.add(btnMute);
//		
//		JToggleButton btnSolo = new JToggleButton("Solo");
//		panel.add(btnSolo);
//		
//		JCheckBox chckbxTriggerOnEnd = new JCheckBox("Trigger on End");
//		panel.add(chckbxTriggerOnEnd);
		
		
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
	
	
	private static class PanelComponent {
		
		private JPanel panel;
		private Session session;
		private JLabel label;
		private JToggleButton btnMute;
		private JToggleButton btnSolo;
		private boolean onNextCycle = false;
		
		public PanelComponent(Session session) {
			this.session = session;
			
			panel = new JPanel();
			panel.setBorder(new MatteBorder(0,0,1,0, Color.gray));
			label = new JLabel();
			label.setPreferredSize(new Dimension(30, 24));
			panel.add(label);
			
			btnMute = new JToggleButton("Mute");
			btnMute.setIcon(IC_EMPTY);
			btnMute.setSelectedIcon(IC_CHECKED);
			panel.add(btnMute);
			btnMute.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ev) {
					if(ev.getStateChange()==ItemEvent.SELECTED){
						toggleState(onNextCycle?Toggle.MUTE_NEXT:Toggle.MUTE, true);
					} else if(ev.getStateChange()==ItemEvent.DESELECTED){
						toggleState(onNextCycle?Toggle.MUTE_NEXT:Toggle.MUTE, false);
					}
				}
			});
			
			btnSolo = new JToggleButton("Solo");
			btnSolo.setIcon(IC_EMPTY);
			btnSolo.setSelectedIcon(IC_CHECKED);
			panel.add(btnSolo);
			btnSolo.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ev) {
					if(ev.getStateChange()==ItemEvent.SELECTED){
						toggleState(onNextCycle?Toggle.SOLO_NEXT:Toggle.SOLO, true);
					} else if(ev.getStateChange()==ItemEvent.DESELECTED){
						toggleState(onNextCycle?Toggle.SOLO_NEXT:Toggle.SOLO, false);
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
		}
		
		
		private void toggleState(Toggle toggle, boolean on) {
			if (on) {
				switch (toggle) {
				case MUTE:
					btnSolo.setSelected(false);
					btnMute.setSelectedIcon(IC_CHECKED);
					break;
				case MUTE_NEXT:
					btnMute.setSelectedIcon(IC_NEXT_CYCLE);
					break;
				case SOLO:
					btnMute.setSelected(false);
					btnSolo.setSelectedIcon(IC_CHECKED);
					break;
				case SOLO_NEXT:
					btnSolo.setSelectedIcon(IC_NEXT_CYCLE);
					break;
				}
			}
			else {
				switch (toggle) {
				case MUTE:
					btnMute.setSelected(false);
					break;
				case MUTE_NEXT:
					btnMute.setSelected(false);
					break;
				case SOLO:
					btnSolo.setSelected(false);
					break;
				case SOLO_NEXT:
					btnMute.setSelected(false);
					break;
				}
			}
		}
		
		
		
		public void updateLabelText() {
			label.setText("#"+(session.getMidiChannelOut()+1));
		}
		
		public JPanel getPanel() {
			return panel;
		}
		
		private static final ImageIcon IC_EMPTY = new ImageIcon(PanelComponent.class.getResource("/ic_empty_circle.png"));
		private static final ImageIcon IC_CHECKED = new ImageIcon(PanelComponent.class.getResource("/ic_check.png"));
		private static final ImageIcon IC_NEXT_CYCLE = new ImageIcon(PanelComponent.class.getResource("/ic_next_cycle.png"));
		private static final ImageIcon IC_OFF_NEXT_CYCLE = new ImageIcon(PanelComponent.class.getResource("/ic_off_next_cycle.png"));
		private static enum Toggle { MUTE, MUTE_NEXT, SOLO, SOLO_NEXT};
	}
	
	
	
}
