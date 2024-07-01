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
package fr.gouv.vitam.processing.data.core.management;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.processing.common.exception.ProcessingStorageWorkspaceException;
import fr.gouv.vitam.processing.common.model.DistributorIndex;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.util.Map;
import java.util.Optional;

/**
 * Interface for process datas management service
 * (persist and restore workflow)
 */
public interface ProcessDataManagement {
    /**
     * Container name
     */
    String PROCESS_CONTAINER = "process";

    String DISTRIBUTOR_INDEX = "distributorIndex";

    /**
     * Create the process container (initialisation) if does not exist
     *
     * @return true if the container was created, false if it already exists
     * @throws ProcessingStorageWorkspaceException when storage error occurs
     */
    boolean createProcessContainer() throws ProcessingStorageWorkspaceException;

    /**
     * Check if the process container exists
     *
     * @return true if the container exists, false otherwise
     * @throws ProcessingStorageWorkspaceException when storage error occurs
     */
    boolean isProcessContainerExist() throws ProcessingStorageWorkspaceException;

    /**
     * Create a folder with the name {folderName} if does not exist on the process container
     *
     * @param folderName the name of the folder to create
     * @return true if the folder was created, false if it already exists
     * @throws ProcessingStorageWorkspaceException when storage error occurs
     */
    boolean createFolder(String folderName) throws ProcessingStorageWorkspaceException;

    /**
     * Check if the folder with name {folderName} exists on process container
     *
     * @param folderName the folder name to check
     * @return true if folder exists on process container, false otherwise
     * @throws ProcessingStorageWorkspaceException when storage error occurs
     */
    boolean isFolderExist(String folderName) throws ProcessingStorageWorkspaceException;

    /**
     * Delete folder with name {folderName} if exists on process container
     *
     * @param folderName the folderName to delete
     * @return true if the folder was removed, false if it does not exist
     * @throws ProcessingStorageWorkspaceException when storage error occurs
     */
    boolean removeFolder(String folderName) throws ProcessingStorageWorkspaceException;

    /**
     * Put process workflow instance in workspace
     *
     * @param folderName the folder to put workflow instance on process container
     * @param processWorkflow the instance to save
     * @throws ProcessingStorageWorkspaceException when storage error occurs
     * @throws InvalidParseOperationException when serializing object to json fail
     */
    void persistProcessWorkflow(String folderName, ProcessWorkflow processWorkflow)
        throws ProcessingStorageWorkspaceException, InvalidParseOperationException;

    void persistDistributorIndex(String fileName, DistributorIndex distributorIndex)
        throws ProcessingStorageWorkspaceException, InvalidParseOperationException;

    Optional<DistributorIndex> getDistributorIndex(String fileName)
        throws ProcessingStorageWorkspaceException, InvalidParseOperationException;

    /**
     * Retrieve a workflow instance on process container
     *
     * @param folderName the folder on process container to get workflow instance
     * @param asyncId the request id (asynchronous id)
     * @return the workflow instance
     * @throws ProcessingStorageWorkspaceException when storage error occurs
     * @throws InvalidParseOperationException when deserializing object to json fail
     */
    ProcessWorkflow getProcessWorkflow(String folderName, String asyncId)
        throws ProcessingStorageWorkspaceException, InvalidParseOperationException;

    /**
     * Delete process workflow from the workspace
     *
     * @param folderName the folder on process container to remove workflow instance
     * @param asyncId the request id (asynchronous id)
     * @throws ProcessingStorageWorkspaceException when storage error occurs
     */
    void removeProcessWorkflow(String folderName, String asyncId) throws ProcessingStorageWorkspaceException;

    /**
     * Get process workflow map for tenantId and folderName (server id from serverIdentity)
     *
     * @param tenantId the tenant ID
     * @param folderName the folder name (server id from serverIdentity)
     * @return map of tenantID process for a server id
     * @throws ProcessingStorageWorkspaceException thrown if an error ocurred when loading process file
     */
    Map<String, ProcessWorkflow> getProcessWorkflowFor(Integer tenantId, String folderName)
        throws ProcessingStorageWorkspaceException;

    boolean removeOperationContainer(ProcessWorkflow processWorkflow, WorkspaceClientFactory workspaceClientFactory);
}
