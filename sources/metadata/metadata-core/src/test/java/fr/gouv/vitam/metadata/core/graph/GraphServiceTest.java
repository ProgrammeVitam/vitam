/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.metadata.core.graph;

import fr.gouv.vitam.metadata.core.database.collections.MongoDbMetadataRepository;
import fr.gouv.vitam.metadata.core.database.collections.Unit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static fr.gouv.vitam.metadata.core.database.collections.GraphLoader.UNIT_VITAM_GRAPH_PROJECTION;
import static fr.gouv.vitam.metadata.core.database.collections.Unit.UNITDEPTHS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

public class GraphServiceTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private GraphService graphService;

    @Mock
    private MongoDbMetadataRepository mongoDbMetadataRepository;

    @Test
    public void should_compute_graph_with_many_level() throws Exception {
        // Given
        HashSet<String> directParents = Sets.newHashSet("11", "12");
        Unit unit1 = new Unit();
        unit1.put("_id", "11");
        HashMap<String, List<String>> uds11 = new HashMap<>();

        uds11.put("1", Lists.newArrayList("1"));
        unit1.put(UNITDEPTHS, uds11);

        Unit unit2 = new Unit();
        unit2.put("_id", "12");
        HashMap<String, List<String>> uds12 = new HashMap<>();

        uds12.put("1", Lists.newArrayList("1"));
        unit2.put(UNITDEPTHS, uds12);

        given(mongoDbMetadataRepository.selectByIds(directParents, UNIT_VITAM_GRAPH_PROJECTION))
            .willReturn(Lists.newArrayList(unit1, unit2));

        Unit unit = new Unit();

        // When
        graphService.compute(unit, directParents);

        // Then
        assertThat(unit.getMapOrEmpty(UNITDEPTHS)).containsEntry("1", Sets.newHashSet("11", "12"))
            .containsEntry("2", Sets.newHashSet("1"));
        assertThat(unit.getInteger("_max")).isEqualTo(3);
        assertThat(unit.getInteger("_min")).isEqualTo(1);
    }

    @Test
    public void should_compute_graph_with_one_level() throws Exception {
        // Given
        HashSet<String> directParents = Sets.newHashSet("1", "2");
        Unit unit1 = new Unit();
        unit1.put("_id", "1");

        Unit unit2 = new Unit();
        unit2.put("_id", "2");

        given(mongoDbMetadataRepository.selectByIds(directParents, UNIT_VITAM_GRAPH_PROJECTION))
            .willReturn(Lists.newArrayList(unit1, unit2));

        Unit unit = new Unit();

        // When
        graphService.compute(unit, directParents);

        // Then
        assertThat(unit.getMapOrEmpty(UNITDEPTHS)).containsEntry("1", Sets.newHashSet("1", "2"));
        assertThat(unit.getInteger("_max")).isEqualTo(2);
        assertThat(unit.getInteger("_min")).isEqualTo(1);
    }

}