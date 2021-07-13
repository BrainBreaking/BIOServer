package com.bbs.neuro.util;


import java.io.File;

public class SettingsUtils {
	// ===========================================================
	// Constants
	// ===========================================================
	public static final String PROJECT_NAME = "bbs";

	// ===========================================================
	// Private fields
	// ===========================================================
	private static String pathname = Utils.getHomeDirectory() + Utils.FILE_SEPARATOR + ".neurotec" + Utils.FILE_SEPARATOR + PROJECT_NAME;

	public static String getSettingsFolder() {
		File settingsFolder = new File(pathname);
		if (!settingsFolder.exists()) {
			settingsFolder.mkdirs();
		}
		return settingsFolder.getAbsolutePath();
	}

}
