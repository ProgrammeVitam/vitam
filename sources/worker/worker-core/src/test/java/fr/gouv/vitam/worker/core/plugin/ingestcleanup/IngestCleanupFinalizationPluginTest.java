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
package fr.gouv.vitam.worker.core.plugin.ingestcleanup;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.ingestcleanup.report.CleanupReportManager;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;

import static fr.gouv.vitam.worker.core.utils.JsonLineTestUtils.assertJsonlReportsEqualUnordered;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

public class IngestCleanupFinalizationPluginTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private StorageClient storageClient;

    @InjectMocks
    private IngestCleanupFinalizationPlugin instance;

    @Mock
    private WorkerParameters params;

    @Mock
    private HandlerIO handlerIO;

    @Before
    public void setUp() throws Exception {
        doReturn(storageClient).when(storageClientFactory).getClient();
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0));
        doAnswer(args -> tempFolder.newFile(args.getArgument(0))).when(handlerIO).getNewLocalFile(anyString());
        doReturn(VitamThreadUtils.getVitamSession().getRequestId()).when(handlerIO).getContainerName();
    }

    @Test
    @RunWithCustomExecutor
    public void testNonExistingReportData() throws Exception {
        // Given
        doThrow(new ContentAddressableStorageNotFoundException(""))
            .when(handlerIO)
            .getInputStreamFromWorkspace(CleanupReportManager.CLEANUP_REPORT_BACKUP_FILE_NAME);

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.WARNING);
    }

    @Test
    @RunWithCustomExecutor
    public void testReportExport() throws Exception {
        // Given
        doReturn(PropertiesUtils.getResourceAsStream("IngestCleanup/Finalization/reportData.json"))
            .when(handlerIO)
            .getInputStreamFromWorkspace(CleanupReportManager.CLEANUP_REPORT_BACKUP_FILE_NAME);

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        String reportFileName = VitamThreadUtils.getVitamSession().getRequestId() + ".jsonl";
        ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
        verify(handlerIO).transferFileToWorkspace(
            eq(reportFileName),
            fileArgumentCaptor.capture(),
            eq(true),
            eq(false)
        );

        assertJsonlReportsEqualUnordered(
            fileArgumentCaptor.getValue(),
            PropertiesUtils.getResourceFile("IngestCleanup/Finalization/expectedReport.jsonl"),
            1
        );

        ArgumentCaptor<ObjectDescription> descriptionArgumentCaptor = ArgumentCaptor.forClass(ObjectDescription.class);
        verify(storageClient).storeFileFromWorkspace(
            eq(VitamConfiguration.getDefaultStrategy()),
            eq(DataCategory.REPORT),
            eq(reportFileName),
            descriptionArgumentCaptor.capture()
        );
        assertThat(descriptionArgumentCaptor.getValue())
            .extracting(ObjectDescription::getWorkspaceContainerGUID, ObjectDescription::getWorkspaceObjectURI)
            .containsExactly(VitamThreadUtils.getVitamSession().getRequestId(), reportFileName);
    }
}
