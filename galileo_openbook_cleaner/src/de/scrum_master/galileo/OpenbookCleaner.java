package de.scrum_master.galileo;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Queue;

import de.scrum_master.galileo.filter.*;
import de.scrum_master.galileo.tool.AvailableBooksChecker;
import de.scrum_master.galileo.tool.DownloadChecker;
import de.scrum_master.util.SimpleLogger;

public class OpenbookCleaner
{
	static Options options = new Options();

	private static final FileFilter HTML_FILES = new FileFilter() {
		public boolean accept(File file) {
			String fileNameLC = file.getName().toLowerCase();
			return fileNameLC.endsWith(".htm") || fileNameLC.endsWith(".html");
		}
	};

	public static void main(String[] args) throws Exception {
		processArgs(args);
		for (Book book : options.books)
			downloadAndCleanBook(book);
	}

	private static void processArgs(String[] args) throws Exception {
		try {
			options.parse(args); }
		catch (RuntimeException e) {
			displayUsageAndExit(1, e.getMessage()); }
		if (options.showHelp)
			displayUsageAndExit(0, null);
		SimpleLogger.VERBOSE = options.logLevel > 0;
		SimpleLogger.DEBUG = options.logLevel > 1;
		SimpleLogger.LOG_THREAD_ID = options.threading > 0;
		if (options.checkAvail)
			AvailableBooksChecker.main(new String[] {});
		if (options.checkMD5)
			DownloadChecker.main(new String[] {});
	}

	private static void displayUsageAndExit(int exitCode, String errorMessage) throws IOException {
		PrintStream out = (exitCode == 0) ? System.out : System.err;
		options.printHelpOn(out, errorMessage);
		System.exit(exitCode);
	}

	private static void downloadAndCleanBook(Book book) throws Exception {
		new Downloader(options.downloadDir, book).download();
		cleanBook(book);
	}

	private static void cleanBook(Book book) throws Exception {
		for (File htmlFile : new File(options.downloadDir, book.unpackDirectory).listFiles(HTML_FILES))
			cleanChapter(book, htmlFile);
	}

	private static void cleanChapter(Book book, File origFile) throws Exception {
		File backupFile = new File(origFile + ".bak");
		// Backups are useful if we want to re-run the application later
		createBackupIfNotExists(origFile, backupFile);
		getFilterChain(book, origFile, backupFile, origFile).run();
	}

	private static void createBackupIfNotExists(File origFile, File backupFile) throws IOException {
		if (!backupFile.exists())
			if (!origFile.renameTo(backupFile))
				throw new IOException("Cannot rename file '" + origFile + "' to '" + backupFile + "'");
	}

	private static FilterChain getFilterChain(Book book, File origFile, File source, File target)
		throws FileNotFoundException
	{
		Queue<Class<? extends BasicFilter>> filters = new LinkedList<>();
		// Currently we have only one filter, but we will keep the FilterChain capability just in case
		filters.add(JsoupFilter.class);
		return new FilterChain(book, origFile, source, target, options.threading == 1, filters);
	}
}
