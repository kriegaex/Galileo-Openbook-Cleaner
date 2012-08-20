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
	private ThreadLocal<Stack<String>> messageStack =
		new ThreadLocal<Stack<String>>() {
			@Override protected Stack<String> initialValue() { return new Stack<String>(); }
		};
	private ThreadLocal<String> message = new ThreadLocal<String>();

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

	after() : within(LoggingAspect) && call(void java.lang.ThreadLocal.set(Object)) {
		messageStack.get().push(message.get());
		Method advice = ((AdviceSignature) thisEnclosingJoinPointStaticPart.getSignature()).getAdvice();
		Log logOptions = advice.getAnnotation(Log.class);
		SimpleLogger.log(logOptions.type(), message.get());
		if (logOptions.indent())
			SimpleLogger.indent();
	}

	after() : @annotation(Log) {
		String message = messageStack.get().pop();
		Method advice = ((AdviceSignature) thisJoinPointStaticPart.getSignature()).getAdvice();
		Log logOptions = advice.getAnnotation(Log.class);
		if (logOptions.indent())
			SimpleLogger.dedent();
		if (logOptions.logDone())
			SimpleLogger.log(logOptions.type(), message + " - done");
	}

	@Log void around(Book book) : processBook() && args(book) {
		message.set("Book: " + book.unpackDirectory); proceed(book);
	}

	@Log void around() : download() {
		message.set("Downloading, verifying (MD5) and unpacking"); proceed();
	}

	@Log void around() : cleanBook() {
		message.set("Filtering"); proceed();
	}

	@Log void around(File origFile) : cleanChapter() && args(*, origFile) {
		message.set("Chapter: " + origFile.getName()); proceed(origFile);
	}

	@Log void around(BasicFilter filter) : runFilter() && this(filter) {
		message.set(filter.getLogMessage()); proceed(filter);
	}

	@Log(logDone = false) void around() : initialiseTitle() {
		message.set("Initialising page title"); proceed();
	}

	@Log(logDone = false) void around() : createIndexLink() {
		message.set("TOC file: creating index link"); proceed();
	}

	@Log void around() : fixFaultyLinkTargets() {
		message.set("TOC file: checking for faulty link targets"); proceed();
	}
	
	@Log(logDone = false) void around() : removeFeedbackForm() {
		message.set("Removing feedback form (if any)"); proceed();
	}
}
