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
package fr.gouv.vitam.storage.offers.tape.cas;

import com.google.common.collect.ImmutableMap;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.storage.tapelibrary.FileBucketConfiguration;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryBucketConfiguration;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryTopologyConfiguration;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.utils.ContainerUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.gouv.vitam.storage.engine.common.utils.ContainerUtils.parseDataCategoryFromContainerName;
import static fr.gouv.vitam.storage.engine.common.utils.ContainerUtils.parseTenantFromContainerName;

public class BucketTopologyHelper {

    public static final String DEFAULT = "default";
    public static final String BACKUP_BUCKET = "backup";
    public static final String BACKUP_FILE_BUCKET = "backup-db";

    private final Map<Pair<Integer, DataCategory>, String> containerToFileBucketMap;
    private final Map<String, String> fileBucketToBucketMap;
    private final Map<String, Integer> tarBufferingTimeoutInMinutesByBucketId;
    private final Set<String> fileBucketIdsToKeepForeverInCache;

    public BucketTopologyHelper(TapeLibraryTopologyConfiguration configuration) {
        Map<String, FileBucketConfiguration> fileBuckets = getFileBucketsConfigurationOrDefault(configuration);
        validateFileBuckets(fileBuckets);

        validateBucketConfiguration(configuration);

        Map<String, List<Integer>> buckets = configuration
            .getBuckets()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getTenants()));

        // Load map
        Map<DataCategory, String> dataCategoryToFileBucketMap = Arrays.stream(DataCategory.values()).collect(
            Collectors.toMap(
                dataCategory -> dataCategory,
                dataCategory ->
                    fileBuckets
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getValue().getDataCategories().contains(dataCategory.getFolder()))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(DEFAULT)
            )
        );

        Map<Pair<Integer, DataCategory>, String> containerToFileBucketMap = new HashMap<>();
        Map<String, String> fileBucketIdToBucketMap = new HashMap<>();

        for (Map.Entry<String, List<Integer>> bucketEntry : buckets.entrySet()) {
            String bucketId = bucketEntry.getKey();
            for (Integer tenant : bucketEntry.getValue()) {
                for (DataCategory dataCategory : DataCategory.values()) {
                    String fileBucketId = getFileBucketId(bucketId, dataCategoryToFileBucketMap.get(dataCategory));

                    containerToFileBucketMap.put(new ImmutablePair<>(tenant, dataCategory), fileBucketId);
                    fileBucketIdToBucketMap.put(fileBucketId, bucketId);
                }
            }
        }
        this.containerToFileBucketMap = MapUtils.unmodifiableMap(containerToFileBucketMap);
        this.fileBucketToBucketMap = MapUtils.unmodifiableMap(fileBucketIdToBucketMap);

        this.tarBufferingTimeoutInMinutesByBucketId = configuration
            .getBuckets()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getTarBufferingTimeoutInMinutes()));

        // fileBucketIds to keep forever in cache
        Set<String> fileBucketIdToKeepForeverInCache = new HashSet<>();
        for (Map.Entry<String, List<Integer>> bucketEntry : buckets.entrySet()) {
            String bucketId = bucketEntry.getKey();
            for (Map.Entry<String, FileBucketConfiguration> fileBucketEntry : fileBuckets.entrySet()) {
                String fileBucket = fileBucketEntry.getKey();
                String fileBucketId = getFileBucketId(bucketId, fileBucket);

                FileBucketConfiguration fileBucketConfiguration = fileBucketEntry.getValue();
                if (fileBucketConfiguration.getKeepForeverInCache()) {
                    fileBucketIdToKeepForeverInCache.add(fileBucketId);
                }
            }
        }
        this.fileBucketIdsToKeepForeverInCache = SetUtils.unmodifiableSet(fileBucketIdToKeepForeverInCache);
    }

    public static String getFileBucketId(String bucket, String fileBucket) {
        return bucket + "-" + fileBucket;
    }

    private Map<String, FileBucketConfiguration> getFileBucketsConfigurationOrDefault(
        TapeLibraryTopologyConfiguration configuration
    ) {
        if (configuration == null) {
            throw new VitamRuntimeException("Invalid conf. Missing file-bucket topology configuration");
        }

        Map<String, FileBucketConfiguration> fileBuckets = configuration.getFileBuckets();

        // Default file bucket configuration
        if (fileBuckets == null) {
            fileBuckets = ImmutableMap.of(
                "metadata",
                new FileBucketConfiguration()
                    .setDataCategories(
                        Arrays.asList(DataCategory.UNIT.getFolder(), DataCategory.OBJECTGROUP.getFolder())
                    )
                    .setKeepForeverInCache(true),
                "objects",
                new FileBucketConfiguration()
                    .setDataCategories(Collections.singletonList(DataCategory.OBJECT.getFolder()))
                    .setKeepForeverInCache(false),
                DEFAULT,
                new FileBucketConfiguration().setDataCategories(Collections.emptyList()).setKeepForeverInCache(true)
            );
        }
        return fileBuckets;
    }

    private void validateFileBuckets(Map<String, FileBucketConfiguration> fileBuckets) throws VitamRuntimeException {
        /*
         * Validate file buckets :
         *   - Named file buckets with distinct & non-empty data categories
         *   - Check reserved file bucket names (`backup-db` is reserved)
         *   - Default empty file bucket
         *   - keepForeverInCache presence
         */

        if (fileBuckets.isEmpty()) {
            throw new VitamRuntimeException("Invalid conf. File buckets must not be null");
        }

        if (fileBuckets.containsKey(BACKUP_FILE_BUCKET)) {
            throw new VitamRuntimeException("Reserved " + BACKUP_FILE_BUCKET + " file bucket");
        }

        if (fileBuckets.containsValue(null)) {
            throw new VitamRuntimeException("Null file bucket configuration");
        }

        if (fileBuckets.values().stream().map(FileBucketConfiguration::getDataCategories).anyMatch(Objects::isNull)) {
            throw new VitamRuntimeException("Null file bucket data categories");
        }

        if (
            fileBuckets.values().stream().map(FileBucketConfiguration::getKeepForeverInCache).anyMatch(Objects::isNull)
        ) {
            throw new VitamRuntimeException("Missing file bucket cache configuration");
        }

        if (
            fileBuckets
                .values()
                .stream()
                .map(FileBucketConfiguration::getDataCategories)
                .anyMatch(dataCategories -> dataCategories.contains(null))
        ) {
            throw new VitamRuntimeException("Null file bucket data categories");
        }

        Stream<String> fileBucketDataCategories = fileBuckets
            .values()
            .stream()
            .map(FileBucketConfiguration::getDataCategories)
            .flatMap(Collection::stream);
        if (hasDuplicates(fileBucketDataCategories)) {
            throw new VitamRuntimeException("Duplicates found in file bucket configuration");
        }

        if (!fileBuckets.containsKey(DEFAULT) || !fileBuckets.get(DEFAULT).getDataCategories().isEmpty()) {
            throw new VitamRuntimeException("Expecting default file bucket with empty set");
        }

        Set<String> folderNames = Arrays.stream(DataCategory.values())
            .map(DataCategory::getFolder)
            .collect(Collectors.toSet());

        for (Map.Entry<String, FileBucketConfiguration> entry : fileBuckets.entrySet()) {
            boolean isDefault = DEFAULT.equals(entry.getKey());
            if (!isDefault && entry.getValue().getDataCategories().isEmpty()) {
                throw new VitamRuntimeException("Expected non empty file bucket configuration " + entry.getKey());
            }

            for (String folderName : entry.getValue().getDataCategories()) {
                if (!folderNames.contains(folderName)) {
                    throw new VitamRuntimeException("Invalid folder name in bucket configuration '" + folderName + "'");
                }
            }
        }
    }

    private void validateBucketConfiguration(TapeLibraryTopologyConfiguration configuration)
        throws VitamRuntimeException {
        /*
         * Validate bucket :
         *   - buckets must be distinct sets & non-empty
         *   - all tenants should map to a bucket
         */
        Map<String, TapeLibraryBucketConfiguration> buckets = configuration.getBuckets();
        if (buckets == null) {
            throw new VitamRuntimeException("Invalid conf. Missing buckets configuration");
        }

        for (TapeLibraryBucketConfiguration bucketConfiguration : buckets.values()) {
            if (bucketConfiguration == null) {
                throw new VitamRuntimeException("Missing bucket configuration");
            }

            if (CollectionUtils.isEmpty(bucketConfiguration.getTenants())) {
                throw new VitamRuntimeException("Missing bucket tenants");
            }

            if (bucketConfiguration.getTarBufferingTimeoutInMinutes() <= 0) {
                throw new VitamRuntimeException("Tar buffering timeout must be positive");
            }
        }

        if (
            hasDuplicates(
                buckets.values().stream().map(TapeLibraryBucketConfiguration::getTenants).flatMap(Collection::stream)
            )
        ) {
            throw new VitamRuntimeException("Duplicates found in file bucket configuration");
        }

        if (buckets.containsKey(BACKUP_BUCKET)) {
            throw new VitamRuntimeException("Reserved " + BACKUP_BUCKET + " bucket");
        }

        Set<Integer> fileBucketTenants = buckets
            .values()
            .stream()
            .map(TapeLibraryBucketConfiguration::getTenants)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        Set<Integer> vitamTenants = new HashSet<>(VitamConfiguration.getTenants());

        SetUtils.SetView<Integer> missingTenants = SetUtils.difference(vitamTenants, fileBucketTenants);
        if (!missingTenants.isEmpty()) {
            throw new VitamRuntimeException("Missing tenants " + missingTenants);
        }

        SetUtils.SetView<Integer> unknownTenants = SetUtils.difference(fileBucketTenants, vitamTenants);
        if (!unknownTenants.isEmpty()) {
            throw new VitamRuntimeException("Unknown tenants " + unknownTenants);
        }
    }

    public String getFileBucketFromContainerName(String containerName) {
        int tenant = parseTenantFromContainerName(containerName);
        DataCategory dataCategory = parseDataCategoryFromContainerName(containerName);
        return this.containerToFileBucketMap.get(ImmutablePair.of(tenant, dataCategory));
    }

    public String getBucketFromFileBucket(String fileBucket) {
        if (BACKUP_FILE_BUCKET.equals(fileBucket)) {
            return BACKUP_BUCKET;
        }

        return this.fileBucketToBucketMap.get(fileBucket);
    }

    public Set<String> listFileBuckets() {
        return new HashSet<>(this.containerToFileBucketMap.values());
    }

    public boolean isValidFileBucketId(String fileBucketId) {
        return this.fileBucketToBucketMap.containsKey(fileBucketId);
    }

    public Set<String> listContainerNames(String fileBucketId) {
        return this.containerToFileBucketMap.entrySet()
            .stream()
            .filter(entry -> entry.getValue().equals(fileBucketId))
            .map(
                entry ->
                    ContainerUtils.buildContainerName(entry.getKey().getRight(), entry.getKey().getLeft().toString())
            )
            .collect(Collectors.toSet());
    }

    public Integer getTarBufferingTimeoutInMinutes(String bucketId) {
        return tarBufferingTimeoutInMinutesByBucketId.get(bucketId);
    }

    public boolean keepFileBucketIdForeverInCache(String fileBucketId) {
        return this.fileBucketIdsToKeepForeverInCache.contains(fileBucketId);
    }

    private static <T> boolean hasDuplicates(Stream<T> stream) {
        final Set<T> set = new HashSet<>();
        return !stream.allMatch(set::add);
    }
}
