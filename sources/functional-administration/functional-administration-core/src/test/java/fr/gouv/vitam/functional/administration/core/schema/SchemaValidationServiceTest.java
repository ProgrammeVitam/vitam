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
package fr.gouv.vitam.functional.administration.core.schema;

import com.fasterxml.jackson.core.type.TypeReference;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.mongodb.VitamMongoCursor;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.schema.SchemaInputModel;
import fr.gouv.vitam.common.model.administration.schema.SchemaResponse;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.exception.schema.SchemaImportValidationException;
import fr.gouv.vitam.functional.administration.common.schema.ErrorReportSchema;
import fr.gouv.vitam.functional.administration.common.schema.Schema;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import fr.gouv.vitam.functional.administration.core.ontologies.OntologyService;
import fr.gouv.vitam.functional.administration.core.ontologies.OntologyServiceImpl;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.guid.GUIDFactory.newRequestIdGUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SchemaValidationServiceTest {

    private static final Integer TENANT_ID = 2;
    private static final Integer ADMIN_TENANT = 1;
    private static final OntologyService ontologyService = Mockito.mock(OntologyServiceImpl.class);
    private static final MongoDbAccessReferential mongoDbAccessReferential = Mockito.mock(
        MongoDbAccessReferential.class
    );
    private static SchemaValidationService schemaValidationService;
    private final TypeReference<List<SchemaInputModel>> listOfSchemaInputType = new TypeReference<>() {};
    private final TypeReference<List<Schema>> listOfSchemaType = new TypeReference<>() {};
    private final TypeReference<List<OntologyModel>> listOfOntologyType = new TypeReference<>() {};

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private List<SchemaResponse> internalSchemaList;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        VitamConfiguration.setAdminTenant(ADMIN_TENANT);
        VitamConfiguration.setTenants(List.of(ADMIN_TENANT, TENANT_ID));
        LogbookOperationsClientFactory.changeMode(null);
        schemaValidationService = new SchemaValidationService(
            mongoDbAccessReferential,
            LogbookOperationsClientFactory.getInstance()
        );
    }

    @Before
    public void setUp() throws IOException, InvalidParseOperationException {
        String operationId = newRequestIdGUID(TENANT_ID).toString();
        internalSchemaList = findUnitInternalSchema();
        VitamThreadUtils.getVitamSession().setRequestId(operationId);
    }

    @Test
    @RunWithCustomExecutor
    public void should_success_when_importing_correct_external_paths() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);

        final File fileSchema = PropertiesUtils.getResourceFile("schema/external-schema-ok.json");
        final List<SchemaInputModel> schemaModelList = JsonHandler.getFromFileAsTypeReference(
            fileSchema,
            listOfSchemaInputType
        );

        final File ontologyFile = PropertiesUtils.getResourceFile("schema/ok-ontologies.json");

        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            ontologyFile,
            listOfOntologyType
        );
        Map<String, OntologyModel> ontologyEltsMapByIdentifier = ontologyModelList
            .stream()
            .collect(Collectors.toMap(OntologyModel::getIdentifier, ontologyElt -> ontologyElt));

        RequestResponseOK<OntologyModel> ontologyResponse = new RequestResponseOK<OntologyModel>().addAllResults(
            ontologyModelList
        );
        when(ontologyService.findOntologies(any())).thenReturn(ontologyResponse);
        DbRequestResult result = new DbRequestResult().setCount(0).setTotal(0).setOffset(0);
        when(mongoDbAccessReferential.findDocumentsWithoutRestrictionOnCurrentTenant(any(), any())).thenReturn(result);
        Map<String, List<ErrorReportSchema>> importErrors = new HashMap<>();
        schemaValidationService.validateExternalSchemaInputs(
            schemaModelList,
            internalSchemaList,
            ontologyEltsMapByIdentifier,
            importErrors
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_importing_some_paths_already_in_internal_schema() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileSchema = PropertiesUtils.getResourceFile(
            "schema/schema-with-paths-already-in-internal-schema.json"
        );
        final List<SchemaInputModel> schemaModelList = JsonHandler.getFromFileAsTypeReference(
            fileSchema,
            listOfSchemaInputType
        );

        final File ontologyFile = PropertiesUtils.getResourceFile("schema/ok-ontologies.json");

        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            ontologyFile,
            listOfOntologyType
        );
        Map<String, OntologyModel> ontologyEltsMapByIdentifier = ontologyModelList
            .stream()
            .collect(Collectors.toMap(OntologyModel::getIdentifier, ontologyElt -> ontologyElt));

        RequestResponseOK<OntologyModel> ontologyResponse = new RequestResponseOK<OntologyModel>().addAllResults(
            ontologyModelList
        );
        when(ontologyService.findOntologies(any())).thenReturn(ontologyResponse);

        DbRequestResult result = new DbRequestResult().setCount(0).setTotal(0).setOffset(0);
        when(mongoDbAccessReferential.findDocumentsWithoutRestrictionOnCurrentTenant(any(), any())).thenReturn(result);

        // When / then
        assertThatThrownBy(
            () ->
                schemaValidationService.validateExternalSchemaInputs(
                    schemaModelList,
                    internalSchemaList,
                    ontologyEltsMapByIdentifier,
                    new HashMap<>()
                )
        )
            .isInstanceOf(SchemaImportValidationException.class)
            .hasMessage("Paths already in current schema = Addressee.BirthPlace.City, Addressee.BirthPlace.Country");
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_importing_some_paths_with_wrong_format() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileSchema = PropertiesUtils.getResourceFile("schema/schema-with-paths-having_bad_format.json");
        final List<SchemaInputModel> schemaModelList = JsonHandler.getFromFileAsTypeReference(
            fileSchema,
            listOfSchemaInputType
        );

        final File ontologyFile = PropertiesUtils.getResourceFile("schema/ok-ontologies.json");

        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            ontologyFile,
            listOfOntologyType
        );
        Map<String, OntologyModel> ontologyEltsMapByIdentifier = ontologyModelList
            .stream()
            .collect(Collectors.toMap(OntologyModel::getIdentifier, ontologyElt -> ontologyElt));

        RequestResponseOK<OntologyModel> ontologyResponse = new RequestResponseOK<OntologyModel>().addAllResults(
            ontologyModelList
        );
        when(ontologyService.findOntologies(any())).thenReturn(ontologyResponse);

        DbRequestResult result = new DbRequestResult().setCount(0).setTotal(0).setOffset(0);
        when(mongoDbAccessReferential.findDocumentsWithoutRestrictionOnCurrentTenant(any(), any())).thenReturn(result);

        // When / then
        assertThatThrownBy(
            () ->
                schemaValidationService.validateExternalSchemaInputs(
                    schemaModelList,
                    internalSchemaList,
                    ontologyEltsMapByIdentifier,
                    new HashMap<>()
                )
        )
            .isInstanceOf(SchemaImportValidationException.class)
            .hasMessage(
                "Some inputs have validation errors: Path (Invoice-Provider, Invoice.Provider .BirthDate, " +
                "Addressee.' ' BirthPlace.City, Invoice & wrong Invoice, Addressee*BirthPlace\\Country, " +
                "With_Underscore), Description (Invoice & wrong Invoice, With_Underscore), " +
                "ShortName (Addressee*BirthPlace\\Country, With_Underscore)"
            );
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_importing_some_paths_with_parents_not_found() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileSchema = PropertiesUtils.getResourceFile("schema/schema-with-missed-parent-paths.json");
        final List<SchemaInputModel> schemaModelList = JsonHandler.getFromFileAsTypeReference(
            fileSchema,
            listOfSchemaInputType
        );

        final File ontologyFile = PropertiesUtils.getResourceFile("schema/ok-ontologies.json");

        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            ontologyFile,
            listOfOntologyType
        );
        Map<String, OntologyModel> ontologyEltsMapByIdentifier = ontologyModelList
            .stream()
            .collect(Collectors.toMap(OntologyModel::getIdentifier, ontologyElt -> ontologyElt));

        RequestResponseOK<OntologyModel> ontologyResponse = new RequestResponseOK<OntologyModel>().addAllResults(
            ontologyModelList
        );
        when(ontologyService.findOntologies(any())).thenReturn(ontologyResponse);
        DbRequestResult result = new DbRequestResult().setCount(0).setTotal(0).setOffset(0);
        when(mongoDbAccessReferential.findDocumentsWithoutRestrictionOnCurrentTenant(any(), any())).thenReturn(result);
        // When / then
        assertThatThrownBy(
            () ->
                schemaValidationService.validateExternalSchemaInputs(
                    schemaModelList,
                    internalSchemaList,
                    ontologyEltsMapByIdentifier,
                    new HashMap<>()
                )
        ).isInstanceOf(SchemaImportValidationException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_importing_some_paths_with_not_found_leaf() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileSchema = PropertiesUtils.getResourceFile("schema/schema-with-missed-leaf.json");
        final List<SchemaInputModel> schemaModelList = JsonHandler.getFromFileAsTypeReference(
            fileSchema,
            listOfSchemaInputType
        );

        final File ontologyFile = PropertiesUtils.getResourceFile("schema/ok-ontologies.json");

        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            ontologyFile,
            listOfOntologyType
        );
        Map<String, OntologyModel> ontologyEltsMapByIdentifier = ontologyModelList
            .stream()
            .collect(Collectors.toMap(OntologyModel::getIdentifier, ontologyElt -> ontologyElt));

        DbRequestResult result = new DbRequestResult().setCount(0).setTotal(0).setOffset(0);
        when(mongoDbAccessReferential.findDocumentsWithoutRestrictionOnCurrentTenant(any(), any())).thenReturn(result);
        RequestResponseOK<OntologyModel> ontologyResponse = new RequestResponseOK<OntologyModel>().addAllResults(
            ontologyModelList
        );
        when(ontologyService.findOntologies(any())).thenReturn(ontologyResponse);

        // When / then
        assertThatThrownBy(
            () ->
                schemaValidationService.validateExternalSchemaInputs(
                    schemaModelList,
                    internalSchemaList,
                    ontologyEltsMapByIdentifier,
                    new HashMap<>()
                )
        )
            .isInstanceOf(SchemaImportValidationException.class)
            .hasMessage("Path leaf missed = SomeDate");
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_importing_some_paths_with_path_as_objects_in_ontology() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileSchema = PropertiesUtils.getResourceFile(
            "schema/schema-with-conflicts-objects-and-ontology.json"
        );
        final List<SchemaInputModel> schemaModelList = JsonHandler.getFromFileAsTypeReference(
            fileSchema,
            listOfSchemaInputType
        );
        final File ontologyFile = PropertiesUtils.getResourceFile("schema/ok-ontologies.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            ontologyFile,
            listOfOntologyType
        );
        Map<String, OntologyModel> ontologyEltsMapByIdentifier = ontologyModelList
            .stream()
            .collect(Collectors.toMap(OntologyModel::getIdentifier, ontologyElt -> ontologyElt));
        final RequestResponseOK<OntologyModel> ontologyResponse = new RequestResponseOK<OntologyModel>().addAllResults(
            ontologyModelList
        );
        when(ontologyService.findOntologies(any())).thenReturn(ontologyResponse);
        assertThatThrownBy(
            () ->
                schemaValidationService.validateExternalSchemaInputs(
                    schemaModelList,
                    internalSchemaList,
                    ontologyEltsMapByIdentifier,
                    new HashMap<>()
                )
        ).isInstanceOf(SchemaImportValidationException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_importing_for_admin_tenant_and_some_paths_already_in_other_tenant_schema()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileSchema = PropertiesUtils.getResourceFile("schema/external-schema-ok.json");
        final List<SchemaInputModel> schemaModelList = JsonHandler.getFromFileAsTypeReference(
            fileSchema,
            listOfSchemaInputType
        );

        final File ontologyFile = PropertiesUtils.getResourceFile("schema/ok-ontologies.json");

        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            ontologyFile,
            listOfOntologyType
        );
        Map<String, OntologyModel> ontologyEltsMapByIdentifier = ontologyModelList
            .stream()
            .collect(Collectors.toMap(OntologyModel::getIdentifier, ontologyElt -> ontologyElt));

        RequestResponseOK<OntologyModel> ontologyResponse = new RequestResponseOK<OntologyModel>().addAllResults(
            ontologyModelList
        );
        when(ontologyService.findOntologies(any())).thenReturn(ontologyResponse);

        final File fileSchemaOtherTenants = PropertiesUtils.getResourceFile(
            "schema/schema-with-paths-already-in-other-tenants-schema.json"
        );
        final List<Schema> currentSchemaList = JsonHandler.getFromFileAsTypeReference(
            fileSchemaOtherTenants,
            listOfSchemaType
        );

        VitamMongoCursor cursor = mock(VitamMongoCursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next())
            .thenReturn(currentSchemaList.get(0))
            .thenReturn(currentSchemaList.get(1))
            .thenReturn(currentSchemaList.get(2));

        DbRequestResult result = new DbRequestResult()
            .setCount(currentSchemaList.size())
            .setTotal(currentSchemaList.size())
            .setOffset(0)
            .setCursor(cursor);
        when(mongoDbAccessReferential.findDocumentsWithoutRestrictionOnCurrentTenant(any(), any())).thenReturn(result);

        // When / then
        assertThatThrownBy(
            () ->
                schemaValidationService.validateExternalSchemaInputs(
                    schemaModelList,
                    internalSchemaList,
                    ontologyEltsMapByIdentifier,
                    new HashMap<>()
                )
        )
            .isInstanceOf(SchemaImportValidationException.class)
            .hasMessage(
                "Paths already in current schema for the current or other tenants = Invoice.Provider.BirthDate, Invoice, Invoice.Provider"
            );
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_importing_some_paths_matching_ontology_of_type_geo_point() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileSchema = PropertiesUtils.getResourceFile(
            "schema/schema-with-path-matching-ontology-of-type-geo-point.json"
        );
        final List<SchemaInputModel> schemaModelList = JsonHandler.getFromFileAsTypeReference(
            fileSchema,
            listOfSchemaInputType
        );
        final File ontologyFile = PropertiesUtils.getResourceFile("schema/ok-ontologies.json");
        final List<OntologyModel> ontologyModelList = JsonHandler.getFromFileAsTypeReference(
            ontologyFile,
            listOfOntologyType
        );
        Map<String, OntologyModel> ontologyEltsMapByIdentifier = ontologyModelList
            .stream()
            .collect(Collectors.toMap(OntologyModel::getIdentifier, ontologyElt -> ontologyElt));
        final RequestResponseOK<OntologyModel> ontologyResponse = new RequestResponseOK<OntologyModel>().addAllResults(
            ontologyModelList
        );
        when(ontologyService.findOntologies(any())).thenReturn(ontologyResponse);
        assertThatThrownBy(
            () ->
                schemaValidationService.validateExternalSchemaInputs(
                    schemaModelList,
                    internalSchemaList,
                    ontologyEltsMapByIdentifier,
                    new HashMap<>()
                )
        )
            .isInstanceOf(SchemaImportValidationException.class)
            .hasMessage("Path matches an ontology of type GEO_POINT = MyGeoPoint");
    }

    private InputStream loadUnitInternalSchema() throws IOException {
        return PropertiesUtils.getResourceAsStream("vitam-unit-internal-schema.json");
    }

    public List<SchemaResponse> findUnitInternalSchema() throws InvalidParseOperationException, IOException {
        InputStream isUnitInternalSchema = loadUnitInternalSchema();
        List<SchemaResponse> unitSchemaModels = JsonHandler.getFromInputStreamAsTypeReference(
            isUnitInternalSchema,
            new TypeReference<>() {}
        );
        return unitSchemaModels;
    }
}
