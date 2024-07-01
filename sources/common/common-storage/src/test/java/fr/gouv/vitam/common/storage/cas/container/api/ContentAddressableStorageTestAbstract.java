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
package fr.gouv.vitam.common.storage.cas.container.api;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    public void givenContainerNotFoundWhenCheckContainerExistenceThenReturnFalse()
        throws ContentAddressableStorageServerException {
        assertFalse(storage.isExistingContainer(CONTAINER_NAME));
    }

    @Test
    public void givenContainerAlreadyExistsWhenCheckContainerExistenceThenReturnFalse()
        throws ContentAddressableStorageServerException {
        storage.createContainer(CONTAINER_NAME);
        assertTrue(storage.isExistingContainer(CONTAINER_NAME));
    }

    @Test
    public void givenContainerNotFoundWhenCreateContainerThenOK() throws ContentAddressableStorageServerException {
        storage.createContainer(CONTAINER_NAME);
        assertTrue(storage.isExistingContainer(CONTAINER_NAME));
    }

    // Object
    @Test
    public void givenObjectNotFoundWhenCheckObjectExistenceThenReturnFalse() throws ContentAddressableStorageException {
        assertFalse(storage.isExistingObject(CONTAINER_NAME, OBJECT_NAME));
    }

    @Test
    public void givenObjectAlreadyExistsWhenCheckObjectExistenceThenReturnFalse()
        throws IOException, ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), DigestType.SHA512, 6906L);

        assertTrue(storage.isExistingObject(CONTAINER_NAME, OBJECT_NAME));
    }

    @Test
    public void givenObjectAlreadyExistsWhenPutObjectThenNotRaiseAnException()
        throws IOException, ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), DigestType.SHA512, 6906L);

        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file2.pdf"), DigestType.SHA512, 6937L);
        assertEquals(
            getInputStream("file2.pdf").available(),
            storage.getObject(CONTAINER_NAME, OBJECT_NAME).getInputStream().available()
        );
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenDeleteObjectThenRaiseAnException() throws ContentAddressableStorageException {
        storage.deleteObject(CONTAINER_NAME, OBJECT_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenObjectNotFoundWhenDeleteObjectThenRaiseAnException()
        throws IOException, ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), DigestType.SHA512, 6906L);
        storage.deleteObject(CONTAINER_NAME, OBJECT_NAME);

        storage.deleteObject(CONTAINER_NAME, OBJECT_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenGetObjectThenRaiseAnException() throws ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.getObject(CONTAINER_NAME, OBJECT_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenPutObjectThenRaiseAnException()
        throws IOException, ContentAddressableStorageException {
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), DigestType.SHA512, 6906L);
    }

    @Test
    public void givenObjectNotFoundWhenPutObjectThenOK() throws Exception {
        storage.createContainer(CONTAINER_NAME);

        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), DigestType.SHA512, 6906L);

        assertThat(storage.getObject(CONTAINER_NAME, OBJECT_NAME).getInputStream()).hasSameContentAs(
            getInputStream("file1.pdf")
        );
    }

    @Test
    public void givenObjectAlreadyExistsWhenDeleteObjectThenOK() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), DigestType.SHA512, 6906L);

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
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), DigestType.SHA512, 6906L);

        String messageDigest = storage.getObjectDigest(CONTAINER_NAME, OBJECT_NAME, ALGO, true);
        Digest digest = new Digest(ALGO);
        digest.update(getInputStream("file1.pdf"));
        System.out.print(digest);
        System.out.print(messageDigest);
        assertEquals(messageDigest, digest.toString());
        // Verify that it works from cache (if there is a cache)
        messageDigest = storage.getObjectDigest(CONTAINER_NAME, OBJECT_NAME, ALGO, true);
        assertEquals(messageDigest, digest.toString());
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
        storage.getContainerInformation(null);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void getContainerInformationStorageNotFoundException() throws Exception {
        storage.getContainerInformation(CONTAINER_NAME);
    }

    @Test
    public void givenObjectAlreadyExistsWhenGetObjectMetadataThenNotRaiseAnException() throws Exception {
        String containerName = TENANT_ID + "_" + TYPE;
        storage.createContainer(containerName);
        storage.putObject(containerName, OBJECT_ID, getInputStream("file1.pdf"), DigestType.SHA512, 6906L);
        storage.putObject(containerName, OBJECT_ID2, getInputStream("file2.pdf"), DigestType.SHA512, 6937L);
        //get metadata of file
        MetadatasObject result = storage.getObjectMetadata(containerName, OBJECT_ID, true);
        assertEquals(OBJECT_ID, result.getObjectName());
        assertEquals(TYPE, result.getType());
        assertEquals(
            "9ba9ef903b46798c83d46bcbd42805eb69ad1b6a8b72e929f87d72f5263a05ade47d8e2f860aece8b9e3acb948364fedf75a3367515cd912965ed22a246ea418",
            result.getDigest()
        );
        assertEquals(6906, result.getFileSize());
        assertNotNull(result.getLastAccessDate());
        assertNotNull(result.getLastModifiedDate());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getObjectMetadataOnObjectIdNullShouldRaiseAnException() throws Exception {
        storage.getObjectMetadata("containerName", null, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getObjectMetadataOnContainerNullShouldRaiseAnException() throws Exception {
        storage.getObjectMetadata(null, "objectId", true);
    }

    @Test
    public void listTest() throws Exception {
        int nbIter = 1;
        String containerName = "object_0";
        storage.createContainer(containerName);
        assertNotNull(storage.getContainerInformation(containerName));
        for (int i = 0; i < 150; i++) {
            storage.putObject(containerName, "object_" + i, new FakeInputStream(100), DigestType.SHA512, 100L);
        }
        ObjectListingListener objectListingListener = mock(ObjectListingListener.class);

        storage.listContainer(containerName, objectListingListener);

        ArgumentCaptor<ObjectEntry> objectEntryArgumentCaptor = ArgumentCaptor.forClass(ObjectEntry.class);
        verify(objectListingListener, times(nbIter * 100 + 50)).handleObjectEntry(objectEntryArgumentCaptor.capture());

        objectEntryArgumentCaptor
            .getAllValues()
            .forEach(capturedObjectEntry -> assertThat(capturedObjectEntry.getSize()).isEqualTo(100L));

        Set<String> capturedFileNames = objectEntryArgumentCaptor
            .getAllValues()
            .stream()
            .map(ObjectEntry::getObjectId)
            .collect(Collectors.toSet());
        Set<String> expectedFileNames = IntStream.range(0, nbIter * 100 + 50)
            .mapToObj(i -> "object_" + i)
            .collect(Collectors.toSet());
        assertThat(capturedFileNames).isEqualTo(expectedFileNames);
    }
}
