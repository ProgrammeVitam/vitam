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

import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
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
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.model.TraceabilityType;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.server.storagetraceability.StorageTraceabilityAdministration;
import fr.gouv.vitam.storage.engine.server.storagetraceability.TraceabilityStorageService;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.collections4.IteratorUtils;
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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;


public class StorageTraceabilityAdministrationTest {

    public static final Integer OVERLAP_DELAY = 300;
    private static final String BACKUP_FILE =
        "0_storage_logbook_20180216185352124_20180219130012834_aecaaaaaacfuexr3aav7ialbvythviqaaaaq.log";
    private static final String BACKUP_FILE_2 =
        "0_storage_logbook_20190202130012834_20180221150319854_aefqaaaaaahm23svabqogaljlfhrmpiaaaaq.log";
    private static final String TRACEABILITY_FILE_MISSING = "0_StorageTraceability_20180101_123456.zip";
    private static final String TRACEABILITY_FILE = "0_StorageTraceability_20180220_031002.zip";
    private static final String STRATEGY_ID = "default";
    private static final Integer tenantId = 0;


    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private LogbookOperationsClient logbookOperationsClient;
    @Mock
    private TimestampGenerator timestampGenerator;
    @Mock
    private WorkspaceClientFactory workspaceClientFactory;
    @Mock
    private WorkspaceClient workspaceClient;
    @Mock
    private TraceabilityStorageService traceabilityLogbookService;
    @Mock
    private StorageClient storageClient;
    @Mock
    private StorageClientFactory storageClientFactory;

    private ArgumentCaptor<LogbookOperationParameters> captor =
        ArgumentCaptor.forClass(LogbookOperationParameters.class);

    @Before
    public void setUp() throws Exception {
        given(storageClientFactory.getClient()).willReturn(storageClient);
        given(workspaceClientFactory.getClient()).willReturn(workspaceClient);

        byte[] hash = {1, 2, 3, 4};
        given(timestampGenerator.generateToken(any(byte[].class),
            eq(DigestType.SHA512), eq(null))).willReturn(hash);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_secure_file_without_element() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // Given
        File file = folder.newFolder();

        // First traceability
        given(traceabilityLogbookService.getLastTraceabilityZip(STRATEGY_ID))
            .willReturn(null);
        // No storage log files found
        given(traceabilityLogbookService.getLastSavedStorageLogIterator(STRATEGY_ID))
            .willReturn(IteratorUtils.emptyIterator());

        StorageTraceabilityAdministration storageAdministration =
            new StorageTraceabilityAdministration(traceabilityLogbookService, logbookOperationsClient,
                file, workspaceClient, timestampGenerator, OVERLAP_DELAY);

        // When
        GUID guid = GUIDFactory.newOperationLogbookGUID(tenantId);
        storageAdministration.generateTraceabilityStorageLogbook(guid);

        // Then
        LogbookOperationParameters log = getMasterLogbookOperations().get(0);
        checkOutcome(log, StatusCode.WARNING);
        assertThat(getTraceabilityEvent(log)).isNull();
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_secure_file_with_one_element() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // Given
        File file = folder.newFolder();

        Path archive = Paths.get(file.getAbsolutePath(), "archive.zip");
        Path archive2 = Paths.get(file.getAbsolutePath(), "archive2.zip");

        AtomicInteger atomicInteger = new AtomicInteger();
        doAnswer(invocation -> {
            int call = atomicInteger.incrementAndGet();
            if (call == 1) {
                InputStream argumentAt = invocation.getArgument(2);
                Files.copy(argumentAt, archive);
            } else {
                InputStream argumentAt = invocation.getArgument(2);
                Files.copy(argumentAt, archive2);
            }
            return null;
        }).when(workspaceClient).putObject(anyString(), anyString(), any(InputStream.class));

        // First traceability
        given(traceabilityLogbookService.getLastTraceabilityZip(STRATEGY_ID))
            .willReturn(null);
        // One storage log file
        given(traceabilityLogbookService.getLastSavedStorageLogIterator(STRATEGY_ID))
            .willReturn(IteratorUtils.singletonIterator(offerLogFor(BACKUP_FILE)));

        doAnswer(o -> fakeResponse("test"))
            .when(traceabilityLogbookService).getObject(STRATEGY_ID, BACKUP_FILE, DataCategory.STORAGELOG);

        given(storageClient.storeFileFromWorkspace(eq(STRATEGY_ID), eq(DataCategory.STORAGETRACEABILITY),
            anyString(), any(ObjectDescription.class)))
            .willReturn(new StoredInfoResult());

        StorageTraceabilityAdministration storageAdministration =
            new StorageTraceabilityAdministration(traceabilityLogbookService, logbookOperationsClient,
                file, workspaceClient, timestampGenerator, OVERLAP_DELAY);

        // insert initial event
        GUID guid = GUIDFactory.newOperationLogbookGUID(tenantId);
        storageAdministration.generateTraceabilityStorageLogbook(guid);

        TraceabilityEvent lastEvent = extractLastTimestampToken();

        // Existing traceability file
        given(traceabilityLogbookService.getLastTraceabilityZip(STRATEGY_ID)).
            willReturn(TRACEABILITY_FILE);

        given(traceabilityLogbookService.getObject(STRATEGY_ID, TRACEABILITY_FILE, DataCategory.STORAGETRACEABILITY))
            .willReturn(fakeResponse(Files.newInputStream(archive)));

        given(traceabilityLogbookService.getLastSavedStorageLogIterator(STRATEGY_ID))
            .willReturn(IteratorUtils.singletonIterator(offerLogFor(BACKUP_FILE_2)));

        doAnswer(o -> fakeResponse("test-2"))
            .when(traceabilityLogbookService).getObject(STRATEGY_ID, BACKUP_FILE_2, DataCategory.STORAGELOG);

        // When
        GUID guid2 = GUIDFactory.newOperationLogbookGUID(tenantId);
        storageAdministration.generateTraceabilityStorageLogbook(guid2);

        // Then
        assertThat(archive2).
            exists();

        String previousHash = BaseXx.getBase64(new String(lastEvent.getTimeStampToken()).getBytes());
        validateFile(archive2, 1, previousHash);

        List<LogbookOperationParameters> logs = getMasterLogbookOperations();

        assertThat(logs).hasSize(2);
        LogbookOperationParameters lastTraceability = logs.get(1);
        checkOutcome(lastTraceability, StatusCode.OK);
        TraceabilityEvent traceabilityEvent = getTraceabilityEvent(lastTraceability);
        assertThat(traceabilityEvent).isNotNull();
    }

    private TraceabilityEvent getTraceabilityEvent(LogbookOperationParameters logbookOperationParameters)
        throws InvalidParseOperationException {
        String evDetData = logbookOperationParameters.getMapParameters().get(LogbookParameterName.masterData);
        if (evDetData == null) {
            return null;
        }
        return JsonHandler.getFromString(evDetData, TraceabilityEvent.class);
    }

    private void checkOutcome(LogbookOperationParameters lastTraceability, StatusCode ok) {
        String outcome = lastTraceability.getMapParameters().get(LogbookParameterName.outcome);
        assertEquals(ok.name(), outcome);
    }

    private List<LogbookOperationParameters> getMasterLogbookOperations()
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        verify(logbookOperationsClient, Mockito.atLeastOnce()).update(captor.capture());
        return captor.getAllValues().stream()
            .filter(l -> l.getMapParameters().get(LogbookParameterName.eventType).equals("STP_STORAGE_SECURISATION"))
            .collect(Collectors.toList());
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_previous_secure_file_not_found() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // Given
        File file = folder.newFolder();

        Path archive = Paths.get(file.getAbsolutePath(), "archive.zip");

        doAnswer(args -> {
            Files.copy((InputStream) args.getArgument(2), archive);
            return null;
        }).when(workspaceClient).putObject(anyString(), anyString(), any(InputStream.class));

        given(traceabilityLogbookService
            .getObject(STRATEGY_ID, TRACEABILITY_FILE_MISSING, DataCategory.STORAGETRACEABILITY))
            .willThrow(new StorageNotFoundException(""));

        given(traceabilityLogbookService.getLastTraceabilityZip(STRATEGY_ID))
            .willReturn(TRACEABILITY_FILE_MISSING);

        // One storage log file
        given(traceabilityLogbookService.getLastSavedStorageLogIterator(STRATEGY_ID))
            .willReturn(IteratorUtils.singletonIterator(offerLogFor(BACKUP_FILE)));

        given(traceabilityLogbookService.getObject(STRATEGY_ID, BACKUP_FILE, DataCategory.STORAGELOG))
            .willReturn(fakeResponse("test"));

        given(storageClient.storeFileFromWorkspace(eq(STRATEGY_ID), eq(DataCategory.STORAGETRACEABILITY),
            anyString(), any(ObjectDescription.class)))
            .willReturn(new StoredInfoResult());

        StorageTraceabilityAdministration storageAdministration =
            new StorageTraceabilityAdministration(traceabilityLogbookService, logbookOperationsClient,
                file, workspaceClient, timestampGenerator, OVERLAP_DELAY);

        // insert initial event
        GUID guid = GUIDFactory.newOperationLogbookGUID(tenantId);

        // When / Then
        assertThatThrownBy(() -> storageAdministration.generateTraceabilityStorageLogbook(guid))
            .isInstanceOf(TraceabilityException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_secure_file_with_multiple_backup_files() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // Given
        File file = folder.newFolder();

        Path archive = Paths.get(file.getAbsolutePath(), "archive.zip");

        doAnswer(args -> {
            Files.copy((InputStream) args.getArgument(2), archive);
            return null;
        }).when(workspaceClient).putObject(anyString(), anyString(), any(InputStream.class));

        // First traceability
        given(traceabilityLogbookService.getLastTraceabilityZip(STRATEGY_ID))
            .willReturn(null);

        // 2 storage log files, but only 1 exists
        given(traceabilityLogbookService.getLastSavedStorageLogIterator(STRATEGY_ID))
            .willReturn(IteratorUtils.arrayIterator(offerLogFor(BACKUP_FILE), offerLogFor(BACKUP_FILE_2)));

        given(traceabilityLogbookService.getObject(STRATEGY_ID, BACKUP_FILE, DataCategory.STORAGELOG))
            .willReturn(fakeResponse("test"));

        given(traceabilityLogbookService.getObject(STRATEGY_ID, BACKUP_FILE_2, DataCategory.STORAGELOG))
            .willReturn(fakeResponse("test-2"));

        given(storageClient.storeFileFromWorkspace(eq(STRATEGY_ID), eq(DataCategory.STORAGETRACEABILITY),
            anyString(), any(ObjectDescription.class)))
            .willReturn(new StoredInfoResult());

        StorageTraceabilityAdministration storageAdministration =
            new StorageTraceabilityAdministration(traceabilityLogbookService, logbookOperationsClient,
                file, workspaceClient, timestampGenerator, OVERLAP_DELAY);

        // insert initial event
        GUID guid = GUIDFactory.newOperationLogbookGUID(tenantId);
        storageAdministration.generateTraceabilityStorageLogbook(guid);

        TraceabilityEvent lastEvent = extractLastTimestampToken();
        assertThat(lastEvent).isNotNull();

        assertThat(archive).exists();
        validateFile(archive, 2, "null");

        List<LogbookOperationParameters> logs = getMasterLogbookOperations();

        assertThat(logs).hasSize(1);
        LogbookOperationParameters lastTraceability = logs.get(0);
        checkOutcome(lastTraceability, StatusCode.OK);
        TraceabilityEvent traceabilityEvent = getTraceabilityEvent(lastTraceability);
        assertThat(traceabilityEvent).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_with_backup_file_not_found() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        // Given
        File file = folder.newFolder();

        // First traceability
        given(traceabilityLogbookService.getLastTraceabilityZip(STRATEGY_ID))
            .willReturn(null);
        // No storage log files found
        given(traceabilityLogbookService.getLastSavedStorageLogIterator(STRATEGY_ID))
            .willReturn(IteratorUtils.singletonIterator(offerLogFor(BACKUP_FILE)));
        given(traceabilityLogbookService.getObject(STRATEGY_ID, BACKUP_FILE, DataCategory.STORAGELOG))
            .willThrow(new StorageNotFoundException(""));

        StorageTraceabilityAdministration storageAdministration =
            new StorageTraceabilityAdministration(traceabilityLogbookService, logbookOperationsClient,
                file, workspaceClient, timestampGenerator, OVERLAP_DELAY);

        GUID guid = GUIDFactory.newOperationLogbookGUID(tenantId);
        // When / Then
        assertThatThrownBy(() -> storageAdministration.generateTraceabilityStorageLogbook(guid))
            .isInstanceOf(TraceabilityException.class);
    }

    private TraceabilityEvent extractLastTimestampToken()
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException,
        InvalidParseOperationException {
        ArgumentCaptor<LogbookOperationParameters> captor = ArgumentCaptor.forClass(LogbookOperationParameters.class);

        verify(logbookOperationsClient, atLeastOnce()).update(captor.capture());
        LogbookOperationParameters log = captor.getValue();
        TraceabilityEvent event = getTraceabilityEvent(log);
        assertEquals(TraceabilityType.STORAGE, event.getLogType());

        return event;
    }

    private void validateFile(Path path, int numberOfElements, String previousHash)
        throws IOException, ArchiveException {
        try (ArchiveInputStream archiveInputStream =
            new ArchiveStreamFactory()
                .createArchiveInputStream(ArchiveStreamFactory.ZIP, Files.newInputStream(path))) {

            ArchiveEntry entry = archiveInputStream.getNextEntry();

            assertThat(entry.getName()).isEqualTo("data.txt");
            byte[] bytes = IOUtils.toByteArray(archiveInputStream);
            assertThat(new String(bytes)).hasLineCount(numberOfElements);

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
            assertThat(properties.getProperty("numberOfElements")).contains(Integer.toString(numberOfElements));
        }
    }

    private AbstractMockClient.FakeInboundResponse fakeResponse(String data) {
        return new AbstractMockClient.FakeInboundResponse(Status.OK,
            new ByteArrayInputStream(data.getBytes()), MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    private AbstractMockClient.FakeInboundResponse fakeResponse(InputStream inputStream) {
        return new AbstractMockClient.FakeInboundResponse(Status.OK,
            inputStream, MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    private OfferLog offerLogFor(String filename) {
        return new OfferLog("", filename, OfferLogAction.WRITE);
    }
}
