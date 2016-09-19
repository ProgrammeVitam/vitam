package fr.gouv.vitam.storage.engine.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.request.CreateObjectDescription;
import fr.gouv.vitam.storage.engine.server.rest.StorageApplication;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceApplication;

public class StorageClientIntegrationTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageClientIntegrationTest.class);
    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not have raized an exception";


    private static final String REST_URI = StorageClient.RESOURCE_PATH;
    private static final String STORAGE_CONF = "storage-engine.conf";
    // private static JunitHelper junitHelper;
    private static VitamServer vitamServer;
    private static int serverPort = 8583;
    private static final int workspacePort = 8987;
    private static StorageClient storageClient;
    private static WorkspaceApplication workspaceApplication;
    private static WorkspaceClient workspaceClient;


    private static final String CONTAINER = "aeaaaaaaaaaam7mxaaaamakwfnzbudaaaaaq";
    private static final String OBJECT =
        "e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804.odp";

    private static final String OBJECT_ID =
        "e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        // junitHelper = new JunitHelper();
        // serverPort = junitHelper.findAvailablePort();
        // launch workspace
        workspaceApplication = new WorkspaceApplication();
        WorkspaceApplication.startApplication("workspace.conf");
        RestAssured.port = serverPort;
        RestAssured.basePath = REST_URI;
        StorageConfiguration serverConfiguration =
            PropertiesUtils.readYaml(PropertiesUtils.findFile(STORAGE_CONF), StorageConfiguration.class);
        Pattern compiledPattern = Pattern.compile(":(\\d+)");
        Matcher matcher = compiledPattern.matcher(serverConfiguration.getUrlWorkspace());
        if (matcher.find()) {
            String seg[] = serverConfiguration.getUrlWorkspace().split(":(\\d+)");
            serverConfiguration.setUrlWorkspace(seg[0]);
        }
        serverConfiguration
            .setUrlWorkspace(serverConfiguration.getUrlWorkspace() + ":" + Integer.toString(workspacePort));
        PropertiesUtils.writeYaml(PropertiesUtils.findFile(STORAGE_CONF), serverConfiguration);
        try {
            StorageApplication.startApplication(
                STORAGE_CONF);
        } catch (VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Composite Application Server", e);
        }

        StorageClientConfiguration storageClientConfiguration = new StorageClientConfiguration("localhost", serverPort,
            false, "/");
        StorageClientFactory.setConfiguration(StorageClientFactory.StorageClientType.STORAGE,
            storageClientConfiguration);
        storageClient = StorageClientFactory.getInstance().getStorageClient();

        workspaceClient = WorkspaceClientFactory.create("http://localhost:" + workspacePort);
        destroyWorkspaceFiles();
        createWorkspaceFiles();
    }

    private static void createWorkspaceFiles()
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException,
        FileNotFoundException {
        try {
            workspaceClient.createContainer(CONTAINER);
        } catch (Exception e) {
            LOGGER.error("Error creating container : " + e);
        }
        try {
            FileInputStream stream = new FileInputStream(
                PropertiesUtils.findFile(
                    OBJECT));
            workspaceClient.putObject(CONTAINER,
                OBJECT_ID,
                stream);
            workspaceClient.getObject(CONTAINER, OBJECT_ID);
        } catch (Exception e) {
            LOGGER.error("Error getting or putting object : " + e);
        }

    }

    private static void destroyWorkspaceFiles()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        try {
            workspaceClient.deleteObject(CONTAINER,
                OBJECT_ID);
        } catch (Exception e) {
            LOGGER.error("Error deleting object : " + e);
        }
        try {
            workspaceClient.deleteContainer(CONTAINER);
        } catch (Exception e) {
            LOGGER.error("Error deleting container : " + e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        destroyWorkspaceFiles();
        storageClient.shutdown();
        workspaceApplication.stop();
        StorageApplication.stop();
        // junitHelper.releasePort(workspacePort);
        // junitHelper.releasePort(serverPort);
    }


    /**
     * TODO test integration to finish (bug on exiting folder)
     */
    @Test
    public final void testStorage() throws VitamClientException, FileNotFoundException {

        CreateObjectDescription description = new CreateObjectDescription();
        description.setWorkspaceContainerGUID(CONTAINER);
        description.setWorkspaceObjectURI(OBJECT_ID);

        // status
        storageClient.getStatus();
        try {
            JsonNode node = storageClient.getStorageInformation("0", "default");
            assertNotNull(node);
            // fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        } catch (VitamException svce) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
        // TODO : when implemented, uncomment this
        /*
         * try { storageClient.exists("0", "default", StorageCollectionType.OBJECTS, "objectId");
         * fail(SHOULD_NOT_RAIZED_AN_EXCEPTION); } catch (StorageServerClientException svce) { // not yet implemented }
         */
        try {
            storageClient.storeFileFromWorkspace("0", "default", StorageCollectionType.OBJECTS, "objectId",
                description);
        } catch (StorageServerClientException svce) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }

        try {
            InputStream stream = storageClient.getContainerObject("0", "default", OBJECT_ID);
            assertNotNull(stream);
        } catch (StorageServerClientException | StorageNotFoundException svce) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }

        // TODO : when implemented, uncomment this
        /*
         * try { storageClient.exists("0", "default", StorageCollectionType.OBJECTS, "objectId");
         * fail(SHOULD_NOT_RAIZED_AN_EXCEPTION); } catch (StorageServerClientException svce) { // not yet implemented }
         * try { storageClient.delete("0", "default", StorageCollectionType.OBJECTS, "objectId");
         * fail(SHOULD_NOT_RAIZED_AN_EXCEPTION); } catch (Exception svce) { // not yet implemented }
         */

    }

}
