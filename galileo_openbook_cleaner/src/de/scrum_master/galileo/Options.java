package de.scrum_master.galileo;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import joptsimple.*;

class Options extends OptionParser {
	boolean showHelp;
	boolean checkAvail;
	boolean checkMD5;
	File downloadDir;
	int logLevel, prettyPrint, threading;
	List<Book> books = new ArrayList<>();
	boolean writeConfig;

	Options() { super(); }

	@Override @SuppressWarnings("unchecked")
	public OptionSet parse(String... arguments) {
		// Set up parsing rules
		OptionSpec<Void>    sp_showHelp    = makeOptionSpec("?", "help",         "Display this help text", null);
		OptionSpec<Void>    sp_checkAvail  = makeOptionSpec("c", "check-avail",  "Check Galileo homepage for available books, compare with known ones", null);
		OptionSpec<Void>    sp_checkMD5    = makeOptionSpec("m", "check-md5",    "Download all known books without storing them, verifying their MD5 checksums (slow! >1 Gb download)", null);
		OptionSpec<File>    sp_downloadDir = makeOptionSpec("d", "download-dir", "Download directory for openbooks; must exist", new File("."));
		OptionSpec<Integer> sp_logLevel    = makeOptionSpec("l", "log-level",    "Log level (0=normal, 1=verbose, 2=debug, 3=trace)", 0);
		OptionSpec<Integer> sp_threading   = makeOptionSpec("t", "threading",    "Threading mode (0=single, 1=multi); single is slower, but better for diagnostics)", 1);
		OptionSpec<Void>    sp_writeConfig = makeOptionSpec("w", "write-config", "Write editable book list to config.xml", null);

		// Parse options
		OptionSet optionSet = super.parse(arguments);

		// Assign parsed values to members for later application access
		showHelp = optionSet.has(sp_showHelp);
		checkAvail = optionSet.has(sp_checkAvail);
		checkMD5 = optionSet.has(sp_checkMD5);
		downloadDir = sp_downloadDir.value(optionSet);
		logLevel = sp_logLevel.value(optionSet);
		threading = sp_threading.value(optionSet);
		writeConfig = optionSet.has(sp_writeConfig);
		Book.readConfig(logLevel > 1);
		if (writeConfig)
			Book.writeConfig(logLevel > 1);
		for (String book_id : optionSet.nonOptionArguments()) {
			if ("all".equalsIgnoreCase(book_id)) {
				books = new ArrayList<>(Book.books.values());
				break;
			}
			try {
				books.add(Book.books.get(book_id.toUpperCase())); }
			catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("invalid book ID '" + book_id + "'", e); }
		}
		if (books.size() == 0 && !(showHelp || checkAvail || checkMD5))
			throw new IllegalArgumentException("you must specify at least one book ID");

		// Validate values
		if (! downloadDir.isDirectory())
			throw new IllegalArgumentException("invalid download directory '" + downloadDir + "', does not exist");
		if (prettyPrint < 0 || prettyPrint > 1)
			throw new IllegalArgumentException("invalid pretty-print mode " + prettyPrint + ", must be 0 or 1");
		if (logLevel < 0 || logLevel > 3)
			throw new IllegalArgumentException("invalid log level " + logLevel + ", must be in [0..3]");
		if (threading < 0 || threading > 1)
			throw new IllegalArgumentException("invalid threading mode " + threading + ", must be 0 or 1");

		return optionSet;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private OptionSpec makeOptionSpec(String shortName, String longName, String helpText, Object defaultValue) {
		OptionSpecBuilder osb = acceptsAll(asList(shortName, longName), helpText);
		return defaultValue == null ? osb : osb.withRequiredArg().ofType((Class) defaultValue.getClass()).defaultsTo(defaultValue);
	}

	public void printHelpOn(PrintStream sink, String errorMessage) throws IOException {
		sink.println(OpenbookCleaner.class.getSimpleName() + " usage: java ... [options] <book_id>*\n");
		printHelpOn(sink);
		sink.println("\nbook_id1 book_id2 ...                   Books to be downloaded & converted");
		sink.println("\nLegal book IDs:");
		String line = "  all (magic value: all books)";
		for (Book book : Book.books.values()) {
			if (line.length() + book.unpackDirectory.length() < 79)
				line += ", " + book.unpackDirectory;
			else {
				sink.println(line + ",");
				line = "  " + book.unpackDirectory;
			}
		}
		sink.println(line);
		if (errorMessage != null)
			sink.println("\nError: " + errorMessage);
	}
}