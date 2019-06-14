package fr.spriggans.strava.app;

import java.util.ResourceBundle;

public class Constants {

	/**
	 * Name of the configuration file
	 */
	private static final String BUNDLE_NAME = "constants";

	/**
	 * Resource bundle containing configuration properties
	 */
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	public static final Integer APP_CLIENT_ID = integer("app.client_id");

	public static final String APP_CLIENT_SECRET = string("app.client_secret");

	public static final String APP_CODE = string("app.code");

	public static final Integer USER_ID = integer("user.id");

	public static final String WORK_BIKE_ID = string("user.work_bike");

	private static Integer integer(final String key) {
		return Integer.valueOf(RESOURCE_BUNDLE.getString(key));
	}

	private static String string(final String key) {
		return RESOURCE_BUNDLE.getString(key);
	}
}
