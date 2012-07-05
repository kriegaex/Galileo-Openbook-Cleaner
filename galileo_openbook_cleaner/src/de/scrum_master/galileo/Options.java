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
		//PARSER.setColumnSize(99);
	}

	private Options() { /* This is a singleton! */ }

	@Parameter(
		names = {"-?", "--help"}, help = true,
		description = "Display this help text")
	boolean showHelp = false;

	@Parameter(
		names = {"-d", "--download-dir"}, converter = DownloadDirConverter.class,
		description = "Download directory for openbook archives (*.zip); must exist")
	File downloadDir = new File(".");

	@Parameter(
		names = {"-p", "--pretty-print"}, validateWith = PrettyPrintModeValidator.class,
		description = "Pretty-print after clean-up (0=no, 1=yes); no saves ~15% processing time")
	int prettyPrint = 1;

	@Parameter(
		names = {"-l", "--log-level"}, validateWith = LogLevelValidator.class,
		description = "Log level (0=normal, 1=verbose, 2=debug)")
	int logLevel =0;

	@Parameter(
		names = {"-t", "--threading"}, validateWith = ThreadingModeValidator.class,
		description = "Threading mode (0=single, 1=multi); single is slower, but better for diagnostics)")
	int threadingMode = 1;

	@Parameter(
		required = true, converter = BookConverter.class,
		description = "[list of book IDs | 'all']")
	List<Book> books = new ArrayList<Book>();

	public static class DownloadDirConverter implements IStringConverter<File> {
		public File convert(String value) {
			File downloadDir = new File(value);
			if (! downloadDir.isDirectory())
				throw new ParameterException("download directory '" + value + "' does not exist");
			return downloadDir;
		}
	}

	public static class PrettyPrintModeValidator implements IParameterValidator {
		public void validate(String name, String value) throws ParameterException {
			int prettyPrintMode = -1;
			try { prettyPrintMode = Integer.parseInt(value); }
			catch (NumberFormatException e) {}
			if (prettyPrintMode < 0 || prettyPrintMode > 1)
				throw new ParameterException("invalid pretty-print mode " + value + ", must be 0 or 1");
		}
	}

	public static class LogLevelValidator implements IParameterValidator {
		public void validate(String name, String value) throws ParameterException {
			int logLevel = -1;
			try { logLevel = Integer.parseInt(value); }
			catch (NumberFormatException e) {}
			if (logLevel < 0 || logLevel > 2)
				throw new ParameterException("invalid log level " + value + ", must be in [0..2]");
		}
	}

	public static class ThreadingModeValidator implements IParameterValidator {
		public void validate(String name, String value) throws ParameterException {
			int threadingMode = -1;
			try { threadingMode = Integer.parseInt(value); }
			catch (NumberFormatException e) {}
			if (threadingMode < 0 || threadingMode > 1)
				throw new ParameterException("invalid threading mode " + value + ", must be 0 or 1");
		}
	}

	public static class BookConverter implements IStringConverter<Book> {
		public Book convert(String value) {
			if ("all".equalsIgnoreCase(value)) {
				// magic value for "all books"
				return null;
			}
			try {
				return Book.valueOf(value.toUpperCase());
			}
			catch (IllegalArgumentException e) {
				throw new ParameterException("invalid book ID '" + value + "'");
			}
		}
	}
}
