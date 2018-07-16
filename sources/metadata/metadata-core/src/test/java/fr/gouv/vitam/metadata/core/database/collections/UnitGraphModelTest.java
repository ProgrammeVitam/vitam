package fr.gouv.vitam.metadata.core.database.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.google.common.collect.Sets;

import org.junit.Test;

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
            entry("2", Sets.newHashSet("3")));
        assertThat(unitGraphModel.graph()).containsExactly("1/2", "2/3");
    }

}