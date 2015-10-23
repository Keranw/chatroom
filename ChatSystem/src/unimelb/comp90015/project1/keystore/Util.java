package unimelb.comp90015.project1.keystore;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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
	
	public KeyManager[] getKeyManagerArray(String keystore,
			String keystorePasswd) {
		KeyManager[] ret = null;
//		String rootDir = System.getProperty("user.dir");
//		String keyFile = rootDir + "/mySrvKeystore";
//
//		File t = new File(keyFile);
//		if (!t.exists()) {
//			throw new RuntimeException("Could not find key manager file");
//		}

		if (null == keystorePasswd) {
			keystorePasswd = "123456";
		}
		
		try {
			InputStream steam = getClass().getResourceAsStream(keystore);
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			KeyStore ks = KeyStore.getInstance("JKS");
			// initialize KeyStore object using keystore name
			ks.load(steam, null);
			kmf.init(ks, keystorePasswd.toCharArray());
			ret = kmf.getKeyManagers();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}

	public TrustManager[] getTrustManagerArray(String truststore,
			String pwd) {
		TrustManager[] ret = null;

		if (null == pwd) {
			pwd = "123456";
		}

		try {
			InputStream steam = getClass().getResourceAsStream(truststore);
			TrustManagerFactory tmf = TrustManagerFactory
					.getInstance("SunX509");
			KeyStore ts = KeyStore.getInstance("JKS");
			// initialize truststore object using truststore name
			ts.load(steam, pwd.toCharArray());
			tmf.init(ts);
			ret = tmf.getTrustManagers();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	public SSLContext getSSLClientContext()
			throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext ret = SSLContext.getInstance("TLSv1.2");
		KeyManager[] km = getKeyManagerArray("/clientkeystore.jks", null);
		TrustManager[] tm = getTrustManagerArray("/clienttruststore.jks", null);
		ret.init(km, tm, null);
		return ret;
	}

	public SSLContext getSSLServerContext()
			throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext ret = SSLContext.getInstance("TLSv1.2");
		KeyManager[] km = getKeyManagerArray("/serverkeystore.jks", null);
		TrustManager[] tm = getTrustManagerArray("/servertruststore.jks", null);
		ret.init(km, tm, null);
		return ret;
	}
}
