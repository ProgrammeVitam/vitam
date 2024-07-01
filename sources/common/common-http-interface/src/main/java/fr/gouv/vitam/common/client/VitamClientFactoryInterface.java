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
package fr.gouv.vitam.common.client;

import fr.gouv.vitam.common.client.configuration.ClientConfiguration;

import javax.ws.rs.client.Client;
import java.util.Map;

/**
 * Vitam Http Client Factory Interface
 *
 * @param <T> the type of the Vitam client returned
 */
public interface VitamClientFactoryInterface<T extends MockOrRestClient> {
    /**
     * Get the internal Http client
     *
     * @return the client
     */
    Client getHttpClient();

    /**
     * Get the internal Http client according to the chunk mode
     *
     * @param useChunkedMode
     * @return the client
     */
    Client getHttpClient(boolean useChunkedMode);

    /**
     * This method returns the correct Client adapted to the business case
     *
     * @return the Vitam client
     */
    T getClient();

    /**
     * Get the resource path of the server.
     *
     * @return the resource path as string
     */
    String getResourcePath();

    /**
     * Get the service URL
     *
     * @return the service URL
     */
    String getServiceUrl();

    /**
     * @return the Default Client configuration (Chunked Mode)
     */
    Map<VitamRestEasyConfiguration, Object> getDefaultConfigCient();

    /**
     * @param chunkedMode
     * @return the Default Client configuration according to the chunked mode
     */
    Map<VitamRestEasyConfiguration, Object> getDefaultConfigCient(boolean chunkedMode);

    /**
     * @return the Vitam client configuration
     */
    ClientConfiguration getClientConfiguration();

    /**
     * @return the current {@link VitamClientType}
     */
    VitamClientType getVitamClientType();

    /**
     * @param vitamClientType to set
     * @return this
     */
    VitamClientFactoryInterface<?> setVitamClientType(VitamClientType vitamClientType);

    /**
     * Change the server resourcePath to use. Only in JUNIT
     *
     * @param resourcePath
     */
    void changeResourcePath(String resourcePath);

    /**
     * Change the server port to use. Only in JUNIT
     *
     * @param port
     */
    void changeServerPort(int port);

    /**
     * Shutdown the Factory
     */
    void shutdown();

    /**
     * Reset the client into the Client pool if possible
     *
     * @param client
     * @param chunk
     */
    void resume(Client client, boolean chunk);

    /**
     * enum to define client type
     */
    enum VitamClientType {
        /**
         * To use only in MOCK ACCESS
         */
        MOCK,
        /**
         * Use real service (need server to be set)
         */
        PRODUCTION,
    }
}
