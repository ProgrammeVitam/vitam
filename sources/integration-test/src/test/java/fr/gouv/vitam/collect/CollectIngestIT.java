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
package fr.gouv.vitam.collect;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.culture.archivesdefrance.seda.v2.LegalStatusType;
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.client.VitamPoolingClient;
import fr.gouv.vitam.access.external.rest.AccessExternalMain;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.external.client.CollectExternalClient;
import fr.gouv.vitam.collect.external.client.CollectExternalClientFactory;
import fr.gouv.vitam.collect.external.external.rest.CollectExternalMain;
import fr.gouv.vitam.collect.internal.CollectInternalMain;
import fr.gouv.vitam.collect.internal.client.CollectInternalClient;
import fr.gouv.vitam.collect.internal.client.CollectInternalClientFactory;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.ingest.external.rest.IngestExternalMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollectionsTestUtils;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.collect.CollectTestHelper.initProjectData;
import static fr.gouv.vitam.collect.CollectTestHelper.initTransaction;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CollectIngestIT extends VitamRuleRunner {

    private static final Integer TENANT_ID = 0;

    private static final String ACCESS_CONTRACT = "aName3";

    private static String prefix;



    @ClassRule public static VitamServerRunner runner =
        new VitamServerRunner(CollectIngestIT.class, mongoRule.getMongoDatabase().getName(),
            ElasticsearchRule.getClusterName(),
            Sets.newHashSet(WorkerMain.class, AdminManagementMain.class, LogbookMain.class, WorkspaceMain.class,
                ProcessManagementMain.class, StorageMain.class, DefaultOfferMain.class, AccessInternalMain.class,
                IngestInternalMain.class, AccessExternalMain.class, IngestExternalMain.class, CollectInternalMain.class,
                CollectExternalMain.class));

    @Rule public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private final VitamContext vitamContext = new VitamContext(TENANT_ID);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        prefix = MetadataCollections.UNIT.getPrefix();
        runner.startMetadataCollectServer();
        runner.startWorkspaceCollectServer();
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        MetadataCollections.UNIT.setPrefix(prefix);
        runner.stopMetadataCollectServer(false);
        runner.stopWorkspaceCollectServer();
        runner.stopMetadataServer(true);
        handleAfterClass();
        runAfter();
        fr.gouv.vitam.common.client.VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    @Test
    @RunWithCustomExecutor
    public void should_ingest_sip_from_transaction() throws Exception {
        String idTransaction;
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            ProjectDto projectDto = initProjectData();

            final RequestResponse<JsonNode> projectResponse = collectClient.initProject(vitamContext, projectDto);
            Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);

            ProjectDto projectDtoResult =
                JsonHandler.getFromJsonNode(((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                    ProjectDto.class);

            TransactionDto transactiondto = initTransaction();

            RequestResponse<JsonNode> transactionResponse =
                collectClient.initTransaction(vitamContext, transactiondto, projectDtoResult.getId());
            Assertions.assertThat(transactionResponse.getStatus()).isEqualTo(200);

            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) transactionResponse;
            TransactionDto transactionDtoResult =
                JsonHandler.getFromJsonNode(requestResponseOK.getFirstResult(), TransactionDto.class);
            idTransaction = transactionDtoResult.getId();
            try (InputStream inputStream = PropertiesUtils.getResourceAsStream("collect/arbo_to_ingest.zip")) {
                RequestResponse<JsonNode> response =
                    collectClient.uploadProjectZip(vitamContext, transactionDtoResult.getId(), inputStream);
                Assertions.assertThat(response.getStatus()).isEqualTo(200);
            }

            final RequestResponseOK<JsonNode> unitsByTransaction =
                (RequestResponseOK<JsonNode>) collectClient.getUnitsByTransaction(vitamContext,
                    transactionDtoResult.getId(), new SelectMultiQuery().getFinalSelect());

            assertEquals(6, unitsByTransaction.getResults().size());
            collectClient.closeTransaction(vitamContext, transactionDtoResult.getId());
        }
        InputStream inputStream;
        try (CollectInternalClient client = CollectInternalClientFactory.getInstance().getClient()) {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
            inputStream = client.generateSip(idTransaction);
            RequestResponse<JsonNode> transactionResponse = client.getTransactionById(idTransaction);
            Assertions.assertThat(transactionResponse.getStatus()).isEqualTo(200);

            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) transactionResponse;
            TransactionDto transactionDtoResult =
                JsonHandler.getFromJsonNode(requestResponseOK.getFirstResult(), TransactionDto.class);
            Assertions.assertThat(transactionDtoResult.getStatus()).isEqualTo(TransactionStatus.SENDING.toString());

        }

        // turn off metadata-collect and run metadata
        runner.stopMetadataCollectServer(true);
        runAfterMongo(Set.of(MetadataCollections.UNIT.getName(), MetadataCollections.OBJECTGROUP.getName()));
        runAfterEs(ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), TENANT_ID),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), TENANT_ID));
        MetadataCollectionsTestUtils.afterTestClass(metadataIndexManager, false);

        runner.startMetadataServer();

        String processId;

        try (IngestExternalClient ingestExternalClient = IngestExternalClientFactory.getInstance().getClient();
            AdminExternalClient adminExternalClient = AdminExternalClientFactory.getInstance().getClient()) {
            RequestResponse<Void> ingest =
                ingestExternalClient.ingest(vitamContext, inputStream, DEFAULT_WORKFLOW.name(),
                    ProcessAction.RESUME.name());
            processId = ingest.getVitamHeaders().get(GlobalDataRest.X_REQUEST_ID);

            final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(adminExternalClient);
            boolean process_timeout =
                vitamPoolingClient.wait(TENANT_ID, processId, ProcessState.COMPLETED, 1800, 1_000L,
                    TimeUnit.MILLISECONDS);
            if (!process_timeout) {
                Assertions.fail("Sip processing not finished : operation (" + processId + "). Timeout exceeded.");
            }

            RequestResponse<ItemStatus> operationResponse =
                adminExternalClient.getOperationProcessExecutionDetails(new VitamContext(TENANT_ID), processId);
            assertTrue(operationResponse.isOk());
            VitamTestHelper.verifyOperation(processId, StatusCode.OK);
        }

        String opi;
        try (AccessExternalClient accessExternalClient = AccessExternalClientFactory.getInstance().getClient()) {
            SelectMultiQuery select = new SelectMultiQuery();
            select.addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), processId));
            vitamContext.setAccessContract(ACCESS_CONTRACT);
            RequestResponse<JsonNode> results = accessExternalClient.selectUnits(vitamContext, select.getFinalSelect());

            assertEquals(6, ((RequestResponseOK<JsonNode>) results).getResults().size());


            opi = ((RequestResponseOK<JsonNode>) results).getResults().get(0).get(VitamFieldsHelper.initialOperation())
                .toString();
        }

        try (AdminExternalClient adminExternalClient = AdminExternalClientFactory.getInstance().getClient()) {

            final String query =
                "{\n" + "  \"$query\":\n" + "    {\n" + "      \"$eq\": {\n" + "        \"Opi\": " + opi + "\n" +
                    "      }\n" + "    },\n" + "  \"$projection\": {},\n" + "  \"$filter\":{}\n" + "}";

            var acRegDetResponseAfterUpdate =
                adminExternalClient.findAccessionRegisterDetails(vitamContext, JsonHandler.getFromString(query));

            AccessionRegisterDetailModel accessionRegisterDetail =
                ((RequestResponseOK<AccessionRegisterDetailModel>) acRegDetResponseAfterUpdate).getResults().get(0);

            Assertions.assertThat(accessionRegisterDetail.getLegalStatus())
                .isEqualTo(LegalStatusType.PRIVATE_ARCHIVE.value());
            Assertions.assertThat(accessionRegisterDetail.getComment().get(0))
                .isEqualTo("Versement du service producteur : Cabinet de Michel Mercier");
            Assertions.assertThat(accessionRegisterDetail.getAcquisitionInformation())
                .isEqualTo("AcquisitionInformation");

        }


        //// Cette requette permet de récupérer des unités par transaction avec l'opi de vitam core
        // du coup le nombre des résultats obtenus doit être zéro.

        runner.startMetadataCollectServer();

        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {

            final RequestResponseOK<JsonNode> unitsByTransaction =
                (RequestResponseOK<JsonNode>) collectClient.getUnitsByTransaction(vitamContext,
                    processId, new SelectMultiQuery().getFinalSelect());

            assertEquals(0, unitsByTransaction.getResults().size());

        }



    }



}
