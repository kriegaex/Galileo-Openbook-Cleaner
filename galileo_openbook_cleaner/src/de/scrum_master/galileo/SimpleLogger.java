package de.scrum_master.galileo;

import java.io.PrintStream;

// TODO: This class could be replaced by java.util.logging.Logger,
// but it is even simpler, so we keep it for the moment.

public class SimpleLogger
{
	static boolean ECHO    = true;
	static boolean VERBOSE = false;
	static boolean DEBUG   = false;
	static boolean ERROR   = true;
	static boolean TIME    = true;

	static void echo   (String message) { log(System.out, ECHO,    message); }
	static void verbose(String message) { log(System.out, VERBOSE, message); }
	static void debug  (String message) { log(System.out, DEBUG,   message); }
	static void error  (String message) { log(System.err, ERROR,   message); }

	static void time   (String header, long milliSeconds) {
		log(System.out, TIME, header + ": " + milliSeconds / 1000.0 + " s");
	}

	private static void log(PrintStream channel, boolean isActive, String message) {
		if (isActive)
			channel.println(message);
	}
}
