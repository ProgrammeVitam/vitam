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
package fr.gouv.vitam.worker.core.plugin;

import static fr.gouv.vitam.common.PropertiesUtils.getConfigAsStream;
import static java.lang.String.format;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.processing.common.exception.InvocationPluginException;
import fr.gouv.vitam.processing.common.exception.PluginNotFoundException;
import fr.gouv.vitam.worker.common.PluginProperties;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

/**
 * load all the plugins according to plugins.json file.
 */
public class PluginLoader {

    private final static String PLUGIN_CONFIG_FILE = "plugins.json";

    /**
     * list of plugins
     */
    private Map<String, PluginConfiguration> plugins;

    /**
     * file describing a list of plugins
     */
    private final String pluginsConfigFile;

    /**
     * create instance with the default configuration file
     */
    public PluginLoader() {
        this(PLUGIN_CONFIG_FILE);
    }

    /**
     * create instance with a custom configuration file
     * @param pluginsConfigFile path of the custom configuration file.
     */
    public PluginLoader(String pluginsConfigFile) {
        this.pluginsConfigFile = pluginsConfigFile;
        this.plugins = new HashMap<>();
    }

    /**
     * load the configuration file containing the list of plugins. file is a json format.
     *
     * Example :
     * <pre>
     * {@code
     *
     * {
     *   "CHECK_DIGEST": {
     *     "className": "fr.gouv.vitam.worker.core.plugin.CheckConformityActionPlugin",
     *     "propertiesFile": "check_conformity_plugin.properties"
     *   }
     * }
     * }
     * </pre>
     * @throws FileNotFoundException if file is not found
     * @throws InvalidParseOperationException if file cannot be parsed
     * @throws PluginNotFoundException if the plugin is not found in the classpath
     */
    public void loadConfiguration() throws FileNotFoundException, InvalidParseOperationException, PluginNotFoundException {
        Map<String, PluginProperties> mapFromInputStream =
            JsonHandler.getMapFromInputStream(getConfigAsStream(pluginsConfigFile), PluginProperties.class);

        for (String pluginId : mapFromInputStream.keySet()) {
            PluginProperties pluginProperties = mapFromInputStream.get(pluginId);
            Class<ActionHandler> actionHandlerClazz = PluginHelper.loadActionHandler(pluginId, pluginProperties);
            plugins.put(pluginId, new PluginConfiguration(pluginProperties.getPropertiesFile(), actionHandlerClazz));
        }
    }

    /**
     * test if a plugin is present
     * @param pluginId id plugin
     * @return true if present, false otherwise
     */
    public boolean contains(String pluginId) {
        return plugins.containsKey(pluginId);
    }

    /**
     *
     * @param pluginId id of a plugin
     * @return an  new instance of a plugin
     * @throws InvocationPluginException the plugin cannot be instanciate.
     * @throws PluginNotFoundException the plugin is not present
     */
    public ActionHandler newInstance(String pluginId) throws InvocationPluginException, PluginNotFoundException {
        PluginConfiguration pluginConfiguration = plugins.get(pluginId);
        if (!contains(pluginId)) {
            throw new PluginNotFoundException(format("Cannot find plugin for id: %s", pluginId));
        }
        try {
            return pluginConfiguration.newInstance();
            // Exception is used here because Class.newInstance propagate the exception launched by the constructor.
        } catch (Exception e) {
            throw new InvocationPluginException("could not instance plugin with action Id: " + pluginId, e);
        }
    }

    /**
     * load All the plugins, and return a Map an instance of each plugin
     * WARNING : plugins are not thread safe
     * @return list of all plugins
     * @throws InvocationPluginException if a plugin cannot be instance
     */
    public Map<String, ActionHandler> loadAllPlugins() throws InvocationPluginException {
        Map<String, ActionHandler> actionHandlers = new HashMap<>();
        for (Map.Entry<String, PluginConfiguration> configurationEntry : plugins.entrySet()) {
            try {
                ActionHandler actionHandler = configurationEntry.getValue().newInstance();
                actionHandlers.put(configurationEntry.getKey(), actionHandler);
                // Exception is used here because Class.newInstance propagate the exception launched by the constructor.
            } catch (Exception e) {
                throw new InvocationPluginException(
                    format("could not instance plugin with action Id: %s", configurationEntry.getKey()), e);
            }
        }
        return actionHandlers;
    }

}
