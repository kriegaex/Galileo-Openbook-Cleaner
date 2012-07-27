package de.scrum_master.galileo;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

class Options extends OptionParser {
	boolean showHelp;
	File downloadDir;
	int logLevel, prettyPrint, threading;
	List<Book> books = new ArrayList<Book>();

	Options() { super(); }

	@Override
	public OptionSet parse(String... arguments)
	{
		// Set up parsing rules
		OptionSpec<Void> sp_showHelp =
			acceptsAll(asList("?", "help"),         "Display this help text");
		OptionSpec<File> sp_downloadDir =
			acceptsAll(asList("d", "download-dir"), "Download directory for openbooks; must exist")
				.withRequiredArg().ofType(File.class).defaultsTo(new File("."));
		OptionSpec<Integer> sp_logLevel =
			acceptsAll(asList("l", "log-level"),    "Log level (0=normal, 1=verbose, 2=debug, 3=trace)")
				.withRequiredArg().ofType(Integer.class).defaultsTo(0);
		OptionSpec<Integer> sp_prettyPrint =
			acceptsAll(asList("p", "pretty-print"), "Pretty-print after clean-up (0=no, 1=yes); no saves ~15% processing time")
				.withRequiredArg().ofType(Integer.class).defaultsTo(1);
		OptionSpec<Integer> sp_threading =
			acceptsAll(asList("t", "threading"),    "Threading mode (0=single, 1=multi); single is slower, but better for diagnostics)")
				.withRequiredArg().ofType(Integer.class).defaultsTo(1);

		// Parse options
		OptionSet optionSet = super.parse(arguments);

		// Assign parsed values to members for later application access
		showHelp = optionSet.has(sp_showHelp);
		downloadDir = sp_downloadDir.value(optionSet);
		logLevel = sp_logLevel.value(optionSet);
		prettyPrint = sp_prettyPrint.value(optionSet);
		threading = sp_threading.value(optionSet);
		for (String book_id : optionSet.nonOptionArguments()) {
			if ("all".equalsIgnoreCase(book_id)) {
				books = asList(Book.values());
				break;
			}
			try {
				books.add(Book.valueOf(book_id.toUpperCase())); }
			catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("invalid book ID '" + book_id + "'", e); }
		}

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

	public void printHelpOn(PrintStream sink, String errorMessage) throws IOException {
		sink.println(OpenbookCleaner.class.getSimpleName() + " usage: java ... [options] <book_id>*\n");
		printHelpOn(sink);
		sink.println("book_id1 book_id2 ...                   Books to be downloaded & converted");
		sink.println("\nLegal book IDs:");
		String line = "  all (magic value: all books)";
		for (Book book : Book.values()) {
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