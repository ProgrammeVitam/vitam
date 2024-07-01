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

package fr.gouv.vitam.storage.offers.migration;

import com.google.common.base.Stopwatch;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectContent;
import fr.gouv.vitam.common.storage.swift.VitamSwiftObjectStorageService;
import fr.gouv.vitam.common.stream.ExactSizeInputStream;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.utils.ContainerUtils;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.StringUtils;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.storage.object.SwiftObject;
import org.openstack4j.model.storage.object.options.ObjectListOptions;
import org.openstack4j.model.storage.object.options.ObjectPutOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static fr.gouv.vitam.common.storage.swift.Swift.X_OBJECT_MANIFEST;
import static fr.gouv.vitam.common.storage.swift.Swift.X_OBJECT_META_DIGEST;
import static fr.gouv.vitam.common.storage.swift.Swift.X_OBJECT_META_DIGEST_TYPE;

/**
 * Handles multiple inconsistencies in large object handling in previous Swift V2 & V3 implementations.
 * - Very large objects (> 36 GB) segments naming might cause invalid digests : "{objectName}/1", "{objectName}/2"..., "{objectName}/10" should be renaming to "{objectName}/00000001", "{objectName}/00000002"..., "{objectName}/00000010" in order to ensure lexical sorting consistency
 * - Large objects (> 4 GB) manifest does not have the appropriate X-Object-Manifest header. Such manifest will be considered an empty object.
 * - Eliminated large object (> 4 GB) still have segments not deleted.
 *
 * This migration service ensures :
 * - Orphan object segments without a parent manifest are deleted
 * - Object segments are properly left-padded with '0's (copy new segment from old segment, check digests & delete old segment)
 * - Manifest object is properly written (size, digest and appropriate X-Object-Manifest/X-Object-Meta-Digest/X-Object-Meta-Digest-Type headers)
 */
public class SwiftMigrationService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SwiftMigrationService.class);

    private final AtomicBoolean isRunning = new AtomicBoolean();
    private final AtomicBoolean migrationSucceeded = new AtomicBoolean();

    private final Supplier<OSClient> osClient;

    public SwiftMigrationService(Supplier<OSClient> osClient) {
        this.osClient = osClient;
    }

    public boolean tryStartMigration(SwiftMigrationMode swiftMigrationMode) {
        boolean lockAcquired = isRunning.compareAndSet(false, true);
        if (!lockAcquired) {
            // A migration is already running
            return false;
        }
        migrationSucceeded.set(false);

        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> {
                try {
                    LOGGER.warn("Starting swift offer migration : " + swiftMigrationMode.toString());
                    migrateOffer(swiftMigrationMode);
                } catch (Exception e) {
                    LOGGER.error("A fatal error occurred during swift offer migration", e);
                } finally {
                    isRunning.set(false);
                    LOGGER.info("Swift offer migration finished");
                }
            });

        return true;
    }

    public boolean isMigrationInProgress() {
        return isRunning.get();
    }

    public boolean hasMigrationSucceeded() {
        return migrationSucceeded.get();
    }

    private void migrateOffer(SwiftMigrationMode swiftMigrationMode)
        throws ContentAddressableStorageException, IOException {
        VitamSwiftObjectStorageService vitamSwiftObjectStorageService = new VitamSwiftObjectStorageService(
            this.osClient
        );

        for (Integer tenant : VitamConfiguration.getTenants()) {
            for (DataCategory dataCategory : DataCategory.values()) {
                String containerName = ContainerUtils.buildContainerName(dataCategory, tenant.toString());
                migrateContainer(swiftMigrationMode, vitamSwiftObjectStorageService, containerName);
            }
        }
        migrationSucceeded.set(true);
    }

    private void migrateContainer(
        SwiftMigrationMode swiftMigrationMode,
        VitamSwiftObjectStorageService vitamSwiftObjectStorageService,
        String containerName
    ) throws ContentAddressableStorageException, IOException {
        LOGGER.warn("Processing swift migration for container " + containerName + ": (" + swiftMigrationMode + ")");
        Stopwatch timer = Stopwatch.createStarted();
        try {
            // Ensure container exists
            if (!isExistingContainer(containerName)) {
                LOGGER.info("Container " + containerName + " not exists.");
                return;
            }

            // List objects to migrate
            MultiValuedMap<String, String> largeObjectChunksByObjectName = listLargeObjects(
                vitamSwiftObjectStorageService,
                containerName
            );

            // Migrate large objects
            for (String objectName : largeObjectChunksByObjectName.keySet()) {
                processLargeObjet(
                    swiftMigrationMode,
                    vitamSwiftObjectStorageService,
                    containerName,
                    objectName,
                    largeObjectChunksByObjectName.get(objectName)
                );
            }
        } finally {
            LOGGER.warn(
                "Done migrating swift container " +
                containerName +
                " in " +
                timer.elapsed(TimeUnit.SECONDS) +
                " seconds."
            );
        }
    }

    private boolean isExistingContainer(String containerName) {
        // Is this the best way to do that ?
        Map<String, String> metadata = osClient.get().objectStorage().containers().getMetadata(containerName);
        // more than 2 metadata then container exists (again, is this the best way ?)
        return metadata.size() > 2;
    }

    private MultiValuedMap<String, String> listLargeObjects(
        VitamSwiftObjectStorageService vitamSwiftObjectStorageService,
        String containerName
    ) throws ContentAddressableStorageException {
        LOGGER.warn("Listing objects for container " + containerName + "...");
        Stopwatch timer = Stopwatch.createStarted();
        int listedObjects = 0;
        try {
            // List large object segments in container (name pattern : {objectName}/{segmentIndex})
            MultiValuedMap<String, String> largeObjectChunksByObjectName = new ArrayListValuedHashMap<>();

            String nextMarker = null;
            do {
                ObjectListOptions objectListOptions = ObjectListOptions.create()
                    .limit(ContentAddressableStorageAbstract.LISTING_MAX_RESULTS);

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
                listedObjects += swiftObjects.size();
                LOGGER.info("Found objets & object segments: " + listedObjects);

                for (SwiftObject swiftObject : swiftObjects) {
                    if (swiftObject.getName().matches("^.*/\\d+$")) {
                        String objectName = StringUtils.substringBeforeLast(swiftObject.getName(), "/");
                        largeObjectChunksByObjectName.put(objectName, swiftObject.getName());
                        LOGGER.info("Segment to process " + containerName + "/" + swiftObject.getName());
                    }
                }

                nextMarker = swiftObjects.get(swiftObjects.size() - 1).getName();
            } while (nextMarker != null);

            return largeObjectChunksByObjectName;
        } finally {
            LOGGER.warn(
                "Listing objects took " +
                timer.elapsed(TimeUnit.SECONDS) +
                " seconds. Total objects found in container " +
                containerName +
                ": " +
                listedObjects
            );
        }
    }

    private void processLargeObjet(
        SwiftMigrationMode swiftMigrationMode,
        VitamSwiftObjectStorageService vitamSwiftObjectStorageService,
        String containerName,
        String objectName,
        Collection<String> segments
    ) throws ContentAddressableStorageException, IOException {
        LOGGER.info("Processing large object " + containerName + "/" + objectName);

        // Check manifest object existence
        Optional<SwiftObject> manifestObject = vitamSwiftObjectStorageService.getObjectInformation(
            containerName,
            objectName,
            Collections.emptyMap()
        );
        if (manifestObject.isEmpty()) {
            deleteOrphanObjectSegments(
                swiftMigrationMode,
                vitamSwiftObjectStorageService,
                containerName,
                objectName,
                segments
            );
            return;
        }

        // Rename large object segments from format (ex. obj/12 to obj/00000012) to fix segment ordering
        SortedSet<String> newSegmentNames = migrateObjectSegmentNames(
            swiftMigrationMode,
            vitamSwiftObjectStorageService,
            containerName,
            objectName,
            segments
        );

        // Migrate large object
        Map<String, String> objectMetadata = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        objectMetadata.putAll(manifestObject.get().getMetadata());
        migrateLargeObjectManifest(
            swiftMigrationMode,
            vitamSwiftObjectStorageService,
            containerName,
            objectName,
            newSegmentNames,
            objectMetadata
        );
    }

    private void deleteOrphanObjectSegments(
        SwiftMigrationMode swiftMigrationMode,
        VitamSwiftObjectStorageService vitamSwiftObjectStorageService,
        String containerName,
        String objectName,
        Collection<String> segments
    ) throws ContentAddressableStorageException {
        if (!swiftMigrationMode.isDeleteOrphanSegments()) {
            LOGGER.warn(
                "INCONSISTENCY FOUND : Orphan large object segments " +
                segments +
                " without parent object manifest: " +
                containerName +
                "/" +
                objectName +
                ". Eliminated object? Incomplete write?" +
                " Run migration script with delete mode to prune container."
            );
            return;
        }

        LOGGER.warn(
            "DELETING orphan large object segments (elimination? incomplete write?): " +
            containerName +
            "/" +
            objectName +
            ". Cleaning up its segments " +
            segments
        );
        for (String segmentName : segments) {
            vitamSwiftObjectStorageService.deleteFullObject(containerName, segmentName, null, Collections.emptyMap());
        }
    }

    private SortedSet<String> migrateObjectSegmentNames(
        SwiftMigrationMode swiftMigrationMode,
        VitamSwiftObjectStorageService vitamSwiftObjectStorageService,
        String containerName,
        String objectName,
        Collection<String> segments
    ) throws IOException, ContentAddressableStorageException {
        Map<String, String> oldSegmentNamesToMigrate = new HashMap<>();
        SortedSet<String> targetSegmentNames = new TreeSet<>();

        for (String segmentName : segments) {
            if (isOldSegmentNameFormat(segmentName)) {
                String targetSegmentName = oldSegmentNameToNewSegmentName(segmentName);
                oldSegmentNamesToMigrate.put(segmentName, targetSegmentName);
                targetSegmentNames.add(targetSegmentName);
            } else {
                targetSegmentNames.add(segmentName);
            }
        }

        if (oldSegmentNamesToMigrate.isEmpty()) {
            LOGGER.info("All segment names already migrated for object " + containerName + "/" + objectName);
            return targetSegmentNames;
        }

        if (!swiftMigrationMode.isFixInconsistencies()) {
            LOGGER.warn(
                "INCONSISTENCY FOUND : Object " +
                containerName +
                "/" +
                objectName +
                " has old segment names " +
                oldSegmentNamesToMigrate.keySet() +
                ". Run migration script with fix inconsistencies mode to prune container."
            );
            return targetSegmentNames;
        }

        for (String oldSegmentName : oldSegmentNamesToMigrate.keySet()) {
            String newSegmentName = oldSegmentNamesToMigrate.get(oldSegmentName);
            renameSegment(vitamSwiftObjectStorageService, containerName, oldSegmentName, newSegmentName);
        }
        return targetSegmentNames;
    }

    private boolean isOldSegmentNameFormat(String segmentName) {
        String segmentId = StringUtils.substringAfterLast(segmentName, "/");
        return segmentId.length() < 8;
    }

    private String oldSegmentNameToNewSegmentName(String segmentName) {
        String segmentPrefix = StringUtils.substringBeforeLast(segmentName, "/");
        String segmentId = StringUtils.substringAfterLast(segmentName, "/");
        return segmentPrefix + "/" + StringUtils.leftPad(segmentId, 8, '0');
    }

    private void renameSegment(
        VitamSwiftObjectStorageService vitamSwiftObjectStorageService,
        String containerName,
        String oldSegmentName,
        String newSegmentName
    ) throws ContentAddressableStorageException, IOException {
        LOGGER.warn(
            "Renaming segment " + containerName + "/" + oldSegmentName + " to " + containerName + "/" + newSegmentName
        );

        ObjectPutOptions objectPutOptions = ObjectPutOptions.create();
        objectPutOptions.getOptions().put("X-Copy-From", containerName + "/" + oldSegmentName);

        vitamSwiftObjectStorageService.put(
            containerName,
            newSegmentName,
            Payloads.create(new NullInputStream(0L)),
            objectPutOptions
        );

        String oldSegmentDigest = computeDigest(vitamSwiftObjectStorageService, containerName, oldSegmentName);
        String newSegmentDigest = computeDigest(vitamSwiftObjectStorageService, containerName, newSegmentName);

        if (!oldSegmentDigest.equals(newSegmentDigest)) {
            throw new IllegalStateException(
                String.format(
                    "Copied segment %s/%s digest (%s) does not match old segment %s/%s digest (%s)",
                    containerName,
                    newSegmentName,
                    newSegmentDigest,
                    containerName,
                    oldSegmentName,
                    oldSegmentDigest
                )
            );
        }

        vitamSwiftObjectStorageService.deleteFullObject(containerName, oldSegmentName, null, Collections.emptyMap());
    }

    private String computeDigest(
        VitamSwiftObjectStorageService vitamSwiftObjectStorageService,
        String containerName,
        String segmentName
    ) throws ContentAddressableStorageException, IOException {
        ObjectContent segmentContent = vitamSwiftObjectStorageService.download(
            containerName,
            segmentName,
            Collections.emptyMap()
        );
        try (
            InputStream inputStream = segmentContent.getInputStream();
            InputStream exactSizeInputStream = new ExactSizeInputStream(inputStream, segmentContent.getSize())
        ) {
            Digest segmentDigest = new Digest(VitamConfiguration.getDefaultDigestType());
            segmentDigest.update(exactSizeInputStream);
            return segmentDigest.digestHex();
        }
    }

    private void migrateLargeObjectManifest(
        SwiftMigrationMode swiftMigrationMode,
        VitamSwiftObjectStorageService vitamSwiftObjectStorageService,
        String containerName,
        String objectName,
        SortedSet<String> sortedSegmentNames,
        Map<String, String> objectMetadata
    ) throws ContentAddressableStorageException, IOException {
        if (
            objectMetadata.containsKey(X_OBJECT_MANIFEST) &&
            objectMetadata.containsKey(X_OBJECT_META_DIGEST) &&
            objectMetadata.containsKey(X_OBJECT_META_DIGEST_TYPE)
        ) {
            LOGGER.info("No migration needed for object manifest " + containerName + "/" + objectName);
            return;
        }

        if (!swiftMigrationMode.isFixInconsistencies()) {
            LOGGER.warn(
                "INCONSISTENCY FOUND : Object " +
                containerName +
                "/" +
                objectName +
                " has missing metadata." +
                " Run migration script with fix inconsistencies mode enabled to set object metadata."
            );
            return;
        }

        String globalDigest = computeGlobalDigestFromSegments(
            vitamSwiftObjectStorageService,
            containerName,
            sortedSegmentNames
        );

        // If initial object digest was set, check computed global digest
        checkInitialObjectDigestMetadata(containerName, objectName, objectMetadata, globalDigest);

        // Update manifest metadata
        updateLargeObjectManifest(vitamSwiftObjectStorageService, containerName, objectName, globalDigest);

        // Double check object digest & metadata
        doubleCheckObjectManifest(vitamSwiftObjectStorageService, containerName, objectName, globalDigest);

        LOGGER.warn("Object " + containerName + "/" + objectName + " migrated successfully. Digest: " + globalDigest);
    }

    private String computeGlobalDigestFromSegments(
        VitamSwiftObjectStorageService vitamSwiftObjectStorageService,
        String containerName,
        SortedSet<String> sortedSegmentNames
    ) throws ContentAddressableStorageException, IOException {
        Digest globalDigest = new Digest(VitamConfiguration.getDefaultDigestType());

        for (String segmentName : sortedSegmentNames) {
            ObjectContent segmentContent = vitamSwiftObjectStorageService.download(
                containerName,
                segmentName,
                Collections.emptyMap()
            );

            try (
                InputStream inputStream = segmentContent.getInputStream();
                InputStream exactSizeInputStream = new ExactSizeInputStream(inputStream, segmentContent.getSize())
            ) {
                globalDigest.update(exactSizeInputStream);
            }
        }

        return globalDigest.digestHex();
    }

    private void checkInitialObjectDigestMetadata(
        String containerName,
        String objectName,
        Map<String, String> objectMetadata,
        String globalDigest
    ) {
        String declaredDigestType = objectMetadata.get(X_OBJECT_META_DIGEST_TYPE);
        if (
            declaredDigestType != null &&
            !declaredDigestType.equals(VitamConfiguration.getDefaultDigestType().getName())
        ) {
            throw new IllegalStateException(
                "Illegal object " +
                containerName +
                "/" +
                objectName +
                " digest type. Expected: " +
                VitamConfiguration.getDefaultDigestType().getName() +
                ", actual: " +
                declaredDigestType
            );
        }

        String declaredObjectDigest = objectMetadata.get(X_OBJECT_META_DIGEST);
        if (declaredObjectDigest != null && !declaredObjectDigest.equals(globalDigest)) {
            throw new IllegalStateException(
                "Illegal object " +
                containerName +
                "/" +
                objectName +
                " digest. Expected=" +
                globalDigest +
                ", actual= " +
                declaredObjectDigest
            );
        }
    }

    private void updateLargeObjectManifest(
        VitamSwiftObjectStorageService vitamSwiftObjectStorageService,
        String containerName,
        String objectName,
        String digest
    ) throws ContentAddressableStorageException {
        String largeObjectPrefix = containerName + "/" + objectName + "/";

        ObjectPutOptions options = ObjectPutOptions.create();
        options.getOptions().put(X_OBJECT_MANIFEST, largeObjectPrefix);
        options.getOptions().put(X_OBJECT_META_DIGEST, digest);
        options.getOptions().put(X_OBJECT_META_DIGEST_TYPE, VitamConfiguration.getDefaultDigestType().getName());

        vitamSwiftObjectStorageService.put(
            containerName,
            objectName,
            Payloads.create(new NullInputStream(0L)),
            options
        );
    }

    private void doubleCheckObjectManifest(
        VitamSwiftObjectStorageService vitamSwiftObjectStorageService,
        String containerName,
        String objectName,
        String expectedDigest
    ) throws ContentAddressableStorageException, IOException {
        // Check digest
        String actualGlobalDigest = computeDigest(vitamSwiftObjectStorageService, containerName, objectName);
        if (!actualGlobalDigest.equals(expectedDigest)) {
            throw new IllegalStateException(
                "Invalid digest value for object " +
                containerName +
                "/" +
                objectName +
                ". Expected: " +
                expectedDigest +
                ", actual: " +
                actualGlobalDigest
            );
        }

        // Check metadata
        Optional<SwiftObject> updatedManifestObject = vitamSwiftObjectStorageService.getObjectInformation(
            containerName,
            objectName,
            Collections.emptyMap()
        );
        if (updatedManifestObject.isEmpty()) {
            throw new IllegalStateException("Could not read manifest " + containerName + "/" + objectName);
        }

        Map<String, String> updatedObjectMetadata = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        updatedObjectMetadata.putAll(updatedManifestObject.get().getMetadata());

        String expectedObjectPrefix = containerName + "/" + objectName + "/";
        String actualObjectPrefix = updatedObjectMetadata.get(X_OBJECT_MANIFEST);
        if (!expectedObjectPrefix.equals(actualObjectPrefix)) {
            throw new IllegalStateException(
                "Manifest header not found for updated manifest " +
                containerName +
                "/" +
                objectName +
                ". Expected: " +
                expectedObjectPrefix +
                ", actual: " +
                actualObjectPrefix
            );
        }

        String updatedDigestType = updatedObjectMetadata.get(X_OBJECT_META_DIGEST_TYPE);
        if (!VitamConfiguration.getDefaultDigestType().getName().equals(updatedDigestType)) {
            throw new IllegalStateException(
                "Invalid updated digest type. Expected: " +
                VitamConfiguration.getDefaultDigestType().getName() +
                ", actual: " +
                updatedDigestType
            );
        }

        String updatedManifestDigestValue = updatedObjectMetadata.get(X_OBJECT_META_DIGEST);
        if (!expectedDigest.equals(updatedManifestDigestValue)) {
            throw new IllegalStateException(
                "Invalid digest value for updated manifest object " +
                containerName +
                "/" +
                objectName +
                ". Expected: " +
                expectedDigest +
                ", actual: " +
                updatedManifestDigestValue
            );
        }
    }
}
