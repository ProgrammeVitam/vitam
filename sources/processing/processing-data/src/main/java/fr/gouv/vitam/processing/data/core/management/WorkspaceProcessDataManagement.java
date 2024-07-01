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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingStorageWorkspaceException;
import fr.gouv.vitam.processing.common.model.DistributorIndex;
import fr.gouv.vitam.processing.common.model.PauseRecover;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Workspace implemenation for workflows datas management
 */
public class WorkspaceProcessDataManagement implements ProcessDataManagement {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkspaceProcessDataManagement.class);

    private static final ProcessDataManagement INSTANCE = new WorkspaceProcessDataManagement();

    private final WorkspaceClientFactory workspaceClientFactory;

    private WorkspaceProcessDataManagement() {
        this.workspaceClientFactory = WorkspaceClientFactory.getInstance();
    }

    @VisibleForTesting
    public WorkspaceProcessDataManagement(WorkspaceClientFactory workspaceClientFactory) {
        this.workspaceClientFactory = workspaceClientFactory;
    }

    /**
     * Get the current instance
     *
     * @return the current instance
     */
    public static ProcessDataManagement getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean createProcessContainer() throws ProcessingStorageWorkspaceException {
        if (isProcessContainerExist()) {
            return false;
        }
        try (WorkspaceClient client = workspaceClientFactory.getClient()) {
            client.createContainer(PROCESS_CONTAINER);
            return true;
        } catch (ContentAddressableStorageServerException exc) {
            LOGGER.error(exc);
            throw new ProcessingStorageWorkspaceException(exc);
        }
    }

    @Override
    public boolean isProcessContainerExist() throws ProcessingStorageWorkspaceException {
        try (WorkspaceClient client = workspaceClientFactory.getClient()) {
            return client.isExistingContainer(PROCESS_CONTAINER);
        } catch (ContentAddressableStorageServerException exc) {
            throw new ProcessingStorageWorkspaceException(exc);
        }
    }

    @Override
    public boolean createFolder(String folderName) throws ProcessingStorageWorkspaceException {
        if (isFolderExist(folderName)) {
            return false;
        }
        try (WorkspaceClient client = workspaceClientFactory.getClient()) {
            client.createFolder(PROCESS_CONTAINER, folderName);
            return true;
        } catch (ContentAddressableStorageServerException exc) {
            throw new ProcessingStorageWorkspaceException(exc);
        } catch (ContentAddressableStorageAlreadyExistException e) {
            LOGGER.warn(e);
            return false;
        }
    }

    @Override
    public boolean isFolderExist(String folderName) throws ProcessingStorageWorkspaceException {
        try (WorkspaceClient client = workspaceClientFactory.getClient()) {
            return client.isExistingFolder(PROCESS_CONTAINER, folderName);
        } catch (ContentAddressableStorageServerException exc) {
            throw new ProcessingStorageWorkspaceException(exc);
        }
    }

    @Override
    public boolean removeFolder(String folderName) throws ProcessingStorageWorkspaceException {
        if (!isFolderExist(folderName)) {
            return false;
        }
        try (WorkspaceClient client = workspaceClientFactory.getClient()) {
            client.deleteFolder(PROCESS_CONTAINER, folderName);
            return true;
        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException exc) {
            throw new ProcessingStorageWorkspaceException(exc);
        }
    }

    @Override
    public void persistProcessWorkflow(String folderName, ProcessWorkflow processWorkflow)
        throws ProcessingStorageWorkspaceException, InvalidParseOperationException {
        LOGGER.debug("[PERSIST] workflow process with execution status : <{}>", processWorkflow.getState());
        try (WorkspaceClient client = workspaceClientFactory.getClient()) {
            client.putObject(
                PROCESS_CONTAINER,
                getPathToObjectFromFolder(folderName, processWorkflow.getOperationId()),
                JsonHandler.writeAsString(processWorkflow).getBytes()
            );
        } catch (ContentAddressableStorageServerException exc) {
            throw new ProcessingStorageWorkspaceException(exc);
        }
    }

    @Override
    public void persistDistributorIndex(String fileName, DistributorIndex distributorIndex)
        throws ProcessingStorageWorkspaceException, InvalidParseOperationException {
        try (WorkspaceClient client = workspaceClientFactory.getClient()) {
            client.putObject(
                PROCESS_CONTAINER,
                getPathToObjectFromFolder(DISTRIBUTOR_INDEX, fileName),
                JsonHandler.writeAsString(distributorIndex).getBytes()
            );
        } catch (ContentAddressableStorageServerException exc) {
            throw new ProcessingStorageWorkspaceException(exc);
        }
    }

    @Override
    public Optional<DistributorIndex> getDistributorIndex(String fileName)
        throws ProcessingStorageWorkspaceException, InvalidParseOperationException {
        Response response = null;
        try (WorkspaceClient client = workspaceClientFactory.getClient()) {
            response = client.getObject(PROCESS_CONTAINER, getPathToObjectFromFolder(DISTRIBUTOR_INDEX, fileName));
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                InputStream is = (InputStream) response.getEntity();
                return Optional.of(JsonHandler.getFromInputStream(is, DistributorIndex.class));
            } else {
                client.consumeAnyEntityAndClose(response);
                throw new ProcessingStorageWorkspaceException(
                    "Workspace error: " + response.getStatusInfo().getReasonPhrase()
                );
            }
        } catch (ContentAddressableStorageServerException exc) {
            throw new ProcessingStorageWorkspaceException(exc);
        } catch (ContentAddressableStorageNotFoundException exc) {
            LOGGER.debug(exc);
            return Optional.empty();
        } finally {
            DefaultClient.staticConsumeAnyEntityAndClose(response);
        }
    }

    @Override
    public ProcessWorkflow getProcessWorkflow(String folderName, String asyncId)
        throws ProcessingStorageWorkspaceException, InvalidParseOperationException {
        Response response = null;
        try (WorkspaceClient client = workspaceClientFactory.getClient()) {
            response = client.getObject(PROCESS_CONTAINER, getPathToObjectFromFolder(folderName, asyncId));
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                InputStream is = (InputStream) response.getEntity();
                return JsonHandler.getFromInputStream(is, ProcessWorkflow.class);
            } else {
                client.consumeAnyEntityAndClose(response);
                throw new ProcessingStorageWorkspaceException(
                    "Workspace error: " + response.getStatusInfo().getReasonPhrase()
                );
            }
        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException exc) {
            throw new ProcessingStorageWorkspaceException(exc);
        } finally {
            DefaultClient.staticConsumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void removeProcessWorkflow(String folderName, String asyncId) throws ProcessingStorageWorkspaceException {
        try (WorkspaceClient client = workspaceClientFactory.getClient()) {
            String path = getPathToObjectFromFolder(folderName, asyncId);
            if (client.isExistingObject(PROCESS_CONTAINER, path)) {
                client.deleteObject(PROCESS_CONTAINER, path);
            }
        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException exc) {
            throw new ProcessingStorageWorkspaceException(exc);
        }
    }

    @Override
    public Map<String, ProcessWorkflow> getProcessWorkflowFor(Integer tenantId, String folderName)
        throws ProcessingStorageWorkspaceException {
        Map<String, ProcessWorkflow> result = new ConcurrentHashMap<>();
        try (WorkspaceClient client = workspaceClientFactory.getClient()) {
            List<URI> uris = JsonHandler.getFromStringAsTypeReference(
                client
                    .getListUriDigitalObjectFromFolder(PROCESS_CONTAINER, folderName)
                    .toJsonNode()
                    .get("$results")
                    .get(0)
                    .toString(),
                new TypeReference<>() {}
            );
            for (URI uri : uris) {
                try {
                    String processId = uri.getPath().substring(0, uri.getPath().lastIndexOf("."));
                    ProcessWorkflow processWorkflow = getProcessWorkflow(folderName, processId);
                    if (ProcessState.RUNNING.equals(processWorkflow.getState())) {
                        processWorkflow.setState(ProcessState.PAUSE);
                        processWorkflow.setStatus(StatusCode.FATAL);
                        processWorkflow.setPauseRecover(PauseRecover.RECOVER_FROM_SERVER_PAUSE);
                    }

                    if (
                        ProcessState.COMPLETED.equals(processWorkflow.getState()) &&
                        null == processWorkflow.getProcessCompletedDate()
                    ) {
                        // The make the processWorkflow cleanable
                        processWorkflow.setProcessCompletedDate(LocalDateUtil.now());
                    }

                    if (tenantId == null || processWorkflow.getTenantId().equals(tenantId)) {
                        result.put(processWorkflow.getOperationId(), processWorkflow);
                    }
                } catch (InvalidParseOperationException e) {
                    LOGGER.error("Error on loading old workflow {} -> cannot be resume", uri.getPath(), e);
                }
            }
        } catch (ContentAddressableStorageServerException | InvalidParseOperationException | InvalidFormatException e) {
            throw new ProcessingStorageWorkspaceException(e);
        }
        return result;
    }

    @Override
    public boolean removeOperationContainer(
        ProcessWorkflow processWorkflow,
        WorkspaceClientFactory workspaceClientFactory
    ) {
        String operationId = processWorkflow.getOperationId();

        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            if (workspaceClient.isExistingContainer(operationId)) {
                workspaceClient.deleteContainer(operationId, true);
            }

            if (
                workspaceClient.isExistingObject(
                    ProcessDataManagement.PROCESS_CONTAINER,
                    DISTRIBUTOR_INDEX + "/" + operationId + ".json"
                )
            ) {
                workspaceClient.deleteObject(
                    ProcessDataManagement.PROCESS_CONTAINER,
                    DISTRIBUTOR_INDEX + "/" + operationId + ".json"
                );
            }
            return true;
        } catch (Exception e) {
            String msg =
                "Error while clear the container " +
                operationId +
                " from the workspace. The background process workflow cleaner should retry to clean the operation container";
            LOGGER.warn(msg, e);
            return false;
        }
    }

    /**
     * Get object name with json extension
     *
     * @param asyncId the async ID
     * @return asyncId.json
     */
    private String getObjectName(String asyncId) {
        return asyncId + ".json";
    }

    /**
     * Get the path to object name (with json extension) from a folder
     *
     * @param folderName the folder
     * @param object the object
     * @return the path to the object from folder like folderName/asyncId.json
     */
    private String getPathToObjectFromFolder(String folderName, String object) {
        return folderName + "/" + getObjectName(object);
    }
}
