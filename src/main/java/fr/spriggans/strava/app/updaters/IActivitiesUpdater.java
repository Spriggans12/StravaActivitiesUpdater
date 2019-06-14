package fr.spriggans.strava.app.updaters;

import javastrava.api.v3.service.Strava;

public interface IActivitiesUpdater {

	public abstract void modifyActivities(Strava strava);
	
}
