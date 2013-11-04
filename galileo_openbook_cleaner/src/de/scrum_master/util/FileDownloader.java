package de.scrum_master.util;

import java.io.*;
import java.math.BigInteger;
import java.net.Authenticator;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileDownloader
{
	private URL        from;
	private File       to;
	private BigInteger md5;
	private boolean    doChecksum;
	private boolean    doWriteToFile;

	// Initialise HTTP proxy settings from system properties
	private static       String proxyHost     = System.getProperty("http.proxyHost");
	private static       int    proxyPort     = 80;
	private static final String proxyUser     = System.getProperty("http.proxyUser");
	private static final String proxyPassword = System.getProperty("http.proxyPassword");
	private static       Proxy  proxy         = Proxy.NO_PROXY;

	static {
		String systemProperty = System.getProperty("http.proxyPort");
		if (systemProperty != null && !"".equals(systemProperty))
			proxyPort = Integer.parseInt(systemProperty);
		if (proxyHost != null && !"".equals(proxyHost))
			proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
		if (proxyUser != null)
			Authenticator.setDefault(new Authenticator() {
				public PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
				}
			});
	}

	private static final String CONNECTION_ERROR_MESSAGE =
		"Are your proxy settings for the JVM (Java Virtual Machine) correct?\n" +
		"Please make sure that http.proxyHost and http.proxyPort are configured correctly.\n" +
		"You may also need http.proxyUser and http.proxyPassword in case of an authenticating proxy.\n\n" +
		"Another possible error cause are restrictive firewall settings.\n" +
		"You may want to try deactivating your (personal) firewall.\n" +
		"If it works then, fix the configuration by permitting Java to access the internet.\n";


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
		this.from = from;
		this.to = to;
		this.md5 = md5;
		this.doChecksum = md5 != null;
		this.doWriteToFile = to != null;
	}

	public void download()
		throws IOException, NoSuchAlgorithmException, MD5MismatchException
	{
		MessageDigest       md5Digest  = null;
		ReadableByteChannel in         = null;
		WritableByteChannel out        = null;
		ByteBuffer          buffer;
		BigInteger          md5Actual;
		try {
			SimpleLogger.debug("Downloading " + from + " ...");
			try {
				InputStream inStream = from.openConnection(proxy).getInputStream();
				if (doChecksum) {
					md5Digest = MessageDigest.getInstance("MD5");
					inStream = new DigestInputStream(inStream, md5Digest);
				}
				in = Channels.newChannel(inStream);
			}
			catch (ConnectException | ProtocolException e) {
				System.err.println(CONNECTION_ERROR_MESSAGE);
				throw e;
			}
			catch (IOException e) {
				if(e.getMessage().contains(": 407"))
					System.err.println(CONNECTION_ERROR_MESSAGE);
				throw e;
			}

			if (doWriteToFile)
				out = Channels.newChannel(new FileOutputStream(to));
			buffer = ByteBuffer.allocate(1 << 20);  // 1 MB

			// Download file, optionally calculate MD5
			while (in.read(buffer) != -1) {
				buffer.flip();
				if (doWriteToFile)
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
			try { in.close(); }  catch (Exception ignored) { }
			try { out.close(); } catch (Exception ignored) { }
		}
	}
}
