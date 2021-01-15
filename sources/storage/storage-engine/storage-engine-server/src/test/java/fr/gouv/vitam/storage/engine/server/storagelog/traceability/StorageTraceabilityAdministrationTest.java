/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.storage.engine.server.storagelog.traceability;

import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.VitamConfiguration;
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
import fr.gouv.vitam.storage.driver.model.StorageLogTraceabilityResult;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
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
    private static final String BACKUP_FILE_TENANT_1 =
        "1_storage_logbook_20180216185352124_20180219130012834_aecaaaaaacfuexr3aav7ialbvythviqaaaaq.log";

    private static final String TRACEABILITY_FILE_MISSING = "0_StorageTraceability_20180101_123456.zip";
    private static final String TRACEABILITY_FILE = "0_StorageTraceability_20180220_031002.zip";
    private static final Integer tenantId = 0;
    private static final int TRACEABILITY_THREAD_POOL_SIZE = 4;

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
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        // Given
        File file = folder.newFolder();

        // First traceability
        given(traceabilityLogbookService.getLastTraceabilityZip(VitamConfiguration.getDefaultStrategy()))
            .willReturn(null);
        // No storage log files found
        given(traceabilityLogbookService.getLastSavedStorageLogIterator(VitamConfiguration.getDefaultStrategy()))
            .willReturn(IteratorUtils.emptyIterator());

        StorageTraceabilityAdministration storageAdministration =
            new StorageTraceabilityAdministration(traceabilityLogbookService, logbookOperationsClient,
                file, workspaceClient, timestampGenerator, OVERLAP_DELAY, TRACEABILITY_THREAD_POOL_SIZE);

        // When
        List<StorageLogTraceabilityResult> results = storageAdministration
            .generateStorageLogTraceabilityOperations(VitamConfiguration.getDefaultStrategy(), Collections
                .singletonList(tenantId));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTenantId()).isEqualTo(tenantId);
        assertThat(results.get(0).getOperationId()).isNotNull();

        // Then
        LogbookOperationParameters log = getMasterLogbookOperations().get(0);
        checkOutcome(log, StatusCode.WARNING);
        assertThat(getTraceabilityEvent(log)).isNull();
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_secure_file_with_one_element() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
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
        given(traceabilityLogbookService.getLastTraceabilityZip(VitamConfiguration.getDefaultStrategy()))
            .willReturn(null);
        // One storage log file
        given(traceabilityLogbookService.getLastSavedStorageLogIterator(VitamConfiguration.getDefaultStrategy()))
            .willReturn(IteratorUtils.singletonIterator(offerLogFor(BACKUP_FILE)));

        doAnswer(o -> fakeResponse("test"))
            .when(traceabilityLogbookService).getObject(VitamConfiguration.getDefaultStrategy(), BACKUP_FILE, DataCategory.STORAGELOG);

        given(storageClient.storeFileFromWorkspace(eq(VitamConfiguration.getDefaultStrategy()), eq(DataCategory.STORAGETRACEABILITY),
            anyString(), any(ObjectDescription.class)))
            .willReturn(new StoredInfoResult());

        StorageTraceabilityAdministration storageAdministration =
            new StorageTraceabilityAdministration(traceabilityLogbookService, logbookOperationsClient,
                file, workspaceClient, timestampGenerator, OVERLAP_DELAY, TRACEABILITY_THREAD_POOL_SIZE);

        // insert initial event
        List<StorageLogTraceabilityResult> traceabilityResult1 = storageAdministration
            .generateStorageLogTraceabilityOperations(VitamConfiguration.getDefaultStrategy(), Collections
                .singletonList(tenantId));

        assertThat(traceabilityResult1).hasSize(1);
        assertThat(traceabilityResult1.get(0).getTenantId()).isEqualTo(tenantId);
        assertThat(traceabilityResult1.get(0).getOperationId()).isNotNull();

        TraceabilityEvent lastEvent = extractLastTimestampToken();

        // Existing traceability file
        given(traceabilityLogbookService.getLastTraceabilityZip(VitamConfiguration.getDefaultStrategy())).
            willReturn(TRACEABILITY_FILE);

        given(traceabilityLogbookService.getObject(VitamConfiguration.getDefaultStrategy(), TRACEABILITY_FILE, DataCategory.STORAGETRACEABILITY))
            .willReturn(fakeResponse(Files.newInputStream(archive)));

        given(traceabilityLogbookService.getLastSavedStorageLogIterator(VitamConfiguration.getDefaultStrategy()))
            .willReturn(IteratorUtils.singletonIterator(offerLogFor(BACKUP_FILE_2)));

        doAnswer(o -> fakeResponse("test-2"))
            .when(traceabilityLogbookService).getObject(VitamConfiguration.getDefaultStrategy(), BACKUP_FILE_2, DataCategory.STORAGELOG);

        // When
        List<StorageLogTraceabilityResult> traceabilityResult2 = storageAdministration
            .generateStorageLogTraceabilityOperations(VitamConfiguration.getDefaultStrategy(), Collections
                .singletonList(tenantId));

        assertThat(traceabilityResult2).hasSize(1);
        assertThat(traceabilityResult2.get(0).getTenantId()).isEqualTo(tenantId);
        assertThat(traceabilityResult2.get(0).getOperationId()).isNotNull();

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
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        // Given
        File file = folder.newFolder();

        Path archive = Paths.get(file.getAbsolutePath(), "archive.zip");

        doAnswer(args -> {
            Files.copy((InputStream) args.getArgument(2), archive);
            return null;
        }).when(workspaceClient).putObject(anyString(), anyString(), any(InputStream.class));

        given(traceabilityLogbookService
            .getObject(VitamConfiguration.getDefaultStrategy(), TRACEABILITY_FILE_MISSING, DataCategory.STORAGETRACEABILITY))
            .willThrow(new StorageNotFoundException(""));

        given(traceabilityLogbookService.getLastTraceabilityZip(VitamConfiguration.getDefaultStrategy()))
            .willReturn(TRACEABILITY_FILE_MISSING);

        // One storage log file
        given(traceabilityLogbookService.getLastSavedStorageLogIterator(VitamConfiguration.getDefaultStrategy()))
            .willReturn(IteratorUtils.singletonIterator(offerLogFor(BACKUP_FILE)));

        given(traceabilityLogbookService.getObject(VitamConfiguration.getDefaultStrategy(), BACKUP_FILE, DataCategory.STORAGELOG))
            .willReturn(fakeResponse("test"));

        given(storageClient.storeFileFromWorkspace(eq(VitamConfiguration.getDefaultStrategy()), eq(DataCategory.STORAGETRACEABILITY),
            anyString(), any(ObjectDescription.class)))
            .willReturn(new StoredInfoResult());

        StorageTraceabilityAdministration storageAdministration =
            new StorageTraceabilityAdministration(traceabilityLogbookService, logbookOperationsClient,
                file, workspaceClient, timestampGenerator, OVERLAP_DELAY, TRACEABILITY_THREAD_POOL_SIZE);

        // When / Then
        assertThatThrownBy(() -> storageAdministration.generateStorageLogTraceabilityOperations(
                VitamConfiguration.getDefaultStrategy(), Collections.singletonList(tenantId)))
            .isInstanceOf(TraceabilityException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_secure_file_with_multiple_backup_files() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        // Given
        File file = folder.newFolder();

        Path archive = Paths.get(file.getAbsolutePath(), "archive.zip");

        doAnswer(args -> {
            Files.copy((InputStream) args.getArgument(2), archive);
            return null;
        }).when(workspaceClient).putObject(anyString(), anyString(), any(InputStream.class));

        // First traceability
        given(traceabilityLogbookService.getLastTraceabilityZip(VitamConfiguration.getDefaultStrategy()))
            .willReturn(null);

        // 2 storage log files, but only 1 exists
        given(traceabilityLogbookService.getLastSavedStorageLogIterator(VitamConfiguration.getDefaultStrategy()))
            .willReturn(IteratorUtils.arrayIterator(offerLogFor(BACKUP_FILE), offerLogFor(BACKUP_FILE_2)));

        given(traceabilityLogbookService.getObject(VitamConfiguration.getDefaultStrategy(), BACKUP_FILE, DataCategory.STORAGELOG))
            .willReturn(fakeResponse("test"));

        given(traceabilityLogbookService.getObject(VitamConfiguration.getDefaultStrategy(), BACKUP_FILE_2, DataCategory.STORAGELOG))
            .willReturn(fakeResponse("test-2"));

        given(storageClient.storeFileFromWorkspace(eq(VitamConfiguration.getDefaultStrategy()), eq(DataCategory.STORAGETRACEABILITY),
            anyString(), any(ObjectDescription.class)))
            .willReturn(new StoredInfoResult());

        StorageTraceabilityAdministration storageAdministration =
            new StorageTraceabilityAdministration(traceabilityLogbookService, logbookOperationsClient,
                file, workspaceClient, timestampGenerator, OVERLAP_DELAY, TRACEABILITY_THREAD_POOL_SIZE);

        // insert initial event
        List<StorageLogTraceabilityResult> resultList = storageAdministration
            .generateStorageLogTraceabilityOperations(VitamConfiguration.getDefaultStrategy(), Collections
                .singletonList(tenantId));

        assertThat(resultList).hasSize(1);
        assertThat(resultList.get(0).getTenantId()).isEqualTo(tenantId);
        assertThat(resultList.get(0).getOperationId()).isNotNull();

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
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        // Given
        File file = folder.newFolder();

        // First traceability
        given(traceabilityLogbookService.getLastTraceabilityZip(VitamConfiguration.getDefaultStrategy()))
            .willReturn(null);
        // No storage log files found
        given(traceabilityLogbookService.getLastSavedStorageLogIterator(VitamConfiguration.getDefaultStrategy()))
            .willReturn(IteratorUtils.singletonIterator(offerLogFor(BACKUP_FILE)));
        given(traceabilityLogbookService.getObject(VitamConfiguration.getDefaultStrategy(), BACKUP_FILE, DataCategory.STORAGELOG))
            .willThrow(new StorageNotFoundException(""));

        StorageTraceabilityAdministration storageAdministration =
            new StorageTraceabilityAdministration(traceabilityLogbookService, logbookOperationsClient,
                file, workspaceClient, timestampGenerator, OVERLAP_DELAY, TRACEABILITY_THREAD_POOL_SIZE);

        GUID guid = GUIDFactory.newOperationLogbookGUID(tenantId);
        // When / Then
        assertThatThrownBy(() -> storageAdministration.generateStorageLogTraceabilityOperations(
            VitamConfiguration.getDefaultStrategy(), Collections.singletonList(tenantId)))
            .isInstanceOf(TraceabilityException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_generate_secure_file_for_multiple_tenants() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(tenantId));
        // Given
        File file = folder.newFolder();

        Path archive_tenant0 = Paths.get(file.getAbsolutePath(), "archive_tenant0.zip");
        Path archive_tenant1 = Paths.get(file.getAbsolutePath(), "archive_tenant1.zip");

        doAnswer(invocation -> {
            int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            InputStream argumentAt = invocation.getArgument(2);
            switch (tenantId) {
                case 0:
                    Files.copy(argumentAt, archive_tenant0);
                    return null;
                case 1:
                    Files.copy(argumentAt, archive_tenant1);
                    return null;
                default:
                    throw new IllegalStateException();
            }
        }).when(workspaceClient).putObject(anyString(), anyString(), any(InputStream.class));

        // First traceability
        given(traceabilityLogbookService.getLastTraceabilityZip(VitamConfiguration.getDefaultStrategy()))
            .willReturn(null);
        // Existing storage log files
        doAnswer(args -> {
            int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            switch (tenantId) {
                case 0:
                    return IteratorUtils.singletonIterator(offerLogFor(BACKUP_FILE));
                case 1:
                    return IteratorUtils.singletonIterator(offerLogFor(BACKUP_FILE_TENANT_1));
                default:
                    throw new IllegalStateException();
            }
        }).when(traceabilityLogbookService).getLastSavedStorageLogIterator(VitamConfiguration.getDefaultStrategy());

        doAnswer(o -> {
            assertThat(VitamThreadUtils.getVitamSession().getTenantId()).isEqualTo(0);
            return fakeResponse("test");
        }).when(traceabilityLogbookService).getObject(VitamConfiguration.getDefaultStrategy(), BACKUP_FILE, DataCategory.STORAGELOG);
        doAnswer(o -> {
            assertThat(VitamThreadUtils.getVitamSession().getTenantId()).isEqualTo(1);
            return fakeResponse("test");
        }).when(traceabilityLogbookService).getObject(VitamConfiguration.getDefaultStrategy(), BACKUP_FILE_TENANT_1, DataCategory.STORAGELOG);

        given(storageClient.storeFileFromWorkspace(eq(VitamConfiguration.getDefaultStrategy()), eq(DataCategory.STORAGETRACEABILITY),
            anyString(), any(ObjectDescription.class)))
            .willReturn(new StoredInfoResult());

        StorageTraceabilityAdministration storageAdministration =
            new StorageTraceabilityAdministration(traceabilityLogbookService, logbookOperationsClient,
                file, workspaceClient, timestampGenerator, OVERLAP_DELAY, TRACEABILITY_THREAD_POOL_SIZE);

        // When : Start traceability for multiple
        List<StorageLogTraceabilityResult> traceabilityResults = storageAdministration
            .generateStorageLogTraceabilityOperations(VitamConfiguration.getDefaultStrategy(), Arrays.asList(0, 1));

        // Then :
        assertThat(traceabilityResults).hasSize(2);
        assertThat(traceabilityResults.get(0).getTenantId()).isEqualTo(0);
        assertThat(traceabilityResults.get(0).getOperationId()).isNotNull();
        assertThat(traceabilityResults.get(1).getTenantId()).isEqualTo(1);
        assertThat(traceabilityResults.get(1).getOperationId()).isNotNull();

        assertThat(archive_tenant0).exists();
        assertThat(archive_tenant1).exists();
        validateFile(archive_tenant0, 1, "null");
        validateFile(archive_tenant1, 1, "null");


        Map<String, LogbookOperationParameters> logs = getMasterLogbookOperations().stream()
            .collect(Collectors
                .toMap(log -> log.getParameterValue(LogbookParameterName.eventIdentifierProcess), log -> log));
        assertThat(logs).hasSize(2);
        assertThat(logs).containsOnlyKeys(traceabilityResults.get(0).getOperationId(), traceabilityResults.get(1).getOperationId());

        LogbookOperationParameters traceability_tenant0 = logs.get(traceabilityResults.get(0).getOperationId());
        LogbookOperationParameters traceability_tenant1 = logs.get(traceabilityResults.get(1).getOperationId());

        checkOutcome(traceability_tenant0, StatusCode.OK);
        checkOutcome(traceability_tenant1, StatusCode.OK);
        TraceabilityEvent traceabilityEvent_tenant0 = getTraceabilityEvent(traceability_tenant0);
        TraceabilityEvent traceabilityEvent_tenant1 = getTraceabilityEvent(traceability_tenant1);
        assertThat(traceabilityEvent_tenant0).isNotNull();
        assertThat(traceabilityEvent_tenant1).isNotNull();
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
