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
package fr.gouv.vitam.functional;

import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchTestHelper;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.schema.SchemaResponse;
import fr.gouv.vitam.common.model.config.CollectionConfiguration;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.server.config.DefaultCollectionConfiguration;
import fr.gouv.vitam.logbook.common.server.config.LogbookIndexationConfiguration;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.config.DedicatedTenantConfiguration;
import fr.gouv.vitam.metadata.core.config.GroupedTenantConfiguration;
import fr.gouv.vitam.metadata.core.config.MetadataIndexationConfiguration;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import io.restassured.RestAssured;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response.Status;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static io.restassured.RestAssured.get;

/**
 * Search by tenant integration test
 */
public class SearchByTenantIT extends VitamRuleRunner {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SearchByTenantIT.class);

    private static final String TITLE = "Title";
    private static final String METADATA_PATH = "/metadata/v1";
    private static final String PROCESSING_PATH = "/processing/v1";
    private static final String WORKER_PATH = "/worker/v1";
    private static final String WORKSPACE_PATH = "/workspace/v1";
    private static final String LOGBOOK_PATH = "/logbook/v1";
    private static final String INGEST_INTERNAL_PATH = "/ingest/v1";
    private static final String ACCESS_INTERNAL_PATH = "/access-internal/v1";
    private static final String STORAGE_PATH = "/storage/v1";
    private static final String OFFER_PATH = "/offer/v1";
    private static final String BATCH_REPORT_PATH = "/batchreport/v1";
    public static final String TENANT_GROUP = "mygrp";

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        SearchByTenantIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            ProcessManagementMain.class,
            AccessInternalMain.class,
            IngestInternalMain.class,
            StorageMain.class,
            DefaultOfferMain.class,
            BatchReportMain.class
        )
    );

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Map<String, List<Integer>> groupedMap = new HashMap<>();
        groupedMap.put("mygrp", List.of(0));
        handleBeforeClass(List.of(1), groupedMap);
        runner.stopServers();

        String defaultUnitEsMapping = ElasticsearchTestHelper.loadUnitMapping();
        String defaultOGEsMapping = ElasticsearchTestHelper.loadObjectGroupMapping();
        String customUnitEsMapping = loadMappingFromResources("custom-unit-es-mapping.json");
        String custom2UnitEsMapping = loadMappingFromResources("custom2-unit-es-mapping.json");
        String customOGEsMapping = loadMappingFromResources("custom-og-es-mapping.json");

        // Update config
        MetadataIndexationConfiguration metadataIndexationConfiguration = new MetadataIndexationConfiguration()
            .setDefaultCollectionConfiguration(
                new fr.gouv.vitam.metadata.core.config.DefaultCollectionConfiguration()
                    .setUnit(new CollectionConfiguration(1, 0).setMappingFile(defaultUnitEsMapping))
                    .setObjectgroup(new CollectionConfiguration(1, 0).setMappingFile(defaultOGEsMapping))
            )
            .setDedicatedTenantConfiguration(
                Collections.singletonList(
                    new DedicatedTenantConfiguration()
                        .setTenants("1")
                        .setUnit(new CollectionConfiguration(1, 0).setMappingFile(custom2UnitEsMapping))
                        .setObjectgroup(new CollectionConfiguration(1, 0).setMappingFile(customOGEsMapping))
                )
            )
            .setGroupedTenantConfiguration(
                Collections.singletonList(
                    new GroupedTenantConfiguration()
                        .setName(TENANT_GROUP)
                        .setTenants("0")
                        .setUnit(new CollectionConfiguration(1, 0).setMappingFile(customUnitEsMapping))
                        .setObjectgroup(new CollectionConfiguration(1, 0).setMappingFile(customOGEsMapping))
                )
            );

        LogbookIndexationConfiguration logbookIndexationConfiguration = new LogbookIndexationConfiguration()
            .setDefaultCollectionConfiguration(
                new DefaultCollectionConfiguration().setLogbookoperation(new CollectionConfiguration(1, 0))
            )
            .setGroupedTenantConfiguration(
                Collections.singletonList(
                    new fr.gouv.vitam.logbook.common.server.config.GroupedTenantConfiguration()
                        .setName(TENANT_GROUP)
                        .setTenants("0")
                        .setLogbookoperation(new CollectionConfiguration(1, 0))
                )
            );

        runner.setCustomMetadataIndexationConfiguration(metadataIndexationConfiguration);
        runner.setCustomLogbookIndexationConfiguration(logbookIndexationConfiguration);

        // Restart servers
        runner.startServers();

        new DataLoader("integration-ingest-internal").prepareData();
    }

    @After
    public void afterTest() throws Exception {
        VitamThreadUtils.getVitamSession().setContractId("aName");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");

        ProcessDataAccessImpl.getInstance().clearWorkflow();
    }

    @AfterClass
    public static void tearDownAfterClass() throws VitamApplicationServerException {
        // Stop running servers
        runner.stopServers();

        // Reset custom config
        runner.setCustomMetadataIndexationConfiguration(null);
        runner.setCustomLogbookIndexationConfiguration(null);

        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    public static GUID prepareVitamSession(int tenantId) {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId("TenantGroupAccessTest");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
        final GUID accessContractRequestId = newOperationLogbookGUID(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(accessContractRequestId);
        return accessContractRequestId;
    }

    @Before
    public void setUpBefore() {}

    @RunWithCustomExecutor
    @Test
    public void testServersStatus() {
        RestAssured.port = VitamServerRunner.PORT_SERVICE_PROCESSING;
        RestAssured.basePath = PROCESSING_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKSPACE;
        RestAssured.basePath = WORKSPACE_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_METADATA;
        RestAssured.basePath = METADATA_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_WORKER;
        RestAssured.basePath = WORKER_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_LOGBOOK;
        RestAssured.basePath = LOGBOOK_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_INGEST_INTERNAL;
        RestAssured.basePath = INGEST_INTERNAL_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_ACCESS_INTERNAL;
        RestAssured.basePath = ACCESS_INTERNAL_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_STORAGE;
        RestAssured.basePath = STORAGE_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_OFFER;
        RestAssured.basePath = OFFER_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());

        RestAssured.port = VitamServerRunner.PORT_SERVICE_BATCH_REPORT;
        RestAssured.basePath = BATCH_REPORT_PATH;
        get("/status").then().statusCode(Status.NO_CONTENT.getStatusCode());
    }

    @RunWithCustomExecutor
    @Test
    public void should_test_custom_searchs_settings() throws Exception {
        // Given
        prepareVitamSession(0); //Using grouped tenants
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            RequestResponse<SchemaResponse> unitSchemaResponse = client.getUnitSchema();
            List<SchemaResponse> schemaResponses =
                ((RequestResponseOK<SchemaResponse>) unitSchemaResponse).getResults();
            Assert.assertNotNull(schemaResponses);
            SchemaResponse titleSchemaElt = schemaResponses
                .stream()
                .filter(schemaResponse -> schemaResponse.getPath().equals(TITLE))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find Title schema path"));
            Assert.assertNotNull(titleSchemaElt);
            Assert.assertNotNull(titleSchemaElt.getCustomSearchTypes());
            Assert.assertEquals(titleSchemaElt.getCustomSearchTypes().get(0), "Strict");

            SchemaResponse descriptionSchemaElt = schemaResponses
                .stream()
                .filter(schemaResponse -> schemaResponse.getPath().equals("Description"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find Description schema path"));
            Assert.assertNotNull(descriptionSchemaElt);
            Assert.assertNotNull(descriptionSchemaElt.getCustomSearchTypes());
            Assert.assertEquals(descriptionSchemaElt.getCustomSearchTypes().get(0), "Minimal");
        }

        // Given
        prepareVitamSession(2); //Using dedicated tenants
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            RequestResponse<SchemaResponse> unitSchemaResponse = client.getUnitSchema();
            List<SchemaResponse> schemaResponses =
                ((RequestResponseOK<SchemaResponse>) unitSchemaResponse).getResults();
            Assert.assertNotNull(schemaResponses);
            SchemaResponse titleSchemaElt = schemaResponses
                .stream()
                .filter(schemaResponse -> schemaResponse.getPath().equals(TITLE))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find Title schema path"));
            Assert.assertNotNull(titleSchemaElt);
            Assert.assertNotNull(titleSchemaElt.getCustomSearchTypes());
            Assert.assertEquals(titleSchemaElt.getCustomSearchTypes().size(), 2);
            Assert.assertEquals(titleSchemaElt.getCustomSearchTypes().get(0), "Strict");
            Assert.assertEquals(titleSchemaElt.getCustomSearchTypes().get(1), "Minimal");

            SchemaResponse descriptionSchemaElt = schemaResponses
                .stream()
                .filter(schemaResponse -> schemaResponse.getPath().equals("Description"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find Description schema path"));
            Assert.assertNotNull(descriptionSchemaElt);
            Assert.assertNotNull(descriptionSchemaElt.getCustomSearchTypes());
            Assert.assertEquals(descriptionSchemaElt.getCustomSearchTypes().get(0), "Strict");
        }

        // Given
        prepareVitamSession(1); //Using default tenants
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            RequestResponse<SchemaResponse> unitSchemaResponse = client.getUnitSchema();
            List<SchemaResponse> schemaResponses =
                ((RequestResponseOK<SchemaResponse>) unitSchemaResponse).getResults();
            Assert.assertNotNull(schemaResponses);
            SchemaResponse titleSchemaElt = schemaResponses
                .stream()
                .filter(schemaResponse -> schemaResponse.getPath().equals(TITLE))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find Title schema path"));
            Assert.assertNotNull(titleSchemaElt);
            Assert.assertNotNull(titleSchemaElt.getCustomSearchTypes());
            Assert.assertEquals(titleSchemaElt.getCustomSearchTypes().size(), 1);
            Assert.assertEquals(titleSchemaElt.getCustomSearchTypes().get(0), "Strict");

            SchemaResponse descriptionSchemaElt = schemaResponses
                .stream()
                .filter(schemaResponse -> schemaResponse.getPath().equals("Description"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find Description schema path"));
            Assert.assertNotNull(descriptionSchemaElt);
            Assert.assertNotNull(descriptionSchemaElt.getCustomSearchTypes());
            Assert.assertEquals(descriptionSchemaElt.getCustomSearchTypes().size(), 1);
            Assert.assertEquals(descriptionSchemaElt.getCustomSearchTypes().get(0), "Minimal");
        }
    }

    private static String loadMappingFromResources(String fileName) {
        try {
            ClassLoader classLoader = SearchByTenantIT.class.getClassLoader();
            URL resource = classLoader.getResource("functional-admin/es-mapping/" + fileName);
            return Paths.get(resource.toURI()).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
