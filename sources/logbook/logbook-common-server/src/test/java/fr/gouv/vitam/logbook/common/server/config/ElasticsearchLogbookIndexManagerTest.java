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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAliasResolver;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexSettings;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ElasticsearchLogbookIndexManagerTest {

    @Test
    public void testIndexAliasResolverWithDefaultOnlyConfig() throws Exception {

        // Given
        LogbookConfiguration config;
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream("./logbook_test_config_defaults_only.yml")) {
            config = PropertiesUtils.readYaml(yamlIS, LogbookConfiguration.class);
        }
        List<Integer> tenants = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 22);

        ElasticsearchLogbookIndexManager indexManager =
            new ElasticsearchLogbookIndexManager(config, tenants);

        // When
        ElasticsearchIndexAliasResolver operationIndexAliasResolver =
            indexManager.getElasticsearchIndexAliasResolver(LogbookCollections.OPERATION);

        // Then
        assertThat(operationIndexAliasResolver.resolveIndexName(0).getName()).isEqualTo("logbookoperation_0");
        assertThat(operationIndexAliasResolver.resolveIndexName(10).getName()).isEqualTo("logbookoperation_10");
        assertThat(operationIndexAliasResolver.resolveIndexName(22).getName()).isEqualTo("logbookoperation_22");
    }

    @Test
    public void testIndexAliasResolverWithCustomConfig() throws Exception {

        // Given
        LogbookConfiguration config;
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream("./logbook_test_config.yml")) {
            config = PropertiesUtils.readYaml(yamlIS, LogbookConfiguration.class);
        }
        List<Integer> tenants = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 22);

        ElasticsearchLogbookIndexManager indexManager =
            new ElasticsearchLogbookIndexManager(config, tenants);

        // When
        ElasticsearchIndexAliasResolver operationIndexAliasResolver =
            indexManager.getElasticsearchIndexAliasResolver(LogbookCollections.OPERATION);

        // Then
        assertThat(operationIndexAliasResolver.resolveIndexName(0).getName()).isEqualTo("logbookoperation_0");
        assertThat(operationIndexAliasResolver.resolveIndexName(10).getName()).isEqualTo("logbookoperation_10");
        assertThat(operationIndexAliasResolver.resolveIndexName(22).getName()).isEqualTo("logbookoperation_grp1");
    }

    @Test
    public void testIndexSettingsWithDefaultOnlyConfig() throws Exception {

        // Given
        LogbookConfiguration config;
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream("./logbook_test_config_defaults_only.yml")) {
            config = PropertiesUtils.readYaml(yamlIS, LogbookConfiguration.class);
        }
        List<Integer> tenants = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 22);

        ElasticsearchLogbookIndexManager indexManager =
            new ElasticsearchLogbookIndexManager(config, tenants);

        // When
        ElasticsearchIndexSettings logbookOperation0IndexSettings =
            indexManager.getElasticsearchIndexSettings(LogbookCollections.OPERATION, 0);
        ElasticsearchIndexSettings logbookOperation10IndexSettings =
            indexManager.getElasticsearchIndexSettings(LogbookCollections.OPERATION, 10);
        ElasticsearchIndexSettings logbookOperation22IndexSettings =
            indexManager.getElasticsearchIndexSettings(LogbookCollections.OPERATION, 22);

        // Then
        assertThat(logbookOperation0IndexSettings.getShards()).isEqualTo(2);
        assertThat(logbookOperation0IndexSettings.getReplicas()).isEqualTo(10);
        assertThat(logbookOperation10IndexSettings.getShards()).isEqualTo(2);
        assertThat(logbookOperation10IndexSettings.getReplicas()).isEqualTo(10);
        assertThat(logbookOperation22IndexSettings.getShards()).isEqualTo(2);
        assertThat(logbookOperation22IndexSettings.getReplicas()).isEqualTo(10);
    }

    @Test
    public void testIndexSettingsWithCustomConfig() throws Exception {

        // Given
        LogbookConfiguration config;
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream("./logbook_test_config.yml")) {
            config = PropertiesUtils.readYaml(yamlIS, LogbookConfiguration.class);
        }
        List<Integer> tenants = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 22);

        ElasticsearchLogbookIndexManager indexManager =
            new ElasticsearchLogbookIndexManager(config, tenants);

        // When
        ElasticsearchIndexSettings logbookOperation0IndexSettings =
            indexManager.getElasticsearchIndexSettings(LogbookCollections.OPERATION, 0);
        ElasticsearchIndexSettings logbookOperation10IndexSettings =
            indexManager.getElasticsearchIndexSettings(LogbookCollections.OPERATION, 10);
        ElasticsearchIndexSettings logbookOperation22IndexSettings =
            indexManager.getElasticsearchIndexSettings(LogbookCollections.OPERATION, 22);

        // Then
        assertThat(logbookOperation0IndexSettings.getShards()).isEqualTo(2);
        assertThat(logbookOperation0IndexSettings.getReplicas()).isEqualTo(10);
        assertThat(logbookOperation10IndexSettings.getShards()).isEqualTo(3);
        assertThat(logbookOperation10IndexSettings.getReplicas()).isEqualTo(11);
        assertThat(logbookOperation22IndexSettings.getShards()).isEqualTo(4);
        assertThat(logbookOperation22IndexSettings.getReplicas()).isEqualTo(12);

        assertThat(logbookOperation10IndexSettings.loadMapping()).isNotNull();
    }
}
