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

package fr.gouv.vitam.functional.administration.common.config;

import fr.gouv.vitam.common.model.config.TenantRange;
import fr.gouv.vitam.common.model.config.TenantRangeParser;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ElasticsearchCustomSearchManager {

    private final ListValuedMap<String, Integer> tenantGroupToTenantMap = new ArrayListValuedHashMap<>();
    private final Map<Pair<Integer, String>, List<String>> unitCustomSearchsTypesByTenantAndFieldMap = new HashMap<>();
    private final Map<Pair<Integer, String>, List<String>> objectGroupCustomSearchsTypesByTenantAndFieldMap =
        new HashMap<>();

    public ElasticsearchCustomSearchManager(AdminManagementConfiguration configuration, List<Integer> tenantIds) {
        if (configuration == null || configuration.getCustomSearchOnFieldsConfiguration() == null) {
            return;
        }

        Optional<CollectionCustomSearchConfiguration> defaultUnitConfiguration = getDefaultUnitConfiguration(
            configuration
        );

        Optional<CollectionCustomSearchConfiguration> defaultObjectGroupConfiguration =
            getDefaultObjectGroupConfiguration(configuration);

        if (defaultUnitConfiguration.isEmpty() && defaultObjectGroupConfiguration.isEmpty()) return;

        handleDedicatedTenantsConfigurations(
            configuration,
            tenantIds,
            defaultUnitConfiguration.orElseGet(null),
            defaultObjectGroupConfiguration.orElseGet(null)
        );

        handleTenantsGroupsConfiguration(
            configuration,
            tenantIds,
            defaultUnitConfiguration.orElseGet(null),
            defaultObjectGroupConfiguration.orElseGet(null)
        );
    }

    private Optional<CollectionCustomSearchConfiguration> getDefaultObjectGroupConfiguration(
        AdminManagementConfiguration configuration
    ) {
        Optional<CollectionCustomSearchConfiguration> defaultObjectGroupConfigurationOpt = Optional.empty();
        if (
            configuration.getCustomSearchOnFieldsConfiguration().getDefaultCustomSearchCollectionConfiguration() != null
        ) {
            defaultObjectGroupConfigurationOpt = Optional.of(
                configuration
                    .getCustomSearchOnFieldsConfiguration()
                    .getDefaultCustomSearchCollectionConfiguration()
                    .getObjectgroupFields()
            );
        }
        return defaultObjectGroupConfigurationOpt;
    }

    private Optional<CollectionCustomSearchConfiguration> getDefaultUnitConfiguration(
        AdminManagementConfiguration configuration
    ) {
        Optional<CollectionCustomSearchConfiguration> defaultUnitConfigurationOpt = Optional.empty();
        if (
            configuration.getCustomSearchOnFieldsConfiguration().getDefaultCustomSearchCollectionConfiguration() != null
        ) {
            defaultUnitConfigurationOpt = Optional.of(
                configuration
                    .getCustomSearchOnFieldsConfiguration()
                    .getDefaultCustomSearchCollectionConfiguration()
                    .getUnitFields()
            );
        }
        return defaultUnitConfigurationOpt;
    }

    private void handleTenantsGroupsConfiguration(
        AdminManagementConfiguration configuration,
        List<Integer> tenantIds,
        CollectionCustomSearchConfiguration defaultUnitConfiguration,
        CollectionCustomSearchConfiguration defaultObjectGroupConfiguration
    ) {
        Map<String, CollectionCustomSearchConfiguration> groupedTenantUnitConfiguration = new HashMap<>();
        Map<String, CollectionCustomSearchConfiguration> groupedTenantObjectGroupConfiguration = new HashMap<>();
        if (
            CollectionUtils.isNotEmpty(
                configuration.getCustomSearchOnFieldsConfiguration().getGroupedTenantConfiguration()
            )
        ) {
            for (GroupedTenantCustomSearchConfiguration groupedTenantConfiguration : configuration
                .getCustomSearchOnFieldsConfiguration()
                .getGroupedTenantConfiguration()) {
                List<TenantRange> tenantRanges = TenantRangeParser.parseTenantRanges(
                    groupedTenantConfiguration.getTenants()
                );

                CollectionCustomSearchConfiguration unitConfiguration = CollectionSearchConfigurationUtils.merge(
                    groupedTenantConfiguration.getUnitFields(),
                    defaultUnitConfiguration
                );
                groupedTenantUnitConfiguration.put(groupedTenantConfiguration.getName(), unitConfiguration);

                CollectionCustomSearchConfiguration objectGroupConfiguration = CollectionSearchConfigurationUtils.merge(
                    groupedTenantConfiguration.getObjectgroupFields(),
                    defaultObjectGroupConfiguration
                );
                groupedTenantObjectGroupConfiguration.put(
                    groupedTenantConfiguration.getName(),
                    objectGroupConfiguration
                );

                for (TenantRange tenantRange : tenantRanges) {
                    for (int tenantId : tenantIds) {
                        if (tenantRange.isInRange(tenantId)) {
                            tenantGroupToTenantMap.get(groupedTenantConfiguration.getName()).add(tenantId);
                        }
                    }
                }
            }
        }
        tenantGroupToTenantMap
            .keySet()
            .forEach(tenantGroupName -> {
                CollectionCustomSearchConfiguration unitConfiguration = groupedTenantUnitConfiguration.get(
                    tenantGroupName
                );

                CollectionCustomSearchConfiguration objectGroupConfiguration =
                    groupedTenantObjectGroupConfiguration.get(tenantGroupName);

                for (Integer tenantId : tenantGroupToTenantMap.get(tenantGroupName)) {
                    if (CollectionUtils.isNotEmpty(unitConfiguration.getCollectionSearchConfigurations())) {
                        for (CollectionSearchConfiguration customSearchConfiguration : unitConfiguration.getCollectionSearchConfigurations()) {
                            this.unitCustomSearchsTypesByTenantAndFieldMap.put(
                                    Pair.of(tenantId, customSearchConfiguration.getFieldPath()),
                                    customSearchConfiguration.getTypes()
                                );
                        }
                    }

                    if (CollectionUtils.isNotEmpty(objectGroupConfiguration.getCollectionSearchConfigurations())) {
                        for (CollectionSearchConfiguration customSearchConfiguration : objectGroupConfiguration.getCollectionSearchConfigurations()) {
                            this.objectGroupCustomSearchsTypesByTenantAndFieldMap.put(
                                    Pair.of(tenantId, customSearchConfiguration.getFieldPath()),
                                    customSearchConfiguration.getTypes()
                                );
                        }
                    }
                }
            });
    }

    private void handleDedicatedTenantsConfigurations(
        AdminManagementConfiguration configuration,
        List<Integer> tenantIds,
        CollectionCustomSearchConfiguration defaultUnitConfiguration,
        CollectionCustomSearchConfiguration defaultObjectGroupConfiguration
    ) {
        Map<Integer, CollectionCustomSearchConfiguration> customTenantUnitConfiguration = new HashMap<>();
        Map<Integer, CollectionCustomSearchConfiguration> customTenantObjectGroupConfiguration = new HashMap<>();

        if (
            CollectionUtils.isNotEmpty(
                configuration.getCustomSearchOnFieldsConfiguration().getDedicatedTenantCustomSearchConfiguration()
            )
        ) {
            for (DedicatedTenantCustomSearchConfiguration dedicatedTenantConfiguration : configuration
                .getCustomSearchOnFieldsConfiguration()
                .getDedicatedTenantCustomSearchConfiguration()) {
                List<TenantRange> tenantRanges = TenantRangeParser.parseTenantRanges(
                    dedicatedTenantConfiguration.getTenants()
                );

                CollectionCustomSearchConfiguration unitConfiguration = CollectionSearchConfigurationUtils.merge(
                    dedicatedTenantConfiguration.getUnitFields(),
                    defaultUnitConfiguration
                );
                CollectionCustomSearchConfiguration objectGroupConfiguration = CollectionSearchConfigurationUtils.merge(
                    dedicatedTenantConfiguration.getObjectgroupFields(),
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
        tenantIds
            .stream()
            .forEach(tenantId -> {
                CollectionCustomSearchConfiguration unitConfiguration = customTenantUnitConfiguration.getOrDefault(
                    tenantId,
                    defaultUnitConfiguration
                );

                if (CollectionUtils.isNotEmpty(unitConfiguration.getCollectionSearchConfigurations())) {
                    for (CollectionSearchConfiguration customSearchConfiguration : unitConfiguration.getCollectionSearchConfigurations()) {
                        this.unitCustomSearchsTypesByTenantAndFieldMap.put(
                                Pair.of(tenantId, customSearchConfiguration.getFieldPath()),
                                customSearchConfiguration.getTypes()
                            );
                    }
                }
                CollectionCustomSearchConfiguration objectGroupConfiguration =
                    customTenantObjectGroupConfiguration.getOrDefault(tenantId, defaultObjectGroupConfiguration);

                if (CollectionUtils.isNotEmpty(objectGroupConfiguration.getCollectionSearchConfigurations())) {
                    for (CollectionSearchConfiguration customSearchConfiguration : objectGroupConfiguration.getCollectionSearchConfigurations()) {
                        this.objectGroupCustomSearchsTypesByTenantAndFieldMap.put(
                                Pair.of(tenantId, customSearchConfiguration.getFieldPath()),
                                customSearchConfiguration.getTypes()
                            );
                    }
                }
            });
    }

    public List<String> getCustomSearchTypes(String collection, int tenantId, String path) {
        switch (collection) {
            case "Unit":
                return this.unitCustomSearchsTypesByTenantAndFieldMap.get(Pair.of(tenantId, path));
            case "ObjectGroup":
                return this.objectGroupCustomSearchsTypesByTenantAndFieldMap.get(Pair.of(tenantId, path));
            default:
                throw new IllegalStateException("Unknown collection " + collection);
        }
    }
}
