package de.scrum_master.galileo;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.Serializer;
import nu.xom.Text;
import nu.xom.XPathContext;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class XOMClutterRemover extends BasicConverter
{
	private boolean  isTOCFile;                  // TOC = table of contents = index.htm*
	private XMLReader tagsoup;                    // plug-in XML reader for XOM
	private Builder   builder;                    // XOM document builder
	private Document  document;                   // XOM document (XML DOM structure)
	private Element   bodyTag;                    // XOM element pointing to HTML <body> tag
	private String    pageTitle;                  // Content of HTML <title> tag 
	private boolean  hasStandardLayout = true;  // Known exception: "UNIX guru" book

	private static final XPathContext context = // XOM XPath context for HTML
		new XPathContext("html", "http://www.w3.org/1999/xhtml");
	private static final Pattern REGEX_HREF =   // Find subchapter no. in TOC link target
		Pattern.compile("(.*_[0-9a-h]+_(?:[a-z0-9]+_)*)([0-9]+)(\\.htm.*)");
	private static final Pattern REGEX_TEXT =   // Find subchapter no. in TOC linkt title
		Pattern.compile("^([0-9A-H]+\\.)([0-9]+)(.*)");

	private static enum XPath                   // XPath query strings mapped to symbolic names
	{
		TITLE                          ("//html:head/html:title"),
		SCRIPTS                        ("//html:script"),

		BODY                           ("//html:body"),
		BODY_NODES                     ("//html:body/node()"),

		NON_STANDARD_MAIN_CONTENT      ("//html:td[@class='buchtext']/node()"),
		HEADING_1_TO_4                 ("*[name()='h1' or name()='h2' or name()='h3' or name()='h4']"),
		NON_STANDARD_TOP_NAVIGATION    ("//html:body/" + HEADING_1_TO_4.query + "[1]/preceding-sibling::node()"),
		NON_STANDARD_BOTTOM_NAVIGATION ("//html:div[@class='navigation']"),

		GREY_TABLE                     ("//html:table[@bgcolor='#EEEEEE' or @bgcolor='#eeeeee']"),
		MAIN_CONTENT_1                 (GREY_TABLE.query + "/html:tr/html:td/html:div[@class='main']/node()|" +
		                                GREY_TABLE.query + "/html:tbody/html:tr/html:td/html:div[@class='main']/node()"),
		MAIN_CONTENT_2                 (GREY_TABLE.query + "/html:tr/html:td/node()|" +
		                                GREY_TABLE.query + "/html:tbody/html:tr/html:td/node()"),

		JUMP_TO_TOP_LINK               ("//html:div/html:a[@href='#top']/.."),
		GRAPHICAL_PARAGRAPH_SEPARATOR  ("//html:div/html:img[@src='common/jupiters.gif']/.."),

		LAST_HR_TAG                    ("//html:hr[position()=last()]"),
		AFTER_LAST_HR_TAG              (LAST_HR_TAG.query + "/following-sibling::node()"),
		FEEDBACK_FORM                  (LAST_HR_TAG.query + "|" + AFTER_LAST_HR_TAG.query), 
		FEEDBACK_FORM_URL_FIELD        (AFTER_LAST_HR_TAG.query + "//html:input[@name='openbookurl']"),

		IMAGE_1                        ("//html:div[@class='bildbox']//html:img"),
		IMAGE_2                        ("//html:td[@class='tabellentext']//html:img"),
		IMAGE_3                        ("//html:a[@href='#bild']/html:img"),
		IMAGE_4                        ("//html:a[@rel='lightbox']/html:img"),
		IMAGE_BOX_1                    ("//html:div[@class='bildbox']"),
		IMAGE_BOX_2                    ("//html:td[@class='tabellentext']//html:img/../.."),
		IMAGE_BOX_3                    ("//html:a[@href='#bild']/html:img/../.."),
		IMAGE_BOX_4                    ("//html:a[@rel='lightbox']"),

		TOC_HEADING_2                  ("//html:h2/html:a/.."),
		INDEX_LINK                     ("//html:a[contains(@href,'stichwort.htm')]"),
		AFTER_INDEX_LINK               (INDEX_LINK.query + "/following::node()"),
		ALL_LINKS                      ("//html:a");

		private final String query;

		private XPath(String query)
		{
			this.query = query;
		}
	}

	public XOMClutterRemover(InputStream in, OutputStream out, File origFile)
		throws SAXException
	{
		super(in, out, origFile, "Removing clutter (header, footer, navigation, ads) and fixing structure");
		isTOCFile = origFile.getName().startsWith("index.htm");
		tagsoup = XMLReaderFactory.createXMLReader("org.ccil.cowan.tagsoup.Parser");
		builder = new Builder(tagsoup);
	}

	@Override
	protected void convert() throws Exception
	{
		parseDocument();
		removeClutter();
		fixStructure();
		writeDocument();
	}

	private void parseDocument() throws Exception
	{
		document = builder.build(in);
		bodyTag = (Element) xPathQuery(XPath.BODY.query).get(0);
		pageTitle = xPathQuery(XPath.TITLE.query).get(0).getValue();
	}

	private void removeClutter()
	{
		fixNode429();
		removeClutterAroundMainContent();
		removeClutterWithinMainContent();
	}

	private void fixStructure()
	{
		if (!hasStandardLayout) {
			bodyTag.addAttribute(new Attribute("style", "font-size: 13px"));
			return;
		}

		overrideBackgroundImage();
		fixImages();
		removeRedundantGreyTable();

		if (isTOCFile) {
			if (! hasIndexLink())
				createIndexLink();
			fixFaultyLinkTargets();
			removeContentAfterIndexLink();
		}
	}

	private void writeDocument() throws Exception
	{
		new Serializer(out, "ISO-8859-1").write(document);
	}

	/*
	 * Individual fix for a buggy heading in "Unix Guru" book's node429.html
	 * which would later make deletion of XPath.NON_STANDARD_TOP_NAVIGATION fail
	 * in method removeClutterWithinMainContent(). 
	 */
	private void fixNode429()
	{
		if (! (origFile.getName().equals("node429.html") && pageTitle.contains("UNIX-Guru")))
			return;
		SimpleLogger.verbose("      Fixing buggy heading...");
		Element buggyParagraph = (Element) xPathQuery("//html:p[contains(text(),'gpGlossar18133')]").get(0);
		Element heading = new Element("h1");
		//heading.appendChild("unix");
		bodyTag.appendChild(heading);
		Element link = new Element("a");
		link.appendChild("unix");
		heading.appendChild(link);
		replaceNodeBy(buggyParagraph, heading);
	}

	private void removeClutterAroundMainContent()
	{
		// Keep JavaScript for source code colouring ('prettyPrint' function) in some books
		// deleteNodes(XPath.SCRIPTS.query);

		Nodes mainContent = xPathQuery(XPath.NON_STANDARD_MAIN_CONTENT.query);
		if (mainContent.size() > 0)
			hasStandardLayout = false;
		else {
			mainContent = xPathQuery(XPath.MAIN_CONTENT_1.query);
			if (mainContent.size() == 0)
				mainContent = xPathQuery(XPath.MAIN_CONTENT_2.query);
		}
		deleteNodes(XPath.BODY_NODES.query);
		moveNodesTo(mainContent, bodyTag);
	}
	
	private void removeClutterWithinMainContent()
	{
		if (hasStandardLayout) {
			deleteNodes(XPath.JUMP_TO_TOP_LINK.query);
			deleteNodes(XPath.GRAPHICAL_PARAGRAPH_SEPARATOR.query);
			if (xPathQuery(XPath.FEEDBACK_FORM_URL_FIELD.query).size() > 0)
				deleteNodes(XPath.FEEDBACK_FORM.query);
		}
		else {
			deleteNodes(XPath.NON_STANDARD_TOP_NAVIGATION.query);
			deleteNodes(XPath.NON_STANDARD_BOTTOM_NAVIGATION.query);
		}
	}

	private void overrideBackgroundImage()
	{
		bodyTag.addAttribute(new Attribute("style", "background: none"));
	}

	private void fixImages()
	{
		Nodes images1 = xPathQuery(XPath.IMAGE_1.query);
		replaceByBigImages(images1);
		Nodes images2 = xPathQuery(XPath.IMAGE_2.query);
		replaceByBigImages(images2);
		Nodes images3 = xPathQuery(XPath.IMAGE_3.query);
		replaceByBigImages(images3);
		Nodes images4 = xPathQuery(XPath.IMAGE_4.query);
		replaceByBigImages(images4);

		replaceBoxesByImages(xPathQuery(XPath.IMAGE_BOX_1.query), images1);
		replaceBoxesByImages(xPathQuery(XPath.IMAGE_BOX_2.query), images2);
		replaceBoxesByImages(xPathQuery(XPath.IMAGE_BOX_3.query), images3);
		replaceBoxesByImages(xPathQuery(XPath.IMAGE_BOX_4.query), images4);
	}

	/*
	 * There is one known occurrence (the "PHP PEAR" book) where there are two grey tables
	 * (background colour #eeeeee) within one document. It is actually a bug in the book's
	 * TOC (index.htm) because there are three lines of HTML code which are repeated erroneously.
	 * JTidy interprets them as two nested tables, handling them gracefully. But after
	 * removeClutterAroundMainContent() there still is a leftover grey table which needs
	 * to be removed. This is done here.
	 */
	private void removeRedundantGreyTable()
	{
		deleteNodes(XPath.GREY_TABLE.query);
	}

	/*
	 * Find out if this page contains a link to the index (stichwort.htm*).
	 */
	private boolean hasIndexLink()
	{
		return xPathQuery(XPath.INDEX_LINK.query).size() > 0;
	}

	/**
	 * Many Galileo Openbooks' tables of contents (TOC, index.htm*) are missing
	 * links to their respective indexes (stichwort.htm*).
	 * 
	 * This is a problem because after clean-up there is no direct way to reach
	 * the index other than from the TOC. This also results in missing pages within
	 * EPUB books created by Calibre, for example. So we need to do something about it,
	 * i.e. insert missing links at the end of the respective TOC.
	 */
	private void createIndexLink()
	{
		if (pageTitle.contains("Ruby on Rails")) {
			SimpleLogger.verbose("      TOC file: not creating index link (no stichwort.htm*");
			return;
		}
		SimpleLogger.verbose("      TOC file: creating index link...");
		Element indexLink = (Element) xPathQuery(XPath.TOC_HEADING_2.query).get(0).copy();
		String fileExtension = ".htm";
		if (((Element) indexLink.getChild(0)).getAttribute("href").getValue().contains(".html"))
			fileExtension = ".html";
		((Element) indexLink.getChild(0)).getAttribute("href").setValue("stichwort" + fileExtension);
		((Text) indexLink.getChild(0).getChild(0)).setValue("Index");
		bodyTag.appendChild(indexLink);

	}

	/**
	 * There is a strange quirk in the table of contents (TOC, index.htm*) of
	 * several (ca. 10) Galileo Openbooks:
	 * 
	 * Some links for subchapters *.x point to the file for subchapter *.(x-1).
	 * The problem there is that after we have removed the surrounding clutter,
	 * there is no more redundant TOC column on the left, so there is no direct way
	 * to reach the missing chapters which have no reference in the TOC. This also
	 * results in missing pages within EPUB books created by Calibre, for example.
	 * So we need to do something about it, i.e. detect and fix the faulty links.
	 *  
	 * Faulty example (abbreviated) from "Ubuntu 11.04" book:
	 * <pre>
	 * &lt;a href="ubuntu_01_001.htm"&gt;1.2.* Blah&lt;/a&gt;
	 * </pre>
	 * For chapter x.2.* the href must be corrected to ubuntu_0x_002.htm.
	 * 
	 * It further complicates the fixing task that there are some (ca. 2) books
	 * which show a similar one-off behaviour for <i>all</i> subchapters by design,
	 * because they have a different numbering scheme. Those books are OK, though,
	 * thus we need to explicitly exclude them from "fixing". <tt>:-(</tt>
	 */
	private void fixFaultyLinkTargets()
	{
		SimpleLogger.verbose("    Checking for faulty TOC link targets...");

		SimpleLogger.verbose("      Book title = " + pageTitle);

		// Exclude the 3 know exceptions and immediately return if one is found
		if (pageTitle.matches(".*(ActionScript 1 und 2|Microsoft-Netzwerk|Shell-Programmierung).*")) {
			SimpleLogger.verbose("      Book title is an exception - no link fixing done");
			return;
		}

		int fixedLinksCount = 0;
		Nodes links = xPathQuery(XPath.ALL_LINKS.query);
		for (int i = 0; i < links.size(); i++) {
			Element link = (Element) links.get(i);
			String href = link.getAttributeValue("href");
			String text = link.getValue();
			Matcher hrefMatcher = REGEX_HREF.matcher(href);
			Matcher textMatcher = REGEX_TEXT.matcher(text);
			if (hrefMatcher.matches() && textMatcher.matches()) {
				int hrefNumber = Integer.parseInt(hrefMatcher.group(2));
				int textNumber = Integer.parseInt(textMatcher.group(2));
				if (hrefNumber != textNumber) {
					SimpleLogger.debug("        Chapter " + text);
					SimpleLogger.debug("          Faulty: " + href);
					String numberFormat = "%0" + hrefMatcher.group(2).length() + "d";
					href=hrefMatcher.group(1) + String.format(numberFormat, textNumber) + hrefMatcher.group(3);
					SimpleLogger.debug("          Fixed:  " + href);
					link.getAttribute("href").setValue(href);
					fixedLinksCount++;
				}
			}
		}
		SimpleLogger.verbose("    Number of fixed links = " + fixedLinksCount);
	}

	/*
	 * There is one known occurrence (the "JavaScript and AJAX" book) where there is
	 * erroneous trailing text after the last TOC entry (the index link pointing to
	 * stichwort.htm*). Because it looks ugly, we remove everything after the index link.
	 */
	private void removeContentAfterIndexLink()
	{
		deleteNodes(XPath.AFTER_INDEX_LINK.query);
	}

	private static void replaceByBigImages(Nodes smallImages)
	{
		for (int i = 0; i < smallImages.size(); i++) {
			Attribute imageSrc = ((Element) smallImages.get(i)).getAttribute("src");
			imageSrc.setValue(imageSrc.getValue().replaceFirst("klein/klein", "/"));
		}
	}

	private static void replaceBoxesByImages(Nodes smallImageBoxes, Nodes smallImages)
	{
		for (int i = 0; i < smallImageBoxes.size(); i++)
			replaceNodeBy(smallImageBoxes.get(i), smallImages.get(i));
	}

	/*
	 * ============================================================================================
	 * GENERAL PURPOSE HELPER METHODS
	 * ============================================================================================
	 */

	private Nodes xPathQuery(String query)
	{
		return document.query(query, context);
	}

	private static void deleteNodes(Nodes nodes)
	{
		for (int i = 0; i < nodes.size(); i++)
			nodes.get(i).detach();
	}

	private void deleteNodes(String xPathQuery)
	{
		deleteNodes(xPathQuery(xPathQuery));
	}

	private static void moveNodesTo(Nodes sourceNodes, Element targetElement)
	{
		for (int i = 0; i < sourceNodes.size(); i++) {
			sourceNodes.get(i).detach();
			targetElement.appendChild(sourceNodes.get(i));
		}
	}

	private static void replaceNodeBy(Node original, Node replacement)
	{
		replacement.detach();
		original.getParent().replaceChild(original, replacement);
	}
}
