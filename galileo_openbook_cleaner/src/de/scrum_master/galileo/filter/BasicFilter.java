package de.scrum_master.galileo.filter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.scrum_master.util.SimpleLogger;

public abstract class BasicFilter implements Runnable
{
	protected InputStream in;          // Where to read input from
	protected OutputStream out;        // Where to write conversion result to
	protected File origFile;           // Needed to determine special files like index.htm 
	protected String debugLogMessage;  // Written to debug log upon conversion start

	protected BasicFilter(InputStream in, OutputStream out, File origFile, String debugLogMessage)
	{
		this.in  = in;
		this.out = out;
		this.origFile = origFile;
		this.debugLogMessage = debugLogMessage;
	}

	public void run()
	{
		SimpleLogger.debug("    " + debugLogMessage + "...");
		try { filter(); }
		catch (Exception e) { throw new RuntimeException(e); }
		finally {
			try { in.close(); }
			catch (IOException e) { e.printStackTrace(); }
			try { out.close(); }
			catch (IOException e) { e.printStackTrace(); }
		}
	}

	/**
	 * Encapsulates the filter action and should not be called by any method other than {@link #run}
	 *
	 * @throws Exception
	 */
	protected abstract void filter() throws Exception;
}
