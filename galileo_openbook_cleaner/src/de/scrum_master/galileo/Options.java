package de.scrum_master.galileo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

@Parameters(separators=" =")
class Options
{
	static final Options VALUES = new Options();
	static final JCommander PARSER = new JCommander(VALUES);
	static {
		PARSER.setProgramName(OpenbookCleaner.class.getSimpleName());
		PARSER.setColumnSize(99);
	}

	private Options() {
		// This is a singleton!
	}

	@Parameter(
		names = {"-?", "--help"},
		description = "Display this help text"
	)
	boolean showHelp = false;

	@Parameter(
		names = {"-d", "--download-dir"},
		converter = DownloadDirConverter.class,
		description = "Download directory for openbook archives (*.zip); must exist"
	)
	File downloadDir = new File(".");

	@Parameter(
		names = {"-n", "--no-pretty-print"},
		description = "No pretty-printing after clean-up (saves ~15% processing time)"
	)
	boolean noPrettyPrint = false;

	@Parameter(
		names = {"-l", "--log-level"},
		validateWith = LogLevelValidator.class,
		description = "Log level (0=normal, 1=verbose, 2=debug)"
	)
	int logLevel =0;

	@Parameter(
		names = {"-s", "--single-thread"},
		description = "Single-threaded mode with intermediate files (for diagnostics)"
	)
	boolean singleThread;

	@Parameter(
		required = true,
		converter = BookConverter.class,
		description = "[list of book IDs | 'all']"
	)
	List<Book> books = new ArrayList<Book>();

	public static class DownloadDirConverter implements IStringConverter<File> {
		public File convert(String value) {
			File downloadDir = new File(value);
			if (! downloadDir.isDirectory())
				throw new ParameterException("download directory '" + value + "' does not exist");
			return downloadDir;
		}
	}

	public static class LogLevelValidator implements IParameterValidator {
		public void validate(String name, String value) throws ParameterException {
			int logLevel;
			try {
				logLevel = Integer.parseInt(value);
			}
			catch (NumberFormatException e) {
				throw new ParameterException("invalid log level '" + value + "', must be an integer in [0..2]");
			}
			if (logLevel < 0 || logLevel > 2)
				throw new ParameterException("invalid log level " + value + ", must be in [0..2]");
		}
	}

	public static class BookConverter implements IStringConverter<Book> {
		public Book convert(String value) {
			if ("all".equals(value)) {
				// null is a magic value for "all books"
				return null;
			}
			try {
				return Book.valueOf(value.toUpperCase());
			}
			catch (IllegalArgumentException e) {
				throw new ParameterException("illegal book ID '" + value + "'");
			}
		}
	}
}
