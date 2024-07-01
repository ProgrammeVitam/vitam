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
package fr.gouv.vitam.access.internal.rest;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit Test class
 */
public class AccessInternalResourceTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private LogbookOperationsClient logbookOperationsClient;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private ProcessingManagementClientFactory processingClientFactory;

    @Mock
    private ProcessingManagementClient processingClient;

    @InjectMocks
    private AccessInternalResourceImpl accessInternalResource;

    @Before
    public void init() {
        GUID request = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(request);

        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);
        given(logbookOperationsClientFactory.getClient()).willReturn(logbookOperationsClient);
        given(processingClientFactory.getClient()).willReturn(processingClient);
    }

    @Test
    @RunWithCustomExecutor
    public void should_create_logbook_when_export_dip() throws Exception {
        // Given
        GUID request = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(request);
        AccessContractModel contract = new AccessContractModel();
        contract.setEveryOriginatingAgency(true);
        contract.setAccessLog(ActivationStatus.ACTIVE);
        VitamThreadUtils.getVitamSession().setContract(contract);

        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);
        given(logbookOperationsClientFactory.getClient()).willReturn(logbookOperationsClient);

        doReturn(new RequestResponseOK<>()).when(processingClient).executeOperationProcess(any(), any(), any());

        // When
        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.eq(VitamFieldsHelper.id(), "test"));
        accessInternalResource.exportDIP(select.getFinalSelect());

        // Then
        checkLogbookStarted(Contexts.EXPORT_DIP);
        checkWorkflowCreated(Contexts.EXPORT_DIP);
        checkSaveFileToWorkspace("query.json");
    }

    /*
     * Elimination analysis
     */
    @Test
    @RunWithCustomExecutor
    public void testStartEliminationAnalysisWorkflow_ShouldStartWorkflow() throws Exception {
        // Given
        AccessContractModel contract = new AccessContractModel();
        contract.setEveryOriginatingAgency(true);
        contract.setAccessLog(ActivationStatus.ACTIVE);
        VitamThreadUtils.getVitamSession().setContract(contract);

        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.eq(VitamFieldsHelper.id(), "test"));

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2011-01-23",
            select.getFinalSelect()
        );

        doReturn(new RequestResponseOK<>()).when(processingClient).executeOperationProcess(any(), any(), any());

        // When
        accessInternalResource.startEliminationAnalysisWorkflow(eliminationRequestBody);

        // Then
        checkLogbookStarted(Contexts.ELIMINATION_ANALYSIS);
        checkWorkflowCreated(Contexts.ELIMINATION_ANALYSIS);
        checkSaveFileToWorkspace("request.json");
    }

    /*
     * Elimination action
     */
    @Test
    @RunWithCustomExecutor
    public void testStartEliminationActionWorkflow_ShouldStartWorkflow() throws Exception {
        // Given
        AccessContractModel contract = new AccessContractModel();
        contract.setEveryOriginatingAgency(true);
        contract.setAccessLog(ActivationStatus.ACTIVE);
        VitamThreadUtils.getVitamSession().setContract(contract);

        SelectMultiQuery select = new SelectMultiQuery();
        select.setQuery(QueryHelper.eq(VitamFieldsHelper.id(), "test"));

        EliminationRequestBody eliminationRequestBody = new EliminationRequestBody(
            "2011-01-23",
            select.getFinalSelect()
        );

        doReturn(new RequestResponseOK<>()).when(processingClient).executeOperationProcess(any(), any(), any());

        // When
        accessInternalResource.startEliminationActionWorkflow(eliminationRequestBody);

        // Then
        checkLogbookStarted(Contexts.ELIMINATION_ACTION);
        checkWorkflowCreated(Contexts.ELIMINATION_ACTION);
        checkSaveFileToWorkspace("request.json");
    }

    /*
     * Bulk Atomic update
     */
    @Test
    @RunWithCustomExecutor
    public void testBuklAtomicUpdate_launchedOK() throws Exception {
        // Given
        AccessContractModel contract = new AccessContractModel();
        contract.setEveryOriginatingAgency(true);
        contract.setWritingPermission(true);
        contract.setAccessLog(ActivationStatus.ACTIVE);
        VitamThreadUtils.getVitamSession().setContract(contract);

        UpdateMultiQuery update = new UpdateMultiQuery();
        update.setQuery(QueryHelper.eq(VitamFieldsHelper.id(), "test"));
        update.addActions(UpdateActionHelper.set("Title", "My title"));

        ArrayNode queries = JsonHandler.createArrayNode();
        queries.add(update.getFinalUpdate());
        ObjectNode requestBody = JsonHandler.createObjectNode();
        requestBody.set("queries", queries);

        doReturn(new RequestResponseOK<>()).when(processingClient).executeOperationProcess(any(), any(), any());

        // When
        accessInternalResource.bulkAtomicUpdateUnits(requestBody);

        // Then
        checkLogbookStarted(Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC);
        checkWorkflowCreated(Contexts.BULK_ATOMIC_UPDATE_UNIT_DESC);
        checkSaveFileToWorkspace("query.json");
    }

    @Test
    @RunWithCustomExecutor
    public void testBuklAtomicUpdate_whenWritinPermissionFalse_then_Unauthorized() throws Exception {
        // Given
        AccessContractModel contract = new AccessContractModel();
        contract.setEveryOriginatingAgency(true);
        contract.setWritingPermission(false);
        contract.setAccessLog(ActivationStatus.ACTIVE);
        VitamThreadUtils.getVitamSession().setContract(contract);

        UpdateMultiQuery update = new UpdateMultiQuery();
        update.setQuery(QueryHelper.eq(VitamFieldsHelper.id(), "test"));
        update.addActions(UpdateActionHelper.set("Title", "My title"));

        ArrayNode queries = JsonHandler.createArrayNode();
        queries.add(update.getFinalUpdate());
        ObjectNode requestBody = JsonHandler.createObjectNode();
        requestBody.set("queries", queries);

        // When
        Response response = accessInternalResource.bulkAtomicUpdateUnits(requestBody);

        // Then
        assertThat(response.getStatus()).isEqualTo(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void testBuklAtomicUpdate_whenPermissionPermissionNull_then_Unauthorized() throws Exception {
        // Given
        AccessContractModel contract = new AccessContractModel();
        contract.setEveryOriginatingAgency(true);
        contract.setWritingPermission(null);
        contract.setAccessLog(ActivationStatus.ACTIVE);
        VitamThreadUtils.getVitamSession().setContract(contract);

        UpdateMultiQuery update = new UpdateMultiQuery();
        update.setQuery(QueryHelper.eq(VitamFieldsHelper.id(), "test"));
        update.addActions(UpdateActionHelper.set("Title", "My title"));

        ArrayNode queries = JsonHandler.createArrayNode();
        queries.add(update.getFinalUpdate());
        ObjectNode requestBody = JsonHandler.createObjectNode();
        requestBody.set("queries", queries);

        // When
        Response response = accessInternalResource.bulkAtomicUpdateUnits(requestBody);

        // Then
        assertThat(response.getStatus()).isEqualTo(Status.UNAUTHORIZED.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void testBuklAtomicUpdate_whenWorkspaceError_then_ServerError() throws Exception {
        // Given
        AccessContractModel contract = new AccessContractModel();
        contract.setEveryOriginatingAgency(true);
        contract.setWritingPermission(true);
        contract.setAccessLog(ActivationStatus.ACTIVE);
        VitamThreadUtils.getVitamSession().setContract(contract);

        UpdateMultiQuery update = new UpdateMultiQuery();
        update.setQuery(QueryHelper.eq(VitamFieldsHelper.id(), "test"));
        update.addActions(UpdateActionHelper.set("Title", "My title"));

        ArrayNode queries = JsonHandler.createArrayNode();
        queries.add(update.getFinalUpdate());
        ObjectNode requestBody = JsonHandler.createObjectNode();
        requestBody.set("queries", queries);

        doReturn(new RequestResponseOK<>()).when(processingClient).executeOperationProcess(any(), any(), any());
        doThrow(ContentAddressableStorageServerException.class).when(workspaceClient).putObject(any(), any(), any());

        // When
        Response response = accessInternalResource.bulkAtomicUpdateUnits(requestBody);

        // Then
        assertThat(response.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void testBuklAtomicUpdate_whenProcessinBadRequest_then_BadRequest() throws Exception {
        // Given
        AccessContractModel contract = new AccessContractModel();
        contract.setEveryOriginatingAgency(true);
        contract.setWritingPermission(true);
        contract.setAccessLog(ActivationStatus.ACTIVE);
        VitamThreadUtils.getVitamSession().setContract(contract);

        UpdateMultiQuery update = new UpdateMultiQuery();
        update.setQuery(QueryHelper.eq(VitamFieldsHelper.id(), "test"));
        update.addActions(UpdateActionHelper.set("Title", "My title"));

        ArrayNode queries = JsonHandler.createArrayNode();
        queries.add(update.getFinalUpdate());
        ObjectNode requestBody = JsonHandler.createObjectNode();
        requestBody.set("queries", queries);

        doReturn(new RequestResponseOK<>()).when(processingClient).executeOperationProcess(any(), any(), any());
        doThrow(BadRequestException.class).when(processingClient).initVitamProcess(any());

        // When
        Response response = accessInternalResource.bulkAtomicUpdateUnits(requestBody);

        // Then
        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    private void checkLogbookStarted(Contexts event)
        throws LogbookClientBadRequestException, LogbookClientAlreadyExistsException, LogbookClientServerException {
        ArgumentCaptor<LogbookOperationParameters> logbookParamsCaptor = forClass(LogbookOperationParameters.class);

        verify(logbookOperationsClient).create(logbookParamsCaptor.capture());

        assertThat(logbookParamsCaptor.getValue()).isNotNull();
        assertThat(
            logbookParamsCaptor.getValue().getParameterValue(LogbookParameterName.outcomeDetailMessage)
        ).contains(VitamLogbookMessages.getLabelOp(event.name() + ".STARTED"));
        assertThat(
            logbookParamsCaptor.getValue().getParameterValue(LogbookParameterName.eventIdentifierRequest)
        ).isEqualTo(VitamThreadUtils.getVitamSession().getRequestId());
    }

    private void checkWorkflowCreated(Contexts context)
        throws BadRequestException, InternalServerException, VitamClientException {
        ArgumentCaptor<ProcessingEntry> processingEntryArgumentCaptor = ArgumentCaptor.forClass(ProcessingEntry.class);

        verify(processingClient).initVitamProcess(processingEntryArgumentCaptor.capture());

        assertThat(processingEntryArgumentCaptor.getValue().getContainer()).isEqualTo(
            VitamThreadUtils.getVitamSession().getRequestId()
        );
        assertThat(processingEntryArgumentCaptor.getValue().getWorkflow()).isEqualTo(context.getEventType());

        verify(processingClient).executeOperationProcess(
            VitamThreadUtils.getVitamSession().getRequestId(),
            context.name(),
            ProcessAction.RESUME.getValue()
        );
    }

    private void checkSaveFileToWorkspace(String s) throws ContentAddressableStorageServerException {
        verify(workspaceClient).putObject(eq(VitamThreadUtils.getVitamSession().getRequestId()), eq(s), any());
    }
}
