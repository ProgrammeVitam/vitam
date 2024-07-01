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
package fr.gouv.vitam.metadata.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.GraphComputeResponse;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.exception.MetadataInvalidSelectException;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNotNull;

public class MetaDataClientMockTest {

    private static final String VALID_QUERY = "{$query: {$eq: {\"aa\" : \"vv\" }}, $projection: {}, $filter: {}}";

    public MetaDataClient client;

    @Before
    public void setUp() {
        MetaDataClientFactory.changeMode(null);
        client = MetaDataClientFactory.getInstance().getClient();
    }

    @Test
    public void selectUnitsTest()
        throws MetaDataExecutionException, MetaDataDocumentSizeException, MetaDataClientServerException, InvalidParseOperationException, VitamDBException {
        assertNotNull(client.selectUnits(JsonHandler.getFromString(VALID_QUERY)));
    }

    @Test
    public void selectUnitsBulkTest()
        throws MetaDataExecutionException, MetaDataDocumentSizeException, MetaDataClientServerException, InvalidParseOperationException, VitamDBException {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        queries.add(JsonHandler.getFromString(VALID_QUERY));
        assertNotNull(client.selectUnitsBulk(queries));
    }

    @Test
    public void selectUnitbyIdTest()
        throws MetaDataExecutionException, MetaDataDocumentSizeException, MetaDataClientServerException, InvalidParseOperationException {
        assertNotNull(client.selectUnitbyId(JsonHandler.getFromString(VALID_QUERY), "unitId"));
    }

    @Test
    public void selectObjectGrouptbyIdTest()
        throws MetaDataExecutionException, MetaDataDocumentSizeException, MetadataInvalidSelectException, MetaDataClientServerException, InvalidParseOperationException, MetaDataNotFoundException {
        assertNotNull(client.selectObjectGrouptbyId(JsonHandler.getFromString(VALID_QUERY), "unitId"));
    }

    @Test
    public void updateUnitbyIdTest()
        throws MetaDataExecutionException, MetaDataDocumentSizeException, MetaDataClientServerException, InvalidParseOperationException, MetaDataNotFoundException {
        assertNotNull(client.updateUnitById(JsonHandler.getFromString(VALID_QUERY), "unitId"));
    }

    @Test
    public void atomicUpdateBulk()
        throws MetaDataExecutionException, MetaDataDocumentSizeException, MetaDataClientServerException, InvalidParseOperationException, MetaDataNotFoundException {
        List<JsonNode> queries = new ArrayList<JsonNode>();
        queries.add(JsonHandler.getFromString(VALID_QUERY));
        assertNotNull(client.atomicUpdateBulk(queries));
    }

    @Test
    public void insertObjectGroupTest()
        throws MetaDataExecutionException, MetaDataNotFoundException, MetaDataAlreadyExistException, MetaDataDocumentSizeException, MetaDataClientServerException, InvalidParseOperationException {
        assertNotNull(client.insertObjectGroup(JsonHandler.getFromString(VALID_QUERY)));
    }

    @Test
    public void launchReindexationTest()
        throws MetaDataClientServerException, MetaDataNotFoundException, InvalidParseOperationException {
        assertNotNull(client.reindex(new IndexParameters()));
    }

    @Test
    public void switchIndexesTest()
        throws MetaDataClientServerException, MetaDataNotFoundException, InvalidParseOperationException {
        assertNotNull(client.switchIndexes(new SwitchIndexParameters()));
    }

    @Test
    public void getUnitByIdRawTest() throws VitamClientException {
        assertNotNull(client.getUnitByIdRaw("unitId"));
    }

    @Test
    public void getObjectGroupByIdRawTest() throws VitamClientException {
        assertNotNull(client.getObjectGroupByIdRaw("objectGroupId"));
    }

    @Test
    public void testComputeGraphByDSL() throws VitamClientException {
        assertNotNull(client.computeGraph(JsonHandler.createObjectNode()));
    }

    @Test
    public void testComputeGraph() throws VitamClientException {
        assertNotNull(client.computeGraph(GraphComputeResponse.GraphComputeAction.UNIT, Sets.newHashSet()));
    }

    @Test
    public void selectObjectsTest()
        throws MetaDataExecutionException, MetaDataDocumentSizeException, MetaDataClientServerException, InvalidParseOperationException, VitamDBException {
        assertNotNull(client.selectObjectGroups(JsonHandler.getFromString(VALID_QUERY)));
    }

    @Test
    public void selectUnitsWithInheritedRulesTest() throws Exception {
        assertThatThrownBy(
            () -> client.selectUnitsWithInheritedRules(JsonHandler.getFromString(VALID_QUERY))
        ).isInstanceOf(UnsupportedOperationException.class);
    }
}
