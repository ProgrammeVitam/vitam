package fr.gouv.vitam.ihmrecette.appserver.populate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

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
        UnitModel unitModel = unitGraph.createGraph(new DescriptiveMetadataModel(), rootId, tenantId, originatingAgency);

        // Then
        assertThat(unitModel.getId()).isNotNull();
        assertThat(unitModel.getTenant()).isEqualTo(1);
        assertThat(unitModel.getSp()).isEqualTo(originatingAgency);
        assertThat(unitModel.getSps()).contains(originatingAgency, "saphir");
        assertThat(unitModel.getUp()).isEqualTo(rootId);
        assertThat(unitModel.getUs()).contains(rootId);
    }

    @Test
    public void should_compute_graph_with_one_parent() {
        // Given
        int tenantId = 1;
        String originatingAgency = "vitam";
        String rootId = "1234";
        UnitModel rootUnit = new UnitModel();
        rootUnit.setUp("123");
        rootUnit.getUs().add("123");
        rootUnit.getUds().put("123", 1);
        given(metadataRepository.findUnitById(rootId)).willReturn(Optional.of(rootUnit));

        // When
        UnitModel unitModel = unitGraph.createGraph(new DescriptiveMetadataModel(), rootId, tenantId, originatingAgency);

        // Then
        assertThat(unitModel.getTenant()).isEqualTo(1);
        assertThat(unitModel.getSp()).isEqualTo(originatingAgency);
        assertThat(unitModel.getSps()).contains(originatingAgency);
        assertThat(unitModel.getUp()).isEqualTo(rootId);
        assertThat(unitModel.getUs()).contains(rootId, "123");
        assertThat(unitModel.getUds()).containsEntry(rootId, 1).containsEntry("123", 2);
    }

    @Test
    public void should_fail_if_root_id_does_not_exist() {
        // Given
        int tenantId = 1;
        String originatingAgency = "vitam";
        String rootId = "1234";
        given(metadataRepository.findUnitById(rootId)).willReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> unitGraph.createGraph(new DescriptiveMetadataModel(), rootId, tenantId, originatingAgency))
            .hasMessageContaining("rootId not present in database: 1234");
    }

}