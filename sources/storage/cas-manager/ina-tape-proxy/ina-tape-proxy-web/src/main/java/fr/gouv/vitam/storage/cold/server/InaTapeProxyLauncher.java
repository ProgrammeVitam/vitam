/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.cold.server;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.serverv2.VitamStarter;
import fr.gouv.vitam.common.serverv2.application.AdminApplication;
import fr.gouv.vitam.storage.cold.InaTapeProxyConfiguration;

import java.io.File;
import java.net.URL;

public class InaTapeProxyLauncher {

    private static final String CONFIGURATION_FILENAME = "ina-tape-proxy-web.conf";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(InaTapeProxyLauncher.class);
    private static VitamStarter SERVER;

    private InaTapeProxyLauncher() {
        // Utility class, prevent instantiation
    }

    public static void main(String[] args) {
        launch(args, true);
    }

    public static void start(String[] args) {
        launch(args, false);
    }

    public static void stop() {
        if (SERVER == null) {
            LOGGER.warn("Server is not running or already stopped.");
            return;
        }

        try {
            LOGGER.info("Stopping Cold Storage server...");
            SERVER.stop();
            LOGGER.info("Server stopped successfully.");
        } catch (VitamApplicationServerException e) {
            LOGGER.error("Error while stopping server", e);
            // Ne pas System.exit en test
            throw new RuntimeException("Failed to stop server", e);
        }
    }

    /**
     * Common startup logic for main() and tests.
     *
     * @param args command-line arguments
     * @param blocking true for main() / false for tests
     */
    private static void launch(String[] args, boolean blocking) {
        try {
            String resolvedConfigPath = resolveConfigurationFile(parseArguments(args));
            SERVER = new InaTapeProxyServer(
                InaTapeProxyConfiguration.class,
                resolvedConfigPath,
                InaTapeProxyApplication.class,
                AdminApplication.class
            );

            addShutdownHook();

            LOGGER.info("Starting Cold Storage server...");
            if (blocking) {
                SERVER.run(); // blocks
            } else {
                SERVER.start(); // non-blocking, suitable for tests
            }
            LOGGER.info("Cold Storage server stopped.");
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid configuration: " + e.getMessage(), e);
            if (blocking) System.exit(2);
        } catch (VitamApplicationServerException e) {
            LOGGER.error("Failed to start Cold Storage server", e);
            if (blocking) System.exit(1);
        } catch (Exception e) {
            LOGGER.error("Unexpected error during startup", e);
            if (blocking) System.exit(99);
        }
    }

    private static void addShutdownHook() {
        Runtime.getRuntime()
            .addShutdownHook(
                new Thread(() -> {
                    try {
                        if (SERVER != null) {
                            LOGGER.info("Stopping Cold Storage server...");
                            SERVER.stop();
                            LOGGER.info("Server stopped successfully.");
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error while stopping server in shutdown hook", e);
                    }
                })
            );
    }

    /**
     * Parse command-line arguments to retrieve the configuration file path.
     * @param args command-line arguments
     * @return configuration file path or null if not provided
     */
    private static String parseArguments(String[] args) {
        if (args == null || args.length == 0) {
            LOGGER.info("No configuration file argument provided; will try to load from classpath.");
            return null;
        }
        String path = args[0];
        LOGGER.info("Using configuration file from argument: " + path);
        return path;
    }

    /**
     * Resolve configuration file: prefer file system path, fallback to classpath resource.
     * @param filePath configuration file path (may be null)
     * @return resolved configuration path (filesystem or classpath URL)
     */
    private static String resolveConfigurationFile(String filePath) {
        if (filePath != null) {
            File configFile = new File(filePath);
            if (!configFile.exists()) {
                throw new IllegalArgumentException("Configuration file not found at: " + filePath);
            }
            if (!configFile.isFile() || !configFile.canRead()) {
                throw new IllegalArgumentException("Configuration file is not readable: " + filePath);
            }
            LOGGER.info("Configuration file found on filesystem: " + configFile.getAbsolutePath());
            return configFile.getAbsolutePath();
        }

        // Fallback: try loading from classpath
        URL resourceUrl = InaTapeProxyLauncher.class.getClassLoader().getResource(CONFIGURATION_FILENAME);
        if (resourceUrl != null) {
            LOGGER.info("Configuration file loaded from classpath: " + CONFIGURATION_FILENAME);
            return resourceUrl.getPath(); // Return URL path for compatibility with VitamStarter
        }

        throw new IllegalArgumentException(
            "No configuration file provided and no " + CONFIGURATION_FILENAME + " found in classpath."
        );
    }
}
