package de.privatepublic.midiutils.ui;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

public class CheckboxList extends JList<JCheckBox> {
	private static final long serialVersionUID = -3733534128522415336L;
	protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

	public CheckboxList(String title) {
		setCellRenderer(new CellRenderer());

		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int index = locationToIndex(e.getPoint());

				if (index != -1) {
					JCheckBox checkbox = getModel().getElementAt(index);
					checkbox.setSelected(!checkbox.isSelected());
					repaint();
				}
			}
		});

		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setBorder(new TitledBorder(null, title, TitledBorder.LEADING, TitledBorder.TOP, null, null));
	}

	protected class CellRenderer implements ListCellRenderer<JCheckBox> {

		@Override
		public Component getListCellRendererComponent(
				JList<? extends JCheckBox> list, JCheckBox checkbox, int index,
				boolean isSelected, boolean cellHasFocus) {

			checkbox.setBackground(isSelected ? getSelectionBackground()
					: getBackground());
			checkbox.setForeground(isSelected ? getSelectionForeground()
					: getForeground());
			checkbox.setEnabled(isEnabled());
			checkbox.setFont(getFont());
			checkbox.setFocusPainted(false);
			checkbox.setBorderPainted(true);
			checkbox.setBorder(isSelected ? UIManager
					.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
			return checkbox;
		}

	}
}
