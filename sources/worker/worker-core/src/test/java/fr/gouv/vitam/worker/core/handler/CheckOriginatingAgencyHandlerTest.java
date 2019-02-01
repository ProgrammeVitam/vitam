package fr.gouv.vitam.worker.core.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response.Status;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;


public class CheckOriginatingAgencyHandlerTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private AdminManagementClient adminClient;

    @Mock
    private AdminManagementClientFactory adminManagementClientFactory;

    @Mock
    private HandlerIO handlerIO;

    private GUID guid;
    private static final Integer TENANT_ID = 0;
    private static final String FAKE_URL = "http://localhost:8083";
    private static final String ORIGINATING_AGENCY_IDENTIFIER = "AG-0000001";
    private static final String TAG_ORIGINATINGAGENCYIDENTIFIER = "OriginatingAgencyIdentifier";
    private static final String EMPTY_REQUIRED_FIELD = "EMPTY_REQUIRED_FIELD";
    CheckOriginatingAgencyHandler handler;
    private static final String HANDLER_ID = "CHECK_AGENT";

    private static final String AGENCY = "{\"_id\":\"aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq\", " +
        "\"_tenant\":\"0\", " +
        "\"Identifier\":\"AG-0000002\", " +
        "\"Name\":\"agency\", " +
        "\"Description\":\"Une description\"}";



    @Before
    public void setUp() throws ProcessingException, FileNotFoundException {
        adminClient = mock(AdminManagementClient.class);
        guid = GUIDFactory.newGUID();
        when(adminManagementClientFactory.getClient()).thenReturn(adminClient);
        handler = new CheckOriginatingAgencyHandler(adminManagementClientFactory);
    }

    @Test
    @RunWithCustomExecutor
    public void givenOriginatingAgencyOKThenReturnOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        Map<String, Object> serviceAgent = new HashMap<>();
        serviceAgent.put(TAG_ORIGINATINGAGENCYIDENTIFIER, ORIGINATING_AGENCY_IDENTIFIER);
        // When
        when(handlerIO.getInput(0)).thenReturn(serviceAgent);
        when(adminClient.getAgencies(any())).thenReturn(ClientMockResultHelper.getAgency().toJsonNode());
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL).setUrlMetadata(FAKE_URL)
                .setObjectNameList(Lists.newArrayList("objectName.json"))
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName(guid.getId());
        // Then
        assertThat(CheckOriginatingAgencyHandler.getId()).isEqualTo(HANDLER_ID);
        ItemStatus response = handler.execute(params, handlerIO);
        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void givenOriginatingAgencyKOThenReturnKO() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        Map<String, Object> serviceAgent = new HashMap<>();
        serviceAgent.put(TAG_ORIGINATINGAGENCYIDENTIFIER, ORIGINATING_AGENCY_IDENTIFIER);
        // When
        when(handlerIO.getInput(0)).thenReturn(serviceAgent);
        when(adminClient.getAgencies(any())).thenReturn(createReponse(AGENCY).toJsonNode());
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL).setUrlMetadata(FAKE_URL)
                .setObjectNameList(Lists.newArrayList("objectName.json"))
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName(guid.getId());
        // Then
        assertThat(CheckOriginatingAgencyHandler.getId()).isEqualTo(HANDLER_ID);
        ItemStatus response = handler.execute(params, handlerIO);
        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.KO);
        String evDetData = response.getItemsStatus().get("CHECK_AGENT").getEvDetailData();
        assertThat(evDetData).contains("AG-000000").contains("error originating agency validation");


    }

    /**
     * Test if no originating agency in the SIP.
     *
     * @throws Exception Exception
     */
    @Test
    @RunWithCustomExecutor
    public void givenNoOriginatingAgencyThenReturnKO() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        Map<String, Object> serviceAgent = new HashMap<>();
        when(handlerIO.getInput(0)).thenReturn(serviceAgent);
        when(adminClient.getAgencies(any())).thenReturn(createReponse(AGENCY).toJsonNode());
        // When
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL).setUrlMetadata(FAKE_URL)
                .setObjectNameList(Lists.newArrayList("objectName.json"))
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName(guid.getId());
        assertThat(CheckOriginatingAgencyHandler.getId()).isEqualTo(HANDLER_ID);
        ItemStatus response = handler.execute(params, handlerIO);
        // Then
        assertThat(response.getGlobalOutcomeDetailSubcode()).isEqualTo(EMPTY_REQUIRED_FIELD);
        assertThat(response.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    /**
     * @param s the original object to be included in response
     * @return a default response
     * @throws InvalidParseOperationException
     */
    private static RequestResponse createReponse(String s) throws InvalidParseOperationException {
        RequestResponseOK responseOK = new RequestResponseOK();
        if (null != s)
            responseOK.addResult(JsonHandler.getFromString(s));
        return responseOK.setHttpCode(Status.OK.getStatusCode());
    }

}
