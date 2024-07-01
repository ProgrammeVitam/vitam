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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.logbook.common.model.coherence.OutcomeStatus;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckAttachementActionHandlerTest {

    private static final String OPI = "#opi";

    private static final String OPI_ID = "OPI_ID";
    private static final String GOT_ID = "GOT_ID";
    private static final String UNIT_ID = "UNIT_ID";

    private static final String EXISTING_GOT_JSON = "[\"" + GOT_ID + "\"]";
    private static final String EXISTING_UNIT_JSON = "[\"" + UNIT_ID + "\"]";

    private static final String EMPTY_JSON_ARRAY = "[]";

    private MetaDataClient metaDataClient;
    private ProcessingManagementClient processingManagementClient;
    private LogbookOperationsClient logbookOperationsClient;

    private CheckAttachementActionHandler checkAttachementActionHandler;

    @Before
    public void setUp() {
        metaDataClient = mock(MetaDataClient.class);
        MetaDataClientFactory metaDataClientFactory = mock(MetaDataClientFactory.class);
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);

        processingManagementClient = mock(ProcessingManagementClient.class);
        ProcessingManagementClientFactory processingManagementClientFactory = mock(
            ProcessingManagementClientFactory.class
        );
        when(processingManagementClientFactory.getClient()).thenReturn(processingManagementClient);

        logbookOperationsClient = mock(LogbookOperationsClient.class);
        LogbookOperationsClientFactory logbookOperationsClientFactory = mock(LogbookOperationsClientFactory.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);

        checkAttachementActionHandler = new CheckAttachementActionHandler(
            metaDataClientFactory,
            processingManagementClientFactory,
            logbookOperationsClientFactory
        );
    }

    @Test
    public void given_MD_when_attach_with_unexisting_MD_then_OK() throws Exception {
        WorkerParameters param = mock(WorkerParameters.class);
        HandlerIO handlerIO = mock(HandlerIO.class);

        when(handlerIO.getJsonFromWorkspace(anyString())).thenReturn(JsonHandler.getFromString(EMPTY_JSON_ARRAY));

        ItemStatus itemStatus = checkAttachementActionHandler.execute(param, handlerIO);

        assertEquals(itemStatus.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void given_MD_when_attach_with_existing_GOT_with_status_KO_Then_KO() throws Exception {
        //given
        WorkerParameters param = mock(WorkerParameters.class);
        HandlerIO handlerIO = mock(HandlerIO.class);
        when(handlerIO.getJsonFromWorkspace(anyString())).thenReturn(JsonHandler.getFromString(EMPTY_JSON_ARRAY));
        when(
            handlerIO.getJsonFromWorkspace(CheckAttachementActionHandler.MAPS_EXISTING_GOTS_GUID_FOR_ATTACHMENT_FILE)
        ).thenReturn(JsonHandler.getFromString(EXISTING_GOT_JSON));

        RequestResponseOK<Map<String, String>> requestResponse = new RequestResponseOK<>();
        requestResponse.addAllResults(Collections.singletonList(Collections.singletonMap(OPI, OPI_ID)));
        JsonNode result = JsonHandler.toJsonNode(requestResponse);

        when(metaDataClient.selectObjectGroups((any(JsonNode.class)))).thenReturn(result);

        LogbookOperation lastLogbookOperation = new LogbookOperation();
        lastLogbookOperation.setEvIdProc(OPI_ID);
        lastLogbookOperation.setEvType(IngestWorkflowConstants.WORKFLOW_IDENTIFIER);
        lastLogbookOperation.setOutcome(OutcomeStatus.KO.toString());

        LogbookOperation logbookOperation = new LogbookOperation();
        logbookOperation.setEvents(Collections.singletonList(lastLogbookOperation));

        RequestResponseOK<LogbookOperation> responseOK = new RequestResponseOK<>();
        responseOK.addAllResults(Collections.singletonList(logbookOperation));
        JsonNode logbookOperationresult = JsonHandler.toJsonNode(responseOK);

        when(logbookOperationsClient.selectOperation(any(ObjectNode.class))).thenReturn(logbookOperationresult);

        when(processingManagementClient.getOperationProcessStatus(OPI_ID)).thenThrow(new WorkflowNotFoundException(""));

        //when
        ItemStatus itemStatus = checkAttachementActionHandler.execute(param, handlerIO);

        //then
        assertEquals(itemStatus.getGlobalStatus(), StatusCode.KO);
    }

    @Test
    public void given_MD_when_attach_with_existing_Unit_with_status_KO_Then_KO() throws Exception {
        //given
        WorkerParameters param = mock(WorkerParameters.class);
        HandlerIO handlerIO = mock(HandlerIO.class);
        when(handlerIO.getJsonFromWorkspace(anyString())).thenReturn(JsonHandler.getFromString(EMPTY_JSON_ARRAY));
        when(
            handlerIO.getJsonFromWorkspace(CheckAttachementActionHandler.MAPS_EXISITING_UNITS_FOR_ATTACHMENT_FILE)
        ).thenReturn(JsonHandler.getFromString(EXISTING_UNIT_JSON));

        RequestResponseOK<Map<String, String>> requestResponse = new RequestResponseOK<>();
        requestResponse.addAllResults(Collections.singletonList(Collections.singletonMap(OPI, OPI_ID)));
        JsonNode result = JsonHandler.toJsonNode(requestResponse);

        when(metaDataClient.selectUnits((any(JsonNode.class)))).thenReturn(result);

        LogbookOperation lastLogbookOperation = new LogbookOperation();
        lastLogbookOperation.setEvIdProc(OPI_ID);
        lastLogbookOperation.setEvType(IngestWorkflowConstants.WORKFLOW_IDENTIFIER);
        lastLogbookOperation.setOutcome(OutcomeStatus.KO.toString());

        LogbookOperation logbookOperation = new LogbookOperation();
        logbookOperation.setEvents(Collections.singletonList(lastLogbookOperation));

        RequestResponseOK<LogbookOperation> responseOK = new RequestResponseOK<>();
        responseOK.addAllResults(Collections.singletonList(logbookOperation));
        JsonNode logbookOperationresult = JsonHandler.toJsonNode(responseOK);

        when(logbookOperationsClient.selectOperation(any(ObjectNode.class))).thenReturn(logbookOperationresult);

        when(processingManagementClient.getOperationProcessStatus(OPI_ID)).thenThrow(new WorkflowNotFoundException(""));

        //when
        ItemStatus itemStatus = checkAttachementActionHandler.execute(param, handlerIO);

        //then
        assertEquals(itemStatus.getGlobalStatus(), StatusCode.KO);
    }

    @Test
    public void given_MD_when_attach_with_existing_GOT_with_status_OK_Then_OK() throws Exception {
        //given
        WorkerParameters param = mock(WorkerParameters.class);
        HandlerIO handlerIO = mock(HandlerIO.class);
        when(handlerIO.getJsonFromWorkspace(anyString())).thenReturn(JsonHandler.getFromString(EMPTY_JSON_ARRAY));
        when(
            handlerIO.getJsonFromWorkspace(CheckAttachementActionHandler.MAPS_EXISTING_GOTS_GUID_FOR_ATTACHMENT_FILE)
        ).thenReturn(JsonHandler.getFromString(EXISTING_GOT_JSON));

        RequestResponseOK<Map<String, String>> requestResponse = new RequestResponseOK<>();
        requestResponse.addAllResults(Collections.singletonList(Collections.singletonMap(OPI, OPI_ID)));
        JsonNode result = JsonHandler.toJsonNode(requestResponse);

        when(metaDataClient.selectObjectGroups((any(JsonNode.class)))).thenReturn(result);

        LogbookOperation lastLogbookOperation = new LogbookOperation();
        lastLogbookOperation.setEvIdProc(OPI_ID);
        lastLogbookOperation.setEvType(IngestWorkflowConstants.WORKFLOW_IDENTIFIER);
        lastLogbookOperation.setOutcome(OutcomeStatus.OK.toString());

        LogbookOperation logbookOperation = new LogbookOperation();
        logbookOperation.setEvents(Collections.singletonList(lastLogbookOperation));

        RequestResponseOK<LogbookOperation> responseOK = new RequestResponseOK<>();
        responseOK.addAllResults(Collections.singletonList(logbookOperation));
        JsonNode logbookOperationresult = JsonHandler.toJsonNode(responseOK);

        when(logbookOperationsClient.selectOperation(any(ObjectNode.class))).thenReturn(logbookOperationresult);

        when(processingManagementClient.getOperationProcessStatus(OPI_ID)).thenThrow(new WorkflowNotFoundException(""));

        //when
        ItemStatus itemStatus = checkAttachementActionHandler.execute(param, handlerIO);

        //then
        assertEquals(itemStatus.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void given_MD_when_attach_with_existing_Unit_with_status_OK_Then_OK() throws Exception {
        //given
        WorkerParameters param = mock(WorkerParameters.class);
        HandlerIO handlerIO = mock(HandlerIO.class);
        when(handlerIO.getJsonFromWorkspace(anyString())).thenReturn(JsonHandler.getFromString(EMPTY_JSON_ARRAY));
        when(
            handlerIO.getJsonFromWorkspace(CheckAttachementActionHandler.MAPS_EXISITING_UNITS_FOR_ATTACHMENT_FILE)
        ).thenReturn(JsonHandler.getFromString(EXISTING_UNIT_JSON));

        RequestResponseOK<Map<String, String>> requestResponse = new RequestResponseOK<>();
        requestResponse.addAllResults(Collections.singletonList(Collections.singletonMap(OPI, OPI_ID)));
        JsonNode result = JsonHandler.toJsonNode(requestResponse);

        when(metaDataClient.selectUnits((any(JsonNode.class)))).thenReturn(result);

        LogbookOperation lastLogbookOperation = new LogbookOperation();
        lastLogbookOperation.setEvIdProc(OPI_ID);
        lastLogbookOperation.setEvType(IngestWorkflowConstants.WORKFLOW_IDENTIFIER);
        lastLogbookOperation.setOutcome(OutcomeStatus.OK.toString());

        LogbookOperation logbookOperation = new LogbookOperation();
        logbookOperation.setEvents(Collections.singletonList(lastLogbookOperation));

        RequestResponseOK<LogbookOperation> responseOK = new RequestResponseOK<>();
        responseOK.addAllResults(Collections.singletonList(logbookOperation));
        JsonNode logbookOperationresult = JsonHandler.toJsonNode(responseOK);

        when(logbookOperationsClient.selectOperation(any(ObjectNode.class))).thenReturn(logbookOperationresult);

        when(processingManagementClient.getOperationProcessStatus(OPI_ID)).thenThrow(new WorkflowNotFoundException(""));

        //when
        ItemStatus itemStatus = checkAttachementActionHandler.execute(param, handlerIO);

        //then
        assertEquals(itemStatus.getGlobalStatus(), StatusCode.OK);
    }
}
