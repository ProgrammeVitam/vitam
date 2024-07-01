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

import fr.gouv.vitam.common.model.config.CollectionConfigurationUtils;
import fr.gouv.vitam.common.model.config.TenantRange;
import fr.gouv.vitam.common.model.config.TenantRangeParser;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LogbookConfigurationValidator {

    public static final String TENANT_GROUP_NAME_PATTERN = "^[a-z][a-z0-9]{1,63}$";

    public static void validateConfiguration(LogbookConfiguration logbookConfiguration) {
        if (logbookConfiguration == null) {
            throw new IllegalStateException("Invalid configuration. Null config");
        }

        validateElasticsearchIndexationConfiguration(logbookConfiguration.getLogbookTenantIndexation());
    }

    private static void validateElasticsearchIndexationConfiguration(
        LogbookIndexationConfiguration indexationConfiguration
    ) {
        if (indexationConfiguration == null) {
            throw new IllegalStateException("Invalid configuration. Missing ES tenant indexation");
        }

        validateDefaultConfig(indexationConfiguration);

        validateTenantGroupNames(indexationConfiguration);

        validateTenantRanges(indexationConfiguration);

        validateCollectionConfiguration(indexationConfiguration);
    }

    private static void validateDefaultConfig(LogbookIndexationConfiguration config) throws IllegalStateException {
        if (config.getDefaultCollectionConfiguration() == null) {
            throw new IllegalStateException("Invalid configuration. Missing default configuration");
        }

        CollectionConfigurationUtils.validate(config.getDefaultCollectionConfiguration().getLogbookoperation(), false);
    }

    private static void validateTenantGroupNames(LogbookIndexationConfiguration indexationConfiguration) {
        if (CollectionUtils.isEmpty(indexationConfiguration.getGroupedTenantConfiguration())) {
            return;
        }

        Set<Object> tenantGroupNames = new HashSet<>();

        indexationConfiguration
            .getGroupedTenantConfiguration()
            .stream()
            .map(GroupedTenantConfiguration::getName)
            .forEach(name -> {
                if (name == null) {
                    throw new IllegalStateException("Invalid configuration. Missing tenant group name");
                }

                if (!name.matches(TENANT_GROUP_NAME_PATTERN)) {
                    throw new IllegalStateException(
                        "Invalid configuration. Tenant group name '" +
                        name +
                        "' does not match regex " +
                        TENANT_GROUP_NAME_PATTERN
                    );
                }

                if (!tenantGroupNames.add(name)) {
                    throw new IllegalStateException("Invalid configuration. Duplicate tenant group name " + name);
                }
            });
    }

    private static void validateTenantRanges(LogbookIndexationConfiguration indexationConfiguration) {
        List<String> tenantRangeStrings = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(indexationConfiguration.getGroupedTenantConfiguration())) {
            indexationConfiguration
                .getGroupedTenantConfiguration()
                .stream()
                .map(GroupedTenantConfiguration::getTenants)
                .forEach(tenantRangeStrings::add);
        }

        if (CollectionUtils.isNotEmpty(indexationConfiguration.getDedicatedTenantConfiguration())) {
            indexationConfiguration
                .getDedicatedTenantConfiguration()
                .stream()
                .map(DedicatedTenantConfiguration::getTenants)
                .forEach(tenantRangeStrings::add);
        }

        if (tenantRangeStrings.contains(null)) {
            throw new IllegalStateException(
                "Invalid configuration. Missing tenants from dedicated tenant or grouped tenant configuration"
            );
        }

        List<TenantRange> tenantRanges = tenantRangeStrings
            .stream()
            .flatMap(tenantRangeString -> TenantRangeParser.parseTenantRanges(tenantRangeString).stream())
            .collect(Collectors.toList());

        // Check tenant range overlapping
        for (int i = 0; i < tenantRanges.size(); i++) {
            for (int j = i + 1; j < tenantRanges.size(); j++) {
                if (TenantRangeParser.doRangesIntersect(tenantRanges.get(i), tenantRanges.get(j))) {
                    throw new IllegalStateException(
                        "Invalid configuration. Overlapping tenant ranges " +
                        tenantRanges.get(i) +
                        " and " +
                        tenantRanges.get(j)
                    );
                }
            }
        }
    }

    private static void validateCollectionConfiguration(LogbookIndexationConfiguration indexationConfiguration) {
        if (CollectionUtils.isNotEmpty(indexationConfiguration.getDedicatedTenantConfiguration())) {
            for (DedicatedTenantConfiguration dedicatedTenantConfiguration : indexationConfiguration.getDedicatedTenantConfiguration()) {
                if (dedicatedTenantConfiguration.getLogbookoperation() != null) {
                    CollectionConfigurationUtils.validate(dedicatedTenantConfiguration.getLogbookoperation(), true);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(indexationConfiguration.getGroupedTenantConfiguration())) {
            for (GroupedTenantConfiguration groupedTenantConfiguration : indexationConfiguration.getGroupedTenantConfiguration()) {
                if (groupedTenantConfiguration.getLogbookoperation() != null) {
                    CollectionConfigurationUtils.validate(groupedTenantConfiguration.getLogbookoperation(), true);
                }
            }
        }
    }
}
