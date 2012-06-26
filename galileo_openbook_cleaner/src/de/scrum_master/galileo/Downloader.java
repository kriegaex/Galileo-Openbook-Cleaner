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
	private final BookInfo bookInfo;
	private final File downloadDirectory;

	private static final FileFilter DIRECTORIES_ONLY = new FileFilter() {
		public boolean accept(File pathname) {
			return pathname.isDirectory();
		}
	};


	Downloader(String downloadDirectory, BookInfo bookInfo)
	{
		this.bookInfo = bookInfo;
		this.downloadDirectory = new File(downloadDirectory);
	}
	
	void download()
		throws IOException, NoSuchAlgorithmException, MD5MismatchException
	{
		downloadBook();
		unpackBook();
		downloadCover();
	}

	private void downloadBook()
		throws IOException, NoSuchAlgorithmException, MD5MismatchException
	{
		String archiveName = bookInfo.downloadArchive.replaceFirst(".*/", "");
		File file = new File(downloadDirectory, archiveName);
		if (! file.exists())
			new FileDownloader(new URL(bookInfo.downloadArchive), file, bookInfo.archiveMD5).download();
	}

	private void unpackBook()
		throws IOException
	{
		File targetDirectory = new File(downloadDirectory, bookInfo.unpackDirectory);

		// Create target directory if necessary
		if (! targetDirectory.exists())
			targetDirectory.mkdir();

		// If openbook was unpacked previously, we are done
		if (hasIndexHtml(targetDirectory))
			return;

		// Unzip openbook archive 
		new ZipFileExtractor(
			new File(downloadDirectory, bookInfo.downloadArchive.replaceFirst(".*/", "")),
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

	private boolean hasIndexHtml(File directory)
	{
		return new File(directory, "index.htm").exists() || new File(directory, "index.html").exists();
	}

	private void moveDirectoryContents(File sourceDirectory, File targetDirectory)
	{
		for (File file : sourceDirectory.listFiles()) {
			File targetFile = new File(targetDirectory, file.getName());
			if (! file.renameTo(targetFile))
				SimpleLogger.error("Not moved: " + file + " -> " + targetFile);
		}
	}

	private void downloadCover()
		throws IOException, NoSuchAlgorithmException, MD5MismatchException
	{
		String imageName = bookInfo.coverImage.replaceFirst(".*/", "");
		File file = new File(downloadDirectory, bookInfo.unpackDirectory + File.separator + imageName);
		if (! file.exists())
			new FileDownloader(new URL(bookInfo.coverImage), file, null).download();
	}

	public static void main(String[] args)
		throws NoSuchAlgorithmException, IOException, MD5MismatchException
	{
		// Usage example
		BookInfo bookInfo = BookInfo.SHELL_PROG;
		System.out.println(
			"Downloading, MD5 checking, unpacking\n" +
			"  " + bookInfo.downloadArchive + "\n" +
			"  " + bookInfo.coverImage
		);
		new Downloader(".", bookInfo).download();
		System.out.println("Done");
//		bookInfo = BookInfo.PHOTOSHOP_CS4;
//		System.out.println(
//			"Downloading, MD5 checking, unpacking\n" +
//			"  " + bookInfo.downloadArchive + "\n" +
//			"  " + bookInfo.coverImage
//		);
//		new Downloader(".", bookInfo).download();
//		System.out.println("Done");
	}
}
