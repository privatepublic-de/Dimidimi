package de.privatepublic.midiutils.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.privatepublic.midiutils.DiMIDImi;
import de.privatepublic.midiutils.MidiDeviceWrapper;
import de.privatepublic.midiutils.Prefs;
import de.privatepublic.midiutils.Session;
import de.privatepublic.midiutils.events.PerformanceReceiver;
import de.privatepublic.midiutils.events.SettingsUpdateReceiver;

public class UIWindow implements PerformanceReceiver, SettingsUpdateReceiver {

	private static final Logger LOG = LoggerFactory.getLogger(UIWindow.class);
	
	public static int WINDOW_MAX_WIDTH = 680;
	public static int WINDOW_MAX_HEIGHT = 300;
	
	private static final String APP_TITLE = "dimidimi Looper";
	
	private static final String[] MIDI_CHANNELS = new String[]{"1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16"};
	private static final String[] QUANTIZE = new String[]{"none","1/2","1/4","1/8","1/16","1/32","1/4t", "1/8t", "1/16t"};
	private static final String[] TRANSPOSE = new String[]{"+24", "+12","+11","+10","+9","+8","+7","+6","+5","+4","+3","+2","+1","0","-1","-2","-3","-4","-5","-6","-7","-8","-9","-10","-11","-12","-24"};
	
	private JFrame frmDimidimi;
	private JTextField textFieldLength;
	private LoopDisplayPanel loopDisplayPanel;
	private JComboBox<String> comboQuantize;
	private JComboBox<String> comboBoxTranspose;
	private JComboBox<String> comboMidiIn;
	private JComboBox<String> comboMidiOut;
	private JCheckBox checkBoxMidiOut;
	private JCheckBox checkBoxMidiIn;
	private JLabel lblDimidimiLooper;
	private JCheckBoxMenuItem menuItemTheme;
	private Session session;
	private String titleExtension = "";

	public UIWindow(Session session) {
		this.session = session;
		try {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Test");
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
		} 
		catch (Exception e) {
			LOG.warn("Could not set look and feel", e);
		}
		initialize();
		frmDimidimi.setVisible(true);
		
		session.registerAsReceiver(this);
		session.emitSettingsUpdated();
		session.emitLoopUpdated();
		LOG.info("User interface built.");
	}
	
	public void closeWindow() {
		frmDimidimi.dispatchEvent(new WindowEvent(frmDimidimi, WindowEvent.WINDOW_CLOSING));
	}
	
	public Rectangle getScreenPosition() {
		return frmDimidimi.getBounds();
	}
	
	public void setScreenPosition(Rectangle r) {
		frmDimidimi.setBounds(r);
	}
	
	private String getWindowTitle() {
		return APP_TITLE+" - "+titleExtension+" (MIDI out #"+(session.getMidiChannelOut()+1)+")"; 
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initialize() {
		frmDimidimi = new JFrame();
		frmDimidimi.setTitle(getWindowTitle());
		frmDimidimi.setBounds(100, 100, 1028, 524);
		frmDimidimi.setMinimumSize(new Dimension(WINDOW_MAX_WIDTH, WINDOW_MAX_HEIGHT));
		frmDimidimi.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frmDimidimi.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				DiMIDImi.removeSession(session);
			}
		});
		setIcon(frmDimidimi);
		
		frmDimidimi.setJMenuBar(buildMenu());
		
		JPanel panelLoop = new JPanel();
		panelLoop.setBackground(Color.WHITE);
		panelLoop.setBorder(new LineBorder(Color.GRAY));
		
		JPanel panelMidi = new JPanel();
		panelMidi.setBorder(null);
		
		JPanel panelTitle = new JPanel();
		
		JPanel panel = new JPanel();
		GroupLayout groupLayout = new GroupLayout(frmDimidimi.getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING)
						.addComponent(panelLoop, GroupLayout.DEFAULT_SIZE, 992, Short.MAX_VALUE)
						.addComponent(panel, GroupLayout.DEFAULT_SIZE, 992, Short.MAX_VALUE)
						.addGroup(Alignment.LEADING, groupLayout.createSequentialGroup()
							.addComponent(panelTitle, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(panelMidi, GroupLayout.DEFAULT_SIZE, 777, Short.MAX_VALUE)))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
						.addComponent(panelMidi, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(panelTitle, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(panelLoop, GroupLayout.DEFAULT_SIZE, 353, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(panel, GroupLayout.PREFERRED_SIZE, 37, GroupLayout.PREFERRED_SIZE)
					.addGap(6))
		);
		SpringLayout sl_panel = new SpringLayout();
		panel.setLayout(sl_panel);
		
		JLabel lblNumberOfQuarters = new JLabel("Length");
		sl_panel.putConstraint(SpringLayout.WEST, lblNumberOfQuarters, 0, SpringLayout.WEST, panel);
		sl_panel.putConstraint(SpringLayout.VERTICAL_CENTER, lblNumberOfQuarters, 0, SpringLayout.VERTICAL_CENTER, panel);
		panel.add(lblNumberOfQuarters);
		
		textFieldLength = new JTextField();
		sl_panel.putConstraint(SpringLayout.WEST, textFieldLength, 6, SpringLayout.EAST, lblNumberOfQuarters);
		textFieldLength.setToolTipText("Loop length in quarter notes");
		sl_panel.putConstraint(SpringLayout.VERTICAL_CENTER, textFieldLength, 0, SpringLayout.VERTICAL_CENTER, panel);
		textFieldLength.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(textFieldLength);
		textFieldLength.setHorizontalAlignment(SwingConstants.CENTER);
		
		textFieldLength.setText("4");
		textFieldLength.setColumns(3);
		textFieldLength.setText(String.valueOf(session.getLengthQuarters()));
		
		JButton btnApply = new JButton("Apply");
		sl_panel.putConstraint(SpringLayout.WEST, btnApply, 6, SpringLayout.EAST, textFieldLength);
		sl_panel.putConstraint(SpringLayout.VERTICAL_CENTER, btnApply, 0, SpringLayout.VERTICAL_CENTER, panel);
		panel.add(btnApply);
		btnApply.setEnabled(false);
		
		JLabel lblQuantizeTo = new JLabel("Quantize");
		sl_panel.putConstraint(SpringLayout.WEST, lblQuantizeTo, 12, SpringLayout.EAST, btnApply);
		sl_panel.putConstraint(SpringLayout.VERTICAL_CENTER, lblQuantizeTo, 0, SpringLayout.VERTICAL_CENTER, panel);
		panel.add(lblQuantizeTo);
		
		comboQuantize = new JComboBox(QUANTIZE);
		sl_panel.putConstraint(SpringLayout.WEST, comboQuantize, 6, SpringLayout.EAST, lblQuantizeTo);
		sl_panel.putConstraint(SpringLayout.VERTICAL_CENTER, comboQuantize, 0, SpringLayout.VERTICAL_CENTER, panel);
		comboQuantize.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(comboQuantize);
		comboQuantize.setMaximumRowCount(12);
		
		JLabel lblTranspose = new JLabel("Transpose");
		sl_panel.putConstraint(SpringLayout.WEST, lblTranspose, 12, SpringLayout.EAST, comboQuantize);
		sl_panel.putConstraint(SpringLayout.VERTICAL_CENTER, lblTranspose, 0, SpringLayout.VERTICAL_CENTER, panel);
		panel.add(lblTranspose);
		
		comboBoxTranspose = new JComboBox(TRANSPOSE);
		sl_panel.putConstraint(SpringLayout.WEST, comboBoxTranspose, 6, SpringLayout.EAST, lblTranspose);
		sl_panel.putConstraint(SpringLayout.VERTICAL_CENTER, comboBoxTranspose, 0, SpringLayout.VERTICAL_CENTER, panel);
		comboBoxTranspose.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(comboBoxTranspose);
		comboBoxTranspose.setMaximumRowCount(27);
		comboBoxTranspose.setSelectedIndex(13);
		
		JButton btnClear = new JButton("Clear");
		sl_panel.putConstraint(SpringLayout.VERTICAL_CENTER, btnClear, 0, SpringLayout.VERTICAL_CENTER, panel);
		panel.add(btnClear);
		
		JButton buttonNewSession = new JButton("+");
		sl_panel.putConstraint(SpringLayout.EAST, btnClear, -6, SpringLayout.WEST, buttonNewSession);
		sl_panel.putConstraint(SpringLayout.VERTICAL_CENTER, buttonNewSession, 0, SpringLayout.VERTICAL_CENTER, panel);
		sl_panel.putConstraint(SpringLayout.EAST, buttonNewSession, -8, SpringLayout.EAST, panel);
		buttonNewSession.setAlignmentX(Component.RIGHT_ALIGNMENT);
		panel.add(buttonNewSession);
		buttonNewSession.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				session.setMidiInputOn(false);
				settingsUpdated();
				DiMIDImi.createSession();
			}
		});
		
		btnClear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				session.clearPattern();
			}
		});
		((JLabel)comboBoxTranspose.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		
		comboBoxTranspose.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				session.setTransposeIndex(comboBoxTranspose.getSelectedIndex());
				session.emitRefreshLoopDisplay();
			}});
		
		comboQuantize.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				session.setQuantizationIndex(comboQuantize.getSelectedIndex());
				session.emitRefreshLoopDisplay();
			}});
		
		
		
		btnApply.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				btnApply.setEnabled(false);
				int numberQuarters = Integer.parseInt(textFieldLength.getText());
				session.setLengthQuarters(numberQuarters);
				session.emitRefreshLoopDisplay();
			}
		});
		textFieldLength.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				checkInput();
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				checkInput();
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				checkInput();
			}
			
			private void checkInput() {
				try {
					int value = Integer.parseInt(textFieldLength.getText());
					if (value>0 && value!=session.getLengthQuarters() && value<=Session.MAX_NUMBER_OF_QUARTERS){
						btnApply.setEnabled(true);	 
					}
					else {
						btnApply.setEnabled(false);	
					}
				}
				catch(NumberFormatException e) {
					btnApply.setEnabled(false);
				}
			}
		});
		
		lblDimidimiLooper = new JLabel(APP_TITLE);
		panelTitle.add(lblDimidimiLooper);
		lblDimidimiLooper.setFont(lblDimidimiLooper.getFont().deriveFont(lblDimidimiLooper.getFont().getStyle() | Font.BOLD, lblDimidimiLooper.getFont().getSize() + 9f));
		lblDimidimiLooper.setIcon(new ImageIcon(UIWindow.class.getResource("/icon-32.png")));
		
		JLabel lblIn = new JLabel("Channel:  In");
		
		comboMidiIn = new JComboBox(MIDI_CHANNELS);
		comboMidiIn.setMaximumRowCount(16);
		
		comboMidiIn.setSelectedIndex(session.getMidiChannelIn());
		
		JLabel lblOut = new JLabel("Out");
		
		comboMidiOut = new JComboBox(MIDI_CHANNELS);
		comboMidiOut.setMaximumRowCount(16);
		comboMidiOut.setSelectedIndex(session.getMidiChannelOut());
		SpringLayout sl_panelMidi = new SpringLayout();
		sl_panelMidi.putConstraint(SpringLayout.EAST, comboMidiIn, -12, SpringLayout.WEST, lblOut);
		sl_panelMidi.putConstraint(SpringLayout.VERTICAL_CENTER, comboMidiOut, 0, SpringLayout.VERTICAL_CENTER, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.VERTICAL_CENTER, lblOut, 0, SpringLayout.VERTICAL_CENTER, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.VERTICAL_CENTER, comboMidiIn, 0, SpringLayout.VERTICAL_CENTER, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.VERTICAL_CENTER, lblIn, 0, SpringLayout.VERTICAL_CENTER, panelMidi);
		panelMidi.setLayout(sl_panelMidi);
		
		panelMidi.add(lblIn);
		
		checkBoxMidiIn = new JCheckBox("");
		sl_panelMidi.putConstraint(SpringLayout.EAST, lblIn, -28, SpringLayout.EAST, checkBoxMidiIn);
		sl_panelMidi.putConstraint(SpringLayout.VERTICAL_CENTER, checkBoxMidiIn, 0, SpringLayout.VERTICAL_CENTER, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.EAST, checkBoxMidiIn, 3, SpringLayout.WEST, comboMidiIn);
		checkBoxMidiIn.setSelected(true);
		checkBoxMidiIn.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				session.setMidiInputOn(checkBoxMidiIn.isSelected());
			}
		});
		checkBoxMidiIn.setToolTipText("Receive Notes from selected Channel");
		panelMidi.add(checkBoxMidiIn);
		panelMidi.add(comboMidiIn);
		panelMidi.add(lblOut);
		
		checkBoxMidiOut = new JCheckBox("");
		sl_panelMidi.putConstraint(SpringLayout.EAST, checkBoxMidiOut, 3, SpringLayout.WEST, comboMidiOut);
		sl_panelMidi.putConstraint(SpringLayout.EAST, lblOut, 0, SpringLayout.WEST, checkBoxMidiOut);
		sl_panelMidi.putConstraint(SpringLayout.VERTICAL_CENTER, checkBoxMidiOut, 0, SpringLayout.VERTICAL_CENTER, panelMidi);
		checkBoxMidiOut.setSelected(true);
		checkBoxMidiOut.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				session.setMidiOutputOn(checkBoxMidiOut.isSelected());
			}
		});
		checkBoxMidiOut.setToolTipText("Output Notes on selcted Channel");
		panelMidi.add(checkBoxMidiOut);
		panelMidi.add(comboMidiOut);
		
		JButton btnNotesOff = new JButton("Panic");
		sl_panelMidi.putConstraint(SpringLayout.EAST, comboMidiOut, -8, SpringLayout.WEST, btnNotesOff);
		sl_panelMidi.putConstraint(SpringLayout.VERTICAL_CENTER, btnNotesOff, 0, SpringLayout.VERTICAL_CENTER, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.EAST, btnNotesOff, -10, SpringLayout.EAST, panelMidi);
		btnNotesOff.setToolTipText("Turns off all playing or stuck MIDI notes.");
		panelMidi.add(btnNotesOff);
		
		btnNotesOff.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				session.getMidiHandler().sendAllNotesOffMidi();
			}});
		panelLoop.setLayout(new BorderLayout(0, 0));
		loopDisplayPanel = new LoopDisplayPanel(session);
		loopDisplayPanel.setBackground(Color.WHITE);
		panelLoop.add(loopDisplayPanel);
		loopDisplayPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		frmDimidimi.getContentPane().setLayout(groupLayout);
		
		
		comboMidiIn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int midiIn = comboMidiIn.getSelectedIndex();
				session.setMidiChannelIn(midiIn);
			}});
		comboMidiOut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int midiOut = comboMidiOut.getSelectedIndex();
				session.setMidiChannelOut(midiOut);
			}});
		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void setIcon(JFrame frame) {
		frame.setIconImage(Toolkit.getDefaultToolkit().getImage(UIWindow.class.getResource("/icon.png")));
		try {
			// set icon for mac os if possible
			Class c = Class.forName("com.apple.eawt.Application");
			Object app = c.getDeclaredMethod ("getApplication", (Class[])null).invoke(null, (Object[])null);
			Method setDockIconImage = c.getDeclaredMethod("setDockIconImage", Image.class);
			setDockIconImage.invoke(app, new ImageIcon(UIWindow.class.getResource("/icon.png")).getImage());
		} catch (Exception e) {
			// fail silently
		}
	}
	
	private JMenuBar buildMenu() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("Session");
		JMenuItem menuItem = new JMenuItem("Load...");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File selectedFile = GUIUtils.loadDialog("Load Session", GUIUtils.FILE_FILTER_SESSION, Prefs.FILE_SESSION_LAST_USED_NAME);
				if (selectedFile!=null) {
					GUIUtils.loadSession(selectedFile, frmDimidimi);
				}
			}
		});
		menu.add(menuItem);
		JMenu recentSub = new JMenu("Recent Sessions");
		recentSub.addMenuListener(new RecentMenuListener(Prefs.RECENT_SESSION_LIST, new RecentMenuActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				super.actionPerformed(e);
				String filename = Prefs.getList(Prefs.RECENT_SESSION_LIST).get(selectedIndex);
				if (!Prefs.LIST_ENTRY_EMPTY_MARKER.equals(filename)) {
					GUIUtils.loadSession(new File(filename), frmDimidimi);
				}
			}
		}));
		menu.add(recentSub);
		
		menu.addSeparator();
		menuItem = new JMenuItem("Save as...");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File selectedFile = GUIUtils.saveDialog("Save Session", GUIUtils.FILE_FILTER_SESSION, Prefs.FILE_SESSION_LAST_USED_NAME);
				if (selectedFile!=null) {
					try {
						DiMIDImi.saveSession(selectedFile);
						Prefs.pushToList(Prefs.RECENT_SESSION_LIST, selectedFile.getPath());
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(frmDimidimi, "Could not write file\n"+e1.getMessage());
		        		LOG.error("Could not write file", e1);
					}
					Prefs.put(Prefs.FILE_SESSION_LAST_USED_NAME, selectedFile.getPath());
				}
			}
		});
		menu.add(menuItem);
		menuBar.add(menu);
		
		menu = new JMenu("Loop");
		menuItem = new JMenuItem("New");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				session.setMidiInputOn(false);
				settingsUpdated();
				DiMIDImi.createSession();
			}
		});
		menu.add(menuItem);
		
		menu.addSeparator();
		menuItem = new JMenuItem("Load...");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File selectedFile = GUIUtils.loadDialog("Load Loop", GUIUtils.FILE_FILTER_LOOP, Prefs.FILE_LOOP_LAST_USED_NAME);
		        if (selectedFile!=null) {
		        	String s = GUIUtils.loadLoop(selectedFile, session, frmDimidimi);
		        	if (s!=null) {
		        		titleExtension = s;
		        		frmDimidimi.setTitle(getWindowTitle());
		        	}
		        }
			}
		});
		menu.add(menuItem);
		recentSub = new JMenu("Recent Loops");
		recentSub.addMenuListener(new RecentMenuListener(Prefs.RECENT_LOOP_LIST, new RecentMenuActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				super.actionPerformed(e);
				String filename = Prefs.getList(Prefs.RECENT_LOOP_LIST).get(selectedIndex);
				if (!Prefs.LIST_ENTRY_EMPTY_MARKER.equals(filename)) {
					String s = GUIUtils.loadLoop(new File(filename), session, frmDimidimi);
		        	if (s!=null) {
		        		titleExtension = s;
		        		frmDimidimi.setTitle(getWindowTitle());
		        	}
				}
			}
		}));
		menu.add(recentSub);
		menu.addSeparator();
		menuItem = new JMenuItem("Save...");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File selectedFile = GUIUtils.saveDialog("Save Loop", GUIUtils.FILE_FILTER_LOOP, Prefs.FILE_LOOP_LAST_USED_NAME);
		        if (selectedFile!=null) {
		        	try {
		        		session.saveLoop(selectedFile);
		        		Prefs.pushToList(Prefs.RECENT_LOOP_LIST, selectedFile.getPath());
		        		titleExtension = FilenameUtils.getBaseName(selectedFile.getName());
		        		frmDimidimi.setTitle(getWindowTitle());
		        	}
		        	catch(Exception e1) {
		        		JOptionPane.showMessageDialog(frmDimidimi, "Could not write file\n"+e1.getMessage());
		        		LOG.error("Could not write file", e1);
		        	}
		        	Prefs.put(Prefs.FILE_LOOP_LAST_USED_NAME, selectedFile.getPath());
		        }
			}
		});
		menu.add(menuItem);
		menu.addSeparator();
		menuItem = new JMenuItem("Clear");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				session.clearPattern();
			}
		});
		menu.add(menuItem);
		
		menuItem = new JMenuItem("Clear Mod Wheel");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				session.clearModWheel();
			}
		});
		menu.add(menuItem);
		
		menuItem = new JMenuItem("Clear Pitch Bend");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				session.clearPitchBend();
			}
		});
		menu.add(menuItem);
		menu.addSeparator();
		
		menuItem = new JMenuItem("Double Loop");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				session.doublePattern();
			}
		});
		menu.add(menuItem);
		
		menuItem = new JMenuItem("Half Speed");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				session.halfSpeed();
			}
		});
		menu.add(menuItem);
		
		menuItem = new JMenuItem("Double Speed");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				session.doubleSpeed();
			}
		});
		menu.add(menuItem);
		
		menuBar.add(menu);
		
		menu = new JMenu("MIDI");
		menuItem = new JMenuItem("Select Devices...");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				CheckboxList listinput = new CheckboxList("Activate Input Devices");
				JCheckBox[] inboxes = new JCheckBox[session.getMidiHandler().getInputDevices().size()];
				for (int i = 0; i < inboxes.length; i++) {
					MidiDeviceWrapper dev = session.getMidiHandler().getInputDevices().get(i);
					JCheckBox cbox = new JCheckBox(dev.toString());
					cbox.setSelected(dev.isActiveForInput());
					inboxes[i] = cbox;
				}
				listinput.setListData(inboxes);
				
				CheckboxList listoutput = new CheckboxList("Activate Output Devices");
				JCheckBox[] outboxes = new JCheckBox[session.getMidiHandler().getOutputDevices().size()];
				for (int i = 0; i < outboxes.length; i++) {
					MidiDeviceWrapper dev = session.getMidiHandler().getOutputDevices().get(i);
					JCheckBox cbox = new JCheckBox(dev.toString());
					cbox.setSelected(dev.isActiveForOutput());
					outboxes[i] = cbox;
				}
				listoutput.setListData(outboxes);
				
				JPanel p = new JPanel(new BorderLayout());

				p.add(listinput,BorderLayout.WEST);
				p.add(listoutput,BorderLayout.EAST);
				
				int result = JOptionPane.showOptionDialog(frmDimidimi, p, "Select MIDI Devices", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
				if (result==JOptionPane.OK_OPTION) {
					for (int i = 0; i < inboxes.length; i++) {
						MidiDeviceWrapper dev = session.getMidiHandler().getInputDevices().get(i);
						dev.setActiveForInput(inboxes[i].isSelected());
					}
					session.getMidiHandler().storeSelectedInDevices();
					
					for (int i = 0; i < outboxes.length; i++) {
						MidiDeviceWrapper dev = session.getMidiHandler().getOutputDevices().get(i);
						dev.setActiveForOutput(outboxes[i].isSelected());
					}
					session.getMidiHandler().storeSelectedOutDevices();
				}
				session.emitRefreshLoopDisplay();
			}
		});
		menu.add(menuItem);
		menuBar.add(menu);
		
		menu = new JMenu("Window");
		menuItemTheme = new JCheckBoxMenuItem("Dark Theme");
		menuItemTheme.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				Prefs.put(Prefs.THEME, menuItemTheme.isSelected()?1:0);
				DiMIDImi.updateSettingsOnAllSessions();
			}
		});
		menu.add(menuItemTheme);
		menu.addSeparator();		
		
		menuItem = new JMenuItem("Arrange Windows");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.SHIFT_DOWN_MASK+Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DiMIDImi.arrangeSessionWindows();
			}
		});
		menu.add(menuItem);
		menu.addSeparator();
		
		menuItem = new JMenuItem("Close");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				closeWindow();
			}
		});
		menu.add(menuItem);
		
		menu.addSeparator();
		menuItem = new JMenuItem("Close All (Exit)");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DiMIDImi.removeAllSessions();
			}
		});
		menu.add(menuItem);
		
		menuBar.add(menu);
		return menuBar;
	}
	
	private class RecentMenuListener implements MenuListener {

		private String listKey;
		private ActionListener listener;
		
		public RecentMenuListener(String listKey, ActionListener listener) {
			this.listKey = listKey;
			this.listener = listener;
		}
		
		@Override
		public void menuSelected(MenuEvent e) {
			JMenu menu = (JMenu) e.getSource();
			menu.removeAll();
			List<String> list = Prefs.getList(listKey);
			for (String entry:list) {
				JMenuItem item = new JMenuItem(StringUtils.abbreviate(entry, entry.length(), 80));
				item.addActionListener(listener);
				menu.add(item);
			}
		}

		@Override
		public void menuDeselected(MenuEvent e) {
		}

		@Override
		public void menuCanceled(MenuEvent e) {
		}
		
	}
	
	private class RecentMenuActionListener implements ActionListener {
		
		protected int selectedIndex = 0;
		
		@Override
		public void actionPerformed(ActionEvent e) {
			JMenuItem menuItem = (JMenuItem)e.getSource();
			selectedIndex = menuItem.getParent().getComponentZOrder(menuItem);
		}
	}
	
	// events
	
	@Override
	public void settingsUpdated() {
		comboQuantize.setSelectedIndex(session.getQuantizationIndex());
		comboBoxTranspose.setSelectedIndex(session.getTransposeIndex());
		textFieldLength.setText(String.valueOf(session.getLengthQuarters()));
		checkBoxMidiIn.setSelected(session.isMidiInputOn());
		checkBoxMidiOut.setSelected(session.isMidiOutputOn());
		comboMidiOut.setSelectedIndex(session.getMidiChannelOut());
		comboMidiIn.setSelectedIndex(session.getMidiChannelIn());
		if (Prefs.get(Prefs.THEME, 0)==0) {
			Theme.CURRENT = Theme.BRIGHT;
			menuItemTheme.setSelected(false);
		}
		else {
			Theme.CURRENT = Theme.DARK;
			menuItemTheme.setSelected(true);
		}
		frmDimidimi.setTitle(getWindowTitle());
	}
	
	@Override
	public void receiveClock(int pos) {
		loopDisplayPanel.updateLoopPosition(pos);
	}

	@Override
	public void receiveActive(boolean active, int pos) {
	}

	@Override
	public void noteOn(int noteNumber, int velocity, int pos) {
	}

	@Override
	public void noteOff(int notenumber, int pos) {
	}

	@Override
	public void receiveCC(int cc, int val, int pos) {
	}

	@Override
	public void receivePitchBend(int val, int pos) {
	}
	
	
}
