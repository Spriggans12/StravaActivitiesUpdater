package fr.spriggans.strava.app.updaters.impl;

import java.time.DayOfWeek;

import fr.spriggans.strava.app.App;
import fr.spriggans.strava.app.Constants;
import fr.spriggans.strava.app.updaters.AbstractActivitiesUpdater;
import fr.spriggans.strava.app.updaters.Condition;
import fr.spriggans.strava.app.updaters.Consequence;
import javastrava.api.v3.model.StravaActivity;
import javastrava.api.v3.model.StravaActivityUpdate;
import javastrava.api.v3.model.reference.StravaActivityType;

public class ChangeWorkBikeAU extends AbstractActivitiesUpdater {

	private int updated = 0;
	
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
			strava.updateActivity(activity.getId(), getUpdatedActivity(activity));
			updated++;
		};
	}

	private StravaActivityUpdate getUpdatedActivity(StravaActivity activity) {
		StravaActivityUpdate update = new StravaActivityUpdate(activity);

		update.setGearId(Constants.WORK_BIKE_ID);
		String desc = "";

		String name = "Vélotaf";
		if (activity.getStartDateLocal() != null) {
			if (activity.getStartDateLocal().getHour() > 12) {
				name += " du soir";
				desc = "Trajet entre le boulot et la maison.\r\n\r\n";
			} else {
				name += " du matin";
				desc = "Trajet entre la maison et le boulot.\r\n\r\n";
			}
		}
		update.setName(name);
		desc += "Mis à jour par informatique, pour utiliser automatiquement le vélotaf et pas le vélo de course ! \\m/";
		update.setDescription(desc);

		return update;
	}

	@Override
	protected void allDone() {
		App.OUT.log("-----------------");
		App.OUT.log(updated + " activities have been updated !");
		App.OUT.log("-----------------");
	}

}
