package fr.spriggans.strava.app.updaters;

import javastrava.api.v3.model.StravaActivity;
import javastrava.api.v3.service.Strava;

public interface Condition {

	public boolean test(Strava strava, StravaActivity activity);
	
}
