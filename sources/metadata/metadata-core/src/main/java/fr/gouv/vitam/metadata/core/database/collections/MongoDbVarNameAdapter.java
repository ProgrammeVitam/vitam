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
package fr.gouv.vitam.metadata.core.database.collections;

import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

/**
 * Model for VarNameAdapter
 */
public class MongoDbVarNameAdapter extends VarNameAdapter {

    /**
     * Constructor
     */
    public MongoDbVarNameAdapter() {
        // Empty constructor
    }

    @Override
    public boolean metadataAdapter() {
        return true;
    }

    /**
     * @see ParserTokens.PROJECTIONARGS
     * @param name as String
     * @return the new name or null if the same 
     * @throws InvalidParseOperationException when parsing error
     */
    @Override
    public String getVariableName(String name) throws InvalidParseOperationException {
        // FIXME P1 INSERT should generate #id, #mgt, ...
        /*
         * final String newname = null; newname = super.getVariableName(name); if (newname != null) { return newname; }
         */
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
                final PROJECTIONARGS proj = ParserTokens.PROJECTIONARGS.parse(realname);
                switch (proj) {
                    case DUA:
                        // Valid for Unit
                        return Unit.APPRAISALRULES + extension;
                    case FORMAT:
                        // Valid for OG
                        // FIXME P2 not valid
                        return ObjectGroup.OBJECTFORMAT + extension;
                    case ID:
                        // Valid for Unit and OG
                        return MetadataDocument.ID;
                    case QUALIFIERS:
                        // Valid for OG
                        return MetadataDocument.QUALIFIERS + extension;
                    case NBUNITS:
                        // Valid for Unit
                        return Unit.NBCHILD;
                    case NBOBJECTS:
                        // Valid for OG
                        return ObjectGroup.NB_COPY;
                    case SIZE:
                        // Valid for OG
                        // FIXME P2 not valid
                        return ObjectGroup.OBJECTSIZE;
                    case TYPE:
                        // Valid for Unit and OG
                        return MetadataDocument.TYPE;
                    case TENANT:
                        // Valid for Unit and OG
                        return MetadataDocument.TENANT_ID;
                    case OBJECT:
                        // Valid for Unit
                        return MetadataDocument.OG;
                    case UNITUPS:
                        // Valid for Unit and OG
                        return MetadataDocument.UP;
                    case MIN:
                        // Valid for Unit
                        return Unit.MINDEPTH;
                    case MAX:
                        // Valid for Unit
                        return Unit.MAXDEPTH;
                    case ALLUNITUPS:
                        // Valid for Unit
                        return Unit.UNITUPS;
                    case MANAGEMENT:
                        // Valid for Unit
                        return Unit.MANAGEMENT + extension;
                    case OPERATIONS:
                        // Valid for Unit and OG
                        return MetadataDocument.OPS;
                    case ALL:
                    default:
                        break;
                }
            } catch (final IllegalArgumentException e) {
                throw new InvalidParseOperationException(e);
            }
        }
        return null;
    }
}
