package de.scrum_master.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

public class ZipFileExtractor
{
	private File archiveFile;
	private File targetDirectory;

	private static final int BUFFER_SIZE = 128 * 1024;

	public ZipFileExtractor(File archiveFile, File targetDirectory) {
		if (archiveFile == null)
			throw new IllegalArgumentException("Parameter 'archiveFile' must not be null");
		if (targetDirectory == null)
			throw new IllegalArgumentException("Parameter 'targetDirectory' must not be null");
		this.archiveFile = archiveFile;
		this.targetDirectory = targetDirectory;
	}

	public ZipFileExtractor(String archiveFile, String targetDirectory) {
		this(new File(archiveFile), new File(targetDirectory));
	}

	public void unzip() throws IOException {
		SimpleLogger.debug("Unzipping archive " + archiveFile + " ...");

		ZipArchiveInputStream zipStream     = null;
		ZipEntry              zipEntry;
		byte[]                buffer        = new byte[BUFFER_SIZE];
		int                   byteCount;
		File                  unzippedFile;
		BufferedOutputStream  outUnzipped   = null;

		try {
			zipStream = new ZipArchiveInputStream(
				new BufferedInputStream(new FileInputStream(archiveFile), BUFFER_SIZE),
				"Cp437", false
			);
			while ((zipEntry = zipStream.getNextZipEntry()) != null) {
				SimpleLogger.debug("  Extracting " + zipEntry);
				unzippedFile = new File(targetDirectory, zipEntry.getName());
				if (zipEntry.isDirectory()) {
					unzippedFile.mkdirs();
					continue;
				}
				unzippedFile.getParentFile().mkdirs();
				outUnzipped = new BufferedOutputStream(new FileOutputStream(unzippedFile), BUFFER_SIZE);
				while ((byteCount = zipStream.read(buffer, 0, BUFFER_SIZE)) != -1)
					outUnzipped.write(buffer, 0, byteCount);
				outUnzipped.close();
			}
		}
		finally {
			try { outUnzipped.close(); } catch (IOException e) { }
			try { zipStream.close(); }   catch (IOException e) { }
		}
		SimpleLogger.debug("Unzipping done");
	}
}
