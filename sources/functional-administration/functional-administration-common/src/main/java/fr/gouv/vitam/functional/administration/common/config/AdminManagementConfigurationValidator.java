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

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.config.CollectionConfiguration;
import fr.gouv.vitam.common.model.config.CollectionConfigurationUtils;
import fr.gouv.vitam.common.model.config.DedicatedVirtualPathsTenantConfiguration;
import fr.gouv.vitam.common.model.config.TenantRangeValidator;
import fr.gouv.vitam.common.model.config.VirtualPathsConfiguration;
import fr.gouv.vitam.common.security.SanityChecker;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AdminManagementConfigurationValidator {

    public static void validateConfiguration(AdminManagementConfiguration adminManagementConfiguration) {
        if (adminManagementConfiguration == null) {
            throw new IllegalStateException("Invalid configuration. Null config");
        }

        validateIndexationConfiguration(adminManagementConfiguration.getIndexationConfiguration());
        try {
            validateElasticsearchSettings(adminManagementConfiguration.getElasticsearchConfigurationFile());
        } catch (IOException | InvalidParseOperationException e) {
            throw new IllegalStateException("Invalid configuration.");
        }

        validateCustomSearchOnFieldsConfiguration(adminManagementConfiguration.getCustomSearchOnFieldsConfiguration());

        validateVirtualPathsConfiguration(adminManagementConfiguration.getVirtualPathsConfiguration());
    }

    private static void validateElasticsearchSettings(String elasticsearchConfigurationFilePath)
        throws IOException, InvalidParseOperationException {
        if (elasticsearchConfigurationFilePath == null) {
            throw new IllegalStateException("Invalid path of elasticsearch Configuration File. Null config");
        }

        File elasticsearchConfigurationFile = new File(elasticsearchConfigurationFilePath);
        SanityChecker.checkJsonFile(elasticsearchConfigurationFile);
        if (!elasticsearchConfigurationFile.exists() || !elasticsearchConfigurationFile.isFile()) {
            throw new IllegalStateException("Not found elasticsearch Configuration File in the provided path");
        }
    }

    private static void validateIndexationConfiguration(
        FunctionalAdminIndexationConfiguration indexationConfiguration
    ) {
        CollectionConfigurationUtils.validate(indexationConfiguration.getDefaultConfiguration(), false);

        for (CollectionConfiguration collectionConfiguration : indexationConfiguration
            .getCollectionConfigurationMap()
            .values()) {
            CollectionConfigurationUtils.validate(collectionConfiguration, true);
        }
    }

    private static void validateCustomSearchOnFieldsConfiguration(
        CustomSearchOnFieldsConfiguration customSearchOnFieldsConfiguration
    ) {
        if (customSearchOnFieldsConfiguration == null) return;

        if (customSearchOnFieldsConfiguration.getDefaultCustomSearchCollectionConfiguration() == null) {
            throw new IllegalStateException("Invalid configuration. Missing default configuration");
        }

        //Default config validation
        if (customSearchOnFieldsConfiguration.getDefaultCustomSearchCollectionConfiguration() != null) {
            CollectionSearchConfigurationUtils.validate(
                customSearchOnFieldsConfiguration.getDefaultCustomSearchCollectionConfiguration().getUnitFields(),
                false
            );

            CollectionSearchConfigurationUtils.validate(
                customSearchOnFieldsConfiguration
                    .getDefaultCustomSearchCollectionConfiguration()
                    .getObjectgroupFields(),
                false
            );
        }

        //Dedicated tenants conf validation

        if (
            CollectionUtils.isNotEmpty(customSearchOnFieldsConfiguration.getDedicatedTenantCustomSearchConfiguration())
        ) {
            customSearchOnFieldsConfiguration
                .getDedicatedTenantCustomSearchConfiguration()
                .stream()
                .forEach(dedicatedConf -> {
                    CollectionSearchConfigurationUtils.validate(dedicatedConf.getUnitFields(), true);
                    CollectionSearchConfigurationUtils.validate(dedicatedConf.getObjectgroupFields(), true);
                });
        }

        if (CollectionUtils.isNotEmpty(customSearchOnFieldsConfiguration.getGroupedTenantConfiguration())) {
            customSearchOnFieldsConfiguration
                .getGroupedTenantConfiguration()
                .stream()
                .forEach(groupsConf -> {
                    if (StringUtils.isEmpty(groupsConf.getTenants())) {
                        throw new IllegalStateException("Invalid configuration. tenants list is empty");
                    }
                    if (StringUtils.isEmpty(groupsConf.getName())) {
                        throw new IllegalStateException("Invalid configuration. group name should not be empty");
                    }
                    CollectionSearchConfigurationUtils.validate(groupsConf.getUnitFields(), true);
                    CollectionSearchConfigurationUtils.validate(groupsConf.getObjectgroupFields(), true);
                });
        }
    }

    private static void validateVirtualPathsConfiguration(VirtualPathsConfiguration virtualPathsConfiguration) {
        if (virtualPathsConfiguration == null) {
            return;
        }
        List<String> tenantRangeStrings = new ArrayList<>();
        if (virtualPathsConfiguration.getDefaultConfiguration() == null) {
            throw new IllegalStateException("Invalid configuration. Missing default virtual paths configuration");
        }

        if (CollectionUtils.isNotEmpty(virtualPathsConfiguration.getDedicatedTenantConfiguration())) {
            virtualPathsConfiguration
                .getDedicatedTenantConfiguration()
                .stream()
                .map(DedicatedVirtualPathsTenantConfiguration::getTenants)
                .forEach(tenantRangeStrings::add);
        }

        TenantRangeValidator.validate(tenantRangeStrings);
    }
}
