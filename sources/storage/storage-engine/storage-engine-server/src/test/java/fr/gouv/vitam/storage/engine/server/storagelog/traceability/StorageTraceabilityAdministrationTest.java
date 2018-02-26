/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.storage.engine.server.storagelog.traceability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.timestamp.TimestampGenerator;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.model.TraceabilityType;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.traceability.LogbookTraceabilityHelper;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.server.storagetraceability.StorageTraceabilityAdministration;
import fr.gouv.vitam.storage.engine.server.storagetraceability.StorageTraceabilityIterator;
import fr.gouv.vitam.storage.engine.server.storagetraceability.TraceabilityLogbookService;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;


public class StorageTraceabilityAdministrationTest {

    public static final Integer OVERLAP_DELAY = 300;
    private static final String BACKUP_FILE = "0_storage_logbook_20180216185352124_20180219130012834_aecaaaaaacfuexr3aav7ialbvythviqaaaaq.log";
    private static final String TRACEABILITY_FILE = "0_StorageTraceability_20180220_031002.zip";
    private static final String STRATEGY_ID = "default";
    private static final Integer tenantId = 0;

    private static LogbookOperationsClient logbookOperationsClient;

    @Rule
    public RunWithCustomExecutorRule runInThread =
    new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        logbookOperationsClient = mock(LogbookOperationsClient.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_secure_file_without_element() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // Given
        byte[] hash = {1, 2, 3, 4};
        File file = folder.newFolder();

        TimestampGenerator timestampGenerator = mock(TimestampGenerator.class);
        WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);
        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
        TraceabilityLogbookService traceabilityLogbookService = mock(TraceabilityLogbookService.class);
        ArgumentCaptor<byte[]> hashCapture = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<LogbookOperationParameters> captor = ArgumentCaptor.forClass(LogbookOperationParameters.class);

        StorageTraceabilityIterator iterator = new StorageTraceabilityIterator(new ArrayList<OfferLog>());
        given(timestampGenerator.generateToken(hashCapture.capture(),
            eq(DigestType.SHA512), eq(null))).willReturn(hash);
        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);
        given(
            traceabilityLogbookService.getLastSaved("default", LogbookTraceabilityHelper.INITIAL_START_DATE))
            .willReturn(iterator);

        StorageTraceabilityAdministration storageAdministration =
            new StorageTraceabilityAdministration(traceabilityLogbookService, logbookOperationsClient,
                file, workspaceClient, timestampGenerator, OVERLAP_DELAY);

        // When
        storageAdministration.generateTraceabilityStorageLogbook();

        // Then
        verify(logbookOperationsClient).update(captor.capture());
        LogbookOperationParameters log = captor.getValue();
        String outcome = log.getMapParameters().get(LogbookParameterName.outcome);
        assertEquals(StatusCode.WARNING.name(), outcome);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_secure_file_with_one_element() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // Given
        byte[] hash = {1, 2, 3, 4};
        File file = folder.newFolder();

        TimestampGenerator timestampGenerator = mock(TimestampGenerator.class);
        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
        StorageClientFactory storageClientFactory = mock(StorageClientFactory.class);
        StorageClient storageClient = mock(StorageClient.class);
        TraceabilityLogbookService traceabilityLogbookService = mock(TraceabilityLogbookService.class);

        Path archive = Paths.get(file.getAbsolutePath(), "archive.zip");
        Path archive2= Paths.get(file.getAbsolutePath(), "archive2.zip");

        AtomicInteger atomicInteger = new AtomicInteger();
        doAnswer(invocation -> {
            int call = atomicInteger.incrementAndGet();
            if (call == 1) {
                InputStream argumentAt = invocation.getArgumentAt(2, InputStream.class);
                Files.copy(argumentAt, archive);
            } else {
                InputStream argumentAt = invocation.getArgumentAt(2, InputStream.class);
                Files.copy(argumentAt, archive2);
            }
            return null;
        }).when(workspaceClient).putObject(anyString(), anyString(), any(InputStream.class));
        doAnswer(invocation -> {
            String container = invocation.getArgumentAt(0, String.class);
            System.out.println("Test in createContainer: " + container);
            return null;
        }).when(workspaceClient).createContainer(anyString());

        given(timestampGenerator.generateToken(any(byte[].class),
            eq(DigestType.SHA512), eq(null))).willReturn(hash);

        List<OfferLog> logs = new ArrayList<>();
        logs.add(new OfferLog(DataCategory.STORAGELOG.getFolder(), BACKUP_FILE, "Action"));
        StorageTraceabilityIterator iterator = new StorageTraceabilityIterator(logs);
        given(traceabilityLogbookService.getLastSaved(STRATEGY_ID, LogbookTraceabilityHelper.INITIAL_START_DATE))
            .willReturn(iterator);

        final Response response = new AbstractMockClient.FakeInboundResponse(Status.OK,
            new ByteArrayInputStream("test".getBytes()), MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
        given(
            traceabilityLogbookService.getObject(STRATEGY_ID, BACKUP_FILE, DataCategory.STORAGELOG))
            .willReturn(response);
        given(traceabilityLogbookService.parseDateFromFileName(anyString(), any(Boolean.class))).willCallRealMethod();

        given(
            storageClient.storeFileFromWorkspace(eq(STRATEGY_ID), eq(DataCategory.STORAGETRACEABILITY), anyString(), any(ObjectDescription.class)))
            .willReturn(new StoredInfoResult());
        given(storageClientFactory.getClient()).willReturn(storageClient);

        StorageTraceabilityAdministration storageAdministration =
            new StorageTraceabilityAdministration(traceabilityLogbookService, logbookOperationsClient,
                file, workspaceClient, timestampGenerator, OVERLAP_DELAY);

        // insert initial event
        storageAdministration.generateTraceabilityStorageLogbook();

        TraceabilityEvent lastEvent = extractLastTimestampToken(logbookOperationsClient, true);

        given(traceabilityLogbookService.getLastTraceability(STRATEGY_ID)).willReturn(TRACEABILITY_FILE);
        final Response traceabilityResponse = new AbstractMockClient.FakeInboundResponse(Status.OK,
            new ByteArrayInputStream(IOUtils.toByteArray(Files.newInputStream(archive))),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
        given(
            traceabilityLogbookService.getObject(STRATEGY_ID, TRACEABILITY_FILE, DataCategory.STORAGETRACEABILITY))
            .willReturn(traceabilityResponse);

        List<OfferLog> traceabilities = new ArrayList<>();
        traceabilities.add(new OfferLog(DataCategory.STORAGELOG.getFolder(), BACKUP_FILE, "Action"));
        StorageTraceabilityIterator iterator2 = new StorageTraceabilityIterator(traceabilities);
        given(traceabilityLogbookService.getLastSaved(eq(STRATEGY_ID), any(LocalDateTime.class)))
            .willReturn(iterator2);

        // When
        storageAdministration.generateTraceabilityStorageLogbook();

        // Then
        assertThat(archive2).exists();
        validateFile(archive2, 1, BaseXx.getBase64(new String(lastEvent.getTimeStampToken()).getBytes()));
    }

    private TraceabilityEvent extractLastTimestampToken(LogbookOperationsClient logbookOperations, Boolean check)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException, InvalidParseOperationException {
        ArgumentCaptor<LogbookOperationParameters> captor = ArgumentCaptor.forClass(LogbookOperationParameters.class);

        verify(logbookOperationsClient, atLeastOnce()).update(captor.capture());
        LogbookOperationParameters log = captor.getValue();
        String evDetData = log.getParameterValue(LogbookParameterName.eventDetailData);
        TraceabilityEvent event = JsonHandler.getFromString(evDetData, TraceabilityEvent.class);

        if (check) {
            assertNotNull(event.getSize());
            assertEquals(TraceabilityType.STORAGE, event.getLogType());
        }

        return event;
    }

    private void validateFile(Path path, int numberOfElement, String previousHash)
        throws IOException, ArchiveException {
        try (ArchiveInputStream archiveInputStream =
            new ArchiveStreamFactory()
                .createArchiveInputStream(ArchiveStreamFactory.ZIP, Files.newInputStream(path))) {

            ArchiveEntry entry = archiveInputStream.getNextEntry();

            assertThat(entry.getName()).isEqualTo("data.txt");
            byte[] bytes = IOUtils.toByteArray(archiveInputStream);
            assertThat(new String(bytes)).hasLineCount(numberOfElement);

            entry = archiveInputStream.getNextEntry();
            assertThat(entry.getName()).isEqualTo("merkleTree.json");

            entry = archiveInputStream.getNextEntry();
            assertThat(entry.getName()).isEqualTo("token.tsp");

            entry = archiveInputStream.getNextEntry();
            assertThat(entry.getName()).isEqualTo("computing_information.txt");
            Properties properties = new Properties();
            properties.load(archiveInputStream);
            assertThat(properties.getProperty("currentHash")).isNotNull();
            assertThat(properties.getProperty("previousTimestampToken")).isEqualTo(previousHash);

            entry = archiveInputStream.getNextEntry();
            assertThat(entry.getName()).isEqualTo("additional_information.txt");
            properties = new Properties();
            properties.load(archiveInputStream);
            assertThat(properties.getProperty("numberOfElement")).contains(Integer.toString(numberOfElement));
        }
    }

}
