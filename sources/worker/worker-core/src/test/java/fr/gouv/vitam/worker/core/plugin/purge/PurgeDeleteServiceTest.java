/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.worker.core.plugin.purge;

import com.google.common.collect.ImmutableMap;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class PurgeDeleteServiceTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

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
    private PurgeDeleteService instance;

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
        instance.deleteObjects(
            ImmutableMap.of(
                "id1",
                VitamConfiguration.getDefaultStrategy(),
                "id2",
                VitamConfiguration.getDefaultStrategy(),
                "id3",
                VitamConfiguration.getDefaultStrategy()
            )
        );

        verify(storageClient).delete(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECT, "id1");
        verify(storageClient).delete(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECT, "id2");
        verify(storageClient).delete(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECT, "id3");
    }

    @Test
    @RunWithCustomExecutor
    public void deleteObjectGroups() throws Exception {
        Map<String, String> gotIdsWithStrategies = ImmutableMap.of(
            "got1",
            VitamConfiguration.getDefaultStrategy(),
            "got2",
            VitamConfiguration.getDefaultStrategy(),
            "got3",
            VitamConfiguration.getDefaultStrategy()
        );
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
        Map<String, String> unitIdsWithStrategies = ImmutableMap.of(
            "unit1",
            VitamConfiguration.getDefaultStrategy(),
            "unit2",
            VitamConfiguration.getDefaultStrategy(),
            "unit3",
            VitamConfiguration.getDefaultStrategy()
        );
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
        instance.detachObjectGroupFromDeleteParentUnits(gotId, new HashSet<>(Arrays.asList("unit1", "unit2")));

        verify(metaDataClient).updateObjectGroupById(any(), eq(gotId));
    }
}
