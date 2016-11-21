/**
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
 */
package fr.gouv.vitam.metadata.core.database.collections;

import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS;
import javassist.expr.Instanceof;

/**
 * Response filter changing _varname to corresponding #varname according to ParserTokens
 */
public class MongoDbMetadataResponseFilter {

    private MongoDbMetadataResponseFilter() {
        // Empty
    }

    private static final void replace(MetadataDocument<?> document, String originalFieldName, String targetFieldName) {
        Object value = document.remove(originalFieldName);
        if (value != null) {
            document.append(targetFieldName, value);
        }
    }

    /**
     * This method will modify the document argument in order to filter as output all _varname to corresponding #varname
     * according to ParserTokens
     * 
     * @param document
     */
    public static final void filterFinalResponse(MetadataDocument<?> document) {
        boolean isUnit = document instanceof Unit; 
        for (PROJECTIONARGS projection : ParserTokens.PROJECTIONARGS.values()) {
            switch (projection) {
                case ID:
                    replace(document, MetadataDocument.ID, VitamFieldsHelper.id());
                    break;
                case NBUNITS:
                    if (isUnit) {
                        replace(document, Unit.NBCHILD, VitamFieldsHelper.nbunits());
                    }
                    break;
                case NBOBJECTS:
                    if (!isUnit) {
                        replace(document, ObjectGroup.NB_COPY, VitamFieldsHelper.nbobjects());
                    }
                    break;
                case OBJECT:
                    replace(document, MetadataDocument.OG, VitamFieldsHelper.object());
                    break;
                case OPERATIONS:
                    replace(document, MetadataDocument.OPS, VitamFieldsHelper.operations());
                    break;
                case QUALIFIERS:
                    replace(document, MetadataDocument.QUALIFIERS, VitamFieldsHelper.qualifiers());
                    break;
                case TYPE:
                    replace(document, MetadataDocument.TYPE, VitamFieldsHelper.type());
                    break;
                case TENANT:
                    replace(document, MetadataDocument.TENANT_ID, VitamFieldsHelper.tenant());
                    break;
                case UNITUPS:
                    replace(document, MetadataDocument.UP, VitamFieldsHelper.unitups());
                    break;
                case ALLUNITUPS:
                    replace(document, Unit.UNITUPS, VitamFieldsHelper.allunitups());
                    break;
                case SIZE:
                case DUA:
                case FORMAT:
                case ALL:
                default:
                    break;
            }
        }
    }
}
