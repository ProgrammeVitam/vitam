/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.metadata.core;


import static difflib.DiffUtils.generateUnifiedDiff;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.mongodb.MongoWriteException;

import difflib.DiffUtils;
import difflib.Patch;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.multiple.RequestMultiple;
import fr.gouv.vitam.common.database.builder.request.multiple.Select;
import fr.gouv.vitam.common.database.parser.request.GlobalDatasParser;
import fr.gouv.vitam.common.database.parser.request.multiple.InsertParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.metadata.api.MetaData;
import fr.gouv.vitam.metadata.api.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbVarNameAdapter;
import fr.gouv.vitam.metadata.core.database.collections.Result;
import fr.gouv.vitam.metadata.core.utils.MetadataJsonResponseUtils;

/**
 * MetaDataImpl implements a MetaData interface
 */
public class MetaDataImpl implements MetaData {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(MetaDataImpl.class);
    private static final String REQUEST_IS_NULL = "Request select is null or is empty";
    private MongoDbAccessMetadataImpl mongoDbAccess;

    /**
     * MetaDataImpl constructor
     *
     * @param configuration of mongoDB access
     * @param mongoDbAccessFactory
     */
    private MetaDataImpl(MetaDataConfiguration configuration, MongoDbAccessMetadataFactory mongoDbAccessFactory) {
        mongoDbAccess = mongoDbAccessFactory.create(configuration);
    }

    /**
     * @return the MongoDbAccessMetadataImpl
     */
    public MongoDbAccessMetadataImpl getMongoDbAccess() {
        return mongoDbAccess;
    }

    /**
     * Get a new MetaDataImpl instance
     *
     * @param configuration of mongoDB access
     * @param mongoDbAccessFactory
     * @return a new instance of MetaDataImpl
     * @throws IllegalArgumentException if mongoDbAccessFactory is null
     */
    public static MetaData newMetadata(MetaDataConfiguration configuration,
        MongoDbAccessMetadataFactory mongoDbAccessFactory) {
        ParametersChecker.checkParameter("MongoDbAccessFactory cannot be null", mongoDbAccessFactory);
        return new MetaDataImpl(configuration, mongoDbAccessFactory);
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
            final InsertParserMultiple insertParser = new InsertParserMultiple(new MongoDbVarNameAdapter());
            insertParser.parse(insertRequest);
            result = DbRequestFactoryImpl.getInstance().create().execRequest(insertParser, result);
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
            final InsertParserMultiple insertParser = new InsertParserMultiple(new MongoDbVarNameAdapter());
            insertParser.parse(objectGroupRequest);
            insertParser.getRequest().addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
            result = DbRequestFactoryImpl.getInstance().create().execRequest(insertParser, result);
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
        LOGGER.debug("SelectUnitsByQuery/ selectQuery: " + selectQuery);
        return selectMetadataObject(selectQuery, null, null);

    }

    @Override
    public JsonNode selectUnitsById(String selectQuery, String unitId)
        throws InvalidParseOperationException, MetaDataExecutionException,
        MetaDataDocumentSizeException {
        LOGGER.debug("SelectUnitsById/ selectQuery: " + selectQuery);
        return selectMetadataObject(selectQuery, unitId, null);
    }

    @Override
    public JsonNode selectObjectGroupById(String selectQuery, String objectGroupId)
        throws InvalidParseOperationException, MetaDataDocumentSizeException, MetaDataExecutionException {
        LOGGER.debug("SelectObjectGroupById - objectGroupId : " + objectGroupId);
        LOGGER.debug("SelectObjectGroupById - selectQuery : " + selectQuery);
        return selectMetadataObject(selectQuery, objectGroupId,
            Collections.singletonList(BuilderToken.FILTERARGS.OBJECTGROUPS));
    }

    // FIXME P0 : maybe do not encapsulate all exception in a MetaDataExecutionException. We may need to know if it is
    // NOT_FOUND for example
    private JsonNode selectMetadataObject(String selectQuery, String unitOrObjectGroupId,
        List<BuilderToken.FILTERARGS> filters)
        throws MetaDataExecutionException, InvalidParseOperationException,
        MetaDataDocumentSizeException {
        Result result = null;
        JsonNode jsonNodeResponse;
        if (Strings.isNullOrEmpty(selectQuery)) {
            throw new InvalidParseOperationException(REQUEST_IS_NULL);
        }
        try {
            // sanity check:InvalidParseOperationException will be thrown if request select invalid or size is too large
            GlobalDatasParser.sanityRequestCheck(selectQuery);
        } catch (final InvalidParseOperationException eInvalidParseOperationException) {
            throw new MetaDataDocumentSizeException(eInvalidParseOperationException);
        }
        try {
            // parse Select request
            final RequestParserMultiple selectRequest = new SelectParserMultiple(new MongoDbVarNameAdapter());
            selectRequest.parse(JsonHandler.getFromString(selectQuery));
            // Reset $roots (add or override id on roots)
            if (unitOrObjectGroupId != null && !unitOrObjectGroupId.isEmpty()) {
                final RequestMultiple request = selectRequest.getRequest();
                if (request != null) {
                    LOGGER.debug("Reset $roots id with :" + unitOrObjectGroupId);
                    request.resetRoots().addRoots(unitOrObjectGroupId);
                }
            }
            if (filters != null && !filters.isEmpty()) {
                final RequestMultiple request = selectRequest.getRequest();
                if (request != null) {
                    final String[] hints = filters.stream()
                        .map(BuilderToken.FILTERARGS::exactToken)
                        .toArray(String[]::new);
                    LOGGER.debug("Adding given $hint filters: " + Arrays.toString(hints));
                    request.addHintFilter(hints);
                }
            }
            // Execute DSL request
            result = DbRequestFactoryImpl.getInstance().create().execRequest(selectRequest, result);
            jsonNodeResponse = MetadataJsonResponseUtils.populateJSONObjectResponse(result, selectRequest);

        } catch (final InstantiationException | IllegalAccessException | MetaDataAlreadyExistException |
            MetaDataNotFoundException e) {
            LOGGER.error(e);
            throw new MetaDataExecutionException(e);
        }
        return jsonNodeResponse;
    }

    @Override
    public JsonNode updateUnitbyId(String updateQuery, String unitId)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataDocumentSizeException {
        Result result = null;
        JsonNode jsonNodeResponse;
        if (Strings.isNullOrEmpty(updateQuery)) {
            throw new InvalidParseOperationException(REQUEST_IS_NULL);
        }
        try {
            // sanity check:InvalidParseOperationException will be thrown if request select invalid or size is too large
            GlobalDatasParser.sanityRequestCheck(updateQuery);
        } catch (final InvalidParseOperationException eInvalidParseOperationException) {
            throw new MetaDataDocumentSizeException(eInvalidParseOperationException);
        }
        try {
            // parse Update request
            final RequestParserMultiple updateRequest = new UpdateParserMultiple();
            updateRequest.parse(JsonHandler.getFromString(updateQuery));
            // Reset $roots (add or override unit_id on roots)
            if (unitId != null && !unitId.isEmpty()) {
                final RequestMultiple request = updateRequest.getRequest();
                if (request != null) {
                    LOGGER.debug("Reset $roots unit_id by :" + unitId);
                    request.resetRoots().addRoots(unitId);
                }
            }

            final String unitBeforeUpdate = JsonHandler.prettyPrint(getUnitById(unitId));

            // Execute DSL request
            result = DbRequestFactoryImpl.getInstance().create().execRequest(updateRequest, result);

            final String unitAfterUpdate = JsonHandler.prettyPrint(getUnitById(unitId));

            final Map<String, List<String>> diffs = new HashMap<>();
            diffs.put(unitId, getConcernedDiffLines(getUnifiedDiff(unitBeforeUpdate, unitAfterUpdate)));

            jsonNodeResponse = MetadataJsonResponseUtils.populateJSONObjectResponse(result, updateRequest, diffs);
        } catch (final MetaDataExecutionException | InvalidParseOperationException e) {
            LOGGER.error(e);
            throw e;
        } catch (final InstantiationException | MetaDataAlreadyExistException | MetaDataNotFoundException |
            IllegalAccessException e) {
            LOGGER.error(e);
            throw new MetaDataExecutionException(e);
        }
        return jsonNodeResponse;
    }

    private JsonNode getUnitById(String id)
        throws MetaDataDocumentSizeException, MetaDataExecutionException, InvalidParseOperationException {
        final Select select = new Select();
        return selectUnitsById(select.getFinalSelect().toString(), id);
    }

    /**
     * Get unified diff
     *
     * @param original the original value
     * @param revised the revisited value
     * @return unified diff (each list entry is a diff line)
     */
    private List<String> getUnifiedDiff(String original, String revised) {
        final List<String> beforeList = Arrays.asList(original.split("\\n"));
        final List<String> revisedList = Arrays.asList(revised.split("\\n"));

        final Patch<String> patch = DiffUtils.diff(beforeList, revisedList);

        return generateUnifiedDiff(original, revised, beforeList, patch, 1);
    }

    /**
     * Retrieve only + and - line on diff (for logbook lifecycle) regexp = line started by + or - with at least one
     * space after and any character
     *
     * @param diff the unified diff
     * @return + and - lines for logbook lifecycle
     */
    private List<String> getConcernedDiffLines(List<String> diff) {
        final List<String> result = new ArrayList<>();
        for (final String line : diff) {
            if (line.matches("^(\\+|-){1}\\s{1,}.*")) {
                // remove the last character which is a ","
                result.add(line.substring(0, line.length() - 1).replace("\"", ""));
            }
        }
        return result;
    }
}
