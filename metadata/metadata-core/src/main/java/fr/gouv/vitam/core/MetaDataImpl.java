/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2016)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.core;


import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoWriteException;

import fr.gouv.vitam.api.MetaData;
import fr.gouv.vitam.api.config.MetaDataConfiguration;
import fr.gouv.vitam.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.builder.request.construct.Request;
import fr.gouv.vitam.builder.request.construct.configuration.ParserTokens;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.core.database.collections.MongoDbVarNameAdapter;
import fr.gouv.vitam.core.database.collections.Result;
import fr.gouv.vitam.core.utils.UnitsJsonUtils;
import fr.gouv.vitam.parser.request.parser.GlobalDatasParser;
import fr.gouv.vitam.parser.request.parser.InsertParser;
import fr.gouv.vitam.parser.request.parser.RequestParser;
import fr.gouv.vitam.parser.request.parser.SelectParser;

/**
 * MetaDataImpl implements a MetaData interface
 */
public final class MetaDataImpl implements MetaData {

    private final DbRequestFactory dbRequestFactory;

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(MetaDataImpl.class);

    private static final String REQUEST_IS_NULL = "Request select is null or is empty";

    /**
     * MetaDataImpl constructor
     *
     * @param configuration of mongoDB access
     * @param mongoDbAccessFactory 
     * @param dbRequestFactory 
     */
    // FIXME REVIEW should be private and adding public static final Metadata newMetadata(...) calling this private
    // constructor
    public MetaDataImpl(MetaDataConfiguration configuration, MongoDbAccessFactory mongoDbAccessFactory,
        DbRequestFactory dbRequestFactory) {
        mongoDbAccessFactory.create(configuration);
        // FIXME REVIEW should check null
        this.dbRequestFactory = dbRequestFactory;
    }

    @Override
    public void insertUnit(JsonNode insertRequest)
        throws InvalidParseOperationException, MetaDataDocumentSizeException, MetaDataExecutionException,
        MetaDataAlreadyExistException, MetaDataNotFoundException {
        Result result = null;
        try {
            GlobalDatasParser.sanityRequestCheck(insertRequest.toString());
        } catch (final InvalidParseOperationException e) {
            throw new MetaDataDocumentSizeException(e);
        }

        try {
            final InsertParser insertParser = new InsertParser(new MongoDbVarNameAdapter());
            insertParser.parse(insertRequest);
            result = dbRequestFactory.create().execRequest(insertParser, result);
        } catch (final InvalidParseOperationException e) {
            throw e;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new MetaDataExecutionException(e);
        } catch (final MetaDataAlreadyExistException e) {
            throw e;
        } catch (final MongoWriteException e) {
            throw new MetaDataAlreadyExistException(e);
        }

        if (result.isError()) {
            throw new MetaDataNotFoundException("Parents not found");
        }
    }

    @Override
    public void insertObjectGroup(JsonNode objectGroupRequest)
        throws InvalidParseOperationException, MetaDataDocumentSizeException, MetaDataExecutionException,
        MetaDataAlreadyExistException, MetaDataNotFoundException {
        Result result = null;

        try {
            GlobalDatasParser.sanityRequestCheck(objectGroupRequest.toString());
        } catch (final InvalidParseOperationException e) {
            throw new MetaDataDocumentSizeException(e);
        }

        try {
            InsertParser insertParser = new InsertParser(new MongoDbVarNameAdapter());
            insertParser.parse(objectGroupRequest);
            insertParser.getRequest().addHintFilter(ParserTokens.FILTERARGS.OBJECTGROUPS.exactToken());
            result = dbRequestFactory.create().execRequest(insertParser, result);
        } catch (final InvalidParseOperationException e) {
            throw e;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new MetaDataExecutionException(e);
        } catch (final MongoWriteException e) {
            throw new MetaDataAlreadyExistException(e);
        }

        if (result.isError()) {
            throw new MetaDataNotFoundException("Parents not found");
        }
    }

    @Override
    public JsonNode selectUnitsByQuery(String selectQuery)
        throws MetaDataExecutionException, InvalidParseOperationException,
        MetaDataDocumentSizeException {
        LOGGER.info("Begin selectUnitsByQuery ...");
        LOGGER.debug("SelectUnitsByQuery/ selectQuery: " + selectQuery);
        return selectUnit(selectQuery, null);

    }

    @Override
    public JsonNode selectUnitsById(String selectQuery, String unitId)
        throws InvalidParseOperationException, MetaDataExecutionException,
        MetaDataDocumentSizeException {
        LOGGER.info("Begin selectUnitsById .../id:" + unitId);
        LOGGER.debug("SelectUnitsById/ selectQuery: " + selectQuery);
        return selectUnit(selectQuery, unitId);
    }



    private JsonNode selectUnit(String selectQuery, String unitId)
        throws MetaDataExecutionException, InvalidParseOperationException,
        MetaDataDocumentSizeException {
        Result result = null;
        JsonNode jsonNodeResponse;
        if (StringUtils.isEmpty(selectQuery)) {
            throw new InvalidParseOperationException(REQUEST_IS_NULL);
        }
        try {
            // sanity check:InvalidParseOperationException will be thrown if request select invalid or size is too large
            GlobalDatasParser.sanityRequestCheck(selectQuery);
        } catch (InvalidParseOperationException eInvalidParseOperationException) {
            throw new MetaDataDocumentSizeException(eInvalidParseOperationException);
        }
        try {
            // parse Select request
            RequestParser selectRequest = new SelectParser();
            selectRequest.parse(selectQuery);
            // Reset $roots (add or override unit_id on roots)
            if (unitId != null && !unitId.isEmpty()) {
                Request request = selectRequest.getRequest();
                if (request != null) {
                    LOGGER.debug("Reset $roots unit_id by :" + unitId);
                    request.resetRoots().addRoots(unitId);
                }
            }
            // Execute DSL request
            result = dbRequestFactory.create().execRequest(selectRequest, result);
            jsonNodeResponse = UnitsJsonUtils.populateJSONObjectResponse(result, selectRequest);

        } catch (final MetaDataExecutionException e) {
            LOGGER.error(e);
            throw e;
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            throw e;
        } catch (final InstantiationException e) {
            LOGGER.error(e);
            throw new MetaDataExecutionException(e);
        } catch (final IllegalAccessException e) {
            LOGGER.error(e);
            throw new MetaDataExecutionException(e);
        } catch (MetaDataAlreadyExistException | MetaDataNotFoundException e) {
            // Should not happen there
            LOGGER.error(e);
            throw new MetaDataExecutionException(e);
        }
        return jsonNodeResponse;
    }

}
