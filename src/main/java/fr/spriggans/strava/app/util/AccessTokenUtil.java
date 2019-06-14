package fr.spriggans.strava.app.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import fr.spriggans.strava.app.App;
import fr.spriggans.strava.app.Constants;
import javastrava.api.v3.auth.AuthorisationService;
import javastrava.api.v3.auth.impl.retrofit.AuthorisationServiceImpl;
import javastrava.api.v3.auth.model.Token;
import javastrava.api.v3.auth.ref.AuthorisationApprovalPrompt;
import javastrava.api.v3.auth.ref.AuthorisationResponseType;
import javastrava.api.v3.auth.ref.AuthorisationScope;
import javastrava.api.v3.service.exception.BadRequestException;
import javastrava.api.v3.service.exception.StravaInternalServerErrorException;
import javastrava.api.v3.service.exception.UnauthorizedException;
import javastrava.config.StravaConfig;

@Deprecated
public class AccessTokenUtil {

	private static final AuthorisationResponseType DEFAULT_RESPONSE_TYPE = AuthorisationResponseType.CODE;

	private static final String DEFAULT_REDIRECT_URI = "http://localhost/redirects"; //$NON-NLS-1$

	private static final CloseableHttpClient httpClient = HttpClients.createDefault();

	/**
	 * <p>
	 * This utility method will log in to Strava with the provided credentials and
	 * return a valid token which has the provided scopes
	 * </p>
	 *
	 * @param username Strava account username
	 * @param password Strava account password
	 * @param scopes   Authorisation scopes to get a token for
	 * @return The Strava access token
	 * @throws BadRequestException   If something bad happens(!)
	 * @throws UnauthorizedException If client secret is invalid
	 */
	public static Token getStravaAccessToken(final String username, final String password,
			final AuthorisationScope... scopes) throws BadRequestException, UnauthorizedException {
		System.out.println("We are going to logging to this URL to allow the application");
		System.out.println("https://www.strava.com/oauth/authorize?client_id=" + Constants.APP_CLIENT_ID
				+ "&response_type=code&approval_prompt=force&scope=write,view_private&redirect_uri=http://localhost/redirects");

		// Login
		final String authenticityToken = getLoginAuthenticityToken();

		// Approve (force it to ensure we get a new token)
		final String approvalCode = approveApplication(scopes);

		AuthorisationService authService = new AuthorisationServiceImpl();
		return authService.tokenExchange(Constants.APP_CLIENT_ID, Constants.APP_CLIENT_SECRET, approvalCode, scopes);
	}

	/**
	 * <p>
	 * Get the login page and extract the authenticity token that Strava cunningly
	 * hides in the login form
	 * </p>
	 *
	 * @return The value of the authenticity token, which should be included when
	 *         posting the form to log in
	 */
	private static String getLoginAuthenticityToken() {
		final BasicNameValuePair[] params = null;
		Document loginPage;
		try {
			loginPage = httpGet(StravaConfig.AUTH_ENDPOINT + "/login", params); //$NON-NLS-1$
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		final Elements authTokens = loginPage.select("input[name=\"authenticity_token\"]"); //$NON-NLS-1$
		if (authTokens.isEmpty()) {
			return null;
		}
		return authTokens.first().attr("value"); //$NON-NLS-1$
	}

	private static Document httpGet(final String uri, final NameValuePair... parameters) throws IOException {
		HttpUriRequest get = null;
		Document page = null;
		if (parameters == null) {
			get = RequestBuilder.get(uri).build();
		} else {
			get = RequestBuilder.get(uri).addParameters(parameters).build();
		}
		final CloseableHttpResponse response = httpClient.execute(get);
		final int status = response.getStatusLine().getStatusCode();
		if (status != 200) {
			throw new StravaInternalServerErrorException(
					"GET " + get.getURI() + " returned status " + Integer.valueOf(status).toString(), null, null); //$NON-NLS-1$ //$NON-NLS-2$
		}
		try {
			final HttpEntity entity = response.getEntity();
			page = Jsoup.parse(EntityUtils.toString(entity));

			EntityUtils.consume(entity);
		} finally {
			response.close();
		}

		return page;
	}

	/**
	 * <p>
	 * Login to the Strava application
	 * </p>
	 *
	 * <p>
	 * This method is provided FOR TESTING PURPOSES ONLY as it's not genuinely
	 * useful and you shouldn't be asking other people for their Strava password
	 * </p>
	 *
	 * <p>
	 * URL POST https://www.strava.com/session
	 * </p>
	 *
	 * @param email             Email address associated with the user account
	 * @param password          Password associated with the user account
	 * @param authenticityToken token handed out by the Strava login page within the
	 *                          login form
	 * @return The string URL to redirect to next
	 */
	private static String login(final String email, final String password, final String authenticityToken) {
		String location = null;
		try {
			final HttpUriRequest login = RequestBuilder.post().setUri(new URI(StravaConfig.AUTH_ENDPOINT + "/session")) //$NON-NLS-1$
					.addParameter("email", email) //$NON-NLS-1$
					.addParameter("password", password).addParameter("authenticity_token", authenticityToken) //$NON-NLS-1$ //$NON-NLS-2$
					.addParameter("utf8", "✓").build(); //$NON-NLS-1$ //$NON-NLS-2$
			final CloseableHttpResponse response2 = httpClient.execute(login);
			final int status = response2.getStatusLine().getStatusCode();
			if (status != 302) {
				throw new StravaInternalServerErrorException(
						"POST " + login.getURI() + " returned status " + Integer.valueOf(status).toString(), null, //$NON-NLS-1$ //$NON-NLS-2$
						null);
			}
			try {
				final HttpEntity entity = response2.getEntity();
				location = response2.getFirstHeader("Location").getValue(); //$NON-NLS-1$
				EntityUtils.consume(entity);

			} finally {
				response2.close();
			}
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final URISyntaxException e) {
			e.printStackTrace();
		}

		return location;

	}

	/**
	 * @param scopes Authorisation scopes to approve
	 * @return The approval code returned by Strava
	 */
	private static String approveApplication(final AuthorisationScope... scopes) {
		// Get the auth page
		final String authenticityToken = getAuthorisationPageAuthenticityToken(scopes);

		// Post an approval to the request
		return acceptApplication(authenticityToken, App.ADDITIONNAL_SCOPES, scopes);
	}

	/**
	 * Get and return the authenticity token 'hidden' in the Strava OAuth page
	 *
	 * @param scopes The authorisation scopes required
	 * @return The authenticity token
	 */
	private static String getAuthorisationPageAuthenticityToken(final AuthorisationScope... scopes) {
		String scopeString = ""; //$NON-NLS-1$
		for (final AuthorisationScope scope : scopes) {
			if (!scopeString.equals("")) { //$NON-NLS-1$
				scopeString = scopeString + ","; //$NON-NLS-1$
			}
			scopeString = scopeString + scope.toString();
		}
		Document authPage;
		try {
			if (scopeString.equals("")) { //$NON-NLS-1$
				authPage = httpGet(StravaConfig.AUTH_ENDPOINT + "/oauth/authorize", //$NON-NLS-1$
						new BasicNameValuePair("client_id", String.valueOf(Constants.APP_CLIENT_ID)), //$NON-NLS-1$
						new BasicNameValuePair("response_type", //$NON-NLS-1$
								DEFAULT_RESPONSE_TYPE.toString()),
						new BasicNameValuePair("redirect_uri", DEFAULT_REDIRECT_URI), new BasicNameValuePair( //$NON-NLS-1$
								"approval_prompt", AuthorisationApprovalPrompt.FORCE.toString())); //$NON-NLS-1$
			} else {
				authPage = httpGet(StravaConfig.AUTH_ENDPOINT + "/oauth/authorize", //$NON-NLS-1$
						new BasicNameValuePair("client_id", String.valueOf(Constants.APP_CLIENT_ID)), //$NON-NLS-1$
						new BasicNameValuePair("response_type", //$NON-NLS-1$
								DEFAULT_RESPONSE_TYPE.toString()),
						new BasicNameValuePair("redirect_uri", DEFAULT_REDIRECT_URI), //$NON-NLS-1$
						new BasicNameValuePair("approval_prompt", AuthorisationApprovalPrompt.FORCE.toString()), //$NON-NLS-1$
						new BasicNameValuePair("scope", scopeString)); //$NON-NLS-1$

			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		final Elements authTokens = authPage.select("input[name=authenticity_token]"); //$NON-NLS-1$
		if ((authTokens == null) || (authTokens.first() == null)) {
			return null;
		}
		return authTokens.first().attr("value"); //$NON-NLS-1$
	}

	/**
	 * <p>
	 * Indicate that the user has allowed the application to access their Strava
	 * data
	 * </p>
	 *
	 * <p>
	 * This method is provided FOR TESTING PURPOSES ONLY
	 * </p>
	 *
	 * @param authenticityToken The hidden value of the authenticity token which
	 *                          must be returned with the form to Strava
	 * @param scopes            The authorisation scopes to accept
	 * @param additionalScopes
	 * @return The code used by
	 *         {@link AuthorisationService#tokenExchange(Integer, String, String, AuthorisationScope...)}
	 *         to get an access token
	 */
	private static String acceptApplication(final String authenticityToken, final String[] additionalScopes,
			final AuthorisationScope... scopes) {
		String scopeString = ""; //$NON-NLS-1$
		for (final AuthorisationScope scope : scopes) {
			if (!scopeString.equals("")) { //$NON-NLS-1$
				scopeString = scopeString + ","; //$NON-NLS-1$
			}
			scopeString = scopeString + scope.toString();
		}
		for (String scope : additionalScopes) {
			if (!scopeString.equals("")) { //$NON-NLS-1$
				scopeString = scopeString + ","; //$NON-NLS-1$
			}
			scopeString = scopeString + scope.toString();
		}
		String location = null;
		try {
			final HttpUriRequest post = RequestBuilder.post()
					.setUri(new URI(StravaConfig.AUTH_ENDPOINT + "/oauth/accept_application")) //$NON-NLS-1$
					.addParameter("client_id", String.valueOf(Constants.APP_CLIENT_ID)) //$NON-NLS-1$
					.addParameter("redirect_uri", DEFAULT_REDIRECT_URI) //$NON-NLS-1$
					.addParameter("response_type", DEFAULT_RESPONSE_TYPE.toString()) //$NON-NLS-1$
					.addParameter("authenticity_token", authenticityToken) //$NON-NLS-1$
					.addParameter("scope", scopeString).build(); //$NON-NLS-1$
			final CloseableHttpResponse response2 = httpClient.execute(post);
			final int status = response2.getStatusLine().getStatusCode();
			if (status != 302) {
				throw new StravaInternalServerErrorException(
						post.getMethod() + " " + post.getURI() + " returned status code " //$NON-NLS-1$ //$NON-NLS-2$
								+ Integer.valueOf(status).toString(),
						null, null);
			}
			try {
				final HttpEntity entity = response2.getEntity();
				location = response2.getFirstHeader("Location").getValue(); //$NON-NLS-1$
				EntityUtils.consume(entity);

			} finally {
				response2.close();
			}
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final URISyntaxException e) {
			e.printStackTrace();
		}

		// Get the code parameter from the redirect URI
		if (location.indexOf("&code=") != -1) { //$NON-NLS-1$
			final String code = location.split("&code=")[1].split("&")[0]; //$NON-NLS-1$ //$NON-NLS-2$
			return code;
		} else {
			return null;
		}

	}
}
