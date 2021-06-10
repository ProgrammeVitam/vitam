/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.storage.swift;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorage;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.common.storage.swift.Swift;
import fr.gouv.vitam.common.storage.swift.SwiftKeystoneFactoryV3;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Integration tests using docker instances with storage swift/keystone API V3
 */
// docker run -d --rm  -p 5000:5000 -p 35357:35357 -p 8080:8080 --name swift jeantil/openstack-keystone-swift:pike
// docker exec -it swift /swift/bin/register-swift-endpoint.sh http://127.0.0.1:8080
public class SwiftV3ITTest {

    private static final String PROVIDER = "openstack-swift-v3";
    private StorageConfiguration configurationSwiftv3;
    private ContentAddressableStorage swift;



    private String containerName;
    private String objectName;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        configurationSwiftv3 = new StorageConfiguration();
        configurationSwiftv3.setProvider(PROVIDER);
        configurationSwiftv3.setSwiftDomain("Default");
        configurationSwiftv3.setSwiftProjectName("test");

        configurationSwiftv3.setSwiftUser("demo");
        configurationSwiftv3.setSwiftPassword("demo");

        configurationSwiftv3.setSwiftKeystoneAuthUrl("http://127.0.0.1:35357/v3");

        containerName = RandomStringUtils.randomNumeric(1) + "_" + RandomStringUtils.randomAlphabetic(10);
        objectName = GUIDFactory.newGUID().getId();

    }

    @Test
    public void swift_api_v3_main_scenario() throws Exception {
        SwiftKeystoneFactoryV3 swiftKeystoneFactoryV3 = new SwiftKeystoneFactoryV3(configurationSwiftv3);
        swift = new Swift(swiftKeystoneFactoryV3, configurationSwiftv3);

        mainScenario(swift);
    }

    private void mainScenario(ContentAddressableStorage swift) throws Exception {

        assertThat(swift.isExistingContainer(containerName)).isFalse();

        assertThat(swift.isExistingObject(containerName, objectName)).isFalse();

        assertThatThrownBy(() -> swift.deleteObject(containerName, objectName),
            "Delete object in a container that does not exists")
            .isInstanceOf(ContentAddressableStorageNotFoundException.class);

        assertThatThrownBy(() -> {
            InputStream stream = getInputStream("file1.pdf");
            swift.putObject(containerName, objectName, stream, DigestType.SHA512, 6_906L);
        }, "Try to upload a file in a container that does not exists")
            .isInstanceOf(ContentAddressableStorageNotFoundException.class);

        assertThatThrownBy(() -> swift.getObject(containerName, objectName),
            "Try to download a file from a container that does not exists")
            .isInstanceOf(ContentAddressableStorageNotFoundException.class);

        assertThatThrownBy(() -> swift.getObjectDigest(containerName, objectName, DigestType.SHA512, false),
            "Compute digest of object from a container that does not exist")
            .isInstanceOf(ContentAddressableStorageNotFoundException.class);

        assertThatCode(() -> swift.createContainer(containerName)).doesNotThrowAnyException();

        assertThat(swift.isExistingContainer(containerName)).isTrue();

        // re-create a container > idempotent
        assertThatCode(() -> swift.createContainer(containerName)).doesNotThrowAnyException();

        assertThat(swift.isExistingContainer(containerName)).isTrue();

        assertThat(swift.isExistingObject(containerName, objectName)).isFalse();

        assertThatThrownBy(() -> swift.getObjectDigest(containerName, objectName, DigestType.SHA512, false),
            "Compute digest of object that does not exists")
            .isInstanceOf(ContentAddressableStorageNotFoundException.class);

        assertThatThrownBy(() -> swift.getObject(containerName, objectName),
            "Try to download a file that does not exists")
            .isInstanceOf(ContentAddressableStorageNotFoundException.class);

        InputStream file1Stream = getInputStream("file1.pdf");
        swift.putObject(containerName, objectName, file1Stream, DigestType.SHA512, 6_906L);
        file1Stream.close();

        // download an existing file
        ObjectContent response = swift.getObject(containerName, objectName);
        try (InputStream is = response.getInputStream()) {
            File fileDownloaded = tempFolder.newFile();
            FileOutputStream fileOutputStream = new FileOutputStream(fileDownloaded);
            IOUtils.copy(is, fileOutputStream);
            File resourceFile = PropertiesUtils.getResourceFile("file1.pdf");
            assertThat(fileDownloaded.length()).isEqualTo(resourceFile.length());
        }

        assertThatThrownBy(() -> swift.getObjectMetadata(containerName, "nonExistObject", false),
            "Try to get metadata of file that does not exists")
            .isInstanceOf(ContentAddressableStorageNotFoundException.class);

        // get an existing file's metadata
        MetadatasObject metadatasObject = swift.getObjectMetadata(containerName, objectName, false);
        assertThat(metadatasObject.getFileSize()).isEqualTo(6_906L);
        assertThat(metadatasObject.getDigest()).isEqualTo(
            "9ba9ef903b46798c83d46bcbd42805eb69ad1b6a8b72e929f87d72f5263a05ade47d8e2f860aece8b9e3acb948364fedf75a3367515cd912965ed22a246ea418");
        assertThat(metadatasObject.getObjectName()).isEqualTo(objectName);
        assertThat(metadatasObject.getType()).isEqualTo(containerName.split("_")[1]);

        assertThat(swift.isExistingObject(containerName, objectName)).isTrue();

        // compute digest of object that does exists
        String computedDigest = swift.getObjectDigest(containerName, objectName, DigestType.SHA512, false);
        assertThat(computedDigest).isEqualTo(
            "9ba9ef903b46798c83d46bcbd42805eb69ad1b6a8b72e929f87d72f5263a05ade47d8e2f860aece8b9e3acb948364fedf75a3367515cd912965ed22a246ea418");

        assertThatThrownBy(() -> {
            InputStream file2Stream = getInputStream("file2.pdf");
            swift.putObject(containerName, objectName, file2Stream, DigestType.SHA512, 0L);
        }, "Try to upload a file on an existing file with an invalid size length (size < filesize)")
            .isInstanceOf(ContentAddressableStorageException.class);

        assertThatThrownBy(() -> {
            InputStream file2Stream = getInputStream("file2.pdf");
            swift.putObject(containerName, objectName, file2Stream, DigestType.SHA512, 1_000_000L);
        }, "Try to upload a file on an existing file with an invalid size length (size > filesize)")
            .isInstanceOf(ContentAddressableStorageException.class);

        assertThatCode(() -> swift.deleteObject(containerName, objectName)).doesNotThrowAnyException();

        // delete an non existing file > idempotent
        assertThatThrownBy(() -> swift.deleteObject(containerName, objectName))
            .isInstanceOf(ContentAddressableStorageNotFoundException.class);

        swift.close();
    }

    @Test
    @Ignore("ignoré a cause d'un bug sur listcontainer")
    //TODO corriger le bug dans le ticket #8205
    public void swift_api_v3_listing_scenario() throws Exception {
        SwiftKeystoneFactoryV3 swiftKeystoneFactoryV2 = new SwiftKeystoneFactoryV3(configurationSwiftv3);
        swift = new Swift(swiftKeystoneFactoryV2, configurationSwiftv3);

        int nbIter = 2;
        assertThatCode(() -> {
            swift.createContainer(containerName);
        }).doesNotThrowAnyException();

        assertThat(swift.isExistingContainer(containerName)).isTrue();

        // upload multiple times the same file1
        for (int i = 0; i < (nbIter * 10 + 5); i++) {

            InputStream file1Stream = getInputStream("file1.pdf");
            swift.putObject(containerName, objectName + i, file1Stream, DigestType.SHA512, 6_906L);
            file1Stream.close();
        }
        ObjectListingListener objectListingListener = mock(ObjectListingListener.class);

        swift.listContainer(containerName, objectListingListener);

        ArgumentCaptor<ObjectEntry> objectEntryArgumentCaptor = ArgumentCaptor.forClass(ObjectEntry.class);
        verify(objectListingListener, times(nbIter * 10 + 5)).handleObjectEntry(objectEntryArgumentCaptor.capture());

        objectEntryArgumentCaptor.getAllValues()
            .forEach(capturedObjectEntry -> assertThat(capturedObjectEntry.getSize()).isEqualTo(6906L));

        Set<String> capturedFileNames = objectEntryArgumentCaptor.getAllValues().stream()
            .map(ObjectEntry::getObjectId)
            .collect(Collectors.toSet());
        Set<String> expectedFileNames = IntStream.range(0, nbIter * 10 + 5)
            .mapToObj(i -> objectName + i)
            .collect(Collectors.toSet());
        assertThat(capturedFileNames).isEqualTo(expectedFileNames);
    }

    private InputStream getInputStream(String file) throws IOException {
        return PropertiesUtils.getResourceAsStream(file);
    }

}
