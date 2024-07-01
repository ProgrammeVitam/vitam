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
package fr.gouv.vitam.worker.core.plugin.reclassification;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UnitType;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.ReclassificationEventDetails;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.ReclassificationOrders;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.UnitGraphInfo;
import fr.gouv.vitam.worker.core.plugin.reclassification.utils.UnitGraphInfoLoader;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static fr.gouv.vitam.worker.core.plugin.reclassification.ReclassificationPreparationCheckGraphHandler.CANNOT_APPLY_RECLASSIFICATION_REQUEST_CYCLE_DETECTED;
import static fr.gouv.vitam.worker.core.plugin.reclassification.ReclassificationPreparationCheckGraphHandler.COULD_NOT_LOAD_UNITS;
import static fr.gouv.vitam.worker.core.plugin.reclassification.ReclassificationPreparationCheckGraphHandler.INVALID_UNIT_TYPE_ATTACHMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@RunWithCustomExecutor
public class ReclassificationPreparationCheckGraphHandlerTest {

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private UnitGraphInfoLoader unitGraphInfoLoader;

    @Mock
    private HandlerIO handlerIO;

    private WorkerParameters parameters;

    ReclassificationPreparationCheckGraphHandler reclassificationPreparationCheckGraphHandler;

    @Before
    public void init() throws Exception {
        doReturn(metaDataClient).when(metaDataClientFactory).getClient();

        int tenant = 0;
        VitamThreadUtils.getVitamSession().setTenantId(tenant);
        String operationId = GUIDFactory.newRequestIdGUID(tenant).toString();
        VitamThreadUtils.getVitamSession().setRequestId(operationId);

        String objectId = GUIDFactory.newGUID().toString();
        parameters = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(operationId)
            .setObjectNameList(Lists.newArrayList(objectId))
            .setObjectName(objectId)
            .setCurrentStep("StepName");

        reclassificationPreparationCheckGraphHandler = new ReclassificationPreparationCheckGraphHandler(
            metaDataClientFactory,
            unitGraphInfoLoader,
            1000
        );
    }

    @Test
    public void execute_GivenNotFoundUnitsThenExpectFatal() throws Exception {
        // Given
        HashSetValuedHashMap<String, String> attachments = new HashSetValuedHashMap<>();
        attachments.put("id1", "id2");
        HashSetValuedHashMap<String, String> detachments = new HashSetValuedHashMap<>();
        detachments.put("id1", "id3");
        ReclassificationOrders reclassificationOrders = new ReclassificationOrders(attachments, detachments);
        doReturn(reclassificationOrders).when(handlerIO).getInput(0);

        Map<String, UnitGraphInfo> unitGraphInfoMap = new HashMap<>();
        unitGraphInfoMap.put("id1", null);
        addUnitGraph(unitGraphInfoMap, "id2", UnitType.INGEST, "id4");
        unitGraphInfoMap.put("id3", null);
        addUnitGraph(unitGraphInfoMap, "id4", UnitType.INGEST, "id5");
        unitGraphInfoMap.put("id5", null);
        doReturn(unitGraphInfoMap).when(unitGraphInfoLoader).selectAllUnitGraphByIds(eq(metaDataClient), any());

        // When
        ItemStatus itemStatus = reclassificationPreparationCheckGraphHandler.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
        ReclassificationEventDetails eventDetails = JsonHandler.getFromString(
            itemStatus.getEvDetailData(),
            ReclassificationEventDetails.class
        );
        assertThat(eventDetails.getError()).isEqualTo(COULD_NOT_LOAD_UNITS);
        assertThat(eventDetails.getNotFoundUnits()).containsExactlyInAnyOrder("id1", "id3", "id5");
    }

    @Test
    public void execute_GivenIllegalUnitTypeAttachmentThenExpectKO() throws Exception {
        // Given
        HashSetValuedHashMap<String, String> attachments = new HashSetValuedHashMap<>();
        attachments.put("id1", "id2");
        HashSetValuedHashMap<String, String> detachments = new HashSetValuedHashMap<>();
        detachments.put("id1", "id3");
        ReclassificationOrders reclassificationOrders = new ReclassificationOrders(attachments, detachments);
        doReturn(reclassificationOrders).when(handlerIO).getInput(0);

        Map<String, UnitGraphInfo> unitGraphInfoMap = new HashMap<>();
        addUnitGraph(unitGraphInfoMap, "id1", UnitType.FILING_UNIT);
        addUnitGraph(unitGraphInfoMap, "id2", UnitType.INGEST);
        addUnitGraph(unitGraphInfoMap, "id3", UnitType.INGEST);

        doReturn(unitGraphInfoMap).when(unitGraphInfoLoader).selectAllUnitGraphByIds(eq(metaDataClient), any());

        // When
        ItemStatus itemStatus = reclassificationPreparationCheckGraphHandler.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(
            JsonHandler.getFromString(itemStatus.getEvDetailData(), ReclassificationEventDetails.class).getError()
        ).isEqualTo(INVALID_UNIT_TYPE_ATTACHMENTS);
    }

    @Test
    public void execute_GraphCycleThenExpectKO() throws Exception {
        // Given : Reclassification with cycle in target graph
        /*
         *     1                  1
         *     ↑                  ↑
         *     2                  2
         *      ↖       ==>      ↗ ↘
         *   3 ← 4              3 ← 4
         *   ↑                  ↑
         *   5                  5
         */

        HashSetValuedHashMap<String, String> attachments = new HashSetValuedHashMap<>();
        attachments.put("id2", "id4");
        attachments.put("id3", "id2");
        HashSetValuedHashMap<String, String> detachments = new HashSetValuedHashMap<>();
        detachments.put("id4", "id2");
        ReclassificationOrders reclassificationOrders = new ReclassificationOrders(attachments, detachments);
        doReturn(reclassificationOrders).when(handlerIO).getInput(0);

        Map<String, UnitGraphInfo> unitGraphInfoMap = new HashMap<>();
        addUnitGraph(unitGraphInfoMap, "id1", UnitType.INGEST);
        addUnitGraph(unitGraphInfoMap, "id2", UnitType.INGEST, "id1");
        addUnitGraph(unitGraphInfoMap, "id3", UnitType.INGEST);
        addUnitGraph(unitGraphInfoMap, "id4", UnitType.INGEST, "id2", "id3");
        addUnitGraph(unitGraphInfoMap, "id5", UnitType.INGEST, "id3");

        doReturn(unitGraphInfoMap).when(unitGraphInfoLoader).selectAllUnitGraphByIds(eq(metaDataClient), any());

        // When
        ItemStatus itemStatus = reclassificationPreparationCheckGraphHandler.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
        ReclassificationEventDetails eventDetails = JsonHandler.getFromString(
            itemStatus.getEvDetailData(),
            ReclassificationEventDetails.class
        );
        assertThat(eventDetails.getError()).isEqualTo(CANNOT_APPLY_RECLASSIFICATION_REQUEST_CYCLE_DETECTED);
        assertThat(eventDetails.getUnitsWithCycles()).containsExactlyInAnyOrder("id2", "id3", "id4");
    }

    @Test
    public void execute_validGraphThenExpectOK() throws Exception {
        // Given : basic attachment & detachment without cycles
        /*
         *     1  2               1   3
         *     ↑                    ↗
         *     3                  2
         */

        HashSetValuedHashMap<String, String> attachments = new HashSetValuedHashMap<>();
        attachments.put("id3", "id2");
        HashSetValuedHashMap<String, String> detachments = new HashSetValuedHashMap<>();
        detachments.put("id3", "id1");
        ReclassificationOrders reclassificationOrders = new ReclassificationOrders(attachments, detachments);
        doReturn(reclassificationOrders).when(handlerIO).getInput(0);

        Map<String, UnitGraphInfo> unitGraphInfoMap = new HashMap<>();
        addUnitGraph(unitGraphInfoMap, "id1", UnitType.INGEST);
        addUnitGraph(unitGraphInfoMap, "id2", UnitType.INGEST);
        addUnitGraph(unitGraphInfoMap, "id3", UnitType.INGEST, "id1");

        doReturn(unitGraphInfoMap).when(unitGraphInfoLoader).selectAllUnitGraphByIds(eq(metaDataClient), any());

        // When
        ItemStatus itemStatus = reclassificationPreparationCheckGraphHandler.execute(parameters, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    private void addUnitGraph(
        Map<String, UnitGraphInfo> unitGraphInfoMap,
        String unitId,
        UnitType unitType,
        String... up
    ) {
        UnitGraphInfo unit = new UnitGraphInfo();
        unit.setId(unitId);
        unit.setUnitType(unitType);
        unit.setUp(new HashSet<>(Arrays.asList(up)));
        unitGraphInfoMap.put(unitId, unit);
    }
}
