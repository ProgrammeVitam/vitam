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
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAliasResolver;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexSettings;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import org.junit.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ElasticsearchFunctionalAdminIndexManagerTest {

    @Test
    public void testIndexAliasResolver() throws Exception {
        // Given
        AdminManagementConfiguration config;
        try (
            final InputStream yamlIS = PropertiesUtils.getConfigAsStream("./functional_administration_test_config.yml")
        ) {
            config = PropertiesUtils.readYaml(yamlIS, AdminManagementConfiguration.class);
        }
        ElasticsearchFunctionalAdminIndexManager indexManager = new ElasticsearchFunctionalAdminIndexManager(config);

        // When
        ElasticsearchIndexAliasResolver accessContractResolver = indexManager.getElasticsearchIndexAliasResolver(
            FunctionalAdminCollections.ACCESS_CONTRACT
        );

        // Then
        assertThat(accessContractResolver.resolveIndexName(null).getName()).isEqualTo("accesscontract");
        assertThat(accessContractResolver.resolveIndexName(0).getName()).isEqualTo("accesscontract");
    }

    @Test
    public void testIndexSettingsWithDefaultOnlyConfig() throws Exception {
        // Given
        AdminManagementConfiguration config;
        try (
            final InputStream yamlIS = PropertiesUtils.getConfigAsStream("./functional_administration_test_config.yml")
        ) {
            config = PropertiesUtils.readYaml(yamlIS, AdminManagementConfiguration.class);
        }

        ElasticsearchFunctionalAdminIndexManager indexManager = new ElasticsearchFunctionalAdminIndexManager(config);

        // When
        ElasticsearchIndexSettings accessContractIndexSettings = indexManager.getElasticsearchIndexSettings(
            FunctionalAdminCollections.ACCESS_CONTRACT
        );
        ElasticsearchIndexSettings ontologyIndexSettings = indexManager.getElasticsearchIndexSettings(
            FunctionalAdminCollections.ONTOLOGY
        );

        // Then
        assertThat(accessContractIndexSettings.getShards()).isEqualTo(3);
        assertThat(accessContractIndexSettings.getReplicas()).isEqualTo(10);
        assertThat(ontologyIndexSettings.getShards()).isEqualTo(4);
        assertThat(ontologyIndexSettings.getReplicas()).isEqualTo(11);
    }
}
