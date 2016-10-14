/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.storage.offers.workspace.rest;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.server.application.configuration.VitamApplicationConfiguration;

/**
 * Workspace offer configuration.
 */
public class DefaultOfferConfiguration implements VitamApplicationConfiguration {
    private String contextPath;
    private String storagePath;
    private String jettyConfig;


    private static final String CONFIGURATION_PARAMETERS = "DefaultOfferConfiguration parameters";


    /**
     * DefaultOfferConfiguration empty constructor for YAMLFactory
     */
    public DefaultOfferConfiguration() {
        // empty
    }


    /**
     * Construct an DefaultOfferConfiguration manually with a specific contextPath
     *
     * @param storagePath path of the storage
     * @param contextPath the application context path
     */
    public DefaultOfferConfiguration(String storagePath, String contextPath) {
        ParametersChecker.checkParameter(CONFIGURATION_PARAMETERS, contextPath);
        this.contextPath = contextPath;
        this.storagePath = storagePath;
    }

    /**
     * getter for context path
     *
     * @return String
     */
    public String getContextPath() {
        return contextPath;
    }


    /**
     * @param contextPath the context path to set
     * @return this
     * @throws IllegalArgumentException if contextPath is null or empty
     */
    public DefaultOfferConfiguration setContextPath(String contextPath) {
        ParametersChecker.checkParameter(CONFIGURATION_PARAMETERS, contextPath);
        this.contextPath = contextPath;
        return this;
    }

    /**
     * getter for storage path
     *
     * @return String
     */
    public String getStoragePath() {
        return storagePath;
    }

    /**
     * setter for storage path
     *
     * @param storagePath as String, path to storage
     * @return this
     */
    public DefaultOfferConfiguration setStoragePath(String storagePath) {
        this.storagePath = storagePath;
        return this;
    }

    /**
     * getter of jetty config
     *
     * @return the jetty config
     */
    public String getJettyConfig() {
        return jettyConfig;
    }

    /**
     * setter of jetty config
     *
     * @param jettyConfig the jetty config to be set
     * @return default configuration
     */
    public DefaultOfferConfiguration setJettyConfig(String jettyConfig) {
        this.jettyConfig = jettyConfig;
        return this;
    }

}
