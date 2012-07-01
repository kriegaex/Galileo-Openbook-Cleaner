package de.scrum_master.galileo;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.Queue;

import com.sun.org.apache.xalan.internal.xsltc.cmdline.getopt.GetOpt;

import de.scrum_master.galileo.filter.BasicFilter;
import de.scrum_master.galileo.filter.FilterChain;
import de.scrum_master.galileo.filter.JTidyFilter;
import de.scrum_master.galileo.filter.PreJTidyFilter;
import de.scrum_master.galileo.filter.XOMUnclutterFilter;
import de.scrum_master.util.SimpleLogger;

public class OpenbookCleaner
{
	private static File downloadDir;
	private static Book[] books;

	private static boolean MULTI_THREADED = true;
	private static boolean PRETTY_PRINT = true;

	private final static FileFilter HTML_FILES = new FileFilter() {
		public boolean accept(File file) {
			String fileNameLC = file.getName().toLowerCase();
			return fileNameLC.endsWith(".htm") || fileNameLC.endsWith(".html");
		}
	};

	private final static String USAGE_TEXT =
		"Usage: java " + OpenbookCleaner.class.getName() + " [-?] | [options] <download_dir> [<book_id>]*\n\n" +
		"Options:\n"+
		"  -?  Show this help text\n" +
		"  -n  No pretty-printing after structural clean-up (saves ~15% processing time)\n" +
		"  -a  Download & clean *all* books\n" +
		"  -v  Verbose output\n" +
		"  -d  Debug output (implies -v)\n" +
		"  -s  Single-threaded mode with intermediate files (for diagnostics)\n\n" +
		"Parameters:\n"+
		"  download_dir  Download directory for openbook archives (*.zip); must exist\n" +
		"  book_id       Book ID; book will be unpacked to subdirectory <download_dir>/<book_id>.\n" +
		"                You can specify multiple book IDs separated by spaces.\n" +
		"                If -a is specified, the book_id list will be ignored.";

	private static final String REGEX_TOC_RUBY = ".*ruby_on_rails_2.index.htm";

	public static void main(String[] args) throws Exception
	{
		long startTimeTotal = System.currentTimeMillis();
		processArgs(args);
		for (Book book : books) {
			long startTime = System.currentTimeMillis();
			SimpleLogger.echo("\nDownloading, verifying (MD5) and unpacking " + book.unpackDirectory + "...");
			new Downloader(downloadDir, book).download();
			SimpleLogger.echo("Processing " + book.unpackDirectory + "...");
			for (File htmlFile : new File(downloadDir, book.unpackDirectory).listFiles(HTML_FILES))
				cleanHTMLFile(htmlFile);
			SimpleLogger.time("Duration for " + book.unpackDirectory, System.currentTimeMillis() - startTime);
		}
		SimpleLogger.time("\nTotal duration", System.currentTimeMillis() - startTimeTotal);
	}

	private static void processArgs(String[] args)
	{
		if (args.length == 0)
			displayUsageAndExit(0);

		// TODO: GetOpt is poorly documented, hard to use and buggy (getCmdArgs falsely returns options
		// if no non-option command-line agrument is given). There are plenty of better free command line
		// parsing tools. If it was not for indepencence of yet another external library, I would not use
		// JRE's GetOpt.
		GetOpt options = new GetOpt(args, "?navds");
		char option;
		try {
			while ((option = (char) options.getNextOption()) != '\uFFFF') {
				SimpleLogger.debug("Option parser: parameter = " + option);
				switch (option) {
					case '?' : displayUsageAndExit(0); break;
					case 'n' : PRETTY_PRINT = false; break;
					case 'a' : books = Book.values(); break;
					case 'v' : SimpleLogger.VERBOSE = true; break;
					case 'd' : SimpleLogger.DEBUG = true; SimpleLogger.VERBOSE = true; break;
					case 's' : MULTI_THREADED = false; break;
				}
			}

			if (options.getCmdArgs().length < 2 && books == null)
				displayUsageAndExit(1, "missing argument(s) - please specify download_dir and either book_id or -a");

			downloadDir = new File(options.getCmdArgs()[0]);
			SimpleLogger.debug("Option parser: download_dir = " + downloadDir);
			if (! downloadDir.isDirectory())
				displayUsageAndExit(1, "download directory '" + downloadDir + "' not found\n");

			if (books != null)
				return;
			int bookCount = options.getCmdArgs().length - 1;
			books = new Book[bookCount];
			try {
				for (int i = 0; i < bookCount; i++) {
					SimpleLogger.debug("Option parser: book_id[" + (i+1) + "] = " + options.getCmdArgs()[i + 1]);
					books[i] = Book.valueOf(options.getCmdArgs()[i + 1].toUpperCase());
				}
			}
			catch (IllegalArgumentException e) {
				displayUsageAndExit(1, "illegal book_id " + e.getMessage().replaceFirst(".*[.]", "").toLowerCase());
			}
		}
		catch (Exception e) {
			displayUsageAndExit(1);
		}
	}

	private static void displayUsageAndExit(int exitCode)
	{
		displayUsageAndExit(exitCode, null);
	}

	private static void displayUsageAndExit(int exitCode, String errorMessage)
	{
		PrintStream out = (exitCode == 0) ? System.out : System.err;
		out.println(
			USAGE_TEXT + "\n\n" +
			"List of legal book_id values (case-insensitive):"
		);
		for (Book book : Book.values())
			out.println("  " + book.name().toLowerCase());
		if (exitCode != 0 && errorMessage != null)
			out.println("\nError: " + errorMessage);
		System.exit(exitCode);
	}

	private static void cleanHTMLFile(File origFile) throws Exception
	{
		File backupFile = new File(origFile + ".bak");
		SimpleLogger.verbose("  " + origFile.getName());
		// Backups are useful if we want to re-run the application later
		createBackupIfNotExists(origFile, backupFile);
		convert(origFile, backupFile, origFile);
	}

	private static void createBackupIfNotExists(File origFile, File backupFile)
		throws IOException
	{
		if (!backupFile.exists())
			origFile.renameTo(backupFile);
	}

	private static void convert(File origFile, File source, File target)
		throws Exception
	{
		Queue<Class<? extends BasicFilter>> filters = new LinkedList<Class<? extends BasicFilter>>();

		// Define conversion steps:

		// 1. Clean up raw HTML where necessary to make it parseable by JTidy
		boolean needsPreJTidy = origFile.getAbsolutePath().matches(REGEX_TOC_RUBY) ? true : false;
		if (needsPreJTidy)
			filters.add(PreJTidyFilter.class);
		// 2. Convert raw HTML into valid XHTML using JTidy
		filters.add(JTidyFilter.class);
		// 3. Remove clutter (header, footer, navigation, ads) using XOM
		filters.add(XOMUnclutterFilter.class);
		// 4. Pretty-print XOM output again using JTidy (optional)
		if (PRETTY_PRINT)
			filters.add(JTidyFilter.class);

		// Run conversion

		// TODO: Decide if each filter chain should also run in its own thread if (MULTI_THREADED),
		// but probably not because it does not seem to speed up conversion.
		new FilterChain(origFile, source, target, MULTI_THREADED, filters).run();
	}
}
