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

package fr.gouv.vitam.common.server.application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jetty.server.Handler;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;

/**
 * Abstract implementation of VitamApplication which handle common tasks for all sub-implementation
 * 
 * @param <A> VitamApplication final class
 * @param <C> VitamApplicationConfiguration final class
 */
public abstract class AbstractVitamApplication<A extends VitamApplication<?>, C> implements VitamApplication<A> {
    private C configuration;
    private Handler applicationHandler;
    private final Class<A> applicationType;
    private final Class<C> configurationType;

    /**
     * Protected constructor assigning application and configuration types Usage example in sub-implementation : class
     * MyApplication extends AbstractVitamApplication<MyApplication, MyApplicationConfiguration> { protected
     * MyApplication() { super(MyApplication.class, MyApplicationConfiguration.class); } }
     *
     * @param applicationType the application class type
     * @param configurationType the configuration class type
     */
    protected AbstractVitamApplication(Class<A> applicationType, Class<C> configurationType) {
        this.applicationType = applicationType;
        this.configurationType = configurationType;
    }

    /**
     * Implement this method to construct your application specific handler
     *
     * @return the generated Handler
     */
    protected abstract Handler buildApplicationHandler();

    /**
     * Must return the name as a string of your configuration file. Example : "logbook.conf"
     *
     * @return the name of the application configuration file
     */
    protected abstract String getConfigFilename();

    @Override
    public A configure(Path configPath) throws VitamApplicationServerException {
        if (configPath == null) {
            throw new IllegalArgumentException();
        }

        try {
            setConfiguration(PropertiesUtils.readYaml(
                configPath, getConfigurationType()));
        } catch (final IOException exc) {
            throw new VitamApplicationServerException("An error occurred while reading the configuration file or " +
                "during the YAML to Java mapping", exc);
        }
        setApplicationHandler(buildApplicationHandler());
        return getApplicationType().cast(this);
    }

    /**
     * Compute a java Path using the command line arguments if any. Otherwise uses the default configuration file name
     *
     * @param args the command line arguments
     * @return the path to the found
     * @throws VitamApplicationServerException if a problem occurs
     */
    public Path computeConfigurationPathFromInputArguments(String... args) throws VitamApplicationServerException {
        Path configurationFile = null;
        if (args != null && args.length >= 1) {
            try {
                final File file = PropertiesUtils.findFile(args[0]);
                configurationFile = file.toPath();
            } catch (final FileNotFoundException e) {// NOSONAR ignore yet
                // ignore
            }
        }
        if (configurationFile == null) {
            try {
                final File file = PropertiesUtils.findFile(getConfigFilename());
                configurationFile = file.toPath();
            } catch (final FileNotFoundException e) {// NOSONAR ignore yet
                // ignore
            }
        }
        if (configurationFile == null) {
            throw new VitamApplicationServerException(
                String.format("Configuration file not found '%s'",
                    getConfigFilename()));
        }
        return configurationFile;
    }

    /**
     * @return the configuration
     */
    public C getConfiguration() {
        return configuration;
    }

    protected void setConfiguration(C configuration) {
        this.configuration = configuration;
    }

    protected Class<A> getApplicationType() {
        return applicationType;
    }

    protected Class<C> getConfigurationType() {
        return configurationType;
    }

    /**
     * @return the application Handler
     */
    public Handler getApplicationHandler() {
        return applicationHandler;
    }

    private void setApplicationHandler(Handler applicationHandler) {
        this.applicationHandler = applicationHandler;
    }
}
