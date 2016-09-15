package de.privatepublic.midiutils.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.privatepublic.midiutils.MidiHandler;
import de.privatepublic.midiutils.MidiHandler.MidiDeviceWrapper;
import de.privatepublic.midiutils.NoteRun;
import de.privatepublic.midiutils.Prefs;
import de.privatepublic.midiutils.events.ClockReceiver;
import de.privatepublic.midiutils.events.LoopUpdateReceiver;
import de.privatepublic.midiutils.events.ManipulateReceiver;
import de.privatepublic.midiutils.events.SettingsUpdateReceiver;
import de.privatepublic.midiutils.events.StorageReceiver;

public class UIWindow implements ClockReceiver, SettingsUpdateReceiver {

	private static final Logger LOG = LoggerFactory.getLogger(UIWindow.class);
	
	private static final String[] MIDI_CHANNELS = new String[]{"1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16"};
	private static final String[] QUANTIZE = new String[]{"none","1/2","1/4","1/8","1/16","1/32","1/4 triplets", "1/8 triplets", "1/16 triplets"};
	private static final String[] TRANSPOSE = new String[]{"+24", "+12","+11","+10","+9","+8","+7","+6","+5","+4","+3","+2","+1","0","-1","-2","-3","-4","-5","-6","-7","-8","-9","-10","-11","-12","-24"};
	
	private JFrame frmDimidimi;
	private JTextField textFieldLength;
	private LoopDisplayPanel loopDisplayPanel;
	private JComboBox<String> comboQuantize;
	private JComboBox<String> comboBoxTranspose;
	private JCheckBox chckbxppq;
	private JPanel panelIndicator;

	/**
	 * Create the application.
	 */
	public UIWindow() {
		try {
			// Set cross-platform Java L&F (also called "Metal")
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
		LOG.info("User interface built.");
	}

	/**
	 * Initialize the contents of the frame.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initialize() {
		frmDimidimi = new JFrame();
		frmDimidimi.setTitle("diMIDImi");
		frmDimidimi.setBounds(100, 100, 990, 557);
		frmDimidimi.setMinimumSize(new Dimension(900, 557));
		frmDimidimi.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmDimidimi.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		setIcon(frmDimidimi);
		
		JPanel panel = new JPanel();
		panel.setBackground(Color.WHITE);
		panel.setBorder(new LineBorder(Color.GRAY));
		
		JButton btnClear = new JButton("Clear");
		
		JLabel lblQuantizeTo = new JLabel("Quantize");
		
		comboQuantize = new JComboBox(QUANTIZE);
		comboQuantize.setMaximumRowCount(12);
		
		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new TitledBorder(null, "MIDI", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		
		JLabel lblDimidimiLooper = new JLabel("diMIDImi Looper");
		lblDimidimiLooper.setFont(lblDimidimiLooper.getFont().deriveFont(lblDimidimiLooper.getFont().getStyle() | Font.BOLD, lblDimidimiLooper.getFont().getSize() + 9f));
		lblDimidimiLooper.setIcon(new ImageIcon(UIWindow.class.getResource("/icon-64.png")));
		
		comboBoxTranspose = new JComboBox(TRANSPOSE);
		comboBoxTranspose.setMaximumRowCount(27);
		comboBoxTranspose.setSelectedIndex(13);
		((JLabel)comboBoxTranspose.getRenderer()).setHorizontalAlignment(JLabel.RIGHT);
		
		JLabel lblTranspose = new JLabel("Transpose");
		
		JLabel lblNumberOfQuarters = new JLabel("Length (¼)");
		
		JButton btnApply = new JButton("Apply");
		btnApply.setEnabled(false);
		
		textFieldLength = new JTextField();
		
		textFieldLength.setText("4");
		textFieldLength.setColumns(10);
		textFieldLength.setText(String.valueOf(MidiHandler.instance().getNumberQuarters()));
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
					if (value>0 && value!=MidiHandler.instance().getNumberQuarters()){
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
		
		
		
		btnApply.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				btnApply.setEnabled(false);
				int numberQuarters = Integer.parseInt(textFieldLength.getText());
				MidiHandler.instance().updateSettings(MidiHandler.instance().getMidiChannelIn(), MidiHandler.instance().getMidiChannelOut(), numberQuarters);
				LoopUpdateReceiver.Dispatcher.sendRefreshLoopDisplay();
			}
		});
		
		JButton btnSave = new JButton("Save...");
		
		JButton btnLoad = new JButton("Load...");
		
		JButton btnDouble = new JButton("Double");
		btnDouble.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		GroupLayout groupLayout = new GroupLayout(frmDimidimi.getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(panel, GroupLayout.DEFAULT_SIZE, 954, Short.MAX_VALUE)
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(lblDimidimiLooper)
							.addPreferredGap(ComponentPlacement.RELATED, 90, Short.MAX_VALUE)
							.addComponent(panel_1, GroupLayout.PREFERRED_SIZE, 624, GroupLayout.PREFERRED_SIZE))
						.addGroup(groupLayout.createSequentialGroup()
							.addComponent(lblNumberOfQuarters)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(textFieldLength, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(btnApply)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(lblQuantizeTo)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(comboQuantize, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(lblTranspose)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(comboBoxTranspose, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED, 248, Short.MAX_VALUE)
							.addComponent(btnDouble)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(btnClear)
							.addGap(18)
							.addComponent(btnLoad)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(btnSave)))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING, false)
						.addComponent(panel_1, GroupLayout.PREFERRED_SIZE, 65, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblDimidimiLooper))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(panel, GroupLayout.DEFAULT_SIZE, 396, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblNumberOfQuarters)
						.addComponent(textFieldLength, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(btnApply)
						.addComponent(lblQuantizeTo)
						.addComponent(comboQuantize, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(comboBoxTranspose, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblTranspose)
						.addComponent(btnSave)
						.addComponent(btnLoad)
						.addComponent(btnClear)
						.addComponent(btnDouble)))
		);
		groupLayout.setAutoCreateContainerGaps(true);
		
		JLabel lblIn = new JLabel("Channel In");
		
		JComboBox comboMidiIn = new JComboBox(MIDI_CHANNELS);
		comboMidiIn.setMaximumRowCount(16);
		
		comboMidiIn.setSelectedIndex(MidiHandler.instance().getMidiChannelIn());
		
		JLabel lblOut = new JLabel("Out");
		
		JComboBox comboMidiOut = new JComboBox(MIDI_CHANNELS);
		comboMidiOut.setMaximumRowCount(16);
		comboMidiOut.setSelectedIndex(MidiHandler.instance().getMidiChannelOut());
		panel_1.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		chckbxppq = new JCheckBox("48ppq");
		chckbxppq.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				MidiHandler.instance().set48PPQ(chckbxppq.isSelected());
				Prefs.put(Prefs.MIDI_48PPQ, chckbxppq.isSelected()?1:2);
			}
		});
		
		JLabel lblClock = new JLabel("clock");
		panel_1.add(lblClock);
		
		panelIndicator = new JPanel();
		panelIndicator.setToolTipText("Active MIDI Clock Input");
		panelIndicator.setBackground(Color.GRAY);
		panelIndicator.setBorder(new LineBorder(new Color(0, 0, 0), 1, true));
		panel_1.add(panelIndicator);
		chckbxppq.setToolTipText("Toggle between 24 or 48 ppq midi clock");
		panel_1.add(chckbxppq);
		panel_1.add(lblIn);
		panel_1.add(comboMidiIn);
		panel_1.add(lblOut);
		panel_1.add(comboMidiOut);
		
		JButton btnSelectInputDevices = new JButton("MIDI Devices...");
		panel_1.add(btnSelectInputDevices);
		btnSelectInputDevices.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
				CheckboxList listinput = new CheckboxList("Activate Input Devices");
				JCheckBox[] inboxes = new JCheckBox[MidiHandler.instance().getInputDevices().size()];
				for (int i = 0; i < inboxes.length; i++) {
					MidiDeviceWrapper dev = MidiHandler.instance().getInputDevices().get(i);
					JCheckBox cbox = new JCheckBox(dev.toString());
					cbox.setSelected(dev.isActiveForInput());
					inboxes[i] = cbox;
				}
				listinput.setListData(inboxes);
				
				CheckboxList listoutput = new CheckboxList("Activate Output Devices");
				JCheckBox[] outboxes = new JCheckBox[MidiHandler.instance().getOutputDevices().size()];
				for (int i = 0; i < outboxes.length; i++) {
					MidiDeviceWrapper dev = MidiHandler.instance().getOutputDevices().get(i);
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
						MidiDeviceWrapper dev = MidiHandler.instance().getInputDevices().get(i);
						dev.setActiveForInput(inboxes[i].isSelected());
					}
					MidiHandler.instance().storeSelectedInDevices();
					
					for (int i = 0; i < outboxes.length; i++) {
						MidiDeviceWrapper dev = MidiHandler.instance().getOutputDevices().get(i);
						dev.setActiveForOutput(outboxes[i].isSelected());
					}
					MidiHandler.instance().storeSelectedOutDevices();
				}
				LoopUpdateReceiver.Dispatcher.sendRefreshLoopDisplay();
			}});
		
		JButton btnNotesOff = new JButton("Panic");
		panel_1.add(btnNotesOff);
		
		btnNotesOff.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MidiHandler.instance().sendAllNotesOff();
			}});
		panel.setLayout(new BorderLayout(0, 0));
		loopDisplayPanel = new LoopDisplayPanel();
		loopDisplayPanel.setBackground(Color.WHITE);
		panel.add(loopDisplayPanel);
		loopDisplayPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		frmDimidimi.getContentPane().setLayout(groupLayout);
		
		ActionListener settingChanged = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int midiIn = comboMidiIn.getSelectedIndex();
				int midiOut = comboMidiOut.getSelectedIndex();
				MidiHandler.instance().updateSettings(midiIn, midiOut, MidiHandler.instance().getNumberQuarters());
			}};
		
		comboMidiIn.addActionListener(settingChanged);
		comboMidiOut.addActionListener(settingChanged);
		
		comboQuantize.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO better organisation
				NoteRun.APPLY_QUANTIZATION = comboQuantize.getSelectedIndex();
				LOG.info("Quantization: {}", comboQuantize.getSelectedItem());
				LoopUpdateReceiver.Dispatcher.sendRefreshLoopDisplay();
			}});
		
		comboBoxTranspose.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				NoteRun.APPLY_TRANSPOSE = comboBoxTranspose.getSelectedIndex();
				LOG.info("Transpose: {}", comboBoxTranspose.getSelectedItem());
				LoopUpdateReceiver.Dispatcher.sendRefreshLoopDisplay();
			}});
		
		btnClear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ManipulateReceiver.Dispatcher.sendClearPattern();;
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
		        		StorageReceiver.Dispatcher.sendSaveRequest(selectedFile);
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
		        		StorageReceiver.Dispatcher.sendLoadRequest(selectedFile);
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
		
		LoopUpdateReceiver.Dispatcher.register(loopDisplayPanel);
		SettingsUpdateReceiver.Dispatcher.register(this);
		SettingsUpdateReceiver.Dispatcher.sendSettingsUpdated();
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
	
	

	@Override
	public void receiveClock(int pos) {
		loopDisplayPanel.updateLoopPosition(pos);
	}

	@Override
	public void receiveActive(boolean active, int pos) {
		if (active) {
			panelIndicator.setBackground(Color.GREEN);
		}
		else {
			panelIndicator.setBackground(Color.GRAY);
		}
	}

	@Override
	public void settingsUpdated() {
		// update quantization, length, transpose
		comboQuantize.setSelectedIndex(NoteRun.APPLY_QUANTIZATION);
		comboBoxTranspose.setSelectedIndex(NoteRun.APPLY_TRANSPOSE);
		textFieldLength.setText(String.valueOf(MidiHandler.instance().getNumberQuarters()));
		chckbxppq.setSelected(MidiHandler.instance().getPPQDiv()==2);
	}
}
