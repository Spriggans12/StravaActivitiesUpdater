package fr.spriggans.strava.app.updaters;

import javastrava.api.v3.model.StravaActivity;
import javastrava.api.v3.service.Strava;

public interface Consequence {

	public void perform(Strava strava, StravaActivity activity);

}
