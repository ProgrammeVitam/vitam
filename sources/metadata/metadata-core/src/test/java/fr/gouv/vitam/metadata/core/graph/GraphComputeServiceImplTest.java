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

import com.google.common.collect.Sets;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.cache.VitamCache;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.GraphComputeResponse;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import org.assertj.core.util.Lists;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

public class GraphComputeServiceImplTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private VitamRepositoryProvider vitamRepositoryProvider;

    @Mock
    private VitamMongoRepository unitRepository;

    @Mock
    private VitamMongoRepository gotRepository;

    @Mock
    private VitamElasticsearchRepository unitEsRepository;

    @Mock
    private VitamElasticsearchRepository gotEsRepository;

    @Mock
    private FindIterable findIterableUnit;

    @Mock
    private MongoCursor mongoCursorUnit;

    @Mock
    private FindIterable findIterableGot;

    @Mock
    private MongoCursor mongoCursorGot;

    @Spy
    private VitamCache<String, Document> cache = GraphComputeCache.getInstance();

    @Spy
    private List<Integer> tenants = Lists.newArrayList(1, 2);

    @InjectMocks
    private GraphComputeServiceImpl graphBuilderService;

    @Before
    public void setup() throws DatabaseException {
        given(unitRepository.findDocuments(any(), anyInt())).willReturn(findIterableUnit);
        given(gotRepository.findDocuments(any(), anyInt())).willReturn(findIterableGot);

        given(unitRepository.findDocuments(any(), any())).willReturn(findIterableUnit);
        given(gotRepository.findDocuments(any(), any())).willReturn(findIterableGot);

        given(
            vitamRepositoryProvider.getVitamMongoRepository(MetadataCollections.UNIT.getVitamCollection())
        ).willReturn(unitRepository);
        given(
            vitamRepositoryProvider.getVitamMongoRepository(MetadataCollections.OBJECTGROUP.getVitamCollection())
        ).willReturn(gotRepository);

        given(findIterableUnit.projection(any())).willReturn(findIterableUnit);
        given(findIterableUnit.iterator()).willReturn(mongoCursorUnit);
        given(findIterableGot.projection(any())).willReturn(findIterableGot);
        given(findIterableGot.iterator()).willReturn(mongoCursorGot);

        Answer<Object> objectAnswer = o ->
            new Document("_id", GUIDFactory.newGUID().getId())
                .append("_og", LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
                .append("_glpd", LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
        when(mongoCursorUnit.next()).thenAnswer(objectAnswer);
        when(mongoCursorGot.next()).thenAnswer(objectAnswer);
    }

    @Test
    @RunWithCustomExecutor
    public void whenBuildGraphThenOK() {
        // Given
        final int[] cpt = { 0 };
        when(mongoCursorUnit.hasNext()).thenAnswer(o -> {
            if (cpt[0] > 4) {
                return false;
            }
            cpt[0]++;
            return true;
        });

        final int[] cptGot = { 0 };
        when(mongoCursorGot.hasNext()).thenAnswer(o -> {
            if (cptGot[0] > 4) {
                return false;
            }
            cptGot[0]++;
            return true;
        });

        GraphComputeResponse response = graphBuilderService.computeGraph(
            MetadataCollections.UNIT,
            Sets.newHashSet("fake1", "fake2", "fake3"),
            true,
            true
        );
        assertThat(response.getUnitCount()).isEqualTo(3);
        assertThat(response.getGotCount()).isEqualTo(3);
    }

    @Test
    @RunWithCustomExecutor
    public void whenBuildObjectGroupGraphThenOK() {
        // Given
        final int[] cpt = { 0 };
        when(mongoCursorGot.hasNext()).thenAnswer(o -> {
            if (cpt[0] > 4) {
                return false;
            }
            cpt[0]++;
            return true;
        });

        GraphComputeResponse response = graphBuilderService.computeGraph(
            MetadataCollections.OBJECTGROUP,
            Sets.newHashSet("fake1", "fake2", "fake3"),
            false,
            true
        );
        assertThat(response.getGotCount()).isEqualTo(3);
    }

    @Test
    @RunWithCustomExecutor
    public void whenBuildUnitGraphThenOK() {
        // Given
        final int[] cpt = { 0 };
        when(mongoCursorUnit.hasNext()).thenAnswer(o -> {
            if (cpt[0] > 4) {
                return false;
            }
            cpt[0]++;
            return true;
        });

        GraphComputeResponse response = graphBuilderService.computeGraph(
            MetadataCollections.UNIT,
            Sets.newHashSet("fake1", "fake2", "fake3"),
            false,
            true
        );
        assertThat(response.getUnitCount()).isEqualTo(3);
    }

    @Test
    @RunWithCustomExecutor
    public void whenBuildGraphThenQueryResultIsEmpty() {
        // given
        GraphComputeResponse response = graphBuilderService.computeGraph(
            MetadataCollections.UNIT,
            Sets.newHashSet(),
            false,
            true
        );
        assertThat(response.getUnitCount()).isEqualTo(0);
        response = graphBuilderService.computeGraph(MetadataCollections.OBJECTGROUP, Sets.newHashSet(), false, true);
        assertThat(response.getGotCount()).isEqualTo(0);
    }
}
