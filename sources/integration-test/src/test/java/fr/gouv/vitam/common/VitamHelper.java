/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

package fr.gouv.vitam.common;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import org.apache.commons.collections4.iterators.PeekingIterator;
import org.bson.Document;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class VitamHelper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamHelper.class);
    private static final long SLEEP_TIME = 20L;

    public static void runStepByStepUntilStepReached(String operationGuid, String targetStepName)
        throws VitamClientException, InternalServerException, InterruptedException, ProcessingException {

        try (ProcessingManagementClient processingClient =
            ProcessingManagementClientFactory.getInstance().getClient()) {

            while (true) {
                ProcessQuery processQuery = new ProcessQuery();
                processQuery.setId(operationGuid);
                RequestResponse<ProcessDetail> response = processingClient.listOperationsDetails(processQuery);
                if(response.isOk()) {
                    ProcessDetail processDetail = ((RequestResponseOK<ProcessDetail>) response).getResults().get(0);
                    switch (ProcessState.valueOf(processDetail.getGlobalState())) {

                        case PAUSE:

                            if (processDetail.getPreviousStep().equals(targetStepName)) {
                                LOGGER.info(operationGuid + " finished step " + targetStepName);
                                return;
                            }
                            processingClient
                                .executeOperationProcess(operationGuid, Contexts.DEFAULT_WORKFLOW.name(),
                                    ProcessAction.NEXT.getValue());
                            break;

                        case RUNNING:

                            // Sleep and retry
                            TimeUnit.MILLISECONDS.sleep(SLEEP_TIME);
                            break;

                        case COMPLETED:
                            throw new VitamRuntimeException("Process completion unexpected " + JsonHandler.unprettyPrint(processDetail));
                    }
                } else {
                    throw new ProcessingException("listOperationsDetails does not response ...");
                }
            }
        }
    }

    public static void verifyLogbook(String operationId, String actionKey, String statusCode) {
        Document operation = (Document) LogbookCollections.OPERATION.getCollection().find(eq("_id", operationId)).first();
        assertThat(operation).isNotNull();
        assertTrue(operation.toString().contains(String.format("%s.%s",actionKey, statusCode)));
    }

    /**
     * Extract lines of jsonl report to list of JsonNode
     * @param reportResponse
     * @return
     * @throws IOException
     * @throws InvalidParseOperationException
     */
    public static List<JsonNode> getReport(Response reportResponse) throws IOException, InvalidParseOperationException {
        List<JsonNode> reportLines = new ArrayList<>();
        try (InputStream is = reportResponse.readEntity(InputStream.class)) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            PeekingIterator<String> linesPeekIterator = new PeekingIterator<>(bufferedReader.lines().iterator());
            while (linesPeekIterator.hasNext()) {
                reportLines.add(JsonHandler.getFromString(linesPeekIterator.next()));
            }
        }
        return reportLines;
    }
}
