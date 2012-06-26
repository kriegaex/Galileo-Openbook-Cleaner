package de.scrum_master.galileo.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.scrum_master.util.SimpleLogger;

/**
 * Currently this class only fixes one specific file: <i>ruby_on_rails_2/index.htm</i>.
 * If different types of pre-Tidy fixing should ever be needed, refactor this class into
 * a base class with specialised subclasses.
 */
public class PreJTidyFilter extends BasicFilter
{
	private static final Pattern REGEX_TITLE      = Pattern.compile("(<title>.+Ruby on Rails 2 ). Das (Entwickler.+)");
	private static final Pattern REGEX_MAIN_TABLE = Pattern.compile("<table .*bgcolor=.#eeeeee.*");

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
		Matcher matcher;
		while ((line = input.readLine()) != null) {
			if ((matcher = REGEX_TITLE.matcher(line)).matches()) {
				SimpleLogger.debug("      Found title tag, removing misleading substring \" – Das\"");
				line = matcher.group(1) + matcher.group(2);
			}
			if (REGEX_MAIN_TABLE.matcher(line).matches()) {
				SimpleLogger.debug("      Found main content table, inserting missing </table> tag before it");
				output.println("</table>");
			}
			output.println(line);
		}
	}
}
