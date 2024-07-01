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

package fr.gouv.vitam.metadata.core.config;

import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAliasResolver;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexSettings;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchUtil;
import fr.gouv.vitam.common.exception.VitamFatalRuntimeException;
import fr.gouv.vitam.common.model.config.CollectionConfiguration;
import fr.gouv.vitam.common.model.config.CollectionConfigurationUtils;
import fr.gouv.vitam.common.model.config.TenantRange;
import fr.gouv.vitam.common.model.config.TenantRangeParser;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.mapping.MappingLoader;
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

public class ElasticsearchMetadataIndexManager {

    private final List<Integer> tenantIds;
    private final Map<Integer, String> tenantToTenantGroupMap = new HashMap<>();
    private final ListValuedMap<String, Integer> tenantGroupToTenantMap = new ArrayListValuedHashMap<>();
    private final Map<Integer, ElasticsearchIndexSettings> unitIndexSettingsMap = new HashMap<>();
    private final Map<Integer, ElasticsearchIndexSettings> objectGroupIndexSettingsMap = new HashMap<>();

    public ElasticsearchMetadataIndexManager(
        MetaDataConfiguration configuration,
        List<Integer> tenantIds,
        MappingLoader mappingLoader
    ) {
        this.tenantIds = tenantIds;

        Map<Integer, CollectionConfiguration> customTenantUnitConfiguration = new HashMap<>();
        Map<String, CollectionConfiguration> groupedTenantUnitConfiguration = new HashMap<>();

        Map<Integer, CollectionConfiguration> customTenantObjectGroupConfiguration = new HashMap<>();
        Map<String, CollectionConfiguration> groupedTenantObjectGroupConfiguration = new HashMap<>();

        CollectionConfiguration defaultUnitConfiguration = configuration
            .getIndexationConfiguration()
            .getDefaultCollectionConfiguration()
            .getUnit();

        CollectionConfiguration defaultObjectGroupConfiguration = configuration
            .getIndexationConfiguration()
            .getDefaultCollectionConfiguration()
            .getObjectgroup();

        if (configuration.getIndexationConfiguration().getDedicatedTenantConfiguration() != null) {
            for (DedicatedTenantConfiguration dedicatedTenantConfiguration : configuration
                .getIndexationConfiguration()
                .getDedicatedTenantConfiguration()) {
                List<TenantRange> tenantRanges = TenantRangeParser.parseTenantRanges(
                    dedicatedTenantConfiguration.getTenants()
                );

                CollectionConfiguration unitConfiguration = CollectionConfigurationUtils.merge(
                    dedicatedTenantConfiguration.getUnit(),
                    defaultUnitConfiguration
                );
                CollectionConfiguration objectGroupConfiguration = CollectionConfigurationUtils.merge(
                    dedicatedTenantConfiguration.getObjectgroup(),
                    defaultObjectGroupConfiguration
                );

                for (TenantRange tenantRange : tenantRanges) {
                    for (int tenantId : tenantIds) {
                        if (tenantRange.isInRange(tenantId)) {
                            customTenantUnitConfiguration.put(tenantId, unitConfiguration);
                            customTenantObjectGroupConfiguration.put(tenantId, objectGroupConfiguration);
                        }
                    }
                }
            }
        }

        if (configuration.getIndexationConfiguration().getGroupedTenantConfiguration() != null) {
            for (GroupedTenantConfiguration groupedTenantConfiguration : configuration
                .getIndexationConfiguration()
                .getGroupedTenantConfiguration()) {
                List<TenantRange> tenantRanges = TenantRangeParser.parseTenantRanges(
                    groupedTenantConfiguration.getTenants()
                );

                CollectionConfiguration unitConfiguration = CollectionConfigurationUtils.merge(
                    groupedTenantConfiguration.getUnit(),
                    defaultUnitConfiguration
                );
                groupedTenantUnitConfiguration.put(groupedTenantConfiguration.getName(), unitConfiguration);

                CollectionConfiguration objectGroupConfiguration = CollectionConfigurationUtils.merge(
                    groupedTenantConfiguration.getObjectgroup(),
                    defaultObjectGroupConfiguration
                );
                groupedTenantObjectGroupConfiguration.put(
                    groupedTenantConfiguration.getName(),
                    objectGroupConfiguration
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

        Supplier<String> unitMappingLoader = getEsMappingLoader(mappingLoader, MetadataCollections.UNIT);
        Supplier<String> objectGroupMappingLoader = getEsMappingLoader(mappingLoader, MetadataCollections.OBJECTGROUP);

        tenantIds
            .stream()
            .filter(not(tenantToTenantGroupMap::containsKey))
            .forEach(tenantId -> {
                CollectionConfiguration unitConfiguration = customTenantUnitConfiguration.getOrDefault(
                    tenantId,
                    defaultUnitConfiguration
                );
                ElasticsearchIndexSettings unitElasticsearchIndexSettings = new ElasticsearchIndexSettings(
                    unitConfiguration.getNumberOfShards(),
                    unitConfiguration.getNumberOfReplicas(),
                    unitMappingLoader
                );
                this.unitIndexSettingsMap.put(tenantId, unitElasticsearchIndexSettings);

                CollectionConfiguration objectGroupConfiguration = customTenantObjectGroupConfiguration.getOrDefault(
                    tenantId,
                    defaultObjectGroupConfiguration
                );
                ElasticsearchIndexSettings objectGroupElasticsearchIndexSettings = new ElasticsearchIndexSettings(
                    objectGroupConfiguration.getNumberOfShards(),
                    objectGroupConfiguration.getNumberOfReplicas(),
                    objectGroupMappingLoader
                );
                this.objectGroupIndexSettingsMap.put(tenantId, objectGroupElasticsearchIndexSettings);
            });

        tenantGroupToTenantMap
            .keySet()
            .forEach(tenantGroupName -> {
                CollectionConfiguration unitConfiguration = groupedTenantUnitConfiguration.get(tenantGroupName);
                ElasticsearchIndexSettings unitElasticsearchIndexSettings = new ElasticsearchIndexSettings(
                    unitConfiguration.getNumberOfShards(),
                    unitConfiguration.getNumberOfReplicas(),
                    unitMappingLoader
                );

                CollectionConfiguration objectGroupConfiguration = groupedTenantObjectGroupConfiguration.get(
                    tenantGroupName
                );
                ElasticsearchIndexSettings objectGroupElasticsearchIndexSettings = new ElasticsearchIndexSettings(
                    objectGroupConfiguration.getNumberOfShards(),
                    objectGroupConfiguration.getNumberOfReplicas(),
                    objectGroupMappingLoader
                );

                for (Integer tenantId : tenantGroupToTenantMap.get(tenantGroupName)) {
                    this.unitIndexSettingsMap.put(tenantId, unitElasticsearchIndexSettings);
                    this.objectGroupIndexSettingsMap.put(tenantId, objectGroupElasticsearchIndexSettings);
                }
            });
    }

    private Supplier<String> getEsMappingLoader(MappingLoader mappingLoader, MetadataCollections collection) {
        return () -> {
            try {
                return ElasticsearchUtil.transferJsonToMapping(mappingLoader.loadMapping(collection.name()));
            } catch (IOException e) {
                throw new VitamFatalRuntimeException(
                    "Could not load mapping for collection " + MetadataCollections.UNIT.getName(),
                    e
                );
            }
        };
    }

    public ElasticsearchIndexAliasResolver getElasticsearchIndexAliasResolver(MetadataCollections collection) {
        switch (collection) {
            case UNIT:
            case OBJECTGROUP:
                break;
            default:
                throw new IllegalStateException("Unknown collection " + collection);
        }
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

    public ElasticsearchIndexSettings getElasticsearchIndexSettings(MetadataCollections collection, int tenantId) {
        switch (collection) {
            case UNIT:
                return this.unitIndexSettingsMap.get(tenantId);
            case OBJECTGROUP:
                return this.objectGroupIndexSettingsMap.get(tenantId);
            default:
                throw new IllegalStateException("Unknown collection " + collection);
        }
    }

    public Collection<Integer> getDedicatedTenants() {
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

    public boolean isGroupedTenant(Integer tenantId) {
        return tenantToTenantGroupMap.containsKey(tenantId);
    }

    public String getTenantGroup(int tenantId) {
        if (!this.tenantToTenantGroupMap.containsKey(tenantId)) {
            throw new IllegalStateException("Tenant " + tenantId + " does not belong to a tenant group");
        }
        return tenantToTenantGroupMap.get(tenantId);
    }
}
