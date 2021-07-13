package com.bbs.neuro.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.filechooser.FileFilter;

public class Utils {
	private Utils() {
	};

	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	public static final String PATH_SEPARATOR = System.getProperty("path.separator");
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");

	public static final String VERSION = "4.2.0.0";
	public static final String COPYRIGHT = "Copyright © 2011-2012 Neurotechnology";

	public static void printTutorialHeader(String description, String name, String [] args) {
		printTutorialHeader(description, name, VERSION, COPYRIGHT, args);
	}

	public static void printTutorialHeader(String description, String name, String version, String [] args)
	{
		printTutorialHeader(description, name, version, COPYRIGHT, args);
	}

	public static void printTutorialHeader(String description, String name, String version, String copyright, String [] args)
	{
		System.out.println(name);
		System.out.println();
		System.out.format("%s (Version: %s)%n", description, version);
		System.out.println(copyright.replace("©", "(C)"));
		System.out.println();
		if(args != null && args.length > 0)
		{
			System.out.println("Arguments:");
			for(int i = 0; i < args.length; i++)
			{
				System.out.format("\t%s%n", args[i]);
			}
			System.out.println();
		}
	}

	public static void writeAllBytes(String pathname, ByteBuffer buffer) throws IOException {
		if (buffer == null) throw new NullPointerException("buffer");
		File file = new File(pathname);
		file.getParentFile().mkdirs();
		FileChannel channel = new FileOutputStream(file).getChannel();
		channel.write(buffer);
		channel.close();
	}

	public static ByteBuffer readAllBytes(String file) throws IOException {
		FileChannel channel = new FileInputStream(file).getChannel();
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect((int) channel.size());
		channel.read(byteBuffer);
		byteBuffer.flip();
		return byteBuffer;
	}

	/**
	 * Gets user working directory.
	 */
	public static String getWorkingDirectory() {
		return System.getProperty("user.dir");
	}

	/**
	 * Gets user home directory.
	 */
	public static String getHomeDirectory() {
		return System.getProperty("user.home");
	}

	public static String combinePath(String part1, String part2) {
		return String.format("%s%s%s", part1, FILE_SEPARATOR, part2);
	}

	public static boolean isNullOrEmpty(String value) {
		return value == null || "".equals(value);
	}

	public static class TemplateFileFilter extends FileFilter {
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || (f.getName().endsWith(".dat") || f.getName().endsWith(".data"));
		}

		@Override
		public String getDescription() {
			return "*.dat; *.data";
		}
	}

	public static class ImageFileFilter extends FileFilter {
		String[] extentions;
		public ImageFileFilter(String extentions) {
			this.extentions = extentions.split(";");
		}

		public boolean accept(File f) {
			for (int i = 0; i < extentions.length; i++) {
				if (f.isDirectory() || f.getName().toLowerCase().endsWith(extentions[i].toLowerCase()))
					return true;
			}
			return false;
		}

		public String getDescription() {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < extentions.length; i++) {
				sb.append(extentions[i]);
				if (i != extentions.length - 1)
					sb.append(", ");
			}
			return sb.toString();
		}
	}

	public static int qualityToPercent(int value) {
		return (2 * value * 100 + 255) / (2 * 255);
	}

	public static int qualityFromPercent(int value) {
		return (2 * value * 255 + 100) / (2 * 100);
	}

	public static String matchingThresholdToString(int value) {
		double p = -value / 12.0;
		NumberFormat nf = NumberFormat.getPercentInstance();
		nf.setMaximumFractionDigits(Math.max(0, (int) Math.ceil(-p) - 2));
		nf.setMinimumIntegerDigits(1);
		return nf.format(Math.pow(10, p));
	}

	public static int matchingThresholdFromString(String value) throws ParseException {
		char percent = new DecimalFormatSymbols().getPercent();
		value = value.replace(percent, ' ');
		Number number = NumberFormat.getNumberInstance().parse(value);
		double parse = number.doubleValue();
		double p = Math.log10(Math.max(Double.MIN_VALUE, Math.min(1, parse / 100)));
		return Math.max(0, (int) Math.round(-12 * p));
	}
}
