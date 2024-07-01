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
package fr.gouv.vitam.common.dsl.schema.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.dsl.schema.DslSchema;
import fr.gouv.vitam.common.dsl.schema.ValidationException;
import fr.gouv.vitam.common.dsl.schema.Validator;
import fr.gouv.vitam.common.dsl.schema.meta.Schema;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Dsl schema validator for multiple queries DSL queries.
 */
public class SelectMultipleSchemaValidator implements DslValidator {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SelectMultipleSchemaValidator.class);

    private static final String MANDATORY_DEPTH_MESSAGE =
        "DSL Graph validation : the query should contain a field $depth or $exactdepth (except on first query when no root is set)";
    private static final String FORBIDDEN_DEPTH_MESSAGE =
        "DSL Graph validation : the first query should not contain a field $depth or $exactdepth when no root is set";

    private final Schema schema;

    /**
     * Constructor
     *
     * @throws IOException thrown when the schema file is not found or invalid
     */
    public SelectMultipleSchemaValidator() throws IOException {
        LOGGER.debug(
            "Loading schema {} from {}",
            DslSchema.SELECT_MULTIPLE.name(),
            DslSchema.SELECT_MULTIPLE.getFilename()
        );
        try (
            final InputStream schemaSource = PropertiesUtils.getResourceAsStream(
                DslSchema.SELECT_MULTIPLE.getFilename()
            )
        ) {
            schema = Schema.getSchema().loadTypes(schemaSource).build();
        }
    }

    @Override
    public void validate(JsonNode dsl) throws ValidationException {
        Validator.validate(schema, "DSL", dsl);
        validateGraph(dsl);
    }

    /**
     * Check if query is a valid stream query or not
     *
     * @param queryJson
     * @throws ValidationException
     */
    public static void validateStreamQuery(JsonNode queryJson) throws ValidationException {
        JsonNode offset = JsonHandler.getNodeByPath(
            queryJson,
            BuilderToken.GLOBAL.FILTER.exactToken() + "." + BuilderToken.SELECTFILTER.OFFSET.exactToken(),
            false
        );
        if (offset != null) {
            throw new ValidationException(
                VitamCodeHelper.toVitamError(
                    VitamCode.GLOBAL_INVALID_DSL,
                    "Cannot use " + BuilderToken.SELECTFILTER.OFFSET.exactToken() + " in this query"
                )
            );
        }
        JsonNode limit = JsonHandler.getNodeByPath(
            queryJson,
            BuilderToken.GLOBAL.FILTER.exactToken() + "." + BuilderToken.SELECTFILTER.LIMIT.exactToken(),
            false
        );
        if (limit != null) {
            throw new ValidationException(
                VitamCodeHelper.toVitamError(
                    VitamCode.GLOBAL_INVALID_DSL,
                    "Cannot use " + BuilderToken.SELECTFILTER.LIMIT.exactToken() + " in this query"
                )
            );
        }
        JsonNode facets = JsonHandler.getNodeByPath(queryJson, BuilderToken.GLOBAL.FACETS.exactToken(), false);
        if (facets != null && facets.elements().hasNext()) {
            throw new ValidationException(
                VitamCodeHelper.toVitamError(
                    VitamCode.GLOBAL_INVALID_DSL,
                    "Cannot use " + BuilderToken.GLOBAL.FACETS.exactToken() + " in this query"
                )
            );
        }
    }

    /**
     * Check if property track_total_hits is already authorized in order to use it in DSL filter
     *
     * @param queryJson
     * @param configAuthorizeTrackTotalHits
     * @throws ValidationException
     */
    public static void checkAuthorizeTrackTotalHits(JsonNode queryJson, boolean configAuthorizeTrackTotalHits)
        throws ValidationException {
        JsonNode dslAuthorizeTrackTotalHits = JsonHandler.getNodeByPath(
            queryJson,
            BuilderToken.GLOBAL.FILTER.exactToken() + "." + BuilderToken.SELECTFILTER.TRACK_TOTAL_HITS.exactToken(),
            false
        );
        if (
            dslAuthorizeTrackTotalHits != null &&
            dslAuthorizeTrackTotalHits.asBoolean() &&
            !configAuthorizeTrackTotalHits
        ) {
            throw new ValidationException(
                VitamCodeHelper.toVitamError(
                    VitamCode.UNAUTHORIZED_PARAMETER_DSL,
                    BuilderToken.SELECTFILTER.TRACK_TOTAL_HITS.exactToken() + " is not authorized!"
                )
            );
        }
    }

    /**
     * Validate graph on dsl query : all queries should contains $depth or $exactdepth except the first query when $root
     * is not set.
     *
     * @param dsl dsl query
     * @throws ValidationException thrwon is graph is invalid
     */
    private void validateGraph(JsonNode dsl) throws ValidationException {
        boolean hasRoot = false;
        if (dsl.has(BuilderToken.GLOBAL.ROOTS.exactToken())) {
            JsonNode roots = dsl.get(BuilderToken.GLOBAL.ROOTS.exactToken());
            if (roots.isArray() && ((ArrayNode) roots).size() > 0) {
                hasRoot = true;
            }
        }

        if (dsl.has(BuilderToken.GLOBAL.QUERY.exactToken())) {
            JsonNode queryNode = dsl.get(BuilderToken.GLOBAL.QUERY.exactToken());
            if (queryNode.isArray()) {
                ArrayNode queries = (ArrayNode) queryNode;
                boolean firstQuery = true;
                for (JsonNode query : queries) {
                    if (firstQuery && !hasRoot) {
                        if (
                            query.has(BuilderToken.QUERYARGS.DEPTH.exactToken()) ||
                            query.has(BuilderToken.QUERYARGS.EXACTDEPTH.exactToken())
                        ) {
                            LOGGER.error(FORBIDDEN_DEPTH_MESSAGE);
                            throw new ValidationException(
                                VitamCodeHelper.toVitamError(VitamCode.GLOBAL_INVALID_DSL, FORBIDDEN_DEPTH_MESSAGE)
                            );
                        }
                    } else {
                        if (
                            !query.has(BuilderToken.QUERYARGS.DEPTH.exactToken()) &&
                            !query.has(BuilderToken.QUERYARGS.EXACTDEPTH.exactToken())
                        ) {
                            LOGGER.error(MANDATORY_DEPTH_MESSAGE);
                            throw new ValidationException(
                                VitamCodeHelper.toVitamError(VitamCode.GLOBAL_INVALID_DSL, MANDATORY_DEPTH_MESSAGE)
                            );
                        }
                    }
                    firstQuery = false;
                }
            }
        }
    }
}
