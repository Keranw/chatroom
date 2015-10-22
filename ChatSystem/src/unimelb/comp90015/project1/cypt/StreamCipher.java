package unimelb.comp90015.project1.cypt;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

import org.apache.commons.codec.binary.Base64;


public class StreamCipher {


	private BigInteger key;
	private BigInteger prime;
	private BigInteger p1;// a
	private BigInteger p2;// b
	private BigInteger r_i;

	public StreamCipher(BigInteger share, BigInteger prime, BigInteger p, BigInteger q) {
		this.key = share; // shared key from DH
		this.prime = prime; // DH prime modulus
		this.p1 = Supplementary.deriveSuppementaryKey(share, p);
		this.p2 = Supplementary.deriveSuppementaryKey(share, q);
		//this.r_i = BigInteger.ZERO;
		this.r_i = Supplementary.parityWordChecksum(share); // shift register
	}

	/***
	 * Updates the shift register for XOR-ing the next byte.
	 */
	public void updateShiftRegister() {
		// implement r_i = [a * (r_i-1) + b] mod p
		r_i = p1.multiply(r_i).add(p2).mod(prime);
	}

	/***
	 * This function returns the shift register to its initial possition
	 */
	public void reset() {
		// r_0 = parityWordChecksum(Key_ab)
		r_i = Supplementary.parityWordChecksum(key);
	}

	/***
	 * Gets N numbers of bits from the MOST SIGNIFICANT BIT (inclusive).
	 * 
	 * @param value Source from bits will be extracted           
	 * @param n The number of bits taken       
	 * @return The n most significant bits from value
	 */
	public byte msb(BigInteger value, int n) {
		BigInteger less = new BigInteger("2");
		less = less.pow(8).subtract(BigInteger.ONE);
		if (value.compareTo(less) < 1) {
			//if the input value is less than 8 bits, just return it 
			return value.byteValue();
		} else {
			//if the input value is larger than 8 bits, get the msb
			BigInteger temp = value.shiftRight(value.bitLength() - n);
			return temp.byteValue();
		}
	}

	/***
	 * Takes a cipher text/plain text and decrypts/encrypts it.
	 * 
	 * @param msg Either Plain Text or Cipher Text.           
	 * @return If PT, then output is CT and vice-versa.
	 */
	private byte[] _crypt(byte[] msg) {
		for (int i = 0; i < msg.length; i++) {
			// implement E(b_i) = b_i XOR msb(r_i)
			msg[i] = (byte) (msg[i] ^ msb(this.r_i, 8));
			updateShiftRegister();
		}
		return msg;
	}

	// -------------------------------------------------------------------//
	// Auxiliary functions to perform encryption and decryption //
	// -------------------------------------------------------------------//
	
	/***
	 * encrypt the input message 
	 * 
	 * @param msg plain text as a string
	 * @return a base64 encoded cipher text string
	 */
	public String encrypt(String msg) {
		String result = null;
		try {
			byte[] asArray = msg.getBytes("UTF-8");
//			result = Base64.getEncoder().encodeToString(_crypt(asArray));
			result = Base64.encodeBase64String(_crypt(asArray));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/***
	 * decrypt the input message
	 * 
	 * @param msg a base64 encoded cipher text string
	 * @return plain text as a string
     */
	public String decrypt(String msg) {
		String result = null;
		try {
//			byte[] asArray = Base64.getDecoder().decode(msg.getBytes("UTF-8"));
			byte[] asArray = Base64.decodeBase64(msg.getBytes("UTF-8"));
			result = new String(_crypt(asArray), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return result;
	}
}
