package unimelb.comp90015.project1.cypt;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Crypto {
	public static String generateStorngPasswordHash(String password,
			String saltStr) throws NoSuchAlgorithmException,
			InvalidKeySpecException {
		int iterations = 1000;
		char[] chars = password.toCharArray();
		byte[] salt = saltStr.getBytes();

		PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 64 * 8);
		SecretKeyFactory skf = SecretKeyFactory
				.getInstance("PBKDF2WithHmacSHA1");
		byte[] hash = skf.generateSecret(spec).getEncoded();
		return toHex(hash);
	}

	public static boolean validatePassword(String originalPasswordHash,
			String storedPasswordHash) throws NoSuchAlgorithmException,
			InvalidKeySpecException {
		byte[] originalHash = fromHex(originalPasswordHash);
		byte[] storedHash = fromHex(storedPasswordHash);

		int diff = originalHash.length ^ storedHash.length;
		for (int i = 0; i < originalHash.length && i < storedHash.length; i++) {
			diff |= originalHash[i] ^ storedHash[i];
		}
		return diff == 0;
	}

	private static String toHex(byte[] array) throws NoSuchAlgorithmException {
		BigInteger bi = new BigInteger(1, array);
		String hex = bi.toString(16);
		int paddingLength = (array.length * 2) - hex.length();
		if (paddingLength > 0) {
			return String.format("%0" + paddingLength + "d", 0) + hex;
		} else {
			return hex;
		}
	}

	private static byte[] fromHex(String hex) throws NoSuchAlgorithmException {
		byte[] bytes = new byte[hex.length() / 2];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2),
					16);
		}
		return bytes;
	}
}
