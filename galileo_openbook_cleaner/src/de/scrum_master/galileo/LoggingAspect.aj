package de.scrum_master.galileo;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Stack;

import org.aspectj.lang.reflect.AdviceSignature;

import de.scrum_master.util.SimpleLogger;
import de.scrum_master.util.SimpleLogger.LogType;
import de.scrum_master.galileo.filter.*;

privileged aspect LoggingAspect
{
	private Stack<String> messageStack = new Stack<String>();
	private String message;

	pointcut processBook()          : execution(* OpenbookCleaner.downloadAndCleanBook(Book));
	pointcut download()             : execution(* Downloader.download());
	pointcut cleanBook()            : execution(* OpenbookCleaner.cleanBook(Book));
	pointcut cleanChapter()         : execution(* OpenbookCleaner.cleanChapter(Book, File));
	pointcut runFilter()            : execution(* BasicFilter.run());
	pointcut initialiseTitle()      : execution(* *Filter.initialiseTitle(boolean));
	pointcut createIndexLink()      : execution(* *Filter.createIndexLink());
	pointcut fixFaultyLinkTargets() : execution(* *Filter.fixFaultyLinkTargets());
	pointcut removeFeedbackForm()   : execution(* *Filter.removeFeedbackForm());

	@Retention(RetentionPolicy.RUNTIME)
	private @interface Log {
		boolean logDone() default true;
		boolean indent()  default true;
		LogType type()    default LogType.VERBOSE;
	}

	after() : set(String LoggingAspect.message) {
		messageStack.push(message);
		Method advice = ((AdviceSignature) thisEnclosingJoinPointStaticPart.getSignature()).getAdvice();
		Log logOptions = advice.getAnnotation(Log.class);
		SimpleLogger.log(logOptions.type(), message);
		if (logOptions.indent())
			SimpleLogger.indent();
	}

	after() : @annotation(Log) {
		String message = messageStack.pop();
		Method advice = ((AdviceSignature) thisJoinPointStaticPart.getSignature()).getAdvice();
		Log logOptions = advice.getAnnotation(Log.class);
		if (logOptions.indent())
			SimpleLogger.dedent();
		if (logOptions.logDone())
			SimpleLogger.log(logOptions.type(), message + " - done");
	}

	@Log void around(Book book) : processBook() && args(book) {
		message = "Book: " + book.unpackDirectory; proceed(book);
	}

	@Log void around() : download() {
		message = "Downloading, verifying (MD5) and unpacking"; proceed();
	}

	@Log void around() : cleanBook() {
		message = "Filtering"; proceed();
	}

	@Log void around(File origFile) : cleanChapter() && args(*, origFile) {
		message = "Chapter: " + origFile.getName(); proceed(origFile);
	}

	@Log void around(BasicFilter filter) : runFilter() && this(filter) {
		message = filter.getLogMessage(); proceed(filter);
	}

	@Log(logDone = false) void around() : initialiseTitle() {
		message = "Initialising page title"; proceed();
	}

	@Log(logDone = false) void around() : createIndexLink() {
		message = "TOC file: creating index link"; proceed();
	}

	@Log void around() : fixFaultyLinkTargets() {
		message = "TOC file: checking for faulty link targets"; proceed();
	}
	
	@Log(logDone = false) void around() : removeFeedbackForm() {
		message = "Removing feedback form (if any)"; proceed();
	}
}
