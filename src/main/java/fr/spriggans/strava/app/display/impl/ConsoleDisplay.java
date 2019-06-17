package fr.spriggans.strava.app.display.impl;

import fr.spriggans.strava.app.display.IDisplay;

public class ConsoleDisplay implements IDisplay {

	@Override
	public void log(String str) {
		System.out.println(str);
	}

	@Override
	public void err(String str) {
		System.err.println(str);
	}

	@Override
	public void err(Throwable t) {
		t.printStackTrace();
	}

}
