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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

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
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.privatepublic.midiutils.DiMIDImi;
import de.privatepublic.midiutils.MidiDeviceWrapper;
import de.privatepublic.midiutils.MidiHandler;
import de.privatepublic.midiutils.Prefs;
import de.privatepublic.midiutils.Session;
import de.privatepublic.midiutils.events.PerformanceReceiver;
import de.privatepublic.midiutils.events.SettingsUpdateReceiver;

public class UIWindow implements PerformanceReceiver, SettingsUpdateReceiver {

	private static final Logger LOG = LoggerFactory.getLogger(UIWindow.class);
	
	private static final String APP_TITLE = "dimidimi Looper";
	
	private static final String[] MIDI_CHANNELS = new String[]{"1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16"};
	private static final String[] QUANTIZE = new String[]{"none","1/2","1/4","1/8","1/16","1/32","1/4 triplets", "1/8 triplets", "1/16 triplets"};
	private static final String[] TRANSPOSE = new String[]{"+24", "+12","+11","+10","+9","+8","+7","+6","+5","+4","+3","+2","+1","0","-1","-2","-3","-4","-5","-6","-7","-8","-9","-10","-11","-12","-24"};
	
//	private boolean active;
	private JFrame frmDimidimi;
	private JTextField textFieldLength;
	private LoopDisplayPanel loopDisplayPanel;
	private JComboBox<String> comboQuantize;
	private JComboBox<String> comboBoxTranspose;
	private JComboBox comboMidiIn;
	private JComboBox comboMidiOut;
	private JCheckBox checkBoxMidiOut;
	private JCheckBox checkBoxMidiIn;
	private JLabel lblDimidimiLooper;
	private JCheckBoxMenuItem checkBoxClockInc;
	JPanel panelActive;
	private Session session;

	/**
	 * Create the application.
	 */
	public UIWindow(Session session) {
		this.session = session;
		try {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Test");
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
		} 
		catch (UnsupportedLookAndFeelException e) {
			// handle exception
		}
		catch (ClassNotFoundException e) {
			// handle exception
		}
		catch (InstantiationException e) {
			// handle exception
		}
		catch (IllegalAccessException e) {
			// handle exception
		}

		initialize();
		frmDimidimi.setVisible(true);
		// this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
		session.registerAsReceiver(this);
		session.emitSettingsUpdated();
		session.emitLoopUpdated();
		LOG.info("User interface built.");
	}

	/**
	 * Initialize the contents of the frame.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initialize() {
		frmDimidimi = new JFrame();
		frmDimidimi.setTitle(APP_TITLE);
		frmDimidimi.setBounds(100, 100, 1028, 524);
		frmDimidimi.setMinimumSize(new Dimension(720, 300));
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
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(panelLoop, GroupLayout.DEFAULT_SIZE, 1016, Short.MAX_VALUE)
						.addComponent(panel, GroupLayout.DEFAULT_SIZE, 1016, Short.MAX_VALUE)
						.addGroup(Alignment.TRAILING, groupLayout.createSequentialGroup()
							.addComponent(panelTitle, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(panelMidi, GroupLayout.DEFAULT_SIZE, 778, Short.MAX_VALUE)))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
						.addComponent(panelTitle, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(panelMidi, GroupLayout.PREFERRED_SIZE, 40, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(panelLoop, GroupLayout.DEFAULT_SIZE, 398, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(panel, GroupLayout.PREFERRED_SIZE, 37, GroupLayout.PREFERRED_SIZE)
					.addContainerGap())
		);
		SpringLayout sl_panel = new SpringLayout();
		panel.setLayout(sl_panel);
		
		JLabel lblNumberOfQuarters = new JLabel("Length");
		sl_panel.putConstraint(SpringLayout.NORTH, lblNumberOfQuarters, 11, SpringLayout.NORTH, panel);
		sl_panel.putConstraint(SpringLayout.WEST, lblNumberOfQuarters, 8, SpringLayout.WEST, panel);
		panel.add(lblNumberOfQuarters);
		
		textFieldLength = new JTextField();
		textFieldLength.setToolTipText("Loop length in quarter notes");
		sl_panel.putConstraint(SpringLayout.NORTH, textFieldLength, 5, SpringLayout.NORTH, panel);
		sl_panel.putConstraint(SpringLayout.WEST, textFieldLength, 56, SpringLayout.WEST, panel);
		textFieldLength.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(textFieldLength);
		textFieldLength.setHorizontalAlignment(SwingConstants.CENTER);
		
		textFieldLength.setText("4");
		textFieldLength.setColumns(3);
		textFieldLength.setText(String.valueOf(session.getLengthQuarters()));
		
		JButton btnApply = new JButton("Apply");
		sl_panel.putConstraint(SpringLayout.NORTH, btnApply, 5, SpringLayout.NORTH, panel);
		sl_panel.putConstraint(SpringLayout.WEST, btnApply, 0, SpringLayout.EAST, textFieldLength);
		panel.add(btnApply);
		btnApply.setEnabled(false);
		
		JLabel lblQuantizeTo = new JLabel("Quantize");
		sl_panel.putConstraint(SpringLayout.NORTH, lblQuantizeTo, 11, SpringLayout.NORTH, panel);
		sl_panel.putConstraint(SpringLayout.WEST, lblQuantizeTo, 0, SpringLayout.EAST, btnApply);
		panel.add(lblQuantizeTo);
		
		comboQuantize = new JComboBox(QUANTIZE);
		sl_panel.putConstraint(SpringLayout.NORTH, comboQuantize, 6, SpringLayout.NORTH, panel);
		sl_panel.putConstraint(SpringLayout.WEST, comboQuantize, 0, SpringLayout.EAST, lblQuantizeTo);
		comboQuantize.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(comboQuantize);
		comboQuantize.setMaximumRowCount(12);
		
		JLabel lblTranspose = new JLabel("Transpose");
		sl_panel.putConstraint(SpringLayout.NORTH, lblTranspose, 11, SpringLayout.NORTH, panel);
		sl_panel.putConstraint(SpringLayout.WEST, lblTranspose, 0, SpringLayout.EAST, comboQuantize);
		panel.add(lblTranspose);
		
		comboBoxTranspose = new JComboBox(TRANSPOSE);
		sl_panel.putConstraint(SpringLayout.NORTH, comboBoxTranspose, 6, SpringLayout.NORTH, panel);
		sl_panel.putConstraint(SpringLayout.WEST, comboBoxTranspose, 0, SpringLayout.EAST, lblTranspose);
		comboBoxTranspose.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(comboBoxTranspose);
		comboBoxTranspose.setMaximumRowCount(27);
		comboBoxTranspose.setSelectedIndex(13);
		
		JButton btnClear = new JButton("Clear");
		sl_panel.putConstraint(SpringLayout.NORTH, btnClear, 5, SpringLayout.NORTH, panel);
		btnClear.setAlignmentX(Component.RIGHT_ALIGNMENT);
		panel.add(btnClear);
		
		JButton buttonNewSession = new JButton("+");
		sl_panel.putConstraint(SpringLayout.EAST, btnClear, 0, SpringLayout.WEST, buttonNewSession);
		sl_panel.putConstraint(SpringLayout.NORTH, buttonNewSession, -5, SpringLayout.NORTH, lblNumberOfQuarters);
		sl_panel.putConstraint(SpringLayout.WEST, buttonNewSession, -83, SpringLayout.EAST, panel);
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
				session.clearPattern();;
			}
		});
		((JLabel)comboBoxTranspose.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		
		comboBoxTranspose.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				session.setTransposeIndex(comboBoxTranspose.getSelectedIndex());
				LOG.info("Transpose: {}", comboBoxTranspose.getSelectedItem());
				session.emitRefreshLoopDisplay();
			}});
		
		comboQuantize.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				session.setQuantizationIndex(comboQuantize.getSelectedIndex());
				LOG.info("Quantization: {}", comboQuantize.getSelectedItem());
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
					if (value>0 && value!=session.getLengthQuarters()){
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
		groupLayout.setAutoCreateContainerGaps(true);
		
		JLabel lblIn = new JLabel("Channel:  In");
		
		comboMidiIn = new JComboBox(MIDI_CHANNELS);
		comboMidiIn.setMaximumRowCount(16);
		
		comboMidiIn.setSelectedIndex(session.getMidiChannelIn());
		
		JLabel lblOut = new JLabel("Out");
		
		comboMidiOut = new JComboBox(MIDI_CHANNELS);
		comboMidiOut.setMaximumRowCount(16);
		comboMidiOut.setSelectedIndex(session.getMidiChannelOut());
		SpringLayout sl_panelMidi = new SpringLayout();
		sl_panelMidi.putConstraint(SpringLayout.NORTH, comboMidiOut, 10, SpringLayout.NORTH, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.NORTH, lblOut, 14, SpringLayout.NORTH, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.NORTH, comboMidiIn, 10, SpringLayout.NORTH, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.NORTH, lblIn, 14, SpringLayout.NORTH, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.EAST, comboMidiIn, -6, SpringLayout.WEST, lblOut);
		panelMidi.setLayout(sl_panelMidi);
		
		panelActive = new JPanel();
		sl_panelMidi.putConstraint(SpringLayout.NORTH, panelActive, 10, SpringLayout.NORTH, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.WEST, panelActive, 0, SpringLayout.WEST, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.SOUTH, panelActive, -10, SpringLayout.SOUTH, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.EAST, panelActive, 20, SpringLayout.WEST, panelMidi);
		panelActive.setToolTipText("Indicates active MIDI clock");
		panelActive.setBorder(null);
		panelActive.setPreferredSize(new Dimension(16, 16));
		panelActive.setBackground(Theme.colorClockOff);
		panelMidi.add(panelActive);
		panelMidi.add(lblIn);
		
		checkBoxMidiIn = new JCheckBox("");
		sl_panelMidi.putConstraint(SpringLayout.EAST, lblIn, -28, SpringLayout.EAST, checkBoxMidiIn);
		sl_panelMidi.putConstraint(SpringLayout.NORTH, checkBoxMidiIn, 10, SpringLayout.NORTH, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.EAST, checkBoxMidiIn, 3, SpringLayout.WEST, comboMidiIn);
		checkBoxMidiIn.setSelected(true);
		checkBoxMidiIn.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				session.setMidiInputOn(checkBoxMidiIn.isSelected());
				LOG.info("Receive Notes {}", session.isMidiInputOn());
			}
		});
		checkBoxMidiIn.setToolTipText("Receive Notes from selected Channel");
		panelMidi.add(checkBoxMidiIn);
		panelMidi.add(comboMidiIn);
		panelMidi.add(lblOut);
		
		checkBoxMidiOut = new JCheckBox("");
		sl_panelMidi.putConstraint(SpringLayout.EAST, checkBoxMidiOut, 3, SpringLayout.WEST, comboMidiOut);
		sl_panelMidi.putConstraint(SpringLayout.EAST, lblOut, 0, SpringLayout.WEST, checkBoxMidiOut);
		sl_panelMidi.putConstraint(SpringLayout.NORTH, checkBoxMidiOut, 10, SpringLayout.NORTH, panelMidi);
		checkBoxMidiOut.setSelected(true);
		checkBoxMidiOut.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				session.setMidiOutputOn(checkBoxMidiOut.isSelected());
				LOG.info("Send Notes {}", session.isMidiOutputOn());
			}
		});
		checkBoxMidiOut.setToolTipText("Output Notes on selcted Channel");
		panelMidi.add(checkBoxMidiOut);
		panelMidi.add(comboMidiOut);
		
		JButton btnNotesOff = new JButton("Panic");
		sl_panelMidi.putConstraint(SpringLayout.EAST, comboMidiOut, 0, SpringLayout.WEST, btnNotesOff);
		sl_panelMidi.putConstraint(SpringLayout.NORTH, btnNotesOff, 9, SpringLayout.NORTH, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.EAST, btnNotesOff, -10, SpringLayout.EAST, panelMidi);
		btnNotesOff.setToolTipText("Turns off all playing or stuck MIDI notes.");
		panelMidi.add(btnNotesOff);
		
		btnNotesOff.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				session.getMidiHandler().sendAllNotesOff();
			}});
		panelLoop.setLayout(new BorderLayout(0, 0));
		loopDisplayPanel = new LoopDisplayPanel(session);
		loopDisplayPanel.setBackground(Color.WHITE);
		panelLoop.add(loopDisplayPanel);
		loopDisplayPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		frmDimidimi.getContentPane().setLayout(groupLayout);
		
		ActionListener settingChanged = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int midiIn = comboMidiIn.getSelectedIndex();
				int midiOut = comboMidiOut.getSelectedIndex();
				session.setMidiChannelIn(midiIn);
				session.setMidiChannelOut(midiOut);
			}};
		
		comboMidiIn.addActionListener(settingChanged);
		comboMidiOut.addActionListener(settingChanged);
		
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
					try {
						DiMIDImi.loadSession(selectedFile);
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(frmDimidimi, "Could not load file\n"+e1.getMessage());
		        		LOG.error("Could not load file", e1);
					}
		        	Prefs.put(Prefs.FILE_SESSION_LAST_USED_NAME, selectedFile.getPath());
				}
			}
		});
		menu.add(menuItem);
		menu.addSeparator();
//		menuItem = new JMenuItem("Save");
//		menu.add(menuItem);
		menuItem = new JMenuItem("Save as...");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File selectedFile = GUIUtils.saveDialog("Save Session", GUIUtils.FILE_FILTER_SESSION, Prefs.FILE_SESSION_LAST_USED_NAME);
				if (selectedFile!=null) {
					try {
						DiMIDImi.saveSession(selectedFile);
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
		menuItem = new JMenuItem("Load...");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File selectedFile = GUIUtils.loadDialog("Load Loop", GUIUtils.FILE_FILTER_LOOP, Prefs.FILE_LOOP_LAST_USED_NAME);
		        if (selectedFile!=null) {
		        	Prefs.put(Prefs.FILE_LOOP_LAST_USED_NAME, selectedFile.getPath());
		        	try {
		        		session.loadLoop(selectedFile);
		        		frmDimidimi.setTitle(APP_TITLE+" - "+FilenameUtils.getBaseName(selectedFile.getName()));
					} catch (Exception e1) {
						LOG.error("Error loading file", e1);
						JOptionPane.showMessageDialog(frmDimidimi,
							    "Error loading file!",
							    "Load Loop",
							    JOptionPane.ERROR_MESSAGE);
					}
		            
		        }
			}
		});
		menu.add(menuItem);
		menu.addSeparator();
		menuItem = new JMenuItem("Save...");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File selectedFile = GUIUtils.saveDialog("Save Loop", GUIUtils.FILE_FILTER_LOOP, Prefs.FILE_LOOP_LAST_USED_NAME);
		        if (selectedFile!=null) {
		        	try {
		        		session.saveLoop(selectedFile);
		        		frmDimidimi.setTitle(APP_TITLE+" - "+FilenameUtils.getBaseName(selectedFile.getName()));
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
		menuItem = new JMenuItem("Double");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				session.doublePattern();
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
		menu.addSeparator();
		checkBoxClockInc = new JCheckBoxMenuItem("48ppq");
		checkBoxClockInc.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean selected = e.getStateChange()==ItemEvent.SELECTED;
				int inc = selected?1:2;
				session.setClockIncrement(inc);
			}
		});
		menu.add(checkBoxClockInc);
		menuBar.add(menu);
		
		return menuBar;
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
	
	@Override
	public void receiveClock(int pos) {
		loopDisplayPanel.updateLoopPosition(pos);
		if (MidiHandler.ACTIVE) {
			if (pos%Session.TICK_COUNT_BASE<Session.TICK_COUNT_BASE/2) {
				panelActive.setBackground(Theme.colorClockOn);
			}
			else {
				panelActive.setBackground(Theme.colorClockOff);
			}
		}
	}

	@Override
	public void receiveActive(boolean active, int pos) {
		if (active) {
			panelActive.setBackground(Theme.colorClockOn);
		}
		else {
			panelActive.setBackground(Theme.colorClockOff);
		}
	}

	@Override
	public void settingsUpdated() {
		// update quantization, length, transpose
		comboQuantize.setSelectedIndex(session.getQuantizationIndex());
		comboBoxTranspose.setSelectedIndex(session.getTransposeIndex());
		textFieldLength.setText(String.valueOf(session.getLengthQuarters()));
		checkBoxClockInc.setSelected(session.getClockIncrement()==1);
		checkBoxMidiIn.setSelected(session.isMidiInputOn());
		checkBoxMidiOut.setSelected(session.isMidiOutputOn());
		comboMidiOut.setSelectedIndex(session.getMidiChannelOut());
		comboMidiIn.setSelectedIndex(session.getMidiChannelIn());
		frmDimidimi.repaint();
	}

	@Override
	public void noteOn(int noteNumber, int velocity, int pos) {
	}

	@Override
	public void noteOff(int notenumber, int pos) {
	}
	
	
}
