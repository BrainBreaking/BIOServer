package com.bbs.neuro.fingers;

import java.util.List;

import com.neurotec.biometrics.NFExtractor;
import com.neurotec.biometrics.NMatcher;
import com.neurotec.cluster.TaskParameter;

public final class FingerTools {
	// ===========================================================
	// Private static fields
	// ===========================================================
	private static FingerTools instance = null;

	// ===========================================================
	// Private fields
	// ===========================================================
	private NFExtractor extractor = null;

	// ===========================================================
	// Private constructor
	// ===========================================================
	private FingerTools() {
		extractor = new NFExtractor();
		updateExtractor();
	}

	// ===========================================================
	// Public static methods
	// ===========================================================
	public static FingerTools getInstance() {
		if (instance == null) {
			instance = new FingerTools();
		}
		return instance;
	}

	// ===========================================================
	// Public methods
	// ===========================================================
	public NFExtractor getExtractor() {
		return extractor;
	}

	public void updateExtractor() {
		FingersSettings settings = FingersSettings.getInstance();
		extractor.setMode(settings.getExtractorMode());
		extractor.setTemplateSize(settings.getExtractorTemplateSize());
		extractor.setReturnedImage(settings.getExtractorReturnedImage());
		extractor.setExtractedRidgeCounts(settings.getExtractorRidgeCounts());
		extractor.setUseQuality(settings.isExtractorUseQuality());
		extractor.setQualityThreshold(settings.getExtractorQualityThreshold());
		if (settings.isExtractorUseMinimalMinutiaeCount()) {
			extractor.setMinMinutiaCount(settings.getExtractorMinimalMinutiaeCount());
		} else {
			extractor.setMinMinutiaCount(0);
		}
	}

	public static void addRemoteMatcherParametersFromApplicationSettings(List<TaskParameter> parameters) {
		FingersSettings settings = FingersSettings.getDefaultInstance();
		parameters.add(new TaskParameter(NMatcher.PART_NONE, NMatcher.PARAMETER_FINGERS_MATCHING_SPEED, settings.getMatcherSpeed().getValue()));
		parameters.add(new TaskParameter(NMatcher.PART_NONE, NMatcher.PARAMETER_FINGERS_MODE, settings.getMatcherMode()));
		parameters.add(new TaskParameter(NMatcher.PART_NONE, NMatcher.PARAMETER_FINGERS_MAXIMAL_ROTATION, settings.getMatcherMaximalRotation()));
		parameters.add(new TaskParameter(NMatcher.PART_NONE, NMatcher.PARAMETER_FINGERS_MIN_MATCHED_COUNT, settings.getMatcherMinimalMatchedFingerCount()));
		parameters.add(new TaskParameter(NMatcher.PART_NONE, NMatcher.PARAMETER_FINGERS_MIN_MATCHED_COUNT_THRESHOLD, settings.getMatcherMinimalMatchedFingerThreshold()));
	}
}
