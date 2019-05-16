/*******************************************************************************
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
 *******************************************************************************/

package fr.gouv.vitam.storage.offers.common.driver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import fr.gouv.vitam.common.client.VitamClientFactory;
import org.bson.Document;
import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.StorageListRequest;
import fr.gouv.vitam.storage.driver.model.StorageObjectRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutResult;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import fr.gouv.vitam.storage.offers.common.database.OfferLogDatabaseService;
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferMain;
import fr.gouv.vitam.storage.offers.common.rest.OfferConfiguration;
import fr.gouv.vitam.storage.offers.workspace.driver.DriverImpl;

/**
 * Integration driver offer tests
 */
public class DriverToOfferTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DriverToOfferTest.class);

    private static final String WORKSPACE_OFFER_CONF = "storage-default-offer-ssl.conf";
    private static final String DEFAULT_STORAGE_CONF = "default-storage.conf";
    private static final String ARCHIVE_FILE_TXT = "archivefile.txt";
    private static final String DATABASE_NAME = "Vitam";

    private static DriverImpl driver;

    private static final String REST_URI = "/offer/v1";

    private static int serverPort;
    private static JunitHelper junitHelper;

    private static Connection connection;
    private static String guid;
    private static DefaultOfferMain application;

    private static int TENANT_ID;

    private static String CONTAINER;
    private static StorageOffer offer = new StorageOffer();

    @Rule
    public MongoRule mongoRule =
        new MongoRule(VitamCollection.getMongoClientOptions(), DATABASE_NAME,
            OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME);


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        final File workspaceOffer = PropertiesUtils.findFile(WORKSPACE_OFFER_CONF);
        final OfferConfiguration realWorkspaceOffer = PropertiesUtils.readYaml(workspaceOffer, OfferConfiguration.class);
        File newWorkspaceOfferConf = File.createTempFile("test", WORKSPACE_OFFER_CONF, workspaceOffer.getParentFile());
        List<MongoDbNode> mongoDbNodes = realWorkspaceOffer.getMongoDbNodes();
        mongoDbNodes.get(0).setDbPort(MongoRule.getDataBasePort());
        realWorkspaceOffer.setMongoDbNodes(mongoDbNodes);
        PropertiesUtils.writeYaml(newWorkspaceOfferConf, realWorkspaceOffer);

        try {
            junitHelper = JunitHelper.getInstance();
            serverPort = junitHelper.findAvailablePort();

            RestAssured.port = serverPort;
            RestAssured.basePath = REST_URI;

            application = new DefaultOfferMain(newWorkspaceOfferConf.getAbsolutePath());
            application.start();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Wokspace Offer Application Server", e);
        }

        driver = new DriverImpl();
        // for parallel test
        TENANT_ID = serverPort;
        CONTAINER = TENANT_ID + "_" + DataCategory.UNIT.getFolder();
    }

    @AfterClass
    public static void shutdownAfterClass() throws Exception {
        if (connection != null) {
            connection.close();
        }
        junitHelper.releasePort(serverPort);

        application.stop();
        VitamClientFactory.resetConnections();

        // delete files
        final StorageConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
                StorageConfiguration.class);
        final File container = new File(conf.getStoragePath() + CONTAINER);
        File object = new File(container.getAbsolutePath(), guid);
        Files.deleteIfExists(object.toPath());
        for (int i = 0; i < 150; i++) {
            object = new File(container.getAbsolutePath(), "f" + i);
            Files.deleteIfExists(object.toPath());
        }
        Files.deleteIfExists(container.toPath());
    }

    @Test
    public void integrationTest() throws Exception {
        // check offer database emptiness
        FindIterable<Document> results = mongoRule.getMongoClient().getDatabase(DATABASE_NAME).getCollection
            (OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME).find();
        assertThat(results).hasSize(0);

        offer.setBaseUrl("https://localhost:" + serverPort);
        Map<String, String> tmp = new HashMap<>();
        tmp.put("keyStore-keyPath", "src/test/resources/client.p12");
        tmp.put("keyStore-keyPassword", "azerty4");
        tmp.put("trustStore-keyPath", "src/test/resources/truststore.jks");
        tmp.put("trustStore-keyPassword", "azerty10");
        offer.setParameters(tmp);
        offer.setId("Default");

        driver.addOffer(offer, null);
        
        connection = driver.connect(offer.getId());
        assertNotNull(connection);

        StoragePutRequest request;
        guid = GUIDFactory.newObjectGUID(TENANT_ID).toString();
        File archiveFile = PropertiesUtils.findFile(ARCHIVE_FILE_TXT);
        try (FileInputStream fin = new FileInputStream(archiveFile)) {
            final MessageDigest messageDigest = MessageDigest.getInstance(VitamConfiguration.getDefaultDigestType().getName());
            try (DigestInputStream digestInputStream = new DigestInputStream(fin, messageDigest)) {
                request = new StoragePutRequest(TENANT_ID, DataCategory.UNIT.getFolder(), guid,
                        VitamConfiguration.getDefaultDigestType().getName(), digestInputStream);
                request.setSize(archiveFile.length());
                final StoragePutResult result = connection.putObject(request);
                assertNotNull(result);

                final StorageConfiguration conf = PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
                        StorageConfiguration.class);
                final File container = new File(conf.getStoragePath() + CONTAINER);
                assertNotNull(container);
                final File object = new File(container.getAbsolutePath(), guid);
                assertNotNull(object);
                assertTrue(com.google.common.io.Files.equal(PropertiesUtils.findFile(ARCHIVE_FILE_TXT), object));

                final String digestToCheck = result.getDigestHashBase16();
                assertNotNull(digestToCheck);
                assertEquals(digestToCheck, BaseXx.getBase16(messageDigest.digest()));
            }
        }

        results = mongoRule.getMongoClient().getDatabase(DATABASE_NAME).getCollection
            (OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME).find(Filters.and(Filters.eq("Container", TENANT_ID +
                "_unit"),Filters.eq("FileName", guid)));
        assertThat(results).hasSize(1);

        try (FileInputStream fin = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            request = new StoragePutRequest(null, DataCategory.UNIT.name(), guid,
                    VitamConfiguration.getDefaultDigestType().getName(), fin);
            request.setSize(archiveFile.length());
            connection.putObject(request);
            fail("Should have an exception !");
        } catch (final StorageDriverException exc) {
            // Nothing, missing tenant parameter
        }

        final StorageObjectRequest getRequest = new StorageObjectRequest(TENANT_ID, DataCategory.UNIT.getFolder(), guid);
        connection.getObject(getRequest);

        // Add some objects
        for (int i = 0; i < 150; i++) {
            try (FakeInputStream fis = new FakeInputStream(50)) {
                request = new StoragePutRequest(TENANT_ID, DataCategory.UNIT.name(), "f" + i,
                        VitamConfiguration.getDefaultDigestType().getName(), fis);
                request.setSize(50);
                connection.putObject(request);
            }
        }
        results = mongoRule.getMongoClient().getDatabase(DATABASE_NAME).getCollection
            (OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME).find(Filters.eq("Container", TENANT_ID + "_unit"));
        // Take into account first object created at the beginning !!!
        assertThat(results).hasSize(151);

        for (int i = 0; i < 150; i++) {
            results = mongoRule.getMongoClient().getDatabase(DATABASE_NAME).getCollection
                (OfferLogDatabaseService.OFFER_LOG_COLLECTION_NAME).find(Filters.and(Filters.eq("Container", TENANT_ID +
                "_unit"),Filters.eq("FileName", "f" + i)));
            assertThat(results).hasSize(1);
        }

        StorageListRequest listRequest = new StorageListRequest(TENANT_ID, DataCategory.UNIT.getFolder(), null, true);
        RequestResponse<JsonNode> response = connection.listObjects(listRequest);
        assertNotNull(response);
        assertEquals(Response.Status.PARTIAL_CONTENT.getStatusCode(), response.getHttpCode());
        assertNotNull(response.getHeaderString(GlobalDataRest.X_CURSOR_ID));

        listRequest = new StorageListRequest(TENANT_ID, DataCategory.UNIT.getFolder(),
                response.getHeaderString(GlobalDataRest.X_CURSOR_ID), true);
        response = connection.listObjects(listRequest);
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getHttpCode());
        assertNotNull(response.getHeaderString(GlobalDataRest.X_CURSOR_ID));
    }
}
