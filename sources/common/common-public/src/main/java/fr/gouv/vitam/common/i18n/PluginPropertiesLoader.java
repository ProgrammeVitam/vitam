/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.common.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.SysErrLogger;

/**
 * Plugin Messages Helper for Logbooks
 */
public class PluginPropertiesLoader {

    static Map<String, String> resourceMap = new HashMap<>();
    private static final String PLUGIN = "PLUGIN";
    
    /**
     * Private constructor
     */
    private PluginPropertiesLoader() {}
    
    /**
     * loadProperties from handler Id and properties file
     * @param handlerID the handler id to load
     * @param propertyFilename the property file name
     */
    public static void loadProperties(String handlerID, String propertyFilename) {
        try (InputStream inputStream = PropertiesUtils.getResourceAsStream(propertyFilename)) {
            ResourceBundle resourceBundle = new PropertyResourceBundle(new InputStreamReader(inputStream, CharsetUtils.UTF8));
            for (final String key : resourceBundle.keySet()) {
                final String value = resourceBundle.getString(key);
                resourceMap.put(key.replaceAll(PLUGIN, handlerID), value);
            }
        } catch (IOException e1) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
        }
    }
    
    /**
     * @param message the message id to get from resource map 
     * @return message detail
     */
    public static final String getString(String message) {
        return resourceMap.get(message);
    }
}
