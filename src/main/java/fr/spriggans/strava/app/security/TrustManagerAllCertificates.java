package fr.spriggans.strava.app.security;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.squareup.okhttp.OkHttpClient;

import javastrava.api.v3.rest.util.RetrofitClientResponseInterceptor;

/**
 * Completely disables all certificates checks. Thus rendering HTTPS
 * useless.<br/>
 * Not really recommended to use this class. It leaves you vulnerable to
 * potential MITM attacks.<br/>
 * But in some cases (testing / setting up environement), it can be useful.
 */
public class TrustManagerAllCertificates implements X509TrustManager {

	public static RetrofitClientResponseInterceptor getUnsafeRetrofitClientResponseInterceptor()
			throws KeyManagementException, NoSuchAlgorithmException {
		OkHttpClient okHttpClient = new OkHttpClient();
		okHttpClient.setSslSocketFactory(getUnsafeSSLFactory());
		okHttpClient.setHostnameVerifier(getUnsafeHostnameVerifier());
		return new RetrofitClientResponseInterceptor(okHttpClient);
	}

	private static SSLSocketFactory getUnsafeSSLFactory() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(null, new TrustManager[] { new TrustManagerAllCertificates() },
				new java.security.SecureRandom());
		return sslContext.getSocketFactory();
	}

	private static HostnameVerifier getUnsafeHostnameVerifier() {
		return (hostname, session) -> true;
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
