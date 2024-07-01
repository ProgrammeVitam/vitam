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
package fr.gouv.vitam.processing.engine.core.operation;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.common.CompressInformation;

import java.util.Collections;

public class OperationContextMonitor {

    public static final String ALL_PARAMS_ARE_REQUIRED = "All params are required";
    public static final String OperationContextFileName = "operation_context.json";

    private final StorageClientFactory storageClientFactory;

    public OperationContextMonitor(StorageClientFactory storageClientFactory) {
        this.storageClientFactory = storageClientFactory;
    }

    public OperationContextMonitor() {
        this(StorageClientFactory.getInstance());
    }

    public static void compressInWorkspace(
        WorkspaceClientFactory workspaceClientFactory,
        String operationContainer,
        LogbookTypeProcess logbookTypeProcess,
        String... files
    ) throws OperationContextException {
        ParametersChecker.checkParameter(ALL_PARAMS_ARE_REQUIRED, operationContainer, logbookTypeProcess, files);

        if (files.length == 0) {
            throw new OperationContextException("files parameter is empty");
        }

        final String outputFile;

        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            // Query DSL + other files
            outputFile = logbookTypeProcess.name() + "_" + operationContainer + ".zip";
            // Zip all files to be stored temporary in the offer
            final CompressInformation compressInformation = new CompressInformation();
            Collections.addAll(compressInformation.getFiles(), files);
            compressInformation.setOutputContainer(operationContainer);
            compressInformation.setOutputFile(outputFile);
            workspaceClient.compress(operationContainer, compressInformation);
        } catch (Exception e) {
            throw new OperationContextException(e);
        }
    }

    /**
     * @param strategy
     * @param operationContainer
     * @param logbookTypeProcess
     * @throws OperationContextException
     */
    public void backup(String strategy, String operationContainer, LogbookTypeProcess logbookTypeProcess)
        throws OperationContextException {
        ParametersChecker.checkParameter("All params are required", strategy, operationContainer, logbookTypeProcess);

        ParametersChecker.checkParameter("StorageClientFactory is required", storageClientFactory);

        try (StorageClient storageClient = storageClientFactory.getClient()) {
            // Query DSL + other files
            String outputFile = logbookTypeProcess.name() + "_" + operationContainer + ".zip";

            // Save the zip file in the offer
            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(operationContainer);
            description.setWorkspaceObjectURI(outputFile);

            storageClient.storeFileFromWorkspace(strategy, DataCategory.TMP, outputFile, description);
            // Should remove the zip file from the workspace
            // The final step of the workflow will remove it
            // Save one call to the workspace ?! or the best is to save space ?!

        } catch (Exception e) {
            throw new OperationContextException(e);
        }
    }

    @VisibleForTesting
    public JsonNode getInformation(String strategy, String operationContainer, LogbookTypeProcess logbookTypeProcess)
        throws OperationContextException, StorageNotFoundException {
        ParametersChecker.checkParameter("All params are required", strategy, operationContainer, logbookTypeProcess);
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            String objectName = logbookTypeProcess.name() + "_" + operationContainer + ".zip";
            return storageClient.getInformation(
                strategy,
                DataCategory.TMP,
                objectName,
                Lists.newArrayList("default"),
                false
            );
        } catch (Exception e) {
            throw new OperationContextException(e);
        }
    }

    public boolean deleteBackup(String strategy, String operationContainer, LogbookTypeProcess logbookTypeProcess)
        throws OperationContextException {
        ParametersChecker.checkParameter("All params are required", strategy, operationContainer, logbookTypeProcess);
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            String objectName = logbookTypeProcess.name() + "_" + operationContainer + ".zip";
            return storageClient.delete(strategy, DataCategory.TMP, objectName);
        } catch (Exception e) {
            throw new OperationContextException(e);
        }
    }
}
