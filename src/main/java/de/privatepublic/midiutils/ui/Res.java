package de.privatepublic.midiutils.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.RoundRectangle2D;
import java.io.InputStream;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.JToggleButton;

public class Res {

	private final static String IMAGE_CHECK_OFF = "3state-off.png";
	private final static String IMAGE_CHECK_ON = "3state-on.png";
	private final static String IMAGE_CHECK_ON_FUTURE = "3state-on-next.png";
	private final static String IMAGE_CHECK_OFF_FUTURE = "3state-off-next.png";
	private final static String IMAGE_CHECK_ON_CHECKMARK = "3state-checked.png";
	
	private final static String IMAGE_CURSOR_EDIT = "crs-pencil.png";
	
	private final static String IMAGE_TOGGLE_OFF = "toggle-off.png";
	private final static String IMAGE_TOGGLE_ON = "toggle-on-black.png";
	private final static String IMAGE_TOGGLE_ON_EXTRA = "toggle-on-red.png";
	
	private final static String IMAGE_DROPDOWN = "dropdown.png";
	private final static String IMAGE_ROUND_S = "round-s.png";
	
	private final static String IMAGE_ICON_32x32 = "icon-32.png";
	private final static String IMAGE_ICON_LARGE = "icon.png";
	
	private final static String PROPS_THEME_BRIGHT = "theme_bright.properties";
	private final static String PROPS_THEME_DARK = "theme_dark.properties";
	
	private static URL getResource(String name) {
		return Res.class.getResource("/"+name);
	}
	
	private static InputStream getResourceStream(String name) {
		return Res.class.getResourceAsStream("/"+name);
	}

	public static URL IMAGE_CHECK_OFF() {
		return getResource(IMAGE_CHECK_OFF);
	}

	public static URL IMAGE_CHECK_ON() {
		return getResource(IMAGE_CHECK_ON);
	}

	public static URL IMAGE_CHECK_ON_FUTURE() {
		return getResource(IMAGE_CHECK_ON_FUTURE);
	}

	public static URL IMAGE_CHECK_OFF_FUTURE() {
		return getResource(IMAGE_CHECK_OFF_FUTURE);
	}

	public static URL IMAGE_CHECK_ON_CHECKMARK() {
		return getResource(IMAGE_CHECK_ON_CHECKMARK);
	}

	public static URL IMAGE_CURSOR_EDIT() {
		return getResource(IMAGE_CURSOR_EDIT);
	}

	public static URL IMAGE_TOGGLE_OFF() {
		return getResource(IMAGE_TOGGLE_OFF);
	}

	public static URL IMAGE_TOGGLE_ON() {
		return getResource(IMAGE_TOGGLE_ON);
	}

	public static URL IMAGE_TOGGLE_ON_EXTRA() {
		return getResource(IMAGE_TOGGLE_ON_EXTRA);
	}

	public static URL IMAGE_ICON_32x32() {
		return getResource(IMAGE_ICON_32x32);
	}

	public static URL IMAGE_ICON_LARGE() {
		return getResource(IMAGE_ICON_LARGE);
	}
	
	public static URL IMAGE_DROPDOWN() {
		return getResource(IMAGE_DROPDOWN);
	}
	
	public static URL IMAGE_ROUND_S() {
		return getResource(IMAGE_ROUND_S);
	}
	
	public static InputStream PROPS_THEME_BRIGHT() {
		return getResourceStream(PROPS_THEME_BRIGHT);
	}

	public static InputStream PROPS_THEME_DARK() {
		return getResourceStream(PROPS_THEME_DARK);
	}
	
	public static final Icon TOGGLE_ICON = new ToggleIcon();
	public static final Icon TOGGLE_ICON_EXTRA = new ToggleIcon(Color.RED);
	
	
	public static class CheckIcon implements Icon {
		
		public static enum Type { ON, OFF, FUTURE_ON, FUTURE_OFF, CHECKED };
		
		private static final Stroke LINE_STROKE = new BasicStroke(2);
		private static final Color COLOR_CHECK = Color.decode("#0096ff");
		
		private Type type;
		
		public CheckIcon(Type type) {
			this.type = type;
		}

		@Override
		public void paintIcon(Component c, Graphics gr, int x, int y) {
			Graphics2D g = (Graphics2D) gr;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			switch(type) {
			case ON:
				g.setColor(Color.WHITE);
				g.fillOval(x, y, 18, 18);
				g.setColor(Color.BLACK);
				g.fillOval(x+1, y+1, 18-2, 18-2);
				break;
			case FUTURE_ON:
				g.setColor(Color.WHITE);
				g.fillOval(x, y, 18, 18);
				g.setColor(Color.BLACK);
				g.fillOval(x+3, y+3, 12, 12);
				break;
			case FUTURE_OFF:
				g.setColor(Color.WHITE);
				g.fillOval(x, y, 18, 18);
				g.setColor(Color.DARK_GRAY);
				g.setStroke(LINE_STROKE);
				g.drawLine(x+5, y+5, x+18-5, y+18-5);
				g.drawLine(x+5, y+18-5, x+18-5, y+5);
				break;
			case OFF:
				g.setColor(Color.WHITE);
				g.fillOval(x, y, 18, 18);
				break;
			default:
				g.setColor(Color.WHITE);
				g.fillOval(x, y, 18, 18);
				g.setColor(COLOR_CHECK);
				g.fillOval(x+1, y+1, 18-2, 18-2);
			}
		}

		@Override
		public int getIconWidth() {
			return 18;
		}

		@Override
		public int getIconHeight() {
			return 18;
		}
		
	}
	
	
	
	
	
	private static class ToggleIcon implements Icon {
		
		final Color bgCol = Color.decode("#cbcbcb");
		Color bgSelectedCol = Color.decode("#5e5e5e");
		
		public ToggleIcon() {
			
		}
		
		public ToggleIcon(Color selectedBackground) {
			bgSelectedCol = selectedBackground;
		}
		
		@Override
		public void paintIcon(Component c, Graphics gr, int x, int y) {
			boolean selected = ((JToggleButton)c).isSelected();
			Graphics2D g = (Graphics2D) gr;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setColor(selected?bgSelectedCol:bgCol);
			g.fill(new RoundRectangle2D.Float(x+1, y+1, 40, 21, 20, 20));
			g.setColor(Color.WHITE);
			g.fillOval(x+(selected?21:4), y+3, 17, 17);
		}

		@Override
		public int getIconWidth() {
			return 42;
		}

		@Override
		public int getIconHeight() {
			return 23;
		}
		
	}
	
}
