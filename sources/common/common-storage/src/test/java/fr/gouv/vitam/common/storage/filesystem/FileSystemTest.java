package fr.gouv.vitam.common.storage.filesystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.storage.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageCompressedFileException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.model.ContainerInformation;

public class FileSystemTest {


    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ContentAddressableStorageAbstract storage;
    private File tempDir;
    private static final String CONTAINER_NAME = "myContainer";
    private static final String WRONG_CONTAINER_NAME = "myWrongContainer";
    private static final String FOLDER_NAME = "myFolder";
    private static final String OBJECT_NAME = "myObject";
    private static final String SLASH = "/";
    private static final String SIP_CONTAINER = "sipContainer";
    private static final String SIP_FOLDER = "SIP";
    private static final String CONTENT_FOLDER = "Content";
    private static final DigestType ALGO = DigestType.MD5;
    private static final String MANIFEST = "manifest.xml";
    private static final String SIP_TAR_GZ = "sip.tar.gz";
    private static final String SIP_TAR = "sip.tar";

    private static final String TENANT_ID = "0";
    private static final String TYPE = "object";
    private static final String OBJECT_ID = "aeaaaaaaaaaam7mxaa2pkak2bnhxy5aaaaaq";
    private static final String OBJECT_ID2 = "aeaaaaaaaaaam7mxaa2pkak2bnhxy4aaaaaq";


    @Before
    public void setup() throws IOException {
        final StorageConfiguration configuration = new StorageConfiguration();
        tempDir = tempFolder.newFolder();
        configuration.setStoragePath(tempDir.getCanonicalPath());
        storage = new FileSystem(configuration);
    }

    // Container
    @Test
    public void givenContainerNotFoundWhenCheckContainerExistenceThenRetunFalse() {
        assertFalse(storage.isExistingContainer(CONTAINER_NAME));
    }

    @Test
    public void givenContainerAlreadyExistsWhenCheckContainerExistenceThenRetunFalse()
        throws ContentAddressableStorageAlreadyExistException {
        storage.createContainer(CONTAINER_NAME);
        assertTrue(storage.isExistingContainer(CONTAINER_NAME));
    }

    @Test(expected = ContentAddressableStorageException.class)
    public void givenContainerAlreadyExistsWhenCreateContainerThenRaiseAnException()
        throws ContentAddressableStorageAlreadyExistException {
        storage.createContainer(CONTAINER_NAME);
        storage.createContainer(CONTAINER_NAME);
        assertFalse(storage.isExistingContainer(CONTAINER_NAME));
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenPurgeContainerThenRaiseAnException()
        throws ContentAddressableStorageNotFoundException {
        storage.purgeContainer(CONTAINER_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenDeleteContainerThenRaiseAnException()
        throws ContentAddressableStorageNotFoundException {
        storage.deleteContainer(CONTAINER_NAME, false);
    }

    @Test
    public void givenContainerNotFoundWhenCreateContainerThenOK()
        throws ContentAddressableStorageAlreadyExistException {
        storage.createContainer(CONTAINER_NAME);
        assertTrue(storage.isExistingContainer(CONTAINER_NAME));
    }

    @Test
    public void givenContainerAlreadyExistsWhenDeleteContainerThenOK()
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageNotFoundException {
        storage.createContainer(CONTAINER_NAME);

        storage.deleteContainer(CONTAINER_NAME, true);
        assertFalse(storage.isExistingContainer(CONTAINER_NAME));
    }

    @Test
    public void givenContainerAlreadyExistsAndEmptyWhenPurgeContainerThenOK()
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageNotFoundException {
        storage.createContainer(CONTAINER_NAME);

        storage.purgeContainer(CONTAINER_NAME);
        assertTrue(storage.isExistingContainer(CONTAINER_NAME));
    }

    @Test
    public void givenContainerAlreadyExistsAndNotEmptyWhenPurgeContainerThenOK()
        throws IOException, ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));

        storage.purgeContainer(CONTAINER_NAME);
        assertTrue(storage.isExistingContainer(CONTAINER_NAME));
        assertFalse(storage.isExistingObject(CONTAINER_NAME, OBJECT_NAME));
    }

    // Folder
    @Test
    public void givenFolderNotFoundWhenCheckContainerExistenceThenRetunFalse() {
        assertFalse(storage.isExistingFolder(CONTAINER_NAME, FOLDER_NAME));
    }

    @Test
    public void givenFolderAlreadyExistsWhenCheckContainerExistenceThenRetunTrue()
        throws ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.createFolder(CONTAINER_NAME, FOLDER_NAME);

        assertTrue(storage.isExistingFolder(CONTAINER_NAME, FOLDER_NAME));
    }

    @Test(expected = ContentAddressableStorageException.class)
    public void givenFolderAlreadyExistsWhenCreateFolderThenRaiseAnException()
        throws ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.createFolder(CONTAINER_NAME, FOLDER_NAME);

        storage.createFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    @Test(expected = ContentAddressableStorageException.class)
    public void givenContainerNotFoundWhenCreateFolderThenRaiseAnException()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageAlreadyExistException {
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
    public void givenContainerNotFoundWhenDeleteFolderThenRaiseAnException()
        throws ContentAddressableStorageNotFoundException {
        storage.deleteFolder(CONTAINER_NAME, FOLDER_NAME);
    }

    @Test
    public void givenFolderNotFoundWhenCreateFolderThenOK() throws ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);

        storage.createFolder(CONTAINER_NAME, FOLDER_NAME);
        assertTrue(storage.isExistingFolder(CONTAINER_NAME, FOLDER_NAME));
    }

    @Test
    public void givenFolderAlreadyExistsWhenDeleteFolderThenOK()
        throws ContentAddressableStorageException, ContentAddressableStorageAlreadyExistException {
        storage.createContainer(CONTAINER_NAME);
        storage.createFolder(CONTAINER_NAME, FOLDER_NAME);

        storage.deleteFolder(CONTAINER_NAME, FOLDER_NAME);
        assertFalse(storage.isExistingFolder(CONTAINER_NAME, FOLDER_NAME));
    }

    // Object
    @Test
    public void givenObjectNotFoundWhenCheckObjectExistenceThenRetunFalse() {
        assertFalse(storage.isExistingObject(CONTAINER_NAME, OBJECT_NAME));
    }

    @Test
    public void givenObjectAlreadyExistsWhenCheckObjectExistenceThenRetunFalse()
        throws IOException, ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));

        assertTrue(storage.isExistingObject(CONTAINER_NAME, OBJECT_NAME));
    }

    @Test
    public void givenObjectAlreadyExistsWhenPutObjectThenNotRaiseAnException()
        throws IOException, ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));

        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file2.pdf"));
        assertEquals(getInputStream("file2.pdf").available(),
            ((InputStream) storage.getObject(CONTAINER_NAME, OBJECT_NAME).getEntity()).available());

    }

    @Test
    public void givenObjectAlreadyExistsWhenGetObjectInformationThenNotRaiseAnException()
        throws IOException, ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));
        final JsonNode jsonNode = storage.getObjectInformation(CONTAINER_NAME, OBJECT_NAME);
        assertNotNull(jsonNode);
        assertNotNull(jsonNode.get("size"));
        assertNotNull(jsonNode.get("object_name"));
        assertNotNull(jsonNode.get("container_name"));
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenGetObjectInformationThenRaiseAnException()
        throws IOException, ContentAddressableStorageException {
        assertNotNull(storage.getObjectInformation("FAKE" + CONTAINER_NAME, OBJECT_NAME));

    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenObjectNotFoundWhenGetObjectInformationThenRaiseAnException()
        throws IOException, ContentAddressableStorageException {
        assertNotNull(storage.getObjectInformation(CONTAINER_NAME, OBJECT_NAME));

    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenDeleteObjectThenRaiseAnException()
        throws ContentAddressableStorageNotFoundException {
        storage.deleteObject(CONTAINER_NAME, OBJECT_NAME);

    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenObjectNotFoundWhenDeleteObjectThenRaiseAnException()
        throws IOException, ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));
        storage.deleteObject(CONTAINER_NAME, OBJECT_NAME);

        storage.deleteObject(CONTAINER_NAME, OBJECT_NAME);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenGetObjectThenRaiseAnException()
        throws IOException, ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));
        storage.deleteContainer(CONTAINER_NAME, true);

        storage.getObject(CONTAINER_NAME, OBJECT_NAME);

    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenPutObjectThenRaiseAnException()
        throws IOException, ContentAddressableStorageException {
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));
    }

    @Test
    public void givenObjectNotFoundWhenPutObjectThenOK() throws IOException, Exception {
        storage.createContainer(CONTAINER_NAME);

        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));
        assertEquals(getInputStream("file1.pdf").available(),
            ((InputStream) storage.getObject(CONTAINER_NAME, OBJECT_NAME).getEntity()).available());
    }

    @Test
    public void givenObjectAlreadyExistsWhenDeleteObjectThenOK() throws IOException, Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));

        storage.deleteObject(CONTAINER_NAME, OBJECT_NAME);
        assertFalse(storage.isExistingObject(CONTAINER_NAME, OBJECT_NAME));
    }


    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenComputeObjectDigestThenRaiseAnException()
        throws ContentAddressableStorageException {
        storage.computeObjectDigest(CONTAINER_NAME, OBJECT_NAME, ALGO);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenObjectNotFoundWhenComputeObjectDigestThenRaiseAnException()
        throws ContentAddressableStorageException {
        storage.createContainer(CONTAINER_NAME);

        storage.computeObjectDigest(CONTAINER_NAME, OBJECT_NAME, ALGO);
    }

    @Test
    public void givenObjectAlreadyExistsWhenWhenComputeObjectDigestThenOK()
        throws ContentAddressableStorageException, IOException {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));

        final String messageDigest = storage.computeObjectDigest(CONTAINER_NAME, OBJECT_NAME, ALGO);
        final Digest digest = new Digest(ALGO);
        digest.update(getInputStream("file1.pdf"));

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
            new StringBuilder().append(contentSubFolder).append(SLASH).append("file2.pdf").toString();
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
        throws IOException, Exception {
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
        throws IOException, Exception {
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
        throws IOException, Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP, getInputStream(SIP_TAR_GZ));
    }

    @Test
    public void givenTarGzSIPAndArchiveTypeWhenUncompressObjectThenExtractOK()
        throws IOException, Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.GZIP, getInputStream(SIP_TAR_GZ));
        assertTrue(storage.isExistingObject(CONTAINER_NAME, SIP_FOLDER + File.separator + MANIFEST));
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenTarGzSIPAndUnsupportedArchiveTypeWhenUncompressObjectThenRaiseException()
        throws IOException, Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, "unsupported", getInputStream(SIP_TAR_GZ));
    }

    @Test
    public void getContainerInformationOK() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        final ContainerInformation containerInformation = storage.getContainerInformation(CONTAINER_NAME);
        assertNotNull(containerInformation);
    }

    @Test
    public void getContainerInformationContainerNameNull() throws Exception {
        final ContainerInformation containerInformation = storage.getContainerInformation(null);
        assertNotNull(containerInformation);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void getContainerInformationStorageNotFoundException() throws Exception {
        storage.getContainerInformation(CONTAINER_NAME);
    }

    @Test
    public void givenTarGzSIPArchiveTypeWhenUncompressObjectAndSearchManifestThenReturnExist()
        throws IOException, Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.GZIP, getInputStream(SIP_TAR_GZ));
        assertTrue(storage.isExistingContainer(CONTAINER_NAME));
        assertTrue(storage.isExistingFolder(CONTAINER_NAME, SIP_FOLDER));
    }

    @Test
    public void givenTarArchiveTypeWhenUncompressObjectAndSearchManifestThenReturnExist()
        throws IOException, Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.TAR, getInputStream(SIP_TAR));
        assertTrue(storage.isExistingContainer(CONTAINER_NAME));
        assertTrue(storage.isExistingFolder(CONTAINER_NAME, SIP_FOLDER));
    }

    @Test
    public void countObjectsOK() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.TAR, getInputStream(SIP_TAR));
        long number = storage.countObjects(CONTAINER_NAME);
        assertNotNull(number);
        assertEquals(7, number);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenCountObjectsThenRaiseAnException()
        throws ContentAddressableStorageNotFoundException {
        storage.countObjects(WRONG_CONTAINER_NAME);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenNullContainerNameWhenCountObjectsThenRaiseAnException()
        throws ContentAddressableStorageNotFoundException {
        storage.countObjects(null);
    }

    @Test
    public void givenObjectAlreadyExistsWhenCheckObjectThenOK()
        throws ContentAddressableStorageException, IOException {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));
        final Digest digest = new Digest(ALGO);
        digest.update(getInputStream("file1.pdf"));
        assertTrue(storage.checkObject(CONTAINER_NAME, OBJECT_NAME, digest.toString(), DigestType.MD5));
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenObjectNotExistingWhenCheckObjectThenOK()
        throws ContentAddressableStorageException, IOException {        
        storage.checkObject(CONTAINER_NAME, OBJECT_NAME, "fakeDigest", DigestType.MD5);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenCheckObjectThenRaiseAnException()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException {
        storage.checkObject(CONTAINER_NAME, OBJECT_NAME, "fakeDigest", null);
    }
    
    @Test
    public void givenObjectAlreadyExistsWhenGetObjectMetadataThenNotRaiseAnException() throws Exception {
        String containerName = TENANT_ID + "_"+ TYPE;
        storage.createContainer(containerName);
        storage.putObject(containerName, OBJECT_ID, getInputStream("file1.pdf"));
        //storage.putObject(containerName, OBJECT_ID2, getInputStream("file2.pdf"));
        //get metadata of file
        MetadatasObject result = storage.getObjectMetadatas(containerName, OBJECT_ID);
        assertEquals(OBJECT_ID, result.getObjectName());
        assertEquals(TYPE, result.getType());
        assertEquals("9ba9ef903b46798c83d46bcbd42805eb69ad1b6a8b72e929f87d72f5263a05ade47d8e2f860aece8b9e3acb948364fedf75a3367515cd912965ed22a246ea418", 
            result.getDigest());
        assertEquals("Vitam_" + TENANT_ID, result.getFileOwner());
        assertEquals(6906, result.getFileSize());
        assertNotNull(result.getLastAccessDate());
        assertNotNull(result.getLastModifiedDate());
        
        storage.putObject(containerName, OBJECT_ID2, getInputStream("file2.pdf"));
        //get metadata of directory
        result = storage.getObjectMetadatas(containerName, null);
        assertEquals("0_object", result.getObjectName());
        assertEquals(TYPE, result.getType());
        assertEquals(null, result.getDigest());
        assertEquals("Vitam_" + TENANT_ID, result.getFileOwner());
        assertEquals(13843, result.getFileSize());
        assertNotNull(result.getLastAccessDate());
        assertNotNull(result.getLastModifiedDate());
    }

    @Test
    public void listTest() throws Exception {
        storage.createContainer("container");
        assertNotNull(storage.getContainerInformation("container"));
        for (int i = 0; i < 100; i++) {
            storage.putObject("container", "file_" + i, new FakeInputStream(100, false));
        }
        PageSet<? extends StorageMetadata> pageSet = storage.listContainer("container");
        assertNotNull(pageSet);
        assertFalse(pageSet.isEmpty());
        assertEquals(100, pageSet.size());

        for (int i = 100; i < 150; i++) {
            storage.putObject("container", "file_" + i, new FakeInputStream(100, false));
        }
        pageSet = storage.listContainer("container");
        assertNotNull(pageSet);
        assertFalse(pageSet.isEmpty());
        assertEquals(100, pageSet.size());
        assertNotNull(pageSet.getNextMarker());

        pageSet = storage.listContainerNext("container", pageSet.getNextMarker());
        assertNotNull(pageSet);
        assertFalse(pageSet.isEmpty());
        assertEquals(50, pageSet.size());
        assertNull(pageSet.getNextMarker());
    }
}
