package fr.gouv.vitam.worker.core.plugin.elimination;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.model.entry.EliminationActionUnitReportEntry;
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
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationActionUnitStatus;
import fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionReportService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EliminationActionDeleteUnitPluginTest {

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

    @Mock
    private EliminationActionReportService eliminationActionReportService;
    private ArrayList<EliminationActionUnitReportEntry> reportEntries;

    @InjectMocks
    private EliminationActionDeleteUnitPlugin instance;

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
            .setProcessId(VitamThreadUtils.getVitamSession().getRequestId())
            .setObjectNameList(Arrays.asList("id_unit_1", "id_unit_2", "id_unit_3", "id_unit_4", "id_unit_5"))
            .setCurrentStep("StepName");

        reportEntries = new ArrayList<>();
        doAnswer((args) -> reportEntries.addAll(args.getArgument(1)))
            .when(eliminationActionReportService)
            .appendUnitEntries(any(), any());

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    @RunWithCustomExecutor
    public void testExecuteList_OK() throws Exception {

        /* id_unit_3 not found */
        JsonNode units = buildUnits(Arrays.asList(1, 2, 4, 5));
        /* id_unit_1 has too many children */
        JsonNode childUnitsForUnit1 = buildChildUnitsResponse("id_unit_1", VitamConfiguration.getBatchSize());
        /* id_unit_4 has a single child. Other units do not have any children */
        JsonNode childUnitsForUnit4 = buildChildUnitsResponse("id_unit_4", 1);
        when(metaDataClient.selectUnits(any())).thenReturn(units, childUnitsForUnit1, childUnitsForUnit4);

        List<ItemStatus> itemStatus = instance.executeList(params, handler);

        assertThat(itemStatus).hasSize(5);
        assertThat(itemStatus.stream().filter(i -> i.getGlobalStatus() == StatusCode.OK)).hasSize(3);
        assertThat(itemStatus.stream().filter(i -> i.getGlobalStatus() == StatusCode.WARNING)).hasSize(2);

        assertThat(reportEntries).hasSize(4);
        assertThat(reportEntries.stream().filter(e -> e.getUnitId().equals("id_unit_1")).findFirst().get()
            .getStatus()).isEqualTo(EliminationActionUnitStatus.NON_DESTROYABLE_HAS_CHILD_UNITS.name());
        assertThat(reportEntries.stream().filter(e -> e.getUnitId().equals("id_unit_2")).findFirst().get()
            .getStatus()).isEqualTo(EliminationActionUnitStatus.DELETED.name());
        assertThat(reportEntries.stream().filter(e -> e.getUnitId().equals("id_unit_4")).findFirst().get()
            .getStatus()).isEqualTo(EliminationActionUnitStatus.NON_DESTROYABLE_HAS_CHILD_UNITS.name());
        assertThat(reportEntries.stream().filter(e -> e.getUnitId().equals("id_unit_5")).findFirst().get()
            .getStatus()).isEqualTo(EliminationActionUnitStatus.DELETED.name());

        verify(eliminationActionDeleteService).deleteUnits(eq(new HashSet<>(Arrays.asList("id_unit_2", "id_unit_5"))));
    }

    private JsonNode buildUnits(List<Integer> indexes) {
        RequestResponseOK<JsonNode> units = new RequestResponseOK<>();
        for (Integer i : indexes) {
            JsonNode unit = JsonHandler.createObjectNode()
                .put(VitamFieldsHelper.id(), "id_unit_" + i)
                .put(VitamFieldsHelper.object(), "id_got_" + i)
                .put(VitamFieldsHelper.originatingAgency(), "sp_" + i)
                .put(VitamFieldsHelper.initialOperation(), "opi_" + i);
            units.addResult(unit);
        }
        return units.toJsonNode();
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
