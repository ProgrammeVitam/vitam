package fr.gouv.vitam.worker.core.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response.Status;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

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
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({AdminManagementClientFactory.class})
public class CheckOriginatingAgencyHandlerTest {
    
    CheckOriginatingAgencyHandler handler;
    private static final String HANDLER_ID = "CHECK_AGENT";
    private AdminManagementClient adminClient;
    private AdminManagementClientFactory adminManagementClientFactory;
    private GUID guid;
    private static final Integer TENANT_ID = 0;
    private static final String FAKE_URL = "http://localhost:8083";
    private static final String ORIGINATING_AGENCY_IDENTIFIER = "AG-0000001";
    private static final String TAG_ORIGINATINGAGENCYIDENTIFIER = "OriginatingAgencyIdentifier";
    
    private static final String AGENCY = "{\"_id\":\"aeaaaaaaaaaaaaabaa4ikakyetch6mqaaacq\", " +
        "\"_tenant\":\"0\", " +
        "\"Identifier\":\"AG-0000002\", " +
        "\"Name\":\"agency\", " +
        "\"Description\":\"Une description\"}";

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private HandlerIO handlerIO = mock(HandlerIO.class);

    @Before
    public void setUp() throws ProcessingException, FileNotFoundException {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminClient = mock(AdminManagementClient.class);
        guid = GUIDFactory.newGUID();
        adminManagementClientFactory = mock(AdminManagementClientFactory.class);
        PowerMockito.when(AdminManagementClientFactory.getInstance()).thenReturn(adminManagementClientFactory);
        PowerMockito.when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminClient);

    }
    
    @Test
    @RunWithCustomExecutor
    public void givenOriginatingAgencyOKThenReturnOK() throws AdminManagementClientServerException, ReferentialException, InvalidParseOperationException {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        Map<String, Object> serviceAgent = new HashMap<>();
        serviceAgent.put(TAG_ORIGINATINGAGENCYIDENTIFIER, ORIGINATING_AGENCY_IDENTIFIER);

        when(handlerIO.getInput(0)).thenReturn(serviceAgent);
        when(adminClient.getAgencies(anyObject())).thenReturn(ClientMockResultHelper.getAgency().toJsonNode());

        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL).setUrlMetadata(FAKE_URL)
                    .setObjectNameList(Lists.newArrayList("objectName.json"))
                    .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName(guid.getId());
        handler = new CheckOriginatingAgencyHandler();
        assertEquals(CheckOriginatingAgencyHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void givenOriginatingAgencyKOThenReturnKO() throws AdminManagementClientServerException, ReferentialException, InvalidParseOperationException {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        Map<String, Object> serviceAgent = new HashMap<>();
        serviceAgent.put(TAG_ORIGINATINGAGENCYIDENTIFIER, ORIGINATING_AGENCY_IDENTIFIER);

        when(handlerIO.getInput(0)).thenReturn(serviceAgent);
        when(adminClient.getAgencies(anyObject())).thenReturn(createReponse(AGENCY).toJsonNode());

        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL).setUrlMetadata(FAKE_URL)
                    .setObjectNameList(Lists.newArrayList("objectName.json"))
                    .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName(guid.getId());
        handler = new CheckOriginatingAgencyHandler();
        assertEquals(CheckOriginatingAgencyHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);

        String evDetData = response.getItemsStatus().get("CHECK_AGENT").getEvDetailData();
        assertTrue(evDetData.contains("AG-000000"));
        assertTrue(evDetData.contains("error originating agency validation"));

    }

    /**
     * Test if no originating agency in the SIP.
     *
     * @throws AdminManagementClientServerException
     * @throws ReferentialException
     * @throws InvalidParseOperationException
     */
    @Test
    @RunWithCustomExecutor
    public void givenNoOriginatingAgencyThenReturnKO()
        throws AdminManagementClientServerException, ReferentialException, InvalidParseOperationException {

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        Map<String, Object> serviceAgent = new HashMap<>();

        when(handlerIO.getInput(0)).thenReturn(serviceAgent);
        when(adminClient.getAgencies(anyObject())).thenReturn(createReponse(AGENCY).toJsonNode());

        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL).setUrlMetadata(FAKE_URL)
                .setObjectNameList(Lists.newArrayList("objectName.json"))
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName(guid.getId());
        handler = new CheckOriginatingAgencyHandler();
        assertEquals(CheckOriginatingAgencyHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }
    
    /**
     * @param s the original object to be included in response
     * @return a default response
     * @throws InvalidParseOperationException
     */
    public static RequestResponse createReponse(String s) throws InvalidParseOperationException {
        RequestResponseOK responseOK =  new RequestResponseOK();
        if (null != s)
            responseOK.addResult(JsonHandler.getFromString(s));
        return responseOK.setHttpCode(Status.OK.getStatusCode());
    }

}
