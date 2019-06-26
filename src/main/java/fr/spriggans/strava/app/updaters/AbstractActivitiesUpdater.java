package fr.spriggans.strava.app.updaters;

import java.time.LocalDateTime;
import java.util.List;

import javastrava.api.v3.model.StravaActivity;
import javastrava.api.v3.service.Strava;

public abstract class AbstractActivitiesUpdater {

	private Condition condition = getCondition();

	private Consequence consequence = getConsequence();

	/**
	 * @return Ending time of the last (most recent) updated activity. Null if no
	 *         activities were updated.
	 */
	public final LocalDateTime updateActivities(Strava strava, List<StravaActivity> activities) {
		preUpdate();
		for (StravaActivity activity : activities) {
			if (condition.test(strava, activity)) {
				consequence.perform(strava, activity);
			}
		}
		return postUpdate();
	}

	protected abstract Consequence getConsequence();

	protected abstract Condition getCondition();

	protected void preUpdate() {
		// Override this in child classes
	}

	/**
	 * @return Ending time of the last (most recent) updated activity. Null if no
	 *         activities were updated.
	 */
	protected LocalDateTime postUpdate() {
		// Override this in child classes
		return null;
	}

}
