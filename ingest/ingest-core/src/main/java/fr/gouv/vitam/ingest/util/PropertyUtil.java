/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.ingest.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by bsui on 19/05/16.
 */
public class PropertyUtil {

    public static final String PROPERTY_FILES_DIR = "/vitam/conf";
    public static final String PROPERTIES_FILE = "ingest-core.properties";
    public static final String PROPERTY_FILE_UPLOAD_DIR = "ingest.core.uploaded.dir";
    private static final String INGEST_MODULE_DIR = "ingest-core";

    // FIXME REVIEW Use Common PropertyUtils

    private static PropertyUtil instance;

    private PropertyUtil() {}

    public static Properties loadProperties() throws IOException {
        Properties properties;
        final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPERTIES_FILE);
        properties = new Properties();
        properties.load(is);
        return properties;
    }

    public static Properties loadProperties(String propertiesFile, String moduleDir) throws IOException {
        Properties properties;

        final InputStream is = new FileInputStream(PROPERTY_FILES_DIR + "/" + propertiesFile);
        properties = new Properties();
        properties.load(is);
        return properties;
    }
}
