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
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import org.bson.Document;

import java.util.List;

/**
 * Response filter changing _varname to corresponding #varname according to ParserTokens
 */
public class MongoDbMetadataResponseFilter {

    private static final String VERSION = "versions";

    private MongoDbMetadataResponseFilter() {
        // Empty
    }

    /**
     * Removes a field from document
     *
     * @param document  the document to update
     * @param fieldName the field to remove
     */
    private static final void remove(Document document, String fieldName) {
        document.remove(fieldName);
    }

    private static final void replace(Document document, String originalFieldName, String targetFieldName) {
        final Object value = document.remove(originalFieldName);
        if (value != null) {
            document.append(targetFieldName, value);
        }
    }

    /**
     * This method will modify the document argument in order to filter as output all _varname to corresponding #varname
     * according to ParserTokens
     *
     * @param document of type Document to be modified
     */
    public static final void filterFinalResponse(MetadataDocument<?> document) {
        final boolean isUnit = document instanceof Unit;
        // Fix me change versions
        // filterVersions(document);
        for (final PROJECTIONARGS projection : ParserTokens.PROJECTIONARGS.values()) {
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
                        replace(document, ObjectGroup.NBCHILD, VitamFieldsHelper.nbobjects());
                    }
                    break;
                case STORAGE:
                    filterStorage(document);
                    break;
                case OBJECT:
                    replace(document, MetadataDocument.OG, VitamFieldsHelper.object());
                    break;
                case OPERATIONS:
                    replace(document, MetadataDocument.OPS, VitamFieldsHelper.operations());
                    break;
                case OPI:
                    replace(document, MetadataDocument.OPI, VitamFieldsHelper.initialOperation());
                    break;
                case QUALIFIERS:
                    filterQualifiers(document);
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
                case VERSION:
                    replace(document, MetadataDocument.VERSION, VitamFieldsHelper.version());
                    break;
                case ATOMIC_VERSION:
                    remove(document, MetadataDocument.ATOMIC_VERSION);
                    break;
                case MIN:
                    replace(document, Unit.MINDEPTH, VitamFieldsHelper.min());
                    break;
                case MAX:
                    replace(document, Unit.MAXDEPTH, VitamFieldsHelper.max());
                    break;
                case ALLUNITUPS:
                    replace(document, Unit.UNITUPS, VitamFieldsHelper.allunitups());
                    break;
                case MANAGEMENT:
                    replace(document, Unit.MANAGEMENT, VitamFieldsHelper.management());
                    break;
                case UNITTYPE:
                    replace(document, Unit.UNIT_TYPE, VitamFieldsHelper.unitType());
                    break;
                case UDS:
                    remove(document, Unit.UNITDEPTHS);
                    break;
                case ORIGINATING_AGENCY:
                    replace(document, MetadataDocument.ORIGINATING_AGENCY, VitamFieldsHelper.originatingAgency());
                    break;
                case ORIGINATING_AGENCIES:
                    replace(document, MetadataDocument.ORIGINATING_AGENCIES, VitamFieldsHelper.originatingAgencies());
                    break;
                case SIZE:
                    replace(document, ObjectGroup.OBJECTSIZE, VitamFieldsHelper.size());
                    break;
                case FORMAT:
                    replace(document, ObjectGroup.OBJECTFORMAT, VitamFieldsHelper.format());
                    break;
                case SCORE:
                    replace(document, VitamDocument.SCORE, ParserTokens.PROJECTIONARGS.SCORE.exactToken());
                    break;
                case GRAPH:
                    remove(document, Unit.GRAPH);
                    break;
                case GRAPH_LAST_PERISTED_DATE:
                    remove(document, MetadataDocument.GRAPH_LAST_PERSISTED_DATE);
                    break;
                case PARENT_ORIGINATING_AGENCIES:
                    remove(document, Unit.PARENT_ORIGINATING_AGENCIES);
                    break;
                case HISTORY:
                    replace(document, Unit.HISTORY, VitamFieldsHelper.history());
                    break;
                case ELIMINATION:
                    replace(document, Unit.ELIMINATION, PROJECTIONARGS.ELIMINATION.exactToken());
                    break;
                case COMPUTEDINHERITEDRULES:
                    replace(document, Unit.COMPUTED_INHERITED_RULES, PROJECTIONARGS.COMPUTEDINHERITEDRULES.exactToken());
                    break;
                case SEDAVERSION:
                    replace(document, MetadataDocument.SEDAVERSION, VitamFieldsHelper.sedaVersion());
                    break;
                case IMPLEMENTATIONVERSION:
                    replace(document, MetadataDocument.IMPLEMENTATIONVERSION, VitamFieldsHelper.implementationVersion());
                    break;
                case DUA:
                case ALL:
                default:
                    break;
            }
        }
    }

    private static final void filterStorage(MetadataDocument<?> document) {
        if (document.get(ObjectGroup.STORAGE) != null) {
            Object storage = ((Document) document).get(ObjectGroup.STORAGE);
            replace((Document) storage, MetadataDocument.NBCHILD, VitamFieldsHelper.nbc());
            replace(document, ObjectGroup.STORAGE, VitamFieldsHelper.storage());
        }
    }

    private static final void filterQualifiers(MetadataDocument<?> document) {
        if (document.get(MetadataDocument.QUALIFIERS) != null) {
            replace(document, MetadataDocument.QUALIFIERS, VitamFieldsHelper.qualifiers());
            for (Object qualifier : (List) document.get(VitamFieldsHelper.qualifiers())) {
                replace((Document) qualifier, MetadataDocument.NBCHILD, VitamFieldsHelper.nbc());
                List versions = ((List) ((Document) qualifier).remove(VERSION));
                if (versions != null) {
                    for (Object version : versions) {
                        replace((Document) version, MetadataDocument.ID, VitamFieldsHelper.id());
                        replace((Document) version, MetadataDocument.OPI, VitamFieldsHelper.initialOperation());
                        replace((Document) qualifier, MetadataDocument.NBCHILD, VitamFieldsHelper.nbc());

                        Object storage = ((Document) version).get(ObjectGroup.STORAGE);
                        if (storage != null) {
                            replace((Document) storage, MetadataDocument.NBCHILD, VitamFieldsHelper.nbc());
                            replace((Document) version, ObjectGroup.STORAGE, VitamFieldsHelper.storage());
                        }
                    }
                    ((Document) qualifier).put(VERSION, versions);
                }
            }
        }
    }
}
