package de.scrum_master.galileo;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import org.aspectj.lang.reflect.AdviceSignature;

import de.scrum_master.util.SimpleLogger;
import de.scrum_master.util.SimpleLogger.LogType;
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

	// ATTENTION: each new pointcut above (except processBook) must also be added here
	pointcut indentLog() :
		download() || cleanBook() || cleanChapter() || runFilter() || initialiseTitle() ||
		createIndexLink() || fixFaultyLinkTargets() || removeFeedbackForm();

	void around() : indentLog() {
		SimpleLogger.indent();
		proceed();
		SimpleLogger.dedent();
	}

	@Retention(RetentionPolicy.RUNTIME)
	private @interface Log {
		String  message() default "";
		LogType type()    default LogType.VERBOSE;
		boolean logDone() default false;
	}

	pointcut printLog() : @annotation(Log);
	
	void around() : printLog() {
		Method advice = ((AdviceSignature) thisJoinPointStaticPart.getSignature()).getAdvice();
		Log logOptions = advice.getAnnotation(Log.class);
		String message = logOptions.message();
		if (!"".equals(message))
			SimpleLogger.log(logOptions.type(), message);
		proceed();
		if (logOptions.logDone() && !"".equals(message))
			SimpleLogger.log(logOptions.type(), message + " - done");
	}

	// Dynamic, manual logging (messages computed during runtime)

	@Log
	void around(Book book) : processBook() && args(book) {
		String message = "Book: " + book.unpackDirectory;
		SimpleLogger.echo(message);
		proceed(book);
		SimpleLogger.echo(message + " - done");
	}

	@Log
	void around(File origFile) : cleanChapter() && args(*, origFile) {
		String message = "Chapter: " + origFile.getName();
		SimpleLogger.verbose(message);
		proceed(origFile);
		SimpleLogger.verbose(message + " - done");
	}

	@Log
	void around(BasicFilter filter) : runFilter() && this(filter) {
		SimpleLogger.verbose(filter.getLogMessage());
		proceed(filter);
	}

	// Simple, declarative logging (hard-coded messages)

	@Log(message = "Downloading, verifying (MD5) and unpacking", logDone = true)
	void around() : download() { proceed(); }

	@Log(message = "Filtering", logDone = true)
	void around() : cleanBook() { proceed(); }

	@Log(message = "Initialising page title")
	void around() : initialiseTitle() { proceed(); }

	@Log(message = "TOC file: creating index link")
	void around() : createIndexLink() { proceed(); }

	@Log(message = "TOC file: checking for faulty link targets")
	void around() : fixFaultyLinkTargets() { proceed(); }
	
	@Log(message = "Removing feedback form (if any)")
	void around() : removeFeedbackForm() { proceed(); }
}
