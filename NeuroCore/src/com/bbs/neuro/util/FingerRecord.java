package com.bbs.neuro.util;

import com.bbs.neuro.fingers.AbstractBiometricRecord;
import com.neurotec.biometrics.NFPosition;
import com.neurotec.biometrics.NFRecord;
import com.neurotec.biometrics.NTemplate;
import com.neurotec.swing.NFView;

public class FingerRecord extends AbstractBiometricRecord {

	// ===========================================================
	// Private fields
	// ===========================================================

	private NFRecord record;
	private NFPosition position;
	private boolean palm = false;
	private boolean missing = false;

	// ===========================================================
	// Properties
	// ===========================================================

	public static final String RECORD_CHANGED_PROPERTY = "record";
	public static final String POSITION_CHANGED_PROPERTY = "position";
	public static final String MISSING_CHANGED_PROPERTY = "missing";
	public static final String PALM_CHANGED_PROPERTY = "palm";

	// ===========================================================
	// Public constructors
	// ===========================================================

	public FingerRecord(NFPosition position) {
		this(position, false);
	}

	public FingerRecord(NFPosition position, boolean palm) {
		this.position = position;
		this.palm = palm;
	}

	public FingerRecord(NFRecord record) {
		this(record, false);
	}

	public FingerRecord(NFRecord record, boolean palm) {
		this.record = record;
		this.position = record.getPosition();
		this.palm = palm;
	}

	// ===========================================================
	// Public methods
	// ===========================================================

	public NFRecord getRecord() {
		return record;
	}

	public void setRecord(NFRecord record) {
		this.record = record;
		fireStateChanged();
	}

	public NFPosition getPosition() {
		return position;
	}

	public void setPosition(NFPosition position) {
		this.position = position;
		fireStateChanged();
	}

	public boolean isPalm() {
		return palm;
	}

	public void setPalm(boolean isPalm) {
		this.palm = isPalm;
		fireStateChanged();
	}

	public boolean isMissing() {
		return missing;
	}

	public void setMissing(boolean missing) {
		if (missing) {
			record = null;
		}
		this.missing = missing;
		fireStateChanged();
	}

	public boolean isEnrolled() {
		return getImage() != null && record != null && !missing;
	}

	public NFView createView() {
		NFView view = new NFView();
		view.setImage(getImage());
		view.setTemplate(getRecord());
		return view;
	}

	@Override
	public void clear() {
		record = null;
		missing = false;
		super.clear();
	}


	public void addToTemplate(NTemplate template) {
		if (palm) {
			if (template.getPalms() == null) {
				template.addPalms();
			}
			template.getPalms().getRecords().addCopy(record);
		} else {
			if (template.getFingers() == null) {
				template.addFingers();
			}
			template.getFingers().getRecords().addCopy(record);
		}
	}

	@Override
	public String toString() {
		if (getImage() == null) {
			return String.format("Finger [%s] (from template)", getPosition());
		} else {
			return String.format("Finger [%s]", getPosition());
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((position == null) ? 0 : position.hashCode());
		result = prime * result + ((record == null) ? 0 : record.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FingerRecord other = (FingerRecord) obj;
		if (position == null) {
			if (other.position != null)
				return false;
		} else if (!position.equals(other.position))
			return false;
		if (record == null) {
			if (other.record != null)
				return false;
		} else if (!record.equals(other.record))
			return false;
		return true;
	}
}
