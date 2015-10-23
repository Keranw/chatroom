package unimelb.comp90015.project1.keystore;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class Util {
	public final static String[] enabledCipherSuites = { "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256" };
	
	public static KeyManager[] getKeyManagerArray(String keystore,
			String keystorePasswd) {
		KeyManager[] ret = null;
		String rootDir = System.getProperty("user.dir");
		String keyFile = rootDir + "/mySrvKeystore";

		File t = new File(keyFile);
		if (!t.exists()) {
			throw new RuntimeException("Could not find key manager file");
		}

		if (null == keystorePasswd) {
			keystorePasswd = "123456";
		}

		try {
			System.out.println("Using keystore: " + keyFile);
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			KeyStore ks = KeyStore.getInstance("JKS");
			// initialize KeyStore object using keystore name
			ks.load(new FileInputStream(keyFile), null);
			kmf.init(ks, keystorePasswd.toCharArray());
			ret = kmf.getKeyManagers();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}

	private static TrustManager[] getTrustManagerArray(String truststore,
			String pwd) {
		TrustManager[] ret = null;
		String rootDir = System.getProperty("user.dir");
		String trustFile = rootDir + "/mySrvKeystore";

		File t = new File(trustFile);
		if (!t.exists()) {
			throw new RuntimeException("Could not find key manager file");
		}

		if (null == pwd) {
			pwd = "123456";
		}

		try {
			System.out.println("Using " + trustFile + " as truststore");
			TrustManagerFactory tmf = TrustManagerFactory
					.getInstance("SunX509");
			KeyStore ts = KeyStore.getInstance("JKS");
			// initialize truststore object using truststore name
			ts.load(new FileInputStream(trustFile), pwd.toCharArray());
			tmf.init(ts);
			ret = tmf.getTrustManagers();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static SSLContext getSSLClientContext()
			throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext ret = SSLContext.getInstance("TLSv1.2");
		TrustManager[] tm = getTrustManagerArray(null, null);
		ret.init(null, tm, null);
		return ret;
	}

	public static SSLContext getSSLServerContext()
			throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext ret = SSLContext.getInstance("TLSv1.2");
		KeyManager[] km = getKeyManagerArray(null, null);
		ret.init(km, null, null);
		return ret;
	}
}
