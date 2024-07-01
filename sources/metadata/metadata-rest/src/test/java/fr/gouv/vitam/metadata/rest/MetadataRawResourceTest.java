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
package fr.gouv.vitam.metadata.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.config.CollectionConfiguration;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.metadata.core.config.DefaultCollectionConfiguration;
import fr.gouv.vitam.metadata.core.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.core.config.MetadataIndexationConfiguration;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.core.mapping.MappingLoader;
import fr.gouv.vitam.metadata.rest.utils.MappingLoaderTestUtils;
import io.restassured.RestAssured;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.metadata.core.database.collections.MetadataCollections.OBJECTGROUP;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataCollections.UNIT;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * MetadataRawResource test
 */
public class MetadataRawResourceTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final String METADATA_URI = "/metadata/v1";
    private static final String JETTY_CONFIG = "jetty-config-test.xml";

    private static final String HOST_NAME = "127.0.0.1";
    private static JunitHelper junitHelper;
    private static int serverPort;

    private static MetadataMain application;
    private static final Integer TENANT_ID = 0;
    static final List tenantList = Lists.newArrayList(TENANT_ID);

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(Unit.class, ObjectGroup.class)
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        UNIT.getVitamCollection().setName(GUIDFactory.newGUID().getId() + UNIT.getClasz().getSimpleName());
        mongoRule.addCollectionToBePurged(UNIT.getName());
        OBJECTGROUP.getVitamCollection()
            .setName(GUIDFactory.newGUID().getId() + OBJECTGROUP.getClasz().getSimpleName());
        mongoRule.addCollectionToBePurged(OBJECTGROUP.getName());
        junitHelper = JunitHelper.getInstance();

        List<ElasticsearchNode> esNodes = Lists.newArrayList(
            new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())
        );

        final List<MongoDbNode> mongo_nodes = new ArrayList<>();
        mongo_nodes.add(new MongoDbNode(HOST_NAME, MongoRule.getDataBasePort()));

        MappingLoader mappingLoader = MappingLoaderTestUtils.getTestMappingLoader();

        final MetaDataConfiguration configuration = new MetaDataConfiguration(
            mongo_nodes,
            MongoRule.VITAM_DB,
            ElasticsearchRule.VITAM_CLUSTER,
            esNodes,
            mappingLoader
        );
        configuration.setJettyConfig(JETTY_CONFIG);
        configuration.setUrlProcessing("http://processing.service.consul:8203/");
        configuration.setContextPath("/metadata");
        configuration.setIndexationConfiguration(
            new MetadataIndexationConfiguration()
                .setDefaultCollectionConfiguration(
                    new DefaultCollectionConfiguration()
                        .setUnit(new CollectionConfiguration(1, 0))
                        .setObjectgroup(new CollectionConfiguration(1, 0))
                )
        );

        VitamConfiguration.setTenants(tenantList);
        serverPort = junitHelper.findAvailablePort();

        File configurationFile = tempFolder.newFile();

        PropertiesUtils.writeYaml(configurationFile, configuration);

        application = new MetadataMain(configurationFile.getAbsolutePath());
        application.start();
        JunitHelper.unsetJettyPortSystemProperty();

        RestAssured.port = serverPort;
        RestAssured.basePath = METADATA_URI;
    }

    @AfterClass
    public static void tearDownAfterClass() {
        mongoRule.handleAfterClass();

        try {
            application.stop();
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.syserr("", e);
        }
        junitHelper.releasePort(serverPort);
        VitamClientFactory.resetConnections();
    }

    @After
    public void tearDown() {
        mongoRule.handleAfter();
    }

    @RunWithCustomExecutor
    @Test
    public void should_find_objectgroup_on_getByIdRaw() throws Exception {
        String operationId = GUIDFactory.newOperationLogbookGUID(TENANT_ID).getId();
        String unitId = GUIDFactory.newUnitGUID(TENANT_ID).getId();
        String objectGroupId = GUIDFactory.newObjectGroupGUID(TENANT_ID).getId();
        ObjectNode objectGroup = (ObjectNode) JsonHandler.getFromInputStream(
            PropertiesUtils.getResourceAsStream("objectgroup.json")
        );
        objectGroup.put("_id", objectGroupId);
        objectGroup.set("_ops", JsonHandler.createArrayNode().add(operationId));
        objectGroup.set("_up", JsonHandler.createArrayNode().add(unitId));
        OBJECTGROUP.getCollection().insertOne(new ObjectGroup(objectGroup));
        String reponseString = given()
            .contentType(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get("/raw/objectgroups/" + objectGroupId)
            .then()
            .statusCode(Status.OK.getStatusCode())
            .extract()
            .body()
            .asString();

        JsonNode responseUnit = JsonHandler.getFromString(reponseString);
        assertThat(responseUnit.get("$results").get(0).get("_nbc").asLong()).isEqualTo(1L);
    }

    @RunWithCustomExecutor
    @Test
    public void should_not_find_objectgroup_on_getByIdRaw() throws Exception {
        String objectGroupId = GUIDFactory.newObjectGroupGUID(TENANT_ID).getId();
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get("/raw/objectgroups/" + objectGroupId)
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }

    @RunWithCustomExecutor
    @Test
    public void should_find_unit_on_getByIdRaw() throws Exception {
        String operationId = GUIDFactory.newOperationLogbookGUID(TENANT_ID).getId();
        String unitId = GUIDFactory.newUnitGUID(TENANT_ID).getId();
        String parentUnitId = GUIDFactory.newUnitGUID(TENANT_ID).getId();
        ObjectNode unit = (ObjectNode) JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream("unit.json"));
        unit.put("_id", unitId);
        unit.set("_ops", JsonHandler.createArrayNode().add(operationId));
        unit.set("_up", JsonHandler.createArrayNode().add(parentUnitId));
        unit.put("_nbc", 1L);
        UNIT.getCollection().insertOne(new Unit(unit));

        String reponseString = given()
            .contentType(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get("/raw/units/" + unitId)
            .then()
            .statusCode(Status.OK.getStatusCode())
            .extract()
            .body()
            .asString();

        JsonNode responseUnit = JsonHandler.getFromString(reponseString);
        assertThat(responseUnit.get("$results").get(0).get("_nbc").asLong()).isEqualTo(1L);
    }

    @RunWithCustomExecutor
    @Test
    public void should_not_find_unit_on_getByIdRaw() throws Exception {
        String unitId = GUIDFactory.newUnitGUID(TENANT_ID).getId();
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when()
            .get("/raw/units/" + unitId)
            .then()
            .statusCode(Status.NOT_FOUND.getStatusCode());
    }
}
