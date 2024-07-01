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
package fr.gouv.vitam.common.storage.filesystem.v2;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageTestAbstract;
import fr.gouv.vitam.common.storage.constants.ExtendedAttributes;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class HashFileSystemTest extends ContentAddressableStorageTestAbstract {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(HashFileSystemTest.class);

    private static final String ROOT_CONTAINER = "container";
    private static final String FILE = "test1";
    private static final String DIGEST_EXTENDED_ATTRIBUTE =
        "SHA-512:a7c976db1723adb41274178dc82e9b777941ab201c69de61d0f2bc6d27a3598f594fa748e50d88d3c2bf1e2c2e72c3cfef78c3c6d4afa90391f7e33ababca48e";
    private static final String HASH =
        "9ba9ef903b46798c83d46bcbd42805eb69ad1b6a8b72e929f87d72f5263a05ade47d8e2f860aece8b9e3acb948364fedf75a3367515cd912965ed22a246ea418";

    @Before
    public void setup() throws IOException {
        final StorageConfiguration configuration = new StorageConfiguration();
        tempDir = tempFolder.newFolder();
        configuration.setStoragePath(tempDir.getCanonicalPath());
        storage = new HashFileSystem(configuration);
        ContentAddressableStorageAbstract.disableContainerCaching();
    }

    /**
     * THis test work only if your temporary folder support linux extended attribute
     *
     * @throws Exception
     */
    @Test
    public void should_write_and_read_extended_attribute() throws Exception {
        // Given
        File rootContainer = new File(tempDir, ROOT_CONTAINER);
        rootContainer.mkdir();

        File container = new File(rootContainer, CONTAINER_NAME);
        container.mkdir();

        Path path = container.toPath().resolve("1").resolve("b").resolve("4").resolve("f");
        Files.createDirectories(path);

        Path file = path.resolve(FILE);
        Files.write(file, new byte[] { 1, 2, 3, 4 });

        String hash = storage.getObjectDigest(CONTAINER_NAME, FILE, DigestType.SHA512, true);

        // When
        String hashExtendedAttribute =
            ((HashFileSystem) storage).readExtendedMetadata(file, ExtendedAttributes.DIGEST.getKey());

        // Then
        assertThat("SHA-512:" + hash).isEqualTo(DIGEST_EXTENDED_ATTRIBUTE);
        if (!("SHA-512:" + hash).equalsIgnoreCase(hashExtendedAttribute)) {
            LOGGER.error("EXTENDED ATTRIBUTE not SUPPORTED! You should consider to use XFS filesystem");
        }
    }

    /**
     * THis test work only if your temporary folder support linux extended attribute
     *
     * @throws Exception
     */
    @Test
    public void should_recalculate_and_update_extended_attribute_when_check_digest() throws Exception {
        String containerName = TENANT_ID + "_" + TYPE;

        storage.createContainer(containerName);

        storage.putObject(containerName, OBJECT_ID, getInputStream("file1.pdf"), DigestType.MD5, 6906L);

        // get current SH512 saved Hash in metadata of file
        String currentDigest =
            ((HashFileSystem) storage).getObjectDigestFromMD(containerName, OBJECT_ID, DigestType.SHA512);
        // should be null as we write an MD5 Hash and not a SH521
        assertNull(currentDigest);

        // check if digest has been updated when call getMetadatas
        MetadatasObject result = storage.getObjectMetadata(containerName, OBJECT_ID, true);
        assertNotNull(result);
        assertEquals(OBJECT_ID, result.getObjectName());
        assertEquals(TYPE, result.getType());
        assertEquals(HASH, result.getDigest());
    }

    @Test
    public void should_throw_no_content_exception_when_file_not_found() throws Exception {
        String containerName = TENANT_ID + "_" + TYPE;

        storage.createContainer(containerName);

        // check if digest has been updated when call getMetadatas
        assertThatThrownBy(() -> storage.getObjectMetadata(containerName, OBJECT_ID, true)).isInstanceOf(
            ContentAddressableStorageNotFoundException.class
        );
    }

    private InputStream getInputStream(String file) throws IOException {
        return PropertiesUtils.getResourceAsStream(file);
    }
}
