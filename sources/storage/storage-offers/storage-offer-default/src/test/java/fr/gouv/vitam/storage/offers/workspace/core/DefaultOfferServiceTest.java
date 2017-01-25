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

package fr.gouv.vitam.storage.offers.workspace.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.server.application.junit.AsyncResponseJunitTest;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.ObjectInit;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.core.WorkspaceConfiguration;

/**
 * Default offer service test implementation
 */
public class DefaultOfferServiceTest {

    private static final String CONTAINER_PATH = "container";
    private static final DataCategory OBJECT_TYPE = DataCategory.OBJECT;
    private static final String FOLDER_PATH = "folder";
    private static final String OBJECT_ID = GUIDFactory.newObjectGUID(0).getId();
    private static final String OBJECT_ID_2 = GUIDFactory.newObjectGUID(0).getId();
    private static final String DEFAULT_STORAGE_CONF = "default-storage.conf";
    private static final String ARCHIVE_FILE_TXT = "archivefile.txt";
    private static final String OBJECT_ID_2_CONTENT = "Vitam Test Content";


    @After
    public void deleteFiles() throws Exception {
        final WorkspaceConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            WorkspaceConfiguration.class);
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH, OBJECT_TYPE.getFolder(), OBJECT_ID));
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH, OBJECT_TYPE.getFolder(), OBJECT_ID_2));
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH, OBJECT_TYPE.getFolder()));
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH, FOLDER_PATH));
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH, OBJECT_ID));
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH, OBJECT_ID_2));
        Files.deleteIfExists(Paths.get(conf.getStoragePath(), CONTAINER_PATH));
    }

    @Test
    public void initOKTest() {
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        assertNotNull(offerService);
    }

    @Test(expected = ContentAddressableStorageException.class)
    public void createObjectTestKO() throws Exception {
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        offerService.createObject("fakeContainer", OBJECT_ID, null, true);
    }

    @Test
    public void createContainerTest() throws Exception {
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        assertNotNull(offerService);

        offerService.initCreateObject(CONTAINER_PATH, getObjectInit(false), OBJECT_ID);

        // check
        final WorkspaceConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            WorkspaceConfiguration.class);
        final File container = new File(conf.getStoragePath() + CONTAINER_PATH);
        assertTrue(container.exists());
        assertTrue(container.isDirectory());

        offerService.initCreateObject(CONTAINER_PATH, getObjectInit(false), OBJECT_ID);
    }

    @Test
    public void createFolderTest() throws Exception {
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        assertNotNull(offerService);

        // container (init)
        offerService.initCreateObject(CONTAINER_PATH, getObjectInit(true), GUIDFactory.newObjectGUID(0).getId());
        // check
        final WorkspaceConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            WorkspaceConfiguration.class);
        final File container = new File(conf.getStoragePath() + CONTAINER_PATH);
        assertTrue(container.exists());
        assertTrue(container.isDirectory());

        // folder
        offerService.createFolder(CONTAINER_PATH, FOLDER_PATH);
        // check
        final File folder = new File(container.getAbsolutePath() + "/" + FOLDER_PATH);
        assertTrue(folder.exists());
        assertTrue(folder.isDirectory());
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
        final WorkspaceConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            WorkspaceConfiguration.class);
        final File container = new File(conf.getStoragePath() + CONTAINER_PATH);
        assertTrue(container.exists());
        assertTrue(container.isDirectory());

        String computedDigest = null;

        // object
        try (FileInputStream in = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            assertNotNull(in);
            computedDigest = offerService.createObject(CONTAINER_PATH, objectInit.getId(),
                in, true);
        }
        // check
        final File testFile = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        final File offerFile = new File(CONTAINER_PATH + "/" + OBJECT_ID);
        assertTrue(com.google.common.io.Files.equal(testFile, offerFile));

        final Digest digest = Digest.digest(testFile, VitamConfiguration.getDefaultDigestType());
        assertEquals(computedDigest, digest.toString());
        assertEquals(
            offerService.getObjectDigest(CONTAINER_PATH, OBJECT_ID,
                VitamConfiguration.getDefaultDigestType()),
            digest.toString());

        assertTrue(offerService.isObjectExist(CONTAINER_PATH, OBJECT_ID));
    }

    // TODO activate when chunk mode is done in {@see DefaultOfferService} method createObject
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
        final WorkspaceConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            WorkspaceConfiguration.class);
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
                        new ByteArrayInputStream(bytes), true);
                } else {
                    bytes = bb.array();
                    computedDigest = offerService.createObject(CONTAINER_PATH, objectInit.getId(),
                        new ByteArrayInputStream(bytes.clone()), false);
                    assertEquals(computedDigest,
                        Digest
                            .digest(new ByteArrayInputStream(bytes.clone()), VitamConfiguration.getDefaultDigestType())
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
        assertEquals(
            offerService.getObjectDigest(CONTAINER_PATH, objectInit.getType().getFolder() + "/" + OBJECT_ID,
                VitamConfiguration.getDefaultDigestType()),
            digest.toString());

        assertTrue(offerService.isObjectExist(CONTAINER_PATH, objectInit.getType().getFolder() + "/" + OBJECT_ID));
    }

    @Test
    public void getObjectTest() throws Exception {
        final DefaultOfferService offerService = DefaultOfferServiceImpl.getInstance();
        assertNotNull(offerService);

        final ObjectInit objectInit = getObjectInit(false);
        offerService.initCreateObject(CONTAINER_PATH, getObjectInit(false), OBJECT_ID_2);

        final InputStream streamToStore = IOUtils.toInputStream(OBJECT_ID_2_CONTENT);
        offerService.createObject(CONTAINER_PATH, OBJECT_ID_2, streamToStore, true);

        final Response response = offerService.getObject(CONTAINER_PATH,
            OBJECT_ID_2, new AsyncResponseJunitTest());
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
        final WorkspaceConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
            WorkspaceConfiguration.class);
        final File container = new File(conf.getStoragePath() + CONTAINER_PATH);
        assertTrue(container.exists());
        assertTrue(container.isDirectory());

        final JsonNode jsonNode = offerService.getCapacity(CONTAINER_PATH);
        assertNotNull(jsonNode);
        assertNotNull(jsonNode.get("usableSpace"));
        assertNotNull(jsonNode.get("usedSpace"));
    }
}
