package com.bbs.neuro.fingers;

import java.awt.Image;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

public abstract class AbstractBiometricRecord implements BiometricRecord {

	// ===========================================================
	// Private fields
	// ===========================================================

	protected Image image;

	// ===========================================================
	// Properties
	// ===========================================================

	public static final String IMAGE_CHANGED_PROPERTY = "image";

	/**
	 * The event listener list for the biometric record.
	 */
	protected EventListenerList listenerList = new EventListenerList();

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
		fireStateChanged();
	}

	public void addChangeListener(ChangeListener listener) {
		listenerList.add(ChangeListener.class, listener);
	}

	public void removeChangeListener(ChangeListener listener) {
		listenerList.remove(ChangeListener.class, listener);
	}

	public void addItemListener(ItemListener listener) {
		listenerList.add(ItemListener.class, listener);

	}
	public void removeItemListener(ItemListener listener) {
		listenerList.remove(ItemListener.class, listener);
	}

	public void clear() {
		image = null;
		fireStateChanged();
		System.gc();
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
		}
		return null;
	}

	/**
	 * Notifies all listeners that have registered interest for
	 * notification on this event type. The event instance
	 * is lazily created using the parameters passed into
	 * the fire method.
	 *
	 * @see EventListenerList
	 */
	protected void fireStateChanged() {
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ChangeListener.class) {
				((ChangeListener) listeners[i + 1]).stateChanged(new ChangeEvent(this));
			}
		}
	}

	protected void fireItemStateChanged(ItemEvent e) {
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ItemListener.class) {
				((ItemListener) listeners[i + 1]).itemStateChanged(e);
			}
		}
	}
}
