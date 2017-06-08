/**
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
 *  In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */

package fr.gouv.vitam.workspace.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageCompressedFileException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;

public class WorkspaceFileSystemTest {

    private static final String CONTAINER_NAME = "myContainer";
    private static final String OBJECT_NAME = "myObject";
    private static final String FOLDER_NAME = "myFolder";
    private static final String SIP_CONTAINER = "sipContainer";
    private static final String SIP_FOLDER = "SIP";
    private static final String SLASH = "/";
    private static final String MANIFEST = "manifest.xml";
    private static final String CONTENT_FOLDER = "Content";
    private static final String SIP_TAR_GZ = "sip.tar.gz";
    private static final String SIP_TAR = "sip.tar";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private WorkspaceFileSystem storage;
    private File tempDir;

    @Before
    public void setup() throws IOException {
        final StorageConfiguration configuration = new StorageConfiguration();
        tempDir = tempFolder.newFolder();
        configuration.setStoragePath(tempDir.getCanonicalPath());
        storage = new WorkspaceFileSystem(configuration);
    }

    private InputStream getInputStream(String file) throws IOException {
        return PropertiesUtils.getResourceAsStream(file);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenPurgeContainerThenRaiseAnException() throws Exception {
        storage.purgeContainer(CONTAINER_NAME);
    }


    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenDeleteContainerThenRaiseAnException() throws Exception {
        storage.deleteContainer(CONTAINER_NAME, false);
    }

    @Test
    public void givenContainerAlreadyExistsWhenDeleteContainerThenOK() throws Exception {
        storage.createContainer(CONTAINER_NAME);

        storage.deleteContainer(CONTAINER_NAME, true);
        Assert.assertFalse(storage.isExistingContainer(CONTAINER_NAME));
    }

    @Test
    public void givenContainerAlreadyExistsAndEmptyWhenPurgeContainerThenOK() throws Exception {
        storage.createContainer(CONTAINER_NAME);

        storage.purgeContainer(CONTAINER_NAME);
        Assert.assertTrue(storage.isExistingContainer(CONTAINER_NAME));
    }

    @Test
    public void givenContainerAlreadyExistsAndNotEmptyWhenPurgeContainerThenOK() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));

        storage.purgeContainer(CONTAINER_NAME);
        Assert.assertTrue(storage.isExistingContainer(CONTAINER_NAME));
        Assert.assertFalse(storage.isExistingObject(CONTAINER_NAME, OBJECT_NAME));
    }

    // Folder
    @Test
    public void givenFolderNotFoundWhenCheckContainerExistenceThenRetunFalse() {
        Assert.assertFalse(storage.isExistingFolder(CONTAINER_NAME, FOLDER_NAME));
    }

    @Test
    public void givenFolderAlreadyExistsWhenCheckContainerExistenceThenRetunTrue()
        throws ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.createFolder(CONTAINER_NAME, FOLDER_NAME);

        Assert.assertTrue(storage.isExistingFolder(CONTAINER_NAME, FOLDER_NAME));
    }

    @Test(expected = ContentAddressableStorageException.class)
    public void givenFolderAlreadyExistsWhenCreateFolderThenRaiseAnException()
        throws ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.createFolder(CONTAINER_NAME, FOLDER_NAME);

        storage.createFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    @Test(expected = ContentAddressableStorageException.class)
    public void givenContainerNotFoundWhenCreateFolderThenRaiseAnException() throws Exception {
        storage.createFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenFolderNotFoundWhenDeleteFolderThenRaiseAnException() throws ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.createFolder(CONTAINER_NAME, FOLDER_NAME);
        storage.deleteFolder(CONTAINER_NAME, FOLDER_NAME);

        storage.deleteFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenDeleteFolderThenRaiseAnException() throws Exception {
        storage.deleteFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    @Test
    public void givenFolderNotFoundWhenCreateFolderThenOK() throws ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);

        storage.createFolder(CONTAINER_NAME, FOLDER_NAME);
        Assert.assertTrue(storage.isExistingFolder(CONTAINER_NAME, FOLDER_NAME));
    }

    @Test
    public void givenFolderAlreadyExistsWhenDeleteFolderThenOK()
        throws ContentAddressableStorageException, ContentAddressableStorageAlreadyExistException {
        storage.createContainer(CONTAINER_NAME);
        storage.createFolder(CONTAINER_NAME, FOLDER_NAME);

        storage.deleteFolder(CONTAINER_NAME, FOLDER_NAME);
        Assert.assertFalse(storage.isExistingFolder(CONTAINER_NAME, FOLDER_NAME));
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenGetObjectThenRaiseAnException()
        throws IOException, ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));
        storage.deleteContainer(CONTAINER_NAME, true);

        storage.getObject(CONTAINER_NAME, OBJECT_NAME);

    }

    // Uri List of Digital Object from Content folder

    @Test
    public void givenContainerAlreadyExistAndFolderAlreadyExistAndSubFolderAlreadyExistWhenCheckListUriNotEmptyThenReturnTrue()
        throws IOException, Exception {

        // Given container exists
        storage.createContainer(SIP_CONTAINER);

        // Given a root folder "SIP_FOLDER", add manifest.xml to this root folder
        final String manifestName =
            new StringBuilder().append(SIP_FOLDER).append(SLASH).append(MANIFEST).toString();
        storage.putObject(SIP_CONTAINER, manifestName, getInputStream(MANIFEST));

        // Given a sub folder "CONTENT_FOLDER" add digital objects
        final String contentSubFolder =
            new StringBuilder().append(SIP_FOLDER).append(SLASH).append(CONTENT_FOLDER).toString();
        // workspace.createFolder(SIP_CONTAINER, contentSubFolder);
        final String fileName1 =
            new StringBuilder().append(contentSubFolder).append(SLASH).append("file1.pdf").toString();
        storage.putObject(SIP_CONTAINER, fileName1, getInputStream("file1.pdf"));
        final String fileName2 =
            new StringBuilder().append(contentSubFolder).append(SLASH).append("space - file2.pdf").toString();
        storage.putObject(SIP_CONTAINER, fileName2, getInputStream("file2.pdf"));

        // Then check that there is 2 URIs found recursively from the content folder
        assertThat(storage.getListUriDigitalObjectFromFolder(SIP_CONTAINER, contentSubFolder)).isNotNull()
            .isNotEmpty();
        assertThat(storage.getListUriDigitalObjectFromFolder(SIP_CONTAINER, contentSubFolder)).hasSize(2);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotExistWhenCheckListUriNotEmptyThenRaiseAnException() throws IOException, Exception {
        // Then check that there is 3 URIs found recursively from the root folder
        assertThat(storage.getListUriDigitalObjectFromFolder(SIP_CONTAINER, SIP_FOLDER)).isNotNull().isNotEmpty();
        assertThat(storage.getListUriDigitalObjectFromFolder(SIP_CONTAINER, SIP_FOLDER)).hasSize(3);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenUnzipObjectThenRaiseAnException()
        throws IOException, Exception {
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, ArchiveStreamFactory.ZIP, getInputStream("sip.zip"));
    }

    @Test(expected = ContentAddressableStorageAlreadyExistException.class)
    public void givenFolderAlreadyExisitsWhenUnzipObjectThenRaiseAnException()
        throws IOException, Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.createFolder(CONTAINER_NAME, SIP_FOLDER);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, ArchiveStreamFactory.ZIP, getInputStream("sip.zip"));
    }

    @Test(expected = ContentAddressableStorageException.class)
    public void givenNullInputStreamWhenUnzipObjectThenRaiseAnException()
        throws IOException, Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, ArchiveStreamFactory.ZIP, null);
    }

    @Test
    public void givenContainerAlreadyExisitsWhenUnzipObjectThenOk()
        throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP, getInputStream("sip.zip"));

    }

    @Test(expected = ContentAddressableStorageCompressedFileException.class)
    public void givenContainerAlreadyExisitsWhenUnzipObjectEmptyThenZipException()
        throws IOException, Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP,
            getInputStream("empty_zip.zip"));
    }

    @Test(expected = ContentAddressableStorageCompressedFileException.class)
    public void givenContainerAlreadyExisitsWhenUnzipObjectNotZipThenZipException()
        throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP,
            getInputStream("SIP_mauvais_format.pdf"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyContainerNameParamWhenUnzipSipThenRaiseAnException() throws Exception {
        storage.uncompressObject(null, null, null, null);
    }

    @Test(expected = ContentAddressableStorageCompressedFileException.class)
    public void givenTarGzSIPAndBadArchiveTypeWhenUncompressObjectThenRaiseAnException()
        throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP, getInputStream(SIP_TAR_GZ));
    }

    @Test
    public void givenTarGzSIPAndArchiveTypeWhenUncompressObjectThenExtractOK()
        throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.GZIP, getInputStream(SIP_TAR_GZ));
        Assert.assertTrue(storage.isExistingObject(CONTAINER_NAME, SIP_FOLDER + File.separator + MANIFEST));
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenTarGzSIPAndUnsupportedArchiveTypeWhenUncompressObjectThenRaiseException()
        throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, "unsupported", getInputStream(SIP_TAR_GZ));
    }

    @Test
    public void givenTarGzSIPArchiveTypeWhenUncompressObjectAndSearchManifestThenReturnExist()
        throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.GZIP, getInputStream(SIP_TAR_GZ));
        Assert.assertTrue(storage.isExistingContainer(CONTAINER_NAME));
        Assert.assertTrue(storage.isExistingFolder(CONTAINER_NAME, SIP_FOLDER));
    }

    @Test
    public void givenTarArchiveTypeWhenUncompressObjectAndSearchManifestThenReturnExist()
        throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.TAR, getInputStream(SIP_TAR));
        Assert.assertTrue(storage.isExistingContainer(CONTAINER_NAME));
        Assert.assertTrue(storage.isExistingFolder(CONTAINER_NAME, SIP_FOLDER));
    }

    @Test
    public void countObjectsOK() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.TAR, getInputStream(SIP_TAR));
        long number = storage.countObjects(CONTAINER_NAME);
        Assert.assertNotNull(number);
        Assert.assertEquals(7, number);
    }

}
