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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableMap;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mockito.MapMatcher;
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
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class PurgeDeleteObjectGroupPluginTest {

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
    private PurgeDeleteService purgeDeleteService;

    private PurgeDeleteObjectGroupPlugin instance;

    @Mock
    private HandlerIO handler;

    private WorkerParameters params;

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");

        doReturn(metaDataClient).when(metaDataClientFactory).getClient();

        ArrayNode got1ObjectsDetails = JsonHandler.createArrayNode();
        got1ObjectsDetails.add(
            JsonHandler.createObjectNode().put("id", "id_got1_object_1").put("strategyId", "default-binary-fake")
        );
        got1ObjectsDetails.add(
            JsonHandler.createObjectNode().put("id", "id_got1_object_2").put("strategyId", "default-binary-fake")
        );

        ArrayNode got2ObjectsDetails = JsonHandler.createArrayNode();
        got1ObjectsDetails.add(
            JsonHandler.createObjectNode().put("id", "id_got2_object_1").put("strategyId", "default-binary-fake")
        );

        params = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
            .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
            .setObjectNameList(Arrays.asList("id_got_1", "id_got_2"))
            .setObjectMetadataList(
                Arrays.asList(
                    JsonHandler.createObjectNode()
                        .put("id", "id_got_1")
                        .put("strategyId", "default-fake")
                        .set("objects", got1ObjectsDetails),
                    JsonHandler.createObjectNode()
                        .put("id", "id_got_2")
                        .put("strategyId", "default-fake")
                        .set("objects", got2ObjectsDetails)
                )
            )
            .setCurrentStep("StepName");

        instance = new PurgeDeleteObjectGroupPlugin("PLUGIN_ACTIOB", purgeDeleteService);
    }

    @After
    public void tearDown() throws Exception {}

    @Test
    @RunWithCustomExecutor
    public void testDeleteObjectGroup_OK() throws Exception {
        List<ItemStatus> itemStatuses = instance.executeList(params, handler);

        assertThat(itemStatuses).hasSize(2);
        assertThat(itemStatuses.get(0).getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(itemStatuses.get(1).getGlobalStatus()).isEqualTo(StatusCode.OK);

        Map<String, String> gotIdsWithStrategies = ImmutableMap.of(
            "id_got_1",
            "default-fake",
            "id_got_2",
            "default-fake"
        );
        Map<String, String> objectsIdsWithStrategies = ImmutableMap.of(
            "id_got1_object_1",
            "default-binary-fake",
            "id_got1_object_2",
            "default-binary-fake",
            "id_got2_object_1",
            "default-binary-fake"
        );
        verify(purgeDeleteService).deleteObjectGroups(argThat(new MapMatcher(gotIdsWithStrategies)));
        verify(purgeDeleteService).deleteObjects(argThat(new MapMatcher(objectsIdsWithStrategies)));
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteObjectGroup_ObjectGroupException() throws Exception {
        doThrow(MetaDataClientServerException.class).when(purgeDeleteService).deleteObjectGroups(any());

        List<ItemStatus> itemStatuses = instance.executeList(params, handler);

        assertThat(itemStatuses).hasSize(1);
        assertThat(itemStatuses.get(0).getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }

    @Test
    @RunWithCustomExecutor
    public void testDeleteObjectGroup_ObjectException() throws Exception {
        doThrow(StorageServerClientException.class).when(purgeDeleteService).deleteObjects(any());

        List<ItemStatus> itemStatuses = instance.executeList(params, handler);

        assertThat(itemStatuses).hasSize(1);
        assertThat(itemStatuses.get(0).getGlobalStatus()).isEqualTo(StatusCode.FATAL);
    }
}
