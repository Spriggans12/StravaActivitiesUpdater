package fr.spriggans.strava.app;

import java.util.List;

import fr.spriggans.strava.app.display.IDisplay;
import fr.spriggans.strava.app.display.impl.ConsoleDisplay;
import fr.spriggans.strava.app.updaters.AbstractActivitiesUpdater;
import fr.spriggans.strava.app.updaters.impl.ChangeWorkBikeAU;
import javastrava.api.v3.auth.AuthorisationService;
import javastrava.api.v3.auth.impl.retrofit.AuthorisationServiceImpl;
import javastrava.api.v3.auth.model.Token;
import javastrava.api.v3.model.StravaActivity;
import javastrava.api.v3.service.Strava;

public class App {

	public static final IDisplay OUT = new ConsoleDisplay();

	public static void main(String[] args) {
		OUT.log("Starting program...");
		
		// Gets a Strava API object
		Strava strava = createStrava();

		// Fetches activities
		List<StravaActivity> activities = getActivitiesToUpdate(strava);

		// Performs the updates
		updateActivities(strava, activities);
		
		OUT.log("All done !");
	}

	private static Strava createStrava() {
		AuthorisationService service = new AuthorisationServiceImpl();
		Token token = service.tokenExchange(Constants.APP_CLIENT_ID, Constants.APP_CLIENT_SECRET, Constants.APP_CODE);
		return new Strava(token);
	}

	private static List<StravaActivity> getActivitiesToUpdate(Strava strava) {
		// TODO Potential improvement :
		// Only update activities from the last execution of the program ?
		// Would require to add a param to allow for all activities to
		// be updated regardless of he activity's date.

		// This fetches the 50 first activities of page 1
		return strava.listAuthenticatedAthleteActivities();
	}

	private static void updateActivities(Strava strava, List<StravaActivity> activities) {
		AbstractActivitiesUpdater updater = new ChangeWorkBikeAU();
		updater.updateActivities(strava, activities);
	}

}
