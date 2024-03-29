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
		SortedSet<String> localConfigURLs = getLocalConfigURLs();
		SortedSet<String> webSiteURLs = getWebSiteURLs();
		SortedSet<String> localConfigOrphans = getOrphans(localConfigURLs, webSiteURLs);
		SortedSet<String> webSiteOrphans = getOrphans(webSiteURLs, localConfigURLs);
		System.out.println("\nOrphan URLs:");
		System.out.println("  Local config orphans: " + localConfigOrphans);
		System.out.println("  Web site orphans:     " + webSiteOrphans);
		System.out.println("\nExecution time: " + (System.currentTimeMillis() - startTime) / 1000.0 + " s");
	}

	private static SortedSet<String> getLocalConfigURLs() {
		Book.readConfig(true);
		SortedSet<String> localConfigURLs = new TreeSet<>();
		for (Book book : Book.books.values())
			localConfigURLs.add(book.downloadArchive.replaceAll(".*/galileo[a-z]+_(.+)\\.zip", "$1"));
		return localConfigURLs;
	}

	private static SortedSet<String> getWebSiteURLs() throws Exception {
		Document webPage;
		Elements downloadLinks;
		SortedSet<String> webSiteURLs = new TreeSet<>();
		webPage = Jsoup.parse(new URL("https://www.rheinwerk-verlag.de/openbook/"), 10000);
		downloadLinks = webPage.select("a.btn-primary");
		for (Element link : downloadLinks) {
			webSiteURLs.add(link.attr("href").replaceAll(".*/", ""));
		}
		/*
		Orphan URLs:
  Local config orphans: [apps_entwickeln_fuer_iphone_und_ipad, asp_net, einstieg_vb_2008, einstieg_vb_2010, java8, visual_csharp, visual_csharp_2010, windows_server_2008]
  Web site orphans:     []
		 */
		return webSiteURLs;
	}

	private static SortedSet<String> getOrphans(SortedSet<String> myURLs, SortedSet<String> otherURLs) {
		SortedSet<String> Orphans = new TreeSet<>();
		for (String url : myURLs)
			if (!otherURLs.contains(url))
				Orphans.add(url);
		return Orphans;
	}
}
