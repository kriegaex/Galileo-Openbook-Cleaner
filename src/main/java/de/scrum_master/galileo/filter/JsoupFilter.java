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
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;

import de.scrum_master.galileo.Book;
import de.scrum_master.util.SimpleLogger;

public class JsoupFilter extends BasicFilter {
	private PrintStream output;                    // Output stream for filtered document
	private boolean     isTOCFile;                 // TOC = table of contents = index.htm*
	private Document    document;                  // Jsoup document (DOM structure)
	private Element     headTag;                   // XOM element pointing to HTML <head> tag
	private Element     bodyTag;                   // XOM element pointing to HTML <body> tag
	private String      pageTitle;                 // Content of HTML <title> tag
	private boolean     hasStandardLayout = true;  // Known exception: "UNIX guru" book

	protected static final String FILE_EXTENSION = ".jsoup";

	private static enum Selector                   // Selector query strings mapped to symbolic names
	{
		HEAD                           ("head"),
		TITLE                          ("head > title"),
		TITLE_META                     ("head > meta[name=title]"),
		SCRIPTS                        ("script"),

		BODY                           ("body"),
		BODY_NODES                     ("body > *"),

		NON_STANDARD_MAIN_CONTENT      ("td.buchtext"),
		TOP_LEVEL_HEADINGS             ("body h1, h2, h3, h4"),
		NON_STANDARD_BOTTOM_NAVIGATION ("div.navigation"),

		GREY_TABLE                     ("table[bgcolor=#eeeeee]"),
		MAIN_CONTENT_1                 (GREY_TABLE.query + " > tr > td > div.main, " +
		                                GREY_TABLE.query + " > tbody > tr > td > div.main"),
		MAIN_CONTENT_2                 (GREY_TABLE.query + " > tr > td, " +
		                                GREY_TABLE.query + " > tbody > tr > td"),

		JUMP_TO_TOP_LINK               ("div[align=center]:has(a[href=#top])"),
		GRAPHICAL_PARAGRAPH_SEPARATOR  ("div:has(img[src=common/jupiters.gif])"),

		FEEDBACK_FORM                  ("form[action*=openbook]"),
		FEEDBACK_LINK                  ("a[href*=/feedback/produkt/]"),

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

		NODE429_BUGGY_PARAGRAPH        ("p:containsOwn(gpGlossar18133)"),

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
		// Sub-chapter no. in TOC link target
		SUB_CHAPTER_HREF         ("(.*_[0-9a-h]+_(?:[a-z0-9]+_)*)([0-9]+)(\\.htm.*)"),
		// Sub-chapter no. in TOC link title
		SUB_CHAPTER_TEXT         ("^([0-9A-H]+\\.)([0-9]+)(.*)"),

		// "Kapitel: " between book title and chapter
		TITLE_INFIX              ("^(.*)(?:Kapitel: )(.*)$"),
		// "Galileo Computing/Design" prefix and " openbook/index" postfix
		TITLE_PREFIX_POSTFIX     ("^(?:Galileo|Rheinwerk (?:Computing|Design)(?: ::|:| [-–]) )?(.*?)(?: (?:[-–]|&ndash;|::)( openbook| index|))?$"),
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
		isTOCFile = origFile.getName().startsWith("index.htm");
	}

	@Override
	protected String getLogMessage() {
		return "Cleaning up HTML, removing clutter, fixing structure";
	}

	@Override
	protected void filter() throws Exception {
		parseDocument();
		removeClutter();
		fixStructure();
		writeDocument();
	}

	private void parseDocument() throws Exception {
		document = Jsoup.parse(in, null, "");
		headTag = findFirstElement(Selector.HEAD);
		bodyTag = findFirstElement(Selector.BODY);
		initialiseTitle(true);
		String charset = document.charset().name();
		SimpleLogger.debug("Character set = " + charset);
		output = new PrintStream(out, false, charset);
	}

	private void removeClutter() {
		fixNode429();
		removeClutterAroundMainContent();
		removeClutterWithinMainContent();
	}

	private void initialiseTitle(boolean removeBookTitle) {
		boolean hasTitleTag = true;
		Element titleTag = findFirstElement(Selector.TITLE);
		if (titleTag == null) {
			// Newer books crawled via HTTrack or similar do not have <title> tags anymore, but <meta name="title">
			hasTitleTag = false;
			titleTag = findFirstElement(Selector.TITLE_META);
		}
		if (titleTag == null) {
			// Should only happen for "teile.html" in book unix_guru
			SimpleLogger.debug("No page title found");
			return;
		}

		Matcher matcher;
		pageTitle = hasTitleTag ? titleTag.text() : titleTag.attr("content");
		SimpleLogger.debug("Original page title: " + pageTitle);
		SimpleLogger.indent();

		if (isTOCFile) {
			// TOC file (index.htm*) gets pre-configured title
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
		SimpleLogger.dedent();
		SimpleLogger.debug("Clean page title:    " + pageTitle);

		if (hasTitleTag)
			titleTag.text(pageTitle);
		else
			titleTag.attr("content", pageTitle);
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
			if (missingIndexLink())
				createIndexLink();
			fixFaultyLinkTargets();
			removeContentAfterIndexLink();
		}
	}

	private void writeDocument() {
		document.outputSettings().prettyPrint(false);
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
		Element buggyParagraph = findFirstElement(Selector.NODE429_BUGGY_PARAGRAPH);
		buggyParagraph.html("<h1><a>unix</a></h1>");
	}

	private void removeClutterAroundMainContent() {
		// Keep JavaScript for source code colouring ('prettyPrint' function) in some books
		// deleteNodes(Selector.SCRIPTS);

		Elements mainContent = findElements(Selector.NON_STANDARD_MAIN_CONTENT);
		if (mainContent.size() > 0)
			hasStandardLayout = false;
		else {
			mainContent = findElements(Selector.MAIN_CONTENT_1);
			if (mainContent.size() == 0)
				mainContent = findElements(Selector.MAIN_CONTENT_2);
		}
		removeComments();
		deleteNodes(Selector.BODY_NODES);
		moveNodes(mainContent, bodyTag);
	}

	private void removeComments() {
		for (Element element : document.getAllElements()) {
			List<Node> comments = new ArrayList<>();
			for (Node node : element.childNodes()) {
				if (node instanceof Comment)
					comments.add(node);
			}
			deleteNodes(comments);
		}
	}

	private void removeClutterWithinMainContent() {
		if (hasStandardLayout) {
			deleteNodes(Selector.JUMP_TO_TOP_LINK);
			deleteNodes(Selector.GRAPHICAL_PARAGRAPH_SEPARATOR);
			removeFeedbackForm();
			removeFeedbackLink();
		}
		else {
			removeNonStandardTopNavigation();
			deleteNodes(Selector.NON_STANDARD_BOTTOM_NAVIGATION);
		}
	}

	/**
	 * Remove user feedback form plus relevant clutter around it
	 *   - preceding heading and text
	 *   - hotizontal separator (HR tag) preceding heading
	 *   - everything after the feedback form until the rest of the document
	 */
	private void removeFeedbackForm() {
		Element feedbackForm = findFirstElement(Selector.FEEDBACK_FORM);
		if (feedbackForm == null) {
			SimpleLogger.debug("No feedback form found -> nothing to remove");
			return;
		}
		SimpleLogger.debug("Feedback form found");
		deleteNodes(getFeedbackElementNeighbourhood(feedbackForm));
		SimpleLogger.debug("Feedback form removed");
	}

	/**
	 * Remove user feedback link plus relevant clutter around it
	 *   - preceding heading and text
	 *   - hotizontal separator (HR tag) preceding heading
	 *   - everything after the feedback link until the rest of the document
	 */
	private void removeFeedbackLink() {
		Element feedbackLink = findFirstElement(Selector.FEEDBACK_LINK);
		if (feedbackLink == null) {
			SimpleLogger.debug("No feedback link found -> nothing to remove");
			return;
		}
		SimpleLogger.debug("Feedback link found");
		deleteNodes(getFeedbackElementNeighbourhood(feedbackLink));
		SimpleLogger.debug("Feedback link removed");
	}

	/**
	 * Find all nodes around a given user feedback form/link.
	 * <p>
	 * Background info: The feedback element is preceded by an HR tag separating it from the main content, a heading
	 * and introductory text. It is usually not followed by any other content, but if so, it will be considered part
	 * of the feedback element's neighbourhood too. Usually we have a structure roughly looking like this for the
	 * feedback form
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
	 * or like this for the feedback link
	 * <pre>
	 *   BR
	 *   HR
	 *   ...
	 *   A[HREF*=/feedback/produkt/]
	 *   ...
	 * </pre>
	 * There are two known cases in book "dreamewaver_8" (02_kap02_002.htm, 04_kap04_002.htm) for which jsoup
	 * parsing returns a different DOM structure: The feedback form is garbled in the former and not an HR tag's
	 * following sibling in the latter case. The jsoup mailing list has been notified about this fact. HTML tidy
	 * returns the expected ("correct") structure, so this might be a jsoup problem.
	 * <p>
	 * The fix also removes one leftover feedback form in "ubuntu_11_04" (linux_18_001.htm).
	 * <p>
	 * <b>Disclaimer:</b> While this implementation can cope with the irregularities in DOM structure concerning
	 * the feedback form, it has no influence on other errors such as garbled text elements occurring in the remainder
	 * of 02_kap02_002.htm and maybe also in other files (unknown).
	 *
	 * @param feedbackElement user feedback form/link for which to determine the neighbourhood
	 *
	 * @return a list of nodes belonging to the feedback element's surrounding neighbourhood (including the feedback
	 * element itself)
	 */
	private List<Node> getFeedbackElementNeighbourhood(final Element feedbackElement) {
		final List<Node> neighbourhood = new ArrayList<>();
		NodeVisitor neighbourhoodFinder = new NodeVisitor() {
			private boolean feedbackFormFound = false;
			private boolean hrTagFound = false;
			@Override
			public void head(Node node, int depth) {
				if (!feedbackFormFound) {
					if (node instanceof Element && "hr".equals(node.nodeName())) {
						hrTagFound = true;
						neighbourhood.clear();
					}
					else if (node == feedbackElement)
						feedbackFormFound = true;
					else if (!hrTagFound)
						return;
				}
				neighbourhood.add(node);
			}
			@Override
			public void tail(Node node, int depth) {}
		};
		document.traverse(neighbourhoodFinder);
		return neighbourhood;
	}

	private void removeNonStandardTopNavigation() {
		Element firstHeading = findFirstElement(Selector.TOP_LEVEL_HEADINGS);
		if (firstHeading == null)
			return;
		int firstHeadingIndex = firstHeading.siblingIndex();
		List<Node> topNavigationEtc = new ArrayList<>();
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
		replaceByBigImages(findElements(Selector.IMAGE_SMALL));
		replaceBoxesByImages(findElements(Selector.IMAGE_BOX_1), findElements(Selector.IMAGE_1));
		replaceBoxesByImages(findElements(Selector.IMAGE_BOX_2), findElements(Selector.IMAGE_2));
		replaceBoxesByImages(findElements(Selector.IMAGE_BOX_3), findElements(Selector.IMAGE_3));
		replaceBoxesByImages(findElements(Selector.IMAGE_BOX_4), findElements(Selector.IMAGE_4));
		replaceBoxesByImages(findElements(Selector.IMAGE_BOX_5), findElements(Selector.IMAGE_5));
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
		deleteNodes(Selector.GREY_TABLE);
	}

	/*
	 * Font sizes for non-standard layout book "UNIX guru" are too small in general and for
	 * page heading in particular. Fix it by adding a custom CSS style tag to each page
	 * so as to avoid having to patch the stylesheet file.
	 */
	private void fixFontSizesForNonStandardLayout() {
		headTag.append(
			"<style type=\"text/css\">\n" +
				"body, p, a, tr, td, td.nav1, td.nav2, .weiter a, .zurueck a, " +
				".themen, .white, .merksatz, .warnung, .beispiel, .anleser { font-size: 13px; }\n" +
				"h1 a, h2 a, h3 a, h4 a, h5 a { font-size: 16px; }\n" +
				"pre, code { font-size: 12px; }\n" +
			"</style>\n"
		);
	}

	/*
	 * Find out if this page contains a link to the index (stichwort.htm*).
	 */
	private boolean missingIndexLink() {
		return findElements(Selector.INDEX_LINK).size() == 0;
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
		Element indexLink = findFirstElement(Selector.TOC_HEADING_2).clone();
		String fileExtension = ".htm";
		Node ankerNode = null;
		for (Node node : indexLink.childNodes()) {
			if (node.hasAttr("href")) {
				ankerNode = node.clone();
				if (ankerNode.attr("href").contains(".html"))
					fileExtension = ".html";
				break;
			}
		}
		if (ankerNode == null)
			throw new RuntimeException(
				"Cannot create index link because no cloneable TOC node has been found (should never happen)"
			);
		ankerNode.attr("href", "stichwort" + fileExtension);
		((Element) ankerNode).text("Index");
		bodyTag.appendChild(ankerNode);
	}

	/**
	 * There is a strange quirk in the table of contents (TOC, index.htm*) of
	 * several (ca. 10) Galileo Openbooks:
	 *
	 * Some links for sub-chapters *.x point to the file for sub-chapter *.(x-1).
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
	 * It further complicates the fixing task that there are some (ca. 3) books
	 * which show a similar one-off behaviour for <i>all</i> sub-chapters by design,
	 * because they have a different numbering scheme. Those books are OK, though,
	 * thus we need to explicitly exclude them from "fixing". <tt>:-(</tt>
	 */
	private void fixFaultyLinkTargets() {
		// Exclude the 3 known exceptions and immediately return if one is found
		if (pageTitle.matches(".*(ActionScript 1 und 2|Microsoft-Netzwerk|Shell-Programmierung).*")) {
			SimpleLogger.debug("Book is an exception - no link fixing done");
			return;
		}

		int fixedLinksCount = 0;
		Elements links = findElements(Selector.ALL_LINKS);
		for (Element link : links) {
			String href = link.attr("href");
			String text = link.text();
			Matcher hrefMatcher = Regex.SUB_CHAPTER_HREF.pattern.matcher(href);
			Matcher textMatcher = Regex.SUB_CHAPTER_TEXT.pattern.matcher(text);
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
		deleteNodes(Selector.AFTER_INDEX_LINK);
	}

	private static void replaceByBigImages(Elements smallImages) {
		for (Element image : smallImages)
			image.attr("src", image.attr("src").replaceFirst("klein/klein", "/"));
	}

	private static void replaceBoxesByImages(Elements smallImageBoxes, Elements smallImages) {
		for (int i = 0; i < smallImageBoxes.size(); i++)
			replaceNode(smallImageBoxes.get(i), smallImages.get(i));
	}

	/*
	 * ============================================================================================
	 * GENERAL PURPOSE HELPER METHODS
	 * ============================================================================================
	 */

	private Elements findElements(Selector selector) {
		return document.select(selector.query);
	}

	private Element findFirstElement(Selector selector) {
		return findElements(selector).first();
	}

	private void deleteNodes(Selector selector) {
		findElements(selector).remove();
	}

	private static void deleteNodes(List<Node> nodes) {
		for (Object node : nodes.toArray())
			((Node) node).remove();
	}

	private static void moveNodes(Elements sourceNodes, Element targetElement) {
		for (Element element : sourceNodes)
			targetElement.insertChildren(-1, element.childNodes());
	}

	private static void replaceNode(Element original, Element replacement) {
		replacement.remove();
		original.replaceWith(replacement);
	}
}
