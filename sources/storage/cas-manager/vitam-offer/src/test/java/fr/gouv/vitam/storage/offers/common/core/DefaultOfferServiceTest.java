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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.ws.rs.core.Response;

import fr.gouv.vitam.common.stream.StreamUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.junit.AsyncResponseJunitTest;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.ObjectInit;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;

/**
 * Default offer service test implementation
 */
public class DefaultOfferServiceTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultOfferServiceTest.class);

    private static final String CONTAINER_PATH = "container";
    private static final DataCategory OBJECT_TYPE = DataCategory.OBJECT;
    private static final String FOLDER_PATH = "folder";
    private static final String OBJECT_ID = GUIDFactory.newObjectGUID(0).getId();
    private static final String OBJECT_ID_2 = GUIDFactory.newObjectGUID(0).getId();
    private static final String OBJECT_ID_3 = GUIDFactory.newObjectGUID(0).getId();
    private static final String OBJECT_ID_DELETE = GUIDFactory.newObjectGUID(0).getId();

    private static final String DEFAULT_STORAGE_CONF = "default-storage.conf";
    private static final String ARCHIVE_FILE_TXT = "archivefile.txt";
    private static final String OBJECT_ID_2_CONTENT = "Vitam Test Content";

    @After
    public void deleteFiles() throws Exception {
        final StorageConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
                StorageConfiguration.class);
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH, OBJECT_TYPE.getFolder(), OBJECT_ID));
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH, OBJECT_TYPE.getFolder(), OBJECT_ID_2));
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH, OBJECT_TYPE.getFolder(), OBJECT_ID_3));
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH, OBJECT_TYPE.getFolder(), OBJECT_ID_DELETE));
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH, OBJECT_TYPE.getFolder()));
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH, FOLDER_PATH));
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH, OBJECT_ID));
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH, OBJECT_ID_2));
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH, OBJECT_ID_3));
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH, OBJECT_ID_DELETE));
        for (int i = 0; i < 150; i++) {
            Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH, "object_" + i));
        }
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH));
        // Clean fake container part
        Path fakeContainerPath = Paths.get(conf.getStoragePath(), "fakeContainer");
        if (Files.exists(fakeContainerPath)) {
            Files.list(fakeContainerPath).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    LOGGER.error(e);
                }
            });
        }
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), "fakeContainer"));
    }

    @Test
    public void initOKTest() {
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        assertNotNull(offerService);
    }

    @Test
    public void createObjectTestNoContainer() throws Exception {
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        offerService.createObject("fakeContainer", OBJECT_ID, new FakeInputStream(1024), true, OBJECT_TYPE);
    }

    @Test
    public void createContainerTest() throws Exception {
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        assertNotNull(offerService);

        offerService.initCreateObject(CONTAINER_PATH, getObjectInit(false), OBJECT_ID);

        // check
        final StorageConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
                StorageConfiguration.class);
        final File container = new File(conf.getStoragePath() + CONTAINER_PATH);
        assertTrue(container.exists());
        assertTrue(container.isDirectory());

        offerService.initCreateObject(CONTAINER_PATH, getObjectInit(false), OBJECT_ID);
    }

    @Test
    public void countObjectsTest() throws Exception {
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        assertNotNull(offerService);

        offerService.initCreateObject(CONTAINER_PATH, getObjectInit(false), OBJECT_ID_2);
        final InputStream streamToStore = StreamUtils.toInputStream(OBJECT_ID_2_CONTENT);
        offerService.createObject(CONTAINER_PATH, OBJECT_ID_2, streamToStore, true, OBJECT_TYPE);

        JsonNode result = offerService.countObjects(CONTAINER_PATH);
        assertEquals(1, result.get("objectNumber").longValue());

    }

    @Test
    public void createObjectTest() throws Exception {
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        assertNotNull(offerService);

        // container
        ObjectInit objectInit = getObjectInit(false);
        objectInit = offerService.initCreateObject(CONTAINER_PATH, objectInit, OBJECT_ID);
        // check
        assertEquals(OBJECT_ID, objectInit.getId());
        final StorageConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
                StorageConfiguration.class);
        final File container = new File(conf.getStoragePath() + CONTAINER_PATH);
        assertTrue(container.exists());
        assertTrue(container.isDirectory());

        String computedDigest = null;

        // object
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            assertNotNull(in);
            computedDigest = offerService.createObject(CONTAINER_PATH, objectInit.getId(), in, true, OBJECT_TYPE);
        }
        // check
        final File testFile = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        final File offerFile = new File(CONTAINER_PATH + "/" + OBJECT_ID);
        assertTrue(com.google.common.io.Files.equal(testFile, offerFile));

        final Digest digest = Digest.digest(testFile, VitamConfiguration.getDefaultDigestType());
        assertEquals(computedDigest, digest.toString());
        assertEquals(offerService.getObjectDigest(CONTAINER_PATH, OBJECT_ID, VitamConfiguration.getDefaultDigestType()),
                digest.toString());

        assertTrue(offerService.isObjectExist(CONTAINER_PATH, OBJECT_ID));
    }

    // TODO activate when chunk mode is done in {@see DefaultOfferService}
    // method createObject
    @Test
    @Ignore
    public void createObjectChunkTest() throws Exception {
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        assertNotNull(offerService);

        // container
        ObjectInit objectInit = getObjectInit(false);
        objectInit = offerService.initCreateObject(CONTAINER_PATH, objectInit, OBJECT_ID);
        // check
        assertEquals(OBJECT_ID, objectInit.getId());
        final StorageConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
                StorageConfiguration.class);
        final File container = new File(conf.getStoragePath() + CONTAINER_PATH);
        assertTrue(container.exists());
        assertTrue(container.isDirectory());

        String computedDigest = null;

        // object
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            assertNotNull(in);

            final FileChannel fc = in.getChannel();
            final ByteBuffer bb = ByteBuffer.allocate(1024);

            byte[] bytes;
            int read = fc.read(bb);
            while (read >= 0) {
                bb.flip();
                if (fc.position() == fc.size()) {
                    bytes = new byte[read];
                    bb.get(bytes, 0, read);
                    computedDigest = offerService.createObject(CONTAINER_PATH, objectInit.getId(),
                            new ByteArrayInputStream(bytes), true, OBJECT_TYPE);
                } else {
                    bytes = bb.array();
                    computedDigest = offerService.createObject(CONTAINER_PATH, objectInit.getId(),
                            new ByteArrayInputStream(bytes.clone()), false, OBJECT_TYPE);
                    assertEquals(computedDigest,
                            Digest.digest(new ByteArrayInputStream(bytes.clone()), VitamConfiguration.getDefaultDigestType())
                                    .toString());
                }
                bb.clear();
                read = fc.read(bb);
            }
        }
        // check
        final File testFile = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        final File offerFile = new File(CONTAINER_PATH + "/" + objectInit.getType().getFolder() + "/" + OBJECT_ID);

        assertTrue(com.google.common.io.Files.equal(testFile, offerFile));

        final Digest digest = Digest.digest(testFile, VitamConfiguration.getDefaultDigestType());
        assertEquals(computedDigest, digest.toString());
        assertEquals(offerService.getObjectDigest(CONTAINER_PATH, objectInit.getType().getFolder() + "/" + OBJECT_ID,
                VitamConfiguration.getDefaultDigestType()), digest.toString());

        assertTrue(offerService.isObjectExist(CONTAINER_PATH, objectInit.getType().getFolder() + "/" + OBJECT_ID));
    }

    @Test
    public void getObjectTest() throws Exception {
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        assertNotNull(offerService);

        offerService.initCreateObject(CONTAINER_PATH, getObjectInit(false), OBJECT_ID_2);

        final InputStream streamToStore = StreamUtils.toInputStream(OBJECT_ID_2_CONTENT);
        offerService.createObject(CONTAINER_PATH, OBJECT_ID_2, streamToStore, true, OBJECT_TYPE);

        final Response response = offerService.getObject(CONTAINER_PATH, OBJECT_ID_2);
        assertNotNull(response);
    }

    private ObjectInit getObjectInit(boolean algo) throws IOException {
        final File file = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        final ObjectInit objectInit = new ObjectInit();
        if (algo) {
            objectInit.setDigestAlgorithm(VitamConfiguration.getDefaultDigestType());
        }
        objectInit.setSize(file.length());
        objectInit.setType(OBJECT_TYPE);
        return objectInit;
    }

    @Test
    public void getCapacityOk() throws Exception {
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        assertNotNull(offerService);

        // container
        ObjectInit objectInit = getObjectInit(false);
        objectInit = offerService.initCreateObject(CONTAINER_PATH, objectInit, OBJECT_ID);
        // check
        assertEquals(OBJECT_ID, objectInit.getId());
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
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        assertNotNull(offerService);
        final JsonNode jsonNode = offerService.getCapacity(CONTAINER_PATH);
        assertNotNull(jsonNode);
        assertNotNull(jsonNode.get("usableSpace"));
        assertNotNull(jsonNode.get("usedSpace"));
    }

    @Test
    public void checkObjectTest() throws Exception {
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        assertNotNull(offerService);

        final ObjectInit objectInit = getObjectInit(true);
        objectInit.setType(DataCategory.UNIT);
        offerService.initCreateObject(CONTAINER_PATH, objectInit, OBJECT_ID_3);

        final InputStream streamToStore = StreamUtils.toInputStream(OBJECT_ID_2_CONTENT);
        String digest = offerService.createObject(CONTAINER_PATH, OBJECT_ID_3, streamToStore, true, OBJECT_TYPE);

        assertTrue(offerService.checkObject(CONTAINER_PATH, OBJECT_ID_3, digest, VitamConfiguration.getDefaultDigestType()));
    }

    @Test
    public void deleteObjectTest() throws Exception {
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        assertNotNull(offerService);
        offerService.initCreateObject(CONTAINER_PATH, getObjectInit(false), OBJECT_ID_DELETE);

        // creation of an object
        final InputStream streamToStore = StreamUtils.toInputStream(OBJECT_ID_2_CONTENT);
        String digest = offerService.createObject(CONTAINER_PATH, OBJECT_ID_DELETE, streamToStore, true, DataCategory.UNIT);

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
        offerService.deleteObject(CONTAINER_PATH, OBJECT_ID_DELETE, digest, VitamConfiguration.getDefaultDigestType()
            , DataCategory.UNIT);

        try {
            // check that the object has been deleted
            offerService.getObject(CONTAINER_PATH, OBJECT_ID_DELETE);
            fail("Should raized an exception");
        } catch (ContentAddressableStorageNotFoundException exc) {

        }

    }

    @Test
    public void listCreateCursorNoContainerTest() throws Exception {
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        assertNotNull(offerService);
        offerService.createCursor(CONTAINER_PATH);
    }

    @Test
    public void listCreateCursorTest() throws Exception {
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        assertNotNull(offerService);
        final ObjectInit objectInit = getObjectInit(false);
        offerService.initCreateObject(CONTAINER_PATH, objectInit, "fake");
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
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        assertNotNull(offerService);
        String id = offerService.createCursor(CONTAINER_PATH);
        assertNotNull(id);
        offerService.finalizeCursor(CONTAINER_PATH, id);
    }

    @Test
    public void listCursorTest() throws Exception {
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        assertNotNull(offerService);
        final ObjectInit objectInit = getObjectInit(false);
        for (int i = 0; i < 150; i++) {
            offerService.initCreateObject(CONTAINER_PATH, objectInit, "object_" + i);
            offerService.createObject(CONTAINER_PATH, "object_" + i, new FakeInputStream(50), true, OBJECT_TYPE);
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
}
