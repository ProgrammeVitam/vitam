/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Utility class for Junit
 */
public class ResourcesPrivateUtilTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PrivateUtilTest.class);

    public static final String GUID_TEST_PROPERTIES = "GUID-test.properties";
    public static final String SHOULD_HAVE_AN_EXCEPTION = "Should have an exception";
    public static final String SHOULD_NOT_HAVE_AN_EXCEPTION = "Should not have an exception";
    public static final String CANNOT_FIND_RESOURCES_TEST_FILE = "CANNOT FIND RESOURCES TEST FILE";

    private final File guidTestPropertiesFile;

    private ResourcesPrivateUtilTest() {
        guidTestPropertiesFile = getTestResourcesFile(GUID_TEST_PROPERTIES);
    }

    /**
     * @return the GUID Properties File
     */
    public final File getGuidTestPropertiesFile() {
        return guidTestPropertiesFile;
    }

    /**
     * @return the ResourcesPublicUtilTest instance
     */
    public static ResourcesPrivateUtilTest getInstance() {
        return new ResourcesPrivateUtilTest();
    }

    private File getTestResourcesFile(String name) {
        File file;
        try {
            file = PropertiesUtils.getResourceFile(name);
        } catch (final FileNotFoundException e) { // NOSONAR
            LOGGER.debug("Not able to load: " + name);
            return null;
        }
        if (file != null && file.exists()) {
            return file;
        }

        return null;
    }

    /**
     * Utility class for Junit
     */
    public static class PrivateUtilTest {

        private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PrivateUtilTest.class);

        private static final String SERVER_IDENTITY_PROPERTIES_FILE = "ServerIdentity.properties";
        public static final String SERVER_IDENTITY_YAML_FILE = "server-identity.conf";

        public static final String SHOULD_RAIZED_AN_EXCEPTION = "Should raized an exception";
        public static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raized an exception";
        public static final String EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION =
            "Expecting exception: IllegalArgumentException";
        public static final String CANNOT_FIND_RESOURCES_TEST_FILE = "CANNOT FIND RESOURCES TEST FILE";

        private final File serverIdentityPropertiesFile;
        private final File serverIdentityYamlFile;

        private PrivateUtilTest() {
            serverIdentityPropertiesFile = getTestResourcesFile(SERVER_IDENTITY_PROPERTIES_FILE);
            serverIdentityYamlFile = getTestResourcesFile(SERVER_IDENTITY_YAML_FILE);
        }

        /**
         * @return the serverIdentityPropertiesFile
         */
        public final File getServerIdentityPropertiesFile() {
            return serverIdentityPropertiesFile;
        }

        /**
         * @return the serverIdentityYamlFile
         */
        public final File getServerIdentityYamlFile() {
            return serverIdentityYamlFile;
        }

        /**
         * @return the PrivateUtilTest instance
         */
        public static PrivateUtilTest getInstance() {
            return new PrivateUtilTest();
        }

        private File getTestResourcesFile(String name) {
            File file;
            try {
                file = PropertiesUtils.getResourceFile(name);
            } catch (final FileNotFoundException e) { // NOSONAR
                LOGGER.debug("Not able to load: " + name);
                return null;
            }
            if (file != null && file.exists()) {
                return file;
            }

            return null;
        }
    }
}
