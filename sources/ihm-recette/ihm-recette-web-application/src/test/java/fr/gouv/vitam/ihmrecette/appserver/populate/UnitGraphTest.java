package fr.gouv.vitam.ihmrecette.appserver.populate;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.BDDMockito.given;

public class UnitGraphTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MetadataRepository metadataRepository;

    @InjectMocks
    private UnitGraph unitGraph;

    @Test
    public void should_compute_graph_with_one_root_parent() {
        // Given
        PopulateModel populateModel = new PopulateModel();
        populateModel.setTenant(1);
        populateModel.setSp("vitam");
        populateModel.setRootId("1234");
        populateModel.setWithGots(false);
        UnitModel rootUnit = new UnitModel(2, "default");
        rootUnit.getSps().add("saphir");
        given(metadataRepository.findUnitById(populateModel.getRootId())).willReturn(Optional.of(rootUnit));

        // When
        UnitGotModel unitGotModel = unitGraph.createGraph(0, populateModel);
        UnitModel unitModel = unitGotModel.getUnit();

        // Then
        assertNotNull(unitModel);
        assertThat(unitModel.getId()).isNotNull();
        assertThat(unitModel.getTenant()).isEqualTo(1);
        assertThat(unitModel.getSp()).isEqualTo(populateModel.getSp());
        assertThat(unitModel.getSps()).contains(populateModel.getSp(), "saphir");
        assertThat(unitModel.getUp()).contains(populateModel.getRootId());
        assertThat(unitModel.getUs()).contains(populateModel.getRootId());
        // and 
        assertNull(unitGotModel.getGot());
    }

    @Test
    public void should_compute_graph_with_one_parent() {
        // Given
        PopulateModel populateModel = new PopulateModel();
        populateModel.setTenant(1);
        populateModel.setSp("vitam");
        populateModel.setRootId("1234");
        populateModel.setWithGots(true);
        UnitModel rootUnit = new UnitModel(2, "default");
        rootUnit.getUp().add("123");
        rootUnit.getUs().add("123");
        rootUnit.getUds().put("123", 1);
        given(metadataRepository.findUnitById(populateModel.getRootId())).willReturn(Optional.of(rootUnit));

        // When
        UnitGotModel unitGotModel = unitGraph.createGraph(0, populateModel);
        UnitModel unitModel = unitGotModel.getUnit();
        ObjectGroupModel gotModel = unitGotModel.getGot();

        // Then
        assertNotNull(unitModel);
        assertThat(unitModel.getTenant()).isEqualTo(1);
        assertThat(unitModel.getSp()).isEqualTo(populateModel.getSp());
        assertThat(unitModel.getSps()).contains(populateModel.getSp());
        assertThat(unitModel.getUp()).contains(populateModel.getRootId());
        assertThat(unitModel.getUs()).contains(populateModel.getRootId(), "123");
        assertThat(unitModel.getUds()).containsEntry(populateModel.getRootId(), 1).containsEntry("123", 2);
        // and
        assertNotNull(gotModel);
        assertThat(gotModel.getTenant()).isEqualTo(1);
        assertThat(gotModel.getSp()).isEqualTo(populateModel.getSp());
        assertThat(gotModel.getSps()).contains(populateModel.getSp());
        assertThat(gotModel.getUp()).contains(unitModel.getId());
    }

    @Test
    public void should_fail_if_root_id_does_not_exist() {
        // Given

        PopulateModel populateModel = new PopulateModel();
        populateModel.setTenant(1);
        populateModel.setSp("vitam");
        populateModel.setRootId("1234");
        populateModel.setWithGots(false);
        given(metadataRepository.findUnitById(populateModel.getRootId())).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> unitGraph.createGraph(0, populateModel))
                .hasMessageContaining("rootId not present in database: 1234");
    }

    @Test
    public void should_generate_operation_id() {
        // Given
        PopulateModel populateModel = new PopulateModel();
        populateModel.setTenant(1);
        populateModel.setSp("vitam");
        populateModel.setWithGots(true);

        // When
        UnitGotModel unitGotModel = unitGraph.createGraph(1, populateModel);

        // Then
        assertThat(unitGotModel.getUnit().getOperationIds()).hasSize(1);
        assertThat(unitGotModel.getUnit().getOperationOriginId()).isNotNull();
        assertThat(unitGotModel.getGot().getOperationOriginId()).isNotNull();
        assertThat(unitGotModel.getGot().getOperationIds()).hasSize(1);
    }

}
