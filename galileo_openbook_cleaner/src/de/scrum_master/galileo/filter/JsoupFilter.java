package de.scrum_master.galileo.filter;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;

import de.scrum_master.galileo.Book;
import de.scrum_master.util.SimpleLogger;
import de.scrum_master.util.SimpleLogger.IndentMode;

public class JsoupFilter extends BasicFilter {
	private PrintStream output;                    // Output stream for filtered document
	private boolean     isTOCFile;                 // TOC = table of contents = index.htm*
	private Document    document;                  // Jsoup document (DOM structure)
	private Element     headTag;                   // XOM element pointing to HTML <head> tag
	private Element     bodyTag;                   // XOM element pointing to HTML <body> tag
	private String      pageTitle;                 // Content of HTML <title> tag
	private boolean     hasStandardLayout = true;  // Known exception: "UNIX guru" book

	protected static final String FILE_EXTENSION = ".jsoup";

	private static final String nonStandardCSS =    // CSS style overrides for "UNIX guru" book
		"body, p, a, tr, td, td.nav1, td.nav2, .weiter a, .zurueck a, " +
			".themen, .white, .merksatz, .warnung, .beispiel, .anleser " +
			"{ font-size: 13px; }\n" +
		"h1 a, h2 a, h3 a, h4 a, h5 a { font-size: 16px; }\n" +
		"pre, code { font-size: 12px; }\n";

	private static enum Selector                   // Selector query strings mapped to symbolic names
	{
		HEAD                           ("head"),
		TITLE                          ("head > title"),
		SCRIPTS                        ("script"),

		BODY                           ("body"),
		BODY_NODES                     ("body > *"),

		NON_STANDARD_MAIN_CONTENT      ("td.buchtext > *"),
		TOP_LEVEL_HEADINGS             ("body h1, h2, h3, h4"),
		NON_STANDARD_BOTTOM_NAVIGATION ("div.navigation"),

		GREY_TABLE                     ("table[bgcolor=#eeeeee]"),
		MAIN_CONTENT_1                 (GREY_TABLE.query + " > tr > td > div.main > *, " +
		                                GREY_TABLE.query + " > tbody > tr > td > div.main > *"),
		MAIN_CONTENT_2                 (GREY_TABLE.query + " > tr > td > *, " +
		                                GREY_TABLE.query + " > tbody > tr > td > *"),

		JUMP_TO_TOP_LINK               ("div:has(a[href=#top])"),
		GRAPHICAL_PARAGRAPH_SEPARATOR  ("div:has(img[src=common/jupiters.gif])"),

		FEEDBACK_FORM                  ("form[action*=openbook]"),

		IMAGE_SMALL                    ("img[src*=klein/klein]"),
		IMAGE_1                        ("div.bildbox img"),
		IMAGE_2                        ("td.tabellentext img"),
		IMAGE_3                        ("a[href=#bild] > img"),
		IMAGE_4                        ("a[rel=lightbox] > img"),
		IMAGE_5                        ("a[onclick*=OpenWin] > img"),
		IMAGE_BOX_1                    ("div.bildbox"),
		IMAGE_BOX_2                    ("td.tabellentext:has(" + IMAGE_SMALL.query + ")"),
		IMAGE_BOX_3                    ("a[href=#bild]:has(img)"),
		IMAGE_BOX_4                    ("a[rel=lightbox]"),
		IMAGE_BOX_5                    ("a[onclick*=OpenWin]:has(img)"),

		TOC_HEADING_2                  ("h2:has(a)"),
		INDEX_LINK                     ("a[href*=stichwort.htm]"),
		AFTER_INDEX_LINK               (INDEX_LINK.query + " + *"),
		ALL_LINKS                      ("a");

		private final String query;

		private Selector(String query) {
			this.query = query;
		}
	}

	private static enum Regex                       // Regex patterns mapped to symbolic names
	{
		// Subchapter no. in TOC link target
		SUBCHAPTER_HREF          ("(.*_[0-9a-h]+_(?:[a-z0-9]+_)*)([0-9]+)(\\.htm.*)"),
		// Subchapter no. in TOC link title
		SUBCHAPTER_TEXT          ("^([0-9A-H]+\\.)([0-9]+)(.*)"),

		// "Kapitel: " between book title and chapter
		TITLE_INFIX              ("^(.*)(?:Kapitel: )(.*)$"),
		// "Galileo Computing/Design" prefix and " openbook/index" postfix
		TITLE_PREFIX_POSTFIX     ("^(?:Galileo (?:Computing|Design)(?: ::|:| [-–]) )?(.*?)(?: (?:[-–]|&ndash;|::)( openbook| index|))?$"),
		// Text before dash for some books with " - " or " &ndash; " within the book title
		TITLE_DASHED_BOOK_PREFIX ("^((?:Excel 2007|Java 7|Adobe.+CS4|Joomla! 1.5|Objektor.*mierung) [-–] )(.*)"),
		// Get book chapter after title and separator
		TITLE_CHAPTER            ("^(?:.+?) (?:[-–]|&ndash;|&#8211;) (.*)");

		private final Pattern pattern;

		private Regex(String regex) {
			pattern = Pattern.compile(regex);
		}
	}

	public JsoupFilter(InputStream in, OutputStream out, Book book, File origFile) throws UnsupportedEncodingException {
		super(in, out, book, origFile);
		output = new PrintStream(out, false, "windows-1252");
		isTOCFile = origFile.getName().startsWith("index.htm");
	}

	@Override
	protected String getLogMessage() {
		return "Cleaning up HTML, removing clutter (header, footer, navigation, ads), fixing structure";
	}

	@Override
	protected void filter() throws Exception {
		parseDocument();
		removeClutter();
		fixStructure();
		writeDocument();
	}

	private void parseDocument() throws Exception {
		document = Jsoup.parse(in, "windows-1252", "");
		headTag = selectorQuery(Selector.HEAD.query).first();
		bodyTag = selectorQuery(Selector.BODY.query).first();
		initialiseTitle(true);
	}

	private void removeClutter() {
		fixNode429();
		removeClutterAroundMainContent();
		removeClutterWithinMainContent();
	}

	private void initialiseTitle(boolean removeBookTitle) {
		Element titleTag = (Element) selectorQuery(Selector.TITLE.query).first();
		if (titleTag == null) {
			// Should only happen for "teile.html" in book unix_guru
			SimpleLogger.debug("No page title found");
			return;
		}

		Matcher matcher;
		pageTitle = titleTag.text();
		SimpleLogger.debug("Original page title: " + pageTitle, IndentMode.INDENT_AFTER);

		if (isTOCFile) {
			// TOC file (index.htm*) gets preconfigured title
			pageTitle = book.title;
		}
		else {
			// Remove "Kapitel: " between book title and chapter
			matcher = Regex.TITLE_INFIX.pattern.matcher(pageTitle);
			if (matcher.matches())
				pageTitle = matcher.group(1) + matcher.group(2);
			SimpleLogger.debug("Step 1 In:         " + pageTitle);

			// Remove "Galileo Computing/Design" prefix and " openbook/index" postfix
			matcher = Regex.TITLE_PREFIX_POSTFIX.pattern.matcher(pageTitle);
			if (matcher.matches())
				pageTitle = matcher.group(1);
			SimpleLogger.debug("Step 2 PrePost:    " + pageTitle);

			if (removeBookTitle) {
				// Get text before dash for some books with " - " or " &ndash; " within the book title
				matcher = Regex.TITLE_DASHED_BOOK_PREFIX.pattern.matcher(pageTitle);
				String titlePrefix = "";
				if (matcher.matches()) {
					titlePrefix = matcher.group(1);
					pageTitle = matcher.group(2);
				}
				SimpleLogger.debug("Step 3 DashedBook: " + pageTitle);

				// Remove book title, only chapter number + name remain
				matcher = Regex.TITLE_CHAPTER.pattern.matcher(pageTitle);
				if (matcher.matches())
					pageTitle = matcher.group(1);
				else
					pageTitle = titlePrefix + pageTitle;
				SimpleLogger.debug("Step 4 Chapter:    " + pageTitle);
			}
		}
		SimpleLogger.debug("Clean page title:    " + pageTitle, IndentMode.DEDENT_BEFORE);

		titleTag.text(pageTitle);
	}

	private void fixStructure() {
		if (!hasStandardLayout) {
			fixFontSizesForNonStandardLayout();
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

	private void writeDocument() throws Exception {
		output.print(document);
	}

	/**
	 * Individual fix for a buggy heading in "Unix Guru" book's node429.html which would later make
	 * deletion of top navigation fail in method removeClutterWithinMainContent().
	 */
	private void fixNode429() {
		if (! (origFile.getName().equals("node429.html") && pageTitle.contains("unix")))
			return;
		SimpleLogger.debug("Fixing buggy heading");
		Element buggyParagraph = selectorQuery("p:containsOwn(gpGlossar18133)").first();
		buggyParagraph.html("<h1><a>unix</a></h1>");
	}

	private void removeClutterAroundMainContent() {
		// Keep JavaScript for source code colouring ('prettyPrint' function) in some books
		// deleteNodes(Selector.SCRIPTS.query);

		Elements mainContent = selectorQuery(Selector.NON_STANDARD_MAIN_CONTENT.query);
		if (mainContent.size() > 0)
			hasStandardLayout = false;
		else {
			mainContent = selectorQuery(Selector.MAIN_CONTENT_1.query);
			if (mainContent.size() == 0)
				mainContent = selectorQuery(Selector.MAIN_CONTENT_2.query);
		}
		removeComments();
		deleteNodes(Selector.BODY_NODES.query);
		moveNodesTo(mainContent, bodyTag);
	}

	private void removeComments() {
		for (Element element : document.getAllElements()) {
			List<Node> comments = new ArrayList<Node>();
			for (Node node : element.childNodes()) {
				if (node instanceof Comment)
					comments.add(node);
			}
			deleteNodes(comments);
		}
	}

	private void removeClutterWithinMainContent() {
		if (hasStandardLayout) {
			deleteNodes(Selector.JUMP_TO_TOP_LINK.query);
			deleteNodes(Selector.GRAPHICAL_PARAGRAPH_SEPARATOR.query);
			removeFeedbackForm();
		}
		else {
			removeNonStandardTopNavigation();
			deleteNodes(Selector.NON_STANDARD_BOTTOM_NAVIGATION.query);
		}
	}

	/**
	 * Remove user feedback form plus relevant clutter around it
	 *   - preceding heading and text
	 *   - hotizontal separator (HR tag) preceding heading
	 *   - everything after the feedback form until the rest of the document
	 */
	private void removeFeedbackForm() {
		Element feedbackForm = selectorQuery(Selector.FEEDBACK_FORM.query).first();
		if (feedbackForm == null) {
			SimpleLogger.debug("No feedback form found -> nothing to remove");
			return;
		}
		SimpleLogger.debug("Feedback form found");
		deleteNodes(getFeedbackFormNeigbourhood(feedbackForm));
		SimpleLogger.debug("Feedback form removed");
	}

	/**
	 * Find all nodes around a given user feedback form.
	 * <p>
	 * Background info: The feedback form is preceded by an HR tag separating it from the main content, a heading
	 * and introductory text. It is usually not follwed by any other content, but if so, it will be considered part
	 * of the feedback form's neighbourhood too. Usually we have a structure roughly looking like this:
	 * <pre>
	 *   BR
	 *   HR
	 *   ...
	 *   FORM[action*=openbook]
	 *       INPUT
	 *       INPUT
	 *       ...
	 *   ...
	 * </pre>
	 * There are two known cases in book "dreamewaver_8" (02_kap02_002.htm, 04_kap04_002.htm) for which jsoup
	 * parsing returns a different DOM structure: The feedback form is garbled in the former and not an HR tag's
	 * following sibling in the latter case. The jsoup mailing list has been notified about this fact. HTML tidy
	 * returns the expected ("correct") structure, so this might be a jsoup problem.
	 * <p>
	 * <b>Disclaimer:</b> While this implementation can cope with the irregularities in DOM structure concerning
	 * the feedback form, has no influence on other errors such as garbled text elements occurring in the remainder
	 * of 02_kap02_002.htm and maybe also in other files (unknown).
	 *
	 * @param feedbackForm user feedback form for which to determine the neighbourhood
	 *
	 * @return a list of nodes belonging to the feedback form's surrounding neighbourhood (including the feedback
	 * form itself)
	 */
	private List<Node> getFeedbackFormNeigbourhood(final Element feedbackForm) {
		final List<Node> neigbourhood = new ArrayList<Node>();
		NodeVisitor neigbourhoodFinder = new NodeVisitor() {
			private boolean feedbackFormFound = false;
			private boolean hrTagFound = false;
			@Override
			public void head(Node node, int depth) {
				if (!feedbackFormFound) {
					if (node instanceof Element && "hr".equals(node.nodeName())) {
						hrTagFound = true;
						neigbourhood.clear();
					}
					else if (node == feedbackForm)
						feedbackFormFound = true;
					else if (!hrTagFound)
						return;
				}
				neigbourhood.add(node);
			}
			@Override
			public void tail(Node node, int depth) {}
		};
		document.traverse(neigbourhoodFinder);
		return neigbourhood;
	}

	private void removeNonStandardTopNavigation() {
		Element firstHeading = selectorQuery(Selector.TOP_LEVEL_HEADINGS.query).first();
		if (firstHeading == null)
			return;
		int firstHeadingIndex = firstHeading.siblingIndex();
		List<Node> topNavigationEtc = new ArrayList<Node>();
		for (Node node : firstHeading.parent().childNodes()) {
			if (node.siblingIndex() < firstHeadingIndex)
				topNavigationEtc.add(node);
		}
		deleteNodes(topNavigationEtc);
	}

	private void overrideBackgroundImage() {
		bodyTag.attr("style", "background: none");
	}

	private void fixImages() {
		replaceByBigImages(selectorQuery(Selector.IMAGE_SMALL.query));
		replaceBoxesByImages(selectorQuery(Selector.IMAGE_BOX_1.query), selectorQuery(Selector.IMAGE_1.query));
		replaceBoxesByImages(selectorQuery(Selector.IMAGE_BOX_2.query), selectorQuery(Selector.IMAGE_2.query));
		replaceBoxesByImages(selectorQuery(Selector.IMAGE_BOX_3.query), selectorQuery(Selector.IMAGE_3.query));
		replaceBoxesByImages(selectorQuery(Selector.IMAGE_BOX_4.query), selectorQuery(Selector.IMAGE_4.query));
		replaceBoxesByImages(selectorQuery(Selector.IMAGE_BOX_5.query), selectorQuery(Selector.IMAGE_5.query));
	}

	/*
	 * There is one known occurrence (the "PHP PEAR" book) where there are two grey tables
	 * (background colour #eeeeee) within one document. It is actually a bug in the book's
	 * TOC (index.htm) because there are three lines of HTML code which are repeated erroneously.
	 * JTidy interprets them as two nested tables, handling them gracefully. But after
	 * removeClutterAroundMainContent() there still is a leftover grey table which needs
	 * to be removed. This is done here.
	 */
	private void removeRedundantGreyTable() {
		deleteNodes(Selector.GREY_TABLE.query);
	}

	/*
	 * Font sizes for non-standard layout book "UNIX guru" are too small in general and
	 * for page heading in particular. Fix it by adding a custom CSS style tag to each page.
	 */
	private void fixFontSizesForNonStandardLayout() {
		headTag.append("<style type=\"text/css\">\n" + nonStandardCSS + "</style>\n");
	}

	/*
	 * Find out if this page contains a link to the index (stichwort.htm*).
	 */
	private boolean hasIndexLink() {
		return selectorQuery(Selector.INDEX_LINK.query).size() > 0;
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
	private void createIndexLink() {
		if (pageTitle.contains("Ruby on Rails")) {
			SimpleLogger.debug("Book is an exception - not creating index link (no stichwort.htm*)");
			return;
		}
		Element indexLink = (Element) selectorQuery(Selector.TOC_HEADING_2.query).first().clone();
		String fileExtension = ".htm";
		Node ankerNode = null;
		for (Node node : indexLink.childNodes()) {
			if (node.hasAttr("href")) {
				ankerNode = node;
				if (ankerNode.attr("href").contains(".html"))
					fileExtension = ".html";
				break;
			}
		}
		ankerNode.attr("href", "stichwort" + fileExtension);
		((Element) ankerNode).text("Index");
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
	private void fixFaultyLinkTargets() {
		// Exclude the 3 know exceptions and immediately return if one is found
		if (pageTitle.matches(".*(ActionScript 1 und 2|Microsoft-Netzwerk|Shell-Programmierung).*")) {
			SimpleLogger.debug("Book is an exception - no link fixing done");
			return;
		}

		int fixedLinksCount = 0;
		Elements links = selectorQuery(Selector.ALL_LINKS.query);
		for (Element link : links) {
			String href = link.attr("href");
			String text = link.text();
			Matcher hrefMatcher = Regex.SUBCHAPTER_HREF.pattern.matcher(href);
			Matcher textMatcher = Regex.SUBCHAPTER_TEXT.pattern.matcher(text);
			if (hrefMatcher.matches() && textMatcher.matches()) {
				int hrefNumber = Integer.parseInt(hrefMatcher.group(2));
				int textNumber = Integer.parseInt(textMatcher.group(2));
				if (hrefNumber != textNumber) {
					SimpleLogger.debug("Chapter " + text);
					SimpleLogger.debug("  Faulty: " + href);
					String numberFormat = "%0" + hrefMatcher.group(2).length() + "d";
					href = hrefMatcher.group(1) + String.format(numberFormat, textNumber) + hrefMatcher.group(3);
					SimpleLogger.debug("  Fixed:  " + href);
					link.attr("href", href);
					fixedLinksCount++;
				}
			}
		}
		SimpleLogger.debug("Number of fixed links = " + fixedLinksCount);
	}

	/*
	 * There is one known occurrence (the "JavaScript and AJAX" book) where there is
	 * erroneous trailing text after the last TOC entry (the index link pointing to
	 * stichwort.htm*). Because it looks ugly, we remove everything after the index link.
	 */
	private void removeContentAfterIndexLink() {
		deleteNodes(Selector.AFTER_INDEX_LINK.query);
	}

	private static void replaceByBigImages(Elements smallImages) {
		for (Element image : smallImages)
			image.attr("src", image.attr("src").replaceFirst("klein/klein", "/"));
	}

	private static void replaceBoxesByImages(Elements smallImageBoxes, Elements smallImages) {
		for (int i = 0; i < smallImageBoxes.size(); i++)
			replaceNodeBy(smallImageBoxes.get(i), smallImages.get(i));
	}

	/*
	 * ============================================================================================
	 * GENERAL PURPOSE HELPER METHODS
	 * ============================================================================================
	 */

	private Elements selectorQuery(String query) {
		return document.select(query);
	}

	private static void deleteNodes(List<Node> nodes) {
		for (Object node : nodes.toArray())
			((Node) node).remove();
	}

	private void deleteNodes(String query) {
		selectorQuery(query).remove();
	}

	private static void moveNodesTo(Elements sourceNodes, Element targetElement) {
		for (Element element : sourceNodes) {
			element.remove();
			targetElement.appendChild(element);
		}
	}

	private static void replaceNodeBy(Element original, Element replacement) {
		replacement.remove();
		original.replaceWith(replacement);
	}
}
