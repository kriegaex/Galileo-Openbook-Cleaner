package de.scrum_master.galileo;

import java.io.File;

import de.scrum_master.util.SimpleLogger;
import de.scrum_master.galileo.filter.*;

privileged aspect LoggingAspect
{
	void around(LogHelper logHelper) : execution(* LogHelper.log()) && this(logHelper) {
		try {
			SimpleLogger.log(logHelper.type, logHelper.message);
			if (logHelper.indent)
				SimpleLogger.indent();
			proceed(logHelper);
		} finally {
			if (logHelper.indent)
				SimpleLogger.dedent();
			if (logHelper.logDone)
				SimpleLogger.log(logHelper.type, logHelper.message + " - done");
		}
	}

	pointcut processBook()          : execution(* OpenbookCleaner.downloadAndCleanBook(Book));
	pointcut download()             : execution(* Downloader.download());
	pointcut cleanBook()            : execution(* OpenbookCleaner.cleanBook(Book));
	pointcut cleanChapter()         : execution(* OpenbookCleaner.cleanChapter(Book, File));
	pointcut runFilter()            : execution(* BasicFilter.run());
	pointcut initialiseTitle()      : execution(* *Filter.initialiseTitle(boolean));
	pointcut createIndexLink()      : execution(* *Filter.createIndexLink());
	pointcut fixFaultyLinkTargets() : execution(* *Filter.fixFaultyLinkTargets());
	pointcut removeFeedbackForm()   : execution(* *Filter.removeFeedbackForm()) || execution(* *Filter.removeFeedbackLink());

	void around(final Book book) : processBook() && args(book) {
		new LogHelper("Book: " + book.unpackDirectory) {
			void log() { proceed(book); } }.log();
	}

	void around() : download() {
		new LogHelper("Downloading, verifying (MD5) and unpacking") {
			void log() { proceed(); } }.log();
	}

	void around() : cleanBook() {
		new LogHelper("Filtering") {
			void log() { proceed(); } }.log();
	}

	void around(final File origFile) : cleanChapter() && args(*, origFile) {
		new LogHelper("Chapter: " + origFile.getName()) {
			void log() { proceed(origFile); } }.log();
	}

	void around(final BasicFilter filter) : runFilter() && this(filter) {
		new LogHelper(filter.getLogMessage()) {
			void log() { proceed(filter); } }.log();
	}

	void around() : initialiseTitle() {
		new LogHelper("Initialising page title", false) {
			void log() { proceed(); } }.log();
	}

	void around() : createIndexLink() {
		new LogHelper("TOC file: creating index link", false) {
			void log() { proceed(); } }.log();
	}

	void around() : fixFaultyLinkTargets() {
		new LogHelper("TOC file: checking for faulty link targets") {
			void log() { proceed(); } }.log();
	}

	void around() : removeFeedbackForm() {
		new LogHelper("Removing feedback form/link (if any)", false) {
			void log() { proceed(); } }.log();
	}
}
