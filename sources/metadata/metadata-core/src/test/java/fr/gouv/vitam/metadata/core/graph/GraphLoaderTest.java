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
package fr.gouv.vitam.metadata.core.graph;

import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbMetadataRepository;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static fr.gouv.vitam.metadata.core.graph.GraphLoader.UNIT_VITAM_GRAPH_PROJECTION;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

public class GraphLoaderTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private GraphLoader graphLoader;

    @Mock
    private MongoDbMetadataRepository mongoDbMetadataRepository;

    @Test
    public void should_compute_graph_with_many_level() throws Exception {
        // Given
        Unit unit = new Unit();
        unit.put("_id", "1");

        Unit unit1 = new Unit();
        unit1.put("_id", "11");
        unit1.put("_up", newHashSet("1"));

        Unit unit2 = new Unit();
        unit2.put("_id", "12");
        unit2.put("_up", newHashSet("1"));

        Unit unit21 = new Unit();
        unit21.put("_id", "21");
        unit21.put("_up", newHashSet("11", "12"));

        given(mongoDbMetadataRepository.selectByIds(newHashSet("11", "12"), UNIT_VITAM_GRAPH_PROJECTION)).willReturn(
            newArrayList(unit1, unit2)
        );

        given(mongoDbMetadataRepository.selectByIds(newHashSet("1"), UNIT_VITAM_GRAPH_PROJECTION)).willReturn(
            newArrayList(unit)
        );

        given(mongoDbMetadataRepository.selectByIds(newHashSet("21"), UNIT_VITAM_GRAPH_PROJECTION)).willReturn(
            newArrayList(unit21)
        );

        // When
        graphLoader.loadGraphInfo(newArrayList("21"));

        // Then
        verify(mongoDbMetadataRepository).selectByIds(newHashSet("11", "12"), UNIT_VITAM_GRAPH_PROJECTION);
        verify(mongoDbMetadataRepository).selectByIds(newHashSet("1"), UNIT_VITAM_GRAPH_PROJECTION);
        verify(mongoDbMetadataRepository).selectByIds(newHashSet("21"), UNIT_VITAM_GRAPH_PROJECTION);
    }

    @Test
    public void should_compute_graph_with_same_parent_in_different_level() throws Exception {
        // Given
        Unit unit = new Unit();
        unit.put("_id", "1");

        Unit unit1 = new Unit();
        unit1.put("_id", "11");
        unit1.put("_up", newHashSet("1"));

        Unit unit21 = new Unit();
        unit21.put("_id", "21");
        unit21.put("_up", newHashSet("11", "1"));

        given(mongoDbMetadataRepository.selectByIds(newHashSet("11", "1"), UNIT_VITAM_GRAPH_PROJECTION)).willReturn(
            newArrayList(unit1, unit)
        );

        given(mongoDbMetadataRepository.selectByIds(newHashSet("1"), UNIT_VITAM_GRAPH_PROJECTION)).willReturn(
            newArrayList(unit)
        );

        given(mongoDbMetadataRepository.selectByIds(newHashSet("21"), UNIT_VITAM_GRAPH_PROJECTION)).willReturn(
            newArrayList(unit21)
        );

        // When
        graphLoader.loadGraphInfo(newHashSet("21"));

        // Then
        verify(mongoDbMetadataRepository).selectByIds(newHashSet("11", "1"), UNIT_VITAM_GRAPH_PROJECTION);
        verify(mongoDbMetadataRepository).selectByIds(newHashSet("21"), UNIT_VITAM_GRAPH_PROJECTION);
        verify(mongoDbMetadataRepository).selectByIds(newHashSet("1"), UNIT_VITAM_GRAPH_PROJECTION);
    }

    @Test
    public void should_compute_graph_with_rake() throws Exception {
        // Given
        Unit unit = new Unit();
        unit.put("_id", "1");

        Unit unit1 = new Unit();
        unit1.put("_id", "11");
        unit1.put("_up", newHashSet("1"));

        Unit unit2 = new Unit();
        unit2.put("_id", "12");
        unit2.put("_up", newHashSet("1"));

        Unit unit3 = new Unit();
        unit3.put("_id", "13");
        unit3.put("_up", newHashSet("1"));

        given(
            mongoDbMetadataRepository.selectByIds(newHashSet("11", "12", "13"), UNIT_VITAM_GRAPH_PROJECTION)
        ).willReturn(newArrayList(unit1, unit2, unit3));

        given(mongoDbMetadataRepository.selectByIds(newHashSet("1"), UNIT_VITAM_GRAPH_PROJECTION)).willReturn(
            newArrayList(unit)
        );

        // When
        graphLoader.loadGraphInfo(newArrayList("11", "12", "13"));

        // Then
        verify(mongoDbMetadataRepository).selectByIds(newHashSet("11", "12", "13"), UNIT_VITAM_GRAPH_PROJECTION);
        verify(mongoDbMetadataRepository).selectByIds(newHashSet("1"), UNIT_VITAM_GRAPH_PROJECTION);
    }

    @Test
    public void should_throw_metadataException_when_id_not_found() {
        // Given / When / Then
        assertThatThrownBy(() -> graphLoader.loadGraphInfo(newArrayList("1")))
            .isInstanceOf(MetaDataException.class)
            .hasMessage("Cannot find parents: [1]");
    }
}
