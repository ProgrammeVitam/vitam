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
import fr.gouv.vitam.api.exception.MetadataInvalidSelectException;
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

    private static final String MUST_BE_SELECT_PARSER = "Must be select request type when searching metadata";
    private static final String REQUEST_IS_NULL = "Request select is null or is empty";

    /**
     * MetaDataImpl constructor
     *
     * @param configuration of mongoDB access
     */
    // FIXME REVIEW should be private and adding public static final Metadata newMetadata(...) calling this private
    // constructor
    public MetaDataImpl(MetaDataConfiguration configuration, MongoDbAccessFactory mongoDbAccessFactory,
        DbRequestFactory dbRequestFactory) {
        mongoDbAccessFactory.create(configuration);
        // FIXME REVIEW should check null
        this.dbRequestFactory = dbRequestFactory;
    }

    // FIXME REVIEW should take a json as input
    @Override
    public void insertUnit(String insertRequest) throws InvalidParseOperationException {
        Result result = null;
        try {
            // Refactor to throw MetaDataDocumentSizeException
            GlobalDatasParser.sanityRequestCheck(insertRequest);
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
        } catch (final MongoWriteException e) {
            throw new MetaDataAlreadyExistException(e);
        }

        if (result.isError()) {
            throw new MetaDataNotFoundException("Parents not found");
        }

    }

    @Override
    public JsonNode selectUnitsByQuery(String selectQuery)
        throws MetaDataExecutionException, InvalidParseOperationException, MetadataInvalidSelectException,
        MetaDataDocumentSizeException {
        LOGGER.info("MetaDataImpl / Begin selectUnitsByQuery ...");
        LOGGER.debug("MetaDataImpl /selectUnitsByQuery/ selectQuery: " + selectQuery);

        Result result = null;
        JsonNode jsonNodeResponse;
        if (StringUtils.isEmpty(selectQuery)) {
            throw new MetadataInvalidSelectException(REQUEST_IS_NULL);
        }
        try {
            // sanity check:InvalidParseOperationException will be thrown if request select invalid or size is too large
            GlobalDatasParser.sanityRequestCheck(selectQuery);
        } catch (InvalidParseOperationException eInvalidParseOperationException) {
            throw new MetaDataDocumentSizeException(eInvalidParseOperationException);
        }


        // parse Select request
        RequestParser selectRequest = new SelectParser();
        selectRequest.parse(selectQuery);
        // check query type: must be instance of Select
        if (!(selectRequest instanceof SelectParser)) {
            throw new MetadataInvalidSelectException(MUST_BE_SELECT_PARSER);
        }

        try {
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
        }
        return jsonNodeResponse;
    }



}
