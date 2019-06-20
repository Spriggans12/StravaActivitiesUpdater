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

	public static final String USERNAME;

	public static final String PASSWORD;

	public static final Integer MAX_TIME_FOR_UPDATE;

	public static final Boolean MAKE_PRIVATE;

	public static final Boolean UPDATE_ALL_ACTIVITIES;

	public static final String ACTIVITY_NAME_PREFIX;

	public static final String ACTIVITY_NAME_MORNING;

	public static final String ACTIVITY_NAME_EVENING;

	public static final String ACTIVITY_NAME_SUFFIX;

	public static final String ACTIVITY_DESC_PREFIX;

	public static final String ACTIVITY_DESC_MORNING;

	public static final String ACTIVITY_DESC_EVENING;

	public static final String ACTIVITY_DESC_SUFFIX;

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
		MAKE_PRIVATE = bool("app.make_private");
		UPDATE_ALL_ACTIVITIES = bool("app.update_regardless_of_date");
		MAX_TIME_FOR_UPDATE = integer("app.max_time_to_update");

		USERNAME = string("user.name");
		PASSWORD = string("user.password");

		ACTIVITY_NAME_PREFIX = string("activity.name.prefix");
		ACTIVITY_NAME_MORNING = string("activity.name.morning");
		ACTIVITY_NAME_EVENING = string("activity.name.evening");
		ACTIVITY_NAME_SUFFIX = string("activity.name.suffix");
		ACTIVITY_DESC_PREFIX = string("activity.desc.prefix");
		ACTIVITY_DESC_MORNING = string("activity.desc.morning");
		ACTIVITY_DESC_EVENING = string("activity.desc.evening");
		ACTIVITY_DESC_SUFFIX = string("activity.desc.suffix");
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

	private static Boolean bool(final String key) {
		return "1".equals(PROPERTIES.getProperty(key));
	}
}
