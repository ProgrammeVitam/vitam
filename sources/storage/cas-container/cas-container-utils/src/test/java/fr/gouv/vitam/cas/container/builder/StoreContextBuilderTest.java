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
package fr.gouv.vitam.cas.container.builder;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorage;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.constants.StorageProvider;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

import static fr.gouv.vitam.common.digest.DigestType.SHA512;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Test When you have swift Vm installed in your computer
 */
public class StoreContextBuilderTest {

    private StorageConfiguration configuration;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        configuration = new StorageConfiguration();
        configuration.setProvider(StorageProvider.SWIFT_AUTH_V3.getValue());
        configuration.setSwiftDomain("default");
        configuration.setSwiftProjectName("demo");
        configuration.setSwiftKeystoneAuthUrl("http://localhost:8000/identity/v3");
        configuration.setSwiftUser("demo");
        configuration.setSwiftPassword("password");
    }

    /**
     * Swift Test comment @Ignore when you want to launch this test
     *
     * @throws Exception Exception
     */
    @Ignore
    @Test
    public void should_write_in_swiftContainer() throws Exception {
        // Given
        ContentAddressableStorage contentAddressableStorage = StoreContextBuilder.newStoreContext(configuration, null);

        if (!contentAddressableStorage.isExistingContainer("testContainer")) {
            contentAddressableStorage.createContainer("testContainer");
        }
        String uuid = UUID.randomUUID().toString();

        File resourceFile = PropertiesUtils.getResourceFile("3500.txt");

        // When
        contentAddressableStorage.putObject("testContainer", uuid, new FileInputStream(resourceFile), SHA512, 3500L);

        // Then
        ObjectContent response = contentAddressableStorage.getObject("testContainer", uuid);
        try (InputStream is = response.getInputStream()) {
            File fileDownloaded = tempFolder.newFile();

            FileOutputStream fileOutputStream = new FileOutputStream(fileDownloaded);
            IOUtils.copy(is, fileOutputStream);

            // Then
            assertThat(fileDownloaded.length()).isEqualTo(resourceFile.length());
        }

        String objectDigest = new Digest(SHA512).update(resourceFile).digestHex();
        assertThat(contentAddressableStorage.getObjectMetadata("testContainer", uuid, false).getDigest()).isEqualTo(
            objectDigest
        );
    }
}
