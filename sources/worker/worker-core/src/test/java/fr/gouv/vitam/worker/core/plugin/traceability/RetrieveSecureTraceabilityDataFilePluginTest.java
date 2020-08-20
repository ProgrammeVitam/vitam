package fr.gouv.vitam.worker.core.plugin.traceability;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.model.entry.TraceabilityReportEntry;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.WorkspaceConstants;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.model.TraceabilityType;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.batch.report.model.ReportStatus.KO;
import static fr.gouv.vitam.batch.report.model.ReportStatus.OK;
import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RetrieveSecureTraceabilityDataFilePluginTest {

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock private StorageClientFactory storageClientFactory;
    @Mock private StorageClient storageClient;
    @Mock private WorkerParameters param;
    @Mock private HandlerIO handler;

    private RetrieveSecureTraceabilityDataFilePlugin retrieveSecureTraceabilityDataFilePlugin;
    private File reportTempFile;

    private static final String DEFAULT_OFFER_ID = "default";
    private static final String SECONDARY_OFFER_ID = "secondary";
    private static final List<String> OFFER_IDS = List.of(DEFAULT_OFFER_ID, SECONDARY_OFFER_ID);


    private static final String FILE_NAME = "file.zip";
    private static final String REPORT_FILENAME = "report";
    private static final String HANDLER_OUTPUT_FILE_NAME = "handler_tmp_file";

    private static final String HASH =
        "5a95a72c714bc8c7d5b6855cf205c7dd33cac566302ab1fc3e41e2534a446746a63d5259db93138b2c9f66881fdcbbde0e38e92d78df1280ba690cf3ee8ffc37";
    private static final String DIGEST = "digest";


    @Before
    public void setUp() throws Exception {
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        retrieveSecureTraceabilityDataFilePlugin = new RetrieveSecureTraceabilityDataFilePlugin(storageClientFactory);

        TraceabilityEvent traceabilityEvent = new TraceabilityEvent(TraceabilityType.OPERATION,
            "", "", HASH, null, "", "", "", 1L, FILE_NAME, 30L, DigestType.SHA512, false, "", null);
        when(param.getObjectMetadata()).thenReturn(JsonHandler.toJsonNode(traceabilityEvent));

        when(storageClient.getOffers(VitamConfiguration.getDefaultStrategy())).thenReturn(OFFER_IDS);

        final File dummy = temporaryFolder.newFile();
        when(handler.getNewLocalFile(anyString())).thenReturn(dummy);

        reportTempFile = temporaryFolder.newFile(REPORT_FILENAME);
        when(handler.getNewLocalFile(endsWith(WorkspaceConstants.REPORT))).thenReturn(reportTempFile);
    }

    @Test
    public void test_when_security_file_doesnt_exists() throws Exception {
        // Given
        when(storageClient
            .exists(eq(VitamConfiguration.getDefaultStrategy()), eq(DataCategory.LOGBOOK), eq(FILE_NAME),
                eq(OFFER_IDS)))
            .thenReturn(Collections.singletonMap(DEFAULT_OFFER_ID, false));
        // When
        ItemStatus itemStatus = retrieveSecureTraceabilityDataFilePlugin.execute(param, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);

        //check report file
        JsonNode report = JsonHandler.getFromFile(reportTempFile);
        TraceabilityReportEntry reportEntry = JsonHandler.getFromJsonNode(report, TraceabilityReportEntry.class);
        assertEquals(TraceabilityType.OPERATION.name(), reportEntry.getOperationType());
        assertEquals(KO.name(), reportEntry.getStatus());
        assertFalse(reportEntry.getOffersHashes().containsKey(DEFAULT_OFFER_ID));
        assertEquals(FILE_NAME, reportEntry.getFileId());
    }

    @Test
    public void test_when_hashes_are_empty() throws Exception {
        // Given
        when(storageClient
            .exists(eq(VitamConfiguration.getDefaultStrategy()), eq(DataCategory.LOGBOOK), eq(FILE_NAME),
                eq(OFFER_IDS)))
            .thenReturn(Collections.singletonMap(DEFAULT_OFFER_ID, true));
        when(storageClient
            .getInformation(eq(VitamConfiguration.getDefaultStrategy()), eq(DataCategory.LOGBOOK), eq(FILE_NAME),
                eq(OFFER_IDS), anyBoolean())).thenReturn(
            createObjectNode()
        );
        // When
        ItemStatus itemStatus = retrieveSecureTraceabilityDataFilePlugin.execute(param, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);

        //check report file
        JsonNode report = JsonHandler.getFromFile(reportTempFile);
        TraceabilityReportEntry reportEntry = JsonHandler.getFromJsonNode(report, TraceabilityReportEntry.class);
        assertEquals(TraceabilityType.OPERATION.name(), reportEntry.getOperationType());
        assertEquals(KO.name(), reportEntry.getStatus());
        assertFalse(reportEntry.getOffersHashes().containsKey(DEFAULT_OFFER_ID));
        assertEquals(FILE_NAME, reportEntry.getFileId());
    }

    @Test
    public void test_when_hashes_are_not_similar() throws Exception {

        when(storageClient
            .exists(eq(VitamConfiguration.getDefaultStrategy()), eq(DataCategory.LOGBOOK), eq(FILE_NAME),
                eq(OFFER_IDS)))
            .thenReturn(Map.ofEntries(Map.entry(DEFAULT_OFFER_ID, true), Map.entry(SECONDARY_OFFER_ID, true)));
        String fakeHash = "HASH";
        when(storageClient
            .getInformation(eq(VitamConfiguration.getDefaultStrategy()), eq(DataCategory.LOGBOOK), eq(FILE_NAME),
                eq(OFFER_IDS), anyBoolean())).thenReturn(
            createObjectNode().setAll(
                Map.of(DEFAULT_OFFER_ID,
                    createObjectNode().put(DIGEST, HASH),
                    SECONDARY_OFFER_ID,
                    createObjectNode().put(DIGEST, fakeHash)
                )
            ));

        // When
        ItemStatus itemStatus = retrieveSecureTraceabilityDataFilePlugin.execute(param, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);

        //check report file
        JsonNode report = JsonHandler.getFromFile(reportTempFile);
        TraceabilityReportEntry reportEntry = JsonHandler.getFromJsonNode(report, TraceabilityReportEntry.class);
        assertEquals(TraceabilityType.OPERATION.name(), reportEntry.getOperationType());
        assertEquals(KO.name(), reportEntry.getStatus());
        assertTrue(reportEntry.getOffersHashes().containsKey(VitamConfiguration.getDefaultStrategy()));
        assertTrue(reportEntry.getOffersHashes().containsKey(DEFAULT_OFFER_ID));
        assertEquals(HASH, reportEntry.getOffersHashes().get(DEFAULT_OFFER_ID));
        assertTrue(reportEntry.getOffersHashes().containsKey(SECONDARY_OFFER_ID));
        assertEquals(fakeHash, reportEntry.getOffersHashes().get(SECONDARY_OFFER_ID));
        assertEquals(FILE_NAME, reportEntry.getFileId());
    }

    @Test
    public void test_when_multi_offer_without_error() throws Exception {

        when(storageClient
            .exists(eq(VitamConfiguration.getDefaultStrategy()), eq(DataCategory.LOGBOOK), eq(FILE_NAME),
                eq(OFFER_IDS)))
            .thenReturn(Map.ofEntries(Map.entry(DEFAULT_OFFER_ID, true), Map.entry(SECONDARY_OFFER_ID, true)));
        when(storageClient
            .getInformation(eq(VitamConfiguration.getDefaultStrategy()), eq(DataCategory.LOGBOOK), eq(FILE_NAME),
                eq(OFFER_IDS), anyBoolean())).thenReturn(
            createObjectNode().setAll(
                Map.of(DEFAULT_OFFER_ID,
                    createObjectNode().put(DIGEST, HASH),
                    SECONDARY_OFFER_ID,
                    createObjectNode().put(DIGEST, HASH)
                )
            ));

        when(handler.getOutput(anyInt())).thenReturn(mock(ProcessingUri.class));

        final File handlerTempFile = temporaryFolder.newFile(HANDLER_OUTPUT_FILE_NAME);
        when(handler.getNewLocalFile(eq(null))).thenReturn(handlerTempFile);
        // When
        ItemStatus itemStatus = retrieveSecureTraceabilityDataFilePlugin.execute(param, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        //check report file
        JsonNode report = JsonHandler.getFromFile(reportTempFile);
        TraceabilityReportEntry reportEntry = JsonHandler.getFromJsonNode(report, TraceabilityReportEntry.class);
        assertEquals(TraceabilityType.OPERATION.name(), reportEntry.getOperationType());
        assertEquals(OK.name(), reportEntry.getStatus());
        assertTrue(reportEntry.getOffersHashes().containsKey(VitamConfiguration.getDefaultStrategy()));
        assertTrue(reportEntry.getOffersHashes().containsKey(DEFAULT_OFFER_ID));
        assertEquals(HASH, reportEntry.getOffersHashes().get(DEFAULT_OFFER_ID));
        assertTrue(reportEntry.getOffersHashes().containsKey(SECONDARY_OFFER_ID));
        assertEquals(HASH, reportEntry.getOffersHashes().get(SECONDARY_OFFER_ID));
        assertEquals(FILE_NAME, reportEntry.getFileId());
    }

    @Test
    public void should_extract_digest_without_error() throws Exception {
        // Given
        when(storageClient
            .exists(eq(VitamConfiguration.getDefaultStrategy()), eq(DataCategory.LOGBOOK), eq(FILE_NAME),
                eq(OFFER_IDS)))
            .thenReturn(Collections.singletonMap(DEFAULT_OFFER_ID, true));
        when(storageClient
            .getInformation(eq(VitamConfiguration.getDefaultStrategy()), eq(DataCategory.LOGBOOK), eq(FILE_NAME),
                eq(OFFER_IDS), anyBoolean())).thenReturn(
            createObjectNode().setAll(
                Map.of(DEFAULT_OFFER_ID,
                    createObjectNode().put(DIGEST, HASH),
                    SECONDARY_OFFER_ID,
                    createObjectNode().put(DIGEST, HASH)
                )
            ));
        when(handler.getOutput(anyInt())).thenReturn(mock(ProcessingUri.class));

        final File handlerTempFile = temporaryFolder.newFile(HANDLER_OUTPUT_FILE_NAME);
        when(handler.getNewLocalFile(eq(null))).thenReturn(handlerTempFile);
        // When
        ItemStatus itemStatus = retrieveSecureTraceabilityDataFilePlugin.execute(param, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        //check report file
        JsonNode report = JsonHandler.getFromFile(reportTempFile);
        TraceabilityReportEntry reportEntry = JsonHandler.getFromJsonNode(report, TraceabilityReportEntry.class);
        assertEquals(TraceabilityType.OPERATION.name(), reportEntry.getOperationType());
        assertEquals(OK.name(), reportEntry.getStatus());
        assertTrue(reportEntry.getOffersHashes().containsKey(VitamConfiguration.getDefaultStrategy()));
        assertTrue(reportEntry.getOffersHashes().containsKey(DEFAULT_OFFER_ID));
        assertEquals(HASH, reportEntry.getOffersHashes().get(DEFAULT_OFFER_ID));
        assertEquals(FILE_NAME, reportEntry.getFileId());
    }
}