package fr.spriggans.strava.app;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Constants {

	private static final Properties PROPERTIES;

	public static final Integer APP_CLIENT_ID;

	public static final String APP_CLIENT_SECRET;

	public static final String APP_CODE;

	public static final Integer USER_ID;

	public static final String WORK_BIKE_ID;

	static {
		PROPERTIES = new Properties();
		try (InputStream input = new FileInputStream("./constants.properties")) {
			PROPERTIES.load(input);
		} catch (IOException e) {
			App.OUT.err(e);
			System.exit(-1);
		}
		APP_CLIENT_ID = integer("app.client_id");
		APP_CLIENT_SECRET = string("app.client_secret");
		APP_CODE = string("app.code");
		USER_ID = integer("user.id");
		WORK_BIKE_ID = string("user.work_bike");
	}

	private Constants() {
		// Empty
	}

	private static Integer integer(final String key) {
		return Integer.valueOf(PROPERTIES.getProperty(key));
	}

	private static String string(final String key) {
		return PROPERTIES.getProperty(key);
	}
}
