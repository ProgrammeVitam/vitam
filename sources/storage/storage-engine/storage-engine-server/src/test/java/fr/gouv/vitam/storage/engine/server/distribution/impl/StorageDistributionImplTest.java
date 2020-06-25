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
package fr.gouv.vitam.storage.engine.server.distribution.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.engine.common.exception.StorageAlreadyExistsException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageTechnicalException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.BulkObjectStoreRequest;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.BulkObjectStoreResponse;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import fr.gouv.vitam.storage.engine.server.distribution.impl.bulk.BulkStorageDistribution;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.storagelog.StorageLog;
import fr.gouv.vitam.storage.engine.server.storagelog.StorageLogFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * StorageDistributionImplTest
 */
public class StorageDistributionImplTest {
    // FIXME P1 Fix Fake Driver

    private static final String OFFER_ID = "default";
    private static final int TENANT_ID = 0;

    private static StorageDistribution simpleDistribution;
    private StorageDistribution customDistribution;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private BulkStorageDistribution bulkStorageDistribution;

    @Before
    public void prepare() throws IOException {
        final StorageConfiguration configuration = new StorageConfiguration();
        configuration.setUrlWorkspace("http://localhost:8080");
        configuration.setTimeoutMsPerKB(1000);
        doReturn(workspaceClient).when(workspaceClientFactory).getClient();
        List<Integer> list = new ArrayList<>();
        list.add(0);
        list.add(1);

        folder.create();
        StorageLog storageLogService =
            StorageLogFactory.getInstanceForTest(list, Paths.get(folder.getRoot().getAbsolutePath()));
        simpleDistribution = new StorageDistributionImpl(configuration, storageLogService);
        customDistribution = new StorageDistributionImpl(workspaceClientFactory, DigestType.SHA1, storageLogService,
            Executors.newFixedThreadPool(16, VitamThreadFactory.getInstance()), 300, bulkStorageDistribution);
    }

    @After
    public void cleanup() {
        simpleDistribution.close();
        customDistribution.close();
    }

    @Test
    @RunWithCustomExecutor
    public void testStoreData_IllegalArguments()
        throws StorageException {
        // storeDataInOffers(String tenantId, String strategyId, String objectId,
        // CreateObjectDescription createObjectDescription, DataCategory
        // category,
        // JsonNode jsonData)
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final ObjectDescription emptyDescription = new ObjectDescription();
        checkInvalidArgumentException(null, null, null, null);
        checkInvalidArgumentException(null, null, null, null);
        checkInvalidArgumentException("strategy_id", null, null, null);
        checkInvalidArgumentException("strategy_id", "object_id", null, null);
        checkInvalidArgumentException("strategy_id", "object_id", emptyDescription, null);
        checkInvalidArgumentException("strategy_id", "object_id", emptyDescription, DataCategory.OBJECT);

        emptyDescription.setWorkspaceContainerGUID("ddd");
        checkInvalidArgumentException("strategy_id", "object_id", emptyDescription, DataCategory.OBJECT);

        emptyDescription.setWorkspaceContainerGUID(null);
        emptyDescription.setWorkspaceObjectURI("ddd");
        checkInvalidArgumentException("strategy_id", "object_id", emptyDescription, DataCategory.OBJECT);
    }

    @Test
    @RunWithCustomExecutor
    // FIXME P1 Update Fake driver : Add objectExistsInOffer
    public void testStoreData_OK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final String objectId = "id1";
        StoredInfoResult storedInfoResult;
        final ObjectDescription createObjectDescription = new ObjectDescription();
        createObjectDescription.setWorkspaceContainerGUID("container1" + this);
        createObjectDescription.setWorkspaceObjectURI("SIP/content/test.pdf");

        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        FileInputStream stream2 = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(workspaceClient);

        when(workspaceClient.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream)
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).build())
            .thenReturn(Response.status(Status.OK).entity(stream2).build());
        try {
            // Store object
            storedInfoResult =
                customDistribution
                    .storeDataInAllOffers(VitamConfiguration.getDefaultStrategy(), objectId, createObjectDescription,
                        DataCategory.OBJECT,
                        "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
            IOUtils.closeQuietly(stream2);
        }
        reset(workspaceClient);
        when(workspaceClient.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenThrow(IllegalStateException.class);
        assertNotNull(storedInfoResult);
        assertEquals(objectId, storedInfoResult.getId());
        assertNull(storedInfoResult.getObjectGroupId());
        String info = storedInfoResult.getInfo();
        assertNotNull(info);
        assertTrue(info.contains("Object") && info.contains("successfully"));
        assertNotNull(storedInfoResult.getCreationTime());
        assertNotNull(storedInfoResult.getLastAccessTime());
        assertNotNull(storedInfoResult.getLastCheckedTime());
        assertNotNull(storedInfoResult.getLastModifiedTime());
        assertNull(storedInfoResult.getUnitIds());

        // Store Unit
        stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        stream2 = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(workspaceClient);
        when(workspaceClient.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream)
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).build())
            .thenReturn(Response.status(Status.OK).entity(stream2).build());
        try {
            storedInfoResult =
                customDistribution
                    .storeDataInAllOffers(VitamConfiguration.getDefaultStrategy(), objectId, createObjectDescription,
                        DataCategory.UNIT,
                        "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
        }
        assertNotNull(storedInfoResult);
        assertEquals(objectId, storedInfoResult.getId());
        info = storedInfoResult.getInfo();
        assertNotNull(info);
        assertTrue(info.contains("Unit") && info.contains("successfully"));

        // Store logbook
        stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        stream2 = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(workspaceClient);
        when(workspaceClient.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream)
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).build())
            .thenReturn(Response.status(Status.OK).entity(stream2).build());
        try {
            storedInfoResult =
                customDistribution
                    .storeDataInAllOffers(VitamConfiguration.getDefaultStrategy(), objectId, createObjectDescription,
                        DataCategory.LOGBOOK,
                        "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
        }
        assertNotNull(storedInfoResult);
        assertEquals(objectId, storedInfoResult.getId());
        info = storedInfoResult.getInfo();
        assertNotNull(info);
        assertTrue(info.contains("Logbook") && info.contains("successfully"));

        // Store storageLog
        stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        stream2 = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(workspaceClient);
        when(workspaceClient.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream)
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).build())
            .thenReturn(Response.status(Status.OK).entity(stream2).build());
        try {
            storedInfoResult =
                customDistribution
                    .storeDataInAllOffers(VitamConfiguration.getDefaultStrategy(), objectId, createObjectDescription,
                        DataCategory.STORAGELOG,
                        "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
        }
        assertNotNull(storedInfoResult);
        assertEquals(objectId, storedInfoResult.getId());
        info = storedInfoResult.getInfo();
        assertNotNull(info);
        assertTrue(info.contains("Storagelog") && info.contains("successfully"));
        // Store object group
        stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        stream2 = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(workspaceClient);
        when(workspaceClient.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream)
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).build())
            .thenReturn(Response.status(Status.OK).entity(stream2).build());
        try {
            storedInfoResult = customDistribution
                .storeDataInAllOffers(VitamConfiguration.getDefaultStrategy(), objectId, createObjectDescription,
                    DataCategory.OBJECTGROUP, "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
        }
        assertNotNull(storedInfoResult);
        assertEquals(objectId, storedInfoResult.getId());
        info = storedInfoResult.getInfo();
        assertNotNull(info);
        assertTrue(info.contains("ObjectGroup") && info.contains("successfully"));

        Digest digest = Digest.digest(new FileInputStream(PropertiesUtils.findFile("object.zip")),
            VitamConfiguration.getDefaultDigestType());
        // lets delete the object on offers

        DataContext context =
            new DataContext(objectId, DataCategory.OBJECT, "192.168.1.1", 0, VitamConfiguration.getDefaultStrategy());

        customDistribution.deleteObjectInAllOffers(VitamConfiguration.getDefaultStrategy(), context);

    }

    @Test(expected = StorageTechnicalException.class)
    @RunWithCustomExecutor
    public void testStoreData_retry_KO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final String objectId = "retry_test";
        final ObjectDescription createObjectDescription = new ObjectDescription();
        createObjectDescription.setWorkspaceContainerGUID("container1" + this);
        createObjectDescription.setWorkspaceObjectURI("SIP/content/test.pdf");

        FileInputStream stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        FileInputStream stream2 = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(workspaceClient);

        when(workspaceClient.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream)
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).build())
            .thenReturn(Response.status(Status.OK).entity(stream)
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).build())
            .thenReturn(Response.status(Status.OK).entity(stream)
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).build())
            .thenReturn(Response.status(Status.OK).entity(stream2).build());
        try {
            // Store object
            customDistribution
                .storeDataInAllOffers(VitamConfiguration.getDefaultStrategy(), objectId, createObjectDescription,
                    DataCategory.OBJECT, "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
            IOUtils.closeQuietly(stream2);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testStoreData_TestDeadlockOfferFailureTransferThreadShutdown() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        // Offer 1 ok, offer 2 will fail
        final String objectId = "fail-offer-default2";
        final ObjectDescription createObjectDescription = new ObjectDescription();
        createObjectDescription.setWorkspaceContainerGUID("container1" + this);
        createObjectDescription.setWorkspaceObjectURI("SIP/content/test.pdf");

        reset(workspaceClient);

        // Long enough to be blocking in MultiplePipedInputStream
        long longFileSize = 10_000_000L;

        when(workspaceClient.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenAnswer((args) -> Response.status(Status.OK).entity(new NullInputStream(longFileSize))
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), longFileSize).build());

        // When / Then
        assertThatThrownBy(() ->
            customDistribution
                .storeDataInAllOffers(VitamConfiguration.getDefaultStrategy(), objectId, createObjectDescription,
                    DataCategory.OBJECT, "testRequester")
        ).isInstanceOf(StorageTechnicalException.class);
    }

    @Test(expected = StorageTechnicalException.class)
    @RunWithCustomExecutor
    public void testStoreData_DigestKO() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final String objectId = "digest_bad_test";
        final ObjectDescription createObjectDescription = new ObjectDescription();
        createObjectDescription.setWorkspaceContainerGUID("container1" + this);
        createObjectDescription.setWorkspaceObjectURI("SIP/content/test.pdf");
        final FileInputStream stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        reset(workspaceClient);
        when(workspaceClient.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK)
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).entity(stream).build());
        try {
            customDistribution
                .storeDataInAllOffers(VitamConfiguration.getDefaultStrategy(), objectId, createObjectDescription,
                    DataCategory.OBJECT, "testRequester");
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    @RunWithCustomExecutor
    @Test(expected = StorageAlreadyExistsException.class)
    public void testObjectAlreadyInOffer() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final String objectId = "conflict";
        final ObjectDescription createObjectDescription = new ObjectDescription();
        createObjectDescription.setWorkspaceContainerGUID("container1" + this);
        createObjectDescription.setWorkspaceObjectURI("SIP/content/test.pdf");

        reset(workspaceClient);
        when(workspaceClient.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(new FileInputStream(PropertiesUtils.findFile("object.zip")))
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).build())
            .thenReturn(Response.status(Status.OK).entity(new FileInputStream(PropertiesUtils.findFile("object.zip")))
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).build())
            .thenReturn(Response.status(Status.OK).entity(new FileInputStream(PropertiesUtils.findFile("object.zip")))
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).build())
            .thenReturn(Response.status(Status.OK).entity(new FileInputStream(PropertiesUtils.findFile("object.zip")))
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).build())
            .thenReturn(Response.status(Status.OK).entity(new FileInputStream(PropertiesUtils.findFile("object.zip")))
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).build())
            .thenReturn(Response.status(Status.OK).entity(new FileInputStream(PropertiesUtils.findFile("object.zip")))
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).build());
        // Store object
        customDistribution
            .storeDataInAllOffers(VitamConfiguration.getDefaultStrategy(), objectId, createObjectDescription,
                DataCategory.OBJECT, "testRequester");
    }

    @Test
    @RunWithCustomExecutor
    public void testStoreData_NotFoundAndWorspaceErrorToTechnicalError() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final String objectId = "id1";
        final ObjectDescription createObjectDescription = new ObjectDescription();
        createObjectDescription.setWorkspaceContainerGUID("container1" + this);
        createObjectDescription.setWorkspaceObjectURI("SIP/content/test.pdf");

        reset(workspaceClient);
        when(workspaceClient.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenThrow(ContentAddressableStorageNotFoundException.class);
        try {
            customDistribution
                .storeDataInAllOffers(VitamConfiguration.getDefaultStrategy(), objectId, createObjectDescription,
                    DataCategory.OBJECT, "testRequester");
            fail("Should produce exception");
        } catch (final StorageException exc) {
            // Expection
        }

        reset(workspaceClient);
        when(workspaceClient.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenThrow(ContentAddressableStorageServerException.class);
        try {
            customDistribution
                .storeDataInAllOffers(VitamConfiguration.getDefaultStrategy(), objectId, createObjectDescription,
                    DataCategory.OBJECT, "testRequester");
            fail("Should produce exception");
        } catch (final StorageTechnicalException exc) {
            // Expection
        }

        final FileInputStream stream = new FileInputStream(PropertiesUtils.findFile("object.zip"));
        IOUtils.closeQuietly(stream);
        reset(workspaceClient);
        when(workspaceClient.getObject("container1" + this, "SIP/content/test.pdf"))
            .thenReturn(Response.status(Status.OK).entity(stream)
                .header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 6349).build());
        try {
            customDistribution
                .storeDataInAllOffers(VitamConfiguration.getDefaultStrategy(), objectId, createObjectDescription,
                    DataCategory.OBJECT, "testRequester");
            fail("Should produce exception");
        } catch (final StorageTechnicalException exc) {
            // Expection
        }
    }

    private void checkInvalidArgumentException(String strategyId, String objectId,
        ObjectDescription createObjectDescription, DataCategory category)
        throws StorageException {
        try {
            simpleDistribution
                .storeDataInAllOffers(strategyId, objectId, createObjectDescription, category, "testRequester");
            fail("Parameter should be considered invalid");
        } catch (final IllegalArgumentException exc) {
            // test OK
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testBulkCreateFromWorkspace()
        throws StorageException {

        // Given
        int tenantId = 2;
        List<String> workspaceObjectURIs = Arrays.asList("uir1", "uri2");
        List<String> objectNames = Arrays.asList("ob1", "ob2");
        String workspaceContainer = "workspaceContainer";
        String requester = "requester";
        List<String> offers = Arrays.asList("default", "default2");
        Map<String, String> digests = ImmutableMap.of("default", "digest1", "default2", "digest2");

        VitamThreadUtils.getVitamSession().setTenantId(tenantId);

        doReturn(digests).when(bulkStorageDistribution).bulkCreateFromWorkspaceWithRetries(anyString(),
            anyInt(), anyList(), anyMap(), anyMap(), any(), anyString(), anyList(), anyList(), anyString());

        BulkObjectStoreRequest bulkObjectStoreRequest = new BulkObjectStoreRequest(
            workspaceContainer, workspaceObjectURIs, DataCategory.UNIT, objectNames
        );

        // When
        BulkObjectStoreResponse bulkObjectStoreResponse =
            customDistribution
                .bulkCreateFromWorkspace(VitamConfiguration.getDefaultStrategy(), bulkObjectStoreRequest, requester);

        // Then
        assertThat(bulkObjectStoreResponse.getDigestType()).isEqualTo(DigestType.SHA1.getName());
        assertThat(bulkObjectStoreResponse.getObjectDigests()).isEqualTo(digests);
        assertThat(bulkObjectStoreResponse.getOfferIds()).isEqualTo(offers);

        ArgumentCaptor<Map<String, StorageOffer>> storageOfferCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, Driver>> storageDriverCaptor = ArgumentCaptor.forClass(Map.class);

        verify(bulkStorageDistribution).bulkCreateFromWorkspaceWithRetries(anyString(),
            eq(tenantId), eq(offers), storageDriverCaptor.capture(), storageOfferCaptor.capture(),
            eq(DataCategory.UNIT), eq(workspaceContainer), eq(workspaceObjectURIs), eq(objectNames), eq(requester)
        );

        assertThat(storageOfferCaptor.getValue().keySet()).containsExactlyInAnyOrderElementsOf(offers);
        assertThat(storageOfferCaptor.getValue().values()).noneMatch(Objects::isNull);

        assertThat(storageDriverCaptor.getValue().keySet()).containsExactlyInAnyOrderElementsOf(offers);
        assertThat(storageDriverCaptor.getValue().values()).noneMatch(Objects::isNull);
    }

    @RunWithCustomExecutor
    @Test
    public void getContainerInformationOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final JsonNode jsonNode = simpleDistribution.getContainerInformation(VitamConfiguration.getDefaultStrategy());
        assertNotNull(jsonNode);
    }

    @RunWithCustomExecutor
    @Test(expected = StorageTechnicalException.class)
    public void getContainerInformationTechnicalException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(-1);
        customDistribution.getContainerInformation(VitamConfiguration.getDefaultStrategy());
    }

    @RunWithCustomExecutor
    @Test
    public void testGetContainerByCategoryIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        try {
            simpleDistribution.getContainerByCategory(null, null, null, null, AccessLogUtils.getNoLogAccessLog());
            fail("Exception excepted");
        } catch (final IllegalArgumentException exc) {
            // nothing, exception needed
        }
        try {
            simpleDistribution.getContainerByCategory(null, null, null, null, AccessLogUtils.getNoLogAccessLog());
            fail("Exception excepted");
        } catch (final IllegalArgumentException exc) {
            // nothing, exception needed
        }
        try {
            simpleDistribution.getContainerByCategory(VitamConfiguration.getDefaultStrategy(), null, null, null,
                AccessLogUtils.getNoLogAccessLog());
            fail("Exception excepted");
        } catch (final IllegalArgumentException exc) {
            // nothing, exception needed
        }
    }

    @RunWithCustomExecutor
    @Test
    public void testGetContainerByCategoryNotFoundException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        simpleDistribution
            .getContainerByCategory(VitamConfiguration.getDefaultStrategy(), null, "0", DataCategory.OBJECT,
                AccessLogUtils.getNoLogAccessLog());
    }

    @RunWithCustomExecutor
    @Test
    public void deleteObjectOK() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        DataContext context =
            new DataContext("0", DataCategory.OBJECT, "192.168.1.1", 0, VitamConfiguration.getDefaultStrategy());

        customDistribution.deleteObjectInAllOffers(VitamConfiguration.getDefaultStrategy(), context);
    }

    @RunWithCustomExecutor
    @Test
    public void testdeleteObjectIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        DataContext context =
            new DataContext("0", DataCategory.OBJECT, null, 0, VitamConfiguration.getDefaultStrategy());

        try {
            customDistribution.deleteObjectInAllOffers(null, context);
            fail("Exception excepted");
        } catch (final IllegalArgumentException exc) {
            // nothing, exception needed
        }
    }

    @RunWithCustomExecutor
    @Test(expected = StorageTechnicalException.class)
    public void timeoutInterruptedTest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        final ObjectDescription createObjectDescription = new ObjectDescription();
        createObjectDescription.setWorkspaceContainerGUID("container1" + this);
        createObjectDescription.setWorkspaceObjectURI("SIP/content/test.pdf");

        FakeInputStream stream = new FakeInputStream(500);
        reset(workspaceClient);

        when(workspaceClient.getObject("container1" + this, "SIP/content/test.pdf")).thenReturn(
            Response.status(Status.OK).entity(stream).header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), (long) 500)
                .build());

        try {
            // Store object
            TransferThread.setJunitMode(true);
            customDistribution
                .storeDataInAllOffers(VitamConfiguration.getDefaultStrategy(), TransferThread.TIMEOUT_TEST,
                    createObjectDescription,
                    DataCategory.OBJECTGROUP, "testRequester");
            TransferThread.setJunitMode(false);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    @RunWithCustomExecutor
    @Test
    public void listContainerObjectsTests() throws Exception {
        try {
            simpleDistribution.listContainerObjects(null, null);
            fail("Waiting for an illegal argument exception");
        } catch (IllegalArgumentException exc) {
            // nothing
        }
        try {
            simpleDistribution.listContainerObjects(VitamConfiguration.getDefaultStrategy(), null);
            fail("Waiting for an illegal argument exception");
        } catch (IllegalArgumentException exc) {
            // nothing
        }
        try {
            simpleDistribution.listContainerObjects(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECT);
            fail("Waiting for an illegal argument exception");
        } catch (IllegalArgumentException exc) {
            // nothing
        }
        try {
            simpleDistribution.listContainerObjects(VitamConfiguration.getDefaultStrategy(), null);
            fail("Waiting for an illegal argument exception");
        } catch (IllegalArgumentException exc) {
            // nothing
        }
        try {
            VitamThreadUtils.getVitamSession().setTenantId(0);
            simpleDistribution.listContainerObjects(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECT);
        } catch (IllegalArgumentException exc) {
            fail("Waiting for an illegal argument exception");
        }
    }

    @RunWithCustomExecutor
    @Test
    public void listContainerObjectsCustomTest() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        CloseableIterator<ObjectEntry> result =
            customDistribution.listContainerObjects(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECT);
        assertNotNull(result);
        List<ObjectEntry> objectEntries = IteratorUtils.toList(result);
        assertThat(objectEntries).hasSize(1);
        assertThat(objectEntries.get(0).getObjectId()).isEqualTo("objectId");
        assertThat(objectEntries.get(0).getSize()).isEqualTo(100L);
    }

    @RunWithCustomExecutor
    @Test
    public void getOfferLogs() throws Exception {
        assertThatCode(() -> {
            simpleDistribution
                .getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECT, 0L, 0, Order.ASC);
        }).isInstanceOf(IllegalArgumentException.class);

        VitamThreadUtils.getVitamSession().setTenantId(0);
        RequestResponse<OfferLog> result =
            simpleDistribution
                .getOfferLogs(VitamConfiguration.getDefaultStrategy(), DataCategory.OBJECT, 0L, 0, Order.ASC);
        assertNotNull(result);
        assertTrue(result.isOk());

        assertThatCode(() -> {
            simpleDistribution.getOfferLogs(null, null, 0L, 0, Order.ASC);
        }).isInstanceOf(IllegalArgumentException.class);

        assertThatCode(() -> {
            simpleDistribution.getOfferLogs(VitamConfiguration.getDefaultStrategy(), null, 0L, 0, Order.ASC);
        }).isInstanceOf(IllegalArgumentException.class);

    }

    @RunWithCustomExecutor
    @Test
    public void getOfferLogsFromOfferId() throws Exception {
        assertThatCode(() -> {
            simpleDistribution
                .getOfferLogsByOfferId(VitamConfiguration.getDefaultStrategy(), OFFER_ID, DataCategory.OBJECT, 0L, 0,
                    Order.ASC);
        }).isInstanceOf(IllegalArgumentException.class);

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        assertThatCode(() -> {
            simpleDistribution
                .getOfferLogsByOfferId(VitamConfiguration.getDefaultStrategy(), null, DataCategory.OBJECT, 0L, 0,
                    Order.ASC);
        }).isInstanceOf(IllegalArgumentException.class);

        RequestResponse<OfferLog> result =
            simpleDistribution
                .getOfferLogsByOfferId(VitamConfiguration.getDefaultStrategy(), OFFER_ID, DataCategory.OBJECT, 1L, 2,
                    Order.ASC);
        assertNotNull(result);
        assertTrue(result.isOk());

        result =
            simpleDistribution
                .getOfferLogsByOfferId(VitamConfiguration.getDefaultStrategy(), OFFER_ID, DataCategory.OBJECT, 0L, 1,
                    Order.DESC);
        assertNotNull(result);
        assertTrue(result.isOk());

        assertThatCode(() -> {
            simpleDistribution.getOfferLogsByOfferId(null, OFFER_ID, null, 0L, 0, Order.ASC);
        }).isInstanceOf(IllegalArgumentException.class);

        assertThatCode(() -> {
            simpleDistribution
                .getOfferLogsByOfferId(VitamConfiguration.getDefaultStrategy(), OFFER_ID, null, 0L, 0, Order.ASC);
        }).isInstanceOf(IllegalArgumentException.class);

    }

    @RunWithCustomExecutor
    @Test
    public void getStorageStrategiesOk() {
        assertThatCode(() -> customDistribution.getStrategies()).doesNotThrowAnyException();
    }
}
