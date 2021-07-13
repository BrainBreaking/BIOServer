package com.bbs.neuro.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;

import com.neurotec.licensing.NLicense;

public class LicenseManager {

	// ===========================================================
	// Private fields
	// ===========================================================
	private static LicenseManager instance = null;
	private final PropertyChangeSupport propertyChangeSupport;
	private ArrayList<String> licenses;
	private int progress;
	private String address = "NeuroServer";
	private String port = "5000";
	private boolean debug = true;

	// ===========================================================
	// Public static fields
	// ===========================================================
	public static final String PROGRESS_CHANGED_PROPERTY = "progress";

	// ===========================================================
	// Private constructor
	// ===========================================================
	private LicenseManager() {
		propertyChangeSupport = new PropertyChangeSupport(this);
		licenses = new ArrayList<String>();
		licenses.add("Biometrics.FingerExtraction");
		//licenses.add("Biometrics.PalmExtraction");
		//licenses.add("Biometrics.FaceExtraction");
		//licenses.add("Biometrics.IrisExtraction");
		//licenses.add("Biometrics.VoiceExtraction");
		licenses.add("Biometrics.FingerMatchingFast");
		licenses.add("Biometrics.FingerMatching");
		//licenses.add("Biometrics.PalmMatchingFast");
		//licenses.add("Biometrics.PalmMatching");
		//licenses.add("Biometrics.VoiceMatching");
		//licenses.add("Biometrics.FaceMatchingFast");
		//licenses.add("Biometrics.FaceMatching");
		//licenses.add("Biometrics.IrisMatchingFast");
		//licenses.add("Biometrics.IrisMatching");
		licenses.add("Biometrics.FingerSegmentation");
		//licenses.add("Biometrics.PalmSegmentation");
		//licenses.add("Biometrics.FaceSegmentation");
		//licenses.add("Biometrics.IrisSegmentation");
		//licenses.add("Biometrics.VoiceSegmentation");

		licenses.add("Biometrics.Standards.FingerTemplates");
		licenses.add("Biometrics.Standards.Fingers");
		//licenses.add("Biometrics.Standards.Faces");
		//licenses.add("Biometrics.Standards.Irises");
	}

	// ===========================================================
	// Public static method
	// ===========================================================
	public static LicenseManager getInstance() {
		if (instance == null) {
			instance = new LicenseManager();
		}
		return instance;
	}

	// ===========================================================
	// Private methods
	// ===========================================================
	private void setProgress(int progress) {
		int oldProgress = getProgress();
		this.progress = progress;
		propertyChangeSupport.firePropertyChange(PROGRESS_CHANGED_PROPERTY, oldProgress, progress);
	}

	// ===========================================================
	// Public methods
	// ===========================================================
	public synchronized boolean obtain() throws Exception {
		return obtain(address, port);
	}
	public synchronized boolean obtain(String address, String port) throws Exception {
		if (debug) {
			System.out.format("Obtaining licenses from server %s:%s\n", address, port);
		}
		int i = 0;
		setProgress(i);
		boolean result = false;
		try {
			for (String license : licenses) {
				boolean state = false;
				try {
					state = NLicense.obtainComponents(address, port, license);
					result |= state;
				} finally {
					if (debug) {
						System.out.println(license + ": " + (state ? "obtainted" : "not obtained"));
					}
				}
				setProgress(++i);
			}
		} finally {
			setProgress(100);
		}
		return true;
	}

	public synchronized void release() {
		if (isProgress()) return;
		String components = licenses.toString().replace("[", "").replace("]", "").replace(" ", "");
		try {
			NLicense.releaseComponents(components);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isProgress() {
		return progress != 0 && progress != 100;
	}

	public int getProgress() {
		return progress;
	}

	public int getLicenseCount() {
		return licenses.size();
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}
}
