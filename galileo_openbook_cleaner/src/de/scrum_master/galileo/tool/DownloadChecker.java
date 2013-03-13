package de.scrum_master.galileo.tool;

import java.math.BigInteger;
import java.net.URL;

import de.scrum_master.galileo.Book;
import de.scrum_master.util.FileDownloader;

public class DownloadChecker {
	/**
	 * Checks <b>all</b> Galileo Openbooks for availability by downloading them and verifying their MD5 checksums.
	 *
	 * <b>Attention:</b> This is a long-running operation (~14 minutes on my old PC using 16 Mbit/s downstream DSL)
	 * which as of 2013-03-10 downloads at least 1.1 Gb of data!
	 */
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		Book.readConfig(true);
		System.out.println("\nChecking book downloads and MD5 checksums:");
		for (String bookID : Book.books.keySet()) {
			Book book = Book.books.get(bookID);
			System.out.print("  " + bookID + " ... ");
			try {
				new FileDownloader(new URL(book.downloadArchive), null, new BigInteger(book.archiveMD5, 16)).download();
				System.out.println("OK");
			} catch (Exception e) {
				System.out.println("failed");
				e.printStackTrace();
			}
		}
		System.out.println("\nExecution time: " + (System.currentTimeMillis() - startTime) / 1000.0 + " s");
	}
}
