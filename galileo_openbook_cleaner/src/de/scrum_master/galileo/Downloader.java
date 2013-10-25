package de.scrum_master.galileo;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

import de.scrum_master.util.*;
import de.scrum_master.util.FileDownloader.*;

class Downloader
{
	private final Book book;
	private final File downloadDirectory;
	private final File targetDirectory;

	private static final FileFilter DIRECTORIES_ONLY = new FileFilter() {
		public boolean accept(File pathName) {
			return pathName.isDirectory();
		}
	};

	Downloader(File downloadDirectory, Book book) {
		this.book = book;
		this.downloadDirectory = downloadDirectory;
		this.targetDirectory = new File(downloadDirectory, book.unpackDirectory);
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
			new FileDownloader(new URL(book.downloadArchive), file, new BigInteger(book.archiveMD5, 16)).download();
	}

	private void unpackBook() throws IOException {
		// Create target directory if necessary
		if (! targetDirectory.exists())
			if (!targetDirectory.mkdir())
				throw new IOException("Cannot create directory '" + targetDirectory + "'");

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
		moveDirectoryUpOneLevel(subDirectory);
	}

	private boolean hasIndexHtml(File directory) {
		return new File(directory, "index.htm").exists() || new File(directory, "index.html").exists();
	}

	private void moveDirectoryUpOneLevel(File subDirectory) {
		File parentDirectory = subDirectory.getParentFile();
		File tmpDirectory = new File(parentDirectory + ".tmp");
		if (! subDirectory.renameTo(tmpDirectory))
			throw new RuntimeException("Cannot rename directory '" + subDirectory + "' to '" + tmpDirectory + "'");
		if (! parentDirectory.delete())
			throw new RuntimeException("Cannot delete directory '" + parentDirectory + "'");
		if (! tmpDirectory.renameTo(parentDirectory))
			throw new RuntimeException("Cannot rename directory '" + tmpDirectory + "' to '" +  parentDirectory + "'");
	}

	private void downloadCoverImage()
		throws IOException, NoSuchAlgorithmException, MD5MismatchException
	{
		// Create target directory if necessary
		if (! targetDirectory.exists())
			if (!targetDirectory.mkdir())
				throw new IOException("Cannot create directory '" + targetDirectory + "'");

		String imageName = book.unpackDirectory + File.separator + "cover.jpg";
		File file = new File(downloadDirectory, imageName);
		if (! file.exists())
			new FileDownloader(new URL(book.coverImage), file, null).download();
	}
}
