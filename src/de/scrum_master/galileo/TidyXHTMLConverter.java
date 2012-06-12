package de.scrum_master.galileo;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.w3c.tidy.Tidy;

public class TidyXHTMLConverter extends BasicConverter
{
	private Tidy tidy;

	public TidyXHTMLConverter(InputStream in, OutputStream out, File origFile)
	{
		super(in, out, origFile, "Converting to clean, pretty-printed XHTML");

		tidy = new Tidy();
		tidy.setXHTML(true);
		tidy.setQuiet(true);
		tidy.setShowErrors(0);
		tidy.setShowWarnings(false);
		tidy.setForceOutput(true);
		//tidy.setInputEncoding("latin1");
		//tidy.setInputEncoding("latin1");
		tidy.setWraplen(0);
		tidy.setSmartIndent(true);
		//tidy.setAsciiChars(true);
		tidy.setTidyMark(false);
		tidy.setEscapeCdata(false);
		tidy.setIndentCdata(false);
		//tidy.setQuoteAmpersand(false);
	}

	@Override
	protected void convert() throws Exception
	{
		tidy.parse(in, out);
	}
}
