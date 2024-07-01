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
package fr.gouv.vitam.common.server.application.configuration;

/**
 * Common interface for all application configuration.
 */
public interface VitamApplicationConfigurationInterface {
    /**
     * getter jettyConfig
     *
     * @return the Jetty config filename
     */
    String getJettyConfig();

    /**
     * setter jettyConfig
     *
     * @param jettyConfig the jetty config to set
     * @return this
     */
    VitamApplicationConfigurationInterface setJettyConfig(String jettyConfig);

    /**
     * getter authentication
     *
     * @return true if authentication is on for the application, false if not
     */
    boolean isAuthentication();

    /**
     * @param authentication the authentication to set
     * @return this
     */
    VitamApplicationConfigurationInterface setAuthentication(boolean authentication);

    /**
     * getter tenantFilter
     *
     * @return true if tenant Filtering is on for the application, false if not
     */
    boolean isTenantFilter();

    VitamApplicationConfigurationInterface setTenantFilter(boolean tenantFilter);

    default String getBaseUrl() {
        return null;
    }

    default String getBaseUri() {
        return null;
    }

    default VitamApplicationConfigurationInterface setBaseUrl(String baseUrl) {
        return this;
    }

    default VitamApplicationConfigurationInterface setBaseUri(String baseUri) {
        return this;
    }
}
