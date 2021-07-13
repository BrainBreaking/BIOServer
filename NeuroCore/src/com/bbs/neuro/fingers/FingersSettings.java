package com.bbs.neuro.fingers;

import com.bbs.neuro.util.SettingsUtils;
import com.bbs.neuro.util.Utils;
import java.io.File;

import org.simpleframework.xml.Default;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import com.neurotec.biometrics.NFEReturnedImage;
import com.neurotec.biometrics.NFETemplateSize;
import com.neurotec.biometrics.NFExtractor;
import com.neurotec.biometrics.NFRidgeCountsType;
import com.neurotec.biometrics.NMatchingSpeed;
import com.neurotec.biometrics.NMatcher;



@Default
public final class FingersSettings implements Cloneable {
	// ===========================================================
	// Private static fields
	// ===========================================================
	private static FingersSettings instance;
	private static FingersSettings defaultInstance;

	// ===========================================================
	// Private fields
	// ===========================================================
	@Element(required = false)
	private String filename;
	@Element(required = false)
	private String selectedFPScanners;
	@Element(required = false)
	private String lastOpenedPath;

	private int extractorMode;
	private int extractorQualityThreshold;
	private int extractorMinimalMinutiaeCount;
	private boolean extractorUseQuality;
	private boolean extractorUseMinimalMinutiaeCount;
	private NFETemplateSize extractorTemplateSize;
	private NFRidgeCountsType extractorRidgeCounts;
	private NFEReturnedImage extractorReturnedImage;

	private int matcherMode;
	private int matcherMaximalRotation;
	private int matcherMinimalMatchedFingerCount;
	private int matcherMinimalMatchedFingerThreshold;
	private NMatchingSpeed matcherSpeed;
        private int matchingThreshold;

	// ===========================================================
	// Public static methods
	// ===========================================================
	public static FingersSettings getDefaultInstance() {
		if (defaultInstance == null) {
			defaultInstance = new FingersSettings();
			defaultInstance.loadDefault();
		}
		return defaultInstance;
	}

	public static FingersSettings getInstance() {
		if (instance == null) {
			instance = new FingersSettings();
			instance.load();
		}
		return instance;
	}
    

	// ===========================================================
	// Private constructor
	// ===========================================================
	private FingersSettings() {
		filename = SettingsUtils.getSettingsFolder() + Utils.FILE_SEPARATOR + "fingerprints.xml";
	}

	// ===========================================================
	// Private methods
	// ===========================================================
	private void load() {
		File file = new File(filename);
		try {
			if (file.exists()) {
				Serializer serializer = new Persister();
				instance = serializer.read(FingersSettings.class, file);
			} else {
				instance = (FingersSettings) getDefaultInstance().clone();
			}
		} catch (Exception e) {
			try {
				instance = (FingersSettings) getDefaultInstance().clone();
			} catch (CloneNotSupportedException e1) {}
		}
	}

	private FingersSettings loadDefault() {
		setLastOpenedPath(Utils.getHomeDirectory());

		NFExtractor extractor = new NFExtractor();
		setExtractorMode(extractor.getMode());
		setExtractorTemplateSize(extractor.getTemplateSize());
		setExtractorReturnedImage(extractor.getReturnedImage());
		setExtractorRidgeCounts(extractor.getExtractedRidgeCounts());
		setExtractorUseQuality(extractor.isUseQuality());
		setExtractorQualityThreshold(extractor.getQualityThreshold());
		setExtractorMinimalMinutiaeCount(extractor.getMinMinutiaCount());
		setExtractorUseMinimalMinutiaeCount(extractor.getMinMinutiaCount() > 0);

		NMatcher matcher = new NMatcher();
		setMatcherSpeed(NMatchingSpeed.LOW);
		setMatcherMaximalRotation(45);
		setMatcherMinimalMatchedFingerCount(10);
		setMatcherMinimalMatchedFingerThreshold(20);
		setMatcherMode(0);
                setMatchingThreshold(18);
                save();
		return this;
	}

	// ===========================================================
	// Public methods
	// ===========================================================
	public void save() {
		Serializer serializer = new Persister();
		File file = new File(filename);
		try {
			serializer.write(this, file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getSelectedFPScanners() {
		return selectedFPScanners;
	}

	public void setSelectedFPScanners(String value) {
		this.selectedFPScanners = value;
	}

	public String getLastOpenedPath() {
		return lastOpenedPath;
	}

	public void setLastOpenedPath(String value) {
		this.lastOpenedPath = value;
	}

	public int getExtractorMode() {
		return extractorMode;
	}

	public void setExtractorMode(int value) {
		this.extractorMode = value;
	}

	public NFETemplateSize getExtractorTemplateSize() {
		return extractorTemplateSize;
	}

	public void setExtractorTemplateSize(NFETemplateSize value) {
		this.extractorTemplateSize = value;
	}

	public int getMatcherMaximalRotation() {
		return matcherMaximalRotation;
	}

	public void setMatcherMaximalRotation(int value) {
		this.matcherMaximalRotation = value;
	}

	public NFEReturnedImage getExtractorReturnedImage() {
		return extractorReturnedImage;
	}

	public void setExtractorReturnedImage(NFEReturnedImage value) {
		this.extractorReturnedImage = value;
	}

	public boolean isExtractorUseQuality() {
		return extractorUseQuality;
	}

	public void setExtractorUseQuality(boolean value) {
		this.extractorUseQuality = value;
	}

	public int getExtractorQualityThreshold() {
		return extractorQualityThreshold;
	}

	public void setExtractorQualityThreshold(int value) {
		this.extractorQualityThreshold = value;
	}

	public int getExtractorMinimalMinutiaeCount() {
		return extractorMinimalMinutiaeCount;
	}

	public void setExtractorMinimalMinutiaeCount(int value) {
		this.extractorMinimalMinutiaeCount = value;
	}

	public boolean isExtractorUseMinimalMinutiaeCount() {
		return extractorUseMinimalMinutiaeCount;
	}

	public void setExtractorUseMinimalMinutiaeCount(boolean value) {
		this.extractorUseMinimalMinutiaeCount = value;
	}

	public NMatchingSpeed getMatcherSpeed() {
		return matcherSpeed;
	}

	public void setMatcherSpeed(NMatchingSpeed value) {
		this.matcherSpeed = value;
	}

	public int getMatcherMode() {
		return matcherMode;
	}

	public void setMatcherMode(int value) {
		this.matcherMode = value;
	}

	public int getMatcherMinimalMatchedFingerCount() {
		return matcherMinimalMatchedFingerCount;
	}

	public void setMatcherMinimalMatchedFingerCount(int value) {
		this.matcherMinimalMatchedFingerCount = value;
	}

	public int getMatcherMinimalMatchedFingerThreshold() {
		return matcherMinimalMatchedFingerThreshold;
	}

	public void setMatcherMinimalMatchedFingerThreshold(int value) {
		this.matcherMinimalMatchedFingerThreshold = value;
	}

	public NFRidgeCountsType getExtractorRidgeCounts() {
		return extractorRidgeCounts;
	}

	public void setExtractorRidgeCounts(NFRidgeCountsType value) {
		this.extractorRidgeCounts = value;
	}

    public void setMatchingThreshold(int i) {
        this.matchingThreshold=i;
    }

    /**
     * @return the matchingThreshold
     */
    public int getMatchingThreshold() {
        return matchingThreshold;
    }
}
