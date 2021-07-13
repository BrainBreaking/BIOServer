package com.bbs.neuro.util;

import java.util.List;
import java.util.ArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.neurotec.biometrics.NFPosition;
import com.neurotec.biometrics.NFRecord;

public final class MultiFingerRecord extends FingerRecord implements ChangeListener {

	// ===========================================================
	// Private fields
	// ===========================================================
	private List<FingerRecord> segmented;

	// ===========================================================
	// Properties
	// ===========================================================


	// ===========================================================
	// Public constructors
	// ===========================================================

	public MultiFingerRecord(NFPosition position) {
		super(position, false);
		segmented = new ArrayList<FingerRecord>();
		NFPosition[] pos;
		if (position.equals(NFPosition.PLAIN_LEFT_FOUR_FINGERS)) {
			pos = new NFPosition[] {NFPosition.LEFT_LITTLE_FINGER, NFPosition.LEFT_RING_FINGER, NFPosition.LEFT_MIDDLE_FINGER, NFPosition.LEFT_INDEX_FINGER};
		} else if (position.equals(NFPosition.PLAIN_RIGHT_FOUR_FINGERS)) {
			pos = new NFPosition[] {NFPosition.RIGHT_INDEX_FINGER, NFPosition.RIGHT_MIDDLE_FINGER, NFPosition.RIGHT_RING_FINGER, NFPosition.RIGHT_LITTLE_FINGER};
		} else if (position.equals(NFPosition.PLAIN_THUMBS)) {
			pos = new NFPosition[] {NFPosition.LEFT_THUMB, NFPosition.RIGHT_THUMB};
		} else {
			pos = null;
		}

		if (pos != null) {
			for (NFPosition item : pos)	{
				FingerRecord finger = new FingerRecord(item);
				finger.addChangeListener(this);
				segmented.add(finger);
			}
		}
	}

	// ===========================================================
	// Public methods
	// ===========================================================
	public void stateChanged(ChangeEvent e) {
		for (FingerRecord item : segmented)	{
			if (!item.isMissing()) {
				super.setMissing(false);
				return;
			}
		}
		super.setMissing(true);
	}

	public void setSegmented(List<FingerRecord> segmented) {
		this.segmented = segmented;
	}

	public List<FingerRecord> getSegmented() {
		return segmented;
	}

	public NFPosition[] getMissingPositions() {
		List<NFPosition> list = new ArrayList<NFPosition>();
		for (FingerRecord item : segmented) {
			if (item.isMissing())
				list.add(item.getPosition());
		}
		return list.size() > 0 ? (NFPosition[]) list.toArray(new NFPosition[list.size()]) : null;
	}

	@Override
	public boolean isEnrolled() {
		if (segmented == null || segmented.size() == 0) return super.isEnrolled();
		for (FingerRecord item : segmented) {
			if (!item.isMissing() && !item.isEnrolled())
				return false;
		}
		return true;
	}

	@Override
	public void setRecord(NFRecord record) {
		if (getRecord() != record) {
			if (record == null)
				if (segmented != null)
					for (FingerRecord item : segmented) {
						item.setRecord(null);
					}
			super.setRecord(record);
		}
	}

	@Override
	public void clear() {
		segmented.clear();
		super.clear();
	}

}
