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
import java.io.InputStream;

/**
 * Utility class for Junit
 */
public class ResourcesPublicUtilTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ResourcesPublicUtilTest.class);

    public static final String GUID_TEST_PROPERTIES = "GUID-test.properties";
    private static final String JSON_TEST_JSON = "json-test.json";
    private static final String JSON_TEST2_JSON = "json-test2.json";
    private static final String JSON_TEST3_JSON = "json-test3.json";
    private static final String JSON_TEST_EMPTY_JSON = "json-test-empty.json";
    public static final String YAML_TEST_CONF = "yaml-test.conf";

    public static final String SHOULD_HAVE_AN_EXCEPTION = "Should have an exception";
    public static final String SHOULD_NOT_HAVE_AN_EXCEPTION = "Should not have an exception";
    public static final String EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION =
        "Expecting exception: IllegalArgumentException";
    static final String SHOULD_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION = "Should raized IllegalArgumentException";
    static final String SHOULD_NOT_RAIZED_ILLEGAL_ARGUMENT_EXCEPTION = "Should not raized IllegalArgumentException";
    static final String EXPECTING_EXCEPTION_FILE_NOT_FOUND_EXCEPTION = "Expecting exception: FileNotFoundException";
    public static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raized an exception";
    public static final String SHOULD_RAIZED_AN_EXCEPTION = "Should raized an exception";
    public static final String CANNOT_FIND_RESOURCES_TEST_FILE = "CANNOT FIND RESOURCES TEST FILE";

    private final File guidTestPropertiesFile;
    private final File jsonTestJsonFile;
    private final File jsonTest2JsonFile;

    private ResourcesPublicUtilTest() {
        guidTestPropertiesFile = getTestResourcesFile(GUID_TEST_PROPERTIES);
        jsonTestJsonFile = getTestResourcesFile(JSON_TEST_JSON);
        jsonTest2JsonFile = getTestResourcesFile(JSON_TEST2_JSON);
    }

    /**
     * @return the GUID Properties File
     */
    public final File getGuidTestPropertiesFile() {
        return guidTestPropertiesFile;
    }

    /**
     * @return the Json File
     */
    public final File getJsonTestJsonFile() {
        return jsonTestJsonFile;
    }

    /**
     * @return the Json2 File
     */
    public final File getJsonTest2JsonFile() {
        return jsonTest2JsonFile;
    }

    /**
     * getJsonTest1JsonInputStream
     *
     * @return the Json1 Stream
     */
    public InputStream getJsonTest1JsonInputStream() {
        return getTestResourcesInputStream(JSON_TEST_JSON);
    }

    /**
     * @return the Json3 Stream
     */
    public InputStream getJsonTest3JsonInputStream() {
        return getTestResourcesInputStream(JSON_TEST3_JSON);
    }

    /**
     * @return the Json empty Stream
     */
    public InputStream getJsonTestEmptyJsonInputStream() {
        return getTestResourcesInputStream(JSON_TEST_EMPTY_JSON);
    }

    /**
     * @return the ResourcesPublicUtilTest instance
     */
    public static ResourcesPublicUtilTest getInstance() {
        return new ResourcesPublicUtilTest();
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

    private InputStream getTestResourcesInputStream(String name) {
        InputStream stream;
        try {
            stream = PropertiesUtils.getResourceAsStream(name);
        } catch (final FileNotFoundException e) { // NOSONAR
            LOGGER.debug("Not able to load: " + name);
            return null;
        }
        if (stream != null) {
            return stream;
        }

        return null;
    }
}
