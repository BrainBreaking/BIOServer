package com.bbs.neuro.fingers;

import java.awt.Image;
import java.awt.event.ItemListener;

import javax.swing.event.ChangeListener;

import com.neurotec.biometrics.NTemplate;
import com.neurotec.swing.NView;


public interface BiometricRecord extends Cloneable {

	boolean isEnrolled();

	Image getImage();
	void setImage(Image image);

	void clear();

	NView createView();
	void addToTemplate(NTemplate template);

	void addChangeListener(ChangeListener listener);
	void removeChangeListener(ChangeListener listener);

	void addItemListener(ItemListener l);
	void removeItemListener(ItemListener l);

	Object clone() throws CloneNotSupportedException;
}
