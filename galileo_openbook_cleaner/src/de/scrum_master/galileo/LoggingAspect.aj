package de.scrum_master.galileo;

import java.io.File;

import de.scrum_master.util.SimpleLogger;
import de.scrum_master.util.SimpleLogger.IndentMode;
import de.scrum_master.galileo.filter.*;

privileged aspect LoggingAspect
{
	pointcut processBook()          : execution(* OpenbookCleaner.downloadAndCleanBook(Book));
	pointcut download()             : execution(* Downloader.download());
	pointcut cleanBook()            : execution(* OpenbookCleaner.cleanBook(Book));
	pointcut cleanChapter()         : execution(* OpenbookCleaner.cleanChapter(Book, File));
	pointcut runFilter()            : execution(* BasicFilter.run());
	pointcut initialiseTitle()      : execution(* XOMUnclutterFilter.initialiseTitle(boolean));
	pointcut createIndexLink()      : execution(* XOMUnclutterFilter.createIndexLink());
	pointcut fixFaultyLinkTargets() : execution(* XOMUnclutterFilter.fixFaultyLinkTargets());

	void around(Book book) : processBook() && args(book) {
		String message = "Book: " + book.unpackDirectory;
		SimpleLogger.echo(message, IndentMode.INDENT_AFTER);
		proceed(book);
		SimpleLogger.echo(message + " - done", IndentMode.DEDENT_BEFORE);
	}
	
	void around() : download() {
		String message = "Downloading, verifying (MD5) and unpacking";
		SimpleLogger.verbose(message, IndentMode.INDENT_AFTER);
		proceed();
		SimpleLogger.verbose(message + " - done", IndentMode.DEDENT_BEFORE);
	}
	
	void around(Book book) : cleanBook() && args(book) {
		String message = "Filtering";
		SimpleLogger.verbose(message, IndentMode.INDENT_AFTER);
		proceed(book);
		SimpleLogger.verbose(message + " - done", IndentMode.DEDENT_BEFORE);
	}

	void around(Book book, File origFile) : cleanChapter() && args(book, origFile) {
		String message = "Chapter: " + origFile.getName();
		SimpleLogger.verbose(message, IndentMode.INDENT_AFTER);
		proceed(book, origFile);
		SimpleLogger.verbose(message + " - done", IndentMode.DEDENT_BEFORE);
	}

	void around(BasicFilter filter) : runFilter() && this(filter) {
		String message = filter.getLogMessage();
		SimpleLogger.verbose(message, IndentMode.INDENT_AFTER);
		proceed(filter);
		SimpleLogger.dedent();
	}

	void around(boolean removeBookTitle) : initialiseTitle() && args(removeBookTitle) {
		String message = "Initialising page title";
		SimpleLogger.verbose(message, IndentMode.INDENT_AFTER);
		proceed(removeBookTitle);
		SimpleLogger.dedent();
	}

	void around() : createIndexLink() {
		String message = "TOC file: creating index link";
		SimpleLogger.verbose(message, IndentMode.INDENT_AFTER);
		proceed();
		SimpleLogger.dedent();
	}

	void around(XOMUnclutterFilter xomFilter) : fixFaultyLinkTargets()  && this(xomFilter) {
		String message = "TOC file: checking for faulty link targets";
		SimpleLogger.verbose(message, IndentMode.INDENT_AFTER);
		proceed(xomFilter);
		SimpleLogger.dedent();
	}
}
