package fr.gouv.vitam.worker.core.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ContractStatus;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.client.model.IngestContractModel;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({AdminManagementClientFactory.class})
public class CheckIngestContractActionHandlerTest {
    CheckIngestContractActionHandler handler;
    private static final String HANDLER_ID = "CHECK_CONTRACT_INGEST";
    private AdminManagementClient adminClient;
    private AdminManagementClientFactory adminManagementClientFactory;
    private GUID guid;
    private static final Integer TENANT_ID = 0;
    private static final String FAKE_URL = "http://localhost:8083";
    private static final String CONTRACT_NAME = "ArchivalAgreement0";

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
    public void givenSipWithValidContractReferenceFoundThenReturnResponseOK()
        throws XMLStreamException, IOException, ProcessingException, InvalidParseOperationException,
        InvalidCreateOperationException, AdminManagementClientServerException,
        ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(handlerIO.getInput(0)).thenReturn(CONTRACT_NAME);

        when(adminClient.findIngestContracts(anyObject()))
            .thenReturn(createIngestContract(ContractStatus.ACTIVE.toString()));

        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL).setUrlMetadata(FAKE_URL)
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName(guid.getId());
        handler = new CheckIngestContractActionHandler();
        assertEquals(CheckIngestContractActionHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);

        reset(adminClient);
        when(adminClient.findIngestContracts(anyObject()))
            .thenReturn(createIngestContract(ContractStatus.INACTIVE.toString()));
        response = handler.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }

    private static RequestResponse createIngestContract(String status) throws InvalidParseOperationException {
        IngestContractModel contract = new IngestContractModel();
        contract.setName("testOK");
        contract.setStatus(status);
        return ClientMockResultHelper.createReponse(contract);
    }
}
