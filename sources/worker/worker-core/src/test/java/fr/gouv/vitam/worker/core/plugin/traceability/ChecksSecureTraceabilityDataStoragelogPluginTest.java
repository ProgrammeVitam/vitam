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

package fr.gouv.vitam.worker.core.plugin.traceability;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.model.entry.TraceabilityReportEntry;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogInfoModel;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.WorkspaceConstants;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.model.TraceabilityType;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.batch.report.model.ReportStatus.KO;
import static fr.gouv.vitam.batch.report.model.ReportStatus.OK;
import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;
import static fr.gouv.vitam.common.model.WorkspaceConstants.DATA_FILE;
import static fr.gouv.vitam.common.model.WorkspaceConstants.TRACEABILITY_OPERATION_DIRECTORY;
import static java.io.File.separator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChecksSecureTraceabilityDataStoragelogPluginTest extends ActionHandler {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    StorageClientFactory storageClientFactory;

    @Mock
    StorageClient storageClient;

    @Mock
    HandlerIO handler;

    @Mock
    WorkerParameters param;

    private ChecksSecureTraceabilityDataStoragelogPlugin checksSecureTraceabilityDataStoragelogPlugin;
    private File reportTempFile;

    private static final String TRACEABILITY_OPERATION_EVENT_JSON_FILE =
        "linkedCheckTraceability/traceability_operation_event.json";
    private static final String TRACEABILITY_STORAGE_EVENT_JSON_FILE =
        "linkedCheckTraceability/traceability_storage_event.json";

    private static final String DEFAULT_OFFER_ID = "default";
    private static final String SECONDARY_OFFER_ID = "secondary";
    private static final List<String> OFFER_IDS = List.of(DEFAULT_OFFER_ID, SECONDARY_OFFER_ID);

    private static final String FILE_NAME = "file.zip";
    private static final String REPORT_FILENAME = "report";

    private static final String OBJECT_NAME = "OBJECT_NAME";
    private static final String FILE_CONTENT = "toto";

    private static final String HASH =
        "10e06b990d44de0091a2113fd95c92fc905166af147aa7632639c41aa7f26b1620c47443813c605b924c05591c161ecc35944fc69c4433a49d10fc6b04a33611";
    private static final String DIGEST = "digest";

    @Before
    public void setUp() throws Exception {
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        checksSecureTraceabilityDataStoragelogPlugin = new ChecksSecureTraceabilityDataStoragelogPlugin(
            storageClientFactory
        );

        lenient().when(param.getObjectName()).thenReturn(OBJECT_NAME);
        lenient()
            .when(handler.getJsonFromWorkspace(eq(param.getObjectName() + separator + WorkspaceConstants.REPORT)))
            .thenReturn(createObjectNode());

        final File dummy = temporaryFolder.newFile();
        when(handler.getNewLocalFile(anyString())).thenReturn(dummy);

        reportTempFile = temporaryFolder.newFile(REPORT_FILENAME);
        lenient().when(handler.getNewLocalFile(endsWith(WorkspaceConstants.REPORT))).thenReturn(reportTempFile);
    }

    private void preapre() throws Exception {
        final File traceabilityFile = PropertiesUtils.getResourceFile(TRACEABILITY_STORAGE_EVENT_JSON_FILE);
        when(handler.getInput(eq(0))).thenReturn(traceabilityFile);

        TraceabilityEvent event = new TraceabilityEvent(
            TraceabilityType.OPERATION,
            null,
            null,
            HASH,
            null,
            null,
            null,
            null,
            1,
            FILE_NAME,
            0,
            null,
            false,
            null,
            null
        );
        when(
            handler.getJsonFromWorkspace(
                eq(TRACEABILITY_OPERATION_DIRECTORY + separator + param.getObjectName() + separator + DATA_FILE)
            )
        ).thenReturn(JsonHandler.toJsonNode(event));

        when(storageClient.getOffers(VitamConfiguration.getDefaultStrategy())).thenReturn(OFFER_IDS);
    }

    @Test
    public void should_skip_operation_traceability_event() throws Exception {
        final File traceabilityFile = PropertiesUtils.getResourceFile(TRACEABILITY_OPERATION_EVENT_JSON_FILE);
        when(handler.getInput(eq(0))).thenReturn(traceabilityFile);

        ItemStatus itemStatus = checksSecureTraceabilityDataStoragelogPlugin.execute(param, handler);

        assertEquals(itemStatus.getGlobalStatus(), StatusCode.OK);
        verify(storageClient, times(0)).exists(anyString(), any(DataCategory.class), anyString(), anyList());
        verify(storageClient, times(0)).getContainerAsync(anyString(), anyString(), any(DataCategory.class), any());
    }

    @Test
    public void test_when_security_file_doesnt_exists() throws Exception {
        preapre();
        // Given
        when(
            storageClient.exists(
                eq(VitamConfiguration.getDefaultStrategy()),
                eq(DataCategory.STORAGELOG),
                eq(FILE_NAME),
                eq(OFFER_IDS)
            )
        ).thenReturn(Map.ofEntries(Map.entry(DEFAULT_OFFER_ID, false), Map.entry(SECONDARY_OFFER_ID, true)));
        // When
        ItemStatus itemStatus = checksSecureTraceabilityDataStoragelogPlugin.execute(param, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);

        //check report file
        JsonNode report = JsonHandler.getFromFile(reportTempFile);
        TraceabilityReportEntry reportEntry = JsonHandler.getFromJsonNode(report, TraceabilityReportEntry.class);
        assertEquals(KO.name(), reportEntry.getStatus());
    }

    @Test
    public void test_when_hashes_are_empty() throws Exception {
        preapre();

        // Given
        when(
            storageClient.exists(
                eq(VitamConfiguration.getDefaultStrategy()),
                eq(DataCategory.STORAGELOG),
                eq(FILE_NAME),
                eq(OFFER_IDS)
            )
        ).thenReturn(Map.ofEntries(Map.entry(DEFAULT_OFFER_ID, true), Map.entry(SECONDARY_OFFER_ID, true)));
        when(
            storageClient.getInformation(
                eq(VitamConfiguration.getDefaultStrategy()),
                eq(DataCategory.STORAGELOG),
                eq(FILE_NAME),
                eq(OFFER_IDS),
                anyBoolean()
            )
        ).thenReturn(createObjectNode());
        // When
        ItemStatus itemStatus = checksSecureTraceabilityDataStoragelogPlugin.execute(param, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);

        //check report file
        JsonNode report = JsonHandler.getFromFile(reportTempFile);
        TraceabilityReportEntry reportEntry = JsonHandler.getFromJsonNode(report, TraceabilityReportEntry.class);
        assertEquals(KO.name(), reportEntry.getStatus());
    }

    @Test
    public void test_when_hashes_are_not_similar() throws Exception {
        preapre();

        when(
            storageClient.exists(
                eq(VitamConfiguration.getDefaultStrategy()),
                eq(DataCategory.STORAGELOG),
                eq(FILE_NAME),
                eq(OFFER_IDS)
            )
        ).thenReturn(Map.ofEntries(Map.entry(DEFAULT_OFFER_ID, true), Map.entry(SECONDARY_OFFER_ID, true)));
        String fakeHash = "HASH";
        when(
            storageClient.getInformation(
                eq(VitamConfiguration.getDefaultStrategy()),
                eq(DataCategory.STORAGELOG),
                eq(FILE_NAME),
                eq(Arrays.asList(DEFAULT_OFFER_ID, SECONDARY_OFFER_ID)),
                anyBoolean()
            )
        ).thenReturn(
            createObjectNode()
                .setAll(
                    Map.of(
                        DEFAULT_OFFER_ID,
                        createObjectNode().put(DIGEST, HASH),
                        SECONDARY_OFFER_ID,
                        createObjectNode().put(DIGEST, fakeHash)
                    )
                )
        );

        // When
        ItemStatus itemStatus = checksSecureTraceabilityDataStoragelogPlugin.execute(param, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);

        //check report file
        JsonNode report = JsonHandler.getFromFile(reportTempFile);
        TraceabilityReportEntry reportEntry = JsonHandler.getFromJsonNode(report, TraceabilityReportEntry.class);
        assertEquals(KO.name(), reportEntry.getStatus());
    }

    @Test
    public void should_verify_storage_operation_event_without_error() throws Exception {
        preapre();

        when(
            storageClient.exists(
                eq(VitamConfiguration.getDefaultStrategy()),
                eq(DataCategory.STORAGELOG),
                eq(FILE_NAME),
                eq(OFFER_IDS)
            )
        ).thenReturn(Map.ofEntries(Map.entry(DEFAULT_OFFER_ID, true), Map.entry(SECONDARY_OFFER_ID, true)));
        when(
            storageClient.getInformation(
                eq(VitamConfiguration.getDefaultStrategy()),
                eq(DataCategory.STORAGELOG),
                eq(FILE_NAME),
                eq(OFFER_IDS),
                anyBoolean()
            )
        ).thenReturn(
            createObjectNode()
                .setAll(
                    Map.of(
                        DEFAULT_OFFER_ID,
                        createObjectNode().put(DIGEST, HASH),
                        SECONDARY_OFFER_ID,
                        createObjectNode().put(DIGEST, HASH)
                    )
                )
        );

        Response response = mock(Response.class);
        when(response.readEntity(eq(InputStream.class))).thenReturn(new ByteArrayInputStream(FILE_CONTENT.getBytes()));

        when(
            storageClient.getContainerAsync(
                anyString(),
                eq(FILE_NAME),
                eq(DataCategory.STORAGELOG),
                any(AccessLogInfoModel.class)
            )
        ).thenReturn(response);
        // When
        ItemStatus itemStatus = checksSecureTraceabilityDataStoragelogPlugin.execute(param, handler);

        // Then
        assertEquals(itemStatus.getGlobalStatus(), StatusCode.OK);
        verify(storageClient).exists(anyString(), any(DataCategory.class), anyString(), anyList());
        verify(storageClient).getContainerAsync(anyString(), anyString(), any(DataCategory.class), any());

        JsonNode report = JsonHandler.getFromFile(reportTempFile);
        TraceabilityReportEntry reportEntry = JsonHandler.getFromJsonNode(report, TraceabilityReportEntry.class);
        assertEquals(OK.name(), reportEntry.getStatus());
    }
}
