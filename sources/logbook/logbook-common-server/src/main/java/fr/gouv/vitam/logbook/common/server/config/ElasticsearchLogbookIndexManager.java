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

package fr.gouv.vitam.logbook.common.server.config;

import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAliasResolver;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexSettings;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil;
import fr.gouv.vitam.common.exception.VitamFatalRuntimeException;
import fr.gouv.vitam.common.model.config.CollectionConfiguration;
import fr.gouv.vitam.common.model.config.CollectionConfigurationUtils;
import fr.gouv.vitam.common.model.config.TenantRange;
import fr.gouv.vitam.common.model.config.TenantRangeParser;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class ElasticsearchLogbookIndexManager {

    public static final String MAPPING_LOGBOOK_OPERATION_FILE = "/logbook-es-mapping.json";

    private final List<Integer> tenantIds;
    private final Map<Integer, String> tenantToTenantGroupMap = new HashMap<>();
    private final ListValuedMap<String, Integer> tenantGroupToTenantMap = new ArrayListValuedHashMap<>();
    private final Map<Integer, ElasticsearchIndexSettings> logbookOperationIndexSettingsMap = new HashMap<>();

    public ElasticsearchLogbookIndexManager(LogbookConfiguration configuration, List<Integer> tenantIds) {
        this.tenantIds = tenantIds;

        Map<Integer, CollectionConfiguration> customTenantLogbookOperationConfiguration = new HashMap<>();
        Map<String, CollectionConfiguration> groupedTenantLogbookOperationConfiguration = new HashMap<>();

        CollectionConfiguration defaultLogbookOperation = configuration
            .getLogbookTenantIndexation()
            .getDefaultCollectionConfiguration()
            .getLogbookoperation();

        if (configuration.getLogbookTenantIndexation().getDedicatedTenantConfiguration() != null) {
            for (DedicatedTenantConfiguration dedicatedTenantConfiguration : configuration
                .getLogbookTenantIndexation()
                .getDedicatedTenantConfiguration()) {
                List<TenantRange> tenantRanges = TenantRangeParser.parseTenantRanges(
                    dedicatedTenantConfiguration.getTenants()
                );

                CollectionConfiguration finalTenantCollectionConfiguration = CollectionConfigurationUtils.merge(
                    dedicatedTenantConfiguration.getLogbookoperation(),
                    defaultLogbookOperation
                );

                for (TenantRange tenantRange : tenantRanges) {
                    for (int tenantId : tenantIds) {
                        if (tenantRange.isInRange(tenantId)) {
                            customTenantLogbookOperationConfiguration.put(tenantId, finalTenantCollectionConfiguration);
                        }
                    }
                }
            }
        }

        if (configuration.getLogbookTenantIndexation().getGroupedTenantConfiguration() != null) {
            for (GroupedTenantConfiguration groupedTenantConfiguration : configuration
                .getLogbookTenantIndexation()
                .getGroupedTenantConfiguration()) {
                List<TenantRange> tenantRanges = TenantRangeParser.parseTenantRanges(
                    groupedTenantConfiguration.getTenants()
                );

                CollectionConfiguration finalTenantCollectionConfiguration = CollectionConfigurationUtils.merge(
                    groupedTenantConfiguration.getLogbookoperation(),
                    defaultLogbookOperation
                );

                groupedTenantLogbookOperationConfiguration.put(
                    groupedTenantConfiguration.getName(),
                    finalTenantCollectionConfiguration
                );

                for (TenantRange tenantRange : tenantRanges) {
                    for (int tenantId : tenantIds) {
                        if (tenantRange.isInRange(tenantId)) {
                            tenantToTenantGroupMap.put(tenantId, groupedTenantConfiguration.getName());
                            tenantGroupToTenantMap.get(groupedTenantConfiguration.getName()).add(tenantId);
                        }
                    }
                }
            }
        }

        Supplier<String> esMappingLoader = () -> {
            try {
                return ElasticsearchUtil.transferJsonToMapping(
                    LogbookOperation.class.getResourceAsStream(MAPPING_LOGBOOK_OPERATION_FILE)
                );
            } catch (IOException e) {
                throw new VitamFatalRuntimeException("Could not load es mapping file", e);
            }
        };

        tenantIds
            .stream()
            .filter(not(tenantToTenantGroupMap::containsKey))
            .forEach(tenantId -> {
                CollectionConfiguration collectionConfiguration =
                    customTenantLogbookOperationConfiguration.getOrDefault(tenantId, defaultLogbookOperation);
                ElasticsearchIndexSettings elasticsearchIndexSettings = new ElasticsearchIndexSettings(
                    collectionConfiguration.getNumberOfShards(),
                    collectionConfiguration.getNumberOfReplicas(),
                    esMappingLoader
                );
                this.logbookOperationIndexSettingsMap.put(tenantId, elasticsearchIndexSettings);
            });

        tenantGroupToTenantMap
            .keySet()
            .forEach(tenantGroupName -> {
                CollectionConfiguration collectionConfiguration = groupedTenantLogbookOperationConfiguration.get(
                    tenantGroupName
                );
                ElasticsearchIndexSettings elasticsearchIndexSettings = new ElasticsearchIndexSettings(
                    collectionConfiguration.getNumberOfShards(),
                    collectionConfiguration.getNumberOfReplicas(),
                    esMappingLoader
                );
                for (Integer tenantId : tenantGroupToTenantMap.get(tenantGroupName)) {
                    this.logbookOperationIndexSettingsMap.put(tenantId, elasticsearchIndexSettings);
                }
            });
    }

    public ElasticsearchIndexAliasResolver getElasticsearchIndexAliasResolver(LogbookCollections collection) {
        validateCollection(collection);
        return tenantId -> {
            if (this.tenantToTenantGroupMap.containsKey(tenantId)) {
                return ElasticsearchIndexAlias.ofMultiTenantCollection(
                    collection.getName(),
                    this.tenantToTenantGroupMap.get(tenantId)
                );
            } else {
                return ElasticsearchIndexAlias.ofMultiTenantCollection(collection.getName(), tenantId);
            }
        };
    }

    public ElasticsearchIndexSettings getElasticsearchIndexSettings(LogbookCollections collection, int tenantId) {
        validateCollection(collection);
        return this.logbookOperationIndexSettingsMap.get(tenantId);
    }

    public List<Integer> getDedicatedTenants() {
        return tenantIds.stream().filter(not(tenantToTenantGroupMap::containsKey)).collect(Collectors.toList());
    }

    public Collection<String> getTenantGroups() {
        return tenantGroupToTenantMap.keySet();
    }

    public List<Integer> getTenantGroupTenants(String tenantGroupName) {
        if (!this.tenantGroupToTenantMap.containsKey(tenantGroupName)) {
            throw new IllegalStateException("No such tenant group " + tenantGroupName);
        }
        return tenantGroupToTenantMap.get(tenantGroupName);
    }

    public boolean isGroupedTenant(int tenantId) {
        return this.tenantToTenantGroupMap.containsKey(tenantId);
    }

    public String getTenantGroup(int tenantId) {
        if (!this.tenantToTenantGroupMap.containsKey(tenantId)) {
            throw new IllegalStateException("Tenant " + tenantId + " does not belong to a tenant group");
        }
        return tenantToTenantGroupMap.get(tenantId);
    }

    private void validateCollection(LogbookCollections collection) {
        switch (collection) {
            case OPERATION:
                break;
            case LIFECYCLE_UNIT:
            case LIFECYCLE_OBJECTGROUP:
            case LIFECYCLE_UNIT_IN_PROCESS:
            case LIFECYCLE_OBJECTGROUP_IN_PROCESS:
                throw new IllegalStateException("Collection not supported " + collection);
            default:
                throw new IllegalStateException("Unknown collection " + collection);
        }
    }
}
