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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class BuildObjectGroupTraceabilityActionPluginTest {

    private static final Integer TENANT_ID = 0;
    private static final String LFC_WITH_METADATA_FILE =
        "BuildObjectGroupTraceabilityActionPlugin/lfcWithMetadata.jsonl";
    private static final String BATCH_DIGESTS_GOTS_FILE =
        "BuildObjectGroupTraceabilityActionPlugin/batchDigestsGots.json";
    private static final String BATCH_DIGESTS_OBJECTS_FILE =
        "BuildObjectGroupTraceabilityActionPlugin/batchDigestsObjects.json";
    private static final String TRACEABILITY_DATA_FILE =
        "BuildObjectGroupTraceabilityActionPlugin/traceabilityData.jsonl";
    private static final String TRACEABILITY_STATS_FILE =
        "BuildObjectGroupTraceabilityActionPlugin/traceabilityStats.json";

    private static final String BATCH_DIGESTS_GOT_PART1_FILE =
        "BuildObjectGroupTraceabilityActionPlugin/batchDigestsGotsPart1.json";
    private static final String BATCH_DIGESTS_GOT_PART2_FILE =
        "BuildObjectGroupTraceabilityActionPlugin/batchDigestsGotsPart2.json";
    private static final String BATCH_DIGESTS_OBJECTS_PART1_FILE =
        "BuildObjectGroupTraceabilityActionPlugin/batchDigestsObjectsPart1.json";
    private static final String BATCH_DIGESTS_OBJECTS_PART2_FILE =
        "BuildObjectGroupTraceabilityActionPlugin/batchDigestsObjectsPart2.json";

    private static final String BATCH_DIGESTS_GOTS_BAD_DIGEST_FILE =
        "BuildObjectGroupTraceabilityActionPlugin/batchDigestsGotsBadDigest.json";
    private static final String BATCH_DIGESTS_GOTS_ALL_BAD_DIGEST_FILE =
        "BuildObjectGroupTraceabilityActionPlugin/batchDigestsGotsAllKoDigest.json";
    private static final String BATCH_DIGESTS_OBJECTS_BAD_DIGEST_FILE =
        "BuildObjectGroupTraceabilityActionPlugin/batchDigestsObjectsBadDigest.json";

    private static final String BATCH_DIGESTS_OBJECTS_ALL_BAD_DIGEST_FILE =
        "BuildObjectGroupTraceabilityActionPlugin/batchDigestsObjectsAllBadDigest.json";
    private static final String TRACEABILITY_DATA_BAD_HASH_FILE =
        "BuildObjectGroupTraceabilityActionPlugin/traceabilityDataBadDigest.jsonl";
    private static final String TRACEABILITY_STATS_BAD_HASH_FILE =
        "BuildObjectGroupTraceabilityActionPlugin/traceabilityStatsBadDigest.json";

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

    public BuildObjectGroupTraceabilityActionPluginTest() {
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
        doReturn(offerIds).when(storageClient).getOffers(anyString());

        RequestResponseOK<BatchObjectInformationResponse> gotDigest = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_GOTS_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        doReturn(gotDigest)
            .when(storageClient)
            .getBatchObjectInformation(
                anyString(),
                eq(DataCategory.OBJECTGROUP),
                eq(offerIds),
                eq(
                    Arrays.asList(
                        "aebaaaaaaaesicexaasycalg6xcwe6qaaaaq.json",
                        "aebaaaaaaaesicexaasycalg6xcwe6aaaaaq.json",
                        "aebaaaaaaaesicexaasycalg6xcwe5aaaaaq.json"
                    )
                )
            );

        RequestResponseOK<BatchObjectInformationResponse> objectDigest = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_OBJECTS_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        doReturn(objectDigest)
            .when(storageClient)
            .getBatchObjectInformation(anyString(), eq(DataCategory.OBJECT), eq(offerIds), anyCollection());

        // When
        BuildObjectGroupTraceabilityActionPlugin plugin = new BuildObjectGroupTraceabilityActionPlugin(
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

        ArgumentCaptor<Collection> objectIds = ArgumentCaptor.forClass(Collection.class);
        verify(storageClient).getBatchObjectInformation(
            anyString(),
            eq(DataCategory.OBJECT),
            eq(offerIds),
            objectIds.capture()
        );
        assertThat(objectIds.getValue()).containsExactlyInAnyOrder(
            "aeaaaaaaaaesicexaasycalg6xcwe5yaaaba",
            "aeaaaaaaaaesicexaasycalg6xcwe4yaaaaq",
            "aeaaaaaaaaesicexaasycalg6xcwe6iaaaaq"
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

        RequestResponseOK<BatchObjectInformationResponse> gotDigest1 = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_GOT_PART1_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        doReturn(gotDigest1)
            .when(storageClient)
            .getBatchObjectInformation(
                anyString(),
                eq(DataCategory.OBJECTGROUP),
                eq(offerIds),
                eq(
                    Arrays.asList(
                        "aebaaaaaaaesicexaasycalg6xcwe6qaaaaq.json",
                        "aebaaaaaaaesicexaasycalg6xcwe6aaaaaq.json"
                    )
                )
            );

        RequestResponseOK<BatchObjectInformationResponse> gotDigest2 = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_GOT_PART2_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        doReturn(gotDigest2)
            .when(storageClient)
            .getBatchObjectInformation(
                anyString(),
                eq(DataCategory.OBJECTGROUP),
                eq(offerIds),
                eq(Arrays.asList("aebaaaaaaaesicexaasycalg6xcwe5aaaaaq.json"))
            );

        RequestResponseOK<BatchObjectInformationResponse> objectDigest1 = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_OBJECTS_PART1_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        RequestResponseOK<BatchObjectInformationResponse> objectDigest2 = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_OBJECTS_PART2_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        when(
            storageClient.getBatchObjectInformation(anyString(), eq(DataCategory.OBJECT), eq(offerIds), anyCollection())
        ).thenReturn(objectDigest1, objectDigest2);

        // When
        BuildObjectGroupTraceabilityActionPlugin plugin = new BuildObjectGroupTraceabilityActionPlugin(
            storageClientFactory,
            2,
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

        ArgumentCaptor<Collection> objectIds = ArgumentCaptor.forClass(Collection.class);
        verify(storageClient, times(2)).getBatchObjectInformation(
            anyString(),
            eq(DataCategory.OBJECT),
            eq(offerIds),
            objectIds.capture()
        );
        assertThat(objectIds.getAllValues().get(0)).containsExactlyInAnyOrder(
            "aeaaaaaaaaesicexaasycalg6xcwe5yaaaba",
            "aeaaaaaaaaesicexaasycalg6xcwe6iaaaaq"
        );
        assertThat(objectIds.getAllValues().get(1)).containsExactlyInAnyOrder("aeaaaaaaaaesicexaasycalg6xcwe4yaaaaq");

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

        RequestResponseOK<BatchObjectInformationResponse> gotDigest = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_GOTS_BAD_DIGEST_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        doReturn(gotDigest)
            .when(storageClient)
            .getBatchObjectInformation(
                anyString(),
                eq(DataCategory.OBJECTGROUP),
                eq(offerIds),
                eq(
                    Arrays.asList(
                        "aebaaaaaaaesicexaasycalg6xcwe6qaaaaq.json",
                        "aebaaaaaaaesicexaasycalg6xcwe6aaaaaq.json",
                        "aebaaaaaaaesicexaasycalg6xcwe5aaaaaq.json"
                    )
                )
            );

        RequestResponseOK<BatchObjectInformationResponse> objectDigest = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_OBJECTS_BAD_DIGEST_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        doReturn(objectDigest)
            .when(storageClient)
            .getBatchObjectInformation(anyString(), eq(DataCategory.OBJECT), eq(offerIds), anyCollection());

        // When
        BuildObjectGroupTraceabilityActionPlugin plugin = new BuildObjectGroupTraceabilityActionPlugin(
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

        ArgumentCaptor<Collection> objectIds = ArgumentCaptor.forClass(Collection.class);
        verify(storageClient).getBatchObjectInformation(
            anyString(),
            eq(DataCategory.OBJECT),
            eq(offerIds),
            objectIds.capture()
        );
        assertThat(objectIds.getValue()).containsExactlyInAnyOrder(
            "aeaaaaaaaaesicexaasycalg6xcwe5yaaaba",
            "aeaaaaaaaaesicexaasycalg6xcwe4yaaaaq",
            "aeaaaaaaaaesicexaasycalg6xcwe6iaaaaq"
        );

        // 2x2 alerts (WARN for missing digest and ERROR for digest mismatch)
        verify(alertService, times(2)).createAlert(eq(VitamLogLevel.ERROR), anyString());
        verify(alertService, times(2)).createAlert(eq(VitamLogLevel.WARN), anyString());
    }

    @Test
    @RunWithCustomExecutor
    public void givenLfcAndMetadataWithAllBadStorageMetadataDigestWhenExecuteThenReturnResponseOK() throws Exception {
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

        RequestResponseOK<BatchObjectInformationResponse> gotDigest = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_GOTS_ALL_BAD_DIGEST_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        doReturn(gotDigest)
            .when(storageClient)
            .getBatchObjectInformation(
                anyString(),
                eq(DataCategory.OBJECTGROUP),
                eq(offerIds),
                eq(
                    Arrays.asList(
                        "aebaaaaaaaesicexaasycalg6xcwe6qaaaaq.json",
                        "aebaaaaaaaesicexaasycalg6xcwe6aaaaaq.json",
                        "aebaaaaaaaesicexaasycalg6xcwe5aaaaaq.json"
                    )
                )
            );

        RequestResponseOK<BatchObjectInformationResponse> objectDigest = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_OBJECTS_BAD_DIGEST_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        doReturn(objectDigest)
            .when(storageClient)
            .getBatchObjectInformation(anyString(), eq(DataCategory.OBJECT), eq(offerIds), anyCollection());

        // When
        BuildObjectGroupTraceabilityActionPlugin plugin = new BuildObjectGroupTraceabilityActionPlugin(
            storageClientFactory,
            1000,
            alertService
        );
        ItemStatus statusCode = plugin.execute(params, handler);

        // Then
        assertThat(statusCode.getGlobalStatus()).isEqualTo(StatusCode.KO);

        // 2x2 alerts (WARN for missing digest and ERROR for digest mismatch)
        verify(alertService, times(2)).createAlert(eq(VitamLogLevel.ERROR), anyString());
        verify(alertService, times(1)).createAlert(eq(VitamLogLevel.WARN), anyString());
    }

    @Test
    @RunWithCustomExecutor
    public void givenLfcAndMetadata_With_AllBadStorageObjectDigests_When_ExecuteThenReturnResponseKO()
        throws Exception {
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

        RequestResponseOK<BatchObjectInformationResponse> gotDigest = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_GOTS_BAD_DIGEST_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        doReturn(gotDigest)
            .when(storageClient)
            .getBatchObjectInformation(
                anyString(),
                eq(DataCategory.OBJECTGROUP),
                eq(offerIds),
                eq(
                    Arrays.asList(
                        "aebaaaaaaaesicexaasycalg6xcwe6qaaaaq.json",
                        "aebaaaaaaaesicexaasycalg6xcwe6aaaaaq.json",
                        "aebaaaaaaaesicexaasycalg6xcwe5aaaaaq.json"
                    )
                )
            );

        RequestResponseOK<BatchObjectInformationResponse> objectDigest = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_OBJECTS_ALL_BAD_DIGEST_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        doReturn(objectDigest)
            .when(storageClient)
            .getBatchObjectInformation(anyString(), eq(DataCategory.OBJECT), eq(offerIds), anyCollection());

        // When
        BuildObjectGroupTraceabilityActionPlugin plugin = new BuildObjectGroupTraceabilityActionPlugin(
            storageClientFactory,
            1000,
            alertService
        );
        ItemStatus statusCode = plugin.execute(params, handler);

        // Then
        assertThat(statusCode.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(statusCode.getData("eventDetailData")).isEqualTo(
            "{\"error\":\"There are at least1 objects with inconsistent digest between database and offers\",\"idObjectKo\":[\"aeaaaaaaaaesicexaasycalg6xcwe5yaaaba\"]}"
        );

        // 2x2 alerts (WARN for missing digest and ERROR for digest mismatch)
        verify(alertService, times(3)).createAlert(eq(VitamLogLevel.ERROR), anyString());
        verify(alertService, times(2)).createAlert(eq(VitamLogLevel.WARN), anyString());
    }

    @Test
    @RunWithCustomExecutor
    public void givenLfcAndMetadata_With_AllBadStorageObjectDigests_GotDigest_When_ExecuteThenReturnResponseKO()
        throws Exception {
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

        RequestResponseOK<BatchObjectInformationResponse> gotDigest = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_GOTS_ALL_BAD_DIGEST_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        doReturn(gotDigest)
            .when(storageClient)
            .getBatchObjectInformation(
                anyString(),
                eq(DataCategory.OBJECTGROUP),
                eq(offerIds),
                eq(
                    Arrays.asList(
                        "aebaaaaaaaesicexaasycalg6xcwe6qaaaaq.json",
                        "aebaaaaaaaesicexaasycalg6xcwe6aaaaaq.json",
                        "aebaaaaaaaesicexaasycalg6xcwe5aaaaaq.json"
                    )
                )
            );

        RequestResponseOK<BatchObjectInformationResponse> objectDigest = JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream(BATCH_DIGESTS_OBJECTS_ALL_BAD_DIGEST_FILE),
            RequestResponseOK.class,
            BatchObjectInformationResponse.class
        );
        doReturn(objectDigest)
            .when(storageClient)
            .getBatchObjectInformation(anyString(), eq(DataCategory.OBJECT), eq(offerIds), anyCollection());

        // When
        BuildObjectGroupTraceabilityActionPlugin plugin = new BuildObjectGroupTraceabilityActionPlugin(
            storageClientFactory,
            1000,
            alertService
        );
        ItemStatus statusCode = plugin.execute(params, handler);

        // Then
        assertThat(statusCode.getGlobalStatus()).isEqualTo(StatusCode.KO);
        assertThat(statusCode.getData("eventDetailData")).isEqualTo(
            "{\"error\":\"There are at least1 metadata with inconsistent digest between database and offers\",\"idObjectKo\":[\"aebaaaaaaaesicexaasycalg6xcwe6qaaaaq\"]}"
        );

        // 2x2 alerts (WARN for missing digest and ERROR for digest mismatch)
        verify(alertService, times(2)).createAlert(eq(VitamLogLevel.ERROR), anyString());
        verify(alertService, times(1)).createAlert(eq(VitamLogLevel.WARN), anyString());
    }
}
