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
package fr.gouv.vitam.worker.common.utils;

import java.net.URI;
import java.util.List;

import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * Class Utils to get Objects from a container into the workspace
 */
public class ContainerExtractionUtils {
    // FIXME REVIEW Use something to clarify globally the Ingest folder organization in one place for all, not in every
    // class!
    // Each class then can add extra organization for its own
    // TODO
    // Retrieve the hard code value for the path of the folder of digital objects
    private static final String DIGITAL_OBJECT_FOLDER_NAME = "SIP";

    // FIXME REVIEW Since build through Factory: use package protected class and constructors for ALL
    /**
     * Constructor that instantiates a workspace client factory
     *
     */
    public ContainerExtractionUtils() {
        // empty constructor
    }

    /**
     * get the uri list of digital object from a container into the workspace *
     *
     * @param workParams parameters of workspace server
     * @return List of Uri
     * @throws ProcessingException - throw when workspace is unavailable.
     *
     */
    public List<URI> getDigitalObjectUriListFromWorkspace(WorkerParameters workParams)
        throws ProcessingException {
        try (final WorkspaceClient workspaceClient =
            WorkspaceClientFactory.create(workParams.getUrlWorkspace())) {
            final String guidContainer = workParams.getContainerName();
            return getDigitalObjectUriListFromWorkspace(workspaceClient, guidContainer);
        }
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
        final List<URI> uriListWorkspace =
            workspaceClient.getListUriDigitalObjectFromFolder(guidContainer, DIGITAL_OBJECT_FOLDER_NAME);
        uriListWorkspace.remove(uriListWorkspace.size() - 1);
        return uriListWorkspace;

    }
}
