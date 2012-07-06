package de.scrum_master.galileo;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import de.scrum_master.util.FileDownloader;
import de.scrum_master.util.FileDownloader.MD5MismatchException;
import de.scrum_master.util.SimpleLogger;
import de.scrum_master.util.ZipFileExtractor;

class Downloader
{
	private final Book book;
	private final File downloadDirectory;
	private final File targetDirectory;

	private static final FileFilter DIRECTORIES_ONLY = new FileFilter() {
		public boolean accept(File pathname) {
			return pathname.isDirectory();
		}
	};

	Downloader(File downloadDirectory, Book book) {
		this.book = book;
		this.downloadDirectory = downloadDirectory;
		this.targetDirectory = new File(downloadDirectory, book.unpackDirectory);
	}

	Downloader(String downloadDirectory, Book book) {
		this(new File(downloadDirectory), book);
	}

	void download()
		throws IOException, NoSuchAlgorithmException, MD5MismatchException
	{
		downloadBook();
		unpackBook();
		downloadCoverImage();
	}

	private void downloadBook()
		throws IOException, NoSuchAlgorithmException, MD5MismatchException
	{
		String archiveName = book.downloadArchive.replaceFirst(".*/", "");
		File file = new File(downloadDirectory, archiveName);
		if (! file.exists())
			new FileDownloader(new URL(book.downloadArchive), file, book.archiveMD5).download();
	}

	private void unpackBook() throws IOException {
		// Create target directory if necessary
		if (! targetDirectory.exists())
			targetDirectory.mkdir();

		// If openbook was unpacked previously, we are done
		if (hasIndexHtml(targetDirectory))
			return;

		// Unzip openbook archive
		new ZipFileExtractor(
			new File(downloadDirectory, book.downloadArchive.replaceFirst(".*/", "")),
			targetDirectory
		).unzip();

		// If zip contents like index.htm* are already on top level, we are done
		if (hasIndexHtml(targetDirectory))
			return;

		// No index.htm* on top level -> find subdirectory
		File[] subDirectories = targetDirectory.listFiles(DIRECTORIES_ONLY);
		assert subDirectories.length == 1;
		File subDirectory = subDirectories[0];

		// Move subdirectory contents one level up to target directory
		moveDirectoryContents(subDirectory, targetDirectory);

		// Remove empty subdirectory
		subDirectory.delete();
	}

	private boolean hasIndexHtml(File directory) {
		return new File(directory, "index.htm").exists() || new File(directory, "index.html").exists();
	}

	private void moveDirectoryContents(File sourceDirectory, File targetDirectory) {
		// TODO: Do not work on file level, but on directory level:
		//   - move sourceDirectory to ../../sourceDirectory.tmp
		//   - delete targetDirectory
		//   - rename sourceDirectory.tmp -> targetDirectory
		for (File file : sourceDirectory.listFiles()) {
			File targetFile = new File(targetDirectory, file.getName());
			if (! file.renameTo(targetFile))
				SimpleLogger.error("  Not moved: " + file + " -> " + targetFile);
		}
	}

	private void downloadCoverImage()
		throws IOException, NoSuchAlgorithmException, MD5MismatchException
	{
		// Create target directory if necessary
		if (! targetDirectory.exists())
			targetDirectory.mkdir();

		String imageName = book.unpackDirectory + File.separator + "cover.jpg";
		File file = new File(downloadDirectory, imageName);
		if (! file.exists())
			new FileDownloader(new URL(book.coverImage), file, null).download();
	}

	public static void main(String[] args)
		throws NoSuchAlgorithmException, IOException, MD5MismatchException
	{
		// Usage example #1: download & unpack one book
		Book myBook = Book.SHELL_PROG;
		SimpleLogger.echo("Downloading, MD5 checking, unpacking");
		SimpleLogger.echo("  " + myBook.downloadArchive);
		SimpleLogger.echo("  " + myBook.coverImage);
		new Downloader(".", myBook).download();
		SimpleLogger.echo("Done\n");

		// Usage example #2
		SimpleLogger.echo("Downloading cover images for all books ...");
		for (Book book : Book.values()) {
			SimpleLogger.echo("  " + book);
			new Downloader("c:\\Dokumente und Einstellungen\\Robin\\Eigene Dateien\\Bücher\\Galileo Computing", book).downloadCoverImage();
		}
		SimpleLogger.echo("Done");
	}
}
