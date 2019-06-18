package fr.spriggans.strava.app.updaters.html;

import java.io.IOException;
import java.util.List;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;

import fr.spriggans.strava.app.Constants;
import javastrava.config.StravaConfig;

public class HtmlRequestActivityUpdater {

	private static final String LOGIN_PAGE = StravaConfig.AUTH_ENDPOINT + "/login";

	private static final String DASHBOARD_PAGE = StravaConfig.AUTH_ENDPOINT + "/dashboard";

	private static final String ACTIVITY_BY_ID_PAGE = StravaConfig.AUTH_ENDPOINT + "/activities/";

	private static final String ACTIVITY_EDIT_WORD = "/edit";

	private static final String ONLY_ME = "only_me";

	private HtmlRequestActivityUpdater() {
		// Empty
	}

	public static WebClient initWebClient() {
		WebClient webClient = new WebClient(BrowserVersion.FIREFOX_52);
		webClient.getOptions().setCssEnabled(false);
		webClient.getOptions().setDownloadImages(false);
		webClient.getOptions().setJavaScriptEnabled(false);
		webClient.getCookieManager().setCookiesEnabled(true);
		return webClient;
	}

	public static boolean login(WebClient webClient) throws IOException {
		HtmlPage loginPage = webClient.getPage(LOGIN_PAGE);

		HtmlForm loginForm = loginPage.getHtmlElementById("login_form");

		loginForm.getInputByName("email").type(Constants.USERNAME);
		loginForm.getInputByName("password").type(Constants.PASSWORD);

		loginPage.getHtmlElementById("login-button").click();

		// Just in case
		webClient.waitForBackgroundJavaScript(10_000);

		HtmlPage dashboardPage = loginForm.click();

		return DASHBOARD_PAGE.equals(dashboardPage.getBaseURI());
	}

	public static void makeActivityPrivate(WebClient webClient, long activityTest) throws IOException {
		HtmlPage activityPage = webClient.getPage(ACTIVITY_BY_ID_PAGE + activityTest + ACTIVITY_EDIT_WORD);
		webClient.waitForBackgroundJavaScript(3_000);

		// Makes the activity private
		List<DomElement> radios = activityPage.getElementsByName("activity[visibility]");
		for (DomElement radio : radios) {
			if (ONLY_ME.equals(((HtmlRadioButtonInput) radio).getValueAttribute())) {
				((HtmlRadioButtonInput) radio).click();
			}
		}

		// Gets the Save button and clicks it
		List<HtmlButton> buttons = activityPage.getByXPath("//button[contains(@class, 'btn-save-activity')]");
		if (!buttons.isEmpty()) {
			buttons.get(0).click();
		}
	}
}
