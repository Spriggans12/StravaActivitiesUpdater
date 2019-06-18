package fr.spriggans.strava.app.updaters;

import java.util.List;

import javastrava.api.v3.model.StravaActivity;
import javastrava.api.v3.service.Strava;

public abstract class AbstractActivitiesUpdater {

	private Condition condition = getCondition();

	private Consequence consequence = getConsequence();

	public final void updateActivities(Strava strava, List<StravaActivity> activities) {
		preUpdate();
		for (StravaActivity activity : activities) {
			if (condition.test(strava, activity)) {
				consequence.perform(strava, activity);
			}
		}
		postUpdate();
	}


	protected abstract Consequence getConsequence();

	protected abstract Condition getCondition();

	protected void preUpdate() {
		// Override this in child classes
	}

	protected void postUpdate() {
		// Override this in child classes
	}

}
