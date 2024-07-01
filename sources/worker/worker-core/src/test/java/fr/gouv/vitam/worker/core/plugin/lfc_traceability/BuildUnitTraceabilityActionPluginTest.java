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
package fr.gouv.vitam.worker.core.plugin.lfc_traceability;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.response.BatchObjectInformationResponse;
import fr.gouv.vitam.worker.common.HandlerIO;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class BuildUnitTraceabilityActionPluginTest {

    private static final Integer TENANT_ID = 0;
    private static final String LFC_WITH_METADATA_FILE = "BuildUnitTraceabilityActionPlugin/lfcWithMetadata.jsonl";
    private static final String BATCH_DIGESTS_FILE = "BuildUnitTraceabilityActionPlugin/batchDigests.json";
    private static final String BATCH_DIGESTS_PART1_FILE = "BuildUnitTraceabilityActionPlugin/batchDigestsPart1.json";
    private static final String BATCH_DIGESTS_PART2_FILE = "BuildUnitTraceabilityActionPlugin/batchDigestsPart2.json";
    private static final String BATCH_DIGESTS_BAD_DIGEST_FILE =
        "BuildUnitTraceabilityActionPlugin/batchDigestsBadDigest.json";
    private static final String BATCH_DIGESTS_ALL_BAD_DIGEST_FILE =
        "BuildUnitTraceabilityActionPlugin/batchDigestsAllBadDigest.json";
    private static final String TRACEABILITY_DATA_FILE = "BuildUnitTraceabilityActionPlugin/traceabilityData.jsonl";
    private static final String TRACEABILITY_STATS_FILE = "BuildUnitTraceabilityActionPlugin/traceabilityStats.json";
    private static final String TRACEABILITY_DATA_BAD_HASH_FILE =
        "BuildUnitTraceabilityActionPlugin/traceabilityDataBadDigest.jsonl";
    private static final String TRACEABILITY_STATS_BAD_HASH_FILE =
        "BuildUnitTraceabilityActionPlugin/traceabilityStatsBadDigest.json";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mock
    private HandlerIO handler;

    @Mock
    private WorkerParameters params;

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private StorageClient storageClient;

    @Mock
    private AlertService alertService;

    public BuildUnitTraceabilityActionPluginTest() {
        // do nothing
    }

    @Before
    public void setUp() throws Exception {
        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        handler = mock(HandlerIO.class);
    }

    @Test
    @RunWithCustomExecutor
    public void givenLfcAndMetadataWhenExecuteThenReturnResponseOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        doReturn(PropertiesUtils.getResourceFile(LFC_WITH_METADATA_FILE)).when(handler).getInput(0);
        doReturn(new ProcessingUri(UriPrefix.MEMORY, "traceabilityData.jsonl")).when(handler).getOutput(0);
        doReturn(new ProcessingUri(UriPrefix.MEMORY, "traceabilityStats.json")).when(handler).getOutput(1);
        File traceabilityDataFile = folder.newFile();
        File traceabilityStatsFile = folder.newFile();
        doReturn(traceabilityDataFile).when(handler).getNewLocalFile("traceabilityData.jsonl");
        doReturn(traceabilityStatsFile).when(handler).getNewLocalFile("traceabilityStats.json");

        List<String> offerIds = Arrays.asList("vitam-iaas-app-02.int", "vitam-iaas-app-03.int");
        doReturn(offerIds).when(storageClient).getOffers(VitamConfiguration.getDefaultStrategy());

        RequestResponseOK<BatchObjectInformationResponse> objectDigest = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        doReturn(objectDigest)
            .when(storageClient)
            .getBatchObjectInformation(
                anyString(),
                eq(DataCategory.UNIT),
                eq(offerIds),
                eq(
                    Arrays.asList(
                        "aeaqaaaaaaesicexaasycalg6wczgmiaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczgwyaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczguqaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczgvqaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczgwaaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczgwqaaaaq.json"
                    )
                )
            );

        // When
        BuildUnitTraceabilityActionPlugin plugin = new BuildUnitTraceabilityActionPlugin(
            storageClientFactory,
            1000,
            alertService
        );
        ItemStatus statusCode = plugin.execute(params, handler);

        // Then
        assertThat(statusCode.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(traceabilityDataFile).hasSameContentAs(PropertiesUtils.getResourceFile(TRACEABILITY_DATA_FILE));
        JsonAssert.assertJsonEquals(
            JsonHandler.getFromFile(traceabilityStatsFile),
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile(TRACEABILITY_STATS_FILE))
        );
        verifyNoMoreInteractions(alertService);
    }

    @Test
    @RunWithCustomExecutor
    public void givenMultipleBatchLfcAndMetadataWhenExecuteThenReturnResponseOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        doReturn(PropertiesUtils.getResourceFile(LFC_WITH_METADATA_FILE)).when(handler).getInput(0);
        doReturn(new ProcessingUri(UriPrefix.MEMORY, "traceabilityData.jsonl")).when(handler).getOutput(0);
        doReturn(new ProcessingUri(UriPrefix.MEMORY, "traceabilityStats.json")).when(handler).getOutput(1);
        File traceabilityDataFile = folder.newFile();
        File traceabilityStatsFile = folder.newFile();
        doReturn(traceabilityDataFile).when(handler).getNewLocalFile("traceabilityData.jsonl");
        doReturn(traceabilityStatsFile).when(handler).getNewLocalFile("traceabilityStats.json");

        List<String> offerIds = Arrays.asList("vitam-iaas-app-02.int", "vitam-iaas-app-03.int");
        doReturn(offerIds).when(storageClient).getOffers(anyString());

        RequestResponseOK<BatchObjectInformationResponse> objectDigest1 = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_PART1_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        doReturn(objectDigest1)
            .when(storageClient)
            .getBatchObjectInformation(
                anyString(),
                eq(DataCategory.UNIT),
                eq(offerIds),
                eq(
                    Arrays.asList(
                        "aeaqaaaaaaesicexaasycalg6wczgmiaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczgwyaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczguqaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczgvqaaaba.json"
                    )
                )
            );

        RequestResponseOK<BatchObjectInformationResponse> objectDigest2 = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_PART2_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        doReturn(objectDigest2)
            .when(storageClient)
            .getBatchObjectInformation(
                anyString(),
                eq(DataCategory.UNIT),
                eq(offerIds),
                eq(
                    Arrays.asList(
                        "aeaqaaaaaaesicexaasycalg6wczgwaaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczgwqaaaaq.json"
                    )
                )
            );

        // When
        BuildUnitTraceabilityActionPlugin plugin = new BuildUnitTraceabilityActionPlugin(
            storageClientFactory,
            4,
            alertService
        );
        ItemStatus statusCode = plugin.execute(params, handler);

        // Then
        assertThat(statusCode.getGlobalStatus()).isEqualTo(StatusCode.OK);
        assertThat(traceabilityDataFile).hasSameContentAs(PropertiesUtils.getResourceFile(TRACEABILITY_DATA_FILE));
        JsonAssert.assertJsonEquals(
            JsonHandler.getFromFile(traceabilityStatsFile),
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile(TRACEABILITY_STATS_FILE))
        );
        verifyNoMoreInteractions(alertService);
    }

    @Test
    @RunWithCustomExecutor
    public void givenLfcAndMetadataWithBadStorageDigestWhenExecuteThenReturnResponseOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        doReturn(PropertiesUtils.getResourceFile(LFC_WITH_METADATA_FILE)).when(handler).getInput(0);
        doReturn(new ProcessingUri(UriPrefix.MEMORY, "traceabilityData.jsonl")).when(handler).getOutput(0);
        doReturn(new ProcessingUri(UriPrefix.MEMORY, "traceabilityStats.json")).when(handler).getOutput(1);
        File traceabilityDataFile = folder.newFile();
        File traceabilityStatsFile = folder.newFile();
        doReturn(traceabilityDataFile).when(handler).getNewLocalFile("traceabilityData.jsonl");
        doReturn(traceabilityStatsFile).when(handler).getNewLocalFile("traceabilityStats.json");

        List<String> offerIds = Arrays.asList("vitam-iaas-app-02.int", "vitam-iaas-app-03.int");
        doReturn(offerIds).when(storageClient).getOffers(anyString());

        RequestResponseOK<BatchObjectInformationResponse> objectDigest = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_BAD_DIGEST_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        doReturn(objectDigest)
            .when(storageClient)
            .getBatchObjectInformation(
                anyString(),
                eq(DataCategory.UNIT),
                eq(offerIds),
                eq(
                    Arrays.asList(
                        "aeaqaaaaaaesicexaasycalg6wczgmiaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczgwyaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczguqaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczgvqaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczgwaaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczgwqaaaaq.json"
                    )
                )
            );

        // When
        BuildUnitTraceabilityActionPlugin plugin = new BuildUnitTraceabilityActionPlugin(
            storageClientFactory,
            1000,
            alertService
        );
        ItemStatus statusCode = plugin.execute(params, handler);

        // Then
        assertThat(statusCode.getGlobalStatus()).isEqualTo(StatusCode.WARNING);
        assertThat(traceabilityDataFile).hasSameContentAs(
            PropertiesUtils.getResourceFile(TRACEABILITY_DATA_BAD_HASH_FILE)
        );
        JsonAssert.assertJsonEquals(
            JsonHandler.getFromFile(traceabilityStatsFile),
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile(TRACEABILITY_STATS_BAD_HASH_FILE))
        );
        // 2 alerts (WARN for missing digest and ERROR for digest mismatch)
        verify(alertService, times(1)).createAlert(eq(VitamLogLevel.WARN), anyString());
        verify(alertService, times(1)).createAlert(eq(VitamLogLevel.ERROR), anyString());
    }

    @Test
    @RunWithCustomExecutor
    public void givenLfcAndMetadataWithAllBadStorageDigestWhenExecuteThenReturnResponseOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        doReturn(PropertiesUtils.getResourceFile(LFC_WITH_METADATA_FILE)).when(handler).getInput(0);
        doReturn(new ProcessingUri(UriPrefix.MEMORY, "traceabilityData.jsonl")).when(handler).getOutput(0);
        doReturn(new ProcessingUri(UriPrefix.MEMORY, "traceabilityStats.json")).when(handler).getOutput(1);
        File traceabilityDataFile = folder.newFile();
        File traceabilityStatsFile = folder.newFile();
        doReturn(traceabilityDataFile).when(handler).getNewLocalFile("traceabilityData.jsonl");
        doReturn(traceabilityStatsFile).when(handler).getNewLocalFile("traceabilityStats.json");

        List<String> offerIds = Arrays.asList("vitam-iaas-app-02.int", "vitam-iaas-app-03.int");
        doReturn(offerIds).when(storageClient).getOffers(anyString());

        RequestResponseOK<BatchObjectInformationResponse> MetadataDigest = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_ALL_BAD_DIGEST_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        doReturn(MetadataDigest)
            .when(storageClient)
            .getBatchObjectInformation(
                anyString(),
                eq(DataCategory.UNIT),
                eq(offerIds),
                eq(
                    Arrays.asList(
                        "aeaqaaaaaaesicexaasycalg6wczgmiaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczgwyaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczguqaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczgvqaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczgwaaaaba.json",
                        "aeaqaaaaaaesicexaasycalg6wczgwqaaaaq.json"
                    )
                )
            );

        // When
        BuildUnitTraceabilityActionPlugin plugin = new BuildUnitTraceabilityActionPlugin(
            storageClientFactory,
            1000,
            alertService
        );
        ItemStatus statusCode = plugin.execute(params, handler);

        // Then
        assertThat(statusCode.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(statusCode.getData("eventDetailData")).isEqualTo(
            "{\"error\":\"There are at least1 metadata with inconsistent digest between database and offers\",\"idObjectKo\":[\"aeaqaaaaaaesicexaasycalg6wczgwyaaaba\"]}"
        );

        // 2 alerts (WARN for missing digest and ERROR for digest mismatch)
        verify(alertService, times(2)).createAlert(eq(VitamLogLevel.WARN), anyString());
        verify(alertService, times(1)).createAlert(eq(VitamLogLevel.ERROR), anyString());
    }
}
