/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 * 
 * This software is a computer program whose purpose is to implement a digital 
 * archiving back-office system managing high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.worker.core.plugin;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.i18n.PluginPropertiesLoader;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.processing.common.exception.PluginNotFoundException;
import fr.gouv.vitam.worker.common.PluginProperties;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

/**
 * Plugin Helper
 */
public class PluginHelper {
    
    private static final String PLUGIN_CONFIG_FILE = "plugins.json";
    private static final String PLUGIN_NOT_FOUND = "Plugin not found exception: ";
    private PluginHelper() {}
    
    /**
     * Get list of plugin
     * @return PluginList
     */
    public static Map<String, Object> getPluginList() {
        try {
            return JsonHandler.getMapFromInputStream(PropertiesUtils.getConfigAsStream(PLUGIN_CONFIG_FILE));
        } catch (InvalidParseOperationException | FileNotFoundException e) {
            return new HashMap<>();
        }
    }
    
    /**
     * Load action Handler
     * @param actionId
     * @param plugin
     * @return action Handler
     * @throws PluginNotFoundException
     */
    public static ActionHandler loadActionHandler(String actionId, PluginProperties plugin) throws PluginNotFoundException {
        try {
            PluginPropertiesLoader.loadProperties(actionId, plugin.getPropertiesFile());
            return (ActionHandler) Thread.currentThread().getContextClassLoader()
                .loadClass(plugin.getClassName()).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new PluginNotFoundException(PLUGIN_NOT_FOUND, e);
        }
    }

}
