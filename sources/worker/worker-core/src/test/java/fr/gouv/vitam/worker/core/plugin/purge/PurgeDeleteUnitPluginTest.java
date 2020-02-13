/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import fr.gouv.vitam.batch.report.model.entry.PurgeUnitReportEntry;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PurgeDeleteUnitPluginTest {

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
    private PurgeDeleteService purgeDeleteService;

    @Mock
    private PurgeReportService purgeReportService;

    @Mock
    private LogbookLifeCyclesClientFactory lfcClientFactory;

    private ArrayList<PurgeUnitReportEntry> reportEntries;

    private PurgeUnitPlugin instance;

    @Mock
    private HandlerIO handler;

    private WorkerParameters params;

    @Before
    public void setUp() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");

        doReturn(metaDataClient).when(metaDataClientFactory).getClient();

        params = WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
            .newGUID().getId()).setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
            .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
            .setProcessId(VitamThreadUtils.getVitamSession().getRequestId())
            .setObjectNameList(Arrays.asList("id_unit_1", "id_unit_2", "id_unit_3", "id_unit_4", "id_unit_5"))
            .setObjectMetadataList(Arrays.asList(
                buildUnit(1), buildUnit(2), buildUnit(3), buildUnit(4), buildUnit(5)))
            .setCurrentStep("StepName");

        reportEntries = new ArrayList<>();
        doAnswer((args) -> reportEntries.addAll(args.getArgument(1)))
            .when(purgeReportService)
            .appendUnitEntries(any(), any());

        instance = new PurgeUnitPlugin("PLUGIN_ACTION", purgeDeleteService, metaDataClientFactory, purgeReportService, lfcClientFactory);
    }

    @After
    public void tearDown() {
    }

    @Test
    @RunWithCustomExecutor
    public void testExecuteList_OK() throws Exception {

        Map<String, String> unitIdsWithStrategies = ImmutableMap.of("id_unit_2", "default-fake",
            "id_unit_3", "default-fake",
            "id_unit_5", "default-fake");

        /* id_unit_1 has too many children */
        JsonNode childUnitsForUnit1 = buildChildUnitsResponse("id_unit_1", VitamConfiguration.getBatchSize());
        /* id_unit_4 has a single child. Other units do not have any children */
        JsonNode childUnitsForUnit4 = buildChildUnitsResponse("id_unit_4", 1);
        when(metaDataClient.selectUnits(any())).thenReturn(childUnitsForUnit1, childUnitsForUnit4);

        List<ItemStatus> itemStatus = instance.executeList(params, handler);

        assertThat(itemStatus).hasSize(5);
        assertThat(itemStatus.stream().filter(i -> i.getGlobalStatus() == StatusCode.OK)).hasSize(3);
        assertThat(itemStatus.stream().filter(i -> i.getGlobalStatus() == StatusCode.WARNING)).hasSize(2);

        assertThat(reportEntries).hasSize(5);
        assertThat(reportEntries.stream().filter(e -> e.getId().equals("id_unit_1")).findFirst().get()
            .getStatus()).isEqualTo(PurgeUnitStatus.NON_DESTROYABLE_HAS_CHILD_UNITS.name());
        assertThat(reportEntries.stream().filter(e -> e.getId().equals("id_unit_2")).findFirst().get()
            .getStatus()).isEqualTo(PurgeUnitStatus.DELETED.name());
        assertThat(reportEntries.stream().filter(e -> e.getId().equals("id_unit_3")).findFirst().get()
            .getStatus()).isEqualTo(PurgeUnitStatus.DELETED.name());
        assertThat(reportEntries.stream().filter(e -> e.getId().equals("id_unit_4")).findFirst().get()
            .getStatus()).isEqualTo(PurgeUnitStatus.NON_DESTROYABLE_HAS_CHILD_UNITS.name());
        assertThat(reportEntries.stream().filter(e -> e.getId().equals("id_unit_5")).findFirst().get()
            .getStatus()).isEqualTo(PurgeUnitStatus.DELETED.name());

        verify(purgeDeleteService).deleteUnits(eq(unitIdsWithStrategies));
    }

    private JsonNode buildUnit(Integer index) {
        ObjectNode storage = JsonHandler.createObjectNode();
        storage.put("strategyId", "default-fake");
        return JsonHandler.createObjectNode()
            .put(VitamFieldsHelper.id(), "id_unit_" + index)
            .put(VitamFieldsHelper.object(), "id_got_" + index)
            .put(VitamFieldsHelper.originatingAgency(), "sp_" + index)
            .put(VitamFieldsHelper.initialOperation(), "opi_" + index)
            .set(VitamFieldsHelper.storage(), storage);
    }

    private JsonNode buildChildUnitsResponse(String unitId, int count) {
        RequestResponseOK<JsonNode> childUnits = new RequestResponseOK<>();
        for (int i = 0; i < count; i++) {
            JsonNode childUnit = JsonHandler.createObjectNode()
                .set(VitamFieldsHelper.unitups(), JsonHandler.createArrayNode().add(unitId));
            childUnits.addResult(childUnit);
        }
        return childUnits.toJsonNode();
    }

}
