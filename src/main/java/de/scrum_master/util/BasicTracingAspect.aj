package de.scrum_master.util;

import java.io.PrintStream;

/**
 * This aspect provides support for printing trace messages into a stream.
 * <p>
 * BasicTracingAspect messages are to be printed before and after constructors (incl.
 * [pre]initialization) and methods in the application classes.and methods
 * are executed.
 * The messages are appended with the string representation of the objects whose
 * constructors and methods are being traced.
 * <p>
 * The aspect defines one abstract pointcut for injecting the tracing functionality
 * into any application classes. To use it, provide a subclass that concretises
 * the abstract pointcut.
 */
public abstract aspect BasicTracingAspect
{
	/**
	 * There are 3 trace levels (values of TRACE_LEVEL):
	 *   0 - No messages are printed
	 *   1 - BasicTracingAspect messages are printed, but there is no indentation according to the call stack
	 *   2 - BasicTracingAspect messages are printed, and they are indented according to the call stack
	 */
	public static int TRACE_LEVEL = 2;
	private static PrintStream stream = System.err;
	private static final InheritableThreadLocal<Integer> callDepth =
		new InheritableThreadLocal<Integer>() {
			@Override protected Integer initialValue() { return 0; }
		};
	private static final InheritableThreadLocal<String> indentText =
		new InheritableThreadLocal<String>() {
			@Override protected String initialValue() { return ""; }
		};

	public static void initStream(PrintStream s) {
		stream = s;
	}

	/**
	 * Prints an "entering" message with indentation.
	 * It is intended to be called at the beginning of the blocks to be traced.
	 */
	public static void traceEntry(String str, Object o) {
		if (TRACE_LEVEL == 0)
			return;
		stream.printf("%5d\t%s%n", Thread.currentThread().getId(), indentText.get() + ">> " + str + ": " + o.toString());
		if (TRACE_LEVEL == 2) {
			callDepth.set(callDepth.get() + 1);
			indentText.set(indentText.get() + "  ");
		}
	}

	/**
	 * Prints an "exiting" message with indentation.
	 * It is intended to be called at the end of the blocks to be traced.
	 */
	public static void traceExit(String str, Object o) {
		if (TRACE_LEVEL == 0)
			return;
		if (TRACE_LEVEL == 2) {
			callDepth.set(callDepth.get() - 1);
			indentText.set(indentText.get().substring(2));
		}
		stream.printf("%5d\t%s%n", Thread.currentThread().getId(), indentText.get() + "<< " + str + ": " + o.toString());
	}

	// Concretise in subclass
	protected abstract pointcut myClass();

	// Bind "this" to variable "obj" (not applicable for pre-initialisation!) 
	pointcut myClass_This(Object obj): this(obj) && myClass();

	// We cannot use "this" in object pre-initialisation (it does not exist yet)
	pointcut myPreInitConstructor(): myClass() && preinitialization(new(..));

	// We can use "this" in object initialisation, constructor and method execution 
	pointcut myInitConstructor(Object obj): myClass_This(obj) && initialization(new(..));
	pointcut myConstructor(Object obj): myClass_This(obj) && execution(new(..));
	pointcut myMethod(Object obj): myClass_This(obj) && execution(* *(..))
		&& !execution(String toString());
		// TODO: use "&& !cflow(execution(String toString()))" if toString calls other advised methods

	before(): myPreInitConstructor() {
		// There is no "this" during pre-initialisation -> use "---" as second parameter
		BasicTracingAspect.traceEntry("[P] " + thisJoinPointStaticPart.getSignature(), "---");
	}

	after(): myPreInitConstructor() {
		// There is no "this" during pre-initialisation -> use "---" as second parameter
		BasicTracingAspect.traceExit("[P] " + thisJoinPointStaticPart.getSignature(), "---");
	}

	before(Object obj): myInitConstructor(obj) {
		BasicTracingAspect.traceEntry("[I] " + thisJoinPointStaticPart.getSignature(), obj);
	}

	after(Object obj): myInitConstructor(obj) {
		BasicTracingAspect.traceExit("[I] " + thisJoinPointStaticPart.getSignature(), obj);
	}

	before(Object obj): myConstructor(obj) {
		traceEntry("[C] " + thisJoinPointStaticPart.getSignature(), obj);
	}

	after(Object obj): myConstructor(obj) {
		traceExit("[C] " + thisJoinPointStaticPart.getSignature(), obj);
	}

	before(Object obj): myMethod(obj) {
		traceEntry("[M] " + thisJoinPointStaticPart.getSignature(), obj);
	}

	after(Object obj): myMethod(obj) {
		traceExit("[M] " + thisJoinPointStaticPart.getSignature(), obj);
	}
}
