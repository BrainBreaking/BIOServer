package com.bbs.neuro.fingers;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.metal.MetalToolTipUI;

import com.neurotec.biometrics.NFPosition;

public final class HandSegmentSelector extends javax.swing.JPanel {

	// ===========================================================
	// Nested classes
	// ===========================================================

	public enum SelectionMode {
		NONE(0),
		FINGER(1),
		SLAPS_AND_TWO_THUMBS(2),
		SLAPS_AND_SEPARATE_THUMBS(3),
		PALMS(4);

		private int value;

		SelectionMode(int value) {
			this.value = value;
		}
	}

	final class Segment {
		private NFPosition position;
		private boolean selected = false;
		private boolean highlighted = false;
		private boolean hovered = false;
		private boolean missing = false;
		private boolean isLeftHand = false;
		private boolean isRightHand = false;
		private boolean isLeftAndRightHand = false;
		private boolean isSingle = false;

		public Segment(NFPosition nfPosition) {
			this.position = nfPosition;
			this.isLeftHand = NFPosition.isLeft(nfPosition);
			this.isRightHand = NFPosition.isRight(nfPosition);
			this.isLeftAndRightHand = NFPosition.isLeftAndRight(nfPosition);
			this.isSingle = NFPosition.isSingleFinger(position);
		}

		public NFPosition getPosition() {
			return position;
		}

		public boolean isLeftHand() {
			return isLeftHand;
		}

		public boolean isRightHand() {
			return isRightHand;
		}

		public boolean isLeftAndRightHand() {
			return isLeftAndRightHand;
		}

		public boolean isSingle() {
			return isSingle;
		}

		public boolean isHovered() {
			return hovered;
		}

		public void setHovered(boolean hover) {
			boolean oldHovered = isHovered();
			if (oldHovered != hover) {
				this.hovered = hover;
				hands.setPositionHoover(position, hover);
				fireStateChanged();
			}
		}

		public boolean isSelected() {
			return selected;
		}

		public void setSelected(boolean selected) {
			boolean oldSelected = isSelected();
			if (oldSelected != selected) {
				this.selected = selected;
				hands.setPositionSelected(position, selected);
				fireStateChanged();
			}
		}

		public boolean isHighlighted() {
			return highlighted;
		}

		public void setHighlighted(boolean highlighted) {
			boolean oldHighlighted = isHighlighted();
			if (oldHighlighted != highlighted) {
				this.highlighted = highlighted;
				hands.setPositionHighlighted(position, highlighted);
				fireStateChanged();
			}
		}

		public boolean isMissing() {
			return missing;
		}

		public void setMissing(boolean missing) {
			boolean oldMissing = isMissing();
			this.missing = missing;
			if (oldMissing != missing) {
				this.missing = missing;
				hands.setPositionMissing(position, missing);
				fireStateChanged();
			}
		}

		public void clear() {
			this.selected = false;
			this.highlighted = false;
			this.missing = false;
			this.hovered = false;
			hands.clearPosition(position);
			fireStateChanged();
		}

		@Override
		public String toString() {
			return "Segment [NFPosition=" + getPosition()
					+ ", Highlighted=" + isHighlighted()
					+ ", Missing=" + isMissing()
					+ ", Hovered="	+ isHovered()
					+ ", Selected=" + isSelected() + "]";
		}
	}

	class MultiLineToolTip extends JToolTip {
		public MultiLineToolTip() {
			Font f = new Font("Segoe UI", 0, 11);
			this.setFont(f);
			setUI(new MultiLineToolTipUI());
		}
	}

	class MultiLineToolTipUI extends MetalToolTipUI {
		private String[] strs;

		@Override
		public void paint(Graphics g, JComponent c) {
			if (strs == null) return;
			FontMetrics metrics = c.getFontMetrics(c.getFont());
			Dimension size = c.getSize();
			g.setColor(c.getBackground());
			g.fillRect(0, 0, size.width, size.height);
			g.setColor(c.getForeground());
			if (strs != null) {
				for (int i = 0; i < strs.length; i++) {
					if (strs[i] != null) {
						int height = (metrics.getHeight()) * (i + 1);
						g.drawString(strs[i], 3, height);
					}
				}
			}
		}

		@Override
		public Dimension getPreferredSize(JComponent c) {
			FontMetrics metrics = c.getFontMetrics(c.getFont());
			String tipText = ((JToolTip) c).getTipText();
			if (tipText == null) {
				strs = null;
				c.repaint();
				return new Dimension(0, 0);
			}
			BufferedReader br = new BufferedReader(new StringReader(tipText));
			String line;
			int maxWidth = 0;

			List<String> list = new ArrayList<String>();
			try {
				while ((line = br.readLine()) != null) {
					int width = SwingUtilities.computeStringWidth(metrics, line);
					maxWidth = (maxWidth < width) ? width : maxWidth;
					list.add(line);
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			int lines = list.size();
			strs = new String[lines];
			int i = 0;
			for (String e : list) {
				strs[i++] = e;
			}
			int height = metrics.getHeight() * lines;
			c.repaint();
			return new Dimension(maxWidth + 6, height + 4);
		}
	}

	// ===========================================================
	// Private fields
	// ===========================================================

	private NHandsComponent hands;
	private JToolBar toolBar;
	private JToggleButton amputatedToggleButton;

	private SelectionMode selectionMode = SelectionMode.FINGER;
	private Scenario scenario = Scenario.PLAIN_FINGER;

	private HashMap<NFPosition, Segment> allSegments = new LinkedHashMap<NFPosition, Segment>();
	private HashMap<NFPosition, Segment> availableSegments = new LinkedHashMap<NFPosition, Segment>();
	private NFPosition favoredPosition = null;

	// ===========================================================
	// Public constructor
	// ===========================================================

	public HandSegmentSelector() {
		initGUI();
		initPositions();
	}

	// ===========================================================
	// Public methods
	// ===========================================================

	public SelectionMode getSelectionMode() {
		return selectionMode;
	}

	public Scenario getScenario() {
		return scenario;
	}

	public void setScenario(Scenario scenario) {
		Scenario oldScenario = this.scenario;
		this.scenario = scenario;
		if (!oldScenario.equals(scenario)) {
			onScenarioChanged();
		}
	}

	public List<NFPosition> getSelectedPositions() {
		List<NFPosition> selected = new ArrayList<NFPosition>();
		for (Segment segment : availableSegments.values()) {
			if (segment.isSelected())
				selected.add(segment.getPosition());
		}
		return selected;
	}

	public NFPosition getHightlightedPosition() {
		for (Segment segment : availableSegments.values()) {
			if (segment.isHighlighted())
				return segment.getPosition();
		}
		return null;
	}

	public List<NFPosition> getMissingPositions() {
		List<NFPosition> missing = new ArrayList<NFPosition>();
		for (Segment segment : availableSegments.values()) {
			if (segment.isMissing())
				missing.add(segment.getPosition());
		}
		return missing;
	}

	// ===========================================================
	//		Visualization
	// ===========================================================

	public void setHighlighted(NFPosition position, boolean highlighted) {
		Segment segment = availableSegments.get(position);
		if (segment != null) segment.setHighlighted(highlighted);
	}

	public void setSelected(NFPosition position, boolean selected) {
		Segment segment = availableSegments.get(position);
		if (segment != null) segment.setSelected(selected);
	}

	public void setSelectedAll(boolean selected) {
		for (Segment segment : availableSegments.values()) {
			if (!segment.isMissing()) segment.setSelected(selected);
		}
	}

	public void setMissing(NFPosition position, boolean missing) {
		Segment segment = availableSegments.get(position);
		if (segment != null) segment.setMissing(missing);
	}

	// ===========================================================
	// 		Clearing
	// ===========================================================

	public void clear() {
		for (Segment segment : availableSegments.values()) {
			segment.clear();
		}
	}

	public void clearHoover() {
		for (Segment segment : availableSegments.values()) {
			segment.setHovered(false);
		}
	}

	public void clearHighlighting() {
		for (Segment segment : availableSegments.values()) {
			segment.setHighlighted(false);
		}
	}

	public void clearSelection() {
		for (Segment segment : availableSegments.values()) {
			if (segment.isSelected()) {
				segment.setSelected(false);
			}
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		amputatedToggleButton.setEnabled(enabled);
		super.setEnabled(enabled);
	}

	// ===========================================================
	// Private methods
	// ===========================================================

	// ===========================================================
	// 		Event handling
	// ===========================================================
	private void onScenarioChanged() {
		clear();
		amputatedToggleButton.setSelected(false);
		availableSegments.clear();
		selectionMode = scenario.getSelectionMode();

		List<NFPosition> allowedPositions = scenario.getAllowedPositions();

		if (allowedPositions != null) {
			for (NFPosition position : allowedPositions) {
				Segment segment = allSegments.get(position);
				if (segment != null) availableSegments.put(position, segment);
			}
		}
		setSelectedAll(true);

		setEnabled(!selectionMode.equals(SelectionMode.NONE));
		hands.setToolTipText(null);
	}

	private void onMouseMoved(List<NFPosition> availablePositions) {
		clearHoover();
		Segment segment = null;
		if (favoredPosition != null) {
			segment = availableSegments.get(favoredPosition);
		} else {
			segment = getAffectedSegment(availablePositions);
		}

		showToolTip(availablePositions);

		if (segment != null && !segment.isMissing()) {
			segment.setHovered(true);
		}
	}

	private void onMousePressed(List<NFPosition> availablePositions, boolean thirdButtonPressed) {
		Segment segment = null;

		if (selectionMode == SelectionMode.PALMS) {
			clearSelection();
			if (thirdButtonPressed) {
				if (availablePositions.size() > 1) {
					favoredPosition = availablePositions.get(1);
					availablePositions = getAvailablePositions(availablePositions);
					showToolTip(availablePositions);

					segment = this.allSegments.get(favoredPosition);
					if (segment != null) {
						clearHoover();
						segment.setHovered(true);
					}
				}
			} else {
				if (availablePositions.size() != 0) {
					NFPosition nfPosition = availablePositions.get(0);
					segment = this.allSegments.get(nfPosition);
				}
			}
		} else {
			segment = getAffectedSegment(availablePositions);
		}

		if (segment != null) {
			if (amputatedToggleButton.isSelected()) {
				segment.setMissing(!segment.isMissing());
			} else {
				segment.setSelected(!segment.isSelected());
			}
		}
	}

	private void showToolTip(List<NFPosition> positions) {
		hands.setToolTipText(createToolTipMessage(positions));
	}


	private String createToolTipMessage(List<NFPosition> positions) {
		if (positions.size() == 0) return null;
		String msg = "Position: " + positions.get(0);
		if (positions.size() > 1) {
			msg += "\nThis is also: ";
			for (int i = 1; i < positions.size(); i++) {
				msg += positions.get(i).toString() + ", ";
			}
			msg += "\nClick right mouse button to select other palm part.";
		}
		return msg;
	}

	private List<NFPosition> getAvailablePositions(List<NFPosition> affectedPositions) {
		ArrayList<NFPosition> list = new ArrayList<NFPosition>();
		List<NFPosition> allowedPositions = scenario.getAllowedPositions();
		for (NFPosition position : affectedPositions) {
			if (allowedPositions.contains(position))
				list.add(position);
		}
		if (favoredPosition != null && list.contains(favoredPosition)) {
			while (favoredPosition != list.get(0)) {
				NFPosition move = list.get(0);
				list.remove(0);
				list.add(move);
			}
		}
		return list;
	}

	private Segment getAffectedSegment(List<NFPosition> availablePositions) {
		for (NFPosition position : availablePositions) {
			if (availableSegments.get(position) != null) {
				return availableSegments.get(position);
			}
		}
		return null;
	}

	// ===========================================================
	// Initialization
	// ===========================================================

	private void initPositions() {
		allSegments.put(NFPosition.LEFT_LITTLE_FINGER, new Segment(NFPosition.LEFT_LITTLE_FINGER));
		allSegments.put(NFPosition.LEFT_RING_FINGER, new Segment(NFPosition.LEFT_RING_FINGER));
		allSegments.put(NFPosition.LEFT_MIDDLE_FINGER, new Segment(NFPosition.LEFT_MIDDLE_FINGER));
		allSegments.put(NFPosition.LEFT_INDEX_FINGER, new Segment(NFPosition.LEFT_INDEX_FINGER));
		allSegments.put(NFPosition.LEFT_THUMB, new Segment(NFPosition.LEFT_THUMB));

		allSegments.put(NFPosition.RIGHT_THUMB, new Segment(NFPosition.RIGHT_THUMB));
		allSegments.put(NFPosition.RIGHT_INDEX_FINGER, new Segment(NFPosition.RIGHT_INDEX_FINGER));
		allSegments.put(NFPosition.RIGHT_MIDDLE_FINGER, new Segment(NFPosition.RIGHT_MIDDLE_FINGER));
		allSegments.put(NFPosition.RIGHT_RING_FINGER, new Segment(NFPosition.RIGHT_RING_FINGER));
		allSegments.put(NFPosition.RIGHT_LITTLE_FINGER, new Segment(NFPosition.RIGHT_LITTLE_FINGER));

		allSegments.put(NFPosition.PLAIN_LEFT_FOUR_FINGERS, new Segment(NFPosition.PLAIN_LEFT_FOUR_FINGERS));
		allSegments.put(NFPosition.PLAIN_RIGHT_FOUR_FINGERS, new Segment(NFPosition.PLAIN_RIGHT_FOUR_FINGERS));
		allSegments.put(NFPosition.PLAIN_LEFT_THUMB, new Segment(NFPosition.PLAIN_LEFT_THUMB));
		allSegments.put(NFPosition.PLAIN_RIGHT_THUMB, new Segment(NFPosition.PLAIN_RIGHT_THUMB));
		allSegments.put(NFPosition.PLAIN_THUMBS, new Segment(NFPosition.PLAIN_THUMBS));

		allSegments.put(NFPosition.LEFT_FULL_PALM, new Segment(NFPosition.LEFT_FULL_PALM));
		allSegments.put(NFPosition.LEFT_LOWER_PALM, new Segment(NFPosition.LEFT_LOWER_PALM));
		allSegments.put(NFPosition.LEFT_UPPER_PALM, new Segment(NFPosition.LEFT_UPPER_PALM));
		allSegments.put(NFPosition.LEFT_HYPOTHENAR, new Segment(NFPosition.LEFT_HYPOTHENAR));
		allSegments.put(NFPosition.LEFT_INTERDIGITAL, new Segment(NFPosition.LEFT_INTERDIGITAL));
		allSegments.put(NFPosition.LEFT_THENAR, new Segment(NFPosition.LEFT_THENAR));

		allSegments.put(NFPosition.RIGHT_FULL_PALM, new Segment(NFPosition.RIGHT_FULL_PALM));
		allSegments.put(NFPosition.RIGHT_LOWER_PALM, new Segment(NFPosition.RIGHT_LOWER_PALM));
		allSegments.put(NFPosition.RIGHT_UPPER_PALM, new Segment(NFPosition.RIGHT_UPPER_PALM));
		allSegments.put(NFPosition.RIGHT_HYPOTHENAR, new Segment(NFPosition.RIGHT_HYPOTHENAR));
		allSegments.put(NFPosition.RIGHT_INTERDIGITAL, new Segment(NFPosition.RIGHT_INTERDIGITAL));
		allSegments.put(NFPosition.RIGHT_THENAR, new Segment(NFPosition.RIGHT_THENAR));
	}

	private void initGUI() {
		try {
			this.setLayout(null);
			this.setOpaque(false);
			this.setPreferredSize(new java.awt.Dimension(300, 250));
			this.setLayout(new BorderLayout());
			{
				toolBar = new JToolBar();
				toolBar.setOpaque(false);
				toolBar.setFloatable(false);
				this.add(toolBar, BorderLayout.NORTH);
				{
					amputatedToggleButton = new JToggleButton();
					toolBar.add(amputatedToggleButton);
					amputatedToggleButton.setText("Amputate");
					amputatedToggleButton.addItemListener(new ItemListener() {
						public void itemStateChanged(ItemEvent e) {
							if (e.getStateChange() == ItemEvent.SELECTED) {
								selectionMode = SelectionMode.FINGER;
							} else {
								selectionMode = getScenario().getSelectionMode();
							}
						}
					});
				}
			}
			{
				hands = new NHandsComponent() {
					@Override
					public Point getToolTipLocation(MouseEvent event) {
						return new Point(0, -20);
					}
					@Override
					public JToolTip createToolTip() {
						MultiLineToolTip tip = new MultiLineToolTip();
						tip.setComponent(this);
						return tip;
					}
				};
				this.add(hands, BorderLayout.CENTER);
				hands.addMouseListener(new MouseAdapter() {
					@Override
					public void mousePressed(MouseEvent e) {
						if (isEnabled()) {
							onMousePressed(getAvailablePositions(hands.getAffectedPositions(e)), e.getButton() == MouseEvent.BUTTON3);
						}
					}
					@Override
					public void mouseExited(MouseEvent e) {
						hands.setToolTipText(null);
						clearHoover();
					}
				});
				hands.addMouseMotionListener(new MouseMotionAdapter() {
					@Override
					public void mouseMoved(MouseEvent e) {
						if (isEnabled()) {
							onMouseMoved(getAvailablePositions(hands.getAffectedPositions(e)));
						}
					}
				});
			}
			{
				this.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseExited(MouseEvent e) {
						hands.setToolTipText(null);
					}
				});
			}
			ToolTipManager.sharedInstance().setInitialDelay(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ===========================================================
	// 		State change events
	// ===========================================================
	private EventListenerList listenerList = new EventListenerList();

	public void addChangeListener(ChangeListener listener) {
		listenerList.add(ChangeListener.class, listener);
	}

	public void removeChangeListener(ChangeListener listener) {
		listenerList.remove(ChangeListener.class, listener);
	}

	private synchronized void fireStateChanged() {
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ChangeListener.class) {
				((ChangeListener) listeners[i + 1]).stateChanged(new ChangeEvent(this));
			}
		}
	}
}
