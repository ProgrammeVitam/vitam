package fr.gouv.vitam.storage.engine.client;

import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.jayway.restassured.RestAssured;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.BasicVitamServer;
import fr.gouv.vitam.common.server.VitamServer;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
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
    private static final String SHOULD_RAIZED_AN_EXCEPTION = "Should have raized an exception";


    private static final String REST_URI = StorageClient.RESOURCE_PATH;
    private static final String STORAGE_CONF = "storage-engine.conf";
    private static JunitHelper junitHelper;
    private static VitamServer vitamServer;
    private static int serverPort;
    private static int workspacePort;
    private static StorageClient storageClient;
    private static WorkspaceApplication workspaceApplication;
    private static WorkspaceClient workspaceClient;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        junitHelper = new JunitHelper();
        serverPort = junitHelper.findAvailablePort();
        workspacePort = junitHelper.findAvailablePort();
        // launch workspace
        workspaceApplication = new WorkspaceApplication();
        workspaceApplication
            .configure(PropertiesUtils.getResourcesPath("workspace.conf").toString(), Integer.toString(workspacePort));

        RestAssured.port = serverPort;
        RestAssured.basePath = REST_URI;
        StorageConfiguration serverConfiguration =
            PropertiesUtils.readYaml(PropertiesUtils.findFile(STORAGE_CONF), StorageConfiguration.class);
        serverConfiguration
            .setUrlWorkspace(serverConfiguration.getUrlWorkspace() + ":" + Integer.toString(workspacePort));
        PropertiesUtils.writeYaml(PropertiesUtils.findFile(STORAGE_CONF), serverConfiguration);
        try {
            vitamServer = StorageApplication.startApplication(
                new String[] {PropertiesUtils.getResourcesFile(STORAGE_CONF).getAbsolutePath(),
                    Integer.toString(serverPort)});
            ((BasicVitamServer) vitamServer).start();
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
        createWorkspaceFiles();
    }

    private static void createWorkspaceFiles()
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException,
        FileNotFoundException {
        workspaceClient.createContainer("aeaaaaaaaaaam7mxaaaamakwfnzbudaaaaaq");
        workspaceClient.createFolder("aeaaaaaaaaaam7mxaaaamakwfnzbudaaaaaq", "SIP");
        workspaceClient.createFolder("aeaaaaaaaaaam7mxaaaamakwfnzbudaaaaaq/SIP", "content");
        FileInputStream stream = new FileInputStream(
            PropertiesUtils.findFile(
                "e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804.odp"));
        workspaceClient.putObject("aeaaaaaaaaaam7mxaaaamakwfnzbudaaaaaq/SIP/content",
            "e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804.odp",
            stream);

    }

    private static void destroyWorkspaceFiles()
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        workspaceClient.deleteObject("aeaaaaaaaaaam7mxaaaamakwfnzbudaaaaaq/SIP/content",
            "e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804.odp");
        workspaceClient.deleteFolder("aeaaaaaaaaaam7mxaaaamakwfnzbudaaaaaq/SIP", "content");
        workspaceClient.deleteFolder("aeaaaaaaaaaam7mxaaaamakwfnzbudaaaaaq", "SIP");
        workspaceClient.deleteContainer("aeaaaaaaaaaam7mxaaaamakwfnzbudaaaaaq");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        destroyWorkspaceFiles();
        storageClient.shutdown();
        try {
            ((BasicVitamServer) vitamServer).stop();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        workspaceApplication.stop();
        junitHelper.releasePort(workspacePort);
        junitHelper.releasePort(serverPort);
    }


    /**
     * TODO test integration to finish (bug on exiting folder)
     */
    @Test
    @Ignore
    public final void testStorage() throws VitamClientException, FileNotFoundException {
        CreateObjectDescription description = new CreateObjectDescription();
        description.setWorkspaceContainerGUID("aeaaaaaaaaaam7mxaaaamakwfnzbudaaaaaq");
        description.setWorkspaceObjectURI(
            "SIP/content/e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804.odp");

        // status
        storageClient.getStatus();
        try {
            storageClient.getStorageInfos("0", "default");
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (StorageServerClientException svce) {
            // not yet implemented
        }
        try {
            storageClient.exists("0", "default", StorageCollectionType.OBJECTS, "objectId");
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (StorageServerClientException svce) {
            // not yet implemented
        }
        try {
            storageClient.storeFileFromWorkspace("0", "default", StorageCollectionType.OBJECTS, "objectId",
                description);
        } catch (StorageServerClientException svce) {
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        }

        try {
            storageClient.exists("0", "default", StorageCollectionType.OBJECTS, "objectId");
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (StorageServerClientException svce) {
            // not yet implemented
        }
        try {
            storageClient.delete("0", "default", StorageCollectionType.OBJECTS, "objectId");
            fail(SHOULD_RAIZED_AN_EXCEPTION);
        } catch (StorageServerClientException svce) {
            // not yet implemented
        }

        try {
            workspaceClient.createContainer("aeaaaaaaaaaam7mxaaaamakwfnzbudaaaaaq");
            workspaceClient.createFolder("aeaaaaaaaaaam7mxaaaamakwfnzbudaaaaaq", "SIP");
            workspaceClient.createFolder("aeaaaaaaaaaam7mxaaaamakwfnzbudaaaaaq", "SIP/content");
        } catch (ContentAddressableStorageAlreadyExistException | ContentAddressableStorageServerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }



}
