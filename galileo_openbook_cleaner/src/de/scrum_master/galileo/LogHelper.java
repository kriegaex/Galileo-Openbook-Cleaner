package de.scrum_master.galileo;

import de.scrum_master.util.SimpleLogger.LogType;

abstract class LogHelper {
	String message;
	boolean logDone;
	boolean indent;
	LogType type;

	LogHelper(String message, boolean logDone, boolean indent, LogType type) {
		this.message = message;
		this.logDone = logDone;
		this.indent = indent;
		this.type = type;
	}
	LogHelper(String message, boolean logDone) {
		this(message, logDone, true, LogType.VERBOSE);
	}
	LogHelper(String message) {
		this(message, true);
	}

	abstract void log();
}
