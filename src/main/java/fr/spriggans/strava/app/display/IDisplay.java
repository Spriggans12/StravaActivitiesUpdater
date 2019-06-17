package fr.spriggans.strava.app.display;

public interface IDisplay {

	void log(String str);

	default void log() {
		log("");
	}

	void err(String str);
	
	void err(Throwable t);

}
