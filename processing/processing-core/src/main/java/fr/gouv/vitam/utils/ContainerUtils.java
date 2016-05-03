package fr.gouv.vitam.utils;

import java.io.IOException;
import java.util.UUID;

import com.google.common.io.ByteStreams;

public class ContainerUtils {

	public static String generateContainerName() {
		return "container" + UUID.randomUUID();
	}

	public static byte[] getTestBytes(String file) throws IOException {
		return ByteStreams.toByteArray(Thread.currentThread().getContextClassLoader().getResourceAsStream(file));
	}

}
