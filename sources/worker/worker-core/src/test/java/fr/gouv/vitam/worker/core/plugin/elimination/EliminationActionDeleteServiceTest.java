package fr.gouv.vitam.worker.core.plugin.elimination;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.ImmutableMap;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;

public class EliminationActionDeleteServiceTest {

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
    private StorageClientFactory storageClientFactory;
    @Mock
    private StorageClient storageClient;

    @Mock
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    @Mock
    private LogbookLifeCyclesClient logbookLifeCyclesClient;

    @InjectMocks
    private EliminationActionDeleteService instance;

    @Before
    public void setUp() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0));

        doReturn(metaDataClient).when(metaDataClientFactory).getClient();
        doReturn(storageClient).when(storageClientFactory).getClient();
        doReturn(logbookLifeCyclesClient).when(logbookLifeCyclesClientFactory).getClient();

    }

    @Test
    @RunWithCustomExecutor
    public void deleteObjects() throws Exception {
        instance.deleteObjects(ImmutableMap.of("id1", VitamConfiguration.getDefaultStrategy(), "id2",
                VitamConfiguration.getDefaultStrategy(), "id3", VitamConfiguration.getDefaultStrategy()));

        verify(storageClient).delete(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECT, "id1");
        verify(storageClient).delete(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECT, "id2");
        verify(storageClient).delete(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECT, "id3");
    }

    @Test
    @RunWithCustomExecutor
    public void deleteObjectGroups() throws Exception {
        Map<String, String> gotIdsWithStrategies = ImmutableMap.of("got1", VitamConfiguration.getDefaultStrategy(),
                "got2", VitamConfiguration.getDefaultStrategy(), "got3", VitamConfiguration.getDefaultStrategy());
        instance.deleteObjectGroups(gotIdsWithStrategies);

        verify(logbookLifeCyclesClient).deleteLifecycleObjectGroupBulk(eq(gotIdsWithStrategies.keySet()));
        verify(metaDataClient).deleteObjectGroupBulk(eq(gotIdsWithStrategies.keySet()));

        verify(storageClient).delete(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECTGROUP, "got1.json");
        verify(storageClient).delete(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECTGROUP, "got2.json");
        verify(storageClient).delete(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECTGROUP, "got3.json");
    }

    @Test
    @RunWithCustomExecutor
    public void deleteUnits() throws Exception {
        Map<String, String> unitIdsWithStrategies = ImmutableMap.of("unit1", VitamConfiguration.getDefaultStrategy(),
                "unit2", VitamConfiguration.getDefaultStrategy(), "unit3", VitamConfiguration.getDefaultStrategy());
        instance.deleteUnits(unitIdsWithStrategies);

        verify(logbookLifeCyclesClient).deleteLifecycleUnitsBulk(eq(unitIdsWithStrategies.keySet()));
        verify(metaDataClient).deleteUnitsBulk(eq(unitIdsWithStrategies.keySet()));

        verify(storageClient).delete(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, "unit1.json");
        verify(storageClient).delete(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, "unit2.json");
        verify(storageClient).delete(VitamConfiguration.getDefaultStrategy(), DataCategory.UNIT, "unit3.json");
    }

    @Test
    @RunWithCustomExecutor
    public void detachObjectGroupFromDeleteParentUnits() throws Exception {

        String opId = GUIDFactory.newGUID().toString();
        String gotId = GUIDFactory.newGUID().toString();
        instance.detachObjectGroupFromDeleteParentUnits(opId, gotId, new HashSet<>(Arrays.asList("unit1", "unit2")),
            "PluginAction");

        verify(metaDataClient).updateObjectGroupById(any(), eq(gotId));
        verify(logbookLifeCyclesClient).update(any(), eq(LifeCycleStatusCode.LIFE_CYCLE_COMMITTED));
    }
}
