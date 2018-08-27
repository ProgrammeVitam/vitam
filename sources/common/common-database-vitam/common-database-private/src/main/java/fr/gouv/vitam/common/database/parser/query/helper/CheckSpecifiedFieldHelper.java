/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
/**
 *
 */
package fr.gouv.vitam.common.database.parser.query.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.database.model.DataType;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;

import java.util.Iterator;

/**
 * Check specified fields helper
 */
public class CheckSpecifiedFieldHelper {

    private static final String ACTION = "$action";
    private static final String SET = "$set";
    private static final String UNSET = "$unset";
    private static final String SETREGEX = "$setregex";
    private static final String ARCHIVE_UNIT_PROFILE = "ArchiveUnitProfile";
    private static final String MANAGEMENT = "#management";
    private static final String IDENTIFIER = "#identifier";
    private static final String ID = "#id";
    private static final String VERSION = "#version";

    /**
     * Constructor.
     */
    public CheckSpecifiedFieldHelper() {
    }

    /**
     * containsSpecifiedField
     * @param query
     * @param dataType
     * @return
     * @throws VitamException
     */
    public static boolean containsSpecifiedField(JsonNode query, DataType dataType) throws VitamException {
        try {
            UpdateParserMultiple parser = new UpdateParserMultiple();
            parser.parse(query);
            ArrayNode actions = (ArrayNode) query.get(ACTION);
            for (JsonNode node : actions) {
                JsonNode fieldNode = node.get(SET);
                if (fieldNode == null) {
                    fieldNode = node.get(UNSET);
                }
                if (fieldNode == null) {
                    fieldNode = node.get(SETREGEX);
                }
                if (fieldNode == null) {
                    throw new IllegalStateException("Invalid Dsl action command.");
                }
                Iterator<String> fieldNames = fieldNode.fieldNames();
                switch (dataType) {
                    case MANAGEMENT:
                        while (fieldNames.hasNext()) {
                            String fieldName = fieldNames.next();
                            if (ARCHIVE_UNIT_PROFILE.equals(fieldName) || fieldName.startsWith(MANAGEMENT)) {
                                return true;
                            }
                        }
                        break;
                    case GRAPH:
                        while (fieldNames.hasNext()) {
                            String fieldName = fieldNames.next();
                            if (MetadataDocumentHelper.getComputedGraphUnitFields().contains(fieldName)) {
                                return true;
                            }
                        }
                        break;
                    case TEMPORARY:
                        while (fieldNames.hasNext()) {
                            String fieldName = fieldNames.next();
                            if (MetadataDocumentHelper.getTemporaryUnitFields().contains(fieldName)) {
                                return true;
                            }
                        }
                        break;
                    case MDD:
                        while (fieldNames.hasNext()) {
                            String fieldName = fieldNames.next();
                            if (fieldName.equals(ID) || fieldName.equals(IDENTIFIER) ||
                                fieldName.equals(VERSION)) {
                                return true;
                            }
                        }
                        break;
                    default:
                }
            }
        } catch (InvalidParseOperationException e) {
            throw new VitamException(e);
        }
        return false;
    }
}
