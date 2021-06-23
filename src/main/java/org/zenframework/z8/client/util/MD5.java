package org.zenframework.z8.client.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {

	public static String hex(String value) {
		MessageDigest messageDigest = null;

		try {
			messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.reset();
			messageDigest.update(value.getBytes());
			byte[] digest = messageDigest.digest();
			return padLeft(new BigInteger(1, digest).toString(16), 32, '0');
		} catch(NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private static String padLeft(String str, int size, char padChar) {
		int pads = size - str.length();
		return pads > 0 ? padding(pads, padChar).concat(str) : str;
	}

	private static String padding(int repeat, char padChar) {
		final char[] buf = new char[repeat];

		for(int i = 0; i < buf.length; i++)
			buf[i] = padChar;

		return new String(buf);
	}

}
