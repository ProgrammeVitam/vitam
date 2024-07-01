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
package fr.gouv.vitam.common.format.identification.siegfried;

import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;

/**
 * Siegfield Client factory
 */
public class SiegfriedClientFactory extends VitamClientFactory<SiegfriedClient> {

    private static final SiegfriedClientFactory Siegfried_CLIENT_FACTORY = new SiegfriedClientFactory();
    private static final String RESOURCE_PATH = "/identify";

    private SiegfriedClientFactory() {
        super(null, RESOURCE_PATH, false);
        disableUseAuthorizationFilter();
    }

    /**
     * Get the SiegfriedClientFactory instance
     *
     * @return the instance
     */
    public static final SiegfriedClientFactory getInstance() {
        return Siegfried_CLIENT_FACTORY;
    }

    /**
     * Get the default type Siegfried client
     *
     * @return the default Siegfried client
     */
    @Override
    public SiegfriedClient getClient() {
        SiegfriedClient client;
        switch (getVitamClientType()) {
            case MOCK:
                client = new SiegfriedClientMock();
                break;
            case PRODUCTION:
                client = new SiegfriedClientRest(this);
                break;
            default:
                throw new IllegalArgumentException("Log type unknown");
        }
        return client;
    }

    /**
     * Change client configuration from server/host params
     *
     * @param server the server param
     * @param port the port params
     */
    final void changeConfiguration(String server, int port) {
        if (server == null || server.isEmpty() || port <= 0) {
            initialisation(null, getResourcePath());
            return;
        }
        initialisation(new ClientConfigurationImpl(server, port), getResourcePath());
    }

    /**
     * @param server the server name to instance
     * @param port the server port to instance
     */
    // TODO P2 should not be public (but IT test)
    public static final void changeMode(String server, int port) {
        getInstance().initialisation(new ClientConfigurationImpl(server, port), getInstance().getResourcePath());
    }

    /**
     * @param configuration null for MOCK
     */
    // TODO P2 should not be public (but IT test)
    public static final void changeMode(ClientConfiguration configuration) {
        getInstance().initialisation(configuration, getInstance().getResourcePath());
    }
}
