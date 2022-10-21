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

package fr.gouv.vitam.collect.internal.service;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MetadataServiceTest {

    private final String TRANSACTION_ID = "TRANSACTION_ID";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MetaDataClientFactory metaDataCollectClientFactory;

    @Mock
    private MetaDataClient metaDataCollectClient;

    private MetadataService metadataService;


    @Before
    public void setUp() throws Exception {
        when(metaDataCollectClientFactory.getClient()).thenReturn(metaDataCollectClient);
        metadataService = new MetadataService(metaDataCollectClientFactory);
    }

    @Test
    public void testSelectUnits() throws Exception {
        SelectMultiQuery select = new SelectMultiQuery();
        JsonNode query = select.getFinalSelect();

        when(metaDataCollectClient.selectUnits(any())).thenAnswer(
            a -> JsonHandler.toJsonNode(new RequestResponseOK<>().setQuery(a.getArgument(0)))
        );

        final JsonNode jsonNode = metadataService.selectUnits(query, TRANSACTION_ID);

        assertEquals("[{\"$in\":{\"#opi\":[\"" + TRANSACTION_ID + "\"]}}]",
            jsonNode.get("$context").get("$query").toString());
    }

    @Test
    public void testSelectAllUnits() throws Exception {
        SelectMultiQuery select = new SelectMultiQuery();
        List<JsonNode> results = List.of(JsonHandler.createObjectNode());
        when(metaDataCollectClient.selectUnits(any())).thenAnswer(
            a -> JsonHandler.toJsonNode(
                new RequestResponseOK<>(a.getArgument(0), results, 1))
        );

        final ScrollSpliterator<JsonNode> scrollSpliterator =
            metadataService.selectUnits(select, TRANSACTION_ID);

        assertEquals(results.size(), scrollSpliterator.estimateSize());
    }

    @Test
    public void testSelectUnitById() throws Exception {
        final String UNIT_ID = "UNIT_ID";
        when(metaDataCollectClient.selectUnitbyId(any(), eq(UNIT_ID))).thenAnswer(
            a -> JsonHandler.toJsonNode(new RequestResponseOK<>().setQuery(a.getArgument(0)))
        );

        metadataService.selectUnitById(UNIT_ID);

        verify(metaDataCollectClient).selectUnitbyId(any(), eq(UNIT_ID));
    }

    @Test
    public void testSelectObjectGroups() throws Exception {
        SelectMultiQuery select = new SelectMultiQuery();
        JsonNode query = select.getFinalSelect();

        when(metaDataCollectClient.selectObjectGroups(any())).thenAnswer(
            a -> JsonHandler.toJsonNode(new RequestResponseOK<>().setQuery(a.getArgument(0)))
        );

        final JsonNode jsonNode = metadataService.selectObjectGroups(query, TRANSACTION_ID);

        assertEquals("[{\"$in\":{\"#opi\":[\"" + TRANSACTION_ID + "\"]}}]",
            jsonNode.get("$context").get("$query").toString());
    }

    @Test
    public void testAtomicBulkUpdate() throws Exception {
        final List<JsonNode> queries = List.of(JsonHandler.createObjectNode());
        metadataService.atomicBulkUpdate(queries);
        verify(metaDataCollectClient).atomicUpdateBulk(eq(queries));
    }
}