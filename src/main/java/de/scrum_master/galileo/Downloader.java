package de.scrum_master.galileo;

import de.scrum_master.util.FileDownloader;
import de.scrum_master.util.FileDownloader.MD5MismatchException;
import de.scrum_master.util.ZipFileExtractor;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.NoSuchAlgorithmException;

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
		if (! file.exists()) {
			if (book.downloadArchive.contains("//dummy/")) {
				System.out.println("  Skipping download (online-only book)");
				return;
			}
			try {
					new FileDownloader(new URL(book.downloadArchive), file, new BigInteger(book.archiveMD5, 16)).download();
			}
			catch (MD5MismatchException e) {
				System.err.println(
					"Possible download problem detected: MD5 checksum mismatch." +
					"\n  Book title:    " + book.title +
					"\n  Download file: " + e.getFile() +
					"\n  MD5 expected:  " + e.getMD5Expected().toString(16) +
					"\n  MD5 actual:    " + e.getMD5Actual().toString(16) +
					"\n\nPossible reasons are:" +
					"\n  - corrupt download file due to incomplete download process" +
					"\n  - download file was updated on server" +
					"\n\nThe program will try to unpack and clean the download archive anyway." +
					"\n\nIn case of problems you have the following options:" +
					"\n  - If you think the download file is corrupt, just delete it and restart the" +
					"\n    program so as to get a fresh download." +
					"\n  - If you think that the download file is okay but the cleanup does not work" +
					"\n    correctly, please notify the program author by creating a ticket at" +
					"\n    https://github.com/kriegaex/Galileo-Openbook-Cleaner/issues, but only if" +
					"\n    an equivalent ticket does not already exist." +
					"\n  - If the cleanup works correctly even despite the MD5 mismatch, please also" +
					"\n    notify the author as explained above. Meanwhile you can avoid this error" +
					"\n    message by using the --write-config option first and afterwards update the" +
					"\n    MD5 checksum for the corresponding openbook manually in config.xml.\n"
				);
			}
		}
	}

	private void unpackBook() throws IOException {
		// Create target directory if necessary
		if (! targetDirectory.exists())
			if (!targetDirectory.mkdir())
				throw new IOException("cannot create directory '" + targetDirectory + "'");

		// If openbook was unpacked previously, we are done
		if (hasIndexHtml(targetDirectory))
			return;

		// Unzip openbook archive
		File zipFile = new File(downloadDirectory, book.downloadArchive.replaceFirst(".*/", ""));
		ZipFileExtractor zipExtractor = new ZipFileExtractor(zipFile, targetDirectory);
		try {
			zipExtractor.unzip();
		}
		catch (FileNotFoundException e) {
			if (book.downloadArchive.contains("//dummy/"))
				throw new IOException(
					"For this online-only book, please make sure to crawl-download and zip it manually before trying to process it.",
					e
				);
			throw e;
		}
		catch (IOException e) {
			if (e.getMessage().equals("Truncated ZIP file"))
				throw new IOException(
					"Probably the download of '" + zipFile + "' was interrupted. Please delete the file and retry.",
					e
				);
			throw e;
		}

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
			throw new RuntimeException("cannot rename directory '" + subDirectory + "' to '" + tmpDirectory + "'");
		if (! parentDirectory.delete())
			throw new RuntimeException("cannot delete directory '" + parentDirectory + "'");
		if (! tmpDirectory.renameTo(parentDirectory))
			throw new RuntimeException("cannot rename directory '" + tmpDirectory + "' to '" +  parentDirectory + "'");
	}

	private void downloadCoverImage()
		throws IOException, NoSuchAlgorithmException, MD5MismatchException
	{
		// Create target directory if necessary
		if (! targetDirectory.exists() && ! targetDirectory.mkdir())
			throw new IOException("cannot create directory '" + targetDirectory + "'");

		String imageName = book.unpackDirectory + File.separator + "cover.jpg";
		File file = new File(downloadDirectory, imageName);
		if (! file.exists())
			new FileDownloader(new URL(book.coverImage), file, null).download();
	}
}
