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
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import fr.gouv.vitam.common.MappingLoaderTestUtils;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.schema.SchemaResponse;
import fr.gouv.vitam.common.model.unit.ArchiveUnitInternalModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbVarNameAdapter;
import fr.gouv.vitam.metadata.core.mapping.MappingLoader;
import fr.gouv.vitam.model.validation.pojo.PojoModel;
import fr.gouv.vitam.model.validation.pojo.PojoModelExtractor;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.gouv.vitam.functional.administration.core.schema.SchemaService.VITAM_OBJECT_GROUP_INTERNAL_SCHEMA_JSON;
import static fr.gouv.vitam.functional.administration.core.schema.SchemaService.VITAM_UNIT_INTERNAL_SCHEMA_JSON;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataCollections.OBJECTGROUP;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataCollections.UNIT;
import static org.assertj.core.api.Assertions.assertThat;

public class MetadataModelValidationTest {

    private MappingLoader mappingLoader;

    @Before
    public void setup() {
        mappingLoader = MappingLoaderTestUtils.getTestMappingLoader();
    }

    @Test
    public void validateUnitSchema() throws Exception {
        ModelValidatorUtils.validateSchema(VITAM_UNIT_INTERNAL_SCHEMA_JSON, UNIT);
    }

    @Test
    public void validateObjectGroupSchema() throws Exception {
        ModelValidatorUtils.validateSchema(VITAM_OBJECT_GROUP_INTERNAL_SCHEMA_JSON, OBJECTGROUP);
    }

    @Test
    public void testUnitElasticsearchMapping() throws Exception {
        ModelValidatorUtils.validateDataModel(mappingLoader.loadMapping(UNIT.name()), UNIT.getVitamCollection());
    }

    @Test
    public void testObjectGroupElasticsearchMapping() throws Exception {
        ModelValidatorUtils.validateDataModel(
            mappingLoader.loadMapping(OBJECTGROUP.name()),
            OBJECTGROUP.getVitamCollection()
        );
    }

    @Test
    public void testSecuredUnitFields() {
        Stream<String> systemFields = UNIT.getVitamDescriptionResolver()
            .getDescriptionTypeByStaticName()
            .keySet()
            .stream()
            .filter(fieldName -> fieldName.startsWith("_"))
            .filter(fieldName -> !fieldName.contains("."));

        assertThat(systemFields).containsExactlyInAnyOrderElementsOf(
            SetUtils.union(
                MetadataDocumentHelper.getSecuredUnitFields(),
                MetadataDocumentHelper.getComputedUnitFields()
            )
        );
    }

    @Test
    public void testSecuredObjectGroupFields() {
        Stream<String> systemFields = OBJECTGROUP.getVitamDescriptionResolver()
            .getDescriptionTypeByStaticName()
            .keySet()
            .stream()
            .filter(fieldName -> fieldName.startsWith("_"))
            .filter(fieldName -> !fieldName.contains("."));

        assertThat(systemFields).containsExactlyInAnyOrderElementsOf(
            SetUtils.union(
                MetadataDocumentHelper.getSecuredObjectGroupFields(),
                MetadataDocumentHelper.getComputedObjectGroupFields()
            )
        );
    }

    @Test
    public void ensureInternalAndExternalModelsAreSynchronized() {
        // List internal model fields
        PojoModelExtractor pojoModelExtractor = new PojoModelExtractor();
        List<PojoModel> internalModelFields = pojoModelExtractor.extractPojoModels(ArchiveUnitInternalModel.class);
        List<PojoModel> externalModelFields = pojoModelExtractor.extractPojoModels(ArchiveUnitModel.class);

        // Adapt external model fields using metadata var name adapter
        MongoDbVarNameAdapter varNameAdapter = new MongoDbVarNameAdapter();
        List<PojoModel> mappedExternalModelToInternalModelFields = externalModelFields
            .stream()
            .map(fieldInfo -> {
                try {
                    String internalLocalPath = mapExternalToInternalFieldName(varNameAdapter, fieldInfo.getLocalPath());
                    String internalFullPath = mapExternalToInternalFieldName(varNameAdapter, fieldInfo.getFullPath());
                    return new PojoModel(
                        internalLocalPath,
                        internalFullPath,
                        fieldInfo.isArray(),
                        fieldInfo.getModelType()
                    );
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException(e);
                }
            })
            .sorted(Comparator.comparing(PojoModel::getFullPath))
            .collect(Collectors.toList());

        // Check equality
        assertThat(internalModelFields)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyElementsOf(mappedExternalModelToInternalModelFields);
    }

    private static String mapExternalToInternalFieldName(MongoDbVarNameAdapter varNameAdapter, String path)
        throws InvalidParseOperationException {
        return Objects.toString(varNameAdapter.getVariableName(path), path);
    }

    @Test
    public void testVarNameAdapterForUnitSchemaFields() throws Exception {
        checkVarNameAdapterAgainstSchemaFields(VITAM_UNIT_INTERNAL_SCHEMA_JSON);
    }

    @Test
    public void testVarNameAdapterForObjectGroupSchemaFields() throws Exception {
        checkVarNameAdapterAgainstSchemaFields(VITAM_OBJECT_GROUP_INTERNAL_SCHEMA_JSON);
    }

    private static void checkVarNameAdapterAgainstSchemaFields(String vitamObjectGroupInternalSchemaJson)
        throws InvalidParseOperationException, InvalidFormatException, FileNotFoundException {
        List<SchemaResponse> schemaResponses = JsonHandler.getFromInputStreamAsTypeReference(
            PropertiesUtils.getResourceAsStream(vitamObjectGroupInternalSchemaJson),
            new TypeReference<>() {}
        );

        MongoDbVarNameAdapter varNameAdapter = new MongoDbVarNameAdapter();

        for (SchemaResponse schemaResponse : schemaResponses) {
            String apiPath = schemaResponse.getApiPath();

            if (apiPath.startsWith("Event.linkingAgentIdentifier")) {
                // FIXME 14040 : Fix Event.linkingAgentIdentifier vs Event.LinkingAgentIdentifier model
                continue;
            }

            String variableName = StringUtils.defaultIfEmpty(varNameAdapter.getVariableName(apiPath), apiPath);
            assertThat(variableName).isEqualTo(schemaResponse.getPath());
        }
    }
}
