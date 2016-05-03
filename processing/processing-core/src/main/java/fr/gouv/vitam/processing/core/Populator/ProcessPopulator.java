package fr.gouv.vitam.processing.core.Populator;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.gouv.vitam.processing.beans.Process;

public class ProcessPopulator {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPopulator.class);
	/**
	 * Json file
	 * 
	 */

	static File FILE_JSON = new File("workflowJSONv1.json");

	public static Process populate() {
		ObjectMapper objectMapper = new ObjectMapper();
		Process process = null;

		try {
			process = objectMapper.readValue(FILE_JSON, Process.class);

		} catch (IOException e) {
			LOGGER.error("Exception thrown by populator", e);
		}
		return process;
	}

}
