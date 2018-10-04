package fr.gouv.vitam.worker.core.plugin.elimination;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class EliminationActionDeleteObjectGroupPluginTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private EliminationActionDeleteService eliminationActionDeleteService;

    @InjectMocks
    private EliminationActionDeleteObjectGroupPlugin instance;

    @Mock
    private HandlerIO handler;

    private WorkerParameters params;

    @Before
    public void setUp() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");

        doReturn(metaDataClient).when(metaDataClientFactory).getClient();

        params = WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
            .newGUID()).setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
            .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
            .setObjectNameList(Arrays.asList("id_got_1", "id_got_2"))
            .setObjectMetadataList(Arrays.asList(
                JsonHandler.toJsonNode(Arrays.asList("id_got1_object_1", "id_got1_object_2")),
                JsonHandler.toJsonNode(Arrays.asList("id_got2_object_1"))))
            .setCurrentStep("StepName");
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteObjectGroup_OK() throws Exception {

        List<ItemStatus> itemStatuses = instance.executeList(params, handler);

        assertThat(itemStatuses).hasSize(2);
        assertThat(itemStatuses.get(0).getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(itemStatuses.get(1).getGlobalStatus()).isEqualTo(StatusCode.OK);

        verify(eliminationActionDeleteService)
            .deleteObjectGroups(eq(new HashSet<>(Arrays.asList("id_got_1", "id_got_2"))));
        verify(eliminationActionDeleteService)
            .deleteObjects(eq(Arrays.asList("id_got1_object_1", "id_got1_object_2", "id_got2_object_1")));

    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteObjectGroup_ObjectGroupException() throws Exception {

        doThrow(MetaDataClientServerException.class).when(eliminationActionDeleteService).deleteObjectGroups(any());

        List<ItemStatus> itemStatuses = instance.executeList(params, handler);

        assertThat(itemStatuses).hasSize(1);
        assertThat(itemStatuses.get(0).getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteObjectGroup_ObjectException() throws Exception {

        doThrow(StorageServerClientException.class).when(eliminationActionDeleteService).deleteObjects(any());

        List<ItemStatus> itemStatuses = instance.executeList(params, handler);

        assertThat(itemStatuses).hasSize(1);
        assertThat(itemStatuses.get(0).getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }
}
