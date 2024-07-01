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

package fr.gouv.vitam.common.database.server.elasticsearch;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ElasticsearchIndexAliasTest {

    @Test
    public void testAliasOfCrossTenantCollection() {
        ElasticsearchIndexAlias elasticsearchIndexAlias = ElasticsearchIndexAlias.ofCrossTenantCollection(
            "MyCollection"
        );

        assertThat(elasticsearchIndexAlias.getName()).isEqualTo("mycollection");
    }

    @Test
    public void testAliasOfMultiTenantCollectionWithDedicatedTenant() {
        ElasticsearchIndexAlias elasticsearchIndexAlias = ElasticsearchIndexAlias.ofMultiTenantCollection(
            "MyCollection",
            2
        );

        assertThat(elasticsearchIndexAlias.getName()).isEqualTo("mycollection_2");
    }

    @Test
    public void testAliasOfMultiTenantCollectionWithTenantGroup() {
        ElasticsearchIndexAlias elasticsearchIndexAlias = ElasticsearchIndexAlias.ofMultiTenantCollection(
            "MyCollection",
            "grp1"
        );

        assertThat(elasticsearchIndexAlias.getName()).isEqualTo("mycollection_grp1");
    }

    @Test
    public void testAliasOfMultiTenantCollectionWithNullTenantGroup() {
        assertThatThrownBy(() -> ElasticsearchIndexAlias.ofMultiTenantCollection("MyCollection", null)).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    @Test
    public void testAliasOfMultiTenantCollectionWithEmptyTenantGroup() {
        assertThatThrownBy(() -> ElasticsearchIndexAlias.ofMultiTenantCollection("MyCollection", "")).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    @Test
    public void testAliasOfMultiTenantCollectionWithInvalidUpperCaseTenantGroup() {
        assertThatThrownBy(
            () -> ElasticsearchIndexAlias.ofMultiTenantCollection("MyCollection", "Invalid")
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testAliasOfFullIndexName() {
        ElasticsearchIndexAlias elasticsearchIndexAlias = ElasticsearchIndexAlias.ofFullIndexName(
            "mycollection_1_1234"
        );

        assertThat(elasticsearchIndexAlias.getName()).isEqualTo("mycollection_1_1234");
    }

    @Test
    public void testAliasOfFullIndexNameWithNullName() {
        assertThatThrownBy(() -> ElasticsearchIndexAlias.ofFullIndexName(null)).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    @Test
    public void testAliasOfFullIndexNameWithEmptyName() {
        assertThatThrownBy(() -> ElasticsearchIndexAlias.ofFullIndexName("")).isInstanceOf(
            IllegalArgumentException.class
        );
    }
}
