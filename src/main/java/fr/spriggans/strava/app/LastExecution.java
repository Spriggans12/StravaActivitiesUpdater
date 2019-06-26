package fr.spriggans.strava.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class LastExecution {

	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss");

	private File dateFile;

	private LocalDateTime date;

	public LastExecution(String dateFilePath) {
		try {
			// Loads and creates the file if it does not exist
			dateFile = new File(dateFilePath);
			dateFile.createNewFile();

			// Reads the last execution time in the file
			loadDateInFile();
		} catch (IOException e) {
			App.OUT.err(e);
		}
	}

	private void loadDateInFile() throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader(dateFile))) {
			String line = br.readLine();
			if (line != null) {
				LocalDateTime.parse(line, formatter);
			}
		} catch (DateTimeParseException e) {
			App.OUT.err("Error while parsing the last execution date from the file " + dateFile.getAbsolutePath());
			App.OUT.err("Program will continue, but update all your activities.");
			App.OUT.err(e);
			date = null;
		}
	}

	/**
	 * Writes to the file, setting its content to <code>date</code>.<br/>
	 * Also changes this object's value of the <code>date</code> field.
	 */
	public void replaceDateWith(LocalDateTime newDate) {
		if (newDate == null) {
			return;
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(dateFile))) {
			writer.write(formatter.format(newDate));
			date = newDate;
		} catch (IOException | DateTimeException e) {
			App.OUT.err(e);
		}
	}

	public LocalDateTime getDate() {
		return date;
	}
}
