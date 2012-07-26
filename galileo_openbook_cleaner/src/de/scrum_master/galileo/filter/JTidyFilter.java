package de.scrum_master.galileo.filter;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.w3c.tidy.Tidy;


public class JTidyFilter extends BasicFilter
{
	private Tidy tidy = new Tidy();

	protected static final String FILE_EXTENSION = ".tidy";

	public JTidyFilter(InputStream in, OutputStream out, File origFile) {
		super(in, out, origFile);

		tidy.setXHTML(true);
		tidy.setQuiet(true);
		tidy.setShowErrors(0);
		tidy.setShowWarnings(false);
		tidy.setForceOutput(true);
		tidy.setWraplen(0);
		tidy.setSmartIndent(true);
		tidy.setTidyMark(false);
		tidy.setEscapeCdata(false);
		tidy.setIndentCdata(false);
	}

	@Override
	protected void filter() throws Exception {
		tidy.parse(in, out);
	}

	protected String getLogMessage() {
		return "Converting to clean, pretty-printed XHTML";
	}
}
