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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import javax.ws.rs.core.Response;

import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;

import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamRequestIterator;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.StorageListRequest;
import fr.gouv.vitam.storage.driver.model.StorageObjectRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutResult;
import fr.gouv.vitam.storage.driver.model.StorageRequest;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferApplication;
import fr.gouv.vitam.storage.offers.workspace.driver.DriverImpl;

/**
 * Integration driver offer tests
 */
public class DriverToOfferTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DriverToOfferTest.class);

    private static final String WORKSPACE_OFFER_CONF = "storage-default-offer.conf";
    private static final String DEFAULT_STORAGE_CONF = "default-storage.conf";
    private static final String ARCHIVE_FILE_TXT = "archivefile.txt";

    private static DriverImpl driver;

    private static final String REST_URI = "/offer/v1";

    private static int serverPort;
    private static JunitHelper junitHelper;

    private static Connection connection;
    private static String guid;
    private static DefaultOfferApplication application;

    private static int TENANT_ID;

    private static String CONTAINER;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();

        RestAssured.port = serverPort;
        RestAssured.basePath = REST_URI;

        final File workspaceOffer = PropertiesUtils.findFile(WORKSPACE_OFFER_CONF);
        final StorageConfiguration realWorkspaceOffer =
            PropertiesUtils.readYaml(workspaceOffer, StorageConfiguration.class);
        // newWorkspaceOfferConf = File.createTempFile("test", WORKSPACE_OFFER_CONF, workspaceOffer.getParentFile());
        // PropertiesUtils.writeYaml(newWorkspaceOfferConf, realWorkspaceOffer);

        try {
            application = new DefaultOfferApplication(realWorkspaceOffer);
            application.start();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Wokspace Offer Application Server", e);
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

    @RunWithCustomExecutor
    @Test
    public void integrationTest() throws Exception {
        connection = driver.connect("http://localhost:" + serverPort, null);
        assertNotNull(connection);

        StoragePutRequest request = null;
        guid = GUIDFactory.newObjectGUID(TENANT_ID).toString();
        try (FileInputStream fin = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            final MessageDigest messageDigest =
                MessageDigest.getInstance(VitamConfiguration.getDefaultDigestType().getName());
            try (DigestInputStream digestInputStream = new DigestInputStream(fin, messageDigest)) {
                request = new StoragePutRequest(TENANT_ID, DataCategory.UNIT.getFolder(), guid,
                    VitamConfiguration.getDefaultDigestType().name(), digestInputStream);
                final StoragePutResult result = connection.putObject(request);
                assertNotNull(result);

                final StorageConfiguration conf =
                    PropertiesUtils.readYaml(PropertiesUtils.findFile(DEFAULT_STORAGE_CONF),
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

        try (FileInputStream fin = new FileInputStream(PropertiesUtils.findFile(ARCHIVE_FILE_TXT))) {
            request = new StoragePutRequest(null, DataCategory.UNIT.name(), guid,
                VitamConfiguration.getDefaultDigestType().getName(), fin);
            connection.putObject(request);
            fail("Should have an exception !");
        } catch (final StorageDriverException exc) {
            // Nothing, missing tenant parameter
        }

        final StorageObjectRequest getRequest =
            new StorageObjectRequest(TENANT_ID, DataCategory.UNIT.getFolder(), guid);
        connection.getObject(getRequest);

        // Add some objects
        for (int i = 0; i < 150; i++) {
            try (FakeInputStream fis = new FakeInputStream(50, false)) {
                request = new StoragePutRequest(TENANT_ID, DataCategory.UNIT.name(), "f" + i,
                    VitamConfiguration.getDefaultDigestType().name(), fis);
                connection.putObject(request);
            }
        }

        StorageListRequest listRequest = new StorageListRequest(TENANT_ID, DataCategory.UNIT.getFolder(),
            null, true);
        Response response = connection.listObjects(listRequest);
        assertNotNull(response);
        assertEquals(Response.Status.PARTIAL_CONTENT.getStatusCode(), response.getStatus());
        assertNotNull(response.getHeaderString(GlobalDataRest.X_CURSOR_ID));

        listRequest = new StorageListRequest(TENANT_ID, DataCategory.UNIT.getFolder(), response.getHeaderString(GlobalDataRest.X_CURSOR_ID), true);
        response = connection.listObjects(listRequest);
        assertNotNull(response);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getHeaderString(GlobalDataRest.X_CURSOR_ID));
    }
}
