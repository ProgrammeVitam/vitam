/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.collect.internal.helpers.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

import java.util.Iterator;
import java.util.Map;

public class CollectVarNameAdapter extends VarNameAdapter {

    public static final String OPI = "_opi";
    public static final String MGT = "_mgt";
    public static final String MGT_APPRAISAL_RULE_RULES_RULE = "_mgt.AppraisalRule.Rules.Rule";
    public static final String ID = "_id";
    public static final String UP = "_up";

    @Override
    public void setVarsValue(ObjectNode currentObject, JsonNode request) throws InvalidParseOperationException {
        final Iterator<Map.Entry<String, JsonNode>> iterator = request.fields();
        while (iterator.hasNext()) {
            final Map.Entry<String, JsonNode> entry = iterator.next();
            String name = entry.getKey();

            final String newname = getVariableName(name);
            if (newname != null) {
                name = newname;
                currentObject.set(name, entry.getValue());
            } else if ((name.charAt(0) != ParserTokens.DEFAULT_HASH_PREFIX_CHAR) && (name.charAt(0) != ParserTokens.DEFAULT_UNDERSCORE_PREFIX_CHAR)) {
                currentObject.set(name, entry.getValue());
            }
        }
    }

    @Override
    public String getVariableName(String name) throws InvalidParseOperationException {
        if (name.charAt(0) == ParserTokens.DEFAULT_HASH_PREFIX_CHAR) {
            // Check on prefix (preceding '.')
            int pos = name.indexOf('.');
            final String realname;
            final String extension;
            if (pos > 1) {
                realname = name.substring(1, pos);
                extension = name.substring(pos);
            } else {
                realname = name.substring(1);
                extension = "";
            }
            try {
                final ParserTokens.PROJECTIONARGS proj = ParserTokens.PROJECTIONARGS.parse(realname);
                switch (proj) {
                    case DUA:
                        return MGT_APPRAISAL_RULE_RULES_RULE;
                    case OPI:
                        return OPI;
                    case ID:
                        return ID;
                    case UNITUPS:
                        return UP;
                    case MANAGEMENT:
                        return MGT + extension;
                    default:
                        break;
                }

            } catch (final IllegalArgumentException e) {
                 throw new InvalidParseOperationException("Name: " + name, e);
            }
        }
        return null;
    }
}
