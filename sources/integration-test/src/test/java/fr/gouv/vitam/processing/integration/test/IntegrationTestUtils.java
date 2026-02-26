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
package fr.gouv.vitam.processing.integration.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.OriginatingAgencyReassignmentRequest;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.WorkFlowExecutionContext;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import jakarta.ws.rs.core.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.common.TestZipUtils.zipFolder;
import static fr.gouv.vitam.common.VitamTestHelper.insertWaitForStepEssentialFiles;
import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static fr.gouv.vitam.common.json.JsonHandler.writeToInpustream;
import static fr.gouv.vitam.common.model.ProcessAction.RESUME;
import static fr.gouv.vitam.common.model.ProcessState.COMPLETED;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.ORIGINATING_AGENCY_REASSIGNMENT;
import static fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess.INGEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IntegrationTestUtils {

    public static ProcessWorkflow ingest(
        ProcessingManagementClient processingClient,
        Integer tenantId,
        String sipFilePath,
        Contexts contexts,
        StatusCode expectedStatus,
        String sipFolder
    ) throws Exception {
        String operationId = createOperationContainer(tenantId);
        // use link sip
        final InputStream zipStream = new FileInputStream(new File(sipFilePath));
        WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance(
            WorkFlowExecutionContext.VITAM
        ).getClient();
        workspaceClient.createContainer(operationId);
        workspaceClient.uncompressObject(operationId, sipFolder, CommonMediaType.ZIP, zipStream);
        // Insert sanityCheck file & StpUpload
        insertWaitForStepEssentialFiles(operationId);

        processingClient.initVitamProcess(operationId, contexts.name());
        final RequestResponse<ItemStatus> resp = processingClient.executeOperationProcess(
            operationId,
            contexts.name(),
            ProcessAction.RESUME.getValue()
        );
        assertNotNull(resp);
        assertThat(resp.isOk()).isTrue();
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), resp.getStatus());
        waitOperation(operationId);
        ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
            .findOneProcessWorkflow(operationId, tenantId);
        assertThat(processWorkflow).isNotNull();
        assertEquals(COMPLETED, processWorkflow.getState());
        assertEquals(expectedStatus, processWorkflow.getStatus());

        return processWorkflow;
    }

    public static String createOperationContainer(Integer tenantId)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(tenantId);
        createLogbookOperation(operationGuid, objectGuid);

        return objectGuid.getId();
    }

    public static void createLogbookOperation(GUID operationId, GUID objectId)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        createLogbookOperation(operationId, objectId, null, INGEST);
    }

    public static List<String> getUnitOriginatingAgencies(JsonNode unit) {
        return StreamSupport.stream(unit.get(VitamFieldsHelper.originatingAgencies()).spliterator(), false)
            .map(JsonNode::asText)
            .toList();
    }

    public static List<String> getUnitOperations(JsonNode unit) {
        return StreamSupport.stream(unit.get(VitamFieldsHelper.operations()).spliterator(), false)
            .map(JsonNode::asText)
            .toList();
    }

    public static void createLogbookOperation(
        GUID operationId,
        GUID objectId,
        String type,
        LogbookTypeProcess typeProc
    ) throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        if (type == null) {
            type = "Process_SIP_unitary";
        }

        final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
            operationId,
            type,
            objectId,
            typeProc,
            StatusCode.STARTED,
            operationId != null ? operationId.toString() : "outcomeDetailMessage",
            operationId
        );
        if ("EXPORT_DIP".equals(type)) {
            initParameters.putParameterValue(
                LogbookParameterName.outcomeDetailMessage,
                VitamLogbookMessages.getLabelOp("EXPORT_DIP.STARTED") + " : " + operationId
            );
        }
        ObjectNode rightsStatementIdentifier = JsonHandler.createObjectNode();
        rightsStatementIdentifier.put("AccessContract", VitamThreadUtils.getVitamSession().getContractId());
        initParameters.putParameterValue(
            LogbookParameterName.rightsStatementIdentifier,
            rightsStatementIdentifier.toString()
        );
        logbookClient.create(initParameters);
    }

    public static String ingestSIP(
        Integer tenantId,
        ProcessingManagementClient processingClient,
        WorkspaceClient workspaceClient,
        ProcessMonitoringImpl processMonitoring,
        String sipFileName,
        String workflowName,
        StatusCode expectedStatus,
        String sipFolder
    )
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException, FileNotFoundException, ContentAddressableStorageException, BadRequestException, InternalServerException, VitamClientException {
        final String ingestContainerName = IntegrationTestUtils.createOperationContainer(tenantId);
        final InputStream zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(sipFileName);
        workspaceClient = WorkspaceClientFactory.getInstance(WorkFlowExecutionContext.VITAM).getClient();
        workspaceClient.createContainer(ingestContainerName);
        workspaceClient.uncompressObject(ingestContainerName, sipFolder, CommonMediaType.ZIP, zipInputStreamSipObject);
        // Insert sanityCheck file & StpUpload
        insertWaitForStepEssentialFiles(ingestContainerName);

        // call processing
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(ingestContainerName, workflowName);
        RequestResponse<ItemStatus> ret2 = processingClient.executeOperationProcess(
            ingestContainerName,
            workflowName,
            RESUME.getValue()
        );
        assertNotNull(ret2);
        assertTrue(ret2.isOk());
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret2.getStatus());
        waitOperation(ingestContainerName);
        ProcessWorkflow processWorkflow2 = processMonitoring.findOneProcessWorkflow(ingestContainerName, tenantId);
        assertNotNull(processWorkflow2);
        assertEquals(COMPLETED, processWorkflow2.getState());
        assertEquals(expectedStatus, processWorkflow2.getStatus());
        return ingestContainerName;
    }

    public static void replaceStringInFile(String targetFilename, String textToReplace, String replacementText)
        throws IOException {
        Path path = PropertiesUtils.getResourcePath(targetFilename);
        Charset charset = StandardCharsets.UTF_8;

        String content = Files.readString(path, charset);
        content = content.replaceAll(textToReplace, replacementText);
        Files.write(path, content.getBytes(charset));
    }

    /**
     * This is a duplicate test for attaching AU to an existing GOT But we want this to test Attach AU by query to an
     * existing one As the query by #object return the wanted number of AU in results We first attach AU to an existing
     * GOT Then in the test of attach to existing AU by query (the query by #object return more than one= > KO)
     * <p>
     * Why after simulateAttachUnitToExistingGOT the returned GOT have two AU
     *
     * @return The id GOT that should have two AU
     * @throws Exception
     */
    public static void simulateAttachUnitToExistingGOT(
        Integer tenantId,
        ProcessMonitoringImpl processMonitoring,
        WorkspaceClient workspaceClient,
        ProcessingManagementClient processingClient,
        String linkAuToExistingGotName,
        String linkAuToExistingGotNameTarget,
        String idGot,
        String zipName,
        String sipFolder
    ) throws Exception {
        replaceStringInFile(
            linkAuToExistingGotName + "/manifest.xml",
            "(?<=<DataObjectGroupExistingReferenceId>).*?(?=</DataObjectGroupExistingReferenceId>)",
            idGot
        );

        String zipPath =
            PropertiesUtils.getResourcePath(linkAuToExistingGotNameTarget).toAbsolutePath() + "/" + zipName;
        zipFolder(PropertiesUtils.getResourcePath(linkAuToExistingGotName), zipPath);

        final String containerName2 = IntegrationTestUtils.createOperationContainer(tenantId);

        // workspace client dezip SIP in workspace
        // use link sip
        final InputStream zipStream = new FileInputStream(
            new File(PropertiesUtils.getResourcePath(linkAuToExistingGotNameTarget).toAbsolutePath() + "/" + zipName)
        );

        workspaceClient.createContainer(containerName2);
        workspaceClient.uncompressObject(containerName2, sipFolder, CommonMediaType.ZIP, zipStream);
        // Insert sanityCheck file & StpUpload
        insertWaitForStepEssentialFiles(containerName2);

        // call processing
        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName2, DEFAULT_WORKFLOW.name());
        final RequestResponse<ItemStatus> ret2 = processingClient.executeOperationProcess(
            containerName2,
            DEFAULT_WORKFLOW.name(),
            RESUME.getValue()
        );

        assertNotNull(ret2);
        assertThat(ret2.isOk()).isTrue();
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), ret2.getStatus());

        waitOperation(containerName2);
        ProcessWorkflow processWorkflow2 = processMonitoring.findOneProcessWorkflow(containerName2, tenantId);
        assertNotNull(processWorkflow2);
        assertEquals(COMPLETED, processWorkflow2.getState());
        assertEquals(WARNING, processWorkflow2.getStatus());
        assertNotNull(processWorkflow2.getSteps());
    }

    public static String getUnitId(JsonNode unit) {
        return unit.get(VitamFieldsHelper.id()).asText();
    }

    public static List<String> getUnitParents(JsonNode unit) {
        return StreamSupport.stream(unit.get(VitamFieldsHelper.unitups()).spliterator(), false)
            .map(JsonNode::asText)
            .collect(Collectors.toList());
    }

    public static String launchOriginatingAgencyReassignmentOperation(
        String sourceOriginatingAgency,
        String newOriginatingAgency,
        boolean propagateToObjectGroups,
        SelectMultiQuery selectQuery
    )
        throws ContentAddressableStorageServerException, InvalidParseOperationException, InternalServerException, BadRequestException, VitamClientException, LogbookClientAlreadyExistsException, LogbookClientBadRequestException, LogbookClientServerException {
        GUID operationId;
        try (
            WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance(
                WorkFlowExecutionContext.VITAM
            ).getClient();
            ProcessingManagementClient processingClient = ProcessingManagementClientFactory.getInstance().getClient();
            LogbookOperationsClient logbookOperationsClient = LogbookOperationsClientFactory.getInstance().getClient();
        ) {
            int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            operationId = GUIDFactory.newRequestIdGUID(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(operationId);

            final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
                operationId,
                Contexts.ORIGINATING_AGENCY_REASSIGNMENT.getEventType(),
                operationId,
                LogbookTypeProcess.ORIGINATING_AGENCY_REASSIGNMENT,
                StatusCode.STARTED,
                operationId.toString(),
                operationId
            );
            ObjectNode rightsStatementIdentifier = JsonHandler.createObjectNode();
            rightsStatementIdentifier.put("AccessContract", VitamThreadUtils.getVitamSession().getContractId());
            initParameters.putParameterValue(
                LogbookParameterName.rightsStatementIdentifier,
                rightsStatementIdentifier.toString()
            );
            logbookOperationsClient.create(initParameters);

            workspaceClient.createContainer(operationId.toString());
            JsonNode queryNode = JsonHandler.getFromInputStream(writeToInpustream(selectQuery.getFinalSelect()));

            OriginatingAgencyReassignmentRequest originatingAgencyReassignmentRequest =
                new OriginatingAgencyReassignmentRequest(
                    queryNode,
                    sourceOriginatingAgency,
                    newOriginatingAgency,
                    propagateToObjectGroups
                );

            workspaceClient.putObject(
                operationId.toString(),
                "request.json",
                JsonHandler.writeToInpustream(originatingAgencyReassignmentRequest)
            );

            workspaceClient.putObject(
                operationId.toString(),
                "query.json",
                writeToInpustream(selectQuery.getFinalSelect())
            );

            processingClient.initVitamProcess(
                new ProcessingEntry(operationId.toString(), ORIGINATING_AGENCY_REASSIGNMENT.name())
            );
            RequestResponse<ItemStatus> cirResponse = processingClient.executeOperationProcess(
                operationId.toString(),
                ORIGINATING_AGENCY_REASSIGNMENT.name(),
                RESUME.getValue()
            );
            assertNotNull(cirResponse);
            assertTrue(cirResponse.isOk());
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), cirResponse.getStatus());
            waitOperation(operationId.toString());
            ProcessWorkflow cirWorkflow = ProcessMonitoringImpl.getInstance()
                .findOneProcessWorkflow(operationId.toString(), tenantId);
            assertNotNull(cirWorkflow);
            assertEquals(COMPLETED, cirWorkflow.getState());
            assertThat(cirWorkflow.getStatus()).isIn(StatusCode.OK, StatusCode.WARNING);
        }
        return operationId.toString();
    }
}
