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
// FIXME REVIEW You must use the correct GUIDFactory method
import fr.gouv.vitam.common.guid.GUIDFactory;

// TODO REVIEW missing licence header
// TODO REVIEW missing javadoc comments
// FIXME REVIEW if FileVitamUtils => Commons

public class FileVitamUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileVitamUtils.class);

	public FileVitamUtils() {
	}

	public static File convertInputStreamToFile(InputStream initialStream) {
                // FIXME REVIEW Don't use absolute path for file 
		File file = new File("/tmp/file1");

		file.setReadable(true);
		file.setWritable(true);

		try {
			FileUtils.copyInputStreamToFile(initialStream, file);
		} catch (IOException e) {
		}

		return file;
	}
        // FIXME REVIEW try to not put the json file in memory .
	public static String convertInputStreamXMLToString(InputStream input) {

		LOGGER.info("In FileVitamUtils.convertInputtreamXMLToString ...");

		String jsonString = "";
		try {

			String xml = read(input);
                        // FIXME REVIEW Declare the correct import
			JSONObject metadataObject = org.json.XML.toJSONObject(xml);
			// FIXME REVIEW Use factory
			metadataObject.put("_id", GUIDFactory.newUnitGUID(0).toString());
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
        // FIXME REVIEW This implemnentation is not correct as it stores all the file in memory. Maybe pache Commons IO (IOUtils.toString(is))
	public static String read(InputStream input) throws IOException {
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
			return buffer.lines().collect(Collectors.joining("\n"));
		}
	}

}
