package fr.gouv.vitam.functional.administration.rest;


import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProbativeValueRequest;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
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
 * ProbativeValueResource Test
 */
public class ProbativeValueResourceTest {


    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());
    @Mock ProcessingManagementClientFactory processingManagementClientFactory;
    @Mock ProcessingManagementClient processingManagementClient;
    @Mock WorkspaceClientFactory workspaceClientFactory;
    @Mock WorkspaceClient workspaceClient;
    @Mock LogbookOperationsClientFactory logbookOperationsClientFactory;
    @Mock LogbookOperationsClient logbookOperationsClient;
    @Mock AdminManagementClientFactory managementClientFactory;
    @Mock AdminManagementClient adminManagementClient;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private static final int TENANT_ID = 0;


    private ProbativeValueResource probativeValueResource;

    @Before
    public void setUp() throws InvalidParseOperationException, AdminManagementClientServerException {
        when(processingManagementClientFactory.getClient()).thenReturn(processingManagementClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        when(managementClientFactory.getClient()).thenReturn(adminManagementClient);
        when(adminManagementClient.findAccessContracts(any()))
                .thenReturn(new RequestResponseOK<AccessContractModel>()
                                .addAllResults(Arrays.asList(new AccessContractModel()
                                        .setEveryOriginatingAgency(true)
                                        .setEveryDataObjectVersion(true))));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        GUID guid = GUIDFactory.newEventGUID(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(guid);
        VitamThreadUtils.getVitamSession().setContractId("fakeContract");
        probativeValueResource =
            new ProbativeValueResource(processingManagementClientFactory, logbookOperationsClientFactory,
                workspaceClientFactory);
    }

    @Test
    @RunWithCustomExecutor
    public void given_empty_query_when_export_then_return_forbidden_request() {

        Response probativeValue = probativeValueResource
            .exportProbativeValue(new ProbativeValueRequest(new Select().getFinalSelect(), "BinaryMaster", "1"));
        assertThat(probativeValue.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void given_good_request_then_return_ok_request() throws Exception {

        Select select = new Select();
        select.setQuery(QueryHelper.eq("name", "dd"));
        ProbativeValueRequest probativeValueRequest = new ProbativeValueRequest(select.getFinalSelect(), "BinaryMaster", "1");

        Response probativeValue = probativeValueResource
            .exportProbativeValue(new ProbativeValueRequest(new Select().getFinalSelect(), "BinaryMaster", "1"));
        assertThat(probativeValue.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());

        when(processingManagementClient
            .executeOperationProcess(anyString(), eq("EXPORT_PROBATIVE_VALUE"), anyString()))
            .thenReturn(new RequestResponseOK<ItemStatus>(new Select().getFinalSelect()).setHttpCode(200));
        probativeValue = probativeValueResource.exportProbativeValue(probativeValueRequest);
        assertThat(probativeValue.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    @RunWithCustomExecutor
    public void fail_when_a_related_server_is_unavailable() throws Exception {

        Select select = new Select();
        select.setQuery(QueryHelper.eq("name", "dd"));
        ProbativeValueRequest probativeValueRequest = new ProbativeValueRequest(select.getFinalSelect(), "BinaryMaster", "1");
        willThrow(ContentAddressableStorageServerException.class).given(workspaceClient).putObject(anyString(), any(), any());
        Response probativeValue = probativeValueResource.exportProbativeValue(probativeValueRequest);
        assertThat(probativeValue.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());


        willThrow(LogbookClientAlreadyExistsException.class).given(logbookOperationsClient).create(any());
        probativeValue = probativeValueResource.exportProbativeValue(probativeValueRequest);
        assertThat(probativeValue.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

}
