package com.bbs.neuro.util;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Platform;
import java.util.logging.Logger;

public class LibraryManager {

    // ===========================================================
    // Private static fields
    // ===========================================================
    private static LibC LIB_C = null;
    private static final String MAIN_LIBRARY = "NCore";
//	private static final ClassLoader[] loaders;
//	private static final java.lang.reflect.Field LIBRARIES;
    private static final String WIN32_X86 = "Win32_x86";
    private static final String WIN64_X64 = "Win64_x64";
    private static final String LINUX_X86 = "Linux_x86";
    private static final String LINUX_X86_64 = "Linux_x86_64";
    private static final String MAC_OS = "/Library/Frameworks/Neurotechnology/";

    // ===========================================================
    // Static constructor
    // ===========================================================
//	static {
//		try {
//			ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
//			ClassLoader currentLoader = LibraryManager.class.getClassLoader();
//			loaders = new ClassLoader[] { systemLoader, currentLoader };
//			LIBRARIES = ClassLoader.class.getDeclaredField("loadedLibraryNames");
//			LIBRARIES.setAccessible(true);
//
//			if (Platform.isLinux() || Platform.isMac()) {
//				LIB_C = (LibC) com.sun.jna.Native.loadLibrary("c",	LibC.class);
//			}
//		} catch (Throwable e) {
//			throw new ExceptionInInitializerError(e);
//		}
//	}
    // ===========================================================
    // Private static methods
    // ===========================================================
//	@SuppressWarnings("unchecked")
//	private static List<String> getLoadedLibraries() {
//		final List<String> libraries = new LinkedList<String>();
//		for (ClassLoader loader : loaders) {
//			try {
//				libraries.addAll((Vector<String>) LIBRARIES.get(loader));
//			} catch (Exception e) {
//				System.err.println("An error occurred while collecting loaded libraries.");
//			}
//		}
//		return libraries;
//	}
//	private static String getLibraryDirectory(String name) {
//		for (String path : getLoadedLibraries()) {
//			if (path.contains(name)) {
//				String tmp = path;
//				if (path.lastIndexOf(Utils.FILE_SEPARATOR) != -1) tmp = path.substring(0, path.lastIndexOf(Utils.FILE_SEPARATOR));
//				return tmp;
//			}
//		}
//		return null;
//	}
//	private static String getJNLPCacheDirectory(){
//		String directory = getLibraryDirectory(MAIN_LIBRARY);
//		if (directory == null) {
//			try {
//				System.out.println("Obtaining JNLP cache directory.");
//				System.loadLibrary(MAIN_LIBRARY);
//				directory = getLibraryDirectory(MAIN_LIBRARY);
//				System.out.println("JNLP cache directory => " + directory);
//			} catch (UnsatisfiedLinkError e) {
//				System.err.println(e.getMessage());
//			}
//		}
//		return directory;
//	}
    private static void changeDirectory(String directory) {
        if (directory != null && new File(directory).isDirectory()) {
            if (Platform.isLinux() || Platform.isMac()) {
                System.out.println("Changing working directory to " + directory);
                LIB_C.chdir(directory);
            }
        }
    }

    // ===========================================================
    // Public static methods
    // ===========================================================
    public static void initLibraryPath() {
        StringBuffer path = new StringBuffer();
        Logger.getAnonymousLogger().info("Working Directory:" + Utils.getWorkingDirectory());
        int index = Utils.getWorkingDirectory().lastIndexOf(Utils.FILE_SEPARATOR);
        if (index == -1) {
            return;
        }
        String part = Utils.getWorkingDirectory() + Utils.FILE_SEPARATOR + "lib";
        if (Platform.isWindows()) {
            
            path.append(part);
            path.append(Utils.FILE_SEPARATOR);
            path.append(Platform.is64Bit() ? WIN64_X64 : WIN32_X86);
            
        } else if (Platform.isLinux()) {
            index = part.lastIndexOf(Utils.FILE_SEPARATOR);
            if (index == -1) {
                return;
            }
            part = part.substring(0, index);
            path.append(part);
            path.append(Utils.FILE_SEPARATOR);
            path.append(Platform.is64Bit() ? LINUX_X86_64 : LINUX_X86);
        } else if (Platform.isMac()) {
            path.append(MAC_OS);
        }
        System.setProperty("jna.library.path", path.toString());
        System.setProperty("java.library.path", path.toString()
                + Utils.PATH_SEPARATOR
                + System.getProperty("java.library.path"));
        
        try {
            Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
            fieldSysPath.setAccessible(true);
            fieldSysPath.set(null, null);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }
    }
    
    public static void loadLibraries(List<String> libraries) {
        String userDir = Utils.getWorkingDirectory();
//		String jnlpDir = getJNLPCacheDirectory();
//		changeDirectory(jnlpDir);
        for (String library : libraries) {
            System.out.println("Loading library " + library);
            try {
                System.loadLibrary(library);
            } catch (UnsatisfiedLinkError e) {
                System.err.println("Library " + library + " was not loaded.");
            }
        }
//		changeDirectory(userDir);
    }
    
    public static String getLibraryPath() {
        StringBuilder path = new StringBuilder();
        int index = Utils.getWorkingDirectory().lastIndexOf(Utils.FILE_SEPARATOR);
        if (index == -1) {
            return null;
        }
        String part = Utils.getWorkingDirectory() + Utils.FILE_SEPARATOR + "lib";
        if (Platform.isWindows()) {
            
            path.append(part);
            path.append(Utils.FILE_SEPARATOR);
            path.append(Platform.is64Bit() ? WIN64_X64 : WIN32_X86);
            
        } else if (Platform.isLinux()) {
            index = part.lastIndexOf(Utils.FILE_SEPARATOR);
            if (index == -1) {
                return null;
            }
            part = part.substring(0, index);
            path.append(part);
            path.append(Utils.FILE_SEPARATOR);
            path.append(Platform.is64Bit() ? LINUX_X86_64 : LINUX_X86);
        } else if (Platform.isMac()) {
            path.append(MAC_OS);
        }
        return path.toString();
    }
    
    private interface LibC extends Library {
        
        int chdir(String new_directory);
    }
}
