package fr.spriggans.strava.app.updaters;

import java.util.List;

import javastrava.api.v3.model.StravaActivity;
import javastrava.api.v3.service.Strava;

public abstract class AbstractActivitiesUpdater {

	private Condition condition = getCondition();

	private Consequence consequence = getConsequence();

	public final void modifyActivities(Strava strava, List<StravaActivity> activities) {
		for (StravaActivity activity : activities) {

			// TODO last update

			if (condition.test(strava, activity)) {
				consequence.perform(strava, activity);
			}
		}
		allDone();
	}

	protected abstract Consequence getConsequence();

	protected abstract Condition getCondition();

	protected void allDone() {
		// Override this in child classes
	}

}
