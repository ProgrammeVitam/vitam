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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.index.model.ReindexationResult;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.VitamTestHelper.insertWaitForStepEssentialFiles;
import static fr.gouv.vitam.common.VitamTestHelper.waitOperation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ReindexSwitchIT extends VitamRuleRunner {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReindexSwitchIT.class);

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        ReindexSwitchIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            ProcessManagementMain.class
        )
    );

    private static final Integer TENANT_ID = 0;

    private static final String SIP_FOLDER = "SIP";

    private static String CONFIG_SIEGFRIED_PATH;

    private static final String SIP_OK = "integration-processing/OK_TEST_REPLAY_1.zip";
    private WorkspaceClient workspaceClient;
    private ProcessingManagementClient processingClient;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass("", Arrays.asList(0, 1), Collections.emptyMap());
        CONFIG_SIEGFRIED_PATH = PropertiesUtils.getResourcePath(
            "integration-processing/format-identifiers.conf"
        ).toString();

        FormatIdentifierFactory.getInstance().changeConfigurationFile(CONFIG_SIEGFRIED_PATH);

        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
        new DataLoader("integration-processing").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass();
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);

        runAfter();

        try (WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
            workspaceClient.deleteContainer("process", true);
        } catch (ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e);
        }
        VitamClientFactory.resetConnections();
    }

    private void createLogbookOperation(GUID operationId, GUID objectId)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        final LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();

        final LogbookOperationParameters initParameters = LogbookParameterHelper.newLogbookOperationParameters(
            operationId,
            "Process_SIP_unitary",
            objectId,
            LogbookTypeProcess.INGEST,
            StatusCode.STARTED,
            operationId != null ? operationId.toString() : "outcomeDetailMessage",
            operationId
        );
        logbookClient.create(initParameters);
    }

    @RunWithCustomExecutor
    @Test
    public void testReindexAndSwitch() throws Exception {
        try (
            AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient()
        ) {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

            launchReindexationAndSwitchAndCheckValues("ACCESS_CONTRACT", "accesscontract", client, "");
            launchReindexationAndSwitchAndCheckValues("FORMATS", "fileformat", client, "");
            launchReindexationAndSwitchAndCheckValues("INGEST_CONTRACT", "ingestcontract", client, "");

            ProcessingIT.prepareVitamSession();
            String containerName = launchIngest();

            JsonNode logbookResultBefore = logbookClient.selectOperationById(containerName);
            SelectMultiQuery selectMulti = new SelectMultiQuery();
            selectMulti.addQueries(QueryHelper.in("#operations", containerName));
            JsonNode nodeUnit = metadataClient.selectUnits(selectMulti.getFinalSelect());
            ArrayNode resultUnit = (ArrayNode) nodeUnit.get("$results");
            JsonNode nodeObject = metadataClient.selectObjectGroups(selectMulti.getFinalSelect());
            ArrayNode resultObject = (ArrayNode) nodeObject.get("$results");

            int sizeUnitsBefore = resultUnit.size();
            int sizeOgBefore = resultObject.size();

            launchReindexationAndSwitchAndCheckValues("Operation", "logbookoperation_0", client, "0");
            launchReindexationAndSwitchAndCheckValues("Unit", "unit_0", client, "0");
            launchReindexationAndSwitchAndCheckValues("ObjectGroup", "objectgroup_0", client, "0");

            JsonNode logbookResultAfter = logbookClient.selectOperationById(containerName);

            validateLogbookOperations(
                logbookResultBefore.get("$results").get(0),
                logbookResultAfter.get("$results").get(0)
            );

            nodeUnit = metadataClient.selectUnits(selectMulti.getFinalSelect());
            resultUnit = (ArrayNode) nodeUnit.get("$results");
            nodeObject = metadataClient.selectObjectGroups(selectMulti.getFinalSelect());
            resultObject = (ArrayNode) nodeObject.get("$results");
            int sizeUnitsAfter = resultUnit.size();
            int sizeOgAfter = resultObject.size();
            assertEquals(sizeUnitsBefore, sizeUnitsAfter);
            assertEquals(sizeOgBefore, sizeOgAfter);
        }
    }

    private void launchReindexationAndSwitchAndCheckValues(
        String collection,
        String alias,
        AdminManagementClient client,
        String tenants
    ) throws Exception {
        String order = "[{\"collection\" : \"" + collection + "\", \"tenants\" : [" + tenants + "]}]";
        Select select = new Select();
        JsonNode queryDsl = select.getFinalSelect();
        int sizeBefore = countCollection(collection, client, queryDsl);

        RequestResponse<ReindexationResult> result = client.launchReindexation(JsonHandler.getFromString(order));
        assertTrue(result.isOk());
        List<ReindexationResult> idxResults = ((RequestResponseOK<ReindexationResult>) result).getResults();
        String newIndexName = idxResults.get(0).getIndexOK().get(0).getIndexName();

        String switchOrder =
            "[{\"collection\" : \"" +
            collection +
            "\", \"alias\" : \"" +
            alias +
            "\", \"indexName\" : \"" +
            newIndexName +
            "\"}]";
        RequestResponse<ReindexationResult> resultSwitch = client.switchIndexes(JsonHandler.getFromString(switchOrder));
        assertTrue(resultSwitch.isOk());

        int sizeAfter = countCollection(collection, client, queryDsl);

        assertEquals(sizeBefore, sizeAfter);
    }

    private int countCollection(String collection, AdminManagementClient client, JsonNode queryDsl) throws Exception {
        int size = 0;
        if ("ACCESS_CONTRACT".equals(collection)) {
            size = ((RequestResponseOK<AccessContractModel>) client.findAccessContracts(queryDsl)).getResults().size();
        } else if ("FORMATS".equals(collection)) {
            size = ((RequestResponseOK<FileFormatModel>) client.getFormats(queryDsl)).getResults().size();
        } else if ("INGEST_CONTRACT".equals(collection)) {
            size = ((RequestResponseOK<IngestContractModel>) client.findIngestContracts(queryDsl)).getResults().size();
        }
        return size;
    }

    private void validateLogbookOperations(JsonNode logbookResultReplay, JsonNode logbookResultNoReplay)
        throws Exception {
        JsonNode evDetDataReplay = JsonHandler.getFromString(logbookResultReplay.get("evDetData").asText());
        JsonNode evDetDataNotReplay = JsonHandler.getFromString(logbookResultNoReplay.get("evDetData").asText());
        assertEquals(evDetDataReplay.get("EvDetailReq").asText(), evDetDataNotReplay.get("EvDetailReq").asText());
    }

    private String launchIngest() throws Exception {
        final GUID operationGuid = GUIDFactory.newOperationLogbookGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        final GUID objectGuid = GUIDFactory.newManifestGUID(TENANT_ID);
        final String containerName = objectGuid.getId();
        createLogbookOperation(operationGuid, objectGuid);

        // workspace client dezip SIP in workspace
        InputStream zipInputStreamSipObject = null;

        zipInputStreamSipObject = PropertiesUtils.getResourceAsStream(SIP_OK);

        //
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        workspaceClient.createContainer(containerName);
        workspaceClient.uncompressObject(containerName, SIP_FOLDER, CommonMediaType.ZIP, zipInputStreamSipObject);
        // Insert sanityCheck file & StpUpload
        insertWaitForStepEssentialFiles(containerName);

        processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        processingClient.initVitamProcess(containerName, Contexts.DEFAULT_WORKFLOW.name());
        final RequestResponse<ItemStatus> ret = processingClient.executeOperationProcess(
            containerName,
            Contexts.DEFAULT_WORKFLOW.name(),
            ProcessAction.RESUME.getValue()
        );

        assertNotNull(ret);
        assertThat(ret.isOk()).isTrue();
        assertEquals(Status.ACCEPTED.getStatusCode(), ret.getStatus());

        waitOperation(containerName);
        ProcessWorkflow processWorkflow = ProcessMonitoringImpl.getInstance()
            .findOneProcessWorkflow(containerName, TENANT_ID);
        assertNotNull(processWorkflow);
        assertEquals(ProcessState.COMPLETED, processWorkflow.getState());
        assertEquals(StatusCode.OK, processWorkflow.getStatus());

        return containerName;
    }
}
