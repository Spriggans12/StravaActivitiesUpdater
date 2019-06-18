package fr.spriggans.strava.app.updaters.impl;

import java.io.IOException;
import java.time.DayOfWeek;

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

	@Override
	protected Condition getCondition() {
		return (strava, activity) -> StravaActivityType.RIDE.equals(activity.getType())
				&& activity.getStartDateLocal() != null
				&& !DayOfWeek.SATURDAY.equals(activity.getStartDateLocal().getDayOfWeek())
				&& !DayOfWeek.SUNDAY.equals(activity.getStartDateLocal().getDayOfWeek())
				&& activity.getElapsedTime() != null && activity.getElapsedTime() < 3600 && activity.getName() != null
				&& !activity.getName().contains("Vélotaf");
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
		};
	}

	private boolean changeActivityPrivacy(StravaActivity activity) {
		try {
			HtmlRequestActivityUpdater.makeActivityPrivate(webClient, activity.getId());
		} catch (FailingHttpStatusCodeException | IOException e) {
			App.OUT.err(e);
			return false;
		}
		return true;
	}

	private StravaActivityUpdate getUpdatedActivity(StravaActivity activity) {
		StravaActivityUpdate res = new StravaActivityUpdate(activity);

		String name = "Vélotaf";
		String desc = "";
		if (activity.getStartDateLocal() != null) {
			if (activity.getStartDateLocal().getHour() > 12) {
				name += " - retour maison";
				desc = "Trajet entre le boulot et la maison.\r\n\r\n";
			} else {
				name += " - aller bosser";
				desc = "Trajet entre la maison et le boulot.\r\n\r\n";
			}
		}

		// Change activity's name
		res.setName(name);

		// Change activity's description
		desc += "Mis à jour par informatique, pour utiliser automatiquement le vélotaf et pas le vélo de course ! \\m/";
		res.setDescription(desc);

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
			App.OUT.err("Erreur lors du login à Strava. Activities will remain public.");
		}
	}

	@Override
	protected void postUpdate() {
		App.OUT.log("-----------------");
		App.OUT.log(updated + " activities have been updated !");
		if (updated != updatedPrivacy) {
			App.OUT.log("/!\\ Only " + updatedPrivacy + " activities were made private !!!");
		}
		App.OUT.log("-----------------");

		if (webClient != null) {
			webClient.close();
		}
	}

}
