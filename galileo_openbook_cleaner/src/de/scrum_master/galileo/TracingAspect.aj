package de.scrum_master.galileo;

import de.scrum_master.util.BasicTracingAspect;
import de.scrum_master.util.SimpleLogger;

import java.io.BufferedOutputStream;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * This aspect concretizes the abstract crosscut in BasicTracingAspect, applying the trace
 * facility to the application classes.
 */
public aspect TracingAspect extends BasicTracingAspect
{
	// Tracing should wrap around timing, timing around logging
	declare precedence: TracingAspect, TimingAspect, LoggingAspect;

	protected static boolean isActive() {
		return OpenbookCleaner.options == null
			? false
			: OpenbookCleaner.options.logLevel > 2;
	}

	protected pointcut myClass():
		if(isActive()) && within(de.scrum_master..*) &&
			!within(*Aspect) && !within(Options) && !within(FileFilter+) && !within(Book) && !within(SimpleLogger);

	public static void main(String[] args) throws Exception {
		BasicTracingAspect.TRACELEVEL = 2;
		BasicTracingAspect.initStream(
			new PrintStream(new BufferedOutputStream(new FileOutputStream("tracing.log")))
		);
		String[] myArgs= {
			"-d", ".",
			"-t1", "-l3",
			"shell_prog"//, "oop"
		};
		OpenbookCleaner.main(myArgs);
		BasicTracingAspect.stream.close();
	}
}
