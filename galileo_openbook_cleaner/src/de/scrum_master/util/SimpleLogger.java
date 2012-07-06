package de.scrum_master.util;

import java.io.PrintStream;

// TODO: This class could be replaced by java.util.logging.Logger,
// but it is even simpler, so we keep it for the moment.

public class SimpleLogger
{
	public static boolean ECHO    = true;
	public static boolean VERBOSE = false;
	public static boolean DEBUG   = false;
	public static boolean ERROR   = true;
	public static boolean TIME    = true;

	public static boolean LOG_THREAD_ID = false;

	public static enum IndentMode { UNCHANGED, INDENT_BEFORE, INDENT_AFTER, DEDENT_BEFORE, DEDENT_AFTER }

	private static InheritableThreadLocal<Integer> indentLevel =
		new InheritableThreadLocal<Integer>() {
			@Override protected Integer initialValue() { return 0; }
		};

	private static InheritableThreadLocal<String> indentText =
		new InheritableThreadLocal<String>() {
			@Override protected String initialValue() { return ""; }
		};

	public static void echo   (String message) { echo   (message, IndentMode.UNCHANGED); }
	public static void verbose(String message) { verbose(message, IndentMode.UNCHANGED); }
	public static void debug  (String message) { debug  (message, IndentMode.UNCHANGED); }
	public static void error  (String message) { error  (message, IndentMode.UNCHANGED); }

	public static void echo   (String message, IndentMode indentMode) { log(System.out, ECHO,    message, indentMode); }
	public static void verbose(String message, IndentMode indentMode) { log(System.out, VERBOSE, message, indentMode); }
	public static void debug  (String message, IndentMode indentMode) { log(System.out, DEBUG,   message, indentMode); }
	public static void error  (String message, IndentMode indentMode) { log(System.err, ERROR,   message, indentMode); }

	public static void time(String header, long milliSeconds) {
		log(System.out, TIME, header + ": " + milliSeconds / 1000.0 + " s");
	}

	public static void indent() {
		indentLevel.set(indentLevel.get() + 1);
		indentText.set(indentText.get() + "  ");
	}

	public static void dedent() {
		if (!indentLevel.get().equals(0)) {
			indentLevel.set(indentLevel.get() - 1);
			indentText.set(indentText.get().substring(2));
		}
	}

	private static void log(PrintStream channel, boolean isActive, String message) {
		log(channel, isActive, message, IndentMode.UNCHANGED);
	}
	private static void log(PrintStream channel, boolean isActive, String message, IndentMode indentMode) {
		if (!isActive)
			return;
		if (indentMode == IndentMode.INDENT_BEFORE)
			indent();
		else if (indentMode == IndentMode.DEDENT_BEFORE)
			dedent();
		if (LOG_THREAD_ID) {
			/* Tab-separated format can be imported into Excel easily via copy & paste.
			 * Add a header row, convert into table layout and there you go: filtering, sorting etc.
			 * are at your command, making it easy to see what happens in multi-threaded mode mode.
			 * TODO: time stamps might be useful - or overkill. No need for then just now.
			 */
			channel.printf("%5d\t%s\n", Thread.currentThread().getId(), indentText.get() + message);
		}
		else
			channel.println(indentText.get() + message);
		if (indentMode == IndentMode.INDENT_AFTER)
			indent();
		else if (indentMode == IndentMode.DEDENT_AFTER)
			dedent();
	}
}
