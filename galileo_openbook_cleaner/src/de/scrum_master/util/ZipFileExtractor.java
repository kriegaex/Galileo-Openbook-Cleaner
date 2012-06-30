package de.scrum_master.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipFileExtractor
{
	private File archiveFile;
	private File targetDirectory;

	private static final int BUFFER_SIZE = 128 * 1024;

	public ZipFileExtractor(File archiveFile, File targetDirectory)
	{
		if (archiveFile == null)
			throw new IllegalArgumentException("Parameter 'archiveFile' must not be null");
		if (targetDirectory == null)
			throw new IllegalArgumentException("Parameter 'targetDirectory' must not be null");
		this.archiveFile = archiveFile;
		this.targetDirectory = targetDirectory;
	}

	public ZipFileExtractor(String archiveFile, String targetDirectory)
	{
		this(new File(archiveFile), new File(targetDirectory));
	}

	public void unzip() throws IOException
	{
		SimpleLogger.debug("Unzipping archive " + archiveFile + " ...");

		ZipInputStream       zipStream     = null;
		ZipEntry             zipEntry;
		byte[]               buffer        = new byte[BUFFER_SIZE];
		int                  byteCount;
		File                 unzippedFile;
		BufferedOutputStream outUnzipped   = null;

		try {
			zipStream = new ZipInputStream(
				new BufferedInputStream(new FileInputStream(archiveFile), BUFFER_SIZE),
				Charset.forName("Cp437")
			);
			while ((zipEntry = zipStream.getNextEntry()) != null) {
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
