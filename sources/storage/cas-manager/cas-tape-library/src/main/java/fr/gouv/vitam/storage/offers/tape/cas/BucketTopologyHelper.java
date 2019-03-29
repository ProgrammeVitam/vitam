/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.storage.offers.tape.cas;

import com.google.common.collect.ImmutableMap;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryTopologyConfiguration;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.utils.ContainerUtils;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.gouv.vitam.storage.engine.common.utils.ContainerUtils.parseDataCategoryFromContainerName;
import static fr.gouv.vitam.storage.engine.common.utils.ContainerUtils.parseTenantFromContainerName;

public class BucketTopologyHelper {

    public static final String DEFAULT = "default";
    private final Map<Pair<Integer, DataCategory>, String> containerToFileBucketMap;
    private final Map<String, String> fileBucketToBucketMap;

    public BucketTopologyHelper(TapeLibraryTopologyConfiguration configuration) {

        Map<String, List<String>> fileBuckets = validateFileBuckets(configuration);
        Map<String, List<Integer>> buckets = validateBuckets(configuration);

        // Load map
        Map<DataCategory, String> dataCategoryToFileBucketMap =
            Arrays.stream(DataCategory.values())
                .collect(
                    Collectors.toMap(
                        dataCategory -> dataCategory,
                        dataCategory -> fileBuckets.entrySet().stream()
                            .filter(entry -> entry.getValue().contains(dataCategory.getFolder()))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse(DEFAULT)
                    )
                );


        Map<Pair<Integer, DataCategory>, String> containerToFileBucketMap = new HashMap<>();
        Map<String, String> fileBucketToBucketMap = new HashMap<>();

        for (Map.Entry<String, List<Integer>> bucketEntry : buckets.entrySet()) {
            String bucketId = bucketEntry.getKey();
            for (Integer tenant : bucketEntry.getValue()) {


                for (DataCategory dataCategory : DataCategory.values()) {
                    String fileBucketId = bucketId + "-" + dataCategoryToFileBucketMap.get(dataCategory);

                    containerToFileBucketMap.put(new ImmutablePair<>(tenant, dataCategory), fileBucketId);
                    fileBucketToBucketMap.put(fileBucketId, bucketId);
                }
            }
        }
        this.containerToFileBucketMap = MapUtils.unmodifiableMap(containerToFileBucketMap);
        this.fileBucketToBucketMap = MapUtils.unmodifiableMap(fileBucketToBucketMap);
    }

    private Map<String, List<String>> validateFileBuckets(TapeLibraryTopologyConfiguration configuration)
        throws VitamRuntimeException {

        if (configuration == null) {
            throw new VitamRuntimeException("Invalid conf. Missing bucket topology configuration");
        }

        /*
         * Validate file buckets :
         *   - Named file buckets with distinct & non empty data categories
         *   - Default empty file bucket
         */
        Map<String, List<String>> fileBuckets = configuration.getFileBuckets();

        // Default file bucket configuration
        if (fileBuckets == null) {
            fileBuckets = ImmutableMap.of(
                "metadata", Arrays.asList(DataCategory.UNIT.getFolder(), DataCategory.OBJECTGROUP.getFolder()),
                "objects", Collections.singletonList(DataCategory.OBJECT.getFolder()),
                DEFAULT, Collections.emptyList()
            );
        }

        if (fileBuckets.isEmpty()) {
            throw new VitamRuntimeException("Invalid conf. File buckets must not be null");
        }

        if (fileBuckets.values().contains(null)) {
            throw new VitamRuntimeException("Null file bucket");
        }

        if (hasDuplicates(fileBuckets.values().stream().flatMap(Collection::stream))) {
            throw new VitamRuntimeException("Duplicates found in file bucket configuration");
        }

        if (!fileBuckets.containsKey(DEFAULT) || !fileBuckets.get(DEFAULT).isEmpty()) {
            throw new VitamRuntimeException("Expecting default file bucket with empty set");
        }

        for (Map.Entry<String, List<String>> entry : fileBuckets.entrySet()) {

            boolean isDefault = DEFAULT.equals(entry.getKey());
            if (!isDefault && entry.getValue().isEmpty()) {
                throw new VitamRuntimeException("Expected non empty file bucket configuration " + entry.getKey());
            }
        }
        return fileBuckets;
    }

    private Map<String, List<Integer>> validateBuckets(TapeLibraryTopologyConfiguration configuration)
        throws VitamRuntimeException {

        /*
         * Validate bucket :
         *   - buckets must be distinct sets & non empty
         *   - all tenants should map to a bucket
         */
        Map<String, List<Integer>> buckets = configuration.getBuckets();
        if (buckets == null) {
            throw new VitamRuntimeException("Invalid conf. Missing buckets configuration");
        }

        if (buckets.values().contains(null)) {
            throw new VitamRuntimeException("Null file bucket");
        }

        if (hasDuplicates(buckets.values().stream().flatMap(Collection::stream))) {
            throw new VitamRuntimeException("Duplicates found in file bucket configuration");
        }

        Set<Integer> fileBucketTenants =
            buckets.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        Set<Integer> vitamTenants = new HashSet<>(VitamConfiguration.getTenants());

        SetUtils.SetView<Integer> missingTenants = SetUtils.difference(vitamTenants, fileBucketTenants);
        if (!missingTenants.isEmpty()) {
            throw new VitamRuntimeException("Missing tenants " + missingTenants);
        }

        SetUtils.SetView<Integer> unknownTenants = SetUtils.difference(fileBucketTenants, vitamTenants);
        if (!unknownTenants.isEmpty()) {
            throw new VitamRuntimeException("Unknown tenants " + unknownTenants);
        }

        return buckets;
    }

    public String getFileBucketFromContainerName(String containerName) {
        int tenant = parseTenantFromContainerName(containerName);
        DataCategory dataCategory = parseDataCategoryFromContainerName(containerName);
        return this.containerToFileBucketMap.get(ImmutablePair.of(tenant, dataCategory));
    }

    public String getBucketFromFileBucket(String fileBucket) {
        return this.fileBucketToBucketMap.get(fileBucket);
    }

    public Set<String> listFileBuckets() {
        return new HashSet<>(this.containerToFileBucketMap.values());
    }

    public Set<String> listContainerNames(String fileBucketId) {
        return this.containerToFileBucketMap.entrySet().stream()
            .filter(entry -> entry.getValue().equals(fileBucketId))
            .map(entry -> ContainerUtils.buildContainerName(entry.getKey().getRight(), entry.getKey().getLeft().toString()))
            .collect(Collectors.toSet());
    }

    private static <T> boolean hasDuplicates(Stream<T> stream) {
        final Set<T> set = new HashSet<>();
        return !stream.allMatch(set::add);
    }
}
