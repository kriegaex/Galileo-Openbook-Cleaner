package de.scrum_master.galileo;

import java.io.File;

import de.scrum_master.util.SimpleLogger;

public privileged aspect TimingAspect
{
	// Timing should wrap around logging
	declare precedence: TimingAspect, LoggingAspect;

	void around(String[] argv) : execution(* OpenbookCleaner.main(String[])) && args(argv) {
		long startTime = System.currentTimeMillis();
		proceed(argv);
		SimpleLogger.time("Total duration", System.currentTimeMillis() - startTime);
	}
	
	void around(Book book) : execution(* OpenbookCleaner.downloadAndCleanBook(Book)) && args(book) {
		long startTime = System.currentTimeMillis();
		proceed(book);
		SimpleLogger.time("Duration for " + book.unpackDirectory, System.currentTimeMillis() - startTime);
		SimpleLogger.echo("");
	}
}
