package de.scrum_master.galileo;

import java.util.Arrays;

import de.scrum_master.util.SimpleLogger;

public class AllOpenbooksCleaner
{
	public static void main(String[] args) throws Exception
	{
		long startTime = System.currentTimeMillis();
		// Forward command line options to OpenbookCleaner.main, always adding current book's name at the end
		int argv = args.length;
		String[] newArgs = Arrays.copyOf(args, argv + 1);
		for (BookInfo bookInfo : BookInfo.values()) {
			newArgs[argv] = bookInfo.unpackDirectory;
			OpenbookCleaner.main(newArgs);
			SimpleLogger.echo("");
		}
		SimpleLogger.time("Total duration", System.currentTimeMillis() - startTime);
	}
}
