package fr.gouv.vitam.metadata.client;

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
    public void insertUnitTest()
        throws MetaDataExecutionException, MetaDataNotFoundException, MetaDataAlreadyExistException,
        MetaDataDocumentSizeException, MetaDataClientServerException, InvalidParseOperationException {
        assertNotNull(client.insertUnit(JsonHandler.getFromString(VALID_QUERY)));
    }

    @Test
    public void selectUnitsTest()
        throws MetaDataExecutionException, MetaDataDocumentSizeException, MetaDataClientServerException,
        InvalidParseOperationException, VitamDBException {
        assertNotNull(client.selectUnits(JsonHandler.getFromString(VALID_QUERY)));
    }

    @Test
    public void selectUnitbyIdTest()
        throws MetaDataExecutionException, MetaDataDocumentSizeException, MetaDataClientServerException,
        InvalidParseOperationException {
        assertNotNull(client.selectUnitbyId(JsonHandler.getFromString(VALID_QUERY), "unitId"));
    }

    @Test
    public void selectObjectGrouptbyIdTest()
        throws MetaDataExecutionException, MetaDataDocumentSizeException, MetadataInvalidSelectException,
        MetaDataClientServerException, InvalidParseOperationException,MetaDataNotFoundException {
        assertNotNull(client.selectObjectGrouptbyId(JsonHandler.getFromString(VALID_QUERY), "unitId"));
    }

    @Test
    public void updateUnitbyIdTest()
        throws MetaDataExecutionException, MetaDataDocumentSizeException, MetaDataClientServerException,
        InvalidParseOperationException, MetaDataNotFoundException {
        assertNotNull(client.updateUnitById(JsonHandler.getFromString(VALID_QUERY), "unitId"));
    }

    @Test
    public void insertObjectGroupTest()
        throws MetaDataExecutionException, MetaDataNotFoundException, MetaDataAlreadyExistException,
        MetaDataDocumentSizeException, MetaDataClientServerException, InvalidParseOperationException {
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
    public void testComputeGraph()
        throws VitamClientException {
        assertNotNull(client.computeGraph(GraphComputeResponse.GraphComputeAction.UNIT, Sets.newHashSet()));
    }

    @Test
    public void selectObjectsTest()
        throws MetaDataExecutionException, MetaDataDocumentSizeException, MetaDataClientServerException,
        InvalidParseOperationException, VitamDBException {
        assertNotNull(client.selectObjectGroups(JsonHandler.getFromString(VALID_QUERY)));
    }

    @Test
    public void selectUnitsWithInheritedRulesTest() throws Exception {
        assertThatThrownBy(() -> client.selectUnitsWithInheritedRules(JsonHandler.getFromString(VALID_QUERY)))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
