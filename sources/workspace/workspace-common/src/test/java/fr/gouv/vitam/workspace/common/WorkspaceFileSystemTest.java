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
package fr.gouv.vitam.workspace.common;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageCompressedFileException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ZipFilesNameNotAllowedException;
import fr.gouv.vitam.workspace.api.model.FileParams;
import fr.gouv.vitam.workspace.api.model.TimeToLive;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

public class WorkspaceFileSystemTest {

    private static final String CONTAINER_NAME = "myContainer";
    private static final String CONTAINER_NAME2 = "myContainer2";
    private static final String OBJECT_NAME = "myObject";
    private static final String FOLDER_NAME = "myFolder";
    private static final String SIP_CONTAINER = "sipContainer";
    private static final String SIP_FOLDER = "SIP";
    private static final String SLASH = "/";
    private static final String MANIFEST = "manifest.xml";
    private static final String CONTENT_FOLDER = "Content";
    private static final String SIP_TAR_GZ = "sip.tar.gz";
    private static final String SIP_TAR = "sip.tar";
    private static final String file1Name = "file1.pdf";
    private static final String file2Name = "file2.pdf";

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

    private long getLength(String file) throws IOException {
        return PropertiesUtils.getResourceFile(file).length();
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

        storage.getObject(CONTAINER_NAME, OBJECT_NAME, null, null);
    }

    @Test
    public void givenFileNotFoundWhenGetObjectThenRaiseAnException() throws Exception {
        storage.createContainer(CONTAINER_NAME);

        assertThatThrownBy(() -> storage.getObject(CONTAINER_NAME, OBJECT_NAME, null, null)).isInstanceOf(
            ContentAddressableStorageNotFoundException.class
        );
    }

    @Test
    public void givenPutAtomicObjectThenGetObjectOK() throws Exception {
        // Given
        storage.createContainer(CONTAINER_NAME);

        // When
        storage.putAtomicObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), getLength("file1.pdf"));

        // Then
        assertThat(storage.isExistingObject(CONTAINER_NAME, OBJECT_NAME)).isTrue();
        InputStream is = (InputStream) storage.getObject(CONTAINER_NAME, OBJECT_NAME, null, null).getEntity();
        assertThat(is).hasSameContentAs(getInputStream("file1.pdf"));
    }

    @Test
    public void givenPutAtomicObjectWithSubDirectoriesThenGetObjectOK() throws Exception {
        // Given
        storage.createContainer(CONTAINER_NAME);

        // When
        storage.putAtomicObject(
            CONTAINER_NAME,
            FOLDER_NAME + "/" + OBJECT_NAME,
            getInputStream("file1.pdf"),
            getLength("file1.pdf")
        );

        // Then
        assertThat(storage.isExistingObject(CONTAINER_NAME, FOLDER_NAME + "/" + OBJECT_NAME)).isTrue();
        InputStream is = (InputStream) storage
            .getObject(CONTAINER_NAME, FOLDER_NAME + "/" + OBJECT_NAME, null, null)
            .getEntity();
        assertThat(is).hasSameContentAs(getInputStream("file1.pdf"));
    }

    @Test
    public void givenExistingFileWhenPutAtomicObjectThenException() throws Exception {
        // Given
        storage.createContainer(CONTAINER_NAME);

        // When / Then
        storage.putAtomicObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), getLength("file1.pdf"));

        assertThatThrownBy(
            () ->
                storage.putAtomicObject(
                    CONTAINER_NAME,
                    OBJECT_NAME,
                    getInputStream("file2.pdf"),
                    getLength("file2.pdf")
                )
        ).isInstanceOf(ContentAddressableStorageException.class);

        assertThat(storage.isExistingObject(CONTAINER_NAME, OBJECT_NAME)).isTrue();
        InputStream is = (InputStream) storage.getObject(CONTAINER_NAME, OBJECT_NAME, null, null).getEntity();
        assertThat(is).hasSameContentAs(getInputStream("file1.pdf"));
    }

    @Test
    public void givenPutAtomicObjectThenFileExists() throws Exception {
        // Given
        storage.createContainer(CONTAINER_NAME);
        storage.putAtomicObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), getLength("file1.pdf"));

        // When
        boolean existingAtomicObject = storage.isExistingObject(CONTAINER_NAME, OBJECT_NAME);

        // Then
        assertThat(existingAtomicObject).isTrue();
    }

    @Test
    public void givenDeletedAtomicObjectThenGetObjectNotExists() throws Exception {
        // Given
        storage.createContainer(CONTAINER_NAME);
        storage.putAtomicObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), getLength("file1.pdf"));

        // When
        storage.deleteObject(CONTAINER_NAME, OBJECT_NAME);

        // Then
        assertThat(storage.isExistingObject(CONTAINER_NAME, OBJECT_NAME)).isFalse();
    }

    @Test
    public void givenDeletedAtomicObjectThenGetObjectThrowsException() throws Exception {
        // Given
        storage.createContainer(CONTAINER_NAME);
        storage.putAtomicObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"), getLength("file1.pdf"));

        // When
        storage.deleteObject(CONTAINER_NAME, OBJECT_NAME);

        // Then
        assertThatThrownBy(() -> storage.getObject(CONTAINER_NAME, OBJECT_NAME, null, null)).isInstanceOf(
            ContentAddressableStorageNotFoundException.class
        );
    }

    @Test
    public void givenExistingFileWhenGetObjectWithZeroOffsetAndNoSizeThenOK() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));

        InputStream is = (InputStream) storage.getObject(CONTAINER_NAME, OBJECT_NAME, 0L, null).getEntity();

        assertThat(is).hasSameContentAs(getInputStream("file1.pdf"));
    }

    @Test
    public void givenExistingFileWhenGetObjectWithZeroOffsetAndMaxSizeThenOK() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));

        InputStream is = (InputStream) storage.getObject(CONTAINER_NAME, OBJECT_NAME, 0L, 1_000_000_000L).getEntity();

        assertThat(is).hasSameContentAs(getInputStream("file1.pdf"));
    }

    @Test
    public void givenExistingFileWhenGetObjectWithOffsetAndSizeThenOK() throws Exception {
        // Given
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));

        // When
        InputStream is = (InputStream) storage.getObject(CONTAINER_NAME, OBJECT_NAME, 100L, 200L).getEntity();

        // Then

        // Skip first 100 bytes & limit to 200 bytes
        InputStream expectedInputStream = getInputStream("file1.pdf");
        IOUtils.readFully(expectedInputStream, 100);
        expectedInputStream = new BoundedInputStream(expectedInputStream, 200L);

        assertThat(is).hasSameContentAs(expectedInputStream);
    }

    @Test
    public void givenExistingFileWhenGetObjectWithOffsetAndNoMaxSizeThenOK() throws Exception {
        // Given
        storage.createContainer(CONTAINER_NAME);
        storage.putObject(CONTAINER_NAME, OBJECT_NAME, getInputStream("file1.pdf"));

        // When
        InputStream is = (InputStream) storage.getObject(CONTAINER_NAME, OBJECT_NAME, 100L, null).getEntity();

        // Then

        // Skip first 100 bytes
        InputStream expectedInputStream = getInputStream("file1.pdf");
        IOUtils.readFully(expectedInputStream, 100);

        assertThat(is).hasSameContentAs(expectedInputStream);
    }

    // Uri List of Digital Object from Content folder

    @Test
    public void givenContainerAlreadyExistAndFolderAlreadyExistAndSubFolderAlreadyExistWhenCheckListUriNotEmptyThenReturnTrue()
        throws IOException, Exception {
        // Given container exists
        storage.createContainer(SIP_CONTAINER);

        // Given a root folder "SIP_FOLDER", add manifest.xml to this root folder
        final String manifestName = new StringBuilder().append(SIP_FOLDER).append(SLASH).append(MANIFEST).toString();
        storage.putObject(SIP_CONTAINER, manifestName, getInputStream(MANIFEST));

        // Given a sub folder "CONTENT_FOLDER" add digital objects
        final String contentSubFolder = new StringBuilder()
            .append(SIP_FOLDER)
            .append(SLASH)
            .append(CONTENT_FOLDER)
            .toString();
        // workspace.createFolder(SIP_CONTAINER, contentSubFolder);
        final String fileName1 = new StringBuilder()
            .append(contentSubFolder)
            .append(SLASH)
            .append("file1.pdf")
            .toString();
        storage.putObject(SIP_CONTAINER, fileName1, getInputStream("file1.pdf"));
        final String fileName2 = new StringBuilder()
            .append(contentSubFolder)
            .append(SLASH)
            .append("space - file2.pdf")
            .toString();
        storage.putObject(SIP_CONTAINER, fileName2, getInputStream("file2.pdf"));

        // Then check that there is 2 URIs found recursively from the content folder
        assertThat(storage.getListUriDigitalObjectFromFolder(SIP_CONTAINER, contentSubFolder)).isNotNull().isNotEmpty();
        assertThat(storage.getListUriDigitalObjectFromFolder(SIP_CONTAINER, contentSubFolder)).hasSize(2);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotExistWhenCheckListUriNotEmptyThenRaiseAnException() throws IOException, Exception {
        // Then check that there is 3 URIs found recursively from the root folder
        assertThat(storage.getListUriDigitalObjectFromFolder(SIP_CONTAINER, SIP_FOLDER)).isNotNull().isNotEmpty();
        assertThat(storage.getListUriDigitalObjectFromFolder(SIP_CONTAINER, SIP_FOLDER)).hasSize(3);
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void givenContainerNotFoundWhenUnzipObjectThenRaiseAnException() throws IOException, Exception {
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP, getInputStream("sip.zip"));
    }

    @Test(expected = ZipFilesNameNotAllowedException.class)
    public void givenSIPWithNotAllowedFileOrDirectoryNameThenException() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(
            CONTAINER_NAME,
            SIP_FOLDER,
            CommonMediaType.ZIP,
            getInputStream("KO_FILE_extension_caractere_special.zip")
        );
    }

    @Test
    public void givenSIPWithSubFoldersThanUncompressZipSucceeds() throws Exception {
        // Given
        storage.createContainer(CONTAINER_NAME);

        // When
        storage.uncompressObject(
            CONTAINER_NAME,
            SIP_FOLDER,
            CommonMediaType.ZIP,
            getInputStream("SIP_OK_SubFolders.zip")
        );

        // Then
        assertThat(
            tempDir
                .toPath()
                .resolve(CONTAINER_NAME)
                .resolve(SIP_FOLDER)
                .resolve("Content")
                .resolve("ID")
                .resolve("13.txt")
        )
            .exists()
            .isRegularFile();

        boolean existingObject = storage.isExistingObject(CONTAINER_NAME, SIP_FOLDER + "/Content/ID/13.txt");
        assertThat(existingObject).isTrue();

        Response object = storage.getObject(CONTAINER_NAME, SIP_FOLDER + "/Content/ID/13.txt", null, null);
        assertThat((InputStream) object.getEntity()).hasContent("test 1");

        assertThat(storage.getListUriDigitalObjectFromFolder(CONTAINER_NAME, SIP_FOLDER)).containsExactlyInAnyOrder(
            URI.create(URLEncoder.encode("manifest.xml", StandardCharsets.UTF_8)),
            URI.create(URLEncoder.encode("Content/ID/13.txt", StandardCharsets.UTF_8))
        );
    }

    @Test
    public void givenSIPWithPathTraversalThenException() throws Exception {
        // Given
        storage.createContainer(CONTAINER_NAME);

        // When / Then
        assertThatThrownBy(
            () ->
                storage.uncompressObject(
                    CONTAINER_NAME,
                    SIP_FOLDER,
                    CommonMediaType.ZIP,
                    getInputStream("SIP_KO_PathTraversalInFileName.zip")
                )
        ).isInstanceOf(ZipFilesNameNotAllowedException.class);

        assertThat(tempDir.toPath().resolve("BadFile.txt")).doesNotExist();

        boolean existingObject = storage.isExistingObject(CONTAINER_NAME, SIP_FOLDER + "/Content/ID/13.txt");
        assertThat(existingObject).isFalse();
    }

    @Test
    public void givenSIPWithBackSlashThenException() throws Exception {
        // Given
        storage.createContainer(CONTAINER_NAME);

        // When / Then
        assertThatThrownBy(
            () ->
                storage.uncompressObject(
                    CONTAINER_NAME,
                    SIP_FOLDER,
                    CommonMediaType.ZIP,
                    getInputStream("SIP_KO_BackSlashInFileName.zip")
                )
        ).isInstanceOf(ZipFilesNameNotAllowedException.class);

        assertThat(
            tempDir
                .toPath()
                .resolve(CONTAINER_NAME)
                .resolve(SIP_FOLDER)
                .resolve("Content")
                .resolve("ID")
                .resolve("13.txt")
        ).doesNotExist();
        assertThat(
            tempDir.toPath().resolve(CONTAINER_NAME).resolve(SIP_FOLDER).resolve("Content").resolve("ID\\13.txt")
        ).doesNotExist();
    }

    @Test
    public void givenSIPWithPlusSignInFileNameThenException() throws Exception {
        // Given
        storage.createContainer(CONTAINER_NAME);

        // When / Then
        assertThatThrownBy(
            () ->
                storage.uncompressObject(
                    CONTAINER_NAME,
                    SIP_FOLDER,
                    CommonMediaType.ZIP,
                    getInputStream("SIP_KO_PlusSignInFileName.zip")
                )
        ).isInstanceOf(ZipFilesNameNotAllowedException.class);

        assertThat(
            tempDir.toPath().resolve(CONTAINER_NAME).resolve(SIP_FOLDER).resolve("Content").resolve("ID+13.txt")
        ).doesNotExist();
    }

    @Test
    public void givenSIPWithAtSignInFileNameThenOK() throws Exception {
        // Given
        storage.createContainer(CONTAINER_NAME);

        // When
        storage.uncompressObject(
            CONTAINER_NAME,
            SIP_FOLDER,
            CommonMediaType.ZIP,
            getInputStream("SIP_OK_AtSignInFileName.zip")
        );

        // Then
        assertThat(tempDir.toPath().resolve(CONTAINER_NAME).resolve(SIP_FOLDER).resolve("Content").resolve("ID@13.txt"))
            .exists()
            .isRegularFile();

        boolean existingObject = storage.isExistingObject(CONTAINER_NAME, SIP_FOLDER + "/Content/ID@13.txt");
        assertThat(existingObject).isTrue();

        Response object = storage.getObject(CONTAINER_NAME, SIP_FOLDER + "/Content/ID@13.txt", null, null);
        assertThat((InputStream) object.getEntity()).hasContent("test 1");

        assertThat(storage.getListUriDigitalObjectFromFolder(CONTAINER_NAME, SIP_FOLDER)).containsExactlyInAnyOrder(
            URI.create(URLEncoder.encode("manifest.xml", StandardCharsets.UTF_8)),
            URI.create(URLEncoder.encode("Content/ID@13.txt", StandardCharsets.UTF_8))
        );
    }

    @Test(expected = ContentAddressableStorageAlreadyExistException.class)
    public void givenFolderAlreadyExisitsWhenUnzipObjectThenRaiseAnException() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.createFolder(CONTAINER_NAME, SIP_FOLDER);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP, getInputStream("sip.zip"));
    }

    @Test(expected = ContentAddressableStorageException.class)
    public void givenNullInputStreamWhenUnzipObjectThenRaiseAnException() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP, null);
    }

    @Test
    public void givenContainerAlreadyExisitsWhenUnzipObjectThenOk() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP, getInputStream("sip.zip"));
    }

    @Test(expected = ContentAddressableStorageCompressedFileException.class)
    public void givenContainerAlreadyExisitsWhenUnzipObjectEmptyThenZipException() throws IOException, Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP, getInputStream("empty_zip.zip"));
    }

    @Test(expected = ContentAddressableStorageException.class)
    public void givenContainerAlreadyExisitsWhenUnzipObjectNotZipThenZipException() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(
            CONTAINER_NAME,
            SIP_FOLDER,
            CommonMediaType.ZIP,
            getInputStream("SIP_mauvais_format.pdf")
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyContainerNameParamWhenUnzipSipThenRaiseAnException() throws Exception {
        storage.uncompressObject(null, null, null, null);
    }

    @Test(expected = ContentAddressableStorageException.class)
    public void givenTarGzSIPAndBadArchiveTypeWhenUncompressObjectThenRaiseAnException() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.ZIP, getInputStream(SIP_TAR_GZ));
    }

    @Test
    public void givenTarGzSIPAndArchiveTypeWhenUncompressObjectThenExtractOK() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.XGZIP, getInputStream(SIP_TAR_GZ));
        Assert.assertTrue(storage.isExistingObject(CONTAINER_NAME, SIP_FOLDER + File.separator + MANIFEST));
    }

    @Test
    public void givenZipSIPAndArchiveTypeWhenManifestFileNamePersonalizedThenRenamedToManifestDotXmlOK()
        throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(
            CONTAINER_NAME,
            SIP_FOLDER,
            CommonMediaType.ZIP,
            getInputStream("personalized_manifest_file_name.zip")
        );
        Assert.assertTrue(storage.isExistingObject(CONTAINER_NAME, SIP_FOLDER + File.separator + MANIFEST));
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenTarGzSIPAndUnsupportedArchiveTypeWhenUncompressObjectThenRaiseException() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, "unsupported", getInputStream(SIP_TAR_GZ));
    }

    @Test
    public void givenTarGzSIPArchiveTypeWhenUncompressObjectAndSearchManifestThenReturnExist() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.XGZIP, getInputStream(SIP_TAR_GZ));
        Assert.assertTrue(storage.isExistingContainer(CONTAINER_NAME));
        Assert.assertTrue(storage.isExistingFolder(CONTAINER_NAME, SIP_FOLDER));
    }

    @Test
    public void givenTarArchiveTypeWhenUncompressObjectAndSearchManifestThenReturnExist() throws Exception {
        storage.createContainer(CONTAINER_NAME);
        storage.uncompressObject(CONTAINER_NAME, SIP_FOLDER, CommonMediaType.TAR, getInputStream(SIP_TAR));
        Assert.assertTrue(storage.isExistingContainer(CONTAINER_NAME));
        Assert.assertTrue(storage.isExistingFolder(CONTAINER_NAME, SIP_FOLDER));
    }

    @Test
    public void should_compress_container_name() throws Exception {
        // Given
        String fileName = "result.zip";
        storage.createContainer(CONTAINER_NAME);
        String folder1 = "folder1";
        storage.createFolder(CONTAINER_NAME, folder1);

        storage.putObject(CONTAINER_NAME + "/" + folder1, "1.txt", new ByteArrayInputStream("bobbie1".getBytes()));
        storage.putObject(CONTAINER_NAME + "/" + folder1, "2.txt", new ByteArrayInputStream("bobby404".getBytes()));
        storage.putObject(CONTAINER_NAME, "3.txt", new ByteArrayInputStream("bobby5".getBytes()));

        // When
        storage.compress(CONTAINER_NAME, Lists.newArrayList(folder1, "3.txt"), fileName, CONTAINER_NAME);

        // Then
        Path zipPath = Paths.get(tempDir.toString(), CONTAINER_NAME, fileName);
        assertThat(zipPath).exists();

        ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();
        ArchiveInputStream archiveInputStream = archiveStreamFactory.createArchiveInputStream(
            ArchiveStreamFactory.ZIP,
            new FileInputStream(zipPath.toString())
        );

        List<ArchiveEntry> entries = findArchiveEntries(archiveInputStream);
        assertThat(entries)
            .extracting("name", "size")
            .containsExactlyInAnyOrder(tuple("folder1/1.txt", 7L), tuple("folder1/2.txt", 8L), tuple("3.txt", 6L));
        assertThat(entries).extracting("name").containsExactlyInAnyOrder("folder1/1.txt", "folder1/2.txt", "3.txt");
    }

    @Test
    public void should_compress_container_name_into_another_container() throws Exception {
        // Given
        String fileName = "targetDir/result.zip";
        storage.createContainer(CONTAINER_NAME);
        storage.createContainer(CONTAINER_NAME2);
        String folder1 = "folder1";
        String folder2 = "targetDir";
        storage.createFolder(CONTAINER_NAME, folder1);
        storage.createFolder(CONTAINER_NAME2, folder2);

        storage.putObject(CONTAINER_NAME + "/" + folder1, "1.txt", new ByteArrayInputStream("bobbie1".getBytes()));
        storage.putObject(CONTAINER_NAME + "/" + folder1, "2.txt", new ByteArrayInputStream("bobby404".getBytes()));
        storage.putObject(CONTAINER_NAME, "3.txt", new ByteArrayInputStream("bobby5".getBytes()));

        // When
        storage.compress(CONTAINER_NAME, Lists.newArrayList(folder1, "3.txt"), fileName, CONTAINER_NAME2);

        // Then
        Path zipPath = Paths.get(tempDir.toString(), CONTAINER_NAME2, fileName);
        assertThat(zipPath).exists();

        ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();
        ArchiveInputStream archiveInputStream = archiveStreamFactory.createArchiveInputStream(
            ArchiveStreamFactory.ZIP,
            new FileInputStream(zipPath.toString())
        );

        List<ArchiveEntry> entries = findArchiveEntries(archiveInputStream);
        assertThat(entries)
            .extracting("name", "size")
            .containsExactlyInAnyOrder(tuple("folder1/1.txt", 7L), tuple("folder1/2.txt", 8L), tuple("3.txt", 6L));
        assertThat(entries).extracting("name").containsExactlyInAnyOrder("folder1/1.txt", "folder1/2.txt", "3.txt");
    }

    @Test
    public void purgeOldFilesInContainerShouldNotDeleteNewFiles() throws Exception {
        // Given
        storage.createContainer(CONTAINER_NAME);
        String folder1 = "folder1";
        storage.createFolder(CONTAINER_NAME, folder1);

        storage.putObject(CONTAINER_NAME + "/" + folder1, "1.txt", new ByteArrayInputStream("bobbie1".getBytes()));
        storage.putObject(CONTAINER_NAME, "2.txt", new ByteArrayInputStream("bobby5".getBytes()));

        // When
        storage.purgeOldFilesInContainer(CONTAINER_NAME, new TimeToLive(1, ChronoUnit.HOURS));

        // Then
        assertThat(storage.getObject(CONTAINER_NAME + "/" + folder1, "1.txt", null, null).getStatus()).isEqualTo(200);
        assertThat(storage.getObject(CONTAINER_NAME, "2.txt", null, null).getStatus()).isEqualTo(200);
    }

    @Test
    public void purgeOldFilesInContainerShouldDeleteOldFiles() throws Exception {
        // Given
        storage.createContainer(CONTAINER_NAME);
        String folder1 = "folder1";
        storage.createFolder(CONTAINER_NAME, folder1);

        storage.putObject(CONTAINER_NAME + "/" + folder1, "1.txt", new ByteArrayInputStream("bobbie1".getBytes()));
        storage.putObject(CONTAINER_NAME, "2.txt", new ByteArrayInputStream("bobby5".getBytes()));

        TimeUnit.SECONDS.sleep(2);

        // When
        storage.purgeOldFilesInContainer(CONTAINER_NAME, new TimeToLive(1, ChronoUnit.SECONDS));

        // Then
        assertThatThrownBy(() -> storage.getObject(CONTAINER_NAME + "/" + folder1, "1.txt", null, null)).isInstanceOf(
            ContentAddressableStorageNotFoundException.class
        );
        assertThatThrownBy(() -> storage.getObject(CONTAINER_NAME, "2.txt", null, null)).isInstanceOf(
            ContentAddressableStorageNotFoundException.class
        );
    }

    @Test
    public void should_get_files_with_params_from_folder_existant_container_and_existant_folder() throws Exception {
        // Given
        storage.createContainer(SIP_CONTAINER);

        final String manifestName = new StringBuilder().append(SIP_FOLDER).append(SLASH).append(MANIFEST).toString();
        storage.putObject(SIP_CONTAINER, manifestName, getInputStream(MANIFEST));

        final String contentSubFolder = new StringBuilder()
            .append(SIP_FOLDER)
            .append(SLASH)
            .append(CONTENT_FOLDER)
            .toString();

        final String file1Path = new StringBuilder()
            .append(contentSubFolder)
            .append(SLASH)
            .append(file1Name)
            .toString();
        storage.putObject(SIP_CONTAINER, file1Path, getInputStream(file1Name));

        final String file2Path = new StringBuilder()
            .append(contentSubFolder)
            .append(SLASH)
            .append(file2Name)
            .toString();
        storage.putObject(SIP_CONTAINER, file2Path, getInputStream(file2Name));

        // When
        Map<String, FileParams> filesWithParamsResult = storage.getFilesWithParamsFromFolder(
            SIP_CONTAINER,
            contentSubFolder
        );

        // Then
        assertThat(filesWithParamsResult).isNotNull().isNotEmpty();
        assertThat(filesWithParamsResult).hasSize(2);
        assertThat(filesWithParamsResult.get(file1Name).getSize()).isEqualTo(getLength(file1Name));
        assertThat(filesWithParamsResult.get(file2Name).getSize()).isEqualTo(getLength(file2Name));
    }

    @Test(expected = ContentAddressableStorageNotFoundException.class)
    public void should_not_get_files_with_params_from_folder_inexistant_container() throws Exception {
        // Given
        final String manifestName = new StringBuilder().append(SIP_FOLDER).append(SLASH).append(MANIFEST).toString();
        storage.putObject(SIP_CONTAINER, manifestName, getInputStream(MANIFEST));

        final String contentSubFolder = new StringBuilder()
            .append(SIP_FOLDER)
            .append(SLASH)
            .append(CONTENT_FOLDER)
            .toString();

        final String file1Path = new StringBuilder()
            .append(contentSubFolder)
            .append(SLASH)
            .append(file1Name)
            .toString();
        storage.putObject(SIP_CONTAINER, file1Path, getInputStream(file1Name));

        final String file2Path = new StringBuilder()
            .append(contentSubFolder)
            .append(SLASH)
            .append(file2Name)
            .toString();
        storage.putObject(SIP_CONTAINER, file2Path, getInputStream(file2Name));

        // When
        storage.getFilesWithParamsFromFolder(SIP_CONTAINER, contentSubFolder);
    }

    @Test
    public void should_not_get_files_with_params_from_folder_inexistant_folder() throws Exception {
        // Given
        storage.createContainer(SIP_CONTAINER);

        final String manifestName = new StringBuilder().append(SIP_FOLDER).append(SLASH).append(MANIFEST).toString();
        storage.putObject(SIP_CONTAINER, manifestName, getInputStream(MANIFEST));

        final String contentSubFolder = new StringBuilder()
            .append(SIP_FOLDER)
            .append(SLASH)
            .append(CONTENT_FOLDER)
            .toString();

        final String file1Path = new StringBuilder()
            .append(contentSubFolder)
            .append(SLASH)
            .append(file1Name)
            .toString();
        storage.putObject(SIP_CONTAINER, file1Path, getInputStream(file1Name));

        final String file2Path = new StringBuilder()
            .append(contentSubFolder)
            .append(SLASH)
            .append(file2Name)
            .toString();
        storage.putObject(SIP_CONTAINER, file2Path, getInputStream(file2Name));

        // When
        Map<String, FileParams> filesWithParamsResult = storage.getFilesWithParamsFromFolder(
            SIP_CONTAINER,
            "wrongFolder"
        );

        // Then
        assertThat(filesWithParamsResult).isNotNull().isEmpty();
    }

    private List<ArchiveEntry> findArchiveEntries(ArchiveInputStream archiveInputStream) throws IOException {
        List<ArchiveEntry> entries = new ArrayList<>();
        ArchiveEntry nextEntry = archiveInputStream.getNextEntry();
        while (nextEntry != null) {
            entries.add(nextEntry);
            nextEntry = archiveInputStream.getNextEntry();
        }
        return entries;
    }
}
