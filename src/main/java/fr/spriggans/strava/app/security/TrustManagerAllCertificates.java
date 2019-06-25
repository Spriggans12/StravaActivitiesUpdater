package fr.spriggans.strava.app.security;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Completely disables all certificates checks. Thus rendering HTTPS
 * useless.<br/>
 * Not really recommended to use this class. It leaves you vulnerable to
 * potential MITM attacks.<br/>
 * But in some cases (testing / setting up environement), it can be useful.
 */
public class TrustManagerAllCertificates implements X509TrustManager {

	public static void disableSSL() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, new TrustManager[] { new TrustManagerAllCertificates() }, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		// Does nothing
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		// Does nothing
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		// Does nothing
		return new X509Certificate[0];
	}

}
