package de.privatepublic.midiutils.ui;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.GridLayout;
import javax.swing.JButton;
import java.awt.FlowLayout;

public class ControllerWindow extends JFrame {

	private JPanel contentPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ControllerWindow frame = new ControllerWindow();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public ControllerWindow() {
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JButton btnMute = new JButton("Mute");
		contentPane.add(btnMute);
		
		JButton btnMute_1 = new JButton("↩ Mute");
		contentPane.add(btnMute_1);
		
		JButton btnSolo = new JButton("Solo");
		contentPane.add(btnSolo);
		
		JButton btnSolo_1 = new JButton("↩ Solo");
		contentPane.add(btnSolo_1);
	}

}
