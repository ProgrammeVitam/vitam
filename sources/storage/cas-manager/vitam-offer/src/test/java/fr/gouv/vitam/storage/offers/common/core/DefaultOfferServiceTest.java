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

package fr.gouv.vitam.storage.offers.common.core;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.offers.common.database.OfferLogDatabaseService;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageDatabaseException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.NullInputStream;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * Default offer service test implementation
 */
public class DefaultOfferServiceTest {

    private static final String CONTAINER_PATH = "container";
    private static final DataCategory OBJECT_TYPE = DataCategory.OBJECT;
    private static final DataCategory UNIT_TYPE = DataCategory.UNIT;
    private static final String OBJECT_ID = GUIDFactory.newObjectGUID(0).getId();
    private static final String OBJECT_ID_2 = GUIDFactory.newObjectGUID(0).getId();
    private static final String OBJECT_ID_3 = GUIDFactory.newObjectGUID(0).getId();
    private static final String OBJECT_ID_DELETE = GUIDFactory.newObjectGUID(0).getId();

    private static final String DEFAULT_STORAGE_CONF = "default-storage.conf";
    private static final String ARCHIVE_FILE_TXT = "archivefile.txt";
    private static final String ARCHIVE_FILE2_TXT = "archivefile_v2.txt";
    private static final String OBJECT_ID_2_CONTENT = "Vitam Test Content";
    private static final String FAKE_CONTAINER = "fakeContainer";
    private static final String OBJECT = "object_";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();


    @Mock
    private OfferLogDatabaseService offerDatabaseService;

    @After
    public void deleteFiles() throws Exception {
        final StorageConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            StorageConfiguration.class);
        cleanQuietly(conf, CONTAINER_PATH);
        cleanQuietly(conf, FAKE_CONTAINER);
    }

    private void cleanQuietly(StorageConfiguration conf, String container) throws IOException {
        File directory = Paths.get(conf.getStoragePath(), container).toFile();
        if(directory.exists()) {
            FileUtils.cleanDirectory(directory);
        }
    }

    @Test
    public void initOKTest() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);
    }

    @Test
    public void createObjectTestNoContainer() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        offerService.createObject(FAKE_CONTAINER, OBJECT_ID, new FakeInputStream(1024), OBJECT_TYPE, null, VitamConfiguration.getDefaultDigestType());
    }

    @Test
    public void createContainerTest() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);
        offerService.createObject(CONTAINER_PATH, OBJECT_ID, new NullInputStream(0), DataCategory.AGENCIES, 0L, DigestType.SHA512);

        // check
        final StorageConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            StorageConfiguration.class);
        final File container = new File(conf.getStoragePath() + CONTAINER_PATH);
        assertTrue(container.exists());
        assertTrue(container.isDirectory());
    }

    @Test
    public void createObjectTest() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);

        // object
        String computedDigest;
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            computedDigest = offerService.createObject(CONTAINER_PATH, OBJECT_ID, in, OBJECT_TYPE, null,
                VitamConfiguration.getDefaultDigestType());
        }

        // check
        final StorageConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            StorageConfiguration.class);
        final File container = new File(conf.getStoragePath() + CONTAINER_PATH);
        assertTrue(container.exists());
        assertTrue(container.isDirectory());

        // check
        final File testFile = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        final File offerFile = new File(CONTAINER_PATH + "/" + OBJECT_ID);
        assertTrue(com.google.common.io.Files.equal(testFile, offerFile));

        final Digest digest = Digest.digest(testFile, VitamConfiguration.getDefaultDigestType());
        assertEquals(computedDigest, digest.toString());
        assertEquals(offerService.getObjectDigest(CONTAINER_PATH, OBJECT_ID, VitamConfiguration.getDefaultDigestType()),
            digest.toString());

        assertTrue(offerService.isObjectExist(CONTAINER_PATH, OBJECT_ID));
        verify(offerDatabaseService).save(CONTAINER_PATH, OBJECT_ID, "write");
    }

    @Test
    public void createObject_OverrideExistingUpdatableObject() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);

        // object
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            offerService.createObject(CONTAINER_PATH, OBJECT_ID, in, UNIT_TYPE, null,
                VitamConfiguration.getDefaultDigestType());
        }
        String computedDigestV2;
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE2_TXT))) {
            computedDigestV2 = offerService.createObject(CONTAINER_PATH, OBJECT_ID, in, UNIT_TYPE, null,
                VitamConfiguration.getDefaultDigestType());
        }

        // check
        final File testFile = PropertiesUtils.findFile(ARCHIVE_FILE2_TXT);
        final StorageConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            StorageConfiguration.class);
        final File container = new File(conf.getStoragePath() + CONTAINER_PATH);
        final File offerFile = new File(container, OBJECT_ID);        assertTrue(com.google.common.io.Files.equal(testFile, offerFile));

        final Digest digest = Digest.digest(testFile, VitamConfiguration.getDefaultDigestType());
        assertEquals(computedDigestV2, digest.toString());
        assertEquals(offerService.getObjectDigest(CONTAINER_PATH, OBJECT_ID, VitamConfiguration.getDefaultDigestType()),
            digest.toString());

        assertTrue(offerService.isObjectExist(CONTAINER_PATH, OBJECT_ID));
        verify(offerDatabaseService, times(2)).save(CONTAINER_PATH, OBJECT_ID, "write");
    }

    @Test
    public void createObject_OverrideExistingNonUpdatableObjectWithSameContent() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);

        // object
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            offerService.createObject(CONTAINER_PATH, OBJECT_ID, in, OBJECT_TYPE, null,
                VitamConfiguration.getDefaultDigestType());
        }
        String computedDigestV2;
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            computedDigestV2 = offerService.createObject(CONTAINER_PATH, OBJECT_ID, in, OBJECT_TYPE, null,
                VitamConfiguration.getDefaultDigestType());
        }

        // check
        final File testFile = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        final StorageConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            StorageConfiguration.class);
        final File container = new File(conf.getStoragePath() + CONTAINER_PATH);
        final File offerFile = new File(container, OBJECT_ID);
        assertTrue(com.google.common.io.Files.equal(testFile, offerFile));

        final Digest digest = Digest.digest(testFile, VitamConfiguration.getDefaultDigestType());
        assertEquals(computedDigestV2, digest.toString());
        assertEquals(offerService.getObjectDigest(CONTAINER_PATH, OBJECT_ID, VitamConfiguration.getDefaultDigestType()),
            digest.toString());

        assertTrue(offerService.isObjectExist(CONTAINER_PATH, OBJECT_ID));
        verify(offerDatabaseService, times(2)).save(CONTAINER_PATH, OBJECT_ID, "write");
    }

    @Test
    public void createObject_TryOverrideExistingNonUpdatableObjectWithDifferentContentFails() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);

        // Given
        String computedDigestV1;
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            computedDigestV1 = offerService.createObject(CONTAINER_PATH, OBJECT_ID, in, OBJECT_TYPE, null,
                VitamConfiguration.getDefaultDigestType());
        }

        // When / Then
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE2_TXT))) {
            assertThatThrownBy(
                () -> offerService.createObject(CONTAINER_PATH, OBJECT_ID, in, OBJECT_TYPE, null,
                    VitamConfiguration.getDefaultDigestType()))
                .isInstanceOf(NonUpdatableContentAddressableStorageException.class);
        }

        final File testFile = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        final StorageConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            StorageConfiguration.class);
        final File container = new File(conf.getStoragePath() + CONTAINER_PATH);
        final File offerFile = new File(container, OBJECT_ID);
        assertTrue(com.google.common.io.Files.equal(testFile, offerFile));

        final Digest digest = Digest.digest(testFile, VitamConfiguration.getDefaultDigestType());
        assertEquals(computedDigestV1, digest.toString());
        assertEquals(offerService.getObjectDigest(CONTAINER_PATH, OBJECT_ID, VitamConfiguration.getDefaultDigestType()),
            digest.toString());

        assertTrue(offerService.isObjectExist(CONTAINER_PATH, OBJECT_ID));
        verify(offerDatabaseService, times(1)).save(CONTAINER_PATH, OBJECT_ID, "write");
    }

    @Test
    public void getObjectTest() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);

        final InputStream streamToStore = StreamUtils.toInputStream(OBJECT_ID_2_CONTENT);
        offerService.createObject(CONTAINER_PATH, OBJECT_ID_2, streamToStore, OBJECT_TYPE, null, VitamConfiguration.getDefaultDigestType());

        final Response response = offerService.getObject(CONTAINER_PATH, OBJECT_ID_2);
        assertNotNull(response);
    }

    @Test
    public void getCapacityOk() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);
        offerService.createObject(CONTAINER_PATH, OBJECT_ID, new NullInputStream(0), DataCategory.AGENCIES, 0L, DigestType.SHA512);
        // check
        final StorageConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            StorageConfiguration.class);
        final File container = new File(conf.getStoragePath() + CONTAINER_PATH);
        assertTrue(container.exists());
        assertTrue(container.isDirectory());

        final JsonNode jsonNode = offerService.getCapacity(CONTAINER_PATH);
        assertNotNull(jsonNode);
        assertNotNull(jsonNode.get("usableSpace"));
        assertNotNull(jsonNode.get("usedSpace"));
    }

    @Test
    public void getCapacityNoContainerOK() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);
        final JsonNode jsonNode = offerService.getCapacity(CONTAINER_PATH);
        assertNotNull(jsonNode);
        assertNotNull(jsonNode.get("usableSpace"));
        assertNotNull(jsonNode.get("usedSpace"));
    }

    @Test
    public void checkObjectTest() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);

        final InputStream streamToStore = StreamUtils.toInputStream(OBJECT_ID_2_CONTENT);
        String digest = offerService.createObject(CONTAINER_PATH, OBJECT_ID_3, streamToStore, OBJECT_TYPE, null, VitamConfiguration.getDefaultDigestType());

        assertTrue(
            offerService.checkObject(CONTAINER_PATH, OBJECT_ID_3, digest, VitamConfiguration.getDefaultDigestType()));
    }

    @Test
    public void deleteObjectTest() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);

        // creation of an object
        final InputStream streamToStore = StreamUtils.toInputStream(OBJECT_ID_2_CONTENT);

        String digest =
            offerService.createObject(CONTAINER_PATH, OBJECT_ID_DELETE, streamToStore,
                DataCategory.UNIT, null, VitamConfiguration.getDefaultDigestType());

        // check if the object has been created
        final Response response = offerService.getObject(CONTAINER_PATH, OBJECT_ID_DELETE);
        assertNotNull(response);

        try {
            // check that if we try to delete an object with a wrong digest, we
            // get a not found exception
            offerService.deleteObject(CONTAINER_PATH, OBJECT_ID_DELETE, "fakeDigest", VitamConfiguration
                .getDefaultDigestType(), DataCategory.UNIT);
            fail("Should raized an exception");
        } catch (ContentAddressableStorageNotFoundException exc) {

        }

        try {
            // check that if we try to delete an object with the wrong digest
            // algorithm, we get a not found exception
            offerService.deleteObject(CONTAINER_PATH, OBJECT_ID_DELETE, digest, VitamConfiguration
                .getSecurityDigestType(), DataCategory.UNIT);
            fail("Should raized an exception");
        } catch (ContentAddressableStorageNotFoundException exc) {

        }

        // check that if we try to delete an object with the correct digest +
        // algorithm, it succeeds
        offerService.deleteObject(CONTAINER_PATH, OBJECT_ID_DELETE, digest, VitamConfiguration.getDefaultDigestType(),
            DataCategory.UNIT);

        try {
            // check that the object has been deleted
            offerService.getObject(CONTAINER_PATH, OBJECT_ID_DELETE);
            fail("Should raized an exception");
        } catch (ContentAddressableStorageNotFoundException exc) {

        }

    }

    @Test
    public void listCreateCursorNoContainerTest() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);
        offerService.createCursor(CONTAINER_PATH);
    }

    @Test
    public void listCreateCursorTest() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);
        String cursorId = offerService.createCursor(CONTAINER_PATH);
        assertNotNull(cursorId);
        List<JsonNode> list = offerService.next(CONTAINER_PATH, cursorId);
        assertNotNull(list);
        assertTrue(list.isEmpty());
        list = offerService.next(CONTAINER_PATH, cursorId);
        // TODO manage with exception
        assertNull(list);
    }

    @Test
    public void finalizeCursorTest() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);
        String id = offerService.createCursor(CONTAINER_PATH);
        assertNotNull(id);
        offerService.finalizeCursor(CONTAINER_PATH, id);
    }

    @Test
    public void listCursorTest() throws Exception {
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);
        for (int i = 0; i < 150; i++) {
            offerService.createObject(CONTAINER_PATH, OBJECT + i, new FakeInputStream(50), OBJECT_TYPE, null, VitamConfiguration.getDefaultDigestType());
        }
        String cursorId = offerService.createCursor(CONTAINER_PATH);
        assertNotNull(cursorId);
        boolean hasNext = offerService.hasNext(CONTAINER_PATH, cursorId);
        assertTrue(hasNext);

        List<JsonNode> list = offerService.next(CONTAINER_PATH, cursorId);
        assertNotNull(list);
        assertEquals(100, list.size());

        hasNext = offerService.hasNext(CONTAINER_PATH, cursorId);
        assertTrue(hasNext);

        list = offerService.next(CONTAINER_PATH, cursorId);
        assertNotNull(list);
        assertEquals(50, list.size());

        hasNext = offerService.hasNext(CONTAINER_PATH, cursorId);
        assertFalse(hasNext);

        list = offerService.next(CONTAINER_PATH, cursorId);
        // TODO manage with exception
        assertNull(list);
    }

    @Test
    public void getOfferLogs() throws Exception {
        when(offerDatabaseService.searchOfferLog(CONTAINER_PATH, 0L, 2, Order.DESC))
            .thenReturn(getOfferLogs(CONTAINER_PATH, 0, 2, Order.DESC));
        when(offerDatabaseService.searchOfferLog(CONTAINER_PATH, 2L, 3, Order.DESC))
            .thenThrow(new ContentAddressableStorageDatabaseException("database error"));
        when(offerDatabaseService.searchOfferLog(CONTAINER_PATH, 5L, 4, Order.DESC))
            .thenThrow(new ContentAddressableStorageServerException("parse error"));
        final DefaultOfferService offerService = new DefaultOfferServiceImpl(offerDatabaseService);
        assertNotNull(offerService);

        assertThatCode(() -> {
            offerService.getOfferLogs(CONTAINER_PATH, 0L, 2, Order.DESC);
        }).doesNotThrowAnyException();
        assertThatCode(() -> {
            offerService.getOfferLogs(CONTAINER_PATH, 2L, 3, Order.DESC);
        }).isInstanceOf(ContentAddressableStorageDatabaseException.class);
        assertThatCode(() -> {
            offerService.getOfferLogs(CONTAINER_PATH, 5L, 4, Order.DESC);
        }).isInstanceOf(ContentAddressableStorageServerException.class);
    }

    private List<OfferLog> getOfferLogs(String containerName, long offset, int limit, Order order) {
        List<OfferLog> offerLogs = new ArrayList<>();
        LongStream.range(offset + 1, offset + 1 + limit).forEach(l -> {
            OfferLog offerLog = new OfferLog(containerName, OBJECT + l, "write");
            offerLog.setSequence(l);
            offerLog.setTime(LocalDateTime.now());
            offerLogs.add(offerLog);
        });
        return offerLogs;
    }
}
