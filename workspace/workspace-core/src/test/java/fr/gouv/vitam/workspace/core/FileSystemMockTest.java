package fr.gouv.vitam.workspace.core;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import fr.gouv.vitam.workspace.api.config.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.core.ContentAddressableStorageImpl;
import fr.gouv.vitam.workspace.core.FileSystemMock;

public class FileSystemMockTest {

	private ContentAddressableStorageImpl storageService;
	private File tempDir;

	@Before
	public void initService() throws IOException {

		StorageConfiguration configuration = new StorageConfiguration();

		tempDir = Files.createTempDir();
		configuration.storagePath = tempDir.getCanonicalPath();
		storageService = new FileSystemMock(configuration);

	}

	@After
	public void deleteTempDir() throws IOException {
		tempDir.deleteOnExit();

	}
	// Container

	@Test
	public void shouldCheckContainerExistence() throws ContentAddressableStorageException {
		String containerName = generateContainerName();
		assertFalse(storageService.containerExists(containerName));

		storageService.createContainer(containerName);
		assertTrue(storageService.containerExists(containerName));
		storageService.deleteContainer(containerName);
		assertFalse(storageService.containerExists(containerName));
	}

	@Test(expected = ContentAddressableStorageException.class)
	public void shouldRaiseAnException_CreatingAContainerWhichAlreadyExists()
			throws ContentAddressableStorageException {
		String containerName = generateContainerName();
		storageService.createContainer(containerName);
		assertTrue(storageService.containerExists(containerName));
		storageService.createContainer(containerName);
		assertFalse(storageService.containerExists(containerName));
		storageService.deleteContainer(containerName);
	}

	@Test(expected = ContentAddressableStorageNotFoundException.class)
	public void shouldRaiseAnException_PurgingAContainerWhichDoesNotExists() throws ContentAddressableStorageException {
		String containerName = generateContainerName();
		assertFalse(storageService.containerExists(containerName));
		storageService.purgeContainer(containerName);

	}

	@Test(expected = ContentAddressableStorageNotFoundException.class)
	public void shouldRaiseAnException_DeletingAContainerWhichDoesNotExists()
			throws ContentAddressableStorageException {
		String containerName = generateContainerName();
		storageService.createContainer(containerName);
		storageService.deleteContainer(containerName);
		storageService.deleteContainer(containerName);
	}

	@Test
	public void shouldCreateAndDeleteContainer() throws ContentAddressableStorageException {
		String containerName = generateContainerName();
		storageService.createContainer(containerName);
		storageService.deleteContainer(containerName);
	}

	@Test
	public void shouldPurgeContainer() throws ContentAddressableStorageException, IOException {
		String containerName = generateContainerName();
		String objectName = "file.pdf";
		assertFalse(storageService.containerExists(containerName));
		try {
			storageService.createContainer(containerName);
			assertFalse(storageService.objectExists(containerName, objectName));
			storageService.putObject(containerName, objectName, getTestBytes("file1.pdf"));
			assertTrue(storageService.objectExists(containerName, objectName));
			storageService.purgeContainer(containerName);
			assertFalse(storageService.objectExists(containerName, objectName));
		} finally {
			storageService.deleteContainer(containerName);
		}
	}

	// Object

	@Test
	public void shouldCheckObjectExistence() throws ContentAddressableStorageException {
		String containerName = generateContainerName();
		String objectName = "file.pdf";
		assertFalse(storageService.objectExists(containerName, objectName));
		try {
			storageService.createContainer(containerName);
			assertFalse(storageService.objectExists(containerName, objectName));
			storageService.putObject(containerName, objectName, getTestBytes("file1.pdf"));
			assertTrue(storageService.objectExists(containerName, objectName));
			storageService.deleteObject(containerName, objectName);
			assertFalse(storageService.objectExists(containerName, objectName));
		} catch (Exception e) {
			storageService.deleteContainer(containerName);
		} finally {
			storageService.deleteContainer(containerName, true);
		}
	}

	@Test
	public void shouldNotRaiseAnException_PuttingAnObjectWhichAlreadyExists()
			throws ContentAddressableStorageException {
		String containerName = generateContainerName();
		try {
			storageService.createContainer(containerName);
			storageService.putObject(containerName, "file.pdf", getTestBytes("file1.pdf"));
			assertTrue(Arrays.equals(getTestBytes("file1.pdf"), storageService.getObject(containerName, "file.pdf")));

			storageService.putObject(containerName, "file.pdf", getTestBytes("file2.pdf"));
			assertTrue(Arrays.equals(getTestBytes("file2.pdf"), storageService.getObject(containerName, "file.pdf")));

			storageService.deleteObject(containerName, "file.pdf");
			assertEquals(0, (storageService.getObject(containerName, "file.pdf")).length);
		} catch (Exception e) {
			storageService.deleteContainer(containerName, true);
		} finally {
			storageService.deleteContainer(containerName, true);
		}
	}

	@Test(expected = ContentAddressableStorageNotFoundException.class)
	public void shouldRaiseAnException_DeletingObject_ContainerDoesNotExists()
			throws ContentAddressableStorageException {
		String containerName = generateContainerName();
		storageService.createContainer(containerName);
		storageService.deleteContainer(containerName);
		assertFalse(storageService.containerExists(containerName));
		storageService.deleteObject(containerName, "file.pdf");

	}

	@Test(expected = ContentAddressableStorageNotFoundException.class)
	public void shouldRaiseAnException_DeletingObjectWhichDoesNotExists()
			throws ContentAddressableStorageException, IOException {
		String containerName = generateContainerName();
		storageService.createContainer(containerName);
		storageService.putObject(containerName, "file.pdf", getTestBytes("file1.pdf"));
		assertTrue(Arrays.equals(getTestBytes("file1.pdf"), storageService.getObject(containerName, "file.pdf")));
		storageService.deleteObject(containerName, "file.pdf");
		assertEquals(0, (storageService.getObject(containerName, "file.pdf")).length);
		storageService.deleteObject(containerName, "file.pdf");

	}

	@Test(expected = ContentAddressableStorageNotFoundException.class)
	public void shouldRaiseAnException_GettingObject_ContainerDoesNotExists()
			throws ContentAddressableStorageException, IOException {
		String containerName = generateContainerName();
		storageService.createContainer(containerName);
		storageService.putObject(containerName, "file.pdf", getTestBytes("file1.pdf"));
		assertTrue(Arrays.equals(getTestBytes("file1.pdf"), storageService.getObject(containerName, "file.pdf")));
		storageService.deleteContainer(containerName, true);
		assertFalse(storageService.containerExists(containerName));
		storageService.getObject(containerName, "file.pdf");

	}
	

	@Test(expected = ContentAddressableStorageNotFoundException.class)
	public void shouldRaiseAnException_PuttingObject_ContainerDoesNotExists()
			throws ContentAddressableStorageException, IOException {
		String containerName = generateContainerName();
		storageService.createContainer(containerName);
		storageService.deleteContainer(containerName);
		assertFalse(storageService.containerExists(containerName));
		storageService.putObject(containerName, "file.pdf", getTestBytes("file1.pdf"));

	}

	@Test
	public void shouldReturnEmptyArrayWhenPutObjectWhichDoesNotExist() throws ContentAddressableStorageException {
		String containerName = generateContainerName();
		try {
			storageService.createContainer(containerName);
			assertEquals(0, (storageService.getObject(containerName, "no-file-here.pdf")).length);
		} finally {
			storageService.deleteContainer(containerName, true);
		}
	}

	@Test
	public void shouldPutGetAndDeleteObject() throws ContentAddressableStorageException {
		String containerName = generateContainerName();
		try {
			storageService.createContainer(containerName);
			storageService.putObject(containerName, "file.pdf", getTestBytes("file1.pdf"));

			assertTrue(Arrays.equals(getTestBytes("file1.pdf"), storageService.getObject(containerName, "file.pdf")));
			storageService.deleteObject(containerName, "file.pdf");
			assertEquals(0, (storageService.getObject(containerName, "file.pdf")).length);
		} catch (Exception e) {
			storageService.deleteContainer(containerName, true);
		} finally {
			storageService.deleteContainer(containerName, true);
		}
	}

	// Check Path parameters (containerName, objectName)

	@Test(expected = IllegalArgumentException.class)
	public void shouldRaiseAnException_CreatingAContainerWithNullParam() {
		storageService.checkContainerNameParam(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldRaiseAnException_CreatingAContainerWithEmptyParam() {
		storageService.checkContainerNameParam("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldRaiseAnException_DeletingAnObjectWithNullParam() throws IOException {
		storageService.checkObjectNameParam("container", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldRaiseAnException_DeletingAnObjectWithEmptyParam() throws IOException {
		storageService.checkObjectNameParam("container", "");
	}

	private String generateContainerName() {
		return "container" + UUID.randomUUID();
	}

	private byte[] getTestBytes(String file) throws IOException {
		return ByteStreams.toByteArray(Thread.currentThread().getContextClassLoader().getResourceAsStream(file));
	}

}
