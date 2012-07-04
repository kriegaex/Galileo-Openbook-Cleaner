package de.scrum_master.galileo;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import com.beust.jcommander.ParameterException;

import de.scrum_master.galileo.filter.BasicFilter;
import de.scrum_master.galileo.filter.FilterChain;
import de.scrum_master.galileo.filter.JTidyFilter;
import de.scrum_master.galileo.filter.PreJTidyFilter;
import de.scrum_master.galileo.filter.XOMUnclutterFilter;
import de.scrum_master.util.SimpleLogger;

public class OpenbookCleaner
{
	private static final FileFilter HTML_FILES = new FileFilter() {
		public boolean accept(File file) {
			String fileNameLC = file.getName().toLowerCase();
			return fileNameLC.endsWith(".htm") || fileNameLC.endsWith(".html");
		}
	};

	public static void main(String[] args) throws Exception {
		long startTimeTotal = System.currentTimeMillis();
		processArgs(args);
		for (Book book : Options.VALUES.books) {
			SimpleLogger.echo("Book: " + book.unpackDirectory);
			SimpleLogger.indent();
			long startTimeBook = System.currentTimeMillis();
			new Downloader(Options.VALUES.downloadDir, book).download();
			cleanBook(book);
			SimpleLogger.time("Duration for " + book.unpackDirectory, System.currentTimeMillis() - startTimeBook);
			SimpleLogger.dedent();
		}
		SimpleLogger.echo("");
		SimpleLogger.time("Total duration", System.currentTimeMillis() - startTimeTotal);
	}

	private static void processArgs(String[] args) {
		try {
			Options.PARSER.parse(args);
		}
		catch (ParameterException e) {
			// Parsing error
			// TODO: get rid of this if-else as soon as JCommander knows a special help mode, not throwing
			// exceptions anymore for required parameters which are irrelevant when help is required
			if (Options.VALUES.showHelp)
				// Case 1: "--help" was part of command line -> ignore error, display help, exit cleanly
				displayUsageAndExit(0, null);
			else
				// Case 2: other parsing error -> display help + error message, exit with error code
				displayUsageAndExit(1, e.getMessage());
		}

		// User wants help -> ignore other parameters, display help, exit cleanly
		if (Options.VALUES.showHelp)
			displayUsageAndExit(0, null);

		// null is a magic value for "all books"
		if (Options.VALUES.books.contains(null))
			Options.VALUES.books = Arrays.asList(Book.values());

		// Configure logging
		SimpleLogger.VERBOSE = Options.VALUES.logLevel > 0;
		SimpleLogger.DEBUG = Options.VALUES.logLevel > 1;
		SimpleLogger.LOG_THREAD_ID = Options.VALUES.threadingMode == 1;

	}

	private static void displayUsageAndExit(int exitCode, String errorMessage) {
		PrintStream out = (exitCode == 0) ? System.out : System.err;
		StringBuilder usageText = new StringBuilder();
		Options.PARSER.usage(usageText);
		out.println(usageText);
		out.println("  Legal book IDs:");
		out.println("    all (magic value to process all books)");
		out.println("    ----------");
		for (Book book : Book.values())
			out.println("    " + book.unpackDirectory);
		if (exitCode != 0)
			out.println("\nError: " + errorMessage);
		System.exit(exitCode);
	}

	private static void cleanBook(Book book) throws Exception {
		SimpleLogger.verbose("Filtering ...");
		SimpleLogger.indent();
		for (File htmlFile : new File(Options.VALUES.downloadDir, book.unpackDirectory).listFiles(HTML_FILES))
			cleanChapter(book, htmlFile);
		SimpleLogger.dedent();
		SimpleLogger.verbose("Filtering done");
	}

	private static void cleanChapter(Book book, File origFile) throws Exception {
		SimpleLogger.verbose("Chapter: " + origFile.getName());
		SimpleLogger.indent();
		File backupFile = new File(origFile + ".bak");
		// Backups are useful if we want to re-run the application later
		createBackupIfNotExists(origFile, backupFile);
		getFilterChain(book, origFile, backupFile, origFile).run();
		SimpleLogger.dedent();
		SimpleLogger.verbose("Chapter done");

	}

	private static void createBackupIfNotExists(File origFile, File backupFile) throws IOException {
		if (!backupFile.exists())
			origFile.renameTo(backupFile);
	}

	private static FilterChain getFilterChain(Book book, File origFile, File source, File target)
		throws FileNotFoundException
	{
		Queue<Class<? extends BasicFilter>> filters =
			new LinkedList<Class<? extends BasicFilter>>();

		// Step 1: clean up raw HTML where necessary to make it parseable by JTidy
		if (book.equals(Book.RUBY_ON_RAILS_2))
			filters.add(PreJTidyFilter.class);
		// Step 2: convert raw HTML into valid XHTML using JTidy
		filters.add(JTidyFilter.class);
		// Step 3: remove clutter (header, footer, navigation, ads) using XOM
		filters.add(XOMUnclutterFilter.class);
		// Step 4: pretty-print XOM output again using JTidy (optional)
		if (Options.VALUES.prettyPrint != 0)
			filters.add(JTidyFilter.class);

		return new FilterChain(
			origFile, source, target,
			Options.VALUES.threadingMode == 1,
			filters
		);
	}
}
