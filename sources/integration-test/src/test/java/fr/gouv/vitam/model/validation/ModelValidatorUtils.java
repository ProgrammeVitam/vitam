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
package fr.gouv.vitam.model.validation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.collections.DynamicParserTokens;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.collections.VitamDescriptionResolver;
import fr.gouv.vitam.common.database.collections.VitamDescriptionType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.OntologyType;
import fr.gouv.vitam.common.ontology.OntologyTestHelper;
import fr.gouv.vitam.model.validation.jsonschema.JsonSchemaField;
import fr.gouv.vitam.model.validation.jsonschema.JsonSchemaFieldParser;
import fr.gouv.vitam.model.validation.mapping.ElasticsearchMappingParser;
import fr.gouv.vitam.model.validation.mapping.ElasticsearchMappingType;
import fr.gouv.vitam.model.validation.pojo.PojoModel;
import fr.gouv.vitam.model.validation.pojo.PojoModelExtractor;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.SoftAssertions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

public class ModelValidatorUtils {

    private static final TypeReference<List<OntologyModel>> LIST_TYPE_REFERENCE = new TypeReference<
        List<OntologyModel>
    >() {};

    private ModelValidatorUtils() {}

    public static void validateDataModel(VitamCollection vitamCollection) throws Exception {
        ParametersChecker.checkParameter("missing params", vitamCollection);
        internalValidateDataModel(null, null, vitamCollection, null);
    }

    public static void validateDataModel(InputStream mappingFileInputStream, VitamCollection vitamCollection)
        throws Exception {
        ParametersChecker.checkParameter("missing params", mappingFileInputStream, vitamCollection);
        internalValidateDataModel(null, mappingFileInputStream, vitamCollection, null);
    }

    public static void validateDataModel(
        Class<?> clazz,
        InputStream mappingFileInputStream,
        VitamCollection vitamCollection,
        InputStream jsonSchemaInputStream
    ) throws Exception {
        ParametersChecker.checkParameter(
            "missing params",
            clazz,
            mappingFileInputStream,
            vitamCollection,
            jsonSchemaInputStream
        );
        internalValidateDataModel(clazz, mappingFileInputStream, vitamCollection, jsonSchemaInputStream);
    }

    private static void internalValidateDataModel(
        Class<?> clazz,
        InputStream mappingFileInputStream,
        VitamCollection vitamCollection,
        InputStream jsonSchemaInputStream
    ) throws Exception {
        SoftAssertions softly = new SoftAssertions();
        try {
            String collectionName = vitamCollection.getClasz().getSimpleName();
            List<OntologyModel> ontologyModels = loadOntology(collectionName);

            Map<String, VitamDescriptionType> descriptionTypeByName = Maps.filterKeys(
                vitamCollection.getVitamDescriptionResolver().getDescriptionTypeByStaticName(),
                key -> !Objects.equals(key, "Title.keyword")
            );

            // Validate vitam-description file VS ontology file
            validateVitamDescriptionAgainstOntology(descriptionTypeByName, ontologyModels, softly);

            // Validate vitam-description file VS pojo model
            if (clazz != null) {
                Map<String, String> externalToInternalMap = ontologyModels
                    .stream()
                    .filter(ontologyModel -> StringUtils.isNoneEmpty(ontologyModel.getApiField()))
                    .collect(toMap(OntologyModel::getApiField, OntologyModel::getIdentifier));

                PojoModelExtractor pojoModelExtractor = new PojoModelExtractor();
                Map<String, PojoModel> models = pojoModelExtractor
                    .extractPojoModels(clazz)
                    .stream()
                    .collect(
                        toMap(
                            model ->
                                externalToInternalMap.containsKey(model.getFullPath())
                                    ? externalToInternalMap.get(model.getFullPath())
                                    : model.getFullPath(),
                            model -> model
                        )
                    );

                validateVitamDescriptionAgainstPojoModels(models, descriptionTypeByName, softly);
            }

            // Validate vitam-description file VS elasticsearch mapping file
            if (mappingFileInputStream != null) {
                ElasticsearchMappingParser elasticsearchMappingParser = new ElasticsearchMappingParser();
                Map<String, ElasticsearchMappingType> mappingTypes = elasticsearchMappingParser.parseMapping(
                    JsonHandler.getFromInputStream(mappingFileInputStream)
                );

                validateVitamDescriptionAgainstElasticsearchMapping(descriptionTypeByName, mappingTypes, softly);
            }

            if (jsonSchemaInputStream != null) {
                Map<String, JsonSchemaField> schemaFieldMap = JsonSchemaFieldParser.parseJsonSchemaFields(
                    jsonSchemaInputStream
                );

                validateVitamDescriptionAgainstJsonSchema(softly, descriptionTypeByName, schemaFieldMap);
            }
        } finally {
            softly.assertAll();
        }
    }

    private static List<OntologyModel> loadOntology(String collectionName)
        throws IOException, InvalidParseOperationException {
        // Read vitam ontology (added as test resource from ansible deployment directory)
        InputStream resourceAsStream = OntologyTestHelper.loadOntologies();

        List<OntologyModel> ontologyModels = JsonHandler.getFromInputStreamAsTypeReference(
            resourceAsStream,
            LIST_TYPE_REFERENCE
        );

        return ontologyModels
            .stream()
            .filter(ontologyModel -> ontologyModel.getCollections().contains(collectionName))
            .collect(Collectors.toList());
    }

    private static void validateVitamDescriptionAgainstPojoModels(
        Map<String, PojoModel> models,
        Map<String, VitamDescriptionType> descriptionTypeByName,
        SoftAssertions softly
    ) {
        SetUtils.SetView<String> missingKeys = SetUtils.difference(models.keySet(), descriptionTypeByName.keySet());
        softly
            .assertThat(missingKeys)
            .withFailMessage(
                "Missing keys " + missingKeys + " from vitam description file," + " but are found in pojo models"
            )
            .isEmpty();

        SetUtils.SetView<String> unexpectedKeys = SetUtils.difference(descriptionTypeByName.keySet(), models.keySet());
        softly
            .assertThat(unexpectedKeys)
            .withFailMessage(
                "Unexpected keys " +
                unexpectedKeys +
                " in vitam description file" +
                " that are not found in pojo models"
            )
            .isEmpty();

        SetUtils.SetView<String> commonKeys = SetUtils.intersection(descriptionTypeByName.keySet(), models.keySet());

        for (String entryName : commonKeys) {
            VitamDescriptionType vitamDescriptionType = descriptionTypeByName.get(entryName);
            PojoModel model = models.get(entryName);

            VitamDescriptionType.VitamCardinality expectedCardinality = model.isArray()
                ? VitamDescriptionType.VitamCardinality.many
                : VitamDescriptionType.VitamCardinality.one;
            softly
                .assertThat(vitamDescriptionType.getCardinality())
                .withFailMessage(
                    "Cardinality mismatch for field " +
                    entryName +
                    ". From pojo model: " +
                    expectedCardinality +
                    " != declared cardinality in vitam description file: " +
                    vitamDescriptionType.getCardinality()
                )
                .isEqualTo(expectedCardinality);

            List<VitamDescriptionType.VitamType> validVitamTypes;
            switch (model.getModelType()) {
                case ENUM:
                    validVitamTypes = Collections.singletonList(VitamDescriptionType.VitamType.keyword);
                    break;
                case STRING:
                    validVitamTypes = Arrays.asList(
                        VitamDescriptionType.VitamType.keyword,
                        VitamDescriptionType.VitamType.text,
                        VitamDescriptionType.VitamType.datetime
                    );
                    break;
                case LONG:
                    validVitamTypes = Collections.singletonList(VitamDescriptionType.VitamType.signed_long);
                    break;
                case DOUBLE:
                    validVitamTypes = Collections.singletonList(VitamDescriptionType.VitamType.signed_double);
                    break;
                case BOOLEAN:
                    validVitamTypes = Collections.singletonList(VitamDescriptionType.VitamType.bool);
                    break;
                case OBJECT:
                    validVitamTypes = Arrays.asList(
                        VitamDescriptionType.VitamType.object,
                        VitamDescriptionType.VitamType.nested_object
                    );
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + model.getModelType());
            }

            assertThat(vitamDescriptionType.getType())
                .withFailMessage(
                    "Type mismatch for field " +
                    entryName +
                    ". From pojo model: " +
                    model.getModelType() +
                    ", from vitam description file: " +
                    vitamDescriptionType.getType()
                )
                .isIn(validVitamTypes);
        }
    }

    private static void validateVitamDescriptionAgainstOntology(
        Map<String, VitamDescriptionType> descriptionTypeByName,
        List<OntologyModel> ontologyModels,
        SoftAssertions softly
    ) {
        // Only consider leaves (objects are skipped)
        List<VitamDescriptionType> leafDescriptionTypes = descriptionTypeByName
            .values()
            .stream()
            .filter(
                descriptionType ->
                    descriptionType.getType() != VitamDescriptionType.VitamType.object &&
                    descriptionType.getType() != VitamDescriptionType.VitamType.nested_object
            )
            .collect(Collectors.toList());

        // Map description types by simple field name (not full path)
        MultiValuedMap<String, VitamDescriptionType> descriptionByFieldName = new ArrayListValuedHashMap<>();
        for (VitamDescriptionType descriptionType : leafDescriptionTypes) {
            if (descriptionType.getPath().equals("Title.keyword")) {
                // Special field
                continue;
            }
            String fieldName = descriptionType.getPath().substring(descriptionType.getPath().lastIndexOf('.') + 1);
            descriptionByFieldName.put(fieldName, descriptionType);
        }

        // Map ontologies by fieldName
        Map<String, OntologyModel> ontologyByFieldName = ontologyModels
            .stream()
            .collect(toMap(OntologyModel::getIdentifier, ontologyModel -> ontologyModel));

        List<String> errors = new ArrayList<>();

        SetUtils.SetView<String> missingFieldNames = SetUtils.difference(
            descriptionByFieldName.keySet(),
            ontologyByFieldName.keySet()
        );
        softly
            .assertThat(missingFieldNames)
            .withFailMessage(
                "Missing field names " +
                missingFieldNames +
                " from ontology " +
                " but are found in vitam description file"
            )
            .isEmpty();

        SetUtils.SetView<String> unexpectedFieldNames = SetUtils.difference(
            ontologyByFieldName.keySet(),
            descriptionByFieldName.keySet()
        );
        softly
            .assertThat(unexpectedFieldNames)
            .withFailMessage(
                "Unexpected field names " +
                unexpectedFieldNames +
                " in ontology " +
                " that are missing from vitam description file"
            )
            .isEmpty();

        SetUtils.SetView<String> commonFieldNames = SetUtils.intersection(
            ontologyByFieldName.keySet(),
            descriptionByFieldName.keySet()
        );

        for (String commonFieldName : commonFieldNames) {
            OntologyModel ontologyModel = ontologyByFieldName.get(commonFieldName);

            for (VitamDescriptionType descriptionType : descriptionByFieldName.get(commonFieldName)) {
                OntologyType expectedOntologyType;
                switch (descriptionType.getType()) {
                    case datetime:
                        expectedOntologyType = OntologyType.DATE;
                        break;
                    case keyword:
                        expectedOntologyType = OntologyType.KEYWORD;
                        break;
                    case text:
                        expectedOntologyType = OntologyType.TEXT;
                        break;
                    case signed_long:
                        expectedOntologyType = OntologyType.LONG;
                        break;
                    case signed_double:
                        expectedOntologyType = OntologyType.DOUBLE;
                        break;
                    case bool:
                        expectedOntologyType = OntologyType.BOOLEAN;
                        break;
                    case object:
                    case nested_object:
                    // Already filtered
                    default:
                        throw new IllegalStateException("Unexpected value: " + descriptionType.getType());
                }

                softly
                    .assertThat(ontologyModel.getType())
                    .withFailMessage(
                        "Type mismatch for field: " +
                        commonFieldName +
                        ". Vitam description type: " +
                        descriptionType.getType() +
                        ", Ontology type: " +
                        ontologyModel.getType()
                    )
                    .isEqualTo(expectedOntologyType);
            }
        }

        assertThat(errors).isEmpty();
    }

    private static void validateVitamDescriptionAgainstElasticsearchMapping(
        Map<String, VitamDescriptionType> descriptionTypeByName,
        Map<String, ElasticsearchMappingType> mappingTypes,
        SoftAssertions softly
    ) {
        SetUtils.SetView<String> missingKeys = SetUtils.difference(
            descriptionTypeByName.keySet(),
            mappingTypes.keySet()
        );
        for (String missingKey : missingKeys) {
            if (missingKey.equals("_id")) {
                // _id is forbidden in ES mapping
                continue;
            }
            softly.fail("Missing key '" + missingKey + "' from ES mapping");
        }

        SetUtils.SetView<String> unexpectedKeys = SetUtils.difference(
            mappingTypes.keySet(),
            descriptionTypeByName.keySet()
        );
        for (String unexpectedKey : unexpectedKeys) {
            softly.fail("Unexpected key '" + unexpectedKey + "' in ES mapping");
        }

        DynamicParserTokens parserTokens = new DynamicParserTokens(
            new VitamDescriptionResolver(new ArrayList<>(descriptionTypeByName.values())),
            Collections.emptyList()
        );
        for (Map.Entry<String, ElasticsearchMappingType> entry : mappingTypes.entrySet()) {
            String fieldName = entry.getKey();
            ElasticsearchMappingType fieldType = entry.getValue();

            boolean isNotAnalyzed = parserTokens.isNotAnalyzed(fieldName);
            switch (fieldType) {
                case TEXT:
                    softly
                        .assertThat(isNotAnalyzed)
                        .withFailMessage("Expected isNotAnalyzed=false for key=" + fieldName + " / type=" + fieldType)
                        .isFalse();
                    break;
                case DATETIME:
                case BOOLEAN:
                case LONG:
                case DOUBLE:
                case KEYWORD:
                case NOT_INDEXED:
                    softly
                        .assertThat(isNotAnalyzed)
                        .withFailMessage("Expected isNotAnalyzed=true for key=" + fieldName + " / type=" + fieldType)
                        .isTrue();
                    break;
                case OBJECT:
                case NESTED_OBJECT:
                    softly.assertThat(parserTokens.isNotAnalyzed(fieldName + ".Any")).isFalse();
                    break;
                default:
                    softly.fail("Unexpected type " + fieldType);
            }
        }
    }

    private static void validateVitamDescriptionAgainstJsonSchema(
        SoftAssertions softly,
        Map<String, VitamDescriptionType> descriptionTypeByName,
        Map<String, JsonSchemaField> schemaFieldMap
    ) {
        SetUtils.SetView<String> missingKeys = SetUtils.difference(
            schemaFieldMap.keySet(),
            descriptionTypeByName.keySet()
        );
        softly
            .assertThat(missingKeys)
            .withFailMessage(
                "Missing keys " + missingKeys + " from vitam description file," + " but are found in json schema file"
            )
            .isEmpty();

        SetUtils.SetView<String> unexpectedKeys = SetUtils.difference(
            descriptionTypeByName.keySet(),
            schemaFieldMap.keySet()
        );
        softly
            .assertThat(unexpectedKeys)
            .withFailMessage(
                "Unexpected keys " +
                unexpectedKeys +
                " in vitam description file" +
                " that are not found in json schema file"
            )
            .isEmpty();

        SetUtils.SetView<String> commonKeys = SetUtils.intersection(
            descriptionTypeByName.keySet(),
            schemaFieldMap.keySet()
        );

        for (String entryName : commonKeys) {
            VitamDescriptionType vitamDescriptionType = descriptionTypeByName.get(entryName);
            JsonSchemaField jsonSchemaField = schemaFieldMap.get(entryName);

            VitamDescriptionType.VitamCardinality expectedCardinality = jsonSchemaField.isArray()
                ? VitamDescriptionType.VitamCardinality.many
                : VitamDescriptionType.VitamCardinality.one;
            softly
                .assertThat(vitamDescriptionType.getCardinality())
                .withFailMessage(
                    "Cardinality mismatch for field " +
                    entryName +
                    ". From json schema file: " +
                    expectedCardinality +
                    " != declared cardinality in vitam description file: " +
                    vitamDescriptionType.getCardinality()
                )
                .isEqualTo(expectedCardinality);

            List<VitamDescriptionType.VitamType> validVitamTypes;
            switch (jsonSchemaField.getFieldType()) {
                case ENUM:
                    validVitamTypes = Collections.singletonList(VitamDescriptionType.VitamType.keyword);
                    break;
                case DATE:
                    validVitamTypes = Collections.singletonList(VitamDescriptionType.VitamType.datetime);
                    break;
                case STRING:
                    validVitamTypes = Arrays.asList(
                        VitamDescriptionType.VitamType.keyword,
                        VitamDescriptionType.VitamType.text
                    );
                    break;
                case INTEGER:
                    validVitamTypes = Collections.singletonList(VitamDescriptionType.VitamType.signed_long);
                    break;
                case NUMERIC:
                    validVitamTypes = Arrays.asList(
                        VitamDescriptionType.VitamType.signed_double,
                        VitamDescriptionType.VitamType.signed_long
                    );
                    break;
                case BOOLEAN:
                    validVitamTypes = Collections.singletonList(VitamDescriptionType.VitamType.bool);
                    break;
                case OBJECT:
                    validVitamTypes = Arrays.asList(
                        VitamDescriptionType.VitamType.object,
                        VitamDescriptionType.VitamType.nested_object
                    );
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + jsonSchemaField.getFieldType());
            }

            assertThat(vitamDescriptionType.getType())
                .withFailMessage(
                    "Type mismatch for field " +
                    entryName +
                    ". From json schema file: " +
                    jsonSchemaField.getFieldType() +
                    ", from vitam description file: " +
                    vitamDescriptionType.getType()
                )
                .isIn(validVitamTypes);
        }
    }
}
