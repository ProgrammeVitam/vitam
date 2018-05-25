package fr.gouv.vitam.ihmrecette.appserver.populate;

import fr.gouv.vitam.common.LocalDateUtil;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static fr.gouv.vitam.common.graph.GraphUtils.createGraphRelation;
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
        rootUnit.setId("1234");
        rootUnit.getSps().add("saphir");
        rootUnit.getParentOriginatingAgencies().put("saphir", Collections.singletonList("1234"));
        given(metadataRepository.findUnitById(populateModel.getRootId())).willReturn(Optional.of(rootUnit));

        // When
        UnitGotModel unitGotModel = unitGraph.createGraph(0, populateModel);
        UnitModel unitModel = unitGotModel.getUnit();

        // Then
        assertNotNull(unitModel);
        assertThat(unitModel.getId()).isNotNull();
        assertThat(unitModel.getTenant()).isEqualTo(1);
        assertThat(unitModel.getSp()).isEqualTo(populateModel.getSp());
        assertThat(unitModel.getSps()).containsExactlyInAnyOrder(populateModel.getSp(), "saphir");
        assertThat(unitModel.getUp()).contains(populateModel.getRootId());
        assertThat(unitModel.getUs()).contains(populateModel.getRootId());
        assertThat(unitModel.getGraph())
            .containsExactlyInAnyOrder(createGraphRelation(unitModel.getId(), "1234"));
        assertThat(unitModel.getParentOriginatingAgencies()).containsOnlyKeys("saphir");
        assertThat(unitModel.getParentOriginatingAgencies().get("saphir")).containsExactly("1234");
        assertThat(LocalDateUtil.parseMongoFormattedDate(unitModel.getGraphLastPersistedDate()))
            .isAfter(LocalDateUtil.now().minusMinutes(1)).isBefore(LocalDateUtil.now().plusSeconds(1));
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
        rootUnit.setId("1234");
        rootUnit.setSp("saphir");
        rootUnit.setSps(new HashSet<>(Arrays.asList("saphir")));
        rootUnit.getUp().add("123");
        rootUnit.getUs().add("123");
        rootUnit.getUds().put("1", Arrays.asList("123"));
        rootUnit.getGraph().add("1234/123");
        rootUnit.getParentOriginatingAgencies().put("saphir", Collections.singletonList("123"));
        given(metadataRepository.findUnitById(populateModel.getRootId())).willReturn(Optional.of(rootUnit));

        // When
        UnitGotModel unitGotModel = unitGraph.createGraph(0, populateModel);
        UnitModel unitModel = unitGotModel.getUnit();
        ObjectGroupModel gotModel = unitGotModel.getGot();

        // Then
        assertNotNull(unitModel);
        assertThat(unitModel.getTenant()).isEqualTo(1);
        assertThat(unitModel.getSp()).isEqualTo(populateModel.getSp());
        assertThat(unitModel.getSps()).containsExactlyInAnyOrder(populateModel.getSp(), "saphir");
        assertThat(unitModel.getUp()).contains(populateModel.getRootId());
        assertThat(unitModel.getUs()).contains(populateModel.getRootId(), "123");
        assertThat(unitModel.getUds()).containsEntry("1", Arrays.asList(populateModel.getRootId())).containsEntry("2", Arrays.asList("123"));
        assertThat(unitModel.getGraph())
            .containsExactlyInAnyOrder("1234/123", createGraphRelation(unitModel.getId(), "1234"));
        assertThat(unitModel.getParentOriginatingAgencies()).containsOnlyKeys("saphir");
        assertThat(unitModel.getParentOriginatingAgencies().get("saphir")).containsExactly("1234", "123");
        assertThat(LocalDateUtil.parseMongoFormattedDate(unitModel.getGraphLastPersistedDate()))
            .isAfter(LocalDateUtil.now().minusMinutes(1)).isBefore(LocalDateUtil.now().plusSeconds(1));
        // and
        assertNotNull(gotModel);
        assertThat(gotModel.getTenant()).isEqualTo(1);
        assertThat(gotModel.getSp()).isEqualTo(populateModel.getSp());
        assertThat(gotModel.getSps()).contains(populateModel.getSp());
        assertThat(gotModel.getUp()).contains(unitModel.getId());
        assertThat(LocalDateUtil.parseMongoFormattedDate(gotModel.getGraphLastPersistedDate()))
            .isAfter(LocalDateUtil.now().minusMinutes(1)).isBefore(LocalDateUtil.now().plusSeconds(1));
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
