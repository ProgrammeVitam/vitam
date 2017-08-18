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
package fr.gouv.vitam.storage.engine.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.gouv.vitam.common.SingletonUtils;
import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceApplication;

/**
 * !!! WARNING !!! : in case of modification of class fr.gouv.vitam.driver.fake.FakeDriverImpl, you need to recompile
 * the storage-offer-mock.jar from the storage-offer-mock module and copy it in src/test/resources in place of the
 * previous one.
 */
public class StorageClientIT {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageClientIT.class);
    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not have raized an exception";
    private static WorkspaceApplication workspaceApplication;

    private static final String REST_URI = StorageClientFactory.RESOURCE_PATH;
    private static final String STORAGE_CONF = "integration-storage/storage-engine.conf";
    private static int serverPort = 8583;
    private static final int workspacePort = 8987;
    private static StorageClient storageClient;
    private static WorkspaceClient workspaceClient;
    private static StorageMain storageMain;

    private static final String CONTAINER_1 = "aeaaaaaaaaaam7mxaaaamakwfnzbudaaaaaq";
    private static final String CONTAINER_2 = "aeaaaaaaaaaam7mxaaaamakwfnzbudaaaaaz";
    private static final String CONTAINER_3 = "aeaaaaaaaaaam7mxaaaamakwfnzbudaaaapq";
    private static final String OBJECT =
        "integration-storage/e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804.odp";

    private static final String OBJECT_ID =
        "e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804";

    private static final String REPORT =
        "e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa803";

    private static final String MANIFEST =
        "e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa802";
    private static final String TMP_FOLDER = "tmp";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        // junitHelper = JunitHelper.getInstance();
        // serverPort = junitHelper.findAvailablePort();
        // launch workspace
        workspaceApplication = new WorkspaceApplication("integration-storage/workspace.conf");
        workspaceApplication.start();
        RestAssured.port = serverPort;
        RestAssured.basePath = REST_URI;
        File storageConfigurationFile = PropertiesUtils.findFile(STORAGE_CONF);
        final StorageConfiguration serverConfiguration =
            PropertiesUtils.readYaml(storageConfigurationFile, StorageConfiguration.class);
        final Pattern compiledPattern = Pattern.compile(":(\\d+)");
        final Matcher matcher = compiledPattern.matcher(serverConfiguration.getUrlWorkspace());
        if (matcher.find()) {
            final String seg[] = serverConfiguration.getUrlWorkspace().split(":(\\d+)");
            serverConfiguration.setUrlWorkspace(seg[0]);
        }
        serverConfiguration
            .setUrlWorkspace(serverConfiguration.getUrlWorkspace() + ":" + Integer.toString(workspacePort));
        TemporaryFolder folder = new TemporaryFolder();
        folder.create();
        ArrayList<Integer> tenants = new ArrayList<Integer>();
        serverConfiguration.setTenants(tenants);
        serverConfiguration.setZippingDirecorty(TMP_FOLDER);
        serverConfiguration.setLoggingDirectory(TMP_FOLDER);

        PropertiesUtils.writeYaml(storageConfigurationFile, serverConfiguration);

        storageMain = new StorageMain(STORAGE_CONF);
        storageMain.start();

        storageClient = StorageClientFactory.getInstance().getClient();

        WorkspaceClientFactory.changeMode("http://localhost:" + workspacePort);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        destroyWorkspaceFiles();
        createWorkspaceFiles();
    }

    private static void createWorkspaceFiles()
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException,
        FileNotFoundException {
        try {
            workspaceClient.createContainer(CONTAINER_1);
            workspaceClient.createContainer(CONTAINER_2);
            workspaceClient.createContainer(CONTAINER_3);
        } catch (final Exception e) {
            LOGGER.error("Error creating container : " + e);
        }
        try {
            FileInputStream stream = new FileInputStream(
                PropertiesUtils.findFile(
                    OBJECT));
            workspaceClient.putObject(CONTAINER_1,
                OBJECT_ID,
                stream);
            stream = new FileInputStream(
                PropertiesUtils.findFile(
                    OBJECT));
            workspaceClient.putObject(CONTAINER_2,
                REPORT,
                stream);
            StreamUtils.closeSilently(workspaceClient.getObject(CONTAINER_1, OBJECT_ID).readEntity(InputStream.class));
        } catch (final Exception e) {
            LOGGER.error("Error getting or putting object : " + e);
        }

    }

    private static void destroyWorkspaceFiles()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        try {
            workspaceClient.deleteObject(CONTAINER_1,
                OBJECT_ID);
            workspaceClient.deleteObject(CONTAINER_2,
                REPORT);
            workspaceClient.deleteObject(CONTAINER_3,
                MANIFEST);
        } catch (final Exception e) {
            LOGGER.error("Error deleting object : " + e);
        }
        try {
            workspaceClient.deleteContainer(CONTAINER_1, true);
            workspaceClient.deleteContainer(CONTAINER_2, true);
            workspaceClient.deleteContainer(CONTAINER_3, true);
        } catch (final Exception e) {
            LOGGER.error("Error deleting container : " + e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        destroyWorkspaceFiles();
        workspaceApplication.stop();
        storageMain.stop();
        // junitHelper.releasePort(workspacePort);
        // junitHelper.releasePort(serverPort);
    }


    // TODO P0 test integration to finish (bug on exiting folder)
    @Test
    public final void testStorage() throws VitamClientException, FileNotFoundException {
        try {
            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(CONTAINER_1);
            description.setWorkspaceObjectURI(OBJECT_ID);
            final ObjectDescription description1 = new ObjectDescription();
            description1.setWorkspaceContainerGUID(CONTAINER_2);
            description1.setWorkspaceObjectURI(REPORT);

            final ObjectDescription description2 = new ObjectDescription();
            description2.setWorkspaceContainerGUID(CONTAINER_3);
            description2.setWorkspaceObjectURI(MANIFEST);
            // status
            // storageClient.getStatus();
            try {
                final JsonNode node = storageClient.getStorageInformation("default");
                assertNotNull(node);
                // fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
            } catch (final VitamException svce) {
                LOGGER.error(svce);
                fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
            }
            // TODO P0 : when implemented, uncomment this
            /*
             * try { storageClient.exists("0", "default", StorageCollectionType.OBJECTS, "objectId");
             * fail(SHOULD_NOT_RAIZED_AN_EXCEPTION); } catch (StorageServerClientException svce) { // not yet
             * implemented }
             */
            try {
                storageClient.storeFileFromWorkspace("default", StorageCollectionType.OBJECTS, "objectId",
                    description);
                storageClient.storeFileFromWorkspace("default2", StorageCollectionType.OBJECTS, "objectId",
                    description);
            } catch (final StorageServerClientException svce) {
                LOGGER.error(svce);
                fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
            }

            try {
                storageClient.storeFileFromWorkspace("default", StorageCollectionType.REPORTS, "objectId",
                    description1);
            } catch (final StorageServerClientException svce) {
                LOGGER.error(svce);
                fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
            }

            try {
                storageClient.storeFileFromWorkspace("default", StorageCollectionType.MANIFESTS, "objectId",
                    description2);
            } catch (final StorageServerClientException svce) {
                LOGGER.error(svce);
                fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
            }


            try {
                final InputStream stream =
                    storageClient.getContainerAsync("default", OBJECT_ID, StorageCollectionType.OBJECTS)
                        .readEntity(InputStream.class);
                assertNotNull(stream);
                final InputStream stream2 =
                    storageClient.getContainerAsync("default2", OBJECT_ID, StorageCollectionType.OBJECTS)
                        .readEntity(InputStream.class);
                assertNotNull(stream2);
            } catch (StorageServerClientException | StorageNotFoundException svce) {
                fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
            }

            try {
                final InputStream stream =
                    storageClient.getContainerAsync("default", REPORT, StorageCollectionType.REPORTS)
                        .readEntity(InputStream.class);
                assertNotNull(stream);
            } catch (StorageServerClientException | StorageNotFoundException svce) {
                LOGGER.error(svce);
                fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
            }

            try {
                final InputStream stream =
                    storageClient.getContainerAsync("default", MANIFEST, StorageCollectionType.MANIFESTS)
                        .readEntity(InputStream.class);
                assertNotNull(stream);
            } catch (StorageServerClientException | StorageNotFoundException svce) {
                LOGGER.error(svce);
                fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
            }

            try {
                assertTrue(storageClient.exists("default", StorageCollectionType.MANIFESTS, MANIFEST, SingletonUtils.singletonList()));
            } catch (StorageServerClientException svce) { // not yet implemented
                LOGGER.error(svce);
                fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
            }

            try {
                final InputStream stream =
                    storageClient.getContainerAsync("default", MANIFEST, StorageCollectionType.MANIFESTS)
                        .readEntity(InputStream.class);
                assertNotNull(stream);
                Digest digest = Digest.digest(stream, VitamConfiguration.getDefaultDigestType());
                storageClient.delete("default", StorageCollectionType.OBJECTS, "objectId", digest.toString(),
                    VitamConfiguration.getDefaultDigestType());
            } catch (Exception svce) {
                LOGGER.error(svce);
                fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
            }



        } catch (

        final Exception e) {
            e.printStackTrace();
            fail("should not raized an exception");
        }
    }

}
