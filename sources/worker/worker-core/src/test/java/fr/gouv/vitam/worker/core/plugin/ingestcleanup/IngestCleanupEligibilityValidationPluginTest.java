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

import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.plugin.ingestcleanup.report.CleanupReportManager;
import fr.gouv.vitam.worker.core.plugin.ingestcleanup.service.IngestCleanupEligibilityService;
import fr.gouv.vitam.worker.core.plugin.preservation.TestHandlerIO;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class IngestCleanupEligibilityValidationPluginTest {

    private static final String INGEST_OPERATION_ID = "aeeaaaaaacesicexaah6kalo7e62mmqaaaaq";

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private IngestCleanupEligibilityService ingestCleanupEligibilityService;

    @InjectMocks
    private IngestCleanupEligibilityValidationPlugin instance;

    private TestHandlerIO handler;
    private WorkerParameters params;

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");

        params = WorkerParametersFactory.newWorkerParameters()
            .putParameterValue(WorkerParameterName.ingestOperationIdToCleanup, INGEST_OPERATION_ID)
            .putParameterValue(WorkerParameterName.containerName, VitamThreadUtils.getVitamSession().getRequestId());

        handler = new TestHandlerIO();
        handler.setNewLocalFileProvider(name -> {
            try {
                return tempFolder.newFile(name);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @After
    public void tearDown() throws Exception {}

    @Test
    @RunWithCustomExecutor
    public void checkEligibilityOK() throws Exception {
        // Given

        // When
        ItemStatus itemStatus = instance.execute(params, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        verify(ingestCleanupEligibilityService).checkChildUnitsFromOtherIngests(eq(INGEST_OPERATION_ID), any());
        verify(ingestCleanupEligibilityService).checkUnitUpdatesFromOtherOperations(eq(INGEST_OPERATION_ID), any());
        verify(ingestCleanupEligibilityService).checkObjectAttachmentsToExistingObjectGroups(
            eq(INGEST_OPERATION_ID),
            any()
        );
        verify(ingestCleanupEligibilityService).checkObjectGroupUpdatesFromOtherOperations(
            eq(INGEST_OPERATION_ID),
            any()
        );
        verifyNoMoreInteractions(ingestCleanupEligibilityService);

        Optional<CleanupReportManager> cleanupReportManager = CleanupReportManager.loadReportDataFromWorkspace(handler);
        assertThat(cleanupReportManager.isPresent()).isTrue();
        assertThat(cleanupReportManager.get().getGlobalStatus()).isEqualTo(StatusCode.OK);
    }

    @Test
    @RunWithCustomExecutor
    public void checkEligibilityWithWarningThanWarning() throws Exception {
        // Given
        doAnswer(args -> {
            ((CleanupReportManager) args.getArgument(1)).reportUnitWarning("unit1", "message");
            return null;
        })
            .when(ingestCleanupEligibilityService)
            .checkUnitUpdatesFromOtherOperations(any(), any());

        // When
        ItemStatus itemStatus = instance.execute(params, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.WARNING);

        Optional<CleanupReportManager> cleanupReportManager = CleanupReportManager.loadReportDataFromWorkspace(handler);
        assertThat(cleanupReportManager.isPresent()).isTrue();
        assertThat(cleanupReportManager.get().getGlobalStatus()).isEqualTo(StatusCode.WARNING);
    }

    @Test
    @RunWithCustomExecutor
    public void checkEligibilityWithErrorThanError() throws Exception {
        // Given
        doAnswer(args -> {
            ((CleanupReportManager) args.getArgument(1)).reportUnitWarning("unit1", "message");
            return null;
        })
            .when(ingestCleanupEligibilityService)
            .checkUnitUpdatesFromOtherOperations(any(), any());
        doAnswer(args -> {
            ((CleanupReportManager) args.getArgument(1)).reportObjectGroupError("og1", "message");
            return null;
        })
            .when(ingestCleanupEligibilityService)
            .checkObjectAttachmentsToExistingObjectGroups(any(), any());

        // When
        ItemStatus itemStatus = instance.execute(params, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);

        Optional<CleanupReportManager> cleanupReportManager = CleanupReportManager.loadReportDataFromWorkspace(handler);
        assertThat(cleanupReportManager.isPresent()).isTrue();
        assertThat(cleanupReportManager.get().getGlobalStatus()).isEqualTo(StatusCode.KO);
    }
}
