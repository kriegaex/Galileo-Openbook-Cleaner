package de.scrum_master.galileo.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import de.scrum_master.util.SimpleLogger;

/*
 * Currently this class only fixes one specific file: <i>ruby_on_rails/index.htm</i>.
 * If different types of pre-Tidy fixing should ever be needed, refactor this class into
 * a base class with specialised subclasses.
 */
public class PreJTidyFilter extends BasicFilter
{
	private static final String REGEX_MAIN_TABLE = "<table .*bgcolor=.#eeeeee.*";

	protected BufferedReader input;
	protected PrintStream output;
	protected String line;

	public PreJTidyFilter(InputStream in, OutputStream out, File origFile)
	{
		super(in, out, origFile, "Fixing HTML so as to enable JTidy to parse it");
		input = new BufferedReader(new InputStreamReader(in));
		output = new PrintStream(out);
	}

	@Override
	protected void filter() throws Exception
	{
		while ((line = input.readLine()) != null) {
			if (line.matches(REGEX_MAIN_TABLE)) {
				SimpleLogger.debug("    Found main content table, inserting missing </table> tag before it");
				output.println("</table>");
			}
			output.println(line);
		}
	}
}
