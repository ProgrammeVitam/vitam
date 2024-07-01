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
package fr.gouv.vitam.worker.core.plugin.elimination;

import com.fasterxml.jackson.core.type.TypeReference;
import fr.gouv.vitam.batch.report.model.entry.EliminationActionUnitReportEntry;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
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
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationActionUnitStatus;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationAnalysisResult;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationEventDetails;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationGlobalStatus;
import fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionReportService;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.After;
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
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static fr.gouv.vitam.worker.core.plugin.elimination.EliminationActionUnitPreparationHandler.DATE_REQUEST_IN_FUTURE;
import static fr.gouv.vitam.worker.core.plugin.elimination.EliminationActionUnitPreparationHandler.REQUEST_JSON;
import static fr.gouv.vitam.worker.core.plugin.elimination.EliminationActionUnitPreparationHandler.UNITS_TO_DELETE_FILE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EliminationActionUnitPreparationHandlerTest {

    private static final TypeReference<JsonLineModel> TYPE_REFERENCE = new TypeReference<>() {};

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @Mock
    private EliminationAnalysisService eliminationAnalysisService;

    @Mock
    private EliminationActionReportService eliminationActionReportService;

    private List<EliminationActionUnitReportEntry> reportEntries;

    @InjectMocks
    private EliminationActionUnitPreparationHandler instance;

    @Mock
    private HandlerIO handler;

    private WorkerParameters params;

    @Before
    public void setUp() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId("opId");

        doReturn(metaDataClient).when(metaDataClientFactory).getClient();

        doAnswer(args -> tempFolder.newFile(args.getArgument(0))).when(handler).getNewLocalFile(any());

        params = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(VitamThreadUtils.getVitamSession().getRequestId())
            .setRequestId(VitamThreadUtils.getVitamSession().getRequestId())
            .setProcessId(VitamThreadUtils.getVitamSession().getRequestId())
            .setObjectName("REF")
            .setCurrentStep("StepName");

        reportEntries = new ArrayList<>();
        doAnswer(args -> reportEntries.addAll(args.getArgument(1)))
            .when(eliminationActionReportService)
            .appendEntries(any(), any());
    }

    private EliminationAnalysisResult createAnalysisResponse(EliminationGlobalStatus destroy) {
        return new EliminationAnalysisResult("opId", destroy, new HashSet<>(), new HashSet<>(), emptyList());
    }

    @After
    public void tearDown() {}

    @Test
    @RunWithCustomExecutor
    public void testExecute_DestroyableAndNonDestroyableUnits_Warn() throws Exception {
        doReturn(
            PropertiesUtils.getResourceAsStream(
                "EliminationAction/EliminationActionUnitPreparationHandler/request.json"
            )
        )
            .when(handler)
            .getInputStreamFromWorkspace(REQUEST_JSON);

        doReturn(
            JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream(
                    "EliminationAction/EliminationActionUnitPreparationHandler/unitsWithInheritedRules.json"
                )
            )
        )
            .when(metaDataClient)
            .selectUnitsWithInheritedRules(any());

        when(
            eliminationAnalysisService.analyzeElimination(anyString(), any(), any(), any(), any(), any(), any(), any())
        ).thenReturn(
            createAnalysisResponse(EliminationGlobalStatus.DESTROY), // id_unit_1
            createAnalysisResponse(EliminationGlobalStatus.KEEP), // id_unit_2
            createAnalysisResponse(EliminationGlobalStatus.DESTROY), // id_unit_3
            createAnalysisResponse(EliminationGlobalStatus.CONFLICT) // id_unit_4
        );

        ItemStatus itemStatus = instance.execute(params, handler);

        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.WARNING);

        EliminationEventDetails eventDetails = JsonHandler.getFromString(
            itemStatus.getEvDetailData(),
            EliminationEventDetails.class
        );
        assertThat(eventDetails.getExpirationDate()).isEqualTo("2015-01-01");
        assertThat(eventDetails.getNbDestroyableUnits()).isEqualTo(2);
        assertThat(eventDetails.getNbNonDestroyableUnits()).isEqualTo(2);

        assertThat(reportEntries).hasSize(2);

        assertThat(reportEntries.get(0).getId()).isEqualTo("id_unit_2");
        assertThat(reportEntries.get(0).getOriginatingAgency()).isEqualTo("sp_2");
        assertThat(reportEntries.get(0).getInitialOperation()).isEqualTo("opi2");
        assertThat(reportEntries.get(0).getStatus()).isEqualTo(EliminationActionUnitStatus.GLOBAL_STATUS_KEEP.name());
        assertThat(reportEntries.get(0).getObjectGroupId()).isEqualTo("id_got_2");

        assertThat(reportEntries.get(1).getId()).isEqualTo("id_unit_4");
        assertThat(reportEntries.get(1).getOriginatingAgency()).isEqualTo("sp_4");
        assertThat(reportEntries.get(1).getInitialOperation()).isEqualTo("opi4");
        assertThat(reportEntries.get(1).getStatus()).isEqualTo(
            EliminationActionUnitStatus.GLOBAL_STATUS_CONFLICT.name()
        );
        assertThat(reportEntries.get(1).getObjectGroupId()).isEqualTo("id_got_4");

        ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
        verify(handler).transferFileToWorkspace(
            eq(UNITS_TO_DELETE_FILE),
            fileArgumentCaptor.capture(),
            eq(true),
            eq(false)
        );

        try (
            JsonLineGenericIterator<JsonLineModel> jsonLineIterator = new JsonLineGenericIterator<>(
                new FileInputStream(fileArgumentCaptor.getValue()),
                TYPE_REFERENCE
            )
        ) {
            List<JsonLineModel> entries = IteratorUtils.toList(jsonLineIterator);

            assertThat(entries).hasSize(2);
            assertThat(entries.get(0).getId()).isEqualTo("id_unit_1");
            assertThat(entries.get(0).getDistribGroup()).isEqualTo(2);
            assertThat(MetadataDocumentHelper.getStrategyIdFromUnit(entries.get(0).getParams())).isEqualTo(
                "default-fake"
            );

            assertThat(entries.get(1).getId()).isEqualTo("id_unit_3");
            assertThat(entries.get(1).getDistribGroup()).isEqualTo(1);
            assertThat(MetadataDocumentHelper.getStrategyIdFromUnit(entries.get(1).getParams())).isEqualTo(
                "default-fake"
            );
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testExecute_DestroyableUnits_OK() throws Exception {
        doReturn(
            PropertiesUtils.getResourceAsStream(
                "EliminationAction/EliminationActionUnitPreparationHandler/request.json"
            )
        )
            .when(handler)
            .getInputStreamFromWorkspace(REQUEST_JSON);

        doReturn(
            JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream(
                    "EliminationAction/EliminationActionUnitPreparationHandler/unitsWithInheritedRules.json"
                )
            )
        )
            .when(metaDataClient)
            .selectUnitsWithInheritedRules(any());

        when(
            eliminationAnalysisService.analyzeElimination(anyString(), any(), any(), any(), any(), any(), any(), any())
        ).thenReturn(
            createAnalysisResponse(EliminationGlobalStatus.DESTROY), // id_unit_1
            createAnalysisResponse(EliminationGlobalStatus.DESTROY), // id_unit_2
            createAnalysisResponse(EliminationGlobalStatus.DESTROY), // id_unit_3
            createAnalysisResponse(EliminationGlobalStatus.DESTROY) // id_unit_4
        );

        ItemStatus itemStatus = instance.execute(params, handler);

        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        EliminationEventDetails eventDetails = JsonHandler.getFromString(
            itemStatus.getEvDetailData(),
            EliminationEventDetails.class
        );
        assertThat(eventDetails.getExpirationDate()).isEqualTo("2015-01-01");
        assertThat(eventDetails.getNbDestroyableUnits()).isEqualTo(4);
        assertThat(eventDetails.getNbNonDestroyableUnits()).isEqualTo(0);

        assertThat(reportEntries).hasSize(0);

        ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
        verify(handler).transferFileToWorkspace(
            eq(UNITS_TO_DELETE_FILE),
            fileArgumentCaptor.capture(),
            eq(true),
            eq(false)
        );

        try (
            JsonLineGenericIterator<JsonLineModel> jsonLineIterator = new JsonLineGenericIterator<>(
                new FileInputStream(fileArgumentCaptor.getValue()),
                TYPE_REFERENCE
            )
        ) {
            List<JsonLineModel> entries = IteratorUtils.toList(jsonLineIterator);

            assertThat(entries).hasSize(4);
            assertThat(entries.get(0).getId()).isEqualTo("id_unit_1");
            assertThat(entries.get(0).getDistribGroup()).isEqualTo(2);
            assertThat(MetadataDocumentHelper.getStrategyIdFromUnit(entries.get(0).getParams())).isEqualTo(
                "default-fake"
            );
            assertThat(entries.get(1).getId()).isEqualTo("id_unit_2");
            assertThat(entries.get(1).getDistribGroup()).isEqualTo(2);
            assertThat(MetadataDocumentHelper.getStrategyIdFromUnit(entries.get(1).getParams())).isEqualTo(
                "default-fake"
            );
            assertThat(entries.get(2).getId()).isEqualTo("id_unit_3");
            assertThat(entries.get(2).getDistribGroup()).isEqualTo(1);
            assertThat(MetadataDocumentHelper.getStrategyIdFromUnit(entries.get(2).getParams())).isEqualTo(
                "default-fake"
            );
            assertThat(entries.get(3).getId()).isEqualTo("id_unit_4");
            assertThat(entries.get(3).getDistribGroup()).isEqualTo(1);
            assertThat(MetadataDocumentHelper.getStrategyIdFromUnit(entries.get(3).getParams())).isEqualTo(
                "default-fake"
            );
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testExecute_EmptyResultSet_Warn() throws Exception {
        // Given
        doReturn(
            PropertiesUtils.getResourceAsStream(
                "EliminationAction/EliminationActionUnitPreparationHandler/request.json"
            )
        )
            .when(handler)
            .getInputStreamFromWorkspace(REQUEST_JSON);

        doReturn(
            JsonHandler.getFromInputStream(
                PropertiesUtils.getResourceAsStream(
                    "EliminationAction/EliminationActionUnitPreparationHandler/emptyUnitsWithInheritedRules.json"
                )
            )
        )
            .when(metaDataClient)
            .selectUnitsWithInheritedRules(any());

        // When
        ItemStatus itemStatus = instance.execute(params, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.WARNING);

        EliminationEventDetails eventDetails = JsonHandler.getFromString(
            itemStatus.getEvDetailData(),
            EliminationEventDetails.class
        );
        assertThat(eventDetails.getExpirationDate()).isEqualTo("2015-01-01");
        assertThat(eventDetails.getNbDestroyableUnits()).isEqualTo(0);
        assertThat(eventDetails.getNbNonDestroyableUnits()).isEqualTo(0);

        assertThat(reportEntries).hasSize(0);

        ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);
        verify(handler).transferFileToWorkspace(
            eq(UNITS_TO_DELETE_FILE),
            fileArgumentCaptor.capture(),
            eq(true),
            eq(false)
        );

        try (
            JsonLineGenericIterator<JsonLineModel> jsonLineIterator = new JsonLineGenericIterator<>(
                new FileInputStream(fileArgumentCaptor.getValue()),
                TYPE_REFERENCE
            )
        ) {
            List<JsonLineModel> entries = IteratorUtils.toList(jsonLineIterator);
            assertThat(entries).isEmpty();
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testExecute_FutureDate_Warn() throws Exception {
        doReturn(
            PropertiesUtils.getResourceAsStream(
                "EliminationAction/EliminationActionUnitPreparationHandler/invalidRequestWithFutureDate.json"
            )
        )
            .when(handler)
            .getInputStreamFromWorkspace(REQUEST_JSON);

        ItemStatus itemStatus = instance.execute(params, handler);

        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);

        EliminationEventDetails eventDetails = JsonHandler.getFromString(
            itemStatus.getEvDetailData(),
            EliminationEventDetails.class
        );
        assertThat(eventDetails.getExpirationDate()).isEqualTo("2050-01-01");
        assertThat(eventDetails.getError()).isEqualTo(DATE_REQUEST_IN_FUTURE);
    }
}
