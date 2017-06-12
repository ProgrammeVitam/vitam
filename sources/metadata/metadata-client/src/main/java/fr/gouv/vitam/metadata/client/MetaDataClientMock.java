package fr.gouv.vitam.metadata.client;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.exception.MetadataInvalidSelectException;
import fr.gouv.vitam.metadata.api.model.UnitPerOriginatingAgency;

/**
 * Mock client implementation for metadata
 */
public class MetaDataClientMock extends AbstractMockClient implements MetaDataClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetaDataClientMock.class);

    @Override
    public JsonNode insertUnit(JsonNode insertQuery)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataAlreadyExistException, MetaDataDocumentSizeException, MetaDataClientServerException {
        return ClientMockResultHelper.getMetaDataResult();
    }

    @Override
    public JsonNode selectUnits(JsonNode selectQuery)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException {
        JsonNode res = null;
        try {
            res = JsonHandler.getFromFile(PropertiesUtils.getResourceFile("result.json"));
        } catch (FileNotFoundException e) {
            LOGGER.error(e);
        }
        return res;
    }

    @Override
    public JsonNode selectUnitbyId(JsonNode selectQuery, String unitId)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException {
        JsonNode res = null;
        try {
            res = JsonHandler.getFromFile(PropertiesUtils.getResourceFile("result.json"));
        } catch (FileNotFoundException e) {
            LOGGER.error(e);
        }
        return res;
    }

    @Override
    public JsonNode selectObjectGrouptbyId(JsonNode selectQuery, String objectGroupId)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetadataInvalidSelectException, MetaDataClientServerException {
        return ClientMockResultHelper.getMetaDataResult();
    }

    @Override
    public JsonNode updateUnitbyId(JsonNode updateQuery, String unitId)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException {
        return ClientMockResultHelper.getMetaDataResult();
    }

    @Override
    public JsonNode insertObjectGroup(JsonNode insertQuery)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetaDataAlreadyExistException, MetaDataDocumentSizeException, MetaDataClientServerException {
        return ClientMockResultHelper.getMetaDataResult();
    }

    @Override public List<UnitPerOriginatingAgency> selectAccessionRegisterByOperationId(String operationId)
        throws MetaDataClientServerException {
        return new ArrayList<>();
    }

}
