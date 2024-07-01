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
package fr.gouv.vitam.metadata.common.bulkatomicupdate;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.InternalActionKeysRetriever;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BulkSelectQueryParallelProcessorTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private MetaDataClient metadataClient;

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @RunWithCustomExecutor
    @Test
    public void testProcessSingleQuery() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0));

        UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();
        updateMultiQuery.addQueries(QueryHelper.exists("#id"));
        updateMultiQuery.addActions(UpdateActionHelper.set("field", "value"));
        JsonNode query = updateMultiQuery.getFinalUpdate();
        List<JsonNode> queries = List.of(query);

        List<BulkSelectQueryResultOK> successResults = new ArrayList<>();
        List<BulkSelectQueryResultFailure> failureResults = new ArrayList<>();
        QueryRestrictionConverter queryRestrictionConverter = mock(QueryRestrictionConverter.class);
        doAnswer(args -> args.getArgument(0)).when(queryRestrictionConverter).convert(any());

        doReturn(
            List.of(new RequestResponseOK<JsonNode>().addResult(JsonHandler.createObjectNode().put("#id", "unitId1")))
        )
            .when(metadataClient)
            .selectUnitsBulk(anyList());

        // When
        BulkSelectQueryParallelProcessor instance = new BulkSelectQueryParallelProcessor(
            metadataClient,
            new InternalActionKeysRetriever(),
            2,
            4,
            10,
            successResults::add,
            failureResults::add,
            queryRestrictionConverter,
            false
        );
        instance.processQueries(queries.iterator());

        // Then
        assertThat(successResults).hasSize(1);
        assertThat(successResults.get(0)).extracting(BulkSelectQueryResultOK::getQueryIndex).isEqualTo(0);
        assertThat(successResults.get(0)).extracting(BulkSelectQueryResultOK::getUnitId).isEqualTo("unitId1");

        assertThat(failureResults).isEmpty();

        verify(queryRestrictionConverter).convert(any());
    }

    @RunWithCustomExecutor
    @Test
    public void testProcessSingleQueryForbidInternalFields() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0));

        UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();
        updateMultiQuery.addQueries(QueryHelper.exists("#id"));
        updateMultiQuery.addActions(UpdateActionHelper.set(VitamFieldsHelper.originatingAgency(), "illegal"));
        JsonNode query = updateMultiQuery.getFinalUpdate();
        List<JsonNode> queries = List.of(query);

        List<BulkSelectQueryResultOK> successResults = new ArrayList<>();
        List<BulkSelectQueryResultFailure> failureResults = new ArrayList<>();
        QueryRestrictionConverter queryRestrictionConverter = mock(QueryRestrictionConverter.class);
        doAnswer(args -> args.getArgument(0)).when(queryRestrictionConverter).convert(any());

        // When
        BulkSelectQueryParallelProcessor instance = new BulkSelectQueryParallelProcessor(
            metadataClient,
            new InternalActionKeysRetriever(),
            2,
            4,
            10,
            successResults::add,
            failureResults::add,
            queryRestrictionConverter,
            false
        );
        instance.processQueries(queries.iterator());

        // Then
        assertThat(successResults).isEmpty();

        assertThat(failureResults).hasSize(1);
        assertThat(failureResults.get(0)).extracting(BulkSelectQueryResultFailure::getQueryIndex).isEqualTo(0);
        assertThat(failureResults.get(0))
            .extracting(BulkSelectQueryResultFailure::getMessage)
            .isEqualTo("Invalid DSL query: cannot contains internal field(s) : '#originating_agency'");

        verify(queryRestrictionConverter, never()).convert(any());

        verify(metadataClient, never()).selectUnitsBulk(anyList());
    }

    @RunWithCustomExecutor
    @Test
    public void testProcessSingleQueryAllowInternalFields() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0));

        UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();
        updateMultiQuery.addQueries(QueryHelper.exists("#id"));
        updateMultiQuery.addActions(UpdateActionHelper.set(VitamFieldsHelper.originatingAgency(), "override"));
        JsonNode query = updateMultiQuery.getFinalUpdate();
        List<JsonNode> queries = List.of(query);

        List<BulkSelectQueryResultOK> successResults = new ArrayList<>();
        List<BulkSelectQueryResultFailure> failureResults = new ArrayList<>();
        QueryRestrictionConverter queryRestrictionConverter = mock(QueryRestrictionConverter.class);
        doAnswer(args -> args.getArgument(0)).when(queryRestrictionConverter).convert(any());

        doReturn(
            List.of(new RequestResponseOK<JsonNode>().addResult(JsonHandler.createObjectNode().put("#id", "unitId1")))
        )
            .when(metadataClient)
            .selectUnitsBulk(anyList());

        // When
        BulkSelectQueryParallelProcessor instance = new BulkSelectQueryParallelProcessor(
            metadataClient,
            new InternalActionKeysRetriever(),
            2,
            4,
            10,
            successResults::add,
            failureResults::add,
            queryRestrictionConverter,
            true
        );
        instance.processQueries(queries.iterator());

        // Then
        assertThat(successResults).hasSize(1);
        assertThat(successResults.get(0)).extracting(BulkSelectQueryResultOK::getQueryIndex).isEqualTo(0);
        assertThat(successResults.get(0)).extracting(BulkSelectQueryResultOK::getUnitId).isEqualTo("unitId1");

        assertThat(failureResults).isEmpty();

        verify(queryRestrictionConverter).convert(any());
    }

    @RunWithCustomExecutor
    @Test
    public void testProcessMultipleQueriesComplex() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0));

        // Complex query set:
        //  For query with queryIndex = 13 ==> Invalid request (#internal_field)
        //  For queries with even queryIndex (0, 2, 4, ...24) ==> OK
        //  For queries with odd queryIndex (1, 3, ... 23)    ==> not found
        List<JsonNode> queries = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();
            updateMultiQuery.addQueries(QueryHelper.eq("#id", "unitId" + i));
            if (i == 13) {
                updateMultiQuery.addActions(UpdateActionHelper.set("#internal_field", "value" + i));
            } else {
                updateMultiQuery.addActions(UpdateActionHelper.set("field", "value" + i));
            }
            JsonNode query = updateMultiQuery.getFinalUpdate();
            queries.add(query);
        }

        List<BulkSelectQueryResultOK> successResults = new ArrayList<>();
        List<BulkSelectQueryResultFailure> failureResults = new ArrayList<>();
        QueryRestrictionConverter queryRestrictionConverter = mock(QueryRestrictionConverter.class);
        doAnswer(args -> {
            SelectParserMultiple parser = new SelectParserMultiple();
            parser.parse(args.getArgument(0));
            Query query = parser.getRequest().getQueries().get(0);
            Query newQuery = QueryHelper.and().add(query, QueryHelper.eq("#opi", "opi"));
            parser.getRequest().getQueries().set(0, newQuery);
            return parser.getRequest().getFinalSelect();
        })
            .when(queryRestrictionConverter)
            .convert(any());

        doAnswer(args -> {
            List<RequestResponseOK<JsonNode>> responses = new ArrayList<>();
            List<JsonNode> batchQueries = args.getArgument(0);
            for (JsonNode query : batchQueries) {
                String unitId = query.get("$query").get(0).get("$and").get(0).get("$eq").get("#id").asText();
                int queryIndex = Integer.parseInt(StringUtils.substringAfter(unitId, "unitId"));
                if (queryIndex % 2 == 0) {
                    responses.add(
                        new RequestResponseOK<JsonNode>().addResult(
                            JsonHandler.createObjectNode().put("#id", "unitId" + queryIndex)
                        )
                    );
                } else {
                    responses.add(new RequestResponseOK<>());
                }
            }
            return responses;
        })
            .when(metadataClient)
            .selectUnitsBulk(anyList());

        // When
        BulkSelectQueryParallelProcessor instance = new BulkSelectQueryParallelProcessor(
            metadataClient,
            new InternalActionKeysRetriever(),
            2,
            4,
            10,
            successResults::add,
            failureResults::add,
            queryRestrictionConverter,
            false
        );
        instance.processQueries(queries.iterator());

        // Then
        assertThat(successResults).hasSize(13);
        assertThat(successResults)
            .extracting(BulkSelectQueryResultOK::getQueryIndex)
            .containsExactlyInAnyOrder(IntStream.iterate(0, i -> i < 25, i -> i + 2).boxed().toArray(Integer[]::new));
        assertThat(successResults)
            .extracting(BulkSelectQueryResultOK::getUnitId)
            .containsExactlyInAnyOrder(
                IntStream.iterate(0, i -> i < 25, i -> i + 2).mapToObj(i -> "unitId" + i).toArray(String[]::new)
            );

        assertThat(failureResults).hasSize(12);
        assertThat(failureResults)
            .extracting(BulkSelectQueryResultFailure::getQueryIndex)
            .containsExactlyInAnyOrder(IntStream.iterate(1, i -> i < 25, i -> i + 2).boxed().toArray(Integer[]::new));
        for (BulkSelectQueryResultFailure failureResult : failureResults) {
            if (failureResult.getQueryIndex() == 13) {
                assertThat(failureResult.getBulkUpdateUnitReportKey()).isEqualTo(
                    BulkUpdateUnitReportKey.INVALID_DSL_QUERY
                );
            } else {
                assertThat(failureResult.getBulkUpdateUnitReportKey()).isEqualTo(
                    BulkUpdateUnitReportKey.UNIT_NOT_FOUND
                );
            }
        }

        // expected 24 = 25 - 1 (invalid query for "unitId13")
        verify(queryRestrictionConverter, times(24)).convert(any());

        // Verify metadata invoked in bulks, with "transformed query"
        ArgumentCaptor<List<JsonNode>> queryArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(metadataClient, times(3)).selectUnitsBulk(queryArgumentCaptor.capture());

        queryArgumentCaptor
            .getAllValues()
            .stream()
            .flatMap(Collection::stream)
            .forEach(q -> assertThat(q.get("$query").get(0).get("$and").get(1).get("$eq").get("#opi")).isNotNull());
    }

    @RunWithCustomExecutor
    @Test
    public void testProcessSingleQueryWithInternalServerError() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0));

        UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();
        updateMultiQuery.addQueries(QueryHelper.exists("#id"));
        updateMultiQuery.addActions(UpdateActionHelper.set("field", "value"));
        JsonNode query = updateMultiQuery.getFinalUpdate();
        List<JsonNode> queries = List.of(query);

        doThrow(new MetaDataClientServerException("prb")).when(metadataClient).selectUnitsBulk(anyList());

        BulkSelectQueryParallelProcessor instance = new BulkSelectQueryParallelProcessor(
            metadataClient,
            new InternalActionKeysRetriever(),
            2,
            4,
            10,
            result -> {},
            result -> {},
            dslQuery -> dslQuery,
            false
        );

        // When

        assertThatThrownBy(() -> instance.processQueries(queries.iterator()))
            // Then
            .isExactlyInstanceOf(VitamRuntimeException.class)
            .hasMessage("One or more FATAL errors occurred during bulk select query execution");
    }
}
