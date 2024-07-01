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
package fr.gouv.vitam.workspace.client;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;

import java.net.URI;

/**
 * WorkspaceClient factory for creating workspace client
 */
public class WorkspaceClientFactory extends VitamClientFactory<WorkspaceClient> {

    private static final WorkspaceClientFactory WORKSPACE_CLIENT_FACTORY = new WorkspaceClientFactory("/workspace/v1");
    private static final WorkspaceClientFactory WORKSPACE_COLLECT_CLIENT_FACTORY = new WorkspaceClientFactory(
        "/workspace-collect/v1"
    );

    private WorkspaceClientFactory(String resourcePath) {
        super(null, resourcePath);
    }

    /**
     * @return the instance
     */
    public static WorkspaceClientFactory getInstance() {
        return getInstance(WorkspaceType.VITAM);
    }

    /**
     * @param workspaceType type of workspace VITAM | COLLECT
     * @return an instance of WorkspaceClientFactory based on the type of workspace
     */
    public static WorkspaceClientFactory getInstance(WorkspaceType workspaceType) {
        if (workspaceType == WorkspaceType.VITAM) {
            return WORKSPACE_CLIENT_FACTORY;
        } else if (workspaceType == WorkspaceType.COLLECT) {
            return WORKSPACE_COLLECT_CLIENT_FACTORY;
        }
        return null;
    }

    @Override
    public WorkspaceClient getClient() {
        return new WorkspaceClient(this);
    }

    /**
     * change mode client by server url
     *
     * @param serviceUrl as String
     */
    public static void changeMode(String serviceUrl) {
        ParametersChecker.checkParameter("Server Url can not be null", serviceUrl);
        final URI uri = URI.create(serviceUrl);
        final ClientConfiguration configuration = new ClientConfigurationImpl(uri.getHost(), uri.getPort());
        changeMode(configuration);
    }

    /**
     * change mode client by server url
     *
     * @param serviceUrl as String
     */
    public static void changeMode(String serviceUrl, WorkspaceType workspaceType) {
        ParametersChecker.checkParameter("Server Url can not be null", serviceUrl);
        final URI uri = URI.create(serviceUrl);
        final ClientConfiguration configuration = new ClientConfigurationImpl(uri.getHost(), uri.getPort());
        changeMode(configuration, workspaceType);
    }

    /**
     * @param configuration null for MOCK
     */
    private static void changeMode(ClientConfiguration configuration) {
        for (WorkspaceType type : WorkspaceType.values()) {
            getInstance(type).initialisation(configuration, getInstance(type).getResourcePath());
        }
    }

    /**
     * @param configuration null for MOCK
     */
    private static void changeMode(ClientConfiguration configuration, WorkspaceType workspaceType) {
        getInstance(workspaceType).initialisation(configuration, getInstance(workspaceType).getResourcePath());
    }
}
