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
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.external.client.CollectExternalClient;
import fr.gouv.vitam.collect.external.client.CollectExternalClientFactory;
import fr.gouv.vitam.collect.external.external.rest.CollectExternalMain;
import fr.gouv.vitam.collect.internal.CollectInternalMain;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import static fr.gouv.vitam.collect.CollectTestHelper.initProjectData;
import static fr.gouv.vitam.collect.CollectTestHelper.initTransaction;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TransactionIT extends VitamRuleRunner {

    private static final Integer TENANT_ID = 0;

    private static final String AU_TO_UPLOAD = "collect/upload_au_collect.json";
    private final VitamContext vitamContext = new VitamContext(TENANT_ID);

    @ClassRule public static VitamServerRunner runner =
        new VitamServerRunner(TransactionIT.class, mongoRule.getMongoDatabase().getName(),
            ElasticsearchRule.getClusterName(),
            Sets.newHashSet(AdminManagementMain.class, LogbookMain.class, WorkspaceMain.class,
                CollectInternalMain.class, CollectExternalMain.class));

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        runner.startMetadataCollectServer();
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        runner.stopMetadataCollectServer(true);
        runner.stopMetadataServer(true);
        fr.gouv.vitam.common.client.VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }

    @Test
    public void reopenAndAbortTransaction() throws Exception {
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


            String transactionId = transactionDtoResult.getId();
            collectClient.closeTransaction(vitamContext, transactionId);
            verifyTransactionStatus(TransactionStatus.READY, transactionId);

            //test reopen
            collectClient.reopenTransaction(vitamContext, transactionId);
            verifyTransactionStatus(TransactionStatus.OPEN, transactionId);


            //test abort
            collectClient.abortTransaction(vitamContext, transactionId);
            verifyTransactionStatus(TransactionStatus.ABORTED, transactionId);
        }
    }

    public void verifyTransactionStatus(TransactionStatus status, String transactionId) throws VitamClientException {
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            RequestResponse<JsonNode> response = client.getTransactionById(vitamContext, transactionId);
            assertThat(response.isOk()).isTrue();
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) response;
            assertNotNull(requestResponseOK.getFirstResult());
            assertThat(requestResponseOK.getFirstResult().get("#id").textValue()).isEqualTo(transactionId);
            assertThat(requestResponseOK.getFirstResult().get("Status").textValue()).isEqualTo(status.name());
        }
    }

    @Test
    public void updateTransaction() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            ProjectDto projectDto = initProjectData();
            String newComment = "New Comment";
            final RequestResponse<JsonNode> projectResponse = collectClient.initProject(vitamContext, projectDto);
            Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);

            ProjectDto projectDtoResult =
                JsonHandler.getFromJsonNode(((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                    ProjectDto.class);
            TransactionDto initialTransaction = initTransaction();

            // INSERT TRANSACTION
            RequestResponse<JsonNode> transactionResponse =
                collectClient.initTransaction(vitamContext, initialTransaction, projectDtoResult.getId());
            Assertions.assertThat(transactionResponse.getStatus()).isEqualTo(SC_OK);
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) transactionResponse;
            TransactionDto transactionDtoResult =
                JsonHandler.getFromJsonNode(requestResponseOK.getFirstResult(), TransactionDto.class);

            // GET PERSISTED TRANSACTION
            RequestResponse<JsonNode> persistedTransactionResponse =
                collectClient.getTransactionById(vitamContext, transactionDtoResult.getId());
            Assertions.assertThat(persistedTransactionResponse.getStatus()).isEqualTo(SC_OK);
            TransactionDto persistedTransaction = JsonHandler.getFromJsonNode(
                (((RequestResponseOK<JsonNode>) persistedTransactionResponse).getFirstResult()), TransactionDto.class);
            assertNotNull(persistedTransaction.getCreationDate());
            assertEquals(persistedTransaction.getCreationDate(), persistedTransaction.getLastUpdate());
            assertEquals(TransactionStatus.OPEN.toString(), persistedTransaction.getStatus());
            assertEquals(initialTransaction.getName(), persistedTransaction.getName());
            assertEquals(initialTransaction.getArchivalProfile(), persistedTransaction.getArchivalProfile());
            assertEquals(initialTransaction.getAcquisitionInformation(),
                persistedTransaction.getAcquisitionInformation());
            assertEquals(initialTransaction.getMessageIdentifier(), persistedTransaction.getMessageIdentifier());

            assertThat(persistedTransaction.getComment()).isNotEqualTo(newComment);

            // Update Transaction
            transactionDtoResult.setComment(newComment);
            RequestResponseOK<JsonNode> updatedTransactionResponse =
                (RequestResponseOK<JsonNode>) collectClient.updateTransaction(vitamContext, transactionDtoResult);
            assertThat(updatedTransactionResponse.getStatus()).isEqualTo(SC_OK);
            TransactionDto updatedTransaction =
                JsonHandler.getFromJsonNode(updatedTransactionResponse.getFirstResult(), TransactionDto.class);

            assertThat(transactionDtoResult.getComment()).isEqualTo(newComment);
            assertNotNull(updatedTransaction.getCreationDate());
            assertNotNull(updatedTransaction.getLastUpdate());
            assertThat(updatedTransaction.getLastUpdate()).isGreaterThan(updatedTransaction.getCreationDate());
        }
    }



    @Test
    public void initTransactionFromProject() throws Exception {
        try (CollectExternalClient collectClient = CollectExternalClientFactory.getInstance().getClient()) {
            ProjectDto projectDto = initProjectData();
            String newComment = "New Comment";
            final RequestResponse<JsonNode> projectResponse = collectClient.initProject(vitamContext, projectDto);
            Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);

            ProjectDto projectDtoResult =
                JsonHandler.getFromJsonNode(((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                    ProjectDto.class);

            TransactionDto initialTransaction = new TransactionDto();
            initialTransaction.setName("Transaction1");

            // INSERT TRANSACTION
            RequestResponse<JsonNode> transactionResponse =
                collectClient.initTransaction(vitamContext, initialTransaction, projectDtoResult.getId());
            Assertions.assertThat(transactionResponse.getStatus()).isEqualTo(SC_OK);
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) transactionResponse;
            TransactionDto transactionDtoResult =
                JsonHandler.getFromJsonNode(requestResponseOK.getFirstResult(), TransactionDto.class);

            // GET PERSISTED TRANSACTION
            RequestResponse<JsonNode> persistedTransactionResponse =
                collectClient.getTransactionById(vitamContext, transactionDtoResult.getId());
            Assertions.assertThat(persistedTransactionResponse.getStatus()).isEqualTo(SC_OK);
            TransactionDto persistedTransaction = JsonHandler.getFromJsonNode(
                (((RequestResponseOK<JsonNode>) persistedTransactionResponse).getFirstResult()), TransactionDto.class);
            assertNotNull(persistedTransaction.getCreationDate());
            assertEquals(persistedTransaction.getCreationDate(), persistedTransaction.getLastUpdate());
            assertEquals(TransactionStatus.OPEN.toString(), persistedTransaction.getStatus());
            assertEquals(initialTransaction.getName(), persistedTransaction.getName());
            assertEquals(projectDto.getArchivalProfile(), persistedTransaction.getArchivalProfile());
            assertEquals(projectDto.getAcquisitionInformation(), persistedTransaction.getAcquisitionInformation());
            assertEquals(projectDto.getMessageIdentifier(), persistedTransaction.getMessageIdentifier());

            assertThat(persistedTransaction.getComment()).isNotEqualTo(newComment);


        }
    }

    @Test
    public void should_create_unit() throws Exception {
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            ProjectDto projectDto = initProjectData();

            final RequestResponse<JsonNode> projectResponse = client.initProject(vitamContext, projectDto);
            Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);

            ProjectDto projectDtoResult =
                JsonHandler.getFromJsonNode(((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                    ProjectDto.class);

            TransactionDto transactiondto = initTransaction();

            RequestResponse<JsonNode> transactionResponse =
                client.initTransaction(vitamContext, transactiondto, projectDtoResult.getId());
            Assertions.assertThat(transactionResponse.getStatus()).isEqualTo(200);

            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) transactionResponse;
            TransactionDto transactionDtoResult =
                JsonHandler.getFromJsonNode(requestResponseOK.getFirstResult(), TransactionDto.class);

            try (InputStream is = PropertiesUtils.getResourceAsStream(AU_TO_UPLOAD)) {
                RequestResponse<JsonNode> response =
                    client.uploadArchiveUnit(vitamContext, JsonHandler.getFromInputStream(is),
                        transactionDtoResult.getId());
                assertThat(response.getStatus()).isEqualTo(200);

            }
        }
    }

    @Test
    public void should_delete_transaction() throws Exception {
        try (CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            ProjectDto projectDto = initProjectData();

            final RequestResponse<JsonNode> projectResponse = client.initProject(vitamContext, projectDto);
            Assertions.assertThat(projectResponse.getStatus()).isEqualTo(200);

            ProjectDto projectDtoResult =
                JsonHandler.getFromJsonNode(((RequestResponseOK<JsonNode>) projectResponse).getFirstResult(),
                    ProjectDto.class);

            TransactionDto transactiondto = initTransaction();

            RequestResponse<JsonNode> transactionResponse =
                client.initTransaction(vitamContext, transactiondto, projectDtoResult.getId());
            Assertions.assertThat(transactionResponse.getStatus()).isEqualTo(200);

            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) transactionResponse;
            TransactionDto transactionDtoResult =
                JsonHandler.getFromJsonNode(requestResponseOK.getFirstResult(), TransactionDto.class);

            RequestResponse<JsonNode> response =
                client.deleteTransactionById(vitamContext, transactionDtoResult.getId());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
