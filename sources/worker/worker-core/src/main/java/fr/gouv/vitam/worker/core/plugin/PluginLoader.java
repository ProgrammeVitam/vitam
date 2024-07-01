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
package fr.gouv.vitam.worker.core.plugin;

import com.google.common.base.Strings;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.i18n.PluginPropertiesLoader;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.security.SafeFileChecker;
import fr.gouv.vitam.processing.common.exception.InvocationPluginException;
import fr.gouv.vitam.processing.common.exception.PluginNotFoundException;
import fr.gouv.vitam.worker.common.PluginProperties;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static fr.gouv.vitam.common.PropertiesUtils.getConfigAsStream;
import static java.lang.String.format;

/**
 * load all the plugins according to plugins.json file.
 */
public class PluginLoader {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PluginLoader.class);

    private static final String PLUGIN_CONFIG_FILE = "plugins.json";
    private static final String WORKER_PLUGIN_WORKSPACE = "plugins-workspace";

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
    public PluginLoader() throws IllegalPathException {
        this(PLUGIN_CONFIG_FILE);
    }

    /**
     * create instance with a custom configuration file
     *
     * @param pluginsConfigFile path of the custom configuration file.
     */
    PluginLoader(String pluginsConfigFile) throws IllegalPathException {
        LOGGER.debug("Load plugin files : " + pluginsConfigFile);
        SafeFileChecker.checkSafeFilePath(VitamConfiguration.getVitamConfigFolder(), pluginsConfigFile);
        this.pluginsConfigFile = pluginsConfigFile;
        this.plugins = new HashMap<>();
    }

    /**
     * load the configuration file containing the list of plugins. file is a json format.
     * <p>
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
     *
     * @throws FileNotFoundException if file is not found
     * @throws InvalidParseOperationException if file cannot be parsed
     * @throws PluginNotFoundException if the plugin is not found in the classpath
     */
    public void loadConfiguration()
        throws FileNotFoundException, InvalidParseOperationException, PluginNotFoundException {
        Map<String, PluginProperties> mapFromInputStream = JsonHandler.getMapFromInputStream(
            getConfigAsStream(pluginsConfigFile),
            PluginProperties.class
        );
        for (Map.Entry<String, PluginProperties> pluginPropertiesEntry : mapFromInputStream.entrySet()) {
            PluginProperties pluginProperties = pluginPropertiesEntry.getValue();
            final Optional<Class<ActionHandler>> actionHandlerClazz;

            if (Strings.isNullOrEmpty(pluginProperties.getJarName())) {
                actionHandlerClazz = loadInternalPlugins(pluginPropertiesEntry.getKey(), pluginProperties);
            } else {
                actionHandlerClazz = loadExternalPlugins(pluginPropertiesEntry.getKey(), pluginProperties);
                LOGGER.debug(
                    "Load external plugin name : {}",
                    actionHandlerClazz.isPresent() ? actionHandlerClazz.get().getName() : "null"
                );
            }
            if (actionHandlerClazz.isPresent()) {
                plugins.put(
                    pluginPropertiesEntry.getKey(),
                    new PluginConfiguration(pluginProperties.getPropertiesFile(), actionHandlerClazz.get())
                );
            }
        }
    }

    private Optional<Class<ActionHandler>> loadInternalPlugins(String handlerID, PluginProperties pluginProperties)
        throws PluginNotFoundException {
        Class<ActionHandler> actionHandlerClazz;
        try {
            if (StringUtils.isNotEmpty(pluginProperties.getPropertiesFile())) {
                SafeFileChecker.checkSafeRessourceFilePath(pluginProperties.getPropertiesFile());
                PluginPropertiesLoader.loadProperties(handlerID, pluginProperties.getPropertiesFile());
            }

            actionHandlerClazz = (Class<ActionHandler>) Thread.currentThread()
                .getContextClassLoader()
                .loadClass(pluginProperties.getClassName());
        } catch (ClassNotFoundException | IllegalPathException e) {
            LOGGER.error("could not find class: {}", pluginProperties.getClassName());
            throw new PluginNotFoundException(format("could not find class: %s", pluginProperties.getClassName()), e);
        }
        return Optional.of(actionHandlerClazz);
    }

    private Optional<Class<ActionHandler>> loadExternalPlugins(String handlerID, PluginProperties pluginProperties) {
        try {
            File jarFile = SafeFileChecker.checkSafeFilePath(
                VitamConfiguration.getVitamConfigFolder(),
                WORKER_PLUGIN_WORKSPACE,
                pluginProperties.getJarName()
            );
            if (!jarFile.exists()) {
                LOGGER.error(
                    "Jar file {} not found in {} folder. FullPath {}",
                    pluginProperties.getJarName(),
                    WORKER_PLUGIN_WORKSPACE,
                    jarFile.getAbsolutePath()
                );
                return Optional.empty();
            }
            URL[] urls = new URL[1];
            urls[0] = jarFile.toURI().toURL();
            URLClassLoader pluginLoader = new URLClassLoader(urls);

            // Load properties file
            String propertiesFile = pluginProperties.getPropertiesFile();
            if (StringUtils.isNotEmpty(propertiesFile)) {
                SafeFileChecker.checkSafeRessourceFilePath(propertiesFile);
                PluginPropertiesLoader.loadProperties(handlerID, propertiesFile, pluginLoader);
            }
            SafeFileChecker.checkSafeRessourceFilePath(pluginProperties.getClassName());
            return Optional.of((Class<ActionHandler>) pluginLoader.loadClass(pluginProperties.getClassName()));
        } catch (ClassNotFoundException | IOException | IllegalPathException e) {
            LOGGER.error(
                "could not find class: " +
                pluginProperties.getClassName() +
                ". the jar file " +
                pluginProperties.getJarName() +
                " should be be in " +
                WORKER_PLUGIN_WORKSPACE +
                " folder",
                e
            );
            return Optional.empty();
        }
    }

    /**
     * test if a plugin is present
     *
     * @param pluginId id plugin
     * @return true if present, false otherwise
     */
    public boolean contains(String pluginId) {
        return plugins.containsKey(pluginId);
    }

    /**
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
     *
     * @return list of all plugins
     * @throws InvocationPluginException if a plugin cannot be instance
     */
    public Map<String, ActionHandler> loadAllPlugins() throws InvocationPluginException {
        Map<String, ActionHandler> actionHandlers = new HashMap<>();
        for (Map.Entry<String, PluginConfiguration> configurationEntry : plugins.entrySet()) {
            try (ActionHandler actionHandler = configurationEntry.getValue().newInstance()) {
                actionHandlers.put(configurationEntry.getKey(), actionHandler);
                // Exception is used here because Class.newInstance propagate the exception launched by the constructor.
            } catch (Exception e) {
                throw new InvocationPluginException(
                    format("could not instance plugin with action Id: %s", configurationEntry.getKey()),
                    e
                );
            }
        }
        return actionHandlers;
    }
}
