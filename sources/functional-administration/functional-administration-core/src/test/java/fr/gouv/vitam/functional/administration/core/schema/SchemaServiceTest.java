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
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.mongodb.VitamMongoCursor;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.schema.SchemaCardinality;
import fr.gouv.vitam.common.model.administration.schema.SchemaInputModel;
import fr.gouv.vitam.common.model.administration.schema.SchemaModel;
import fr.gouv.vitam.common.model.administration.schema.SchemaOrigin;
import fr.gouv.vitam.common.model.administration.schema.SchemaResponse;
import fr.gouv.vitam.common.model.administration.schema.SchemaStringSizeType;
import fr.gouv.vitam.common.model.administration.schema.SchemaType;
import fr.gouv.vitam.common.model.administration.schema.SchemaTypeDetail;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.schema.Schema;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.backup.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.core.ontologies.OntologyService;
import fr.gouv.vitam.functional.administration.core.ontologies.OntologyServiceImpl;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static fr.gouv.vitam.common.PropertiesUtils.getResourceFile;
import static fr.gouv.vitam.common.guid.GUIDFactory.newRequestIdGUID;
import static fr.gouv.vitam.common.json.JsonHandler.getFromFileAsTypeReference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SchemaServiceTest {

    private static final Integer TENANT_ID = 2;
    private static final Integer ADMIN_TENANT = 1;
    private static final FunctionalBackupService functionalBackupService = Mockito.mock(FunctionalBackupService.class);
    private static final MongoDbAccessAdminImpl mongoDbAccessAdminMocked = Mockito.mock(MongoDbAccessAdminImpl.class);
    private static final OntologyService ontologyService = Mockito.mock(OntologyServiceImpl.class);
    private static SchemaService schemaService;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        VitamConfiguration.setAdminTenant(ADMIN_TENANT);
        VitamConfiguration.setTenants(List.of(ADMIN_TENANT));

        LogbookOperationsClientFactory.changeMode(null);

        schemaService = new SchemaService(mongoDbAccessAdminMocked, functionalBackupService, ontologyService);
    }

    @AfterClass
    public static void tearDownAfterClass() {}

    @Before
    public void setUp() throws FileNotFoundException, InvalidParseOperationException, ReferentialException {
        String operationId = newRequestIdGUID(TENANT_ID).toString();

        VitamThreadUtils.getVitamSession().setRequestId(operationId);

        final RequestResponseOK<OntologyModel> mockedOntologies = new RequestResponseOK<OntologyModel>().addAllResults(
            getFromFileAsTypeReference(getResourceFile("schema/ok-ontologies.json"), new TypeReference<>() {})
        );
        when(ontologyService.findOntologies(any())).thenReturn(mockedOntologies);
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_unit_internal_schema()
        throws IOException, InvalidParseOperationException, ReferentialException {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);

        final List<SchemaResponse> internalSchema = schemaService.findUnitInternalSchema();
        assertThat(internalSchema).isNotEmpty();

        final Optional<SchemaResponse> addressBirthPlaceAddressSchemaEltOpt = internalSchema
            .stream()
            .filter(schemaElt -> "Addressee.BirthPlace.Address".equals(schemaElt.getPath()))
            .findAny();

        assertThat(addressBirthPlaceAddressSchemaEltOpt).isPresent();
        final SchemaResponse addressBirthPlaceAddressSchemaElt = addressBirthPlaceAddressSchemaEltOpt.get();

        assertEquals(addressBirthPlaceAddressSchemaElt.getType(), SchemaType.TEXT);
        assertEquals(addressBirthPlaceAddressSchemaElt.getSedaField(), "Address");
        assertEquals(addressBirthPlaceAddressSchemaElt.getCollection(), "Unit");
        assertEquals(addressBirthPlaceAddressSchemaElt.getCardinality(), SchemaCardinality.ONE);
        assertEquals(
            addressBirthPlaceAddressSchemaElt.getDescription(),
            "Mapping : unit-es-mapping.json. En plus des balises Tag et Keyword, il est possible d'indexer les objets avec des éléments pré-définis : Adresse. Références : ead.address"
        );
        assertThat(addressBirthPlaceAddressSchemaElt.getSedaVersions()).contains("2.1");
        assertThat(addressBirthPlaceAddressSchemaElt.getSedaVersions()).contains("2.2");
        assertThat(addressBirthPlaceAddressSchemaElt.getSedaVersions()).contains("2.3");
        assertThat(addressBirthPlaceAddressSchemaElt.getTypeDetail()).isEqualTo(SchemaTypeDetail.STRING);
        assertThat(addressBirthPlaceAddressSchemaElt.getStringSize()).isEqualTo(SchemaStringSizeType.SHORT);
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_object_group_internal_schema()
        throws IOException, InvalidParseOperationException, ReferentialException {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final List<SchemaResponse> internalSchema = schemaService.findObjectGroupInternalSchema();

        // THEN
        assertThat(internalSchema).isNotEmpty();

        final Optional<SchemaResponse> algorithSchemaEltOpt = internalSchema
            .stream()
            .filter(schemaElt -> "_qualifiers.versions.Algorithm".equals(schemaElt.getPath()))
            .findAny();
        assertThat(algorithSchemaEltOpt).isPresent();

        final SchemaResponse algorithSchemaElt = algorithSchemaEltOpt.get();
        assertEquals(algorithSchemaElt.getType(), SchemaType.KEYWORD);
        assertEquals(algorithSchemaElt.getSedaField(), "Algorithm");
        assertEquals(algorithSchemaElt.getPath(), "_qualifiers.versions.Algorithm");
        assertEquals(algorithSchemaElt.getApiPath(), "#qualifiers.versions.Algorithm");
        assertEquals(algorithSchemaElt.getCollection(), "ObjectGroup");
        assertEquals(algorithSchemaElt.getCardinality(), SchemaCardinality.ONE);
        assertThat(algorithSchemaElt.getSedaVersions()).contains("2.1");
        assertThat(algorithSchemaElt.getSedaVersions()).contains("2.2");
        assertThat(algorithSchemaElt.getSedaVersions()).contains("2.3");
        assertThat(algorithSchemaElt.getDescription()).isNotEmpty();

        final Optional<SchemaResponse> persistentIdentifierContentSchemaEltOpt = internalSchema
            .stream()
            .filter(
                schemaElt ->
                    "_qualifiers.versions.PersistentIdentifier.PersistentIdentifierContent".equals(schemaElt.getPath())
            )
            .findAny();
        assertThat(persistentIdentifierContentSchemaEltOpt).isPresent();

        final SchemaResponse persistentIdentifierContentElt = persistentIdentifierContentSchemaEltOpt.get();
        assertEquals(persistentIdentifierContentElt.getType(), SchemaType.KEYWORD);
        assertEquals(persistentIdentifierContentElt.getSedaField(), "PersistentIdentifierContent");
        assertEquals(persistentIdentifierContentElt.getCollection(), "ObjectGroup");
        assertEquals(persistentIdentifierContentElt.getCardinality(), SchemaCardinality.ONE);
        assertThat(persistentIdentifierContentElt.getSedaVersions()).contains("2.2");
        assertThat(persistentIdentifierContentElt.getSedaVersions()).contains("2.3");
        assertThat(persistentIdentifierContentElt.getSedaVersions()).doesNotContain("2.1");
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_unit_schema_with_internal_and_external_schemas()
        throws IOException, InvalidParseOperationException, ReferentialException, InvalidCreateOperationException {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);

        final File fileExternalSchema = getResourceFile("schema/external-schema-db-ok.json");
        final List<Schema> schemaModelList = getFromFileAsTypeReference(fileExternalSchema, new TypeReference<>() {});

        VitamMongoCursor cursor = mock(VitamMongoCursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next())
            .thenReturn(schemaModelList.get(0))
            .thenReturn(schemaModelList.get(1))
            .thenReturn(schemaModelList.get(2));

        DbRequestResult result = new DbRequestResult()
            .setCount(schemaModelList.size())
            .setTotal(schemaModelList.size())
            .setOffset(0)
            .setCursor(cursor);
        when(mongoDbAccessAdminMocked.findDocumentsWithoutRestrictionOnCurrentTenant(any(), any())).thenReturn(result);

        final List<SchemaResponse> unitSchema = schemaService.findUnitSchema();
        assertThat(unitSchema).isNotEmpty();

        final Optional<SchemaResponse> addressBirthPlaceAddressSchemaEltOpt = unitSchema
            .stream()
            .filter(schemaElt -> "Addressee.BirthPlace.Address".equals(schemaElt.getPath()))
            .findAny();
        assertThat(addressBirthPlaceAddressSchemaEltOpt).isPresent();

        final SchemaResponse addressBirthPlaceAddressSchemaElt = addressBirthPlaceAddressSchemaEltOpt.get();
        assertEquals(addressBirthPlaceAddressSchemaElt.getType(), SchemaType.TEXT);
        assertEquals(addressBirthPlaceAddressSchemaElt.getOrigin(), SchemaOrigin.INTERNAL);
        assertEquals(addressBirthPlaceAddressSchemaElt.getSedaField(), "Address");
        assertEquals(addressBirthPlaceAddressSchemaElt.getCollection(), "Unit");
        assertEquals(addressBirthPlaceAddressSchemaElt.getCardinality(), SchemaCardinality.ONE);
        assertEquals(
            addressBirthPlaceAddressSchemaElt.getDescription(),
            "Mapping : unit-es-mapping.json. En plus des balises Tag et Keyword, il est possible d'indexer les objets avec des éléments pré-définis : Adresse. Références : ead.address"
        );
        assertThat(addressBirthPlaceAddressSchemaElt.getSedaVersions()).contains("2.1");
        assertThat(addressBirthPlaceAddressSchemaElt.getSedaVersions()).contains("2.2");
        assertThat(addressBirthPlaceAddressSchemaElt.getSedaVersions()).contains("2.3");
        assertThat(addressBirthPlaceAddressSchemaElt.getTypeDetail()).isEqualTo(SchemaTypeDetail.STRING);
        assertThat(addressBirthPlaceAddressSchemaElt.getStringSize()).isEqualTo(SchemaStringSizeType.SHORT);

        final Optional<SchemaResponse> birthDateSchemaEltOpt = unitSchema
            .stream()
            .filter(schemaElt -> "Invoice.Provider.BirthDate".equals(schemaElt.getPath()))
            .findAny();
        assertThat(birthDateSchemaEltOpt).isPresent();

        final SchemaResponse birthDateSchemaElt = birthDateSchemaEltOpt.get();
        assertEquals(birthDateSchemaElt.getType(), SchemaType.DATE);
        assertEquals(birthDateSchemaElt.getOrigin(), SchemaOrigin.EXTERNAL);
        assertThat(birthDateSchemaElt.getTypeDetail()).isEqualTo(SchemaTypeDetail.DATETIME);
        assertThat(birthDateSchemaElt.getStringSize()).isNull();

        final Optional<SchemaResponse> invoiceSchemaEltOpt = unitSchema
            .stream()
            .filter(schemaElt -> "Invoice".equals(schemaElt.getPath()))
            .findAny();
        assertThat(invoiceSchemaEltOpt).isPresent();

        final SchemaResponse invoiceSchemaElt = invoiceSchemaEltOpt.get();
        assertEquals(invoiceSchemaElt.getType(), SchemaType.OBJECT);
        assertEquals(invoiceSchemaElt.getCardinality(), SchemaCardinality.MANY);
        assertEquals(invoiceSchemaElt.getOrigin(), SchemaOrigin.EXTERNAL);
    }

    @Test
    @RunWithCustomExecutor
    public void should_failed_when_validation_failed() throws IOException, VitamException {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileInputSchema = getResourceFile("schema/external-schema-ok.json");
        final List<SchemaInputModel> schemaModelInputList = getFromFileAsTypeReference(
            fileInputSchema,
            new TypeReference<>() {}
        );

        final File fileExternalSchema = getResourceFile("schema/external-schema-db-ok.json");
        final List<SchemaModel> schemaModelList = getFromFileAsTypeReference(
            fileExternalSchema,
            new TypeReference<>() {}
        );

        DbRequestResult dbRequestResult = mock(DbRequestResult.class);
        final RequestResponseOK response = new RequestResponseOK<>();
        response.addAllResults(schemaModelList);

        final File ontologyFile = getResourceFile("schema/ok-ontologies.json");

        final List<OntologyModel> ontologyModelList = getFromFileAsTypeReference(
            ontologyFile,
            new TypeReference<>() {}
        );

        RequestResponseOK<OntologyModel> ontologyResponse = new RequestResponseOK<OntologyModel>().addAllResults(
            ontologyModelList
        );
        when(ontologyService.findOntologies(any())).thenReturn(ontologyResponse);

        when(dbRequestResult.getCount()).thenReturn(Long.valueOf(schemaModelList.size()));
        when(dbRequestResult.getTotal()).thenReturn(Long.valueOf(schemaModelList.size()));

        when(dbRequestResult.getRequestResponseOK(any(), any(), any())).thenReturn(response);

        when(mongoDbAccessAdminMocked.findDocumentsWithoutRestrictionOnCurrentTenant(any(), any())).thenReturn(
            dbRequestResult
        );

        final RequestResponse<SchemaModel> importingResponse = schemaService.importExternalSchemaElements(
            schemaModelInputList
        );
        assertNotNull(importingResponse);
        assertEquals(importingResponse.getStatus(), HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    @RunWithCustomExecutor
    public void should_success_when_validation_success() throws IOException, VitamException {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        final File fileInputSchema = getResourceFile("schema/external-schema-ok.json");
        final List<SchemaInputModel> schemaModelInputList = getFromFileAsTypeReference(
            fileInputSchema,
            new TypeReference<>() {}
        );

        final File fileExternalSchema = getResourceFile("schema/external-schema-db-ok.json");
        final List<Schema> schemaModelList = getFromFileAsTypeReference(fileExternalSchema, new TypeReference<>() {});

        DbRequestResult dbRequestResult = mock(DbRequestResult.class);
        final RequestResponseOK response = new RequestResponseOK<>();
        response.addAllResults(Collections.emptyList());

        final File ontologyFile = getResourceFile("schema/ok-ontologies.json");

        final List<OntologyModel> ontologyModelList = getFromFileAsTypeReference(
            ontologyFile,
            new TypeReference<>() {}
        );

        final RequestResponseOK<OntologyModel> ontologyResponse = new RequestResponseOK<OntologyModel>().addAllResults(
            ontologyModelList
        );
        when(ontologyService.findOntologies(any())).thenReturn(ontologyResponse);

        when(dbRequestResult.getCount()).thenReturn(Long.valueOf(schemaModelList.size()));
        when(dbRequestResult.getTotal()).thenReturn(Long.valueOf(schemaModelList.size()));

        doNothing().when(dbRequestResult).close();

        when(dbRequestResult.getRequestResponseOK(any(), any(), any())).thenReturn(response);

        when(mongoDbAccessAdminMocked.findDocumentsWithoutRestrictionOnCurrentTenant(any(), any())).thenReturn(
            dbRequestResult
        );

        when(mongoDbAccessAdminMocked.insertDocument(any(), eq(FunctionalAdminCollections.SCHEMA))).thenReturn(
            dbRequestResult
        );

        final RequestResponse<SchemaModel> importingResponse = schemaService.importExternalSchemaElements(
            schemaModelInputList
        );
        assertNotNull(importingResponse);
        assertEquals(HttpStatus.SC_CREATED, importingResponse.getStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void checkAndDeleteExternalSchemaElementsByPaths_ok() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);

        setUpMockedFindDocumentsResponse("schema/external-schema-to-delete-ok.json");

        List<String> pathsToDelete = List.of("Invoice.Provider.BirthDate");

        schemaService.checkAndDeleteExternalSchemaElementsByPaths(pathsToDelete, true);
    }

    @Test
    @RunWithCustomExecutor
    public void checkAndDeleteExternalSchemaElementsByPaths_nok() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);

        setUpMockedFindDocumentsResponse("schema/external-schema-to-delete-ok.json");

        List<String> pathsToDelete = List.of("Invoice");

        assertThrows(
            BadRequestException.class,
            () -> schemaService.checkAndDeleteExternalSchemaElementsByPaths(pathsToDelete, true)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void checkAndDeleteExternalSchemaElementsByPaths_internal() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);

        setUpMockedFindDocumentsResponse("schema/external-schema-to-delete-ok.json");

        List<String> pathsToDelete = List.of("Invoice.Provider.BirthDate", "OriginatingSystemId");

        assertThrows(
            BadRequestException.class,
            () -> schemaService.checkAndDeleteExternalSchemaElementsByPaths(pathsToDelete, true)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void checkAndDeleteExternalSchemaElementsByPaths_nonExistingPaths() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);

        setUpMockedFindDocumentsResponse("schema/external-schema-to-delete-ok.json");

        List<String> pathsToDelete = List.of("Invoice.Provider.BirthDate", "NonExistingPath");

        assertThrows(
            BadRequestException.class,
            () -> schemaService.checkAndDeleteExternalSchemaElementsByPaths(pathsToDelete, true)
        );

        verify(mongoDbAccessAdminMocked, times(0)).deleteDocument(any(), any());
    }

    @Test
    @RunWithCustomExecutor
    public void checkAndDeleteExternalSchemaElementsByPaths_currentTenantOnly() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        setUpMockedFindDocumentsResponse("schema/external-schema-to-delete-one-tenant.json");

        List<String> pathsToDelete = List.of("Invoice.Provider");

        schemaService.checkAndDeleteExternalSchemaElementsByPaths(pathsToDelete, false);

        verify(mongoDbAccessAdminMocked, times(1)).deleteDocument(any(), any());
    }

    @Test
    @RunWithCustomExecutor
    public void checkAndDeleteExternalSchemaElementsByPaths_allTenant() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);

        setUpMockedFindDocumentsResponse("schema/external-schema-to-delete-multi-tenant.json");

        List<String> pathsToDelete = List.of("Invoice");

        assertThrows(
            BadRequestException.class,
            () -> schemaService.checkAndDeleteExternalSchemaElementsByPaths(pathsToDelete, true)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void checkAndDeleteExternalSchemaElementsUsedOnMultipleTenant() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(ADMIN_TENANT);
        VitamConfiguration.setTenants(List.of(1, 4));

        setUpMockedFindDocumentsResponse("schema/external-schema-only-current-tenant-paths.json");

        List<String> pathsToDelete = List.of("Invoice", "Invoice.Provider", "Invoice.Provider.BirthDate");

        assertThrows(
            BadRequestException.class,
            () -> schemaService.checkAndDeleteExternalSchemaElementsByPaths(pathsToDelete, true)
        );
    }

    private void setUpMockedFindDocumentsResponse(String filePath)
        throws IOException, InvalidParseOperationException, ReferentialException {
        final File fileExternalSchema = getResourceFile(filePath);
        final List<SchemaModel> schemaModelList = getFromFileAsTypeReference(
            fileExternalSchema,
            new TypeReference<>() {}
        );
        DbRequestResult dbRequestResult = mock(DbRequestResult.class);
        final RequestResponseOK response = new RequestResponseOK<>();
        response.addAllResults(schemaModelList);

        when(dbRequestResult.getCount()).thenReturn(Long.valueOf(schemaModelList.size()));
        when(dbRequestResult.getTotal()).thenReturn(Long.valueOf(schemaModelList.size()));
        when(dbRequestResult.getRequestResponseOK(any(), any(), any())).thenReturn(response);
        when(mongoDbAccessAdminMocked.findDocumentsWithoutRestrictionOnCurrentTenant(any(), any())).thenReturn(
            dbRequestResult
        );
    }
}
