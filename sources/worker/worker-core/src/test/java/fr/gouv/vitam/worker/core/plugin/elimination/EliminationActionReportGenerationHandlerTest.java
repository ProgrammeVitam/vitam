package fr.gouv.vitam.worker.core.plugin.elimination;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.collection.CloseableIteratorUtils;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.BackupService;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationActionObjectGroupStatus;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationActionUnitStatus;
import fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionObjectGroupReportEntry;
import fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionObjectGroupReportService;
import fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionUnitReportEntry;
import fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionUnitReportService;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class EliminationActionReportGenerationHandlerTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private EliminationActionUnitReportService eliminationActionUnitReportService;

    @Mock
    private EliminationActionObjectGroupReportService eliminationActionObjectGroupReportService;

    @Mock
    private BackupService backupService;

    @InjectMocks
    private EliminationActionReportGenerationHandler instance;

    @Mock
    private HandlerIO handler;

    private WorkerParameters params;

    @Before
    public void setUp() throws Exception {

        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");

        doAnswer(args -> tempFolder.newFile(args.getArgumentAt(0, String.class))).when(handler).getNewLocalFile(any());

        params = WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
            .newGUID()).setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
            .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
            .setProcessId(VitamThreadUtils.getVitamSession().getRequestId())
            .setObjectName("REF")
            .setCurrentStep("StepName");

        doReturn(VitamThreadUtils.getVitamSession().getRequestId())
            .when(handler).getContainerName();
    }


    @Test
    @RunWithCustomExecutor
    public void testExecute_OK() throws Exception {

        doReturn(CloseableIteratorUtils.toCloseableIterator(Arrays.asList(
            new EliminationActionUnitReportEntry("id_unit_1", "sp1", "opi1", "id_got_1",
                EliminationActionUnitStatus.DELETED),
            new EliminationActionUnitReportEntry("id_unit_2", "sp2", "opi2", "id_got_2",
                EliminationActionUnitStatus.DELETED),
            new EliminationActionUnitReportEntry("id_unit_3", "sp3", "opi3", "id_got_3",
                EliminationActionUnitStatus.NON_DESTROYABLE_HAS_CHILD_UNITS),
            new EliminationActionUnitReportEntry("id_unit_4", "sp4", "opi4", "id_got_2",
                EliminationActionUnitStatus.GLOBAL_STATUS_KEEP)).iterator()))
            .when(eliminationActionUnitReportService).exportUnits(any());

        doReturn(CloseableIteratorUtils.toCloseableIterator(Arrays.asList(
            new EliminationActionObjectGroupReportEntry("id_got_1", "sp1", "opi1", null,
                new HashSet<>(Arrays.asList("id_got_1_object_1", "id_got_1_object_2")),
                EliminationActionObjectGroupStatus.DELETED),
            new EliminationActionObjectGroupReportEntry("id_got_2",
                "sp2", "opi2", new HashSet<>(singletonList("id_unit_2")), null,
                EliminationActionObjectGroupStatus.PARTIAL_DETACHMENT))
            .iterator()))
            .when(eliminationActionObjectGroupReportService).exportObjectGroups(any());

        AtomicReference<String> reportReference = new AtomicReference<>();
        doAnswer((args) -> {

            InputStream is = args.getArgumentAt(0, InputStream.class);
            reportReference.set(IOUtils.toString(is, StandardCharsets.UTF_8));
            return null;
        }).when(backupService)
            .backup(any(), eq(DataCategory.REPORT), eq(VitamThreadUtils.getVitamSession().getRequestId() + ".json"));

        ItemStatus itemStatus = instance.execute(params, handler);

        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        verify(backupService)
            .backup(any(), eq(DataCategory.REPORT), eq(VitamThreadUtils.getVitamSession().getRequestId() + ".json"));

        String report = reportReference.get();

        String expectedJson = IOUtils.toString(PropertiesUtils
                .getResourceAsStream("EliminationAction/EliminationActionReportGenerationHandler/expectedReport.json"),
            StandardCharsets.UTF_8);

        JsonAssert.assertJsonEquals(expectedJson, report, JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }
}
