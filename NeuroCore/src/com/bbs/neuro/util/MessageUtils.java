package com.bbs.neuro.util;


import java.awt.Component;

import javax.swing.JOptionPane;

public class MessageUtils {

	public static void showError(Component component, Throwable error) {
		showError(component, error, null);
	}

	public static void showError(Component component, Throwable error, String message) {
		String msg = "An error occurred. Please see log for more details.";
		String reason = null;

		if (!Utils.isNullOrEmpty(message)) {
			msg = message;
		}
		if (error.getMessage() != null) {
			reason = error.getMessage();
		} else if (error.getCause() != null) {
			reason = error.getCause().getMessage();
		}

		msg.concat((Utils.isNullOrEmpty(reason)) ? "" : System.getProperty("line.separator") +  "Reason: " + reason);
		showError(component, msg);
	}

	/**
	 * Shows the error message with the exclamation mark.
	 * @param component Parent component of the message.
	 * @param message Error message.
	 */
	public static void showError(Component component, String message) {
		JOptionPane.showMessageDialog(component, message, "Error", JOptionPane.ERROR_MESSAGE);
	}

	public static boolean showQuestion(Component component, String title, String format, Object... args) {
		String str = String.format(format, args);

		int n = JOptionPane.showConfirmDialog(component, str, title, JOptionPane.YES_NO_OPTION);
		if (n == JOptionPane.YES_OPTION) {
			return true;
		}
		return false;
	}

	/**
	 * Shows the information message with the exclamation mark.
	 * @param parentComponent Parent component of the message.
	 * @param title Title of the message.
	 * @param format Message format.
	 * @param args Message arguments.
	 */
	public static void showInformation(Component parentComponent, String title, String format, Object... args) {
		String str = String.format(format, args);
		showInformation(parentComponent, title, str);
	}

	public static void showInformation(Component parentComponent, String message) {
		JOptionPane.showMessageDialog(parentComponent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
	}

	/**
	 * Shows the information message with the exclamation mark.
	 * @param parentComponent Parent component of the message.
	 * @param title Title of the message.
	 * @param message Error message.
	 */
	public static void showInformation(Component parentComponent, String title, String message) {
		JOptionPane.showMessageDialog(parentComponent, message, title, JOptionPane.INFORMATION_MESSAGE);
	}
}
