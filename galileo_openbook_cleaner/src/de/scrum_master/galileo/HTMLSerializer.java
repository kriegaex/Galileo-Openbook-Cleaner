package de.scrum_master.galileo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import nu.xom.Element;
import nu.xom.Serializer;

public class HTMLSerializer extends Serializer
{
	@Override
	protected void writeEmptyElementTag(Element element) throws IOException
	{
		// Some HTML tags (e.g. "script", "p") must not use minimized empty tags
		// like "<script />", but explicit closing tags like "<script></script>". 
		super.writeStartTag(element);
		super.writeEndTag(element);
	}

	public HTMLSerializer(OutputStream out, String encoding)
		throws UnsupportedEncodingException
	{
		super(out, encoding);
	}

	public HTMLSerializer(OutputStream out)
	{
		super(out);
	}
}
