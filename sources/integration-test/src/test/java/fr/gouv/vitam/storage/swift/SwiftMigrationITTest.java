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
package fr.gouv.vitam.storage.swift;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.swift.Swift;
import fr.gouv.vitam.common.storage.swift.SwiftKeystoneFactoryV3;
import fr.gouv.vitam.common.storage.swift.VitamSwiftObjectStorageService;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.utils.ContainerUtils;
import fr.gouv.vitam.storage.offers.migration.SwiftMigrationMode;
import fr.gouv.vitam.storage.offers.migration.SwiftMigrationService;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.storage.object.SwiftObject;
import org.openstack4j.model.storage.object.options.ObjectListOptions;
import org.openstack4j.model.storage.object.options.ObjectPutOptions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.storage.swift.Swift.X_OBJECT_MANIFEST;
import static fr.gouv.vitam.common.storage.swift.Swift.X_OBJECT_META_DIGEST;
import static fr.gouv.vitam.common.storage.swift.Swift.X_OBJECT_META_DIGEST_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests using docker instances with storage swift/keystone API V2 & V3
 */
// docker run -d --rm  -p 5000:5000 -p 35357:35357 -p 8080:8080 --name swift jeantil/openstack-keystone-swift:pike
// docker exec -it swift /swift/bin/register-swift-endpoint.sh http://127.0.0.1:8080
public class SwiftMigrationITTest {

    private static final String PROVIDER_V3 = "openstack-swift-v3";
    private static final String DATA_DIGEST =
        "9ba9ef903b46798c83d46bcbd42805eb69ad1b6a8b72e929f87d72f5263a05ade47d8e2f860aece8b9e3acb948364fedf75a3367515cd912965ed22a246ea418";
    private static final String SHA512_DIGEST_TYPE = "SHA-512";
    private static final int MAX_RETRIES = 100;
    private List<Integer> initialTenants;
    private String initialEnvName;
    private SwiftKeystoneFactoryV3 swiftKeystoneFactoryV3;
    private Swift swift;
    private VitamSwiftObjectStorageService vitamSwiftObjectStorageService;

    private String containerName;

    @Before
    public void setUp() throws Exception {
        initialTenants = VitamConfiguration.getTenants();
        initialEnvName = VitamConfiguration.getEnvironmentName();
        VitamConfiguration.setTenants(List.of(0, 1));
        VitamConfiguration.setEnvironmentName(RandomStringUtils.randomNumeric(8));

        StorageConfiguration configurationSwift = new StorageConfiguration();
        configurationSwift.setProvider(PROVIDER_V3);
        configurationSwift.setSwiftDomain("Default");
        configurationSwift.setSwiftProjectName("test");

        configurationSwift.setSwiftUser("demo");
        configurationSwift.setSwiftPassword("demo");
        configurationSwift.setSwiftKeystoneAuthUrl("http://127.0.0.1:35357/v3");
        configurationSwift.setEnableCustomHeaders(false);

        swiftKeystoneFactoryV3 = new SwiftKeystoneFactoryV3(configurationSwift);

        swift = new Swift(swiftKeystoneFactoryV3, configurationSwift, 700L);
        vitamSwiftObjectStorageService = new VitamSwiftObjectStorageService(swiftKeystoneFactoryV3);

        containerName = ContainerUtils.buildContainerName(DataCategory.OBJECT, "0");
        String emptyExistingContainer = ContainerUtils.buildContainerName(DataCategory.UNIT, "0");
        swift.createContainer(containerName);
        swift.createContainer(emptyExistingContainer);

        // obj0 : Regular (small) object
        writeRegularObject(containerName, "obj0");

        // obj1 : Object segments without manifest object ==> To delete
        writeLargeObject(containerName, "obj1", true, false, false, false);

        // obj2 : Object segments with manifest object with "X-Object-Manifest", and no digest headers
        writeLargeObject(containerName, "obj2", true, true, true, false);

        // obj3 : Object segments with manifest object missing "X-Object-Manifest" header (OpenStack Swift behaviour)
        writeLargeObject(containerName, "obj3", true, true, false, true);

        // obj4 : Object segments with manifest object existing "X-Object-Manifest" header (Ceph behaviour)
        writeLargeObject(containerName, "obj4", true, true, true, true);

        // obj5 : Already migrated object
        writeLargeObject(containerName, "obj5", false, true, true, true);
    }

    @After
    public void cleanup() {
        VitamConfiguration.setTenants(initialTenants);
        VitamConfiguration.setEnvironmentName(initialEnvName);
    }

    @Test
    public void migrate_swift_v3_analysis_only() throws Exception {
        // Given prepared data in container

        // When
        SwiftMigrationService swiftMigrationService = new SwiftMigrationService(swiftKeystoneFactoryV3);
        boolean migrationStarted = swiftMigrationService.tryStartMigration(SwiftMigrationMode.MODE_0_ANALYSIS_ONLY);

        for (int i = 0; i < MAX_RETRIES; i++) {
            if (!swiftMigrationService.isMigrationInProgress()) {
                break;
            }
            TimeUnit.SECONDS.sleep(1);
        }
        boolean isMigrationInProgress = swiftMigrationService.isMigrationInProgress();
        boolean hasMigrationSucceeded = swiftMigrationService.hasMigrationSucceeded();

        // Then
        assertThat(migrationStarted).isTrue();
        assertThat(isMigrationInProgress).isFalse();
        assertThat(hasMigrationSucceeded).isTrue();

        Set<String> allObjectsAndSegments = listContainer(vitamSwiftObjectStorageService, containerName, true);

        // obj0 : 1 regular obj
        // obj1 : 10x segments + no manifest
        // obj2..obj5 : 10x segments + 1 manifest
        assertThat(allObjectsAndSegments).hasSize(1 + 10 + 11 * 4);

        // Ensure no segment renamed or deleted
        assertThat(allObjectsAndSegments)
            .doesNotContain("obj1/000001", "obj2/00000002", "obj3/00000009", "obj4/00000010")
            .contains("obj1/1", "obj2/2", "obj3/9", "obj4/10", "obj5/00000010");

        // Ensure no manifest header/metadata updated
        checkLargeObjectHeaders(containerName, "obj2", true, false);
        checkLargeObjectHeaders(containerName, "obj3", false, true);
        checkLargeObjectHeaders(containerName, "obj4", true, true);
        checkLargeObjectHeaders(containerName, "obj5", true, true);
    }

    @Test
    public void migrate_swift_v3_fix_inconsistencies_without_delete() throws Exception {
        // Given prepared data in container

        // When
        SwiftMigrationService swiftMigrationService = new SwiftMigrationService(swiftKeystoneFactoryV3);
        boolean migrationStarted = swiftMigrationService.tryStartMigration(
            SwiftMigrationMode.MODE_1_FIX_INCONSISTENCIES
        );

        for (int i = 0; i < MAX_RETRIES; i++) {
            if (!swiftMigrationService.isMigrationInProgress()) {
                break;
            }
            TimeUnit.SECONDS.sleep(1);
        }
        boolean isMigrationInProgress = swiftMigrationService.isMigrationInProgress();
        boolean hasMigrationSucceeded = swiftMigrationService.hasMigrationSucceeded();

        // Then
        assertThat(migrationStarted).isTrue();
        assertThat(isMigrationInProgress).isFalse();
        assertThat(hasMigrationSucceeded).isTrue();

        Set<String> allObjectsAndSegments = listContainer(vitamSwiftObjectStorageService, containerName, true);

        // obj0 : 1 regular obj
        // obj1 : 10x segments + no manifest
        // obj2..obj5 : 10x segments + 1 manifest
        assertThat(allObjectsAndSegments).hasSize(1 + 10 + 11 * 4);

        // Obj0 : Has valid content
        checkObjectContent(containerName, "obj0");

        // Obj1 have not been deleted, but still have no manifest and have its old segment name format
        assertThatThrownBy(() -> swift.getObject(containerName, "obj1")).isInstanceOf(
            ContentAddressableStorageNotFoundException.class
        );
        assertThat(allObjectsAndSegments)
            .doesNotContain("obj1", "obj1/000001", "obj1/00000010")
            .contains("obj1/1", "obj1/10");

        // obj2..obj5 : Check objects have no old segment names, have proper headers & content
        for (String objectName : List.of("obj2", "obj3", "obj4", "obj5")) {
            checkLargeObjectHeaders(containerName, objectName, true, true);
            checkObjectContent(containerName, objectName);
            assertThat(allObjectsAndSegments)
                .doesNotContain(objectName + "/1", objectName + "/9", objectName + "/10")
                .contains(objectName + "/00000001", objectName + "/00000009", objectName + "/00000010");
        }
    }

    @Test
    public void migrate_swift_v3_full() throws Exception {
        // Given prepared data in container

        // When
        SwiftMigrationService swiftMigrationService = new SwiftMigrationService(swiftKeystoneFactoryV3);
        boolean migrationStarted = swiftMigrationService.tryStartMigration(
            SwiftMigrationMode.MODE_2_FIX_INCONSISTENCIES_AND_PURGE_DELETED
        );

        for (int i = 0; i < MAX_RETRIES; i++) {
            if (!swiftMigrationService.isMigrationInProgress()) {
                break;
            }
            TimeUnit.SECONDS.sleep(1);
        }
        boolean isMigrationInProgress = swiftMigrationService.isMigrationInProgress();
        boolean hasMigrationSucceeded = swiftMigrationService.hasMigrationSucceeded();

        // Then
        assertThat(migrationStarted).isTrue();
        assertThat(isMigrationInProgress).isFalse();
        assertThat(hasMigrationSucceeded).isTrue();

        Set<String> allObjectsAndSegments = listContainer(vitamSwiftObjectStorageService, containerName, true);

        // obj0 : 1 regular obj
        // obj1 : Deleted
        // obj2..obj5 : 10x segments + 1 manifest
        assertThat(allObjectsAndSegments).hasSize(1 + 11 * 4);

        // Obj0 : Has valid content
        checkObjectContent(containerName, "obj0");

        // Obj1 have not been deleted, but still have no manifest and have its old segment name format
        assertThatThrownBy(() -> swift.getObject(containerName, "obj1")).isInstanceOf(
            ContentAddressableStorageNotFoundException.class
        );
        assertThat(allObjectsAndSegments).doesNotContain("obj1", "obj1/1", "obj1/000001", "obj1/10", "obj1/00000010");

        // obj2..obj5 : Check objects have no old segment names, have proper headers & content
        for (String objectName : List.of("obj2", "obj3", "obj4", "obj5")) {
            checkLargeObjectHeaders(containerName, objectName, true, true);
            checkObjectContent(containerName, objectName);
            assertThat(allObjectsAndSegments)
                .doesNotContain(objectName + "/1", objectName + "/9", objectName + "/10")
                .contains(objectName + "/00000001", objectName + "/00000009", objectName + "/00000010");
        }
    }

    private void writeLargeObject(
        String containerName,
        String objectName,
        boolean useOldSegmentNameFormat,
        boolean writeManifestObject,
        boolean hasManifestHeader,
        boolean hasDigestMetadata
    ) throws ContentAddressableStorageException, IOException {
        // 6906 bytes --> 10 segments (9*700+606)
        byte[] data = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("file1.pdf"));

        final int SEGMENT_SIZE = 700;
        for (int i = 1, pos = 0; pos < data.length; i++, pos += SEGMENT_SIZE) {
            byte[] segment = Arrays.copyOfRange(data, pos, Math.min(pos + SEGMENT_SIZE, data.length));
            String segmentName = useOldSegmentNameFormat
                ? getOldObjectSegmentName(objectName, i)
                : getNewObjectSegmentName(objectName, i);
            vitamSwiftObjectStorageService.put(
                containerName,
                segmentName,
                Payloads.create(new ByteArrayInputStream(segment))
            );
        }

        if (writeManifestObject) {
            ObjectPutOptions manifestOptions = ObjectPutOptions.create();
            if (hasManifestHeader) {
                manifestOptions.getOptions().put(X_OBJECT_MANIFEST, containerName + "/" + objectName + "/");
            }
            if (hasDigestMetadata) {
                manifestOptions.getOptions().put(X_OBJECT_META_DIGEST, DATA_DIGEST);
                manifestOptions.getOptions().put(X_OBJECT_META_DIGEST_TYPE, SHA512_DIGEST_TYPE);
            }
            vitamSwiftObjectStorageService.put(
                containerName,
                objectName,
                Payloads.create(new NullInputStream(0)),
                manifestOptions
            );
        }
    }

    private String getOldObjectSegmentName(String objectName, int index) {
        return objectName + "/" + index;
    }

    private String getNewObjectSegmentName(String objectName, int index) {
        return objectName + "/" + String.format("%08d", index);
    }

    private Set<String> listContainer(
        VitamSwiftObjectStorageService vitamSwiftObjectStorageService,
        String containerName,
        boolean includeSegments
    ) throws ContentAddressableStorageException {
        Set<String> results = new HashSet<>();
        String nextMarker = null;
        do {
            ObjectListOptions objectListOptions = ObjectListOptions.create().limit(1000);

            if (nextMarker != null) {
                objectListOptions.marker(nextMarker);
            }

            List<? extends SwiftObject> swiftObjects = vitamSwiftObjectStorageService.list(
                containerName,
                objectListOptions,
                Collections.emptyMap()
            );

            if (swiftObjects.isEmpty()) {
                break;
            }

            for (SwiftObject obj : swiftObjects) {
                if (obj.getName().contains("/") && !includeSegments) {
                    // Skip segment
                    continue;
                }
                results.add(obj.getName());
            }
            nextMarker = swiftObjects.get(swiftObjects.size() - 1).getName();
        } while (nextMarker != null);
        return results;
    }

    private void writeRegularObject(String containerName, String objectName)
        throws ContentAddressableStorageException, IOException {
        ObjectPutOptions obj0Options = ObjectPutOptions.create();
        obj0Options.getOptions().put(X_OBJECT_META_DIGEST, DATA_DIGEST);
        obj0Options.getOptions().put(X_OBJECT_META_DIGEST_TYPE, SHA512_DIGEST_TYPE);
        vitamSwiftObjectStorageService.put(
            containerName,
            objectName,
            Payloads.create(PropertiesUtils.getResourceAsStream("file1.pdf")),
            obj0Options
        );
    }

    private void checkLargeObjectHeaders(
        String containerName,
        String objectName,
        boolean hasManifestHeader,
        boolean hasDigestMetadata
    ) throws ContentAddressableStorageException {
        Map<String, String> headers = vitamSwiftObjectStorageService.getMetadata(
            containerName,
            objectName,
            Collections.emptyMap()
        );
        if (hasManifestHeader) {
            assertThat(headers.get(X_OBJECT_MANIFEST)).isEqualTo(containerName + "/" + objectName + "/");
        } else {
            assertThat(headers).doesNotContainKey(X_OBJECT_MANIFEST);
        }
        if (hasDigestMetadata) {
            assertThat(headers.get(X_OBJECT_META_DIGEST)).isEqualTo(DATA_DIGEST);
            assertThat(headers.get(X_OBJECT_META_DIGEST_TYPE)).isEqualTo(SHA512_DIGEST_TYPE);
        } else {
            assertThat(headers).doesNotContainKeys(X_OBJECT_META_DIGEST, X_OBJECT_META_DIGEST_TYPE);
        }
    }

    private void checkObjectContent(String containerName, String objectName)
        throws ContentAddressableStorageException, IOException {
        byte[] expectedContent = IOUtils.toByteArray(PropertiesUtils.getResourceAsStream("file1.pdf"));

        ObjectContent object = swift.getObject(containerName, objectName);
        assertThat(object.getSize()).withFailMessage("Check size for " + objectName).isEqualTo(expectedContent.length);
        assertThat(object.getInputStream())
            .withFailMessage("Check content for " + objectName)
            .hasSameContentAs(new ByteArrayInputStream(expectedContent));
    }
}
