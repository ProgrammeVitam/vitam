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
package fr.gouv.vitam.functional.administration.rest;


import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.when;

/**
 * EvidenceResource Test
 */
public class EvidenceResourceTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
    @Mock ProcessingManagementClientFactory processingManagementClientFactory;
    @Mock ProcessingManagementClient processingManagementClient;
    @Mock WorkspaceClientFactory workspaceClientFactory;
    @Mock WorkspaceClient workspaceClient;
    @Mock LogbookOperationsClientFactory logbookOperationsClientFactory;
    @Mock LogbookOperationsClient logbookOperationsClient;
    @Mock MongoDbAccessAdminImpl mongoDbAccess;
    @Mock DbRequestResult dbRequestResult;
    @Mock VitamCounterService vitamCounterService;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private static final int TENANT_ID = 0;
    private EvidenceResource evidenceResource;

    @Before
    public void setUp() throws InvalidParseOperationException, ReferentialException {
        when(processingManagementClientFactory.getClient()).thenReturn(processingManagementClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);

        AccessContractModel accessContract = new AccessContractModel().setEveryOriginatingAgency(true)
            .setEveryDataObjectVersion(true);
        accessContract.setIdentifier("fakeContract");
        when(dbRequestResult.getDocuments(any(), any())).thenReturn(Arrays.asList(accessContract));
        when(mongoDbAccess.findDocuments(any(), any())).thenReturn(dbRequestResult);

        VitamThreadUtils.getVitamSession().setContractId("fakeContract");
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        evidenceResource =
            new EvidenceResource(processingManagementClientFactory, logbookOperationsClientFactory,
                workspaceClientFactory, mongoDbAccess, vitamCounterService);
        GUID guid = GUIDFactory.newEventGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(guid);
    }

    @Test
    @RunWithCustomExecutor
    public void given_empty_query_when_audit_then_return_forbidden_request() throws Exception {

        Response audit = evidenceResource.audit(new Select().getFinalSelect());
        assertThat(audit.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void given_good_query_then_return_ok_status() throws Exception {

        // given
        SelectMultiQuery selectMultiQuery = new SelectMultiQuery();

        selectMultiQuery.setQuery(QueryHelper.eq("title", "test"));

        when(processingManagementClient
            .executeOperationProcess(anyString(), eq("EVIDENCE_AUDIT"), anyString()))
            .thenReturn(new RequestResponseOK<ItemStatus>(new Select().getFinalSelect()).setHttpCode(200));
        Response audit = evidenceResource.audit(selectMultiQuery.getFinalSelect());

        //then
        assertThat(audit.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void fail_when_a_related_server_is_unavailable() throws Exception {
        SelectMultiQuery selectMultiQuery = new SelectMultiQuery();

        selectMultiQuery.setQuery(QueryHelper.eq("title", "test"));


        willThrow(ContentAddressableStorageServerException.class).given(workspaceClient)
            .putObject(anyString(), any(), any());
        Response audit = evidenceResource.audit(selectMultiQuery.getFinalSelect());
        assertThat(audit.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());


        willThrow(LogbookClientAlreadyExistsException.class).given(logbookOperationsClient).create(any());
        audit = evidenceResource.audit(selectMultiQuery.getFinalSelect());
        assertThat(audit.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());


    }

}
