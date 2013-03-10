package de.scrum_master.galileo.tool;

import java.net.URL;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.scrum_master.galileo.Book;

public class AvailableBooksChecker {
	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		SortedSet<String> knownURLs = getKnownURLs();
		SortedSet<String> foundURLs = getFoundURLs();
		SortedSet<String> knownOrphanURLs = getMyOrphanURLs(knownURLs, foundURLs);
		SortedSet<String> foundOrphanURLs = getMyOrphanURLs(foundURLs, knownURLs);
		System.out.println("Known orphan URLs: " + knownOrphanURLs);
		System.out.println("Found orphan URLs: " + foundOrphanURLs);
		System.out.println("\nExecution time: " + (System.currentTimeMillis() - startTime) / 1000.0 + " s");
	}

	private static SortedSet<String> getKnownURLs() {
		Book.readConfig(true);
		SortedSet<String> knownURLs = new TreeSet<String>();
		for (Book book : Book.books.values())
			knownURLs.add(book.downloadArchive);
		return knownURLs;
	}

	private static SortedSet<String> getFoundURLs() throws Exception {
		Document webPage;
		Elements downloadLinks;
		SortedSet<String> foundURLs = new TreeSet<String>();
		webPage = Jsoup.parse(new URL("http://www.galileocomputing.de/katalog/openbook"), 10000);
		downloadLinks = webPage.select("a[href*=.zip]");
		for (Element link : downloadLinks)
			foundURLs.add(link.attr("href"));
		webPage = Jsoup.parse(new URL("http://www.galileodesign.de/katalog/openbook"), 10000);
		downloadLinks = webPage.select("a[href*=.zip]");
		for (Element link : downloadLinks)
			foundURLs.add(link.attr("href"));
		return foundURLs;
	}

	private static SortedSet<String> getMyOrphanURLs(SortedSet<String> myURLs, SortedSet<String> otherURLs) {
		SortedSet<String> orphanURLs = new TreeSet<String>();
		for (String url : myURLs)
			if (!otherURLs.contains(url))
				orphanURLs.add(url);
		return orphanURLs;
	}
}
