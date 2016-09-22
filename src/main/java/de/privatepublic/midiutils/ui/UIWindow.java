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
import java.lang.reflect.Method;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
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
import javax.swing.filechooser.FileFilter;

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
	private JCheckBox chckbxclockinc;
	private JLabel lblDimidimiLooper;
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
		settingsUpdated();
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
		frmDimidimi.setMinimumSize(new Dimension(1028, 300));
		frmDimidimi.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frmDimidimi.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				DiMIDImi.removeSession(session);
			}
		});
		setIcon(frmDimidimi);
		
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("Session");
		JMenuItem menuItem = new JMenuItem("Load...");
		menu.add(menuItem);
		menu.addSeparator();
		menuItem = new JMenuItem("Save");
		menu.add(menuItem);
		menuItem = new JMenuItem("Save as...");
		menu.add(menuItem);
		menuBar.add(menu);
		
		frmDimidimi.setJMenuBar(menuBar);
		
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
		sl_panel.putConstraint(SpringLayout.NORTH, textFieldLength, 5, SpringLayout.NORTH, panel);
		sl_panel.putConstraint(SpringLayout.WEST, textFieldLength, 56, SpringLayout.WEST, panel);
		textFieldLength.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(textFieldLength);
		textFieldLength.setHorizontalAlignment(SwingConstants.CENTER);
		
		textFieldLength.setText("4");
		textFieldLength.setColumns(3);
		textFieldLength.setText(String.valueOf(session.getLengthQuarters()));
		
		JLabel lblLength2 = new JLabel("¼s");
		sl_panel.putConstraint(SpringLayout.NORTH, lblLength2, 11, SpringLayout.NORTH, panel);
		sl_panel.putConstraint(SpringLayout.WEST, lblLength2, 111, SpringLayout.WEST, panel);
		panel.add(lblLength2);
		
		JButton btnApply = new JButton("Apply");
		sl_panel.putConstraint(SpringLayout.NORTH, btnApply, 5, SpringLayout.NORTH, panel);
		sl_panel.putConstraint(SpringLayout.WEST, btnApply, 133, SpringLayout.WEST, panel);
		panel.add(btnApply);
		btnApply.setEnabled(false);
		
		JLabel lblQuantizeTo = new JLabel("Quantize");
		sl_panel.putConstraint(SpringLayout.NORTH, lblQuantizeTo, 11, SpringLayout.NORTH, panel);
		sl_panel.putConstraint(SpringLayout.WEST, lblQuantizeTo, 218, SpringLayout.WEST, panel);
		panel.add(lblQuantizeTo);
		
		comboQuantize = new JComboBox(QUANTIZE);
		sl_panel.putConstraint(SpringLayout.NORTH, comboQuantize, 6, SpringLayout.NORTH, panel);
		sl_panel.putConstraint(SpringLayout.WEST, comboQuantize, 279, SpringLayout.WEST, panel);
		comboQuantize.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(comboQuantize);
		comboQuantize.setMaximumRowCount(12);
		
		JLabel lblTranspose = new JLabel("Transpose");
		sl_panel.putConstraint(SpringLayout.NORTH, lblTranspose, 11, SpringLayout.NORTH, panel);
		sl_panel.putConstraint(SpringLayout.WEST, lblTranspose, 420, SpringLayout.WEST, panel);
		panel.add(lblTranspose);
		
		comboBoxTranspose = new JComboBox(TRANSPOSE);
		sl_panel.putConstraint(SpringLayout.NORTH, comboBoxTranspose, 6, SpringLayout.NORTH, panel);
		sl_panel.putConstraint(SpringLayout.WEST, comboBoxTranspose, 490, SpringLayout.WEST, panel);
		comboBoxTranspose.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(comboBoxTranspose);
		comboBoxTranspose.setMaximumRowCount(27);
		comboBoxTranspose.setSelectedIndex(13);
		
		JButton btnDouble = new JButton("x2");
		sl_panel.putConstraint(SpringLayout.NORTH, btnDouble, 5, SpringLayout.NORTH, panel);
		btnDouble.setAlignmentX(Component.RIGHT_ALIGNMENT);
		panel.add(btnDouble);
		
		JButton btnClear = new JButton("Clear");
		sl_panel.putConstraint(SpringLayout.WEST, btnDouble, -94, SpringLayout.WEST, btnClear);
		sl_panel.putConstraint(SpringLayout.EAST, btnDouble, -5, SpringLayout.WEST, btnClear);
		sl_panel.putConstraint(SpringLayout.NORTH, btnClear, 5, SpringLayout.NORTH, panel);
		btnClear.setAlignmentX(Component.RIGHT_ALIGNMENT);
		panel.add(btnClear);
		
		JButton btnLoad = new JButton("Load...");
		sl_panel.putConstraint(SpringLayout.WEST, btnClear, -81, SpringLayout.WEST, btnLoad);
		sl_panel.putConstraint(SpringLayout.EAST, btnClear, -5, SpringLayout.WEST, btnLoad);
		sl_panel.putConstraint(SpringLayout.NORTH, btnLoad, 5, SpringLayout.NORTH, panel);
		btnLoad.setAlignmentX(Component.RIGHT_ALIGNMENT);
		panel.add(btnLoad);
		
		JButton btnSave = new JButton("Save...");
		sl_panel.putConstraint(SpringLayout.WEST, btnLoad, -91, SpringLayout.WEST, btnSave);
		sl_panel.putConstraint(SpringLayout.EAST, btnLoad, -5, SpringLayout.WEST, btnSave);
		sl_panel.putConstraint(SpringLayout.NORTH, btnSave, 5, SpringLayout.NORTH, panel);
		btnSave.setAlignmentX(Component.RIGHT_ALIGNMENT);
		panel.add(btnSave);
		
		JButton buttonNewSession = new JButton("+");
		sl_panel.putConstraint(SpringLayout.WEST, btnSave, -90, SpringLayout.WEST, buttonNewSession);
		sl_panel.putConstraint(SpringLayout.EAST, btnSave, -6, SpringLayout.WEST, buttonNewSession);
		sl_panel.putConstraint(SpringLayout.NORTH, buttonNewSession, -5, SpringLayout.NORTH, lblNumberOfQuarters);
		sl_panel.putConstraint(SpringLayout.WEST, buttonNewSession, -83, SpringLayout.EAST, panel);
		sl_panel.putConstraint(SpringLayout.EAST, buttonNewSession, -8, SpringLayout.EAST, panel);
		buttonNewSession.setAlignmentX(Component.RIGHT_ALIGNMENT);
		panel.add(buttonNewSession);
		buttonNewSession.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DiMIDImi.createSession();
			}
		});
		
		
		btnSave.addActionListener(new ActionListener() {
			@SuppressWarnings("serial")
			public void actionPerformed(ActionEvent arg0) {
				String recentFile = Prefs.get(Prefs.FILE_LAST_USED_NAME, null);
		        JFileChooser chooser = new JFileChooser() {
					@Override
		            public void approveSelection(){
		                File f = getSelectedFile();
		                if(f.exists()){
		                    int result = JOptionPane.showConfirmDialog(this, "Overwrite existing file?", "File exists", JOptionPane.YES_NO_CANCEL_OPTION);
		                    switch(result){
		                        case JOptionPane.YES_OPTION:
		                            super.approveSelection();
		                            return;
		                        case JOptionPane.NO_OPTION:
		                            return;
		                        case JOptionPane.CLOSED_OPTION:
		                            return;
		                        case JOptionPane.CANCEL_OPTION:
		                            cancelSelection();
		                            return;
		                    }
		                }
		                super.approveSelection();
		            }        
		        };
		        chooser.setAcceptAllFileFilterUsed(false);
		        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
		        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		        chooser.addChoosableFileFilter(new FileFilter() {
					@Override
					public String getDescription() { return "diMIDImi Loop (*.diMIDImi)"; }
					@Override
					public boolean accept(File f) {	return (f.isDirectory() || f.getName().toLowerCase().endsWith(".dimidimi"));}
				});
		        
		        if (recentFile!=null) {
		        	chooser.setSelectedFile(new File(recentFile));
		        	chooser.setCurrentDirectory(new File(recentFile).getParentFile());
		        }
		        
		        chooser.setFileFilter(chooser.getChoosableFileFilters()[0]);
		        
		        chooser.setMultiSelectionEnabled(false);
		        chooser.setDialogTitle("Save Loop");
		        int retvalue = chooser.showDialog(null, "Save Loop");
		        if (retvalue==JFileChooser.APPROVE_OPTION) {
		        	File selectedFile = chooser.getSelectedFile();
		        	if (!"dimidimi".equals(FilenameUtils.getExtension(selectedFile.getName()))) {
		        		selectedFile = new File(selectedFile.getPath()+".dimidimi");
		        	}
		        	try {
		        		session.saveLoop(selectedFile);
		        		frmDimidimi.setTitle(APP_TITLE+" - "+FilenameUtils.getBaseName(selectedFile.getName()));
		        	}
		        	catch(Exception e) {
		        		JOptionPane.showMessageDialog(frmDimidimi, "Could not write file\n"+e.getMessage());
		        		LOG.error("Could not write file", e);
		        	}
		        	Prefs.put(Prefs.FILE_LAST_USED_NAME, selectedFile.getPath());
		        }
			}
		});
		
		btnLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String recentPath = Prefs.get(Prefs.FILE_LAST_USED_NAME, null);
		        JFileChooser chooser = new JFileChooser();
		        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
		        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		        chooser.setMultiSelectionEnabled(false);
		        chooser.setAcceptAllFileFilterUsed(false);
		        chooser.addChoosableFileFilter(new FileFilter() {
					@Override
					public String getDescription() { return "diMIDImi Loop (*.diMIDImi)"; }
					@Override
					public boolean accept(File f) {	return f.isDirectory() || (f.getName().toLowerCase().endsWith(".dimidimi"));}
				});
		        chooser.setDialogTitle("Load Loop");
		        if (recentPath!=null) {
		        	chooser.setSelectedFile(new File(recentPath));
		        	chooser.setCurrentDirectory(new File(recentPath).getParentFile());
		        }
		        int retvalue = chooser.showDialog(null, "Load Loop");
		        if (retvalue==JFileChooser.APPROVE_OPTION) {
		        	File selectedFile = chooser.getSelectedFile();
		        	Prefs.put(Prefs.FILE_LAST_USED_NAME, selectedFile.getPath());
		        	try {
		        		session.loadLoop(selectedFile);
		        		frmDimidimi.setTitle(APP_TITLE+" - "+FilenameUtils.getBaseName(selectedFile.getName()));
					} catch (Exception e) {
						LOG.error("Error loading file", e);
						JOptionPane.showMessageDialog(frmDimidimi,
							    "Error loading file!",
							    "Load Loop",
							    JOptionPane.ERROR_MESSAGE);
					}
		            
		        }
			}
		});
		
		btnClear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				session.clearPattern();;
			}
		});
		btnDouble.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				session.doublePattern();
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
		
		JComboBox comboMidiIn = new JComboBox(MIDI_CHANNELS);
		comboMidiIn.setMaximumRowCount(16);
		
		comboMidiIn.setSelectedIndex(session.getMidiChannelIn());
		
		JLabel lblOut = new JLabel("Out");
		
		JComboBox comboMidiOut = new JComboBox(MIDI_CHANNELS);
		comboMidiOut.setMaximumRowCount(16);
		comboMidiOut.setSelectedIndex(session.getMidiChannelOut());
		
		chckbxclockinc = new JCheckBox("48ppq");
		chckbxclockinc.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				boolean selected = e.getStateChange()==ItemEvent.SELECTED;
				int inc = selected?1:2;
				session.setClockIncrement(inc);
			}
		});
		SpringLayout sl_panelMidi = new SpringLayout();
		sl_panelMidi.putConstraint(SpringLayout.NORTH, comboMidiOut, 10, SpringLayout.NORTH, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.NORTH, lblOut, 14, SpringLayout.NORTH, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.NORTH, comboMidiIn, 10, SpringLayout.NORTH, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.NORTH, lblIn, 14, SpringLayout.NORTH, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.NORTH, chckbxclockinc, 10, SpringLayout.NORTH, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.EAST, chckbxclockinc, -12, SpringLayout.WEST, lblIn);
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
		chckbxclockinc.setToolTipText("Toggle between 24 or 48 ppq midi clock");
		panelMidi.add(chckbxclockinc);
		panelMidi.add(lblIn);
		
		JCheckBox checkBoxMidiIn = new JCheckBox("");
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
		
		JCheckBox checkBoxMidiOut = new JCheckBox("");
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
		
		JButton btnSelectInputDevices = new JButton("Devices...");
		sl_panelMidi.putConstraint(SpringLayout.NORTH, btnSelectInputDevices, 9, SpringLayout.NORTH, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.EAST, comboMidiOut, -16, SpringLayout.WEST, btnSelectInputDevices);
		panelMidi.add(btnSelectInputDevices);
		btnSelectInputDevices.addActionListener(new ActionListener() {
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
			}});
		
		JButton btnNotesOff = new JButton("Panic");
		sl_panelMidi.putConstraint(SpringLayout.NORTH, btnNotesOff, 9, SpringLayout.NORTH, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.EAST, btnSelectInputDevices, -16, SpringLayout.WEST, btnNotesOff);
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
		chckbxclockinc.setSelected(session.getClockIncrement()==1);
		frmDimidimi.repaint();
	}

	@Override
	public void noteOn(int noteNumber, int velocity, int pos) {
	}

	@Override
	public void noteOff(int notenumber, int pos) {
	}
}
