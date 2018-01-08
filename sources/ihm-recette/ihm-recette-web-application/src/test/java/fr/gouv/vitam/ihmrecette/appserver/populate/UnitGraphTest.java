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
        int tenantId = 1;
        String originatingAgency = "vitam";
        String rootId = "1234";
        UnitModel rootUnit = new UnitModel();
        rootUnit.getSps().add("saphir");
        given(metadataRepository.findUnitById(rootId)).willReturn(Optional.of(rootUnit));

        // When
        UnitGotModel unitGotModel = unitGraph.createGraph(0, rootId, tenantId, originatingAgency, false);
        UnitModel unitModel = unitGotModel.getUnit();

        // Then
        assertNotNull(unitModel);
        assertThat(unitModel.getId()).isNotNull();
        assertThat(unitModel.getTenant()).isEqualTo(1);
        assertThat(unitModel.getSp()).isEqualTo(originatingAgency);
        assertThat(unitModel.getSps()).contains(originatingAgency, "saphir");
        assertThat(unitModel.getUp()).contains(rootId);
        assertThat(unitModel.getUs()).contains(rootId);
        // and 
        assertNull(unitGotModel.getGot());
    }

    @Test
    public void should_compute_graph_with_one_parent() {
        // Given
        int tenantId = 1;
        String originatingAgency = "vitam";
        String rootId = "1234";
        UnitModel rootUnit = new UnitModel();
        rootUnit.getUp().add("123");
        rootUnit.getUs().add("123");
        rootUnit.getUds().put("123", 1);
        given(metadataRepository.findUnitById(rootId)).willReturn(Optional.of(rootUnit));

        // When
        UnitGotModel unitGotModel = unitGraph.createGraph(0, rootId, tenantId, originatingAgency, true);
        UnitModel unitModel = unitGotModel.getUnit();
        ObjectGroupModel gotModel = unitGotModel.getGot();

        // Then
        assertNotNull(unitModel);
        assertThat(unitModel.getTenant()).isEqualTo(1);
        assertThat(unitModel.getSp()).isEqualTo(originatingAgency);
        assertThat(unitModel.getSps()).contains(originatingAgency);
        assertThat(unitModel.getUp()).contains(rootId);
        assertThat(unitModel.getUs()).contains(rootId, "123");
        assertThat(unitModel.getUds()).containsEntry(rootId, 1).containsEntry("123", 2);
        // and
        assertNotNull(gotModel);
        assertThat(gotModel.getTenant()).isEqualTo(1);
        assertThat(gotModel.getSp()).isEqualTo(originatingAgency);
        assertThat(gotModel.getSps()).contains(originatingAgency);
        assertThat(gotModel.getUp()).contains(unitModel.getId());
    }

    @Test
    public void should_fail_if_root_id_does_not_exist() {
        // Given
        int tenantId = 1;
        String originatingAgency = "vitam";
        String rootId = "1234";
        given(metadataRepository.findUnitById(rootId)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> unitGraph.createGraph(0, rootId, tenantId, originatingAgency, false))
                .hasMessageContaining("rootId not present in database: 1234");
    }

}