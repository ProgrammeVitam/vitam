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
package fr.gouv.vitam.processing.common.utils;

import java.net.URI;
import java.util.List;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * Class Utils to get Objects from a container into the workspace
 */
public class ContainerExtractionUtils {

    private WorkspaceClientFactory workspaceClientFactory;
    // TODO
    // Retrieve the hard code value for the path of the folder of digital objects
    private final String digitalObjectFolderName = "sip/content";

    public ContainerExtractionUtils(WorkspaceClientFactory workspaceClientFactory) {
        ParametersChecker.checkParameter("workspaceFactory is a mandatory parameter", workspaceClientFactory);
        this.workspaceClientFactory = workspaceClientFactory;
    }

    /**
     * get the uri list of digital object from a container into the workspace *
     * 
     * @param params - parameters of workspace server
     * @return List<URI>
     * @throws ProcessingException - throw when workspace is unavailable.
     * 
     */
    public List<URI> getDigitalObjectUriListFromWorkspace(WorkParams workParams)
        throws ProcessingException {
        List<URI> uriListWorkspace = null;
        WorkspaceClient workspaceClient =
            workspaceClientFactory.create(workParams.getServerConfiguration().getUrlWorkspace());
        String guidContainer = workParams.getGuuid();
        uriListWorkspace = getDigitalObjectUriListFromWorkspace(workspaceClient, guidContainer);
        return uriListWorkspace;
    }

    /**
     * * get the uri list of digital object from a container into the workspace *
     * 
     * @param workspaceClient
     * @param guidContainer
     * @return List<URI> - list uri
     */
    private List<URI> getDigitalObjectUriListFromWorkspace(WorkspaceClient workspaceClient, String guidContainer)
        throws ProcessingException {
        List<URI> uriListWorkspace =
            workspaceClient.getListUriDigitalObjectFromFolder(guidContainer, digitalObjectFolderName);
        return uriListWorkspace;

    }
}
