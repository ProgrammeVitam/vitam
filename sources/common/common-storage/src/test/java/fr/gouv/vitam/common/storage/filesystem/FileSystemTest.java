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
package fr.gouv.vitam.common.storage.filesystem;

import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageTestAbstract;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FileSystemTest extends ContentAddressableStorageTestAbstract {

    /*                                         ⬆
     *                                  Tests are there
     */
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    public FileSystem fileSystem;
    public static final boolean NO_CACHE = true;

    @Before
    public void setup() throws IOException {
        final StorageConfiguration configuration = new StorageConfiguration();
        tempDir = tempFolder.newFolder();
        configuration.setStoragePath(tempDir.getCanonicalPath());
        storage = new FileSystem(configuration);
        fileSystem = new FileSystem(configuration);
        ContentAddressableStorageAbstract.disableContainerCaching();
    }

    @Test
    public void getObjectMetadata_cas_file_not_found() {
        //GIVEN
        String containerName = TENANT_ID + "_" + TYPE;

        File container = new File(tempDir, CONTAINER_NAME);
        container.mkdir();

        //WHEN && THEN
        assertThatThrownBy(() -> fileSystem.getObjectMetadata(containerName, OBJECT_ID, NO_CACHE)).isInstanceOf(
            ContentAddressableStorageNotFoundException.class
        );
    }
}
