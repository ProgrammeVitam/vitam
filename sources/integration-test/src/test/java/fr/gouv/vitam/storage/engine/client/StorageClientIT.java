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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SingletonUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;

/**
 * !!! WARNING !!! : in case of modification of class fr.gouv.vitam.driver.fake.FakeDriverImpl, you need to recompile
 * the storage-offer-mock.jar from the storage-offer-mock module and copy it in src/test/resources in place of the
 * previous one.
 */
@Ignore("Really test of storage and offer is developped in the BackupAndReconstructionIT that does not use mock")
public class StorageClientIT {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageClientIT.class);
    public static final String CONFIG_WORKSPACE_PATH = "integration-storage/workspace.conf";
    private static WorkspaceMain workspaceMain;

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

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        File vitamTempFolder = temporaryFolder.newFolder();
        String TMP_FOLDER = vitamTempFolder.getAbsolutePath();
        SystemPropertyUtil.set("vitam.tmp.folder", TMP_FOLDER);

        // junitHelper = JunitHelper.getInstance();
        // serverPort = junitHelper.findAvailablePort();
        // launch workspace
        File workspaceConfigurationFile = PropertiesUtils.findFile(CONFIG_WORKSPACE_PATH);
        final fr.gouv.vitam.common.storage.StorageConfiguration workspaceConfiguration =
            PropertiesUtils.readYaml(workspaceConfigurationFile, fr.gouv.vitam.common.storage.StorageConfiguration.class);
        workspaceConfiguration.setStoragePath(TMP_FOLDER);
        PropertiesUtils.writeYaml(workspaceConfigurationFile, workspaceConfiguration);

        workspaceMain = new WorkspaceMain(CONFIG_WORKSPACE_PATH);
        workspaceMain.start();
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

    private static void createWorkspaceFiles() {
        try {
            workspaceClient.createContainer(CONTAINER_1);
            workspaceClient.createContainer(CONTAINER_2);
            workspaceClient.createContainer(CONTAINER_3);
        } catch (final Exception e) {
            LOGGER.error("Error creating container : ", e);
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
            LOGGER.error("Error getting or putting object : ", e);
        }

    }

    private static void destroyWorkspaceFiles() {
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
        if(workspaceMain != null) {
            workspaceMain.stop();
        }
        if(storageMain != null) {
            storageMain.stop();
        }
        if(workspaceClient != null) {
            workspaceClient.close();
        }
        // junitHelper.releasePort(workspacePort);
        // junitHelper.releasePort(serverPort);
    }


    // TODO P0 test integration to finish (bug on exiting folder)
    @Test
    public final void testStorage() throws Exception {
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
        final JsonNode node = storageClient.getStorageInformation("default");
        assertNotNull(node);

        // TODO P0 : when implemented, uncomment this
        /*
         * try { storageClient.exists("0", "default", StorageCollectionType.OBJECTS, "objectId");
         * fail(SHOULD_NOT_RAIZED_AN_EXCEPTION); } catch (StorageServerClientException svce) { // not yet
         * implemented }
         */
        storageClient.storeFileFromWorkspace("default", DataCategory.OBJECT, "objectId",
            description);
        storageClient.storeFileFromWorkspace("default2", DataCategory.OBJECT, "objectId",
            description);

        storageClient.storeFileFromWorkspace("default", DataCategory.REPORT, "objectId",
            description1);

        storageClient.storeFileFromWorkspace("default", DataCategory.MANIFEST, "objectId",
            description2);

        final InputStream stream =
            storageClient.getContainerAsync("default", OBJECT_ID, DataCategory.OBJECT)
                .readEntity(InputStream.class);
        assertNotNull(stream);
        final InputStream stream2 =
            storageClient.getContainerAsync("default2", OBJECT_ID, DataCategory.OBJECT)
                .readEntity(InputStream.class);
        assertNotNull(stream2);

        final InputStream stream3 =
            storageClient.getContainerAsync("default", REPORT, DataCategory.REPORT)
                .readEntity(InputStream.class);
        assertNotNull(stream3);

        final InputStream stream4 =
            storageClient.getContainerAsync("default", MANIFEST, DataCategory.MANIFEST)
                .readEntity(InputStream.class);
        assertNotNull(stream4);

        assertTrue(storageClient
            .exists("default", DataCategory.MANIFEST, MANIFEST, SingletonUtils.singletonList()));

        final InputStream stream5 =
            storageClient.getContainerAsync("default", MANIFEST, DataCategory.MANIFEST)
                .readEntity(InputStream.class);
        assertNotNull(stream5);
        Digest digest = Digest.digest(stream5, VitamConfiguration.getDefaultDigestType());
        storageClient.delete("default", DataCategory.OBJECT, "objectId", digest.toString(),
            VitamConfiguration.getDefaultDigestType());
    }
}
