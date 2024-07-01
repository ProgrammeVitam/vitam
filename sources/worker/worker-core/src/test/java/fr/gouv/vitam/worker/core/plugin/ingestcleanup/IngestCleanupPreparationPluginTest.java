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
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.plugin.ingestcleanup.report.CleanupReportManager;
import fr.gouv.vitam.worker.core.plugin.preservation.TestHandlerIO;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.FileNotFoundException;
import java.io.IOException;

import static fr.gouv.vitam.worker.core.utils.JsonLineTestUtils.assertJsonlReportsEqualUnordered;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

public class IngestCleanupPreparationPluginTest {

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
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private AdminManagementClientFactory adminManagementClientFactory;

    @Mock
    private AdminManagementClient adminManagementClient;

    @InjectMocks
    private IngestCleanupPreparationPlugin instance;

    private WorkerParameters params;
    private TestHandlerIO handlerIO;

    @Before
    public void setUp() throws Exception {
        doReturn(metaDataClient).when(metaDataClientFactory).getClient();
        doReturn(adminManagementClient).when(adminManagementClientFactory).getClient();
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0));

        params = WorkerParametersFactory.newWorkerParameters()
            .putParameterValue(WorkerParameterName.ingestOperationIdToCleanup, INGEST_OPERATION_ID)
            .putParameterValue(WorkerParameterName.containerName, VitamThreadUtils.getVitamSession().getRequestId());
        handlerIO = new TestHandlerIO().setContainerName(VitamThreadUtils.getVitamSession().getRequestId());
        handlerIO.setNewLocalFileProvider(name -> {
            try {
                return tempFolder.newFile(name);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @RunWithCustomExecutor
    @Test
    public void testPrepareUnitDistribution() throws Exception {
        // Given
        givenInitialReportData();
        givenUnits("IngestCleanup/Preparation/units.json");
        givenObjectGroups("IngestCleanup/Preparation/objectGroups.json");
        givenExistingAccessionRegisterDetails();

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertJsonlReportsEqualUnordered(
            handlerIO.getFileFromWorkspace(IngestCleanupPreparationPlugin.UNITS_TO_DELETE_JSONL),
            PropertiesUtils.getResourceFile("IngestCleanup/Preparation/expectedUnitDistributionFile.jsonl"),
            0
        );
    }

    @RunWithCustomExecutor
    @Test
    public void testPrepareObjectGroupDistribution() throws Exception {
        // Given
        givenInitialReportData();
        givenUnits("IngestCleanup/Preparation/units.json");
        givenObjectGroups("IngestCleanup/Preparation/objectGroups.json");
        givenExistingAccessionRegisterDetails();

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertJsonlReportsEqualUnordered(
            handlerIO.getFileFromWorkspace(IngestCleanupPreparationPlugin.OBJECT_GROUPS_TO_DELETE_JSONL),
            PropertiesUtils.getResourceFile("IngestCleanup/Preparation/expectedObjectGroupDistributionFile.jsonl"),
            0
        );
    }

    @RunWithCustomExecutor
    @Test
    public void testPrepareAccessingRegisterDistributionWhenExistingAccessionRegisterDetails() throws Exception {
        // Given
        givenInitialReportData();
        givenUnits("IngestCleanup/Preparation/units.json");
        givenObjectGroups("IngestCleanup/Preparation/objectGroups.json");
        givenExistingAccessionRegisterDetails();

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertJsonlReportsEqualUnordered(
            handlerIO.getFileFromWorkspace(IngestCleanupPreparationPlugin.ACCESSION_REGISTERS_JSONL),
            PropertiesUtils.getResourceFile(
                "IngestCleanup/Preparation/expectedAccessionRegisterDistributionFile.jsonl"
            ),
            0
        );
    }

    @RunWithCustomExecutor
    @Test
    public void testPrepareAccessingRegisterDistributionWhenNoAccessionRegisterDetails() throws Exception {
        // Given
        givenInitialReportData();
        givenUnits("IngestCleanup/Preparation/units.json");
        givenObjectGroups("IngestCleanup/Preparation/objectGroups.json");
        givenNoAccessionRegisterDetails();

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(handlerIO.getFileFromWorkspace(IngestCleanupPreparationPlugin.ACCESSION_REGISTERS_JSONL)).hasContent(
            ""
        );
    }

    @RunWithCustomExecutor
    @Test
    public void testPrepareAccessingRegisterDistributionWhenNoMetadataFound() throws Exception {
        // Given
        givenInitialReportData();
        givenUnits("IngestCleanup/Preparation/emptyUnits.json");
        givenObjectGroups("IngestCleanup/Preparation/emptyObjectGroups.json");
        givenExistingAccessionRegisterDetails();

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(handlerIO.getFileFromWorkspace(IngestCleanupPreparationPlugin.ACCESSION_REGISTERS_JSONL)).hasContent(
            ""
        );
    }

    @RunWithCustomExecutor
    @Test
    public void testPrepareReport() throws Exception {
        // Given
        givenInitialReportData();
        givenUnits("IngestCleanup/Preparation/units.json");
        givenObjectGroups("IngestCleanup/Preparation/objectGroups.json");
        givenExistingAccessionRegisterDetails();

        // When
        ItemStatus itemStatus = instance.execute(params, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        CleanupReportManager updatedReportManager = CleanupReportManager.loadReportDataFromWorkspace(
            handlerIO
        ).orElseThrow(AssertionError::new);
        JsonAssert.assertJsonEquals(
            JsonHandler.toJsonNode(updatedReportManager.getCleanupReport()),
            JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream("IngestCleanup/Preparation/expectedReportData.json")
            )
        );
    }

    private void givenInitialReportData() throws ProcessingException, FileNotFoundException {
        handlerIO.transferFileToWorkspace(
            CleanupReportManager.CLEANUP_REPORT_BACKUP_FILE_NAME,
            PropertiesUtils.getResourceFile("IngestCleanup/Preparation/initialReportData.json"),
            true,
            false
        );
    }

    private void givenUnits(String responseFile)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException, MetaDataClientServerException, FileNotFoundException {
        doReturn(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(responseFile)))
            .when(metaDataClient)
            .selectUnits(any());
    }

    private void givenObjectGroups(String reponseFile)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException, MetaDataClientServerException, FileNotFoundException {
        doReturn(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(reponseFile)))
            .when(metaDataClient)
            .selectObjectGroups(any());
    }

    private void givenExistingAccessionRegisterDetails() throws InvalidParseOperationException, ReferentialException {
        doReturn(new RequestResponseOK<AccessionRegisterDetail>().addResult(new AccessionRegisterDetail()))
            .when(adminManagementClient)
            .getAccessionRegisterDetail(any());
    }

    private void givenNoAccessionRegisterDetails() throws InvalidParseOperationException, ReferentialException {
        doReturn(new RequestResponseOK<AccessionRegisterDetail>())
            .when(adminManagementClient)
            .getAccessionRegisterDetail(any());
    }
}
