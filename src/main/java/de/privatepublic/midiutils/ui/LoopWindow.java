package de.privatepublic.midiutils.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.swing.ButtonGroup;
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
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.privatepublic.midiutils.DiMIDImi;
import de.privatepublic.midiutils.Loop;
import de.privatepublic.midiutils.Loop.QueuedState;
import de.privatepublic.midiutils.MidiDeviceWrapper;
import de.privatepublic.midiutils.MidiHandler;
import de.privatepublic.midiutils.Note;
import de.privatepublic.midiutils.Note.TransformationProvider;
import de.privatepublic.midiutils.Prefs;
import de.privatepublic.midiutils.events.FocusReceiver;
import de.privatepublic.midiutils.events.PerformanceReceiver;
import de.privatepublic.midiutils.events.SettingsUpdateReceiver;

public class LoopWindow implements PerformanceReceiver, SettingsUpdateReceiver, FocusReceiver {

	private static final Logger LOG = LoggerFactory.getLogger(LoopWindow.class);
	
	public static int WINDOW_MAX_WIDTH = 680;
	public static int WINDOW_MAX_HEIGHT = 300;
	
	private static final String APP_TITLE = "dimidimi Looper";
	
	private static final String[] MIDI_CHANNELS_IN = new String[]{"In: 1","In: 2","In: 3","In: 4","In: 5","In: 6","In: 7","In: 8","In: 9","In: 10","In: 11","In: 12","In: 13","In: 14","In: 15","In: 16"};
	private static final String[] MIDI_CHANNELS_OUT = new String[]{"Out: 1","Out: 2","Out: 3","Out: 4","Out: 5","Out: 6","Out: 7","Out: 8","Out: 9","Out: 10","Out: 11","Out: 12","Out: 13","Out: 14","Out: 15","Out: 16"};
	private JFrame frmDimidimi;
	private JPanel panelMidi;
	private LoopDisplayPanel loopDisplayPanel;
	private JComboBox<String> comboQuantize;
	private JComboBox<String> comboBoxTranspose;
	private JComboBox<String> comboMidiIn;
	private JComboBox<String> comboMidiOut;
	private JCheckBox toggleRecord;
	private JLabel lblDimidimiLooper;
	private JRadioButtonMenuItem menuItemThemeDark;
	private JRadioButtonMenuItem menuItemThemeBright;
	private JCheckBoxMenuItem menuItemAnimate;
	private JCheckBox toggleDrumsLayout;
	private JPanel panelLoop;
	private Loop loop;
	private String titleExtension = null;
	private JCheckBox toggleMetronome;

	public LoopWindow(Loop loop) {
		this.loop = loop;
		try {
			System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", APP_TITLE);
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
		} 
		catch (Exception e) {
			LOG.warn("Could not set look and feel", e);
		}
		initialize();
		
		loop.registerReceiver(this);
		loop.triggerSettingsUpdated();
		loop.triggerNotesUpdated();
		LOG.debug("User interface built.");
	}
	
	public void setVisible(boolean visible) {
		frmDimidimi.setVisible(visible);		
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
		return APP_TITLE+" - "+(titleExtension!=null?titleExtension:"")+" (#"+(loop.getMidiChannelOut()+1)+")"; 
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
				Loop.removeLoop(loop);
			}
		});
		setIcon(frmDimidimi);
		
		frmDimidimi.setJMenuBar(buildMenu());
		
		panelLoop = new JPanel();
		panelLoop.setFocusable(false);
		panelLoop.setBackground(Theme.APPLY.colorBackground());
		panelLoop.setBorder(new LineBorder(Theme.APPLY.colorBackground(), 3));
		
		panelMidi = new JPanel();
		panelMidi.setBorder(null);
		
		panelFooter = new JPanel();
		panelFooter.setBorder(null);
		GroupLayout groupLayout = new GroupLayout(frmDimidimi.getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
					.addComponent(panelLoop, GroupLayout.DEFAULT_SIZE, 1028, Short.MAX_VALUE)
					.addComponent(panelFooter, GroupLayout.DEFAULT_SIZE, 1028, Short.MAX_VALUE)
					.addComponent(panelMidi, GroupLayout.DEFAULT_SIZE, 1028, Short.MAX_VALUE))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(panelMidi, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
					.addComponent(panelLoop, GroupLayout.DEFAULT_SIZE, 409, Short.MAX_VALUE)
					.addGap(1)
					.addComponent(panelFooter, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE)
					.addGap(4))
		);
		SpringLayout sl_panel = new SpringLayout();
		panelFooter.setLayout(sl_panel);
		slider = new JSlider();
		slider.setOpaque(false);
		slider.setPaintTicks(true);
		labelLength = new JLabel("8");
		labelLength.setOpaque(false);
		sl_panel.putConstraint(SpringLayout.WEST, labelLength, 0, SpringLayout.EAST, slider);
		labelLength.setHorizontalAlignment(SwingConstants.CENTER);
		sl_panel.putConstraint(SpringLayout.VERTICAL_CENTER, slider, 0, SpringLayout.VERTICAL_CENTER, panelFooter);
		
		labelLength.setPreferredSize(new Dimension(20, 16));
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				int quarters = Math.max(slider.getValue(), 1);
				slider.setToolTipText("Loop length: "+quarters+" quarter note"+(quarters>1?"s":""));
				labelLength.setText(""+quarters);
				loop.setLengthQuarters(quarters);
				loop.triggerRefreshLoopDisplay();
			}
		});
		slider.setSnapToTicks(true);
		slider.setValue(8);
		slider.setMinimum(0);
		slider.setMaximum(32);
		slider.setLabelTable(LENGTH_LABELS);
		slider.setMinorTickSpacing(1);
		slider.setMajorTickSpacing(4);
		panelFooter.add(slider);
		sl_panel.putConstraint(SpringLayout.VERTICAL_CENTER, labelLength, 0, SpringLayout.VERTICAL_CENTER, panelFooter);
		
		panelFooter.add(labelLength);
		
		comboQuantize = new JComboBox(TransformationProvider.QUANTIZE_LABEL);
		sl_panel.putConstraint(SpringLayout.WEST, comboQuantize, 0, SpringLayout.EAST, labelLength);
		comboQuantize.setToolTipText("Note quantization");
		sl_panel.putConstraint(SpringLayout.VERTICAL_CENTER, comboQuantize, 0, SpringLayout.VERTICAL_CENTER, panelFooter);
		panelFooter.add(comboQuantize);
		comboQuantize.setMaximumRowCount(12);
		
		comboBoxTranspose = new JComboBox(TransformationProvider.TRANSPOSE_LABEL);
		sl_panel.putConstraint(SpringLayout.WEST, comboBoxTranspose, 6, SpringLayout.EAST, comboQuantize);
		comboBoxTranspose.setToolTipText("Transpose semitones");
		sl_panel.putConstraint(SpringLayout.VERTICAL_CENTER, comboBoxTranspose, 0, SpringLayout.VERTICAL_CENTER, panelFooter);
		panelFooter.add(comboBoxTranspose);
		comboBoxTranspose.setMaximumRowCount(27);
		comboBoxTranspose.setSelectedIndex(13);
		
		JButton btnClear = new JButton("Clear");
		btnClear.setToolTipText("Clear loop");
		sl_panel.putConstraint(SpringLayout.VERTICAL_CENTER, btnClear, 0, SpringLayout.VERTICAL_CENTER, panelFooter);
		panelFooter.add(btnClear);
		
		JButton buttonNewLoop = new JButton("+");
		sl_panel.putConstraint(SpringLayout.EAST, buttonNewLoop, -6, SpringLayout.EAST, panelFooter);
		buttonNewLoop.setToolTipText("Create new loop window");
		sl_panel.putConstraint(SpringLayout.EAST, btnClear, -6, SpringLayout.WEST, buttonNewLoop);
		sl_panel.putConstraint(SpringLayout.VERTICAL_CENTER, buttonNewLoop, 0, SpringLayout.VERTICAL_CENTER, panelFooter);
		buttonNewLoop.setAlignmentX(Component.RIGHT_ALIGNMENT);
		panelFooter.add(buttonNewLoop);
		
		toggleDrumsLayout = new JCheckBox(VIEW_LABELS[0]);
		toggleDrumsLayout.setPreferredSize(new Dimension(100, 23));
		sl_panel.putConstraint(SpringLayout.WEST, slider, 0, SpringLayout.EAST, toggleDrumsLayout);
		sl_panel.putConstraint(SpringLayout.WEST, toggleDrumsLayout, 6, SpringLayout.WEST, panelFooter);
		panelFooter.add(toggleDrumsLayout);
		toggleDrumsLayout.setOpaque(false);
		toggleDrumsLayout.setBorderPainted(false);
		toggleDrumsLayout.setToolTipText("Toggle notes/drums view");
		toggleDrumsLayout.setIcon(new ImageIcon(Res.IMAGE_TOGGLE_OFF()));
		toggleDrumsLayout.setSelectedIcon(new ImageIcon(Res.IMAGE_TOGGLE_ON()));
		sl_panel.putConstraint(SpringLayout.VERTICAL_CENTER, toggleDrumsLayout, 0, SpringLayout.VERTICAL_CENTER, panelFooter);
		
		toggleDrumsLayout.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loop.setDrums(toggleDrumsLayout.isSelected());
				toggleDrumsLayout.setText(VIEW_LABELS[loop.isDrums()?1:0]);
			}
		});
		
		buttonNewLoop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loop.setRecordOn(false);
				Loop.createLoop();
			}
		});
		
		btnClear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loop.clearPattern();
			}
		});
		 ((JLabel)comboBoxTranspose.getRenderer()).setHorizontalAlignment(JLabel.CENTER);
		
		comboBoxTranspose.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				loop.setTransposeIndex(comboBoxTranspose.getSelectedIndex());
				loop.triggerRefreshLoopDisplay();
			}});
		
		comboQuantize.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loop.setQuantizationIndex(comboQuantize.getSelectedIndex());
				loop.triggerRefreshLoopDisplay();
			}});
		
		comboMidiIn = new JComboBox(MIDI_CHANNELS_IN);
		comboMidiIn.setToolTipText("MIDI Channel In");
		comboMidiIn.setMaximumRowCount(16);
		
		comboMidiIn.setSelectedIndex(loop.getMidiChannelIn());
		
		comboMidiOut = new JComboBox(MIDI_CHANNELS_OUT);
		comboMidiOut.setToolTipText("MIDI Channel Out");
		comboMidiOut.setMaximumRowCount(16);
		comboMidiOut.setSelectedIndex(loop.getMidiChannelOut());
		SpringLayout sl_panelMidi = new SpringLayout();
		sl_panelMidi.putConstraint(SpringLayout.EAST, comboMidiOut, -6, SpringLayout.EAST, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.EAST, comboMidiIn, -6, SpringLayout.WEST, comboMidiOut);
		sl_panelMidi.putConstraint(SpringLayout.VERTICAL_CENTER, comboMidiOut, 0, SpringLayout.VERTICAL_CENTER, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.VERTICAL_CENTER, comboMidiIn, 0, SpringLayout.VERTICAL_CENTER, panelMidi);
		panelMidi.setLayout(sl_panelMidi);
		
		toggleMetronome = new JCheckBox("Metronome");
		toggleMetronome.setIcon(new ImageIcon(Res.IMAGE_TOGGLE_OFF()));
		toggleMetronome.setSelectedIcon(new ImageIcon(Res.IMAGE_TOGGLE_ON()));
		toggleMetronome.setHorizontalTextPosition(SwingConstants.LEADING);
		toggleMetronome.setOpaque(false);
		toggleMetronome.setToolTipText("Turn on metronome");
		sl_panelMidi.putConstraint(SpringLayout.VERTICAL_CENTER, toggleMetronome, 0, SpringLayout.VERTICAL_CENTER, panelMidi);
		toggleMetronome.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loop.setMetronomeEnabled(toggleMetronome.isSelected());
			}
		});
		panelMidi.add(toggleMetronome);
		
		toggleRecord = new JCheckBox();
		sl_panelMidi.putConstraint(SpringLayout.EAST, toggleMetronome, -6, SpringLayout.WEST, toggleRecord);
		toggleRecord.setHorizontalTextPosition(SwingConstants.LEADING);
		toggleRecord.setOpaque(false);
		sl_panelMidi.putConstraint(SpringLayout.EAST, toggleRecord, -6, SpringLayout.WEST, comboMidiIn);
		toggleRecord.setBorderPainted(false);
		sl_panelMidi.putConstraint(SpringLayout.NORTH, toggleRecord, -4, SpringLayout.NORTH, toggleMetronome);
		toggleRecord.setText("Record");
		toggleRecord.setIcon(new ImageIcon(Res.IMAGE_TOGGLE_OFF()));
		toggleRecord.setSelectedIcon(new ImageIcon(Res.IMAGE_TOGGLE_ON_EXTRA()));
		sl_panelMidi.putConstraint(SpringLayout.VERTICAL_CENTER, toggleRecord, 0, SpringLayout.VERTICAL_CENTER, panelMidi);
		toggleRecord.setSelected(true);
		toggleRecord.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				loop.setRecordOn(toggleRecord.isSelected());
			}
		});
		toggleRecord.setToolTipText("Record notes from selected channel");
		panelMidi.add(toggleRecord);
		panelMidi.add(comboMidiIn);
		panelMidi.add(comboMidiOut);
		
		lblDimidimiLooper = new JLabel("dimidimi");
		sl_panelMidi.putConstraint(SpringLayout.WEST, lblDimidimiLooper, 6, SpringLayout.WEST, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.VERTICAL_CENTER, lblDimidimiLooper, 0, SpringLayout.VERTICAL_CENTER, panelMidi);
		panelMidi.add(lblDimidimiLooper);
		lblDimidimiLooper.setFont(lblDimidimiLooper.getFont().deriveFont(lblDimidimiLooper.getFont().getStyle() | Font.BOLD, lblDimidimiLooper.getFont().getSize() + 3f));
		lblDimidimiLooper.setIcon(new ImageIcon(Res.IMAGE_ICON_32x32()));
		
		chckbxMute = new JCheckBox("Mute");
		chckbxMute.setToolTipText("Mute this loop");
		chckbxMute.setIcon(new ImageIcon(Res.IMAGE_CHECK_OFF()));
		chckbxMute.setSelectedIcon(new ImageIcon(Res.IMAGE_CHECK_ON()));
		chckbxMute.setOpaque(false);
		sl_panelMidi.putConstraint(SpringLayout.VERTICAL_CENTER, chckbxMute, 0, SpringLayout.VERTICAL_CENTER, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.WEST, chckbxMute, 6, SpringLayout.EAST, lblDimidimiLooper);
		panelMidi.add(chckbxMute);
		chckbxMute.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loop.setMuted(chckbxMute.isSelected());
			}
		});
		
		chckbxSolo = new JCheckBox("Solo");
		chckbxSolo.setToolTipText("Play this loop solo");
		chckbxSolo.setIcon(new ImageIcon(Res.IMAGE_CHECK_OFF()));
		chckbxSolo.setSelectedIcon(new ImageIcon(Res.IMAGE_CHECK_ON()));
		chckbxSolo.setOpaque(false);
		sl_panelMidi.putConstraint(SpringLayout.VERTICAL_CENTER, chckbxSolo, 0, SpringLayout.VERTICAL_CENTER, panelMidi);
		sl_panelMidi.putConstraint(SpringLayout.WEST, chckbxSolo, 6, SpringLayout.EAST, chckbxMute);
		panelMidi.add(chckbxSolo);
		chckbxSolo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loop.setSolo(chckbxSolo.isSelected());
			}
		});
		panelLoop.setLayout(new BorderLayout(0, 0));
		loopDisplayPanel = new LoopDisplayPanel(loop);
		loopDisplayPanel.setBackground(Color.WHITE);
		panelLoop.add(loopDisplayPanel);
		loopDisplayPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		loopDisplayPanel.requestFocusInWindow();
		
		frmDimidimi.getContentPane().setLayout(groupLayout);
		
		
		comboMidiIn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int midiIn = comboMidiIn.getSelectedIndex();
				loop.setMidiChannelIn(midiIn);
			}});
		comboMidiOut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int midiOut = comboMidiOut.getSelectedIndex();
				loop.setMidiChannelOut(midiOut);
			}});
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void setIcon(JFrame frame) {
		frame.setIconImage(Toolkit.getDefaultToolkit().getImage(Res.IMAGE_ICON_LARGE()));
		try {
			// set icon for mac os if possible
			Class c = Class.forName("com.apple.eawt.Application");
			Object app = c.getDeclaredMethod ("getApplication", (Class[])null).invoke(null, (Object[])null);
			Method setDockIconImage = c.getDeclaredMethod("setDockIconImage", Image.class);
			setDockIconImage.invoke(app, new ImageIcon(Res.IMAGE_ICON_LARGE()).getImage());
		} catch (Exception e) {
			// fail silently
		}
	}
	
	private JMenuBar buildMenu() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("File");
		JMenuItem  menuItem;
		JMenu recentSub;
		
		menuItem = new JMenuItem("Open Session...");
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
		recentSub = new JMenu("Recent Sessions");
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
		menuItem = new JMenuItem("Save Session as...");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File selectedFile = GUIUtils.saveDialog("Save Session", GUIUtils.FILE_FILTER_SESSION, Prefs.FILE_SESSION_LAST_USED_NAME);
				if (selectedFile!=null) {
					try {
						Loop.saveLoop(selectedFile);
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
		
		
		menu.addSeparator();
		menuItem = new JMenuItem("New Loop");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loop.setRecordOn(false);
				onSettingsUpdated();
				Loop.createLoop();
			}
		});
		menu.add(menuItem);
		
		menuItem = new JMenuItem("Open Loop...");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File selectedFile = GUIUtils.loadDialog("Open Loop", GUIUtils.FILE_FILTER_LOOP, Prefs.FILE_LOOP_LAST_USED_NAME);
		        if (selectedFile!=null) {
		        	String s = GUIUtils.loadLoop(selectedFile, loop, frmDimidimi);
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
					String s = GUIUtils.loadLoop(new File(filename), loop, frmDimidimi);
		        	if (s!=null) {
		        		titleExtension = s;
		        		frmDimidimi.setTitle(getWindowTitle());
		        	}
				}
			}
		}));
		menu.add(recentSub);
		menu.addSeparator();
		menuItem = new JMenuItem("Save Loop...");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File selectedFile = GUIUtils.saveDialog("Save Loop", GUIUtils.FILE_FILTER_LOOP, Prefs.FILE_LOOP_LAST_USED_NAME);
		        if (selectedFile!=null) {
		        	try {
		        		loop.saveToFile(selectedFile);
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
		
		menuItem = new JMenuItem("Close Loop");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				closeWindow();
			}
		});
		menu.add(menuItem);
		
		menu.addSeparator();
		menuItem = new JMenuItem("Exit");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Loop.removeAllLoops();
			}
		});
		menu.add(menuItem);
		menuBar.add(menu);

		
		menu = new JMenu("Edit");
		
		menuItem = new JMenuItem("Select All");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loopDisplayPanel.selectAll();
			}
		});
		menu.add(menuItem);
		menu.addSeparator();
		
		menuItem = new JMenuItem("Clear");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loop.clearPattern();
			}
		});
		menu.add(menuItem);
		menu.addSeparator();
		
		menuItem = new JMenuItem("Clear Mod Wheel");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loop.clearModWheel();
			}
		});
		menu.add(menuItem);
		
		menuItem = new JMenuItem("Clear Pitch Bend");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loop.clearPitchBend();
			}
		});
		menu.add(menuItem);
		menu.addSeparator();
		
		menuItem = new JMenuItem("Duplicate Loop");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loop.doublePattern();
			}
		});
		menu.add(menuItem);
		menu.addSeparator();
		
		menuItem = new JMenuItem("Half Speed");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loop.halfSpeed();
			}
		});
		menu.add(menuItem);
		
		menuItem = new JMenuItem("Double Speed");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loop.doubleSpeed();
			}
		});
		menu.add(menuItem);
		menu.addSeparator();
		
		menuItem = new JMenuItem("Apply quantisation");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (Note note: loop.getNotesList()) { // TODO move to loop class
					note.setPosStart(note.getPosStart(loop));
					note.setPosEnd(note.getPosEnd(loop));
				}
				loop.triggerRefreshLoopDisplay();
			}
		});
		menu.add(menuItem);
		
		menuItem = new JMenuItem("Apply transposition");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (Note note: loop.getNotesList()) {  // TODO move to loop class
					note.setNoteNumber(note.getNoteNumber(loop));
				}
				loop.triggerRefreshLoopDisplay();
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
				loop.triggerRefreshLoopDisplay();
			}
		});
		menu.add(menuItem);
		menu.addSeparator();
		menuItem = new JMenuItem("Panic (All Notes Off)");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				MidiHandler.instance().sendAllNotesOffMidi(loop, true);
			}
		});
		menu.add(menuItem);
		menuBar.add(menu);

		
		menu = new JMenu("Window");
		menuItem = new JMenuItem("Arrange Windows");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.SHIFT_DOWN_MASK+Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LoopWindow.arrangeLoopWindows();
			}
		});
		menu.add(menuItem);
		menu.addSeparator();
		
		menuItem = new JMenuItem("Show Controller");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DiMIDImi.getControllerWindow().setVisible(true);
			}
		});
		menu.add(menuItem);
		menu.addSeparator();
		
		menuItemAnimate = new JCheckBoxMenuItem("Animated Notes");
		menuItemAnimate.setSelected(Prefs.get(Prefs.ANIMATE, 0)==1);
		menuItemAnimate.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
					Prefs.put(Prefs.ANIMATE, menuItemAnimate.isSelected()?1:0);
					Loop.updateSettingsOnAllLoops();
			}
		});
		menu.add(menuItemAnimate);
		menu.addSeparator();
		
		ButtonGroup group = new ButtonGroup();
		menuItemThemeDark = new JRadioButtonMenuItem("Dark Theme");
		group.add(menuItemThemeDark);
		menuItemThemeDark.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				Theme selectedTheme = menuItemThemeDark.isSelected()?Theme.DARK:Theme.BRIGHT;
				if (selectedTheme!=Theme.APPLY) {
					Theme.APPLY = selectedTheme;
					Prefs.put(Prefs.THEME, menuItemThemeDark.isSelected()?1:0);
					Loop.updateSettingsOnAllLoops();
				}
			}
		});
		menu.add(menuItemThemeDark);
		menuItemThemeBright = new JRadioButtonMenuItem("Bright Theme");
		group.add(menuItemThemeBright);
		menu.add(menuItemThemeBright);
		
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
	public void onSettingsUpdated() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (Prefs.get(Prefs.THEME, 0)==0) {
					Theme.APPLY = Theme.BRIGHT;
					menuItemThemeDark.setSelected(false);
					menuItemThemeBright.setSelected(true);
				}
				else {
					Theme.APPLY = Theme.DARK;
					menuItemThemeDark.setSelected(true);
					menuItemThemeBright.setSelected(false);
				}
				frmDimidimi.getContentPane().setBackground(Theme.APPLY.colorBackground());
				panelFooter.setBackground(Theme.APPLY.colorBackground());
				panelLoop.setBackground(Theme.APPLY.colorBackground());
				panelLoop.setBorder(new LineBorder(Theme.APPLY.colorBackground(), 3));
				labelLength.setForeground(Theme.APPLY.colorForeground());
				toggleDrumsLayout.setForeground(Theme.APPLY.colorForeground());
				comboQuantize.setSelectedIndex(loop.getQuantizationIndex());
				comboBoxTranspose.setSelectedIndex(loop.getTransposeIndex());
				slider.setValue(loop.getLengthQuarters());
				labelLength.setText(""+loop.getLengthQuarters());
				toggleRecord.setSelected(loop.isRecordOn());
				comboMidiOut.setSelectedIndex(loop.getMidiChannelOut());
				comboMidiIn.setSelectedIndex(loop.getMidiChannelIn());
				chckbxMute.setSelected(loop.isMuted());
				chckbxSolo.setSelected(loop.isSolo());
				panelMidi.setBackground(Theme.isBright()?loop.getNoteColorPlayed():loop.getNoteColor(false));
				toggleDrumsLayout.setSelected(loop.isDrums());
				toggleDrumsLayout.setText(VIEW_LABELS[loop.isDrums()?1:0]);
				if (loop.getName()!=null && titleExtension==null) { 
					titleExtension = loop.getName();
				}
				frmDimidimi.setTitle(getWindowTitle());
				boolean animationOn = Prefs.get(Prefs.ANIMATE, 0)==1;
				menuItemAnimate.setSelected(animationOn);
				loopDisplayPanel.setAnimate(animationOn);
			}
		});
	}
	
	@Override
	public void onClock(int pos) {
		loopDisplayPanel.updateLoopPosition(pos);
	}

	@Override
	public void onActivityChange(boolean active, int pos) {
	}

	@Override
	public void onNoteOn(int noteNumber, int velocity, int pos) {
	}

	@Override
	public void onNoteOff(int notenumber, int pos) {
	}

	@Override
	public void onReceiveCC(int cc, int val, int pos) {
	}

	@Override
	public void onReceivePitchBend(int val, int pos) {
	}

	@Override
	public void onStateChange(boolean mute, boolean solo, QueuedState queuedMute, QueuedState queuedSolo) {	
		panelMidi.setBackground(loop.getColorBackground());
		chckbxMute.setSelected(loop.isMuted());
		chckbxSolo.setSelected(loop.isSolo());
	}
	
	private static final Dictionary<Integer, JLabel> LENGTH_LABELS = new Hashtable<Integer, JLabel>();
	private JSlider slider;
	private JLabel labelLength;
	private JPanel panelFooter;
	private JCheckBox chckbxMute;
	private JCheckBox chckbxSolo;
	
	static {
		LENGTH_LABELS.put(0, new JLabel("Len"));
        LENGTH_LABELS.put(8, new JLabel("8"));
        LENGTH_LABELS.put(16, new JLabel("16"));
        LENGTH_LABELS.put(24, new JLabel("24"));
        LENGTH_LABELS.put(32, new JLabel("32"));
	}

	@Override
	public void onFocusLoop(Loop loop) {
		if (loop==this.loop) {
			frmDimidimi.requestFocus();
		}
	}

	public static void arrangeLoopWindows() {
		Rectangle rect = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int width = rect.width;
		int height = rect.height;
		
		LOG.info("Arranging windows in {}x{}", width, height);		
		int minwidth = WINDOW_MAX_WIDTH-40;
		int minheight = WINDOW_MAX_HEIGHT;
		int maxcols = width / minwidth;
		int maxrows = height / minheight;
		int numcols = maxcols;
		int numrows = maxrows;
		int number = Loop.getLoops().size();
		switch(number) {
		case 1:
			// maximize single window
			numcols = 1;
			numrows = 1;
			break;
		case 2:
			numcols = 1;
			numrows = Math.min(2, maxcols);
			break;
		case 3:
		case 4:
			numcols = Math.min(2, maxcols);
			numrows = Math.min(2, maxrows);
			break;
		case 5:
		case 6:
			numcols = Math.min(2, maxcols);
			numrows = Math.min(3, maxrows);
			break;
		}
		int wwidth = width/numcols;
		int wheight = height/numrows;
		int row = 0;
		int col = 0;
		int rowiteration = 0;
		for (Loop loop: Loop.getLoops()) {
			Rectangle pos = new Rectangle(rect.x+20*rowiteration+col*wwidth, rect.y+20*rowiteration+row*wheight, wwidth-20, wheight-20);
			loop.getWindow().setScreenPosition(pos);
			col = (col+1)%numcols;
			if (col==0) {
				row = (row+1)%numrows;
				if (row==0) {
					rowiteration++;
				}
			}
		}
	}
	
	private static final String[] VIEW_LABELS = new String[] {"Notes", "Drums"};
}
