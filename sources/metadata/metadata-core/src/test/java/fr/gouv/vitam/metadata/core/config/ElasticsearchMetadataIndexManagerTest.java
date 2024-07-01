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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAliasResolver;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexSettings;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.mapping.MappingLoader;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class ElasticsearchMetadataIndexManagerTest {

    @Test
    public void testIndexAliasResolverWithDefaultOnlyConfig() throws Exception {
        // Given
        MetaDataConfiguration config;
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream("./metadata_test_config_defaults_only.yml")) {
            config = PropertiesUtils.readYaml(yamlIS, MetaDataConfiguration.class);
        }
        List<Integer> tenants = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 22);
        MappingLoader mappingLoader = mock(MappingLoader.class);
        doAnswer(args -> new ByteArrayInputStream("{}".getBytes())).when(mappingLoader).loadMapping(any());

        ElasticsearchMetadataIndexManager indexManager = new ElasticsearchMetadataIndexManager(
            config,
            tenants,
            mappingLoader
        );

        // When
        ElasticsearchIndexAliasResolver unitIndexAliasResolver = indexManager.getElasticsearchIndexAliasResolver(
            MetadataCollections.UNIT
        );
        ElasticsearchIndexAliasResolver objectGroupIndexAliasResolver = indexManager.getElasticsearchIndexAliasResolver(
            MetadataCollections.OBJECTGROUP
        );

        // Then
        assertThat(unitIndexAliasResolver.resolveIndexName(0).getName()).isEqualTo("unit_0");
        assertThat(unitIndexAliasResolver.resolveIndexName(10).getName()).isEqualTo("unit_10");
        assertThat(unitIndexAliasResolver.resolveIndexName(22).getName()).isEqualTo("unit_22");
        assertThat(objectGroupIndexAliasResolver.resolveIndexName(0).getName()).isEqualTo("objectgroup_0");
        assertThat(objectGroupIndexAliasResolver.resolveIndexName(10).getName()).isEqualTo("objectgroup_10");
        assertThat(objectGroupIndexAliasResolver.resolveIndexName(22).getName()).isEqualTo("objectgroup_22");
    }

    @Test
    public void testIndexAliasResolverWithCustomConfig() throws Exception {
        // Given
        MetaDataConfiguration config;
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream("./metadata_test_config.yml")) {
            config = PropertiesUtils.readYaml(yamlIS, MetaDataConfiguration.class);
        }
        List<Integer> tenants = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 22);
        MappingLoader mappingLoader = mock(MappingLoader.class);
        doAnswer(args -> new ByteArrayInputStream("{}".getBytes())).when(mappingLoader).loadMapping(any());

        ElasticsearchMetadataIndexManager indexManager = new ElasticsearchMetadataIndexManager(
            config,
            tenants,
            mappingLoader
        );

        // When
        ElasticsearchIndexAliasResolver unitIndexAliasResolver = indexManager.getElasticsearchIndexAliasResolver(
            MetadataCollections.UNIT
        );
        ElasticsearchIndexAliasResolver objectGroupIndexAliasResolver = indexManager.getElasticsearchIndexAliasResolver(
            MetadataCollections.OBJECTGROUP
        );

        // Then
        assertThat(unitIndexAliasResolver.resolveIndexName(0).getName()).isEqualTo("unit_0");
        assertThat(unitIndexAliasResolver.resolveIndexName(10).getName()).isEqualTo("unit_10");
        assertThat(unitIndexAliasResolver.resolveIndexName(22).getName()).isEqualTo("unit_grp1");
        assertThat(objectGroupIndexAliasResolver.resolveIndexName(0).getName()).isEqualTo("objectgroup_0");
        assertThat(objectGroupIndexAliasResolver.resolveIndexName(10).getName()).isEqualTo("objectgroup_10");
        assertThat(objectGroupIndexAliasResolver.resolveIndexName(22).getName()).isEqualTo("objectgroup_grp1");
    }

    @Test
    public void testIndexSettingsWithDefaultOnlyConfig() throws Exception {
        // Given
        MetaDataConfiguration config;
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream("./metadata_test_config_defaults_only.yml")) {
            config = PropertiesUtils.readYaml(yamlIS, MetaDataConfiguration.class);
        }
        List<Integer> tenants = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 22);
        MappingLoader mappingLoader = mock(MappingLoader.class);
        doAnswer(args -> new ByteArrayInputStream("{}".getBytes())).when(mappingLoader).loadMapping(any());

        ElasticsearchMetadataIndexManager indexManager = new ElasticsearchMetadataIndexManager(
            config,
            tenants,
            mappingLoader
        );

        // When
        ElasticsearchIndexSettings unit0IndexSettings = indexManager.getElasticsearchIndexSettings(
            MetadataCollections.UNIT,
            0
        );
        ElasticsearchIndexSettings unit10IndexSettings = indexManager.getElasticsearchIndexSettings(
            MetadataCollections.UNIT,
            10
        );
        ElasticsearchIndexSettings unit22IndexSettings = indexManager.getElasticsearchIndexSettings(
            MetadataCollections.UNIT,
            22
        );
        ElasticsearchIndexSettings objectGroup0IndexSettings = indexManager.getElasticsearchIndexSettings(
            MetadataCollections.OBJECTGROUP,
            0
        );
        ElasticsearchIndexSettings objectGroup10IndexSettings = indexManager.getElasticsearchIndexSettings(
            MetadataCollections.OBJECTGROUP,
            10
        );
        ElasticsearchIndexSettings objectGroup22IndexSettings = indexManager.getElasticsearchIndexSettings(
            MetadataCollections.OBJECTGROUP,
            22
        );

        // Then
        assertThat(unit0IndexSettings.getShards()).isEqualTo(3);
        assertThat(unit0IndexSettings.getReplicas()).isEqualTo(10);
        assertThat(unit10IndexSettings.getShards()).isEqualTo(3);
        assertThat(unit10IndexSettings.getReplicas()).isEqualTo(10);
        assertThat(unit22IndexSettings.getShards()).isEqualTo(3);
        assertThat(unit22IndexSettings.getReplicas()).isEqualTo(10);

        assertThat(objectGroup0IndexSettings.getShards()).isEqualTo(3);
        assertThat(objectGroup0IndexSettings.getReplicas()).isEqualTo(11);
        assertThat(objectGroup10IndexSettings.getShards()).isEqualTo(3);
        assertThat(objectGroup10IndexSettings.getReplicas()).isEqualTo(11);
        assertThat(objectGroup22IndexSettings.getShards()).isEqualTo(3);
        assertThat(objectGroup22IndexSettings.getReplicas()).isEqualTo(11);
    }

    @Test
    public void testIndexSettingsWithCustomConfig() throws Exception {
        // Given
        MetaDataConfiguration config;
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream("./metadata_test_config.yml")) {
            config = PropertiesUtils.readYaml(yamlIS, MetaDataConfiguration.class);
        }
        List<Integer> tenants = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 22);
        MappingLoader mappingLoader = mock(MappingLoader.class);
        doAnswer(args -> new ByteArrayInputStream("unit".getBytes())).when(mappingLoader).loadMapping("UNIT");
        doAnswer(args -> new ByteArrayInputStream("og".getBytes())).when(mappingLoader).loadMapping("OBJECTGROUP");

        ElasticsearchMetadataIndexManager indexManager = new ElasticsearchMetadataIndexManager(
            config,
            tenants,
            mappingLoader
        );

        // When
        ElasticsearchIndexSettings unit0IndexSettings = indexManager.getElasticsearchIndexSettings(
            MetadataCollections.UNIT,
            0
        );
        ElasticsearchIndexSettings unit10IndexSettings = indexManager.getElasticsearchIndexSettings(
            MetadataCollections.UNIT,
            10
        );
        ElasticsearchIndexSettings unit22IndexSettings = indexManager.getElasticsearchIndexSettings(
            MetadataCollections.UNIT,
            22
        );
        ElasticsearchIndexSettings objectGroup0IndexSettings = indexManager.getElasticsearchIndexSettings(
            MetadataCollections.OBJECTGROUP,
            0
        );
        ElasticsearchIndexSettings objectGroup10IndexSettings = indexManager.getElasticsearchIndexSettings(
            MetadataCollections.OBJECTGROUP,
            10
        );
        ElasticsearchIndexSettings objectGroup22IndexSettings = indexManager.getElasticsearchIndexSettings(
            MetadataCollections.OBJECTGROUP,
            22
        );

        // Then
        assertThat(unit0IndexSettings.getShards()).isEqualTo(3);
        assertThat(unit0IndexSettings.getReplicas()).isEqualTo(10);
        assertThat(unit10IndexSettings.getShards()).isEqualTo(4);
        assertThat(unit10IndexSettings.getReplicas()).isEqualTo(12);
        assertThat(unit22IndexSettings.getShards()).isEqualTo(5);
        assertThat(unit22IndexSettings.getReplicas()).isEqualTo(14);

        assertThat(objectGroup0IndexSettings.getShards()).isEqualTo(3);
        assertThat(objectGroup0IndexSettings.getReplicas()).isEqualTo(11);
        assertThat(objectGroup10IndexSettings.getShards()).isEqualTo(5);
        assertThat(objectGroup10IndexSettings.getReplicas()).isEqualTo(13);
        assertThat(objectGroup22IndexSettings.getShards()).isEqualTo(6);
        assertThat(objectGroup22IndexSettings.getReplicas()).isEqualTo(15);

        assertThat(unit10IndexSettings.loadMapping()).isEqualTo("unit");
        assertThat(objectGroup22IndexSettings.loadMapping()).isEqualTo("og");
    }
}
