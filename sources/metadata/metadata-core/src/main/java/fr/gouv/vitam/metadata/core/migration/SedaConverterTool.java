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
package fr.gouv.vitam.metadata.core.migration;

import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * utils for converting objects from seda 2.0 to seda 2.1 spec
 */
public class SedaConverterTool {

    private enum FieldsTreeToTransformToList {
        FilePlanPosition,
        SystemId,
        OriginatingSystemId,
        ArchivalAgencyArchiveUnitIdentifier,
        OriginatingAgencyArchiveUnitIdentifier,
        TransferringAgencyArchiveUnitIdentifier,
        Language,
        AuthorizedAgent,
        Signature("Signer");

        String[] fields;
        FieldsTreeToTransformToList(String... _fields){
            fields = _fields;
        }

        String[] getFields(){
            return fields;
        }
    };

    private enum ObjectFieldsToDelete {
        Signature("DateSignature"),
        Root("RestrictionRuleIdRef", "RestrictionValue", "RestrictionEndDate", "Href");
        String[] fields;
        ObjectFieldsToDelete(String... _fields){
            fields = _fields;
        }

        String[] getFields(){
            return fields;
        }
    }


    /**
     * convert given unit from seda 2.0 to seda 2.1 schema compatible
     * @param unit unit object to convert
     * @return unit converted to seda 2.1 schema (same reference given in input)
     */
    public static Unit convertUnitToSeda21(Unit unit) {

        for(ObjectFieldsToDelete objectWrapperFieldsDelete : ObjectFieldsToDelete.values()) {
            //remove first level fields
            if(ObjectFieldsToDelete.Root.compareTo(objectWrapperFieldsDelete) == 0){
                for(String field : objectWrapperFieldsDelete.getFields()){
                    if(unit.get(field) != null) {
                        unit.remove(field);
                    }
                }
            }
            //remove embedded fields
            Object object = unit.get(objectWrapperFieldsDelete.name());
            if (object != null && object instanceof  Document) {
                Document document = (Document)object;
                for(String field : objectWrapperFieldsDelete.getFields()){
                    if(document.get(field) != null) {
                        document.remove(field);
                    }
                }
            }
        }
        //update fields
        for(FieldsTreeToTransformToList firstLevelField : FieldsTreeToTransformToList.values()) {
            Object object = unit.get(firstLevelField.name());
            if (object != null && !object.getClass().isArray() &&
                !object.getClass().isAssignableFrom(List.class) &&
                !object.getClass().isAssignableFrom(ArrayList.class)) {

                if(object instanceof  Document &&
                    firstLevelField.getFields() != null &&
                    firstLevelField.getFields().length > 0){

                    Document document = (Document)object;
                    for(String subField : firstLevelField.getFields()) {
                        Object subFieldValue = document.get(subField);
                        if(subFieldValue != null){
                               document.replace(subField, Arrays.asList(subFieldValue));
                        }
                    }
                }

                unit.replace(firstLevelField.name(), Arrays.asList(object));
            }
        }

        return unit;
    }
}
