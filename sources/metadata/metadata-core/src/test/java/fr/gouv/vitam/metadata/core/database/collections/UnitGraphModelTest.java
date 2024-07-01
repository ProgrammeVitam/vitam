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
package fr.gouv.vitam.metadata.core.database.collections;

import com.google.common.collect.Sets;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class UnitGraphModelTest {

    @Test
    public void should_initialize_graph() {
        // Given / When
        UnitGraphModel unitGraphModel = new UnitGraphModel("1", "sp");

        // Then
        assertThat(unitGraphModel.minDepth()).isEqualTo(1);
        assertThat(unitGraphModel.maxDepth()).isEqualTo(1);
        assertThat(unitGraphModel.originatingAgencies()).containsExactly("sp");
        assertThat(unitGraphModel.originatingAgency()).isEqualTo("sp");
        assertThat(unitGraphModel.unitDepths()).isEmpty();
        assertThat(unitGraphModel.graph()).isEmpty();
    }

    @Test
    public void should_add_parent_to_empty_au() {
        // Given
        UnitGraphModel unitGraphModel = new UnitGraphModel("1", "sp");
        UnitGraphModel parentModel = new UnitGraphModel("2", "sp_parent");

        // When
        unitGraphModel.addParent(parentModel);

        // Then
        assertThat(unitGraphModel.minDepth()).isEqualTo(1);
        assertThat(unitGraphModel.maxDepth()).isEqualTo(2);
        assertThat(unitGraphModel.parents()).containsExactly("2");
        assertThat(unitGraphModel.ancestors()).containsExactly("2");
        assertThat(unitGraphModel.originatingAgencies()).containsExactlyInAnyOrder("sp", "sp_parent");
        assertThat(unitGraphModel.originatingAgency()).isEqualTo("sp");
        assertThat(unitGraphModel.unitDepths()).contains(entry("1", Sets.newHashSet("2")));
        assertThat(unitGraphModel.graph()).contains("1/2");
    }

    @Test
    public void should_add_parent_to_au_with_already_one_parent() {
        // Given
        UnitGraphModel unitGraphModel = new UnitGraphModel("1", "sp");
        UnitGraphModel parentModel = new UnitGraphModel("2", "sp_parent");
        UnitGraphModel grandParentModel = new UnitGraphModel("3", "sp_parent");
        parentModel.addParent(grandParentModel);

        // When
        unitGraphModel.addParent(parentModel);

        // Then
        assertThat(unitGraphModel.minDepth()).isEqualTo(1);
        assertThat(unitGraphModel.maxDepth()).isEqualTo(3);
        assertThat(unitGraphModel.parents()).containsExactly("2");
        assertThat(unitGraphModel.ancestors()).containsExactly("2", "3");
        assertThat(unitGraphModel.originatingAgencies()).containsExactlyInAnyOrder("sp", "sp_parent");
        assertThat(unitGraphModel.originatingAgency()).isEqualTo("sp");
        assertThat(unitGraphModel.unitDepths()).containsExactly(
            entry("1", Sets.newHashSet("2")),
            entry("2", Sets.newHashSet("3"))
        );
        assertThat(unitGraphModel.graph()).containsExactly("1/2", "2/3");
    }
}
