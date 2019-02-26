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
package fr.gouv.vitam.common.storage.cas.container.api;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Abstract Junit Test for classes that implements ContentAddressableStorage API
 */
public abstract class ContentAddressableStorageTestAbstract {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    protected ContentAddressableStorage storage;
    protected File tempDir;
    protected static final String CONTAINER_NAME = "myContainer";
    protected static final String OBJECT_NAME = GUIDFactory.newGUID().getId() + ".json";
    protected static final DigestType ALGO = DigestType.SHA512;

    protected static final String TENANT_ID = "0";
    protected static final String TYPE = "object";
    protected static final String OBJECT_ID = "aeaaaaaaaaaam7mxaa2pkak2bnhxy5aaaaaq";
    protected static final String OBJECT_ID2 = "aeaaaaaaaaaam7mxaa2pkak2bnhxy4aaaaaq";

    // Container
    @Test
    public void givenContainerNotFoundWhenCheckContainerExistenceThenRetunFalse() {
        assertFalse(storage.isExistingContainer(CONTAINER_NAME));
    }

    @Test
    public void givenContainerAlreadyExistsWhenCheckContainerExistenceThenRetunFalse()
            throws ContentAddressableStorageServerException {
        storage.createContainer(CONTAINER_NAME);
        assertTrue(storage.isExistingContainer(CONTAINER_NAME));
    }

    @Test
    public void givenContainerNotFoundWhenCreateContainerThenOK()
            throws ContentAddressableStorageServerException {
        storage.createContainer(CONTAINER_NAME);
        assertTrue(storage.isExistingContainer(CONTAINER_NAME));
    }

    // Object
    @Test
    public void givenObjectNotFoundWhenCheckObjectExistenceThenRetunFalse()
            throws ContentAddressableStorageServerException {
        assertFalse(storage.isExistingObject(CONTAINER_NAME, OBJECT_NAME));
    }

    @Test
    public void givenObjectAlreadyExistsWhenCheckObjectExistenceThenRetunFalse()
            throws IOException, ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), DigestType.SHA512, null);

        assertTrue(storage.isExistingObject(CONTAINER_NAME, OBJECT_NAME));
    }

    @Test
    public void givenObjectAlreadyExistsWhenPutObjectThenNotRaiseAnException()
            throws IOException, ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), DigestType.SHA512, null);

        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file2.pdf"), DigestType.SHA512, null);
        assertEquals(getInputStream("file2.pdf").available(),
                storage.getObject(CONTAINER_NAME, OBJECT_NAME).getInputStream().available());
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenDeleteObjectThenRaiseAnException()
            throws ContentAddressableStorageException {
        storage.deleteObject(CONTAINER_NAME, OBJECT_NAME);

    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenObjectNotFoundWhenDeleteObjectThenRaiseAnException()
            throws IOException, ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), DigestType.SHA512, null);
        storage.deleteObject(CONTAINER_NAME, OBJECT_NAME);

        storage.deleteObject(CONTAINER_NAME, OBJECT_NAME);
    }


    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenGetObjectThenRaiseAnException()
            throws ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.getObject(CONTAINER_NAME, OBJECT_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenPutObjectThenRaiseAnException()
            throws IOException, ContentAddressableStorageException {
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), DigestType.SHA512, null);
    }

    @Test
    public void givenObjectNotFoundWhenPutObjectThenOK() throws Exception {
        storage.createContainer(CONTAINER_NAME);

        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), DigestType.SHA512, null);
        assertEquals(getInputStream("file1.pdf").available(),
                storage.getObject(CONTAINER_NAME, OBJECT_NAME).getInputStream().available());
    }

    @Test
    public void givenObjectAlreadyExistsWhenDeleteObjectThenOK() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), DigestType.SHA512, null);

        storage.deleteObject(CONTAINER_NAME, OBJECT_NAME);
        assertFalse(storage.isExistingObject(CONTAINER_NAME, OBJECT_NAME));
    }


    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenComputeObjectDigestThenRaiseAnException()
            throws ContentAddressableStorageException {
        storage.getObjectDigest(CONTAINER_NAME, OBJECT_NAME, ALGO, true);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenObjectNotFoundWhenComputeObjectDigestThenRaiseAnException()
            throws ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.getObjectDigest(CONTAINER_NAME, OBJECT_NAME, ALGO, true);
    }

    @Test
    public void givenObjectAlreadyExistsWhenWhenComputeObjectDigestThenOK()
            throws ContentAddressableStorageException, IOException {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), DigestType.SHA512, null);

        String messageDigest = storage.getObjectDigest(CONTAINER_NAME, OBJECT_NAME, ALGO, true);
        Digest digest = new Digest(ALGO);
        digest.update(getInputStream("file1.pdf"));
        System.out.print(digest);
        System.out.print(messageDigest);
        assertTrue(messageDigest.equals(digest.toString()));
        // Verify that it works from cache (if there is a cache)
        messageDigest = storage.getObjectDigest(CONTAINER_NAME, OBJECT_NAME, ALGO, true);
        assertTrue(messageDigest.equals(digest.toString()));
    }

    // Check Path parameters (containerName,folder, objectName)
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenCreateContainerThenRaiseAnException() {
        ParametersChecker.checkParameter("Null Param", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenCreateContainerThenRaiseAnException() {
        ParametersChecker.checkParameter("Empty Param", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenCreateFolderThenRaiseAnException() {
        ParametersChecker.checkParameter("Null Param", CONTAINER_NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenCreateFolderOThenRaiseAnException() {
        ParametersChecker.checkParameter("Empty Param", CONTAINER_NAME, "");
    }

    private InputStream getInputStream(String file) throws IOException {
        return PropertiesUtils.getResourceAsStream(file);
    }

    @Test
    public void getContainerInformationOK() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        final ContainerInformation containerInformation = storage.getContainerInformation(CONTAINER_NAME);
        assertNotNull(containerInformation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getContainerInformationContainerNameNull() throws Exception {
        final ContainerInformation containerInformation = storage.getContainerInformation(null);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void getContainerInformationStorageNotFoundException() throws Exception {
        storage.getContainerInformation(CONTAINER_NAME);
    }

    @Test
    public void givenObjectAlreadyExistsWhenGetObjectMetadataThenNotRaiseAnException() throws Exception {
        String containerName = TENANT_ID + "_" + TYPE;
        storage.createContainer(containerName);
        storage.putObject(containerName, OBJECT_ID, getInputStream("file1.pdf"), DigestType.SHA512, null);
        storage.putObject(containerName, OBJECT_ID2, getInputStream("file2.pdf"), DigestType.SHA512, null);
        //get metadata of file
        MetadatasObject result = storage.getObjectMetadatas(containerName, OBJECT_ID, true);
        assertEquals(OBJECT_ID, result.getObjectName());
        assertEquals(TYPE, result.getType());
        assertEquals(
                "9ba9ef903b46798c83d46bcbd42805eb69ad1b6a8b72e929f87d72f5263a05ade47d8e2f860aece8b9e3acb948364fedf75a3367515cd912965ed22a246ea418",
                result.getDigest());
        assertEquals("Vitam_" + TENANT_ID, result.getFileOwner());
        assertEquals(6906, result.getFileSize());
        assertNotNull(result.getLastAccessDate());
        assertNotNull(result.getLastModifiedDate());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getObjectMetadataOnObjectIdNullShouldRaiseAnException() throws Exception {
        storage.getObjectMetadatas("containerName", null, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getObjectMetadataOnContainerNullShouldRaiseAnException() throws Exception {
        storage.getObjectMetadatas(null, "objectId", true);
    }

    @Test
    public void listTest() throws Exception {
        int nbIter = 1;
        String containerName = "object_0";
        storage.createContainer(containerName);
        assertNotNull(storage.getContainerInformation(containerName));
        for (int i = 0; i < 100; i++) {
            storage.putObject(containerName, GUIDFactory.newGUID().getId(), new FakeInputStream(100), DigestType.SHA512,
                    null);
        }
        VitamPageSet<? extends VitamStorageMetadata> pageSet = storage.listContainer(containerName);
        assertNotNull(pageSet);
        assertFalse(pageSet.isEmpty());
        assertEquals(100, pageSet.size());

        for (int i = 100; i < (nbIter * 100 + 50); i++) {
            storage.putObject(containerName, GUIDFactory.newGUID().getId(), new FakeInputStream(100), DigestType.SHA512,
                    null);
        }
        // First list without marker
        pageSet = storage.listContainer(containerName);
        // Loop with marker with complete PageSet (100 elements)
        for (int i = 1; i < nbIter; i++) {
            pageSet = storage.listContainerNext(containerName, pageSet.getNextMarker());
            assertNotNull(pageSet);
            assertFalse(pageSet.isEmpty());
            assertEquals(100, pageSet.size());
            assertNotNull(pageSet.getNextMarker());
        }
        // Last listContainer with only 50 results
        pageSet = storage.listContainerNext(containerName, pageSet.getNextMarker());
        assertNotNull(pageSet);
        assertFalse(pageSet.isEmpty());
        assertEquals(50, pageSet.size());
        assertNull(pageSet.getNextMarker());

    }

}
