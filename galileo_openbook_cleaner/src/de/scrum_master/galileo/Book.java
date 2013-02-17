package de.scrum_master.galileo;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
	public final static XStream XSTREAM         = new XStream(new DomDriver());

	static {
		XSTREAM.alias("galileo-openbooks", TreeMap.class);
		XSTREAM.alias("book", Book.class);
		XSTREAM.alias("id", String.class);
	}

	public static SortedMap<String, Book> books = new TreeMap<String, Book>();

	public static void writeConfig() throws Exception {
		OutputStream configFileStream = null;
		OutputStreamWriter configFileWriter = null;
		try {
			SimpleLogger.debug("Writing local configuration file " + CONFIG_FILE);
			configFileStream = new FileOutputStream(CONFIG_FILE);
			configFileWriter = new OutputStreamWriter(configFileStream, "iso-8859-15");
			XSTREAM.toXML(books, configFileWriter);
		} catch (Exception e) {
			SimpleLogger.debug("Cannot write local configuration file " + CONFIG_FILE);
			throw e;
		}
		finally {
			try { if (configFileWriter != null) configFileWriter.close(); } catch (Exception e) { }
			try { if (configFileStream != null) configFileStream.close(); } catch (Exception e) { }
		}
	}

	@SuppressWarnings("unchecked")
	public static void readConfig(boolean debugMode) {
		InputStream configFileStream = null;
		InputStreamReader configFileReader = null;
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
				configFileReader = new InputStreamReader(configFileStream, "iso-8859-15");
				books = (SortedMap<String, Book>) XSTREAM.fromXML(configFileReader);
			} catch (Exception e) {
				throw new RuntimeException(
					"Configuration file " + CONFIG_FILE + " could not be read or parsed correctly. " +
					"Please check for syntax errors.",
					e
				);
			}
		}
		finally {
			try { if (configFileReader != null) configFileReader.close(); } catch (Exception e) { }
			try { if (configFileStream != null) configFileStream.close(); } catch (Exception e) { }
			// Reset debug channel to previously saved state
			SimpleLogger.DEBUG = debugModeOrig;
		}
	}
}
