package de.scrum_master.galileo;

import java.io.*;
import java.util.SortedMap;
import java.util.TreeMap;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import de.scrum_master.util.SimpleLogger;

public class Book
{
	public String title;
	public String unpackDirectory;
	public String downloadArchive;
	public String archiveMD5;
	public String coverImage;

	public final static String  CONFIG_FOLDER   = "resource";
	public final static String  CONFIG_FILE     = "config.xml";
	public final static String  CONFIG_ENCODING = "iso-8859-15";
	public final static XStream XSTREAM         = new XStream(new DomDriver());

	static {
		XSTREAM.alias("galileo-openbooks", TreeMap.class);
		XSTREAM.alias("book", Book.class);
		XSTREAM.alias("id", String.class);
	}

	public static SortedMap<String, Book> books = new TreeMap<String, Book>();

	public static void writeConfig(boolean debugMode) {
		Writer configFileWriter = null;
		boolean debugModeOrig = SimpleLogger.DEBUG;
		try {
			// Activate the debug channel, if specified by caller
			SimpleLogger.DEBUG = debugMode;
			SimpleLogger.debug("Writing local configuration file " + CONFIG_FILE);
			configFileWriter = new FileWriter(CONFIG_FILE);
			configFileWriter.write("<?xml version=\"1.0\" encoding=\"" + CONFIG_ENCODING + "\"?>\n");
			XSTREAM.toXML(books, configFileWriter);
			configFileWriter.write("\n");
		} catch (Exception e) {
			SimpleLogger.debug("Cannot write local configuration file " + CONFIG_FILE);
			throw new RuntimeException("Cannot write local configuration file " + CONFIG_FILE, e);
		}
		finally {
			try { if (configFileWriter != null) configFileWriter.close(); } catch (Exception ignored) { }
			// Reset debug channel to previously saved state
			SimpleLogger.DEBUG = debugModeOrig;
		}
	}

	@SuppressWarnings("unchecked")
	public static void readConfig(boolean debugMode) {
		InputStream configFileStream = null;
		boolean debugModeOrig = SimpleLogger.DEBUG;
		try {
			// Activate the debug channel, if specified by caller
			SimpleLogger.DEBUG = debugMode;
			try {
				SimpleLogger.debug("Reading local configuration file " + CONFIG_FILE);
				configFileStream = new FileInputStream(CONFIG_FILE);
			} catch (Exception e) {
				SimpleLogger.debug("Cannot open local configuration file " + CONFIG_FILE);
			}
			if (configFileStream == null) try {
				SimpleLogger.debug("Reading IDE configuration file " + CONFIG_FOLDER + "/" + CONFIG_FILE);
				configFileStream = new FileInputStream(CONFIG_FOLDER + "/" + CONFIG_FILE);
			} catch (Exception e) {
				SimpleLogger.debug("Cannot open IDE configuration file " + CONFIG_FOLDER + "/" + CONFIG_FILE);
			}
			if (configFileStream == null) try {
				SimpleLogger.debug("Reading JAR configuration file " + CONFIG_FILE);
				configFileStream = Book.class.getResourceAsStream("/" + CONFIG_FILE);
				if (configFileStream == null)
					throw new RuntimeException("Configuration file " + CONFIG_FILE + " not found or unreadable");
			} catch (RuntimeException e) {
				SimpleLogger.debug("Cannot open JAR configuration file " + CONFIG_FILE);
				throw e;
			}
			try {
				books = (SortedMap<String, Book>) XSTREAM.fromXML(configFileStream);
			} catch (Exception e) {
				throw new RuntimeException(
					"Configuration file " + CONFIG_FILE + " could not be read or parsed correctly. " +
					"Please check for syntax errors.",
					e
				);
			}
		}
		finally {
			try { if (configFileStream != null) configFileStream.close(); } catch (Exception ignored) { }
			// Reset debug channel to previously saved state
			SimpleLogger.DEBUG = debugModeOrig;
		}
	}
}
