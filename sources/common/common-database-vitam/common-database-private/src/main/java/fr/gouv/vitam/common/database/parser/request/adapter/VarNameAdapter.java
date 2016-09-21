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
package fr.gouv.vitam.common.database.parser.request.adapter;

import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * Model for VarNameAdapter
 */
public class VarNameAdapter {

    /**
     * Empty Constructor
     */
    public VarNameAdapter() {
        // Empty
    }

    /**
     *
     * @param name String
     * @return null
     * @throws InvalidParseOperationException invalid parse operation exception
     */
    public String getVariableName(String name) throws InvalidParseOperationException {
        return null;
    }

    /**
     * Set Vars = Value (Json)
     *
     * @param currentObject ObjectNode
     * @param request JsonNode
     * @throws InvalidParseOperationException invalid parse operation exception
     */
    public void setVarsValue(ObjectNode currentObject, JsonNode request) throws InvalidParseOperationException {
        final Iterator<Entry<String, JsonNode>> iterator = request.fields();
        while (iterator.hasNext()) {
            final Entry<String, JsonNode> entry = iterator.next();
            String name = entry.getKey();
            final String newname = getVariableName(name);
            if (newname != null) {
                name = newname;
            }
            currentObject.set(name, entry.getValue());
        }
    }

    /**
     * Set simple Var (no value)
     *
     * @param req QUERY
     * @param currentObject ObjectNode
     * @param request JsonNode
     * @throws InvalidParseOperationException invalid parse operation exception
     */
    public void setVar(QUERY req, ObjectNode currentObject, JsonNode request) throws InvalidParseOperationException {
        String variableName = request.asText();
        final String val = getVariableName(variableName);
        if (val != null) {
            variableName = val;
        }
        currentObject.put(req.exactToken(), variableName);
    }

    /**
     * Set an array of Var (no Value)
     *
     * @param array ArrayNode
     * @throws InvalidParseOperationException invalid parse operation exception
     */
    public void setVarArray(ArrayNode array) throws InvalidParseOperationException {
        final ArrayNode copy = array.deepCopy();
        array.removeAll();
        for (final JsonNode value : copy) {
            final String name = getVariableName(value.asText());
            if (name != null) {
                array.add(name);
            } else {
                array.add(value.asText());
            }
        }
    }

    /**
     * Check if no arguments is using any fix '#' Parameter
     *
     * @param request JsonNode
     * @throws InvalidParseOperationException invalid parse operation exception
     */
    public void checkNoParameter(JsonNode request) throws InvalidParseOperationException {
        final Iterator<Entry<String, JsonNode>> iterator = request.fields();
        while (iterator.hasNext()) {
            final Entry<String, JsonNode> entry = iterator.next();
            if (entry.getKey().charAt(0) == ParserTokens.DEFAULT_HASH_PREFIX_CHAR ||
                entry.getValue().asText().charAt(0) == ParserTokens.DEFAULT_HASH_PREFIX_CHAR) {
                throw new InvalidParseOperationException(
                    "Parameter using '#' not allowed in Rename action: " + request);
            }
        }
    }
}
