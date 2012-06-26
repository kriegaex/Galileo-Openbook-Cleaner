package de.scrum_master.galileo;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import org.xml.sax.SAXException;

import com.sun.org.apache.xalan.internal.xsltc.cmdline.getopt.GetOpt;

import de.scrum_master.galileo.filter.JTidyFilter;
import de.scrum_master.galileo.filter.PreJTidyFilter;
import de.scrum_master.galileo.filter.XOMUnclutterFilter;
import de.scrum_master.util.SimpleLogger;

public class OpenbookCleaner
{
	private static File downloadDir;
	private static BookInfo bookInfo;

	private static boolean SINGLE_THREADED_WITH_INTERMEDIATE_FILES = false;

	private final static FileFilter HTML_FILES = new FileFilter() {
		public boolean accept(File file) {
			String fileNameLC = file.getName().toLowerCase();
			return fileNameLC.endsWith(".htm") || fileNameLC.endsWith(".html");
		}
	};

	private final static String USAGE_TEXT =
		"Usage: java " + OpenbookCleaner.class.getName() + " [-?] | [options] <download_dir> <book_id>\n\n" +
		"Options:\n"+
		"  -?  show this help text\n" +
		"  -v  verbose output\n" +
		"  -d  debug output (implies -v)\n" +
		"  -s  single-threaded mode with intermediate files (for diagnostics)\n\n" +
		"Parameters:\n"+
		"  download_dir  download directory for openbook archives (*.zip); must exist" +
		"  book_id       book ID; book will be unpacked to subdirectory <download_dir>/<book_id>";

	private static final String REGEX_TOC_RUBY = ".*ruby_on_rails.index.htm";

	public static void main(String[] args) throws Exception
	{
		long startTime = System.currentTimeMillis();

		processArgs(args);

		SimpleLogger.echo("Downloading, verifying (MD5) and unpacking " + bookInfo + "...");
		new Downloader(downloadDir, bookInfo).download();

		SimpleLogger.echo("Processing " + bookInfo + "...");
		for (File htmlFile : new File(downloadDir, bookInfo.unpackDirectory).listFiles(HTML_FILES))
			cleanHTMLFile(htmlFile);

		SimpleLogger.time("Duration for " + bookInfo, System.currentTimeMillis() - startTime);
	}

	private static void processArgs(String[] args)
	{
		if (args.length < 2)
			displayUsageAndExit(0);

		// TODO: GetOpt is poorly documented, hard to use and buggy (getCmdArgs falsely returns options
		// if no non-option command-line agrument is given). There are plenty of better free command line
		// parsing tools. If it was not for indepencence of yet another external library, I would not use
		// JRE's GetOpt.
		GetOpt options = new GetOpt(args, "?vds");
		try {
			int i = options.getNextOption();
			 while (i != -1) {
				SimpleLogger.debug("Option parser: parameter = " + (char) i);
				switch ((char) i) {
					case '?' :
						displayUsageAndExit(0);
						break;
					case 'v' :
						SimpleLogger.VERBOSE = true;
						break;
					case 'd' :
						SimpleLogger.DEBUG= true;
						SimpleLogger.VERBOSE = true;
						break;
					case 's' :
						SINGLE_THREADED_WITH_INTERMEDIATE_FILES = true;
						break;
				}
				i = options.getNextOption();
			}

			if (options.getCmdArgs().length < 2)
				displayUsageAndExit(0);

			downloadDir = new File(options.getCmdArgs()[0]);
			SimpleLogger.debug("Option parser: download_dir = " + downloadDir);
			if (! downloadDir.isDirectory())
				displayUsageAndExit(1, "download directory '" + downloadDir + "' not found\n");

			try {
				bookInfo = BookInfo.valueOf(options.getCmdArgs()[1].toUpperCase());
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
		for (BookInfo md : BookInfo.values())
			out.println("  " + md.name().toLowerCase());
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
		doConversion(origFile, new FileInputStream(backupFile), new FileOutputStream(origFile));
	}

	private static void createBackupIfNotExists(File origFile, File backupFile)
		throws IOException
	{
		if (!backupFile.exists())
			origFile.renameTo(backupFile);
	}

	private static void doConversion(File origFile, InputStream rawInput, OutputStream finalOutput)
		throws FileNotFoundException, SAXException, IOException
	{
		// Conversion steps for both modes (single-/multi-threaded):
		//   1. Clean up raw HTML where necessary to make it parseable by JTidy
		//   2. Convert raw HTML into valid XHTML using JTidy
		//   3. Remove clutter (header, footer, navigation, ads) using XOM
		//   4. Pretty-print XOM output again using JTidy (optional)

		final boolean needsPreJTidy = origFile.getAbsolutePath().matches(REGEX_TOC_RUBY) ? true : false;

		if (SINGLE_THREADED_WITH_INTERMEDIATE_FILES) {
			// Single-threaded mode is slower (~40%), but good for diagnostic purposes:
			//   - It creates files for each intermediate processing step.
			//   - Log output is in (chrono)logical order.

			// Set up intermediate files
			File preJTidyFile = new File(origFile + ".pretidy");
			File jTidyFile    = new File(origFile + ".tidy");
			File xomFile      = new File(origFile + ".xom");

			// Run conversion steps, using output of step (n) as input of step (n+1)
			if (needsPreJTidy) {
				new PreJTidyFilter(rawInput, new FileOutputStream(preJTidyFile), origFile).run();
				new JTidyFilter(new FileInputStream(preJTidyFile), new FileOutputStream(jTidyFile), origFile).run();
			}
			else {
				new JTidyFilter(rawInput, new FileOutputStream(jTidyFile), origFile).run();
			}
			new XOMUnclutterFilter(new FileInputStream(jTidyFile), new FileOutputStream(xomFile), origFile).run();
			new JTidyFilter(new FileInputStream(xomFile), finalOutput, origFile).run();
		}
		else {
			// Multi-threaded mode is faster, but not so good for diagnostic purposes:
			//   - There are no files for intermediate processing steps.
			//   - Log output is garbled because of multi-threading.

			// Set up pipes
			PipedOutputStream preJTidyOutput    = new PipedOutputStream();
			PipedInputStream  preJTidyInput     = new PipedInputStream(preJTidyOutput);
			PipedOutputStream jTidyOutput       = new PipedOutputStream();
			PipedInputStream  jTidyInput        = new PipedInputStream(jTidyOutput);
			PipedOutputStream unclutteredOutput = new PipedOutputStream();
			PipedInputStream  unclutteredInput  = new PipedInputStream(unclutteredOutput);

			// Run threads, piping output of thread (n) into input of thread (n+1)
			if (needsPreJTidy) {
				new Thread(new PreJTidyFilter(rawInput, preJTidyOutput, origFile)).start();
				new Thread(new JTidyFilter(preJTidyInput, jTidyOutput, origFile)).start();
			}
			else {
				new Thread(new JTidyFilter(rawInput, jTidyOutput, origFile)).start();
			}
			new Thread (new XOMUnclutterFilter(jTidyInput, unclutteredOutput, origFile)).start();
			new Thread (new JTidyFilter(unclutteredInput, finalOutput, origFile)).start();
		}
	}
}
