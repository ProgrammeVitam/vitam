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

import fr.gouv.vitam.common.PropertiesUtils;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class ElasticsearchCustomSearchManagerTest {

    @Test
    public void testIndexAliasResolverWithNoCustomSearchConfig() throws Exception {
        // Given
        AdminManagementConfiguration config;
        try (
            final InputStream yamlIS = PropertiesUtils.getConfigAsStream(
                "./functional_administration_test_config_without_custom_search.yml"
            )
        ) {
            config = PropertiesUtils.readYaml(yamlIS, AdminManagementConfiguration.class);
        }
        List<Integer> tenants = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 22);
        ElasticsearchCustomSearchManager indexManager = new ElasticsearchCustomSearchManager(config, tenants);

        // When
        List<String> unit0IndexSettings = indexManager.getCustomSearchTypes("Unit", 0, "Title");
        List<String> unit10IndexSettings = indexManager.getCustomSearchTypes("Unit", 10, "Title");
        List<String> unit22IndexSettings = indexManager.getCustomSearchTypes("Unit", 22, "Title");
        List<String> objectGroup0IndexSettings = indexManager.getCustomSearchTypes("ObjectGroup", 0, "Title");
        List<String> objectGroup10IndexSettings = indexManager.getCustomSearchTypes("ObjectGroup", 10, "Title");
        List<String> objectGroup22IndexSettings = indexManager.getCustomSearchTypes("ObjectGroup", 22, "Title");

        // Then
        assertThat(unit0IndexSettings).isNull();
        assertThat(unit10IndexSettings).isNull();
        assertThat(unit22IndexSettings).isNull();
        assertThat(objectGroup0IndexSettings).isNull();
        assertThat(objectGroup10IndexSettings).isNull();
        assertThat(objectGroup22IndexSettings).isNull();
    }

    @Test
    public void testIndexAliasResolverWithDefaultOnlyConfig() throws Exception {
        // Given
        AdminManagementConfiguration config;
        try (
            final InputStream yamlIS = PropertiesUtils.getConfigAsStream(
                "./functional_administration_test_customsearch_config_defaults_only.yml"
            )
        ) {
            config = PropertiesUtils.readYaml(yamlIS, AdminManagementConfiguration.class);
        }
        for (int i = 0; i < 3; i++) {
            List<Integer> tenants = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 22);
            Random random = new Random();
            int randomIndex = random.nextInt(tenants.size());
            int randomTenant = tenants.get(randomIndex);

            ElasticsearchCustomSearchManager indexManager = new ElasticsearchCustomSearchManager(config, tenants);

            // When
            List<String> unitTitleIndexSettings = indexManager.getCustomSearchTypes("Unit", randomTenant, "Title");

            List<String> unitDescriptionIndexSettings = indexManager.getCustomSearchTypes(
                "Unit",
                randomTenant,
                "Description"
            );

            List<String> unitUnknownIndexSettings = indexManager.getCustomSearchTypes("Unit", randomTenant, "Unknown");

            List<String> objectGroupIndexSettings = indexManager.getCustomSearchTypes(
                "ObjectGroup",
                randomTenant,
                "Metadata"
            );
            List<String> otherObjectGroupIndexSettings = indexManager.getCustomSearchTypes(
                "ObjectGroup",
                randomTenant,
                "SomePath"
            );

            List<String> unknownObjectGroupIndexSettings = indexManager.getCustomSearchTypes(
                "ObjectGroup",
                randomTenant,
                "Unknown"
            );

            // Then
            assertThat(unitTitleIndexSettings).hasSize(2);
            assertThat(unitDescriptionIndexSettings).hasSize(1);
            assertThat(unitUnknownIndexSettings).isNull();

            assertThat(unitTitleIndexSettings).hasSameElementsAs(List.of("Strict", "Minimal"));
            assertThat(unitDescriptionIndexSettings).hasSameElementsAs(List.of("Minimal"));

            assertThat(objectGroupIndexSettings).hasSize(2);
            assertThat(otherObjectGroupIndexSettings).hasSize(1);

            assertThat(objectGroupIndexSettings).hasSameElementsAs(List.of("Strict", "EnglishMinimal"));
            assertThat(otherObjectGroupIndexSettings).hasSameElementsAs(List.of("SomeSearch"));
            assertThat(unknownObjectGroupIndexSettings).isNull();
        }
    }

    @Test
    public void testIndexAliasResolverWithDedicatedTenantsConfig() throws Exception {
        // Given
        AdminManagementConfiguration config;
        try (
            final InputStream yamlIS = PropertiesUtils.getConfigAsStream("./functional_administration_test_config.yml")
        ) {
            config = PropertiesUtils.readYaml(yamlIS, AdminManagementConfiguration.class);
        }
        List<Integer> tenants = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 22);
        ElasticsearchCustomSearchManager indexManager = new ElasticsearchCustomSearchManager(config, tenants);

        // When
        List<String> unit0IndexSettings = indexManager.getCustomSearchTypes("Unit", 0, "Title");
        List<String> unit4IndexSettings = indexManager.getCustomSearchTypes("Unit", 4, "Title");

        List<String> unitUnknownIndexSettings = indexManager.getCustomSearchTypes("Unit", 0, "Unknown");

        // Then
        assertThat(unit0IndexSettings).hasSize(2);
        assertThat(unit4IndexSettings).hasSize(1);
        assertThat(unitUnknownIndexSettings).isNull();

        assertThat(unit0IndexSettings).hasSameElementsAs(List.of("Strict", "Minimal"));
        assertThat(unit4IndexSettings).hasSameElementsAs(List.of("SomeSearch"));
    }

    @Test
    public void testIndexAliasResolverWithGroupedTenantsConfig() throws Exception {
        // Given
        AdminManagementConfiguration config;
        try (
            final InputStream yamlIS = PropertiesUtils.getConfigAsStream("./functional_administration_test_config.yml")
        ) {
            config = PropertiesUtils.readYaml(yamlIS, AdminManagementConfiguration.class);
        }
        List<Integer> tenants = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 22);
        ElasticsearchCustomSearchManager indexManager = new ElasticsearchCustomSearchManager(config, tenants);

        // When
        List<String> unit0IndexSettings = indexManager.getCustomSearchTypes("Unit", 7, "Title");
        List<String> unit4IndexSettings = indexManager.getCustomSearchTypes("Unit", 7, "Description");

        List<String> unitUnknownIndexSettings = indexManager.getCustomSearchTypes("Unit", 7, "Unknown");

        // Then
        assertThat(unit0IndexSettings).hasSize(3);
        assertThat(unit4IndexSettings).hasSize(1);
        assertThat(unitUnknownIndexSettings).isNull();

        assertThat(unit0IndexSettings).hasSameElementsAs(List.of("GroupSearch1", "GroupSearch2", "GroupSearch3"));
        assertThat(unit4IndexSettings).hasSameElementsAs(List.of("SomeSearchGrp"));
    }
}
