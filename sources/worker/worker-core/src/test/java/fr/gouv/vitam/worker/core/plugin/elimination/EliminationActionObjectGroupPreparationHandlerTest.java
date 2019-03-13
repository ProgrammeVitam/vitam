package fr.gouv.vitam.worker.core.plugin.elimination;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.model.entry.EliminationActionObjectGroupReportEntry;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.collection.CloseableIteratorUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
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
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationActionObjectGroupStatus;
import fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionReportService;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static fr.gouv.vitam.worker.core.plugin.elimination.EliminationActionObjectGroupPreparationHandler.OBJECT_GROUPS_TO_DELETE_FILE;
import static fr.gouv.vitam.worker.core.plugin.elimination.EliminationActionObjectGroupPreparationHandler.OBJECT_GROUPS_TO_DETACH_FILE;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class EliminationActionObjectGroupPreparationHandlerTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private EliminationActionReportService eliminationActionReportService;

    private ArrayList<EliminationActionObjectGroupReportEntry> reportEntries;

    @Mock
    private HandlerIO handler;

    private WorkerParameters params;

    @Before
    public void setUp() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");

        doAnswer(args -> tempFolder.newFile(args.getArgument(0))).when(handler).getNewLocalFile(any());

        doReturn(metaDataClient).when(metaDataClientFactory).getClient();

        params = WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
            .newGUID()).setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
            .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
            .setProcessId(VitamThreadUtils.getVitamSession().getRequestId())
            .setObjectName("REF")
            .setCurrentStep("StepName");

        doReturn(VitamThreadUtils.getVitamSession().getRequestId())
            .when(handler).getContainerName();

        reportEntries = new ArrayList<>();
        doAnswer((args) -> reportEntries.addAll(args.getArgument(1)))
            .when(eliminationActionReportService)
            .appendObjectGroupEntries(any(), any());
    }

    @Test
    @RunWithCustomExecutor
    public void testExecute_OK() throws Exception {

        doReturn(CloseableIteratorUtils
            .toCloseableIterator(asList("id_got_1", "id_got_2", "id_got_3", "id_got_4", "id_got_5").iterator()))
            .when(eliminationActionReportService).exportDistinctObjectGroups(any());

        JsonNode objectGroups = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(
            "EliminationAction/EliminationActionObjectGroupPreparationHandler/objectGroups.json"));
        doReturn(objectGroups).when(metaDataClient).selectObjectGroups(any());

        JsonNode existingUnits = JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(
            "EliminationAction/EliminationActionObjectGroupPreparationHandler/existingUnits.json"));
        doReturn(existingUnits).when(metaDataClient).selectUnits(any());

        EliminationActionObjectGroupPreparationHandler instance = new EliminationActionObjectGroupPreparationHandler(
            metaDataClientFactory, eliminationActionReportService, 10);
        ItemStatus itemStatus = instance.execute(params, handler);

        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        assertThat(reportEntries).hasSize(4);
        checkReportEntry(reportEntries, "id_got_1", "sp_1", "opi1",
            Arrays.asList("id_got_1_object_1", "id_got_1_object_2"), null,
            EliminationActionObjectGroupStatus.DELETED);
        checkReportEntry(reportEntries, "id_got_2", "sp_2", "opi2", null, Arrays.asList("id_unit_20"),
            EliminationActionObjectGroupStatus.PARTIAL_DETACHMENT);
        checkReportEntry(reportEntries, "id_got_3", "sp_3", "opi3",
            Arrays.asList("id_got_3_object_1"), null,
            EliminationActionObjectGroupStatus.DELETED);
        checkReportEntry(reportEntries, "id_got_5", "sp_5", "opi5", null, Arrays.asList("id_unit_50", "id_unit_51"),
            EliminationActionObjectGroupStatus.PARTIAL_DETACHMENT);

        ArgumentCaptor<File> objectGroupsToDeleteFileArgCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<File> objectGroupsToDetachFileArgCaptor = ArgumentCaptor.forClass(File.class);
        verify(handler).transferFileToWorkspace(eq(OBJECT_GROUPS_TO_DELETE_FILE),
            objectGroupsToDeleteFileArgCaptor.capture(), eq(true), eq(false));
        verify(handler).transferFileToWorkspace(eq(OBJECT_GROUPS_TO_DETACH_FILE),
            objectGroupsToDetachFileArgCaptor.capture(), eq(true), eq(false));

        List<JsonLineModel> objectGroupsToDelete =
            FileUtils.readLines(objectGroupsToDeleteFileArgCaptor.getValue(), StandardCharsets.UTF_8)
                .stream().map(EliminationActionObjectGroupPreparationHandlerTest::parse).collect(toList());

        assertThat(objectGroupsToDelete).hasSize(2);

        checkObjectGroupToDelete(objectGroupsToDelete, "id_got_1", "id_got_1_object_1",
            "id_got_1_object_2");
        checkObjectGroupToDelete(objectGroupsToDelete, "id_got_3", "id_got_3_object_1");

        List<JsonLineModel> objectGroupsToDetach =
            FileUtils.readLines(objectGroupsToDetachFileArgCaptor.getValue(), StandardCharsets.UTF_8)
                .stream().map(EliminationActionObjectGroupPreparationHandlerTest::parse).collect(toList());

        assertThat(objectGroupsToDetach).hasSize(2);
        checkObjectGroupToDetach(objectGroupsToDetach, "id_got_2", "id_unit_20");
        checkObjectGroupToDetach(objectGroupsToDetach, "id_got_5", "id_unit_50", "id_unit_51");
    }

    private void checkReportEntry(ArrayList<EliminationActionObjectGroupReportEntry> entries, String id,
        String sp, String opi, List<String> deletedObjectIds, List<String> deletedParentUnitIds,
        EliminationActionObjectGroupStatus status) {

        EliminationActionObjectGroupReportEntry entry =
            entries.stream().filter(e -> e.getObjectGroupId().equals(id)).findFirst().get();

        assertThat(entry.getObjectGroupId()).isEqualTo(id);
        assertThat(entry.getOriginatingAgency()).isEqualTo(sp);
        assertThat(entry.getInitialOperation()).isEqualTo(opi);
        assertThat(entry.getStatus()).isEqualTo(status.name());
        assertThat(SetUtils.emptyIfNull(entry.getObjectIds()))
            .containsExactlyInAnyOrder(ListUtils.emptyIfNull(deletedObjectIds).toArray(new String[0]));
        assertThat(SetUtils.emptyIfNull(entry.getDeletedParentUnitIds()))
            .containsExactlyInAnyOrder(ListUtils.emptyIfNull(deletedParentUnitIds).toArray(new String[0]));
    }

    private void checkObjectGroupToDetach(List<JsonLineModel> objectGroupsToDetach, String id,
        String... removedParentIds)
        throws InvalidParseOperationException {

        JsonLineModel objectGroupToDetach =
            objectGroupsToDetach.stream().filter(o -> o.getId().equals(id)).findFirst().get();

        assertThat(objectGroupToDetach.getId()).isEqualTo(id);
        assertThat(objectGroupToDetach.getDistribGroup()).isNull();
        assertThat(JsonHandler.getFromJsonNode(objectGroupToDetach.getParams(), Set.class))
            .containsExactlyInAnyOrder(removedParentIds);
    }

    private void checkObjectGroupToDelete(List<JsonLineModel> objectGroupsToDelete, String id, String... objectIds)
        throws InvalidParseOperationException {
        JsonLineModel objectGroupToDelete =
            objectGroupsToDelete.stream().filter(o -> o.getId().equals(id)).findFirst().get();

        assertThat(objectGroupToDelete.getDistribGroup()).isNull();
        assertThat(JsonHandler.getFromJsonNode(objectGroupToDelete.getParams(), Set.class))
            .containsExactlyInAnyOrder(objectIds);
    }

    private static JsonLineModel parse(String line) {
        try {
            return JsonHandler.getFromString(line, JsonLineModel.class);
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
