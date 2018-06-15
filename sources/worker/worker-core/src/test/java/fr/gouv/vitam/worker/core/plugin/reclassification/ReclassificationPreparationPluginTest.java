package fr.gouv.vitam.worker.core.plugin.reclassification;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.model.ChainedFileModel;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.worker.core.plugin.reclassification.ReclassificationPreparationPlugin.ACCESS_CONTRACT_NOT_FOUND_OR_NOT_ACTIVE;
import static fr.gouv.vitam.worker.core.plugin.reclassification.ReclassificationPreparationPlugin.ACCESS_DENIED_OR_MISSING_UNITS;
import static fr.gouv.vitam.worker.core.plugin.reclassification.ReclassificationPreparationPlugin.CONCURRENT_RECLASSIFICATION_PROCESS;
import static fr.gouv.vitam.worker.core.plugin.reclassification.ReclassificationPreparationPlugin.INVALID_JSON;
import static fr.gouv.vitam.worker.core.plugin.reclassification.ReclassificationPreparationPlugin.NO_ACCESS_CONTRACT_PROVIDED;
import static fr.gouv.vitam.worker.core.plugin.reclassification.ReclassificationPreparationPlugin.RECLASSIFICATION_WORKFLOW_IDENTIFIER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

@RunWithCustomExecutor
public class ReclassificationPreparationPluginTest {

    @ClassRule
    public static RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private AdminManagementClientFactory adminManagementClientFactory;
    @Mock
    private AdminManagementClient adminManagementClient;

    @Mock
    private MetaDataClientFactory metaDataClientFactory;
    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private LightweightWorkflowLock lightweightWorkflowLock;

    @Mock
    private UnitGraphInfoLoader unitGraphInfoLoader;

    @Mock
    private HandlerIO handlerIO;
    private Map<String, File> transferedFiles = new HashMap<>();

    private WorkerParameters parameters;

    @Before
    public void init() throws Exception {
        doReturn(adminManagementClient).when(adminManagementClientFactory).getClient();
        doReturn(metaDataClient).when(metaDataClientFactory).getClient();

        int tenant = 0;
        VitamThreadUtils.getVitamSession().setTenantId(tenant);
        String operationId = GUIDFactory.newRequestIdGUID(tenant).toString();
        VitamThreadUtils.getVitamSession().setRequestId(operationId);

        String objectId = GUIDFactory.newGUID().toString();
        parameters = WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
            .newGUID()).setContainerName(operationId)
            .setObjectNameList(Lists.newArrayList(objectId))
            .setObjectName(objectId).setCurrentStep("StepName");

        doAnswer((args) -> tempFolder.newFile()).when(handlerIO).getNewLocalFile(anyString());

        doAnswer((args) -> {
            String path = args.getArgumentAt(0, String.class);
            File file = args.getArgumentAt(1, File.class);
            transferedFiles.put(path, file);
            return null;
        }).when(handlerIO).transferFileToWorkspace(anyString(), any(), eq(true), eq(false));

        doAnswer((args) -> {
            String path = args.getArgumentAt(0, String.class);
            InputStream is = args.getArgumentAt(1, InputStream.class);
            File file = tempFolder.newFile();
            Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            transferedFiles.put(path, file);
            return null;
        }).when(handlerIO).transferInputStreamToWorkspace(anyString(), any(), any(null), eq(false));
    }

    @Test
    public void execute_GivenConcurrentProcessFoundThenExpectKO() throws Exception {

        ReclassificationPreparationPlugin reclassificationPreparationPlugin =
            new ReclassificationPreparationPlugin(adminManagementClientFactory, metaDataClientFactory,
                unitGraphInfoLoader, lightweightWorkflowLock, 1000, 1000, 1000, 1000);

        ProcessDetail concurrentProcessDetail = new ProcessDetail();
        concurrentProcessDetail.setOperationId("SomeOtherProcessId");
        concurrentProcessDetail.setGlobalState("PAUSE");
        concurrentProcessDetail.setStepStatus("FATAL");

        doReturn(Collections.singletonList(concurrentProcessDetail)).when(lightweightWorkflowLock)
            .listConcurrentReclassificationWorkflows(RECLASSIFICATION_WORKFLOW_IDENTIFIER,
                VitamThreadUtils.getVitamSession().getRequestId());

        // When
        ItemStatus itemStatus = reclassificationPreparationPlugin.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(
            JsonHandler.getFromString(itemStatus.getEvDetailData(), ReclassificationEventDetails.class).getError())
            .isEqualTo(CONCURRENT_RECLASSIFICATION_PROCESS);
    }

    @Test
    public void execute_GivenInvalidJsonThenExpectKO() throws Exception {

        ReclassificationPreparationPlugin reclassificationPreparationPlugin =
            new ReclassificationPreparationPlugin(adminManagementClientFactory, metaDataClientFactory,
                unitGraphInfoLoader, lightweightWorkflowLock, 1000, 1000, 1000, 1000);

        givenNoConcurrentWorkflow();
        givenJsonRequest("Reclassification/bad_request.json");

        // When
        ItemStatus itemStatus = reclassificationPreparationPlugin.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(
            JsonHandler.getFromString(itemStatus.getEvDetailData(), ReclassificationEventDetails.class).getError())
            .isEqualTo(INVALID_JSON);
    }

    @Test
    public void execute_GivenNoAccessContractProvidedThenKO() throws Exception {

        ReclassificationPreparationPlugin reclassificationPreparationPlugin =
            new ReclassificationPreparationPlugin(adminManagementClientFactory, metaDataClientFactory,
                unitGraphInfoLoader, lightweightWorkflowLock, 1000, 1000, 1000, 1000);

        givenNoConcurrentWorkflow();
        givenJsonRequest("Reclassification/reclassification.json");

        VitamThreadUtils.getVitamSession().setContractId(null);

        // When
        ItemStatus itemStatus = reclassificationPreparationPlugin.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(
            JsonHandler.getFromString(itemStatus.getEvDetailData(), ReclassificationEventDetails.class).getError())
            .isEqualTo(NO_ACCESS_CONTRACT_PROVIDED);
    }

    @Test
    public void execute_GivenAccessContractNotFoundThenKO() throws Exception {

        ReclassificationPreparationPlugin reclassificationPreparationPlugin =
            new ReclassificationPreparationPlugin(adminManagementClientFactory, metaDataClientFactory,
                unitGraphInfoLoader, lightweightWorkflowLock, 1000, 1000, 1000, 1000);

        givenNoConcurrentWorkflow();
        givenJsonRequest("Reclassification/reclassification.json");

        String accessContractId = "ContractId";
        VitamThreadUtils.getVitamSession().setContractId(accessContractId);
        RequestResponseOK<Object> emptyResponse = new RequestResponseOK<>();
        doReturn(emptyResponse).when(adminManagementClient).findAccessContracts(any());

        // When
        ItemStatus itemStatus = reclassificationPreparationPlugin.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(
            JsonHandler.getFromString(itemStatus.getEvDetailData(), ReclassificationEventDetails.class).getError())
            .isEqualTo(ACCESS_CONTRACT_NOT_FOUND_OR_NOT_ACTIVE);
    }

    @Test
    public void execute_GivenDocumentIdsNotFoundViaAccessContractThenExpectKO() throws Exception {

        ReclassificationPreparationPlugin reclassificationPreparationPlugin =
            new ReclassificationPreparationPlugin(adminManagementClientFactory, metaDataClientFactory,
                unitGraphInfoLoader, lightweightWorkflowLock, 1000, 1000, 1000, 1000);

        givenNoConcurrentWorkflow();
        givenJsonRequest("Reclassification/reclassification.json");
        givenExistingAccessContract();
        givenAccessibleUnitIds("aeaqaaaaaaesfqasaaokqald5n655vaaaaaq");

        // When
        ItemStatus itemStatus = reclassificationPreparationPlugin.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        ReclassificationEventDetails eventDetails = JsonHandler.getFromString(
            itemStatus.getEvDetailData(), ReclassificationEventDetails.class);
        assertThat(eventDetails.getError()).isEqualTo(ACCESS_DENIED_OR_MISSING_UNITS);
        assertThat(eventDetails.getMissingOrForbiddenUnits())
            .containsExactlyInAnyOrder("aeaqaaaaaaesfqasaaokqald5n7xoayaaabq",
                "aeaqaaaaaaesfqasaaokqald5n7xociaaaaq", "aeaqaaaaaaesfqasaaokqald5n7xocyaaaaq");
    }

    @Test
    public void execute_testOK() throws Exception {

        /*
         * Test case :
         * ===========
         *   Unit 1 : aeaqaaaaaaesfqasaaokqald5n655iiaaabq (no got)
         *    +->  Unit 2 : aeaqaaaaaaesfqasaaokqald5n655uqaaaaq With Got aebaaaaaaaesfqasaaokqald5n655gyaaaaq
         *    +->  Unit 3 : aeaqaaaaaaesfqasaaokqald5n655vaaaaaq With Got aebaaaaaaaesfqasaaokqald5n655hqaaaaq
         *   Unit 4 : aeaqaaaaaaesfqasaaokqald5n7xoayaaabq  (no got)
         *    +->  Unit 5: aeaqaaaaaaesfqasaaokqald5n7xociaaaaq With Got aebaaaaaaaesfqasaaokqald5n7xoaaaaaaq
         *    +->  Unit 6 : aeaqaaaaaaesfqasaaokqald5n7xocyaaaaq With Got aebaaaaaaaesfqasaaokqald5n7xoaqaaaaq
         *
         * Actions :
         * =========
         * - Attach 5 -> 3
         * - Detach 6 -> 4
         */

        ReclassificationPreparationPlugin reclassificationPreparationPlugin =
            new ReclassificationPreparationPlugin(adminManagementClientFactory, metaDataClientFactory,
                unitGraphInfoLoader, lightweightWorkflowLock, 1000, 1000, 1000, 1000);

        givenNoConcurrentWorkflow();
        givenJsonRequest("Reclassification/reclassification.json");
        givenExistingAccessContract();
        givenAccessibleUnitIds("aeaqaaaaaaesfqasaaokqald5n655vaaaaaq", "aeaqaaaaaaesfqasaaokqald5n7xoayaaabq",
            "aeaqaaaaaaesfqasaaokqald5n7xociaaaaq", "aeaqaaaaaaesfqasaaokqald5n7xocyaaaaq");
        givenDocuments("Reclassification/units.json");
        givenUnitsToUpdate("Reclassification/childUnits.json");

        // When
        ItemStatus itemStatus = reclassificationPreparationPlugin.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(transferedFiles).hasSize(4);

        assertThat(JsonHandler
            .getFromFile(transferedFiles.get("UnitsToDetach/aeaqaaaaaaesfqasaaokqald5n7xocyaaaaq"), String[].class))
            .containsExactlyInAnyOrder("aeaqaaaaaaesfqasaaokqald5n7xoayaaabq");
        assertThat(JsonHandler
            .getFromFile(transferedFiles.get("UnitsToAttach/aeaqaaaaaaesfqasaaokqald5n7xociaaaaq"), String[].class))
            .containsExactlyInAnyOrder("aeaqaaaaaaesfqasaaokqald5n655vaaaaaq");

        ChainedFileModel gotChainedFile =
            JsonHandler
                .getFromFile(transferedFiles.get("ObjectGroupsToUpdate/chainedFile.json"), ChainedFileModel.class);
        assertThat(gotChainedFile.getElements()).containsExactlyInAnyOrder("aebaaaaaaaesfqasaaokqald5n655hqaaaaq",
            "aebaaaaaaaesfqasaaokqald5n7xoaaaaaaq", "aebaaaaaaaesfqasaaokqald5n7xoaqaaaaq");

        ChainedFileModel unitChainedFile =
            JsonHandler.getFromFile(transferedFiles.get("UnitsToUpdate/chainedFile.json"), ChainedFileModel.class);
        assertThat(unitChainedFile.getElements())
            .containsExactlyInAnyOrder("aeaqaaaaaaesfqasaaokqald5n655vaaaaaq", "aeaqaaaaaaesfqasaaokqald5n7xocyaaaaq",
                "aeaqaaaaaaesfqasaaokqald5n7xociaaaaq", "aeaqaaaaaaesfqasaaokqald5n7xoayaaabq");
    }

    private void givenAccessibleUnitIds(String... ids)
        throws InvalidParseOperationException, InvalidCreateOperationException, VitamDBException,
        MetaDataDocumentSizeException, MetaDataExecutionException, MetaDataClientServerException {
        doReturn(new HashSet<>(Arrays.asList(ids)))
            .when(unitGraphInfoLoader).selectUnitsByIdsAndAccessContract(any(), any(), any());
    }

    private void givenNoConcurrentWorkflow() throws ReclassificationException, VitamClientException {
        doReturn(Collections.EMPTY_LIST).when(lightweightWorkflowLock)
            .listConcurrentReclassificationWorkflows(any(), any());
    }

    private void givenJsonRequest(String jsonRequestFile)
        throws IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        doReturn(PropertiesUtils.getResourceAsStream(jsonRequestFile))
            .when(handlerIO).getInputStreamFromWorkspace("request.json");
    }

    private void givenExistingAccessContract()
        throws InvalidParseOperationException, AdminManagementClientServerException {
        String accessContractId = "ContractId";
        VitamThreadUtils.getVitamSession().setContractId(accessContractId);
        AccessContractModel accessContractModel = new AccessContractModel();
        accessContractModel.setEveryOriginatingAgency(true);
        doReturn(new RequestResponseOK<>().addResult(accessContractModel)).when(adminManagementClient)
            .findAccessContracts(any());
    }

    @SuppressWarnings("unchecked")
    private void givenDocuments(String unitsFileName)
        throws VitamDBException, InvalidParseOperationException, MetaDataExecutionException,
        MetaDataDocumentSizeException, MetaDataClientServerException, InvalidCreateOperationException,
        FileNotFoundException {

        JsonNode jsonUnits = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(unitsFileName));
        UnitGraphInfo[] unitGraphInfo = JsonHandler.getFromJsonNode(jsonUnits, UnitGraphInfo[].class);

        doAnswer((args) -> {

            Set<String> ids = (Set<String>) args.getArgumentAt(1, Set.class);

            return Arrays.stream(unitGraphInfo)
                .filter(unit -> ids.contains(unit.getId()))
                .collect(Collectors.toMap(
                    UnitGraphInfo::getId,
                    unit -> unit
                ));

        }).when(unitGraphInfoLoader).selectAllUnitGraphByIds(any(), any());
    }

    private void givenUnitsToUpdate(String unitsFileName)
        throws InvalidParseOperationException, InvalidCreateOperationException, FileNotFoundException {

        JsonNode jsonUnits = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(unitsFileName));
        List<JsonNode> entries = new ArrayList<>();
        for (JsonNode jsonUnit : jsonUnits) {
            entries.add(jsonUnit);
        }
        doReturn(entries.iterator()).when(unitGraphInfoLoader).selectAllUnitsToUpdate(any(), any());
    }
}
