package fr.spriggans.strava.app.updaters.impl;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;

import fr.spriggans.strava.app.App;
import fr.spriggans.strava.app.Constants;
import fr.spriggans.strava.app.updaters.AbstractActivitiesUpdater;
import fr.spriggans.strava.app.updaters.Condition;
import fr.spriggans.strava.app.updaters.Consequence;
import fr.spriggans.strava.app.updaters.html.HtmlRequestActivityUpdater;
import javastrava.api.v3.model.StravaActivity;
import javastrava.api.v3.model.StravaActivityUpdate;
import javastrava.api.v3.model.reference.StravaActivityType;

public class ChangeWorkBikeAU extends AbstractActivitiesUpdater {

	private int updated = 0;

	private int updatedPrivacy = 0;

	private WebClient webClient;

	private boolean loggedInApp;

	private LocalDateTime lastEndingTime = null;

	@Override
	protected Condition getCondition() {
		return (strava, activity) -> StravaActivityType.RIDE.equals(activity.getType())
				&& activity.getStartDateLocal() != null
				&& !DayOfWeek.SATURDAY.equals(activity.getStartDateLocal().getDayOfWeek())
				&& !DayOfWeek.SUNDAY.equals(activity.getStartDateLocal().getDayOfWeek())
				&& activity.getElapsedTime() != null && activity.getElapsedTime() < Constants.MAX_TIME_FOR_UPDATE
				&& activity.getCommute() != null && !activity.getCommute();
	}

	@Override
	protected Consequence getConsequence() {
		return (strava, activity) -> {
			// Hack to change the activity's privacy settings through HTTP
			if (loggedInApp && changeActivityPrivacy(activity)) {
				updatedPrivacy++;
			}

			// Updates the activity using the official API
			strava.updateActivity(activity.getId(), getUpdatedActivity(activity));
			updated++;
			
			// Keep track of the most recent activity ending time
			LocalDateTime endingTimeOfActivity = getEndingTime(activity);
			if(lastEndingTime == null || endingTimeOfActivity.isAfter(lastEndingTime)) {
				lastEndingTime = activity.getStartDateLocal();
			}
		};
	}
	
	private LocalDateTime getEndingTime(StravaActivity activity) {
		return activity.getStartDateLocal().plusSeconds(activity.getElapsedTime());
	}

	private boolean changeActivityPrivacy(StravaActivity activity) {
		if (Constants.MAKE_PRIVATE) {
			try {
				HtmlRequestActivityUpdater.makeActivityPrivate(webClient, activity.getId());
			} catch (FailingHttpStatusCodeException | IOException e) {
				App.OUT.err(e);
				return false;
			}
		}
		return true;
	}

	private StravaActivityUpdate getUpdatedActivity(StravaActivity activity) {
		StravaActivityUpdate res = new StravaActivityUpdate(activity);

		String name = Constants.ACTIVITY_NAME_PREFIX;
		String desc = Constants.ACTIVITY_DESC_PREFIX;
		if (activity.getStartDateLocal() != null) {
			if (activity.getStartDateLocal().getHour() > 12) {
				// Evening
				name += Constants.ACTIVITY_NAME_EVENING;
				desc += Constants.ACTIVITY_DESC_EVENING;
			} else {
				// Morning
				name += Constants.ACTIVITY_NAME_MORNING;
				desc += Constants.ACTIVITY_DESC_MORNING;
			}
		}

		// Change activity's name
		res.setName(name + Constants.ACTIVITY_NAME_SUFFIX);

		// Change activity's description
		res.setDescription(desc + Constants.ACTIVITY_DESC_SUFFIX);

		// Use the work bike
		res.setGearId(Constants.WORK_BIKE_ID);

		// Flagged as transportation
		res.setCommute(true);

		return res;
	}

	@Override
	protected void preUpdate() {
		loggedInApp = false;
		try {
			webClient = HtmlRequestActivityUpdater.initWebClient();

			// Login to Strava
			if (HtmlRequestActivityUpdater.login(webClient)) {
				// JS ON
				webClient.getOptions().setJavaScriptEnabled(true);
				loggedInApp = true;
			}
		} catch (FailingHttpStatusCodeException | IOException e) {
			App.OUT.err(e);
		}
		if (!loggedInApp) {
			App.OUT.err("Error while login to Strava's website. Activities will remain public.");
		}
	}

	@Override
	protected LocalDateTime postUpdate() {
		App.OUT.log("-----------------");
		App.OUT.log(updated + " activities have been updated !");
		if (updated != updatedPrivacy) {
			App.OUT.log("/!\\ Only " + updatedPrivacy + " activities were made private !!!");
		}
		App.OUT.log("-----------------");

		if (webClient != null) {
			webClient.close();
		}
		return lastEndingTime;
	}

}
