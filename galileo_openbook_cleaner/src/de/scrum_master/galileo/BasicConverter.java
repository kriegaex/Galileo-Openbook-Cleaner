package de.scrum_master.galileo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class BasicConverter implements Runnable
{
	protected InputStream in;          // Where to read input from
	protected OutputStream out;        // Where to write conversion result to
	protected File origFile;           // Needed to determine special files like index.htm 
	protected String debugLogMessage;  // Written to debug log upon conversion start

	protected BasicConverter(InputStream in, OutputStream out, File origFile, String debugLogMessage)
	{
		this.in  = in;
		this.out = out;
		this.origFile = origFile;
		this.debugLogMessage = debugLogMessage;
	}

	public void run()
	{
		SimpleLogger.debug("    " + debugLogMessage + "...");
		try {
			convert();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				in.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			try {
				out.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected abstract void convert() throws Exception;
}
