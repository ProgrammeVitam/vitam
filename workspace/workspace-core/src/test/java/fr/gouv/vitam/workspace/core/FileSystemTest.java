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
package fr.gouv.vitam.workspace.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fr.gouv.vitam.workspace.api.config.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.common.ParametersChecker;

public class FileSystemTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ContentAddressableStorageImpl workspace;
    private File tempDir;
    private static final String CONTAINER_NAME = "myContainer";
    private static final String FOLDER_NAME = "myFolder";
    private static final String OBJECT_NAME = "myObject";
    private static final String SLASH = "/";
    private static final String SIP_CONTAINER = "sipContainer";
    private static final String SIP_FOLDER = "SIP";
    private static final String CONTENT_FOLDER = "Content";
    private static final String MANIFEST_NAME = "sip/manifest.xml";

    @Before
    public void setup() throws IOException {
        StorageConfiguration configuration = new StorageConfiguration();
        tempDir = tempFolder.newFolder();
        configuration.setStoragePath(tempDir.getCanonicalPath());
        workspace = new FileSystem(configuration);
    }

    // Container
    @Test
    public void givenContainerNotFoundWhenCheckContainerExistenceThenRetunFalse() {
        assertFalse(workspace.containerExists(CONTAINER_NAME));
    }

    @Test
    public void givenContainerAlreadyExistsWhenCheckContainerExistenceThenRetunFalse()
        throws ContentAddressableStorageAlreadyExistException {
        workspace.createContainer(CONTAINER_NAME);
        assertTrue(workspace.containerExists(CONTAINER_NAME));
    }

    @Test(expected = ContentAddressableStorageException.class)
    public void givenContainerAlreadyExistsWhenCreateContainerThenRaiseAnException()
        throws ContentAddressableStorageAlreadyExistException {
        workspace.createContainer(CONTAINER_NAME);

        workspace.createContainer(CONTAINER_NAME);
        assertFalse(workspace.containerExists(CONTAINER_NAME));
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenPurgeContainerThenRaiseAnException()
        throws ContentAddressableStorageNotFoundException {
        workspace.purgeContainer(CONTAINER_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenDeleteContainerThenRaiseAnException()
        throws ContentAddressableStorageNotFoundException {
        workspace.deleteContainer(CONTAINER_NAME);
    }

    @Test
    public void givenContainerNotFoundWhenCreateContainerThenOK()
        throws ContentAddressableStorageAlreadyExistException {
        workspace.createContainer(CONTAINER_NAME);
        assertTrue(workspace.containerExists(CONTAINER_NAME));
    }

    @Test
    public void givenContainerAlreadyExistsWhenDeleteContainerThenOK()
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageNotFoundException {
        workspace.createContainer(CONTAINER_NAME);

        workspace.deleteContainer(CONTAINER_NAME);
        assertFalse(workspace.containerExists(CONTAINER_NAME));
    }

    @Test
    public void givenContainerAlreadyExistsAndEmptyWhenPurgeContainerThenOK()
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageNotFoundException {
        workspace.createContainer(CONTAINER_NAME);

        workspace.purgeContainer(CONTAINER_NAME);
        assertTrue(workspace.containerExists(CONTAINER_NAME));
    }

    @Test
    public void givenContainerAlreadyExistsAndNotEmptyWhenPurgeContainerThenOK()
        throws IOException, ContentAddressableStorageException {
        workspace.createContainer(CONTAINER_NAME);
        workspace.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));

        workspace.purgeContainer(CONTAINER_NAME);
        assertTrue(workspace.containerExists(CONTAINER_NAME));
        assertFalse(workspace.objectExists(CONTAINER_NAME, OBJECT_NAME));
    }

    // Folder

    @Test
    public void givenFolderNotFoundWhenCheckContainerExistenceThenRetunFalse() {
        assertFalse(workspace.folderExists(CONTAINER_NAME, FOLDER_NAME));
    }

    @Test
    public void givenFolderAlreadyExistsWhenCheckContainerExistenceThenRetunTrue()
        throws ContentAddressableStorageException {
        workspace.createContainer(CONTAINER_NAME);
        workspace.createFolder(CONTAINER_NAME, FOLDER_NAME);

        assertTrue(workspace.folderExists(CONTAINER_NAME, FOLDER_NAME));
    }

    @Test(expected = ContentAddressableStorageException.class)
    public void givenFolderAlreadyExistsWhenCreateFolderThenRaiseAnException()
        throws ContentAddressableStorageException {
        workspace.createContainer(CONTAINER_NAME);
        workspace.createFolder(CONTAINER_NAME, FOLDER_NAME);

        workspace.createFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    @Test(expected = ContentAddressableStorageException.class)
    public void givenContainerNotFoundWhenCreateFolderThenRaiseAnException()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageAlreadyExistException {
        workspace.createFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenFolderNotFoundWhenDeleteFolderThenRaiseAnException() throws ContentAddressableStorageException {
        workspace.createContainer(CONTAINER_NAME);
        workspace.createFolder(CONTAINER_NAME, FOLDER_NAME);
        workspace.deleteFolder(CONTAINER_NAME, FOLDER_NAME);

        workspace.deleteFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenDeleteFolderThenRaiseAnException()
        throws ContentAddressableStorageNotFoundException {
        workspace.deleteFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    @Test
    public void givenFolderNotFoundWhenCreateFolderThenOK() throws ContentAddressableStorageException {
        workspace.createContainer(CONTAINER_NAME);

        workspace.createFolder(CONTAINER_NAME, FOLDER_NAME);
        assertTrue(workspace.folderExists(CONTAINER_NAME, FOLDER_NAME));
    }

    @Test
    public void givenFolderAlreadyExistsWhenDeleteFolderThenOK()
        throws ContentAddressableStorageException, ContentAddressableStorageAlreadyExistException {
        workspace.createContainer(CONTAINER_NAME);
        workspace.createFolder(CONTAINER_NAME, FOLDER_NAME);

        workspace.deleteFolder(CONTAINER_NAME, FOLDER_NAME);
        assertFalse(workspace.folderExists(CONTAINER_NAME, FOLDER_NAME));
    }

    // Object

    @Test
    public void givenObjectNotFoundWhenCheckObjectExistenceThenRetunFalse() {
        assertFalse(workspace.objectExists(CONTAINER_NAME, OBJECT_NAME));
    }

    @Test
    public void givenObjectAlreadyExistsWhenCheckObjectExistenceThenRetunFalse()
        throws IOException, ContentAddressableStorageException {
        workspace.createContainer(CONTAINER_NAME);
        workspace.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));

        assertTrue(workspace.objectExists(CONTAINER_NAME, OBJECT_NAME));
    }

    @Test
    public void givenObjectAlreadyExistsWhenPutObjectThenNotRaiseAnException()
        throws IOException, ContentAddressableStorageException {
        workspace.createContainer(CONTAINER_NAME);
        workspace.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));

        workspace.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file2.pdf"));
        assertEquals(getInputStream("file2.pdf").available(),
            workspace.getObject(CONTAINER_NAME, OBJECT_NAME).available());

    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenDeleteObjectThenRaiseAnException()
        throws ContentAddressableStorageNotFoundException {
        workspace.deleteObject(CONTAINER_NAME, OBJECT_NAME);

    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenObjectNotFoundWhenDeleteObjectThenRaiseAnException()
        throws IOException, ContentAddressableStorageException {
        workspace.createContainer(CONTAINER_NAME);
        workspace.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));
        workspace.deleteObject(CONTAINER_NAME, OBJECT_NAME);

        workspace.deleteObject(CONTAINER_NAME, OBJECT_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenGetObjectThenRaiseAnException()
        throws IOException, ContentAddressableStorageException {
        workspace.createContainer(CONTAINER_NAME);
        workspace.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));
        workspace.deleteContainer(CONTAINER_NAME, true);

        workspace.getObject(CONTAINER_NAME, OBJECT_NAME);

    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenPutObjectThenRaiseAnException()
        throws IOException, ContentAddressableStorageException {
        workspace.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));
    }

    @Test
    public void givenObjectNotFoundWhenPutObjectThenOK() throws IOException, Exception {
        workspace.createContainer(CONTAINER_NAME);

        workspace.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));
        assertEquals(getInputStream("file1.pdf").available(),
            workspace.getObject(CONTAINER_NAME, OBJECT_NAME).available());
    }

    @Test
    public void givenObjectAlreadyExistsWhenDeleteObjectThenOK() throws IOException, Exception {
        workspace.createContainer(CONTAINER_NAME);
        workspace.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));

        workspace.deleteObject(CONTAINER_NAME, OBJECT_NAME);
        assertFalse(workspace.objectExists(CONTAINER_NAME, OBJECT_NAME));
    }

    // Check Path parameters (containerName,folder, objectName)
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenCreateContainerThenRaiseAnException() {
        ParametersChecker.checkParamater("Null Param", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenCreateContainerThenRaiseAnException() {
        ParametersChecker.checkParamater("Empty Param", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenCreateFolderThenRaiseAnException() {
        ParametersChecker.checkParamater("Null Param", CONTAINER_NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenCreateFolderOThenRaiseAnException() {
        ParametersChecker.checkParamater("Empty Param", CONTAINER_NAME, "");
    }

    private InputStream getInputStream(String file) throws IOException {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
    }

    // Uri List of Digital Object from Content folder

    @Test
    public void givenContainerAlreadyExistAndFolderAlreadyExistAndSubFolderAlreadyExistWhenCheckListUriNotEmptyThenReturnTrue()
        throws IOException, Exception {

        // Given container exists
        workspace.createContainer(SIP_CONTAINER);

        // Given a root folder "SIP_FOLDER", add manifest.xml to this root folder
        String manifestName = new StringBuilder().append(SIP_FOLDER).append(SLASH).append("manifest.xml").toString();
        workspace.putObject(SIP_CONTAINER, manifestName, getInputStream("manifest.xml"));

        // Given a sub folder "CONTENT_FOLDER" add digital objects
        String contentSubFolder =
            new StringBuilder().append(SIP_FOLDER).append(SLASH).append(CONTENT_FOLDER).toString();
        // workspace.createFolder(SIP_CONTAINER, contentSubFolder);
        String fileName1 = new StringBuilder().append(contentSubFolder).append(SLASH).append("file1.pdf").toString();
        workspace.putObject(SIP_CONTAINER, fileName1, getInputStream("file1.pdf"));
        String fileName2 = new StringBuilder().append(contentSubFolder).append(SLASH).append("file2.pdf").toString();
        workspace.putObject(SIP_CONTAINER, fileName2, getInputStream("file2.pdf"));

        // Then check that there is 2 URIs found recursively from the content folder
        assertThat(workspace.getListUriDigitalObjectFromFolder(SIP_CONTAINER, contentSubFolder)).isNotNull()
            .isNotEmpty();
        assertThat(workspace.getListUriDigitalObjectFromFolder(SIP_CONTAINER, contentSubFolder)).hasSize(2);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotExistWhenCheckListUriNotEmptyThenRaiseAnException() throws IOException, Exception {
        // Then check that there is 3 URIs found recursively from the root folder
        assertThat(workspace.getListUriDigitalObjectFromFolder(SIP_CONTAINER, SIP_FOLDER)).isNotNull().isNotEmpty();
        assertThat(workspace.getListUriDigitalObjectFromFolder(SIP_CONTAINER, SIP_FOLDER)).hasSize(3);
    }


    @Test
    public void given_ZipInputStream_when_Finish_Then_ObjectExistsOnContainer() throws IOException, Exception {
        workspace.unzipSipObject(CONTAINER_NAME, getInputStream("sip.zip"));
        assertTrue(workspace.objectExists(CONTAINER_NAME, MANIFEST_NAME));
    }

    @Test(expected = ContentAddressableStorageAlreadyExistException.class)
    public void givenZipInputStream_containerName_exists_when_Extracting_Then_RaiseAnException()
        throws IOException, Exception {
        workspace.createContainer(CONTAINER_NAME);
        workspace.unzipSipObject(CONTAINER_NAME, getInputStream("sip.zip"));
        assertTrue(workspace.objectExists(CONTAINER_NAME, OBJECT_NAME));
    }

    @Test(expected = ContentAddressableStorageException.class)
    public void given_EmptyParam_When_UnzipSip_Then_RaiseAnException() throws Exception {
        workspace.unzipSipObject(CONTAINER_NAME, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void given_EmptyContainerNameParam_When_UnzipSip_Then_RaiseAnException() throws Exception {
        workspace.unzipSipObject(null, null);
    }

}
