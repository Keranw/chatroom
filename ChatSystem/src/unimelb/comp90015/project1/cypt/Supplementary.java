package unimelb.comp90015.project1.cypt;

import java.math.BigInteger;

import com.sun.org.apache.xpath.internal.operations.Mod;

public class Supplementary {

	/***
	 * Receives a 2048 bits key and applies a word by word XOR to yield a 64 bit
	 * integer at the end.
	 * 
	 * @param key 2048 bit integer form part A1 DH Key Exchange Protocol
	 *            
	 * @return A 64 bit integer
	 */
	public static BigInteger parityWordChecksum(BigInteger key) {
		BigInteger result = new BigInteger("0");
		BigInteger temp = BigInteger.ZERO;
		for (int i = 0; i < 64; i++) {
			temp = temp.setBit(i);
		}

		for (int i = 0; i < 2048; i += 64) {
			result = result.xor(key.shiftRight(i).and(temp));
		}
		return result;
	}

	/***
	 * 
	 * @param key 2048 bit integer form part A1 DH Key Exchange Protocol
	 *            
	 * @param p A random 64 bit prime integer
	 *            
	 * @return A 64 bit integer for use as a key for a Stream Cipher
	 */
	public static BigInteger deriveSuppementaryKey(BigInteger key, BigInteger p) {
		BigInteger result = key.mod(p); 
		return result;
	}
}
