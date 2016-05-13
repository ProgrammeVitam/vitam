package fr.gouv.vitam.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


public class ContainerUtils {

	public static String generateContainerName() {
		return "container" + UUID.randomUUID();
	}

	public static InputStream getTestStream(String file) throws IOException {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
	}

}
