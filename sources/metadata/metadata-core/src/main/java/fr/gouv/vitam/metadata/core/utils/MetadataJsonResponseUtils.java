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
package fr.gouv.vitam.metadata.core.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.metadata.core.database.collections.Result;

import java.util.List;
import java.util.Map;

/**
 * The purpose of this class is to centralize the generation of a metadata json response
 */
public final class MetadataJsonResponseUtils {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetadataJsonResponseUtils.class);

    /**
     * Hide default public constructor
     */
    private MetadataJsonResponseUtils() {
        // Nothing to do
    }

    /**
     * create Json response
     *
     * @param result contains final unit(s)/ObjectGroup(s) list <br>
     * can be empty
     * @param selectRequest the select request of type RequestParserMultiple
     * @return ArrayNode {$hits{},$context{},$result:[{}....{}],} <br>
     * $context will be added later (Access)</br>
     * $result array of units or ObjectGroup (can be empty)
     * @throws InvalidParseOperationException thrown when json query is not valid
     */
    public static ArrayNode populateJSONObjectResponse(Result result, RequestParserMultiple selectRequest)
        throws InvalidParseOperationException {
        ArrayNode jsonListResponse = JsonHandler.createArrayNode();
        // TODO P1 : review if statement because if result.getFinal().get("Result") == null and selectRequest
        // is instanceof SelectParserMultiple, we have an IllegalArgumentException during call to
        // getMetadataJsonObject(). This should not be the case
        // the nbResult problem originates from DbRequest.execRequest:Result result = roots;
        if (
            result != null &&
            result.getNbResult() > 0 &&
            selectRequest instanceof SelectParserMultiple &&
            result.hasFinalResult()
        ) {
            LOGGER.debug("Result document: " + result.getFinal().toString());
            jsonListResponse = (ArrayNode) getMetadataJsonObject(result.getListFiltered());
        }
        LOGGER.debug("MetaDataImpl / selectUnitsByQuery /Results: " + jsonListResponse.toString());
        return jsonListResponse;
    }

    private static JsonNode getMetadataJsonObject(Object unitOrObjectGroup) throws InvalidParseOperationException {
        return JsonHandler.toJsonNode(unitOrObjectGroup);
    }

    /**
     * create Json response with diff information
     *
     * @param result contains final unit(s)/ObjectGroup(s) list <br>
     * can be empty
     * @param diff the diff map list with the unit id as key and the diff list as value
     * @return JsonNode {$hits{},$context{},$result:[{_id:...,_diff:...}},...{}]} <br>
     * $context will be added later (Access)</br>
     * $result array of units or ObjectGroup (can be empty)
     */
    public static ArrayNode populateJSONObjectResponse(Result result, Map<String, List<String>> diff) {
        ArrayNode arrayJsonListResponse = JsonHandler.createArrayNode();
        if (result != null && result.getNbResult() > 0) {
            arrayJsonListResponse = getJsonDiff(diff);
        }
        LOGGER.debug("populateJSONObjectResponse: " + arrayJsonListResponse.toString());
        return arrayJsonListResponse;
    }

    private static ArrayNode getJsonDiff(Map<String, List<String>> diff) {
        final ArrayNode diffArrayNode = JsonHandler.createArrayNode();
        for (final String id : diff.keySet()) {
            final ObjectNode diffNode = JsonHandler.createObjectNode();
            diffNode.put("#id", id);
            diffNode.put("#diff", String.join("\n", diff.get(id)));
            diffArrayNode.add(diffNode);
        }
        return diffArrayNode;
    }
}
