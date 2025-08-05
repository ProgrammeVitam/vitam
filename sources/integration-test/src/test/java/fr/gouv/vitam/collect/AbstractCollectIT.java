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
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.client.VitamPoolingClient;
import fr.gouv.vitam.collect.internal.client.CollectInternalClient;
import fr.gouv.vitam.collect.internal.client.CollectInternalClientFactory;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.VitamTestHelper;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.logbook.common.parameters.Contexts;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Abstract base class for Collect integration tests.
 * Contains common methods and fields used by both CollectIngestIT and CollectSipIngestIT.
 */
public abstract class AbstractCollectIT extends VitamRuleRunner {

    protected static final Integer TENANT_ID = 0;
    protected static final String ACCESS_CONTRACT = "aName3";

    protected final VitamContext vitamContext = new VitamContext(TENANT_ID);

    /**
     * Prepares the Vitam session with tenant ID and contract information.
     */
    public static void prepareVitamSession() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setContractId("aName3");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
    }

    /**
     * Switches from metadata-collect to metadata server.
     * This is used when transitioning from collect operations to Vitam core operations.
     *
     * @param runner The VitamServerRunner instance
     * @throws VitamApplicationServerException If server operations fail
     * @throws IOException If I/O operations fail
     */
    protected static void switchToVitamMetadataHack(VitamServerRunner runner)
        throws VitamApplicationServerException, IOException {
        // turn off metadata-collect and run metadata
        runner.stopMetadataCollectServer(true);
        runner.startMetadataServer();
    }

    /**
     * Switches from metadata server to metadata-collect server.
     * This is used when transitioning from Vitam core operations to collect operations.
     *
     * @param runner The VitamServerRunner instance
     * @throws IOException If I/O operations fail
     * @throws VitamApplicationServerException If server operations fail
     */
    protected static void switchToCollectMetadataHack(VitamServerRunner runner)
        throws IOException, VitamApplicationServerException {
        // turn off metadata and run metadata-collect
        runner.stopMetadataServer(true);
        runner.startMetadataCollectServer();
    }

    /**
     * Ingests a SIP to Vitam using the default workflow.
     *
     * @param inputStream The SIP input stream
     * @return The process ID of the ingest operation
     * @throws VitamException If any Vitam operation fails
     */
    protected String ingestToVitam(InputStream inputStream) throws VitamException {
        return ingestToVitam(inputStream, DEFAULT_WORKFLOW);
    }

    /**
     * Ingests a SIP to Vitam using the specified workflow context.
     *
     * @param inputStream The SIP input stream
     * @param contexts The workflow context to use
     * @return The process ID of the ingest operation
     * @throws VitamException If any Vitam operation fails
     */
    protected String ingestToVitam(InputStream inputStream, Contexts contexts) throws VitamException {
        String processId;

        try (
            IngestExternalClient ingestExternalClient = IngestExternalClientFactory.getInstance().getClient();
            AdminExternalClient adminExternalClient = AdminExternalClientFactory.getInstance().getClient()
        ) {
            RequestResponse<Void> ingest = ingestExternalClient.ingest(
                vitamContext,
                inputStream,
                contexts.name(),
                ProcessAction.RESUME.name()
            );
            processId = ingest.getVitamHeaders().get(GlobalDataRest.X_REQUEST_ID);

            final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(adminExternalClient);
            boolean process_timeout = vitamPoolingClient.wait(
                TENANT_ID,
                processId,
                ProcessState.COMPLETED,
                1800,
                1_000L,
                TimeUnit.MILLISECONDS
            );
            if (!process_timeout) {
                throw new RuntimeException(
                    "Sip processing not finished : operation (" + processId + "). Timeout exceeded."
                );
            }

            RequestResponse<ItemStatus> operationResponse = adminExternalClient.getOperationProcessExecutionDetails(
                new VitamContext(TENANT_ID),
                processId
            );
            assertTrue(operationResponse.isOk());
            VitamTestHelper.verifyOperation(processId, StatusCode.OK);
        }
        return processId;
    }

    /**
     * Generates a SIP from a transaction.
     *
     * @param idTransaction The transaction ID
     * @return An input stream containing the generated SIP
     * @throws VitamClientException If client operations fail
     * @throws InvalidParseOperationException If parsing operations fail
     */
    protected static InputStream generateSip(String idTransaction)
        throws VitamClientException, InvalidParseOperationException {
        InputStream inputStream;
        try (CollectInternalClient client = CollectInternalClientFactory.getInstance().getClient()) {
            inputStream = client.generateSip(idTransaction);
            RequestResponse<JsonNode> transactionResponse = client.getTransactionById(idTransaction);
            assertThat(transactionResponse.getStatus()).isEqualTo(200);

            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) transactionResponse;
            assertThat(
                JsonHandler.getFromJsonNode(
                    requestResponseOK.getFirstResult(),
                    fr.gouv.vitam.collect.common.dto.TransactionDto.class
                ).getStatus()
            ).isEqualTo(fr.gouv.vitam.collect.common.enums.TransactionStatus.SENDING.toString());
        }
        return inputStream;
    }

    /**
     * Selects Vitam metadata units by operation ID.
     *
     * @param processIds The operation IDs to search for
     * @return A list of JsonNode objects representing the units
     * @throws InvalidCreateOperationException If query creation fails
     * @throws VitamClientException If client operations fail
     */
    protected List<JsonNode> selectVitamMetadataUnitsByOpi(String... processIds)
        throws InvalidCreateOperationException, VitamClientException {
        try (AccessExternalClient accessExternalClient = AccessExternalClientFactory.getInstance().getClient()) {
            SelectMultiQuery select = new SelectMultiQuery();
            select.addQueries(QueryHelper.in(VitamFieldsHelper.initialOperation(), processIds));
            vitamContext.setAccessContract(ACCESS_CONTRACT);
            return (
                (RequestResponseOK<JsonNode>) accessExternalClient.selectUnits(vitamContext, select.getFinalSelect())
            ).getResults();
        }
    }

    /**
     * Gets a unit by its title from a list of results.
     *
     * @param results The list of units to search
     * @param title The title to search for
     * @return The JsonNode representing the unit with the specified title
     * @throws java.util.NoSuchElementException If no unit with the specified title is found
     */
    protected static JsonNode getUnitByTitle(List<JsonNode> results, String title) {
        return results.stream().filter(unit -> title.equals(unit.get("Title").asText())).findFirst().orElseThrow();
    }
}
