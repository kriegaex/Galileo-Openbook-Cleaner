package de.scrum_master.galileo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import org.xml.sax.SAXException;

import com.sun.org.apache.xalan.internal.xsltc.cmdline.getopt.GetOpt;

public class OpenbookCleaner
{
	private static File   baseDir;
	private static File[] htmlFiles;
	private static boolean SINGLE_THREADED_WITH_INTERMEDIATE_FILES = false;

	private final static String USAGE_TEXT =
		"Usage: java " + OpenbookCleaner.class.getName() + " [-?] | [options] <book_path>\n\n" +
		"Options:\n"+
		"  -?  show this help text\n" +
		"  -v  verbose output\n" +
		"  -d  debug output (implies -v)\n" +
		"  -s  single-threaded mode with intermediate files (for diagnostics)\n\n" +
		"Parameters:\n"+
		"  book_path  base path containing the book to be cleaned";

	public static void main(String[] args) throws Exception
	{
		long startTime = System.currentTimeMillis();

		processArgs(args);
		SimpleLogger.echo("Processing " + baseDir.getName() + "...");
		for (File htmlFile : htmlFiles)
			cleanHTMLFile(htmlFile);

		SimpleLogger.time("Duration for " + baseDir.getName(), System.currentTimeMillis() - startTime);
	}

	private static void processArgs(String[] args)
	{
		if (args.length == 0)	
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
			baseDir = new File(options.getCmdArgs()[0]);
			SimpleLogger.debug("Option parser: book_path = " + baseDir);
			if (! baseDir.isDirectory()) {
				SimpleLogger.error("Error: book base directory '" + baseDir + "' not found\n");
				displayUsageAndExit(1);
			}
		}
		catch (Exception e) {
			displayUsageAndExit(1);
		}

		htmlFiles = baseDir.listFiles(
			new FileFilter() {
				public boolean accept(File file) {
					String fileNameLC = file.getName().toLowerCase(); 
					return fileNameLC.endsWith(".htm") || fileNameLC.endsWith(".html");
					//return fileNameLC.endsWith("apps_06_005.html");
					//return fileNameLC.endsWith("node429.html");
					//return fileNameLC.endsWith("index.htm") || fileNameLC.endsWith("index.html");
				}
			}
		);
	}

	private static void displayUsageAndExit(int exitCode)
	{
		PrintStream out = (exitCode == 0) ? System.out : System.err;
		out.println(USAGE_TEXT);
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
		if (backupFile.exists())
			return;
		if (origFile.getAbsolutePath().matches(".*ruby_on_rails.index.htm.?"))
			createFilteredBackupForRubyOnRailsTOC(origFile, backupFile);
		else
			origFile.renameTo(backupFile);
	}

	private static void createFilteredBackupForRubyOnRailsTOC(File origFile, File backupFile)
		throws FileNotFoundException, IOException
	{
		SimpleLogger.debug("    Special case for Ruby on Rails book: insert missing </table> tag");
		BufferedReader input = null;
		PrintStream output = null;
		try {
			input = new BufferedReader(new FileReader(origFile));
			output = new PrintStream(backupFile);
			String line;
			while (input.ready()) {
				line = input.readLine();
				if (line.toLowerCase().matches("<table .*bgcolor=.#eeeeee.*")) {
					SimpleLogger.debug("    Found main content table, inserting missing </table> tag before it");
					output.println("</table>");
				}
				output.println(line);
			}
		}
		finally {
			if (input != null) {
				try {
					input.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (output != null) {
				output.close();
			}
		}
	}

	private static void doConversion(File origFile, InputStream rawInput, OutputStream finalOutput)
		throws FileNotFoundException, SAXException, IOException
	{
		// Conversion steps for both modes (single-/multi-threaded):
		//   1. Convert raw HTML into valid XHTML using JTidy
		//   2. Remove clutter (header, footer, navigation, ads) using XOM
		//   3. Pretty-print XOM output again using JTidy (optional)

		if (SINGLE_THREADED_WITH_INTERMEDIATE_FILES) {
			// Single-threaded mode is slower (~40%), but good for diagnostic purposes:
			//   - It creates files for each intermediate processing step.
			//   - Log output is in (chrono)logical order.

			File tidyFile = new File(origFile + ".tidy");
			File xomFile  = new File(origFile + ".xom");

			new TidyXHTMLConverter(rawInput, new FileOutputStream(tidyFile), origFile).run();
			new XOMClutterRemover(new FileInputStream(tidyFile), new FileOutputStream(xomFile), origFile).run();
			new TidyXHTMLConverter(new FileInputStream(xomFile), finalOutput, origFile).run();
		}
		else {
			// Multi-threaded mode is faster, but not so good for diagnostic purposes:
			//   - There are no files for intermediate processing steps.
			//   - Log output is garbled because of multi-threading.

			PipedOutputStream tidyOutput = new PipedOutputStream();
			PipedInputStream tidyInput = new PipedInputStream((PipedOutputStream) tidyOutput);
			PipedOutputStream unclutteredOutput = new PipedOutputStream();
			PipedInputStream unclutteredInput = new PipedInputStream((PipedOutputStream) unclutteredOutput);

			new Thread(new TidyXHTMLConverter(rawInput, tidyOutput, origFile)).start();
			new Thread(new XOMClutterRemover(tidyInput, unclutteredOutput, origFile)).start();
			new Thread(new TidyXHTMLConverter(unclutteredInput, finalOutput, origFile)).start();
		}
	}
}
