package fr.gouv.vitam.worker.core.plugin.elimination;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;

import fr.gouv.vitam.common.error.ServiceName;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AccessionRegisterException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class EliminationActionAccessionRegisterUpdatePluginTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();


    @Mock
    private AdminManagementClientFactory adminManagementClientFactory;

    @Mock
    private AdminManagementClient adminManagementClient;

    @InjectMocks
    private EliminationActionAccessionRegisterUpdatePlugin instance;

    @Mock
    private HandlerIO handler;

    private WorkerParameters params;

    @Before
    public void setUp() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");

        doReturn(adminManagementClient).when(adminManagementClientFactory).getClient();


        params = WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
            .newGUID()).setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
            .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
            .setProcessId(VitamThreadUtils.getVitamSession().getRequestId())
            .setObjectName("REF")
            .setCurrentStep("StepName")
            .setObjectMetadata(JsonHandler.createObjectNode());
    }

    @Test
    @RunWithCustomExecutor
    public void test_when_update_accession_register_then_FATAL() throws Exception {

        when(adminManagementClient.createOrUpdateAccessionRegister(any()))
            .thenThrow(new AccessionRegisterException("Simulate FATAL"));

        // Given / When
        ItemStatus itemStatus = instance.execute(params, handler);

        Assertions.assertThat(itemStatus).isNotNull();
        Assertions.assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }


    @Test
    @RunWithCustomExecutor
    public void test_when_update_accession_register_then_Conflict() throws Exception {

        VitamError ve =
            new VitamError(Response.Status.CONFLICT.name()).setHttpCode(Response.Status.CONFLICT.getStatusCode())
                .setContext(ServiceName.EXTERNAL_ACCESS.getName())
                .setState("code_vitam")
                .setMessage(Response.Status.CONFLICT.getReasonPhrase())
                .setDescription("Document already exists in database");

        when(adminManagementClient.createOrUpdateAccessionRegister(any()))
            .thenReturn(ve);
        // Given / When
        ItemStatus itemStatus = instance.execute(params, handler);

        Assertions.assertThat(itemStatus).isNotNull();
        Assertions.assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.ALREADY_EXECUTED);
    }


    @Test
    @RunWithCustomExecutor
    public void test_when_update_accession_register_then_OK() throws Exception {

        RequestResponse<AccessionRegisterDetailModel> resp = new RequestResponseOK<>();
        resp.setHttpCode(Response.Status.OK.getStatusCode());

        when(adminManagementClient.createOrUpdateAccessionRegister(any()))
            .thenReturn(resp);

        // Given / When
        ItemStatus itemStatus = instance.execute(params, handler);

        Assertions.assertThat(itemStatus).isNotNull();
        Assertions.assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }
}
