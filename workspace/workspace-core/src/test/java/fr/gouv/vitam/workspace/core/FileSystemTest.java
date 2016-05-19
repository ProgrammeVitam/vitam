package fr.gouv.vitam.workspace.core;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fr.gouv.vitam.workspace.common.ParametersChecker;
import fr.gouv.vitam.workspace.api.config.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.core.ContentAddressableStorageImpl;
import fr.gouv.vitam.workspace.core.FileSystem;
// TODO REVIEW Add licence header
public class FileSystemTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ContentAddressableStorageImpl workspace;
    private File tempDir;
    // TODO REVIEW Add variable for file.pdf
    private static final String CONTAINER_NAME = "myContainer";
    private static final String FOLDER_NAME = "myFolder";
    private static final String OBJECT_NAME = "myObject";

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
    public void givenContainerAlreadyExistsWhenCheckContainerExistenceThenRetunFalse() {
        workspace.createContainer(CONTAINER_NAME);
        assertTrue(workspace.containerExists(CONTAINER_NAME));
    }

    @Test(expected = ContentAddressableStorageException.class)
    public void givenContainerAlreadyExistsWhenCreateContainerThenRaiseAnException() {
        workspace.createContainer(CONTAINER_NAME);

        workspace.createContainer(CONTAINER_NAME);
        assertFalse(workspace.containerExists(CONTAINER_NAME));
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenPurgeContainerThenRaiseAnException() {
        workspace.purgeContainer(CONTAINER_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenDeleteContainerThenRaiseAnException() {
        workspace.deleteContainer(CONTAINER_NAME);
    }

    @Test
    public void givenContainerNotFoundWhenCreateContainerThenOK() {
        workspace.createContainer(CONTAINER_NAME);
        assertTrue(workspace.containerExists(CONTAINER_NAME));
    }

    @Test
    public void givenContainerAlreadyExistsWhenDeleteContainerThenOK() {
        workspace.createContainer(CONTAINER_NAME);

        workspace.deleteContainer(CONTAINER_NAME);
        assertFalse(workspace.containerExists(CONTAINER_NAME));
    }

    @Test
    public void givenContainerAlreadyExistsAndEmptyWhenPurgeContainerThenOK() {
        workspace.createContainer(CONTAINER_NAME);

        workspace.purgeContainer(CONTAINER_NAME);
        assertTrue(workspace.containerExists(CONTAINER_NAME));
    }

    @Test
    public void givenContainerAlreadyExistsAndNotEmptyWhenPurgeContainerThenOK() throws IOException {
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
    public void givenFolderAlreadyExistsWhenCheckContainerExistenceThenRetunTrue() {
        workspace.createContainer(CONTAINER_NAME);
        workspace.createFolder(CONTAINER_NAME, FOLDER_NAME);

        assertTrue(workspace.folderExists(CONTAINER_NAME, FOLDER_NAME));
    }

    @Test(expected = ContentAddressableStorageException.class)
    public void givenFolderAlreadyExistsWhenCreateFolderThenRaiseAnException() {
        workspace.createContainer(CONTAINER_NAME);
        workspace.createFolder(CONTAINER_NAME, FOLDER_NAME);

        workspace.createFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    @Test(expected = ContentAddressableStorageException.class)
    public void givenContainerNotFoundWhenCreateFolderThenRaiseAnException() {
        workspace.createFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenFolderNotFoundWhenDeleteFolderThenRaiseAnException() {
        workspace.createContainer(CONTAINER_NAME);
        workspace.createFolder(CONTAINER_NAME, FOLDER_NAME);
        workspace.deleteFolder(CONTAINER_NAME, FOLDER_NAME);

        workspace.deleteFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenDeleteFolderThenRaiseAnException() {
        workspace.deleteFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    @Test
    public void givenFolderNotFoundWhenCreateFolderThenOK() {
        workspace.createContainer(CONTAINER_NAME);

        workspace.createFolder(CONTAINER_NAME, FOLDER_NAME);
        assertTrue(workspace.folderExists(CONTAINER_NAME, FOLDER_NAME));
    }

    @Test
    public void givenFolderAlreadyExistsWhenDeleteFolderThenOK() {
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
    public void givenObjectAlreadyExistsWhenCheckObjectExistenceThenRetunFalse() throws IOException {
        workspace.createContainer(CONTAINER_NAME);
        workspace.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));

        assertTrue(workspace.objectExists(CONTAINER_NAME, OBJECT_NAME));
    }

    @Test
    public void givenObjectAlreadyExistsWhenPutObjectThenNotRaiseAnException() throws IOException {
        workspace.createContainer(CONTAINER_NAME);
        workspace.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));

        workspace.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file2.pdf"));
        assertEquals(getInputStream("file2.pdf").available(),
                workspace.getObject(CONTAINER_NAME, OBJECT_NAME).available());

    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenDeleteObjectThenRaiseAnException() {
        workspace.deleteObject(CONTAINER_NAME, OBJECT_NAME);

    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenObjectNotFoundWhenDeleteObjectThenRaiseAnException() throws IOException {
        workspace.createContainer(CONTAINER_NAME);
        workspace.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));
        workspace.deleteObject(CONTAINER_NAME, OBJECT_NAME);

        workspace.deleteObject(CONTAINER_NAME, OBJECT_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenGetObjectThenRaiseAnException() throws IOException {
        workspace.createContainer(CONTAINER_NAME);
        workspace.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));
        workspace.deleteContainer(CONTAINER_NAME, true);

        workspace.getObject(CONTAINER_NAME, OBJECT_NAME);

    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenPutObjectThenRaiseAnException() throws IOException {
        workspace.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));
    }

    @Test
    public void givenObjectNotFoundWhenGetObjectThenReturnEmptyStream() throws IOException {
        workspace.createContainer(CONTAINER_NAME);

        assertEquals(0, workspace.getObject(CONTAINER_NAME, OBJECT_NAME).available());
    }

    @Test
    public void givenObjectNotFoundWhenPutObjectThenOK() throws IOException {
        workspace.createContainer(CONTAINER_NAME);

        workspace.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));
        assertEquals(getInputStream("file1.pdf").available(),
                workspace.getObject(CONTAINER_NAME, OBJECT_NAME).available());
    }

    @Test
    public void givenObjectAlreadyExistsWhenDeleteObjectThenOK() throws IOException {
        workspace.createContainer(CONTAINER_NAME);
        workspace.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));

        workspace.deleteObject(CONTAINER_NAME, OBJECT_NAME);
        assertEquals(0, workspace.getObject(CONTAINER_NAME, OBJECT_NAME).available());
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
}
