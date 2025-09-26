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

package fr.gouv.vitam.metadata.core.database.collections;

import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import org.apache.commons.lang3.StringUtils;

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
     * @param name as String
     * @return the new name or null if the same
     * @throws InvalidParseOperationException when parsing error
     * @see PROJECTIONARGS
     */
    @Override
    public String getVariableName(String name) throws InvalidParseOperationException {
        String[] fields = StringUtils.split(name, '.');
        StringBuilder sb = new StringBuilder();
        for (String field : fields) {
            if (!sb.isEmpty()) {
                sb.append(".");
            }
            sb.append(resolveField(field));
        }
        String resolvedName = sb.toString();
        if (resolvedName.equals(name)) {
            return null;
        }
        return resolvedName;
    }

    private String resolveField(String field) throws InvalidParseOperationException {
        if (field.isEmpty()) {
            throw new InvalidParseOperationException("Empty field name");
        }
        char firstChar = field.charAt(0);
        if (firstChar != ParserTokens.DEFAULT_HASH_PREFIX_CHAR) {
            return field;
        }
        try {
            // Fixme: use proper schema for field resolving...
            final PROJECTIONARGS proj = PROJECTIONARGS.parse(field.substring(1));
            return switch (proj) {
                case ID -> VitamDocument.ID; // Valid for Unit and OG
                case QUALIFIERS -> MetadataDocument.QUALIFIERS; // Valid for OG
                case NBUNITS -> Unit.NBCHILD; // Valid for Unit
                case NBOBJECTS -> ObjectGroup.NBCHILD; // Valid for OG
                case NBC -> MetadataDocument.NBCHILD;
                case SIZE -> ObjectGroup.OBJECTSIZE; // Valid for OG
                case TYPE -> MetadataDocument.TYPE; // Valid for Unit and OG
                case TENANT -> VitamDocument.TENANT_ID; // Valid for Unit and OG
                case OBJECT -> MetadataDocument.OG; // Valid for Unit
                case UNITUPS -> MetadataDocument.UP; // Valid for Unit and OG
                case MIN -> Unit.MINDEPTH; // Valid for Unit
                case MAX -> Unit.MAXDEPTH; // Valid for Unit
                case ALLUNITUPS -> Unit.UNITUPS; // Valid for Unit
                case UNITTYPE -> Unit.UNIT_TYPE; // Valid for Unit
                case MANAGEMENT -> Unit.MANAGEMENT; // Valid for Unit
                case OPERATIONS -> MetadataDocument.OPS; // Valid for Unit and OG
                case OPI -> MetadataDocument.OPI; // Valid for Unit and OG
                case ORIGINATING_AGENCY -> MetadataDocument.ORIGINATING_AGENCY; // Valid for Unit and OG
                case ORIGINATING_AGENCIES -> MetadataDocument.ORIGINATING_AGENCIES; // Valid for Unit and OG
                case VERSION -> MetadataDocument.VERSION; // Valid for Unit and OG (And for VitamDocument items)
                case ATOMIC_VERSION -> MetadataDocument.ATOMIC_VERSION; // Valid for Unit and OG (And for VitamDocument items)
                case STORAGE -> ObjectGroup.STORAGE; // Valid for OG an Unit
                case SCORE -> VitamDocument.SCORE;
                case UDS -> Unit.UNITDEPTHS;
                case GRAPH -> Unit.GRAPH;
                case ELIMINATION -> Unit.ELIMINATION;
                case COMPUTEDINHERITEDRULES -> Unit.COMPUTED_INHERITED_RULES;
                case VALIDCOMPUTEDINHERITEDRULES -> Unit.VALID_COMPUTED_INHERITED_RULES;
                case OPTS -> Unit.OPERATION_TRANSFERS;
                case GRAPH_LAST_PERSISTED_DATE -> MetadataDocument.GRAPH_LAST_PERSISTED_DATE;
                case HISTORY -> Unit.HISTORY;
                case SEDAVERSION -> VitamDocument.SEDAVERSION;
                case IMPLEMENTATIONVERSION -> VitamDocument.IMPLEMENTATIONVERSION;
                case APPROXIMATE_CREATION_DATE -> MetadataDocument.APPROXIMATE_CREATION_DATE;
                case APPROXIMATE_UPDATE_DATE -> MetadataDocument.APPROXIMATE_UPDATE_DATE;
                case BATCHID -> MetadataDocument.BATCH_ID;
                case MANAGEMENTCONTRACTID -> Unit.MANAGEMENT_CONTRACT_ID;
                case UPLOADPATH -> MetadataDocument.UPLOAD_PATH;
                case FORMAT -> ObjectGroup.OBJECTFORMAT; // Deprecated, to be removed
                case USAGE, // Deprecated, to be removed
                    // Reserved for LogbookOperation & LFC
                    LAST_PERSISTED_DATE -> throw new InvalidParseOperationException("Invalid field '" + field + "'");
            };
        } catch (final IllegalArgumentException e) {
            throw new InvalidParseOperationException("Invalid field name '" + field + "'", e);
        }
    }
}
