package fr.gouv.vitam.worker.core.handler;

import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.model.administration.StorageDetailModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class CheckIngestContractActionHandlerTest {
    CheckIngestContractActionHandler handler;
    private static final String HANDLER_ID = "CHECK_CONTRACT_INGEST";
    private AdminManagementClient adminClient;
    private AdminManagementClientFactory adminManagementClientFactory;
    private StorageClient storageClient;
    private StorageClientFactory storageClientFactory;
    private GUID guid;

    private static final Integer TENANT_ID = 0;
    private static final String FAKE_URL = "http://localhost:8083";
    private static final String CONTRACT_NAME = "ArchivalAgreement0";
    private static final String CONTRACT_IDENTIFIER = "ArchivalAgreement0";
    private static final String MANAGEMENT_CONTRACT_IDENTIFIER = "ManagementContractIdentifier";
    private static final String COMMENT = "comment";
    private static final String ORIGINATING_AGENCY_IDENTIFIER = "OriginatingAgencyIdentifier";
    private static final String SUBMISSION_AGENCY_IDENTIFIER = "SubmissionAgencyIdentifier";
    private static final String ARCHIVAL_AGREEMENT = "ArchivalAgreement";
    private static final String MESSAGE_IDENTIFIER = "MessageIdentifier";

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
            VitamThreadPoolExecutor.getDefaultExecutor());

    private HandlerIO handlerIO = mock(HandlerIO.class);

    @Before
    public void setUp() throws ProcessingException, FileNotFoundException {
        adminClient = mock(AdminManagementClient.class);
        storageClient = mock(StorageClient.class);
        guid = GUIDFactory.newGUID();
        adminManagementClientFactory = mock(AdminManagementClientFactory.class);
        when(adminManagementClientFactory.getClient()).thenReturn(adminClient);
        storageClientFactory = mock(StorageClientFactory.class);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithValidContractReferenceFoundThenReturnResponseOK()
            throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setContextId("FakeContext");

        when(adminClient.findIngestContractsByID(any()))
                .thenReturn(createIngestContract(ActivationStatus.ACTIVE, null));
        when(adminClient.findContextById(any())).thenReturn(ClientMockResultHelper.getContexts(200));
        when(handlerIO.getInput(0)).thenReturn(getMandatoryValueMapInstance(true));

        handler = new CheckIngestContractActionHandler(adminManagementClientFactory, storageClientFactory);
        assertEquals(CheckIngestContractActionHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(getWorkerParametersInstance(), handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);

        reset(adminClient);
        when(adminClient.findIngestContractsByID(any()))
                .thenReturn(createIngestContract(ActivationStatus.INACTIVE, null));
        response = handler.execute(getWorkerParametersInstance(), handlerIO);
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "CONTRACT_INACTIVE");
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithoutContractThenReturnResponseKO() {

        when(handlerIO.getInput(0)).thenReturn(getMandatoryValueMapInstance(false));

        handler = new CheckIngestContractActionHandler(adminManagementClientFactory, storageClientFactory);
        assertEquals(CheckIngestContractActionHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(getWorkerParametersInstance(), handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
        Assertions.assertThat(response.getEvDetailData()).contains("Error ingest contract not found in the Manifest");
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithValidManagementContractThenReturnResponseOK() throws InvalidParseOperationException,
            AdminManagementClientServerException, ReferentialNotFoundException, StorageServerClientException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setContextId("FakeContext");

        when(adminClient.findIngestContractsByID(any()))
                .thenReturn(createIngestContract(ActivationStatus.ACTIVE, MANAGEMENT_CONTRACT_IDENTIFIER));
        when(adminClient.findContextById(any())).thenReturn(ClientMockResultHelper.getContexts(200));
        when(adminClient.findManagementContractsByID(any()))
                .thenReturn(createManagementContract(ActivationStatus.ACTIVE));
        when(storageClient.getStorageStrategies()).thenReturn(createStorageStrategies());
        when(handlerIO.getInput(0)).thenReturn(getMandatoryValueMapInstance(true));

        handler = new CheckIngestContractActionHandler(adminManagementClientFactory, storageClientFactory);
        assertEquals(CheckIngestContractActionHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(getWorkerParametersInstance(), handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);

    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithManagementContractNotFoundThenReturnResponseKO()
            throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setContextId("FakeContext");

        when(adminClient.findIngestContractsByID(any()))
                .thenReturn(createIngestContract(ActivationStatus.ACTIVE, MANAGEMENT_CONTRACT_IDENTIFIER));
        when(adminClient.findContextById(any())).thenReturn(ClientMockResultHelper.getContexts(200));
        when(adminClient.findManagementContractsByID(any()))
                .thenThrow(new ReferentialNotFoundException("MC not found"));
        when(handlerIO.getInput(0)).thenReturn(getMandatoryValueMapInstance(true));

        handler = new CheckIngestContractActionHandler(adminManagementClientFactory, storageClientFactory);
        assertEquals(CheckIngestContractActionHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(getWorkerParametersInstance(), handlerIO);
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "MANAGEMENT_CONTRACT_UNKNOWN");
        assertEquals(response.getGlobalStatus(), StatusCode.KO);

    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithManagementContractInactiveThenReturnResponseKO()
            throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setContextId("FakeContext");

        when(adminClient.findIngestContractsByID(any()))
                .thenReturn(createIngestContract(ActivationStatus.ACTIVE, MANAGEMENT_CONTRACT_IDENTIFIER));
        when(adminClient.findContextById(any())).thenReturn(ClientMockResultHelper.getContexts(200));
        when(adminClient.findManagementContractsByID(any()))
                .thenReturn(createManagementContract(ActivationStatus.INACTIVE));
        when(handlerIO.getInput(0)).thenReturn(getMandatoryValueMapInstance(true));

        handler = new CheckIngestContractActionHandler(adminManagementClientFactory, storageClientFactory);
        assertEquals(CheckIngestContractActionHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(getWorkerParametersInstance(), handlerIO);
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "MANAGEMENT_CONTRACT_INACTIVE");
        assertEquals(response.getGlobalStatus(), StatusCode.KO);

    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithManagementContractNotFoundStrategyThenReturnResponseKO()
            throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException,
            StorageServerClientException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setContextId("FakeContext");

        when(adminClient.findIngestContractsByID(any()))
                .thenReturn(createIngestContract(ActivationStatus.ACTIVE, MANAGEMENT_CONTRACT_IDENTIFIER));
        when(adminClient.findContextById(any())).thenReturn(ClientMockResultHelper.getContexts(200));
        when(adminClient.findManagementContractsByID(any()))
                .thenReturn(createManagementContractWithStrategies(ActivationStatus.ACTIVE,"default","default","fake-fake-fake"));
        when(storageClient.getStorageStrategies()).thenReturn(createStorageStrategies());
        when(handlerIO.getInput(0)).thenReturn(getMandatoryValueMapInstance(true));

        handler = new CheckIngestContractActionHandler(adminManagementClientFactory, storageClientFactory);
        assertEquals(CheckIngestContractActionHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(getWorkerParametersInstance(), handlerIO);
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "MANAGEMENT_CONTRACT_INVALID");
        assertEquals(response.getGlobalStatus(), StatusCode.KO);

    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithManagementContractNotReferentUnitStrategyThenReturnResponseKO()
            throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException,
            StorageServerClientException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setContextId("FakeContext");

        when(adminClient.findIngestContractsByID(any()))
                .thenReturn(createIngestContract(ActivationStatus.ACTIVE, MANAGEMENT_CONTRACT_IDENTIFIER));
        when(adminClient.findContextById(any())).thenReturn(ClientMockResultHelper.getContexts(200));
        when(adminClient.findManagementContractsByID(any()))
                .thenReturn(createManagementContractWithStrategies(ActivationStatus.ACTIVE,"withDefault","withoutDefault","withoutDefault"));
        when(storageClient.getStorageStrategies()).thenReturn(createStorageStrategies());
        when(handlerIO.getInput(0)).thenReturn(getMandatoryValueMapInstance(true));

        handler = new CheckIngestContractActionHandler(adminManagementClientFactory, storageClientFactory);
        assertEquals(CheckIngestContractActionHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(getWorkerParametersInstance(), handlerIO);
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "MANAGEMENT_CONTRACT_INVALID");
        assertEquals(response.getGlobalStatus(), StatusCode.KO);

    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWithManagementContractStorageExceptionThenReturnResponseFatal()
            throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException,
            StorageServerClientException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setContextId("FakeContext");

        when(adminClient.findIngestContractsByID(any()))
                .thenReturn(createIngestContract(ActivationStatus.ACTIVE, MANAGEMENT_CONTRACT_IDENTIFIER));
        when(adminClient.findContextById(any())).thenReturn(ClientMockResultHelper.getContexts(200));
        when(adminClient.findManagementContractsByID(any()))
                .thenReturn(createManagementContractWithStrategies(ActivationStatus.ACTIVE,"withDefault","withDefault","withoutDefault"));
        when(storageClient.getStorageStrategies()).thenThrow(new StorageServerClientException("exception fatal"));
        when(handlerIO.getInput(0)).thenReturn(getMandatoryValueMapInstance(true));

        handler = new CheckIngestContractActionHandler(adminManagementClientFactory, storageClientFactory);
        assertEquals(CheckIngestContractActionHandler.getId(), HANDLER_ID);

        ItemStatus response = handler.execute(getWorkerParametersInstance(), handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.FATAL);
    }

    /**
     * Create an instance of IngestContract.
     *
     * @param status
     * @param managementContractId
     * @return the created instance.
     * @throws InvalidParseOperationException
     */
    private static RequestResponse<IngestContractModel> createIngestContract(ActivationStatus status,
            String managementContractId) throws InvalidParseOperationException {
        IngestContractModel contract = new IngestContractModel();
        contract.setIdentifier(CONTRACT_IDENTIFIER);
        contract.setStatus(status);
        contract.setManagementContractId(managementContractId);
        return ClientMockResultHelper.createResponse(contract);
    }

    /**
     * Create an instance of ManagementContract.
     *
     * @param status
     * @param unitStrategy
     * @param gotStrategy
     * @param objectStrategy
     * @return the created instance.
     */
    private static RequestResponse<ManagementContractModel> createManagementContractWithStrategies(
            ActivationStatus status, String unitStrategy, String gotStrategy, String objectStrategy) {
        ManagementContractModel contract = new ManagementContractModel();
        contract.setIdentifier(MANAGEMENT_CONTRACT_IDENTIFIER);
        contract.setStatus(status);
        StorageDetailModel storageDetailModel = new StorageDetailModel();
        storageDetailModel.setUnitStrategy(unitStrategy);
        storageDetailModel.setObjectGroupStrategy(gotStrategy);
        storageDetailModel.setObjectStrategy(objectStrategy);
        contract.setStorage(storageDetailModel);
        return ClientMockResultHelper.createResponse(contract);
    }

    /**
     * Create an instance of ManagementContract.
     *
     * @param status
     * @return the created instance.
     */
    private static RequestResponse<ManagementContractModel> createManagementContract(ActivationStatus status) {
        ManagementContractModel contract = new ManagementContractModel();
        contract.setIdentifier(MANAGEMENT_CONTRACT_IDENTIFIER);
        contract.setStatus(status);
        StorageDetailModel storageDetailModel = new StorageDetailModel();
        storageDetailModel.setUnitStrategy("withDefault");
        storageDetailModel.setObjectGroupStrategy("withDefault");
        storageDetailModel.setObjectStrategy("withoutDefault");
        contract.setStorage(storageDetailModel);
        return ClientMockResultHelper.createResponse(contract);
    }

    /**
     * Create an instance of response for StorageStrategies.
     *
     * @throws InvalidParseOperationException
     */
    private static RequestResponse<StorageStrategy> createStorageStrategies() throws InvalidParseOperationException {
        StorageStrategy defaultStrategy = new StorageStrategy();
        defaultStrategy.setId("default");
        defaultStrategy.setOffers(Arrays.asList(createOfferReference("offerReferent", true),
                createOfferReference("offerNotReferent", false)));
        StorageStrategy withDefaultStrategy = new StorageStrategy();
        withDefaultStrategy.setId("withDefault");
        withDefaultStrategy.setOffers(Arrays.asList(createOfferReference("offerReferent", true),
                createOfferReference("offerNotReferent", false)));
        StorageStrategy withoutDefaultStrategy = new StorageStrategy();
        withoutDefaultStrategy.setId("withoutDefault");
        withoutDefaultStrategy.setOffers(Arrays.asList(createOfferReference("offerNotReferent", false)));

        return ClientMockResultHelper
                .createResponse(Arrays.asList(defaultStrategy, withDefaultStrategy, withoutDefaultStrategy));
    }

    private static OfferReference createOfferReference(String offerReferenceId, boolean isReferent) {
        OfferReference offerReference = new OfferReference(offerReferenceId);
        offerReference.setReferent(isReferent);
        return offerReference;
    }

    /**
     * Create an instance of mandatoryValueMap with/without IngestContract.
     *
     * @param withIngestContract with or withour IngestConstract
     * @return the created instance.
     */
    private Map<String, Object> getMandatoryValueMapInstance(boolean withIngestContract) {
        final Map<String, Object> mandatoryValueMap = new HashMap<>();
        mandatoryValueMap.put(COMMENT, "Dossier d'agent Nicolas Perrin");
        mandatoryValueMap.put(SUBMISSION_AGENCY_IDENTIFIER, "Vitam");
        mandatoryValueMap.put(ORIGINATING_AGENCY_IDENTIFIER, ORIGINATING_AGENCY_IDENTIFIER);
        mandatoryValueMap.put(MESSAGE_IDENTIFIER, "Dossier d'agent Nicolas Perrin");
        if (withIngestContract) {
            mandatoryValueMap.put(ARCHIVAL_AGREEMENT, CONTRACT_NAME);
        }

        return mandatoryValueMap;
    }

    /**
     * Create an instance of WorkerParameters with fake data.
     *
     * @return the created instance.
     */
    private WorkerParameters getWorkerParametersInstance() {
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL)
                .setUrlMetadata(FAKE_URL).setObjectNameList(Lists.newArrayList("objectName.json"))
                .setObjectName("objectName.json").setCurrentStep("STP_INGEST_CONTROL_SIP")
                .setContainerName(guid.getId());

        return params;
    }

}
