package de.scrum_master.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileDownloader
{
	private URL        from;
	private File       to;
	private BigInteger md5;
	private boolean    doChecksum;

	public class MD5MismatchException extends Exception {
		private File file;
		private BigInteger md5Expected;
		private BigInteger md5Actual;

		private static final long serialVersionUID = 5271376926114647849L;

		MD5MismatchException(File file, BigInteger md5Expected, BigInteger md5Actual) {
			super(file + ": expected " + md5Expected.toString(16) + ", got " + md5Actual.toString(16));
			this.file = file;
			this.md5Expected = md5Expected;
			this.md5Actual = md5Actual;
		}

		public File getFile() { return file; }
		public BigInteger getMD5Expected() { return md5Expected; }
		public BigInteger getMD5Actual() { return md5Actual; }
	}

	public FileDownloader(URL from, File to, BigInteger md5) {
		if (from == null)
			throw new IllegalArgumentException("Parameter 'from' must not be null");
		if (to == null)
			throw new IllegalArgumentException("Parameter 'to' must not be null");
		this.from = from;
		this.to = to;
		this.md5 = md5;
		this.doChecksum = md5 != null;
	}

	public FileDownloader(String fromURL, String toFile, String md5) throws MalformedURLException {
		this(
			new URL(fromURL),
			new File(toFile),
			md5 == null ? null : new BigInteger(md5, 16)
		);
	}

	public FileDownloader(URL from, File to) {
		this(from, to, null);
	}

	public FileDownloader(String fromURL, String toFile) throws MalformedURLException {
		this(fromURL, toFile, null);
	}

	public void download()
		throws IOException, NoSuchAlgorithmException, MD5MismatchException
	{
		OutputStream        outStream;
		MessageDigest       md5Digest  = null;
		ReadableByteChannel in         = null;
		WritableByteChannel out        = null;
		ByteBuffer          buffer;
		BigInteger          md5Actual;
		try {
			SimpleLogger.debug("Downloading " + from + " ...");
			outStream = new FileOutputStream(to);
			if (doChecksum) {
				md5Digest = MessageDigest.getInstance("MD5");
				outStream = new DigestOutputStream(outStream, md5Digest);
			}
			in = Channels.newChannel(from.openStream());
			out = Channels.newChannel(outStream);
			buffer = ByteBuffer.allocate(1 << 20);  // 1 MB

			// Download file, optionally calculate MD5 while writing output
			while (in.read(buffer) != -1) {
				buffer.flip();
				out.write(buffer);
				buffer.clear();
			}

			if (doChecksum) {
				md5Actual = new BigInteger(1, md5Digest.digest());
				if (! md5Actual.equals(md5))
					throw new MD5MismatchException(to, md5, md5Actual);
				SimpleLogger.debug("  MD5 checksum OK");
			}
			SimpleLogger.debug("Download done");
		}
		finally {
			try { in.close(); }  catch (Exception e) { }
			try { out.close(); } catch (Exception e) { }
		}
	}
}
