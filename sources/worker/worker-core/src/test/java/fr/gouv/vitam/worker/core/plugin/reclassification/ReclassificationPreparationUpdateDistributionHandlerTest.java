package fr.gouv.vitam.worker.core.plugin.reclassification;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.ReclassificationOrders;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doReturn;

@RunWithCustomExecutor
public class ReclassificationPreparationUpdateDistributionHandlerTest {

    @ClassRule
    public static RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;
    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private HandlerIO handlerIO;
    private Map<String, File> transferredFiles = new HashMap<>();

    private WorkerParameters parameters;

    private ReclassificationPreparationUpdateDistributionHandler reclassificationPreparationLoadHandlerPlugin;

    @Before
    public void init() throws Exception {
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

        doAnswer((args) -> {
            String path = args.getArgument(0);
            InputStream is = args.getArgument(1);
            File file = tempFolder.newFile();
            Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            transferredFiles.put(path, file);
            return null;
        }).when(handlerIO).transferInputStreamToWorkspace(any(), any(), any(), eq(false));

        reclassificationPreparationLoadHandlerPlugin =
            new ReclassificationPreparationUpdateDistributionHandler(metaDataClientFactory);
    }

    @Test
    public void execute_whenExportFailedThenExpectFatal() throws Exception {

        // Given
        HashSetValuedHashMap<String, String> attachments = new HashSetValuedHashMap<>();
        attachments.put("id1", "id2");
        HashSetValuedHashMap<String, String> detachments = new HashSetValuedHashMap<>();
        detachments.put("id1", "id3");
        ReclassificationOrders reclassificationOrders = new ReclassificationOrders(attachments, detachments);
        doReturn(reclassificationOrders).when(handlerIO).getInput(0);

        doThrow(MetaDataExecutionException.class).when(metaDataClient)
            .exportReclassificationChildNodes(any(), any(), any());

        // When
        ItemStatus itemStatus = reclassificationPreparationLoadHandlerPlugin.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }

    @Test
    public void execute_whenExportOKThenExpectOK() throws Exception {

        // Given
        HashSetValuedHashMap<String, String> attachments = new HashSetValuedHashMap<>();
        attachments.putAll("id1", Arrays.asList("id2", "id3"));
        attachments.putAll("id2", Arrays.asList("id3", "id4"));
        HashSetValuedHashMap<String, String> detachments = new HashSetValuedHashMap<>();
        detachments.put("id1", "id5");
        detachments.put("id3", "id5");
        ReclassificationOrders reclassificationOrders = new ReclassificationOrders(attachments, detachments);
        Mockito.doReturn(reclassificationOrders).when(handlerIO).getInput(0);

        // When
        ItemStatus itemStatus = reclassificationPreparationLoadHandlerPlugin.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(transferredFiles).hasSize(4);

        assertThat(JsonHandler
            .getFromFile(transferredFiles.get("UnitsToDetach/id1"), String[].class))
            .containsExactlyInAnyOrder("id5");
        assertThat(JsonHandler
            .getFromFile(transferredFiles.get("UnitsToDetach/id3"), String[].class))
            .containsExactlyInAnyOrder("id5");
        assertThat(JsonHandler
            .getFromFile(transferredFiles.get("UnitsToAttach/id1"), String[].class))
            .containsExactlyInAnyOrder("id2", "id3");
        assertThat(JsonHandler
            .getFromFile(transferredFiles.get("UnitsToAttach/id2"), String[].class))
            .containsExactlyInAnyOrder("id3", "id4");

        verify(metaDataClient)
            .exportReclassificationChildNodes(eq(new HashSet<>(Arrays.asList("id1", "id2", "id3"))), any(), any());
    }
}
