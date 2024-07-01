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
package fr.gouv.vitam.storage.engine.server.distribution.impl.bulk;

import com.google.common.collect.ImmutableMap;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.stream.MultiplexedStreamWriter;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.exception.StorageDriverPreconditionFailedException;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResult;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResultEntry;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import fr.gouv.vitam.storage.engine.server.distribution.impl.TransfertTimeoutHelper;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BulkPutTransferManagerTest {

    private static final String WORKSPACE_CONTAINER = "workspaceContainer";
    private static final String STRATEGY = "default";
    private static final int ATTEMPT = 1;
    private static final int TENANT_ID = 2;
    private static final DataCategory DATA_CATEGORY = DataCategory.UNIT;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    private DigestType digestType = DigestType.SHA512;

    @Mock
    private AlertService alertService;

    @Mock
    private Driver driver1;

    @Mock
    private Driver driver2;

    @Mock
    private Connection connection1;

    @Mock
    private Connection connection2;

    @Mock
    private TransfertTimeoutHelper transfertTimeoutHelper;

    private ExecutorService executor;

    private BulkPutTransferManager bulkPutTransferManager;

    @Before
    public void before() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID());
        executor = Executors.newFixedThreadPool(5, VitamThreadFactory.getInstance());

        doReturn(60000L).when(transfertTimeoutHelper).getBulkTransferTimeout(anyLong(), anyInt());

        bulkPutTransferManager = new BulkPutTransferManager(
            workspaceClientFactory,
            digestType,
            alertService,
            executor,
            transfertTimeoutHelper
        );

        doReturn(workspaceClient).when(workspaceClientFactory).getClient();

        doReturn(connection1).when(driver1).connect(any());
        doReturn(connection2).when(driver2).connect(any());
    }

    @After
    public void after() throws Exception {
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @RunWithCustomExecutor
    public void bulkSendDataToOffersOk() throws Exception {
        // Given
        List<String> offerIds = Arrays.asList("offer1", "offer2");

        Map<String, StorageOffer> storageOfferMap = ImmutableMap.of(
            "offer1",
            mock(StorageOffer.class),
            "offer2",
            mock(StorageOffer.class)
        );

        Map<String, Driver> driverMap = ImmutableMap.of("offer1", driver1, "offer2", driver2);

        List<String> workspaceObjectURIs = Arrays.asList("uri1", "uri2", "uri3");
        List<String> objectIds = Arrays.asList("obj1", "obj2", "obj3");

        File file1 = PropertiesUtils.getResourceFile("file1.txt");
        File file2 = PropertiesUtils.getResourceFile("file2.txt");
        File file3 = PropertiesUtils.getResourceFile("file3.txt");

        doReturn(getMultiplexedStream(file1, file2, file3))
            .when(workspaceClient)
            .bulkGetObjects(WORKSPACE_CONTAINER, workspaceObjectURIs);

        StorageBulkPutResult storageBulkPutResult = new StorageBulkPutResult(
            Arrays.asList(
                new StorageBulkPutResultEntry("obj1", getFileDigest(file1), file1.length()),
                new StorageBulkPutResultEntry("obj2", getFileDigest(file2), file1.length()),
                new StorageBulkPutResultEntry("obj3", getFileDigest(file3), file2.length())
            )
        );
        doReturn(storageBulkPutResult).when(connection1).bulkPutObjects(any());
        doReturn(storageBulkPutResult).when(connection2).bulkPutObjects(any());

        // When
        BulkPutResult bulkPutResult = bulkPutTransferManager.bulkSendDataToOffers(
            WORKSPACE_CONTAINER,
            STRATEGY,
            ATTEMPT,
            TENANT_ID,
            DATA_CATEGORY,
            offerIds,
            driverMap,
            storageOfferMap,
            workspaceObjectURIs,
            objectIds
        );

        // Then
        assertThat(bulkPutResult.getObjectInfos()).hasSize(3);
        assertThat(bulkPutResult.getObjectInfos().get(0).getObjectId()).isEqualTo("obj1");
        assertThat(bulkPutResult.getObjectInfos().get(1).getObjectId()).isEqualTo("obj2");
        assertThat(bulkPutResult.getObjectInfos().get(2).getObjectId()).isEqualTo("obj3");
        assertThat(bulkPutResult.getObjectInfos().get(0).getDigest()).isEqualTo(getFileDigest(file1));
        assertThat(bulkPutResult.getObjectInfos().get(0).getSize()).isEqualTo(file1.length());
        assertThat(bulkPutResult.getStatusByOfferIds()).isEqualTo(
            ImmutableMap.of("offer1", OfferBulkPutStatus.OK, "offer2", OfferBulkPutStatus.OK)
        );

        verify(transfertTimeoutHelper).getBulkTransferTimeout(106L, 3);
    }

    @Test
    @RunWithCustomExecutor
    public void bulkSendDataToOffersKoWorkspaceError() throws Exception {
        // Given
        List<String> offerIds = Arrays.asList("offer1", "offer2");

        Map<String, StorageOffer> storageOfferMap = ImmutableMap.of(
            "offer1",
            mock(StorageOffer.class),
            "offer2",
            mock(StorageOffer.class)
        );

        Map<String, Driver> driverMap = ImmutableMap.of("offer1", driver1, "offer2", driver2);

        List<String> workspaceObjectURIs = Arrays.asList("uri1", "uri2", "uri3");
        List<String> objectIds = Arrays.asList("obj1", "obj2", "obj3");

        doThrow(ContentAddressableStorageServerException.class)
            .when(workspaceClient)
            .bulkGetObjects(WORKSPACE_CONTAINER, workspaceObjectURIs);

        // When
        BulkPutResult bulkPutResult = bulkPutTransferManager.bulkSendDataToOffers(
            WORKSPACE_CONTAINER,
            STRATEGY,
            ATTEMPT,
            TENANT_ID,
            DATA_CATEGORY,
            offerIds,
            driverMap,
            storageOfferMap,
            workspaceObjectURIs,
            objectIds
        );

        // Then
        assertThat(bulkPutResult.getObjectInfos()).isNull();
        assertThat(bulkPutResult.getStatusByOfferIds()).isEqualTo(
            ImmutableMap.of("offer1", OfferBulkPutStatus.KO, "offer2", OfferBulkPutStatus.KO)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void bulkSendDataToOffersConflictNotFoundInWorkspace() throws Exception {
        // Given
        List<String> offerIds = Arrays.asList("offer1", "offer2");

        Map<String, StorageOffer> storageOfferMap = ImmutableMap.of(
            "offer1",
            mock(StorageOffer.class),
            "offer2",
            mock(StorageOffer.class)
        );

        Map<String, Driver> driverMap = ImmutableMap.of("offer1", driver1, "offer2", driver2);

        List<String> workspaceObjectURIs = Arrays.asList("uri1", "uri2", "uri3");
        List<String> objectIds = Arrays.asList("obj1", "obj2", "obj3");

        doThrow(ContentAddressableStorageNotFoundException.class)
            .when(workspaceClient)
            .bulkGetObjects(WORKSPACE_CONTAINER, workspaceObjectURIs);

        // When
        BulkPutResult bulkPutResult = bulkPutTransferManager.bulkSendDataToOffers(
            WORKSPACE_CONTAINER,
            STRATEGY,
            ATTEMPT,
            TENANT_ID,
            DATA_CATEGORY,
            offerIds,
            driverMap,
            storageOfferMap,
            workspaceObjectURIs,
            objectIds
        );

        // Then
        assertThat(bulkPutResult.getObjectInfos()).isNull();
        assertThat(bulkPutResult.getStatusByOfferIds()).isEqualTo(
            ImmutableMap.of("offer1", OfferBulkPutStatus.BLOCKER, "offer2", OfferBulkPutStatus.BLOCKER)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void bulkSendDataToOffersKoBrokenWorkspaceStream() throws Exception {
        // Given
        List<String> offerIds = Arrays.asList("offer1", "offer2");

        Map<String, StorageOffer> storageOfferMap = ImmutableMap.of(
            "offer1",
            mock(StorageOffer.class),
            "offer2",
            mock(StorageOffer.class)
        );

        Map<String, Driver> driverMap = ImmutableMap.of("offer1", driver1, "offer2", driver2);

        List<String> workspaceObjectURIs = Arrays.asList("uri1", "uri2", "uri3");
        List<String> objectIds = Arrays.asList("obj1", "obj2", "obj3");

        File file1 = PropertiesUtils.getResourceFile("file1.txt");
        File file2 = PropertiesUtils.getResourceFile("file2.txt");
        File file3 = PropertiesUtils.getResourceFile("file3.txt");

        doReturn(getBrokenMultiplexedStream(file1, file2, file3))
            .when(workspaceClient)
            .bulkGetObjects(WORKSPACE_CONTAINER, workspaceObjectURIs);

        // When
        BulkPutResult bulkPutResult = bulkPutTransferManager.bulkSendDataToOffers(
            WORKSPACE_CONTAINER,
            STRATEGY,
            ATTEMPT,
            TENANT_ID,
            DATA_CATEGORY,
            offerIds,
            driverMap,
            storageOfferMap,
            workspaceObjectURIs,
            objectIds
        );

        // Then
        assertThat(bulkPutResult.getObjectInfos()).isNull();
        assertThat(bulkPutResult.getStatusByOfferIds()).isEqualTo(
            ImmutableMap.of("offer1", OfferBulkPutStatus.KO, "offer2", OfferBulkPutStatus.KO)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void bulkSendDataToOffersOfferKo() throws Exception {
        // Given
        List<String> offerIds = Arrays.asList("offer1", "offer2");

        Map<String, StorageOffer> storageOfferMap = ImmutableMap.of(
            "offer1",
            mock(StorageOffer.class),
            "offer2",
            mock(StorageOffer.class)
        );

        Map<String, Driver> driverMap = ImmutableMap.of("offer1", driver1, "offer2", driver2);

        List<String> workspaceObjectURIs = Arrays.asList("uri1", "uri2", "uri3");
        List<String> objectIds = Arrays.asList("obj1", "obj2", "obj3");

        File file1 = PropertiesUtils.getResourceFile("file1.txt");
        File file2 = PropertiesUtils.getResourceFile("file2.txt");
        File file3 = PropertiesUtils.getResourceFile("file3.txt");

        doReturn(getMultiplexedStream(file1, file2, file3))
            .when(workspaceClient)
            .bulkGetObjects(WORKSPACE_CONTAINER, workspaceObjectURIs);

        StorageBulkPutResult storageBulkPutResult = new StorageBulkPutResult(
            Arrays.asList(
                new StorageBulkPutResultEntry("obj1", getFileDigest(file1), file1.length()),
                new StorageBulkPutResultEntry("obj2", getFileDigest(file2), file1.length()),
                new StorageBulkPutResultEntry("obj3", getFileDigest(file3), file2.length())
            )
        );
        doReturn(storageBulkPutResult).when(connection1).bulkPutObjects(any());

        doThrow(new StorageDriverException("", "", true)).when(connection2).bulkPutObjects(any());

        // When
        BulkPutResult bulkPutResult = bulkPutTransferManager.bulkSendDataToOffers(
            WORKSPACE_CONTAINER,
            STRATEGY,
            ATTEMPT,
            TENANT_ID,
            DATA_CATEGORY,
            offerIds,
            driverMap,
            storageOfferMap,
            workspaceObjectURIs,
            objectIds
        );

        // Then
        assertThat(bulkPutResult.getObjectInfos()).hasSize(3);
        assertThat(bulkPutResult.getObjectInfos().get(0).getObjectId()).isEqualTo("obj1");
        assertThat(bulkPutResult.getObjectInfos().get(1).getObjectId()).isEqualTo("obj2");
        assertThat(bulkPutResult.getObjectInfos().get(2).getObjectId()).isEqualTo("obj3");
        assertThat(bulkPutResult.getObjectInfos().get(0).getDigest()).isEqualTo(getFileDigest(file1));
        assertThat(bulkPutResult.getObjectInfos().get(0).getSize()).isEqualTo(file1.length());
        assertThat(bulkPutResult.getStatusByOfferIds()).isEqualTo(
            ImmutableMap.of("offer1", OfferBulkPutStatus.OK, "offer2", OfferBulkPutStatus.KO)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void bulkSendDataToOffersOfferConflict() throws Exception {
        // Given
        List<String> offerIds = Arrays.asList("offer1", "offer2");

        Map<String, StorageOffer> storageOfferMap = ImmutableMap.of(
            "offer1",
            mock(StorageOffer.class),
            "offer2",
            mock(StorageOffer.class)
        );

        Map<String, Driver> driverMap = ImmutableMap.of("offer1", driver1, "offer2", driver2);

        List<String> workspaceObjectURIs = Arrays.asList("uri1", "uri2", "uri3");
        List<String> objectIds = Arrays.asList("obj1", "obj2", "obj3");

        File file1 = PropertiesUtils.getResourceFile("file1.txt");
        File file2 = PropertiesUtils.getResourceFile("file2.txt");
        File file3 = PropertiesUtils.getResourceFile("file3.txt");

        doReturn(getMultiplexedStream(file1, file2, file3))
            .when(workspaceClient)
            .bulkGetObjects(WORKSPACE_CONTAINER, workspaceObjectURIs);

        StorageBulkPutResult storageBulkPutResult = new StorageBulkPutResult(
            Arrays.asList(
                new StorageBulkPutResultEntry("obj1", getFileDigest(file1), file1.length()),
                new StorageBulkPutResultEntry("obj2", getFileDigest(file2), file1.length()),
                new StorageBulkPutResultEntry("obj3", getFileDigest(file3), file2.length())
            )
        );
        doReturn(storageBulkPutResult).when(connection1).bulkPutObjects(any());

        doThrow(StorageDriverPreconditionFailedException.class).when(connection2).bulkPutObjects(any());

        // When
        BulkPutResult bulkPutResult = bulkPutTransferManager.bulkSendDataToOffers(
            WORKSPACE_CONTAINER,
            STRATEGY,
            ATTEMPT,
            TENANT_ID,
            DATA_CATEGORY,
            offerIds,
            driverMap,
            storageOfferMap,
            workspaceObjectURIs,
            objectIds
        );

        // Then
        assertThat(bulkPutResult.getObjectInfos()).hasSize(3);
        assertThat(bulkPutResult.getObjectInfos().get(0).getObjectId()).isEqualTo("obj1");
        assertThat(bulkPutResult.getObjectInfos().get(1).getObjectId()).isEqualTo("obj2");
        assertThat(bulkPutResult.getObjectInfos().get(2).getObjectId()).isEqualTo("obj3");
        assertThat(bulkPutResult.getObjectInfos().get(0).getDigest()).isEqualTo(getFileDigest(file1));
        assertThat(bulkPutResult.getObjectInfos().get(0).getSize()).isEqualTo(file1.length());
        assertThat(bulkPutResult.getStatusByOfferIds()).isEqualTo(
            ImmutableMap.of("offer1", OfferBulkPutStatus.OK, "offer2", OfferBulkPutStatus.BLOCKER)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void bulkSendDataToOffersOfferTimeout() throws Exception {
        // Given
        List<String> offerIds = Arrays.asList("offer1", "offer2");

        Map<String, StorageOffer> storageOfferMap = ImmutableMap.of(
            "offer1",
            mock(StorageOffer.class),
            "offer2",
            mock(StorageOffer.class)
        );

        Map<String, Driver> driverMap = ImmutableMap.of("offer1", driver1, "offer2", driver2);

        List<String> workspaceObjectURIs = Arrays.asList("uri1", "uri2", "uri3");
        List<String> objectIds = Arrays.asList("obj1", "obj2", "obj3");

        File file1 = PropertiesUtils.getResourceFile("file1.txt");
        File file2 = PropertiesUtils.getResourceFile("file2.txt");
        File file3 = PropertiesUtils.getResourceFile("file3.txt");

        doReturn(getMultiplexedStream(file1, file2, file3))
            .when(workspaceClient)
            .bulkGetObjects(WORKSPACE_CONTAINER, workspaceObjectURIs);

        StorageBulkPutResult storageBulkPutResult = new StorageBulkPutResult(
            Arrays.asList(
                new StorageBulkPutResultEntry("obj1", getFileDigest(file1), file1.length()),
                new StorageBulkPutResultEntry("obj2", getFileDigest(file2), file1.length()),
                new StorageBulkPutResultEntry("obj3", getFileDigest(file3), file2.length())
            )
        );
        doReturn(storageBulkPutResult).when(connection1).bulkPutObjects(any());
        doAnswer(args -> {
            Thread.sleep(3000);
            return storageBulkPutResult;
        })
            .when(connection2)
            .bulkPutObjects(any());

        doReturn(2000L).when(transfertTimeoutHelper).getBulkTransferTimeout(anyLong(), anyInt());

        // When
        BulkPutResult bulkPutResult = bulkPutTransferManager.bulkSendDataToOffers(
            WORKSPACE_CONTAINER,
            STRATEGY,
            ATTEMPT,
            TENANT_ID,
            DATA_CATEGORY,
            offerIds,
            driverMap,
            storageOfferMap,
            workspaceObjectURIs,
            objectIds
        );

        // Then
        assertThat(bulkPutResult.getObjectInfos()).hasSize(3);
        assertThat(bulkPutResult.getObjectInfos().get(0).getObjectId()).isEqualTo("obj1");
        assertThat(bulkPutResult.getObjectInfos().get(1).getObjectId()).isEqualTo("obj2");
        assertThat(bulkPutResult.getObjectInfos().get(2).getObjectId()).isEqualTo("obj3");
        assertThat(bulkPutResult.getObjectInfos().get(0).getDigest()).isEqualTo(getFileDigest(file1));
        assertThat(bulkPutResult.getObjectInfos().get(0).getSize()).isEqualTo(file1.length());
        assertThat(bulkPutResult.getStatusByOfferIds()).isEqualTo(
            ImmutableMap.of("offer1", OfferBulkPutStatus.OK, "offer2", OfferBulkPutStatus.KO)
        );

        verify(transfertTimeoutHelper).getBulkTransferTimeout(106L, 3);
    }

    @Test
    @RunWithCustomExecutor
    public void bulkSendDataToOfferTestDeadlockOfferFailureTransferThreadShutdown() throws Exception {
        // Given
        List<String> offerIds = Arrays.asList("offer1", "offer2");

        Map<String, StorageOffer> storageOfferMap = ImmutableMap.of(
            "offer1",
            mock(StorageOffer.class),
            "offer2",
            mock(StorageOffer.class)
        );

        Map<String, Driver> driverMap = ImmutableMap.of("offer1", driver1, "offer2", driver2);

        List<String> workspaceObjectURIs = Arrays.asList("uri1");
        List<String> objectIds = Arrays.asList("obj1");

        // Long enough to be blocking in MultiplePipedInputStream
        long longFileSize = 10_000_000L;
        File veryLargeFile = folder.newFile(GUIDFactory.newGUID().toString());
        Digest digest = new Digest(DigestType.SHA512);
        try (OutputStream os = new FileOutputStream(veryLargeFile)) {
            MultiplexedStreamWriter multiplexedStreamWriter = new MultiplexedStreamWriter(os);

            NullInputStream bigFileInputStream = new NullInputStream(longFileSize);
            multiplexedStreamWriter.appendEntry(longFileSize, digest.getDigestInputStream(bigFileInputStream));
            multiplexedStreamWriter.appendEndOfFile();
        }

        doReturn(
            Response.ok(new FileInputStream(veryLargeFile))
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), veryLargeFile.length())
                .build()
        )
            .when(workspaceClient)
            .bulkGetObjects(WORKSPACE_CONTAINER, workspaceObjectURIs);

        StorageBulkPutResult storageBulkPutResult = new StorageBulkPutResult(
            Arrays.asList(new StorageBulkPutResultEntry("obj1", digest.digestHex(), longFileSize))
        );
        doReturn(storageBulkPutResult).when(connection1).bulkPutObjects(any());

        doThrow(new StorageDriverException("driver", "ko", true)).when(connection2).bulkPutObjects(any());

        // When / Then
        BulkPutResult bulkPutResult = bulkPutTransferManager.bulkSendDataToOffers(
            WORKSPACE_CONTAINER,
            STRATEGY,
            ATTEMPT,
            TENANT_ID,
            DATA_CATEGORY,
            offerIds,
            driverMap,
            storageOfferMap,
            workspaceObjectURIs,
            objectIds
        );

        // Then
        assertThat(bulkPutResult.getObjectInfos()).hasSize(1);
        assertThat(bulkPutResult.getObjectInfos().get(0).getObjectId()).isEqualTo("obj1");
        assertThat(bulkPutResult.getObjectInfos().get(0).getDigest()).isEqualTo(digest.digestHex());
        assertThat(bulkPutResult.getObjectInfos().get(0).getSize()).isEqualTo(longFileSize);
        assertThat(bulkPutResult.getStatusByOfferIds()).isEqualTo(
            ImmutableMap.of("offer1", OfferBulkPutStatus.OK, "offer2", OfferBulkPutStatus.KO)
        );
    }

    private Response getMultiplexedStream(File... files) throws IOException {
        byte[] multiplexedStreamBody = getMultiplexedStreamBody(files);

        return Response.ok(new ByteArrayInputStream(multiplexedStreamBody))
            .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), multiplexedStreamBody.length)
            .build();
    }

    private Response getBrokenMultiplexedStream(File... files) throws IOException {
        byte[] multiplexedStreamBody = getMultiplexedStreamBody(files);

        return Response.ok(new ByteArrayInputStream(multiplexedStreamBody, 0, multiplexedStreamBody.length - 1))
            .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), multiplexedStreamBody.length)
            .build();
    }

    private byte[] getMultiplexedStreamBody(File... files) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        MultiplexedStreamWriter multiplexedStreamWriter = new MultiplexedStreamWriter(byteArrayOutputStream);

        // Append content entries
        for (File file : files) {
            try (FileInputStream fis = new FileInputStream(file)) {
                multiplexedStreamWriter.appendEntry(file.length(), fis);
            }
        }
        multiplexedStreamWriter.appendEndOfFile();
        return byteArrayOutputStream.toByteArray();
    }

    private String getFileDigest(File file) throws IOException {
        return new Digest(this.digestType).update(file).digestHex();
    }
}
