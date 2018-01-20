package de.privatepublic.midiutils.ui;

import java.io.InputStream;
import java.net.URL;

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
	
}
