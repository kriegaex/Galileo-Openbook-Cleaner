package de.scrum_master.galileo;

import java.io.File;

import de.scrum_master.util.SimpleLogger;
import de.scrum_master.galileo.filter.*;

privileged aspect LoggingAspect
{
	pointcut processBook()          : execution(* OpenbookCleaner.downloadAndCleanBook(Book));
	pointcut download()             : execution(* Downloader.download());
	pointcut cleanBook()            : execution(* OpenbookCleaner.cleanBook(Book));
	pointcut cleanChapter()         : execution(* OpenbookCleaner.cleanChapter(Book, File));
	pointcut runFilter()            : execution(* BasicFilter.run());
	pointcut initialiseTitle()      : execution(* *Filter.initialiseTitle(boolean));
	pointcut createIndexLink()      : execution(* *Filter.createIndexLink());
	pointcut fixFaultyLinkTargets() : execution(* *Filter.fixFaultyLinkTargets());
	pointcut removeFeedbackForm()   : execution(* *Filter.removeFeedbackForm());
	
	// ATTENTION: each new pointcut above must also be added here
	pointcut catchAll() :
		processBook() || download() || cleanBook() || cleanChapter() || runFilter() || initialiseTitle() ||
		createIndexLink() || fixFaultyLinkTargets() || removeFeedbackForm();

	// This advice takes care of indentation, so as to avoid duplicate code in the other ones
	void around() : catchAll() {
		SimpleLogger.indent();
		proceed();
		SimpleLogger.dedent();
	}

	void around(Book book) : processBook() && args(book) {
		String message = "Book: " + book.unpackDirectory;
		SimpleLogger.echo(message);
		proceed(book);
		SimpleLogger.echo(message + " - done");
	}
	
	void around() : download() {
		String message = "Downloading, verifying (MD5) and unpacking";
		SimpleLogger.verbose(message);
		proceed();
		SimpleLogger.verbose(message + " - done");
	}
	
	void around(Book book) : cleanBook() && args(book) {
		String message = "Filtering";
		SimpleLogger.verbose(message);
		proceed(book);
		SimpleLogger.verbose(message + " - done");
	}

	void around(Book book, File origFile) : cleanChapter() && args(book, origFile) {
		String message = "Chapter: " + origFile.getName();
		SimpleLogger.verbose(message);
		proceed(book, origFile);
		SimpleLogger.verbose(message + " - done");
	}

	void around(BasicFilter filter) : runFilter() && this(filter) {
		SimpleLogger.verbose(filter.getLogMessage());
		proceed(filter);
	}

	void around(boolean removeBookTitle) : initialiseTitle() && args(removeBookTitle) {
		SimpleLogger.verbose("Initialising page title");
		proceed(removeBookTitle);
	}

	void around() : createIndexLink() {
		SimpleLogger.verbose("TOC file: creating index link");
		proceed();
	}

	void around(BasicFilter filter) : fixFaultyLinkTargets()  && this(filter) {
		SimpleLogger.verbose("TOC file: checking for faulty link targets");
		proceed(filter);
	}
	
	void around() : removeFeedbackForm() {
		SimpleLogger.verbose("Removing feedback form (if any)");
		proceed();
	}
}
