package fr.spriggans.strava.app;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

		// Program init
		preExecution();

		// Gets a Strava API object
		Strava strava = createStrava();

		// Fetches activities
		List<StravaActivity> activities = getActivitiesToUpdate(strava);

		// Performs the updates
		LocalDateTime endOfLastActivity = updateActivities(strava, activities);

		// Post program stuff
		postExecution(endOfLastActivity);

		OUT.log("All done !");
	}

	private static void preExecution() {
		// Sets Log level
		Level level = Level.parse(Constants.LOGS_LEVEL);
		if (level != null) {
			Logger.getLogger("").setLevel(level);
		}
	}

	private static Strava createStrava() {
		AuthorisationService service = new AuthorisationServiceImpl();
		Token token = service.tokenExchange(Constants.APP_CLIENT_ID, Constants.APP_CLIENT_SECRET, Constants.APP_CODE);
		return new Strava(token);
	}

	private static List<StravaActivity> getActivitiesToUpdate(Strava strava) {
		LocalDateTime updateFrom = null;
		if (!Constants.UPDATE_ALL_ACTIVITIES) {
			// Update only from the last execution time
			updateFrom = Constants.LAST_EXECUTION.getDate();
		}

		// This fetches the 50 first activities of page 1
		return strava.listAuthenticatedAthleteActivities(null, updateFrom);
	}

	/**
	 * @return Ending time of the last (most recent) updated activity. Null if no
	 *         activities were updated.
	 */
	private static LocalDateTime updateActivities(Strava strava, List<StravaActivity> activities) {
		AbstractActivitiesUpdater updater = new ChangeWorkBikeAU();
		return updater.updateActivities(strava, activities);
	}

	private static void postExecution(LocalDateTime endOfLastActivity) {
		// Updates the date in the file (won't be updated if null)
		Constants.LAST_EXECUTION.replaceDateWith(endOfLastActivity);
	}
}
