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
package fr.gouv.vitam.common.database.parser.request.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Model for VarNameAdapter for Insert
 */
public class VarNameInsertAdapter extends VarNameAdapter {

    VarNameAdapter adapter;

    /**
     * Constructor
     *
     * @param adapter VarNameAdapter
     */
    public VarNameInsertAdapter(VarNameAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public boolean metadataAdapter() {
        return adapter.metadataAdapter();
    }

    public String getVariableName(String name) throws InvalidParseOperationException {
        if (!adapter.metadataAdapter() && PROJECTIONARGS.notAllowedOnSet(name)) {
            throw new InvalidParseOperationException("Name not allowed in Insert: " + name);
        }
        if (!this.metadataAdapter() && ParserTokens.PROJECTIONARGS.notAllowedOnSetExternal(name)) {
            throw new InvalidParseOperationException("Illegal variable name found: " + name);
        }
        return adapter.getVariableName(name);
    }

    /**
     * Check for Insert from Builder
     *
     * @param rootNode JsonNode
     * @return the new JsonNode in replacement of rootNode
     * @throws InvalidParseOperationException if rootNode could not parse to JSON
     */
    public JsonNode getFixedVarNameJsonNode(JsonNode rootNode) throws InvalidParseOperationException {
        // Note: some values are not allowed, as #id
        if (rootNode instanceof ArrayNode) {
            final ArrayNode object = JsonHandler.createArrayNode();
            final Iterator<JsonNode> fieldIterator = rootNode.elements();
            while (fieldIterator.hasNext()) {
                final JsonNode node = getFixedVarNameJsonNode(fieldIterator.next());
                object.add(node);
            }
            return object;
        }
        if (rootNode.isObject()) {
            final ObjectNode object = JsonHandler.createObjectNode();
            final Iterator<Entry<String, JsonNode>> fieldIterator = rootNode.fields();
            while (fieldIterator.hasNext()) {
                final Entry<String, JsonNode> entry = fieldIterator.next();
                String name = entry.getKey();
                final String newname = getVariableName(name);
                if (newname != null) {
                    name = newname;
                }
                if (entry.getValue().isObject() || entry.getValue().isArray()) {
                    object.set(name, getFixedVarNameJsonNode(entry.getValue()));
                } else {
                    object.set(name, entry.getValue());
                }
            }
            return object;
        }
        return rootNode;
    }
}
