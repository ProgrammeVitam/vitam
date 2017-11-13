package fr.gouv.vitam.metadata.client;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.exception.MetadataInvalidSelectException;

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
        InvalidParseOperationException {
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
        MetaDataClientServerException, InvalidParseOperationException {
        assertNotNull(client.selectObjectGrouptbyId(JsonHandler.getFromString(VALID_QUERY), "unitId"));
    }

    @Test
    public void updateUnitbyIdTest()
        throws MetaDataExecutionException, MetaDataDocumentSizeException, MetaDataClientServerException,
        InvalidParseOperationException, MetaDataNotFoundException {
        assertNotNull(client.updateUnitbyId(JsonHandler.getFromString(VALID_QUERY), "unitId"));
    }

    @Test
    public void insertObjectGroupTest()
        throws MetaDataExecutionException, MetaDataNotFoundException, MetaDataAlreadyExistException,
        MetaDataDocumentSizeException, MetaDataClientServerException, InvalidParseOperationException {
        assertNotNull(client.insertObjectGroup(JsonHandler.getFromString(VALID_QUERY)));
    }

}
