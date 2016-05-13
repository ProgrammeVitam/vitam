package fr.gouv.vitam.processing.core.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.gouv.vitam.common.UUID22;

public class FileVitamUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileVitamUtils.class);

	public FileVitamUtils() {
	}

	public static File convertInputStreamToFile(InputStream initialStream) {

		File file = new File("/tmp/file1");

		file.setReadable(true);
		file.setWritable(true);

		try {
			FileUtils.copyInputStreamToFile(initialStream, file);
		} catch (IOException e) {
		}

		return file;
	}

	public static String convertInputStreamXMLToString(InputStream input) {

		LOGGER.info("In FileVitamUtils.convertInputStreamXMLToString ...");

		String jsonString = "";
		try {

			String xml = read(input);
			JSONObject metadataObject = org.json.XML.toJSONObject(xml);
			metadataObject.put("_id", new UUID22().toString());
			jsonString = metadataObject.toString();

			LOGGER.info("jsonString :" + jsonString);

		} catch (Exception e) {

			LOGGER.error("An excpetion thrown when converting inputStream to String", e);

		}
		return jsonString;
	}

	public static String convertInputStreamToString(InputStream input) {

		LOGGER.info("In FileVitamUtils.convertInputStreamToString ...");

		String jsonString = "";
		try {

			jsonString = read(input);

			LOGGER.info("jsonString :" + jsonString);

		} catch (Exception e) {

			LOGGER.error("An excpetion thrown when converting inputStream to String", e);

		}
		return jsonString;
	}

	public static String read(InputStream input) throws IOException {
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
			return buffer.lines().collect(Collectors.joining("\n"));
		}
	}

}
