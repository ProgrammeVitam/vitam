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
package fr.gouv.vitam.worker.core.plugin.probativevalue;

import com.google.common.io.Files;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.functional.administration.core.backup.BackupService;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.plugin.probativevalue.ProbativeCreateReport.ReportVersion2;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ProbativeReportEntry;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ProbativeReportV2;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.REPORT;
import static fr.gouv.vitam.worker.core.plugin.evidence.EvidenceService.JSON;
import static fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter.TestWorkerParameterBuilder.workerParameterBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProbativeCreateReportTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private BackupService backupService;

    @InjectMocks
    private ProbativeCreateReport probativeCreateReport;

    private static final String DISTRIBUTION_FILE = "distributionFile.jsonl";

    @Test
    @RunWithCustomExecutor
    public void should_backup_report() throws Exception {
        // Given
        final String containerName = "DEFAULT_CONTAINER_NAME";
        final String probativeEntryIdInDistributionFile = "BATMAN_GOT_ID";
        final AtomicReference<File> reportFileRef = new AtomicReference<>();

        PreservationRequest request = new PreservationRequest();
        ProbativeReportEntry object = ProbativeReportEntry.koFrom(
            LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
            Collections.singletonList("unitId"),
            "groupId",
            "objectId",
            "usageVersion"
        );

        HandlerIO handlerIO = mock(HandlerIO.class);
        when(handlerIO.isExistingFileInWorkspace(eq(DISTRIBUTION_FILE))).thenReturn(true);

        when(handlerIO.getInputStreamFromWorkspace(any())).thenReturn(
            writeDistributionFile(new JsonLineModel(probativeEntryIdInDistributionFile, 0, null))
        );
        when(handlerIO.getFileFromWorkspace(eq("request"))).thenReturn(pojoToFile(request));
        when(handlerIO.getNewLocalFile(any())).thenReturn(tempFolder.newFile(GUIDFactory.newGUID().getId()));
        when(handlerIO.getFileFromWorkspace(eq(probativeEntryIdInDistributionFile))).thenReturn(pojoToFile(object));

        when(handlerIO.getContainerName()).thenReturn(containerName);

        WorkerParameters build = workerParameterBuilder().withContainerName(containerName).build();

        doAnswer(args -> {
            String filename = args.getArgument(0);
            reportFileRef.set(tempFolder.newFile(filename));
            Files.copy(args.getArgument(1), reportFileRef.get());
            return null;
        })
            .when(handlerIO)
            .transferAtomicFileToWorkspace(anyString(), any(File.class));

        // When
        probativeCreateReport.execute(build, handlerIO);

        // Then
        verify(backupService).storeIntoOffers(
            eq(containerName),
            eq(containerName + JSON),
            eq(REPORT),
            eq(containerName + JSON),
            eq(VitamConfiguration.getDefaultStrategy())
        );

        Assert.assertNotNull(reportFileRef.get());

        ProbativeReportV2 report = JsonHandler.getFromFile(reportFileRef.get(), ProbativeReportV2.class);
        assertThat(
            LocalDateUtil.parseMongoFormattedDate(report.getReportSummary().getEvEndDateTime())
        ).isAfterOrEqualTo(LocalDateUtil.parseMongoFormattedDate(report.getReportSummary().getEvStartDateTime()));

        FileUtils.deleteQuietly(reportFileRef.get());
    }

    @Test
    @RunWithCustomExecutor
    public void should_store_report_atomically_when_report_not_exists_in_workspace() throws Exception {
        // Given
        String probativeEntryIdInDistributionFile = "BATMAN_GOT_ID";
        String containerName = "MyContainerName";

        HandlerIO handler = mock(HandlerIO.class);
        when(handler.isExistingFileInWorkspace(eq(DISTRIBUTION_FILE))).thenReturn(true);

        doReturn(false).when(handler).isExistingFileInWorkspace(containerName + JSON);
        doReturn(containerName).when(handler).getContainerName();
        doReturn(tempFolder.newFile(GUIDFactory.newGUID().getId())).when(handler).getNewLocalFile(anyString());

        PreservationRequest request = new PreservationRequest();
        doReturn(pojoToFile(request)).when(handler).getFileFromWorkspace("request");

        doReturn(writeDistributionFile(new JsonLineModel(probativeEntryIdInDistributionFile, 0, null)))
            .when(handler)
            .getInputStreamFromWorkspace("distributionFile.jsonl");

        ProbativeReportEntry object = ProbativeReportEntry.koFrom(
            LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
            Collections.singletonList("unitId"),
            "groupId",
            "objectId",
            "usageVersion"
        );
        doReturn(pojoToFile(object)).when(handler).getFileFromWorkspace(probativeEntryIdInDistributionFile);

        WorkerParameters build = workerParameterBuilder().withContainerName(containerName).build();

        // When
        ItemStatus execute = probativeCreateReport.execute(build, handler);

        // Then
        assertThat(execute.getGlobalStatus()).isEqualTo(StatusCode.OK);
        verify(handler).transferAtomicFileToWorkspace(eq(containerName + JSON), any());
        verify(backupService).storeIntoOffers(
            eq(containerName),
            eq(containerName + JSON),
            eq(REPORT),
            eq(containerName + JSON),
            eq(VitamConfiguration.getDefaultStrategy())
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_backup_existing_report_when_report_already_stored_in_workspace() throws Exception {
        // Given
        String containerName = "MyContainerName";

        HandlerIO handler = mock(HandlerIO.class);
        when(handler.isExistingFileInWorkspace(eq(DISTRIBUTION_FILE))).thenReturn(true);
        when(handler.isExistingFileInWorkspace(containerName + JSON)).thenReturn(true);
        doReturn(containerName).when(handler).getContainerName();

        WorkerParameters build = workerParameterBuilder().withContainerName(containerName).build();

        // When
        probativeCreateReport.execute(build, handler);

        // Then
        verify(handler, never()).getFileFromWorkspace(any());
        verify(handler, never()).transferAtomicFileToWorkspace(anyString(), any());
        verify(backupService).storeIntoOffers(
            eq(containerName),
            eq(containerName + JSON),
            eq(REPORT),
            eq(containerName + JSON),
            eq(VitamConfiguration.getDefaultStrategy())
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_item_status_OK() throws Exception {
        // Given
        String probativeEntryIdInDistributionFile = "BATMAN_GOT_ID";

        PreservationRequest request = new PreservationRequest();
        ProbativeReportEntry object = ProbativeReportEntry.koFrom(
            LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()),
            Collections.singletonList("unitId"),
            "groupId",
            "objectId",
            "usageVersion"
        );

        HandlerIO handler = mock(HandlerIO.class);
        when(handler.isExistingFileInWorkspace(eq(DISTRIBUTION_FILE))).thenReturn(true);

        /*handler.transferFileToWorkspace("request", pojoToFile(request), false, false);
        handler.setInputStreamFromWorkspace(
            writeDistributionFile(new JsonLineModel(probativeEntryIdInDistributionFile, 0, null)));
        handler.transferFileToWorkspace(probativeEntryIdInDistributionFile, pojoToFile(object), false, false);
        handler.setNewLocalFile(tempFolder.newFile(GUIDFactory.newGUID().getId()));*/

        when(handler.getInputStreamFromWorkspace(any())).thenReturn(
            writeDistributionFile(new JsonLineModel(probativeEntryIdInDistributionFile, 0, null))
        );
        when(handler.getFileFromWorkspace(eq("request"))).thenReturn(pojoToFile(request));
        when(handler.getNewLocalFile(any())).thenReturn(tempFolder.newFile(GUIDFactory.newGUID().getId()));
        when(handler.getFileFromWorkspace(eq(probativeEntryIdInDistributionFile))).thenReturn(pojoToFile(object));

        String containerName = handler.getContainerName();
        WorkerParameters build = workerParameterBuilder().withContainerName(containerName).build();

        // When
        ItemStatus itemStatus = probativeCreateReport.execute(build, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(OK);
        assertThat(itemStatus.getMasterData().get(LogbookParameterName.eventDetailData.name())).isEqualTo(
            JsonHandler.unprettyPrint(new ReportVersion2())
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_item_status_FATAL_when_something_wrong() throws Exception {
        // Given
        HandlerIO handler = mock(HandlerIO.class);
        when(handler.isExistingFileInWorkspace(eq(DISTRIBUTION_FILE))).thenThrow(new ProcessingException(""));

        String containerName = handler.getContainerName();
        WorkerParameters build = workerParameterBuilder().withContainerName(containerName).build();

        when(backupService.storeIntoOffers(anyString(), anyString(), any(), any(), anyString())).thenThrow(
            new IllegalStateException()
        );

        // When
        ItemStatus itemStatus = probativeCreateReport.execute(build, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(FATAL);
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_item_status_KO_when_cannot_deserialize_report_entry() throws Exception {
        // Given
        String probativeEntryIdInDistributionFile = "BATMAN_GOT_ID";

        PreservationRequest request = new PreservationRequest();

        Map<String, List<Integer>> notReportEntryObject = new HashMap<>();
        notReportEntryObject.put("unitId", Arrays.asList(1, 2, 3));
        notReportEntryObject.put("objectGroupId", Arrays.asList(1, 2, 3));

        HandlerIO handler = mock(HandlerIO.class);
        when(handler.isExistingFileInWorkspace(eq(DISTRIBUTION_FILE))).thenReturn(true);
        /*handler.transferFileToWorkspace("request", pojoToFile(request), false, false);
        handler.setInputStreamFromWorkspace(
            writeDistributionFile(new JsonLineModel(probativeEntryIdInDistributionFile, 0, null)));
        handler.transferFileToWorkspace(probativeEntryIdInDistributionFile, pojoToFile(notReportEntryObject), false,
            false);
        handler.setNewLocalFile(tempFolder.newFile(GUIDFactory.newGUID().getId()));*/

        when(handler.getInputStreamFromWorkspace(any())).thenReturn(
            writeDistributionFile(new JsonLineModel(probativeEntryIdInDistributionFile, 0, null))
        );
        when(handler.getFileFromWorkspace(eq("request"))).thenReturn(pojoToFile(request));
        when(handler.getNewLocalFile(any())).thenReturn(tempFolder.newFile(GUIDFactory.newGUID().getId()));
        when(handler.getFileFromWorkspace(eq(probativeEntryIdInDistributionFile))).thenReturn(
            pojoToFile(notReportEntryObject)
        );

        String containerName = handler.getContainerName();
        WorkerParameters build = workerParameterBuilder().withContainerName(containerName).build();

        // When
        ItemStatus itemStatus = probativeCreateReport.execute(build, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(FATAL);
    }

    private File pojoToFile(Object object) throws IOException, InvalidParseOperationException {
        File file = tempFolder.newFile();
        JsonHandler.writeAsFile(object, file);
        return file;
    }

    private InputStream writeDistributionFile(JsonLineModel distribution) {
        return new ByteArrayInputStream(JsonHandler.unprettyPrint(distribution).getBytes());
    }
}
