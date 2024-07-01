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
package fr.gouv.vitam.common.database.translators.elasticsearch;

import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * Fake VarNameAdapter as in Metadata
 */
public class FakeMetadataVarNameAdapter extends VarNameAdapter {

    /**
     * Empty constructor
     */
    public FakeMetadataVarNameAdapter() {
        // Empty
    }

    @Override
    public boolean metadataAdapter() {
        return true;
    }

    /**
     * @param name as String
     * @return the new name or null if the same
     * @throws InvalidParseOperationException when parsing error
     * @see ParserTokens.PROJECTIONARGS
     */
    @Override
    public String getVariableName(String name) throws InvalidParseOperationException {
        if (name.charAt(0) == ParserTokens.DEFAULT_HASH_PREFIX_CHAR) {
            // Check on prefix (preceding '.')
            int pos = name.indexOf('.');
            final String realname;
            if (pos > 1) {
                realname = name.substring(1, pos);
            } else {
                realname = name.substring(1);
            }
            try {
                final PROJECTIONARGS proj = ParserTokens.PROJECTIONARGS.parse(realname);
                switch (proj) {
                    case ID:
                        // Valid for Unit and OG
                        return VitamDocument.ID;
                    case TENANT:
                        // Valid for Unit and OG
                        return VitamDocument.TENANT_ID;
                    case VERSION:
                        // Valid for Unit and OG (And for VitamDocument items)
                        return VitamDocument.VERSION;
                    case SCORE:
                        return VitamDocument.SCORE;
                    case SEDAVERSION:
                        return VitamDocument.SEDAVERSION;
                    case IMPLEMENTATIONVERSION:
                        return VitamDocument.IMPLEMENTATIONVERSION;
                    case ALL:
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
