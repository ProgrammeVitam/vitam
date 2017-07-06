/**
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
 */

package fr.gouv.vitam.storage.engine.client;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.functional.administration.rules.core.RulesSecurisator;
import org.apache.commons.io.FileUtils;
import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamRequestIterator;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.server.rest.StorageApplication;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferApplication;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceApplication;
import junit.framework.TestCase;

public class StorageTestMultiIT {
    private static final int NB_MULTIPLE_THREADS = 100;

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageTestMultiIT.class);

    private static DefaultOfferApplication defaultOfferApplication;
    private static final String DEFAULT_OFFER_CONF = "storage-test/storage-default-offer-ssl.conf";
    private static final String OFFER_FOLDER = "offer";

    private static StorageApplication storageApplication;
    private static StorageClient storageClient;
    private static final String STORAGE_CONF = "storage-test/storage-engine.conf";

    private static WorkspaceApplication workspaceApplication;
    private static WorkspaceClient workspaceClient;
    private static int workspacePort = 8987;
    private static final String WORKSPACE_CONF = "storage-test/workspace.conf";
    private static final String WORKSPACE_FOLDER = "workspace";
    private static final String TMP_FOLDER = "tmp";

    private static final String CONTAINER = "object";
    private static String OBJECT_ID;
    private static int size = 500;
    static TemporaryFolder folder = new TemporaryFolder();
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        // workspace
        workspaceApplication = new WorkspaceApplication(WORKSPACE_CONF);
        workspaceApplication.start();

        WorkspaceClientFactory.changeMode("http://localhost:" + workspacePort);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();

        // first offer
        final fr.gouv.vitam.common.storage.StorageConfiguration offerConfiguration = PropertiesUtils
            .readYaml(PropertiesUtils.findFile(DEFAULT_OFFER_CONF),
                fr.gouv.vitam.common.storage.StorageConfiguration.class);
        defaultOfferApplication = new DefaultOfferApplication(offerConfiguration);
        defaultOfferApplication.start();

        // storage engine
        final StorageConfiguration serverConfiguration =
            PropertiesUtils.readYaml(PropertiesUtils.findFile(STORAGE_CONF),
                StorageConfiguration.class);
        final Pattern compiledPattern = Pattern.compile(":(\\d+)");
        final Matcher matcher = compiledPattern.matcher(serverConfiguration.getUrlWorkspace());
        if (matcher.find()) {
            final String seg[] = serverConfiguration.getUrlWorkspace().split(":(\\d+)");
            serverConfiguration.setUrlWorkspace(seg[0]);
        }
        serverConfiguration
            .setUrlWorkspace(serverConfiguration.getUrlWorkspace() + ":" + Integer.toString(workspacePort));

        folder.create();
        serverConfiguration.setZippingDirecorty(folder.newFolder().getAbsolutePath());
        serverConfiguration.setLoggingDirectory(folder.newFolder().getAbsolutePath());

        try {
            storageApplication = new StorageApplication(serverConfiguration);
            storageApplication.start();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException("Cannot start the Composite Application Server", e);
        }

        StorageClientFactory.getInstance().setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        storageClient = StorageClientFactory.getInstance().getClient();

    }

    @AfterClass
    public static void afterClass() throws Exception {
        cleanWorkspace();
        // final clean, remove workspace folder
        try {
            workspaceClient.deleteContainer(WORKSPACE_FOLDER, true);
        } catch (Exception exc) {
            // nothing
        }
        // Ugly style but necessary because this is the folder representing the
        // workspace
        File workspaceFolder = new File(WORKSPACE_FOLDER);
        if (workspaceFolder.exists()) {
            try {
                // if clean workspace delete did not work
                FileUtils.cleanDirectory(workspaceFolder);
                FileUtils.deleteDirectory(workspaceFolder);
            } catch (Exception e) {
                // ignore
            }
        }
        workspaceClient.close();
        workspaceApplication.stop();
        cleanOffers();
        // delete offer parent folder
        File offerFolder = new File(OFFER_FOLDER);
        if (offerFolder.exists()) {
            try {
                // if clean offer delete did not work
                FileUtils.cleanDirectory(offerFolder);
                FileUtils.deleteDirectory(offerFolder);
            } catch (Exception e) {
                // ignore
            }
        }
        storageClient.close();
        defaultOfferApplication.stop();
        storageApplication.stop();
        folder.delete();
    }

    public static void afterTest() {
        cleanWorkspace();
        try {
            cleanOffers();
        } catch (Exception e) {
            // nothing
        }
    }

    private static void cleanWorkspace() {
        try {
            workspaceClient.deleteContainer(CONTAINER, true);
        } catch (Exception exc) {
            // nothing
        }
    }

    private static void cleanOffers() {
        // ugly style but we don't have the digest here
        File directory = new File(OFFER_FOLDER + "/" + "0_" + CONTAINER);
        try {
            FileUtils.cleanDirectory(directory);
            FileUtils.deleteDirectory(directory);
        } catch (IOException | IllegalArgumentException e) {
            // ignore
        }
    }

    private static void populateWorkspace() throws ContentAddressableStorageServerException {
        try (WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {
            try {
                workspaceClient.createContainer(CONTAINER);
            } catch (ContentAddressableStorageAlreadyExistException | ContentAddressableStorageServerException e) {
                // nothing
            }
    
            try (FakeInputStream fis = new FakeInputStream(size, true, true)) {
                workspaceClient.putObject(CONTAINER, OBJECT_ID, fis);
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testRulesSecurisator () throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0));
        VitamThreadUtils.getVitamSession().setTenantId(0);

            RulesSecurisator rulesSecurisator = new RulesSecurisator();
        final GUID eipMaster = GUIDFactory.newOperationLogbookGUID(0);


        rulesSecurisator.secureFileRules(1 , new FileInputStream(PropertiesUtils.findFile("static-offer.json")), "json",
                eipMaster);
            rulesSecurisator.secureFileRules(2 , new FileInputStream(PropertiesUtils.findFile("static-offer.json")), "json",
                eipMaster);
            VitamRequestIterator<JsonNode> result = storageClient.listContainer("default", DataCategory.RULES);
            TestCase.assertNotNull(result);
            Assert.assertTrue(result.hasNext());
            JsonNode node = result.next();
            TestCase.assertNotNull(node);

        //    assertEquals(node.get("objectId").asText()., "0_RULES-1.json");
        assertTrue(node.get("objectId").asText().startsWith("0_RULES-1"));
        assertTrue(node.get("objectId").asText().endsWith(".json"));

    }


    @RunWithCustomExecutor
    //@Test
    @Ignore // To be executed only when trying to see memory footprint
    public void testBigFile() throws InterruptedException {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0));
        VitamThreadUtils.getVitamSession().setTenantId(0);
        // 1 GB
        size = 1024000000;
        OBJECT_ID = GUIDFactory.newObjectGroupGUID(0).getId();
        final ObjectDescription description = new ObjectDescription();
        description.setWorkspaceContainerGUID(CONTAINER);
        description.setWorkspaceObjectURI(OBJECT_ID);
        Thread.sleep(50);
        try {
            populateWorkspace();
        } catch (Exception e1) {
            LOGGER.error("During populate size: " + size, e1);
            assert (false);
        }
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            try {
                storageClient.storeFileFromWorkspace("default", StorageCollectionType.OBJECTS,
                    GUIDFactory.newObjectGroupGUID(0).getId(), description);
            } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException |
                StorageServerClientException e) {
                LOGGER.error("Size: " + size, e);
                assert (false);
            }
    
            // see other test for full listing, here, we only have one object !
            try {
                VitamRequestIterator<JsonNode> result = storageClient.listContainer("default", DataCategory.OBJECT);
                TestCase.assertNotNull(result);
                Assert.assertTrue(result.hasNext());
                JsonNode node = result.next();
                TestCase.assertNotNull(node);
                Assert.assertFalse(result.hasNext());
            } catch (StorageServerClientException exc) {
                Assert.fail("Should not raize an exception");
            }
        }
        Thread.sleep(10);

        try {
            afterTest();
        } catch (Exception e) {
            // ignore
        }
        LOGGER.warn("Test Passed with size: " + size);
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            LOGGER.warn("Interruption with size: " + size);
            assert (false);
        }
    }
    
    @RunWithCustomExecutor
    @Test
    public void test() {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0));
        VitamThreadUtils.getVitamSession().setTenantId(0);
        size = 500;
        for (int i = 0; i < 10; i++) {
            OBJECT_ID = GUIDFactory.newObjectGroupGUID(0).getId();
            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(CONTAINER);
            description.setWorkspaceObjectURI(OBJECT_ID);
            try {
                populateWorkspace();
            } catch (Exception e1) {
                LOGGER.error("During populate size: " + size, e1);
                assert (false);
                break;
            }
            try {
                storageClient.storeFileFromWorkspace("default", StorageCollectionType.OBJECTS, OBJECT_ID, description);
            } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException | StorageServerClientException e) {
                LOGGER.error("Size: " + size, e);
                assert (false);
                break;
            }

            // see other test for full listing, here, we only have one object !
            try {
                VitamRequestIterator<JsonNode> result = storageClient.listContainer("default", DataCategory.OBJECT);
                TestCase.assertNotNull(result);
                Assert.assertTrue(result.hasNext());
                JsonNode node = result.next();
                TestCase.assertNotNull(node);
                Assert.assertFalse(result.hasNext());
            } catch (StorageServerClientException exc) {
                Assert.fail("Should not raize an exception");
            }

            try {
                afterTest();
            } catch (Exception e) {
                // ignore
            }
            LOGGER.warn("Test Passed with size: " + size);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                LOGGER.warn("Interruption with size: " + size);
                assert (false);
                break;
            }
            size *= 2;
        }
    }
    
   
    @RunWithCustomExecutor
    @Test
    public void testParallel() throws InterruptedException {
        size = 6 * 64000;
        ExecutorService executor = new VitamThreadPoolExecutor(NB_MULTIPLE_THREADS);
        List<Future<Boolean>> list = new ArrayList<Future<Boolean>>(NB_MULTIPLE_THREADS);
        List<FullTest> listFullTest = new ArrayList<FullTest>(NB_MULTIPLE_THREADS);
        Thread.sleep(100);
        // initialize Workspace
        try {
            workspaceClient.createContainer(CONTAINER);
        } catch (ContentAddressableStorageAlreadyExistException | ContentAddressableStorageServerException e) {
            // nothing
        }
        LOGGER.info("START creation of {} files in Workspace", NB_MULTIPLE_THREADS);
        List<GUID> workspaceGUIDs = new ArrayList<>(NB_MULTIPLE_THREADS);
        for (int i = 0; i < NB_MULTIPLE_THREADS; i++) {
            GUID requestId = GUIDFactory.newRequestIdGUID(0);
            VitamThreadUtils.getVitamSession().setRequestId(requestId);
            VitamThreadUtils.getVitamSession().setTenantId(0);
            GUID objectId = GUIDFactory.newObjectGUID(0);
            workspaceGUIDs.add(objectId);
            try {
                try (FakeInputStream fis = new FakeInputStream(size)) {
                    workspaceClient.putObject(CONTAINER, objectId.getId(), fis);
                }
            } catch (Exception e1) {
                LOGGER.error("During populate size: " + size, e1);
                assert (false);
            }
            listFullTest.add(new FullTest(requestId, objectId, objectId));
        }
        LOGGER.warn("START creation of {} parallel storage clients", NB_MULTIPLE_THREADS);
        // FIXME change to 10, 10000 for Java Mission Control benchmark
        Thread.sleep(10);
        long start1 = System.nanoTime();
        // Launch Storage tests in parallel
        for (FullTest fullTest : listFullTest) {
            list.add(executor.submit(fullTest));
        }
        Thread.sleep(100);
        executor.shutdown();
        LOGGER.info("WAITING for end of {} parallel storage clients", NB_MULTIPLE_THREADS);
        if (!executor.awaitTermination(600000, TimeUnit.MILLISECONDS)) {
            LOGGER.warn("Timeout: {}", 600000);
            assertTrue("TIMEOUT", false);
        }
        // Waiting for each request to end
        int ok1 = 0;
        int ko1 = 0;
        for (Future<Boolean> future : list) {
            try {
                if (future.get() != true) {
                    ko1++;
                } else {
                    ok1++;
                }
            } catch (ExecutionException | InterruptedException | CancellationException e) {
                LOGGER.error(e);
                ko1++;
            }
        }
        long stop1 = System.nanoTime();
        LOGGER.info("END of {} parallel storage clients", NB_MULTIPLE_THREADS);
        LOGGER.warn("Step 1 OK: {} KO: {} in {} s", ok1, ko1, ((stop1 - start1) / 1000000000));
        executor.shutdownNow();
        assertTrue("BAD WRITE " + ok1 + " : " + ko1, ko1 == 0);

        // Retry with other GUID
        executor = new VitamThreadPoolExecutor(NB_MULTIPLE_THREADS);
        list.clear();
        listFullTest.clear();
        for (GUID objectId : workspaceGUIDs) {
            GUID requestId = GUIDFactory.newRequestIdGUID(0);
            VitamThreadUtils.getVitamSession().setRequestId(requestId);
            VitamThreadUtils.getVitamSession().setTenantId(0);
            listFullTest.add(new FullTest(requestId, objectId, GUIDFactory.newObjectGUID(0)));
        }
        // FIXME change to 10, 10000 for Java Mission Control benchmark
        Thread.sleep(10);
        LOGGER.warn("START creation of {} parallel storage clients", NB_MULTIPLE_THREADS);
        long start2 = System.nanoTime();
        // Launch Storage tests in parallel
        for (FullTest fullTest : listFullTest) {
            list.add(executor.submit(fullTest));
        }
        Thread.sleep(100);
        executor.shutdown();
        LOGGER.info("WAITING for end of {} parallel storage clients", NB_MULTIPLE_THREADS);
        if (!executor.awaitTermination(600000, TimeUnit.MILLISECONDS)) {
            LOGGER.warn("Timeout: {}", 600000);
            assertTrue("TIMEOUT", false);
        }
        // Waiting for each request to end
        int ok = 0;
        int ko = 0;
        for (Future<Boolean> future : list) {
            try {
                if (future.get() != true) {
                    ko++;
                } else {
                    ok++;
                }
            } catch (ExecutionException | InterruptedException | CancellationException e) {
                LOGGER.error(e);
                ko++;
            }
        }
        long stop2 = System.nanoTime();
        LOGGER.info("END of {} parallel storage clients", NB_MULTIPLE_THREADS);
        LOGGER.warn("Step 1 OK: {} KO: {} in {} s", ok1, ko1, ((stop1 - start1) / 1000000000));
        LOGGER.warn("Step 2 OK: {} KO: {} in {} s", ok, ko, ((stop2 - start2) / 1000000000));
        // FIXME change to 10, 100000 for Java Mission Control benchmark
        Thread.sleep(10);
        assertTrue("BAD WRITE " + ok + " : " + ko, ko == 0);
        // Cleaning
        try {
            afterTest();
        } catch (Exception e) {
            // ignore
        }

    }

    private static class FullTest implements Callable<Boolean> {
        private final GUID requestId;
        private final GUID objectId;
        private final GUID storageId;

        public FullTest(GUID requestId, GUID objectId, GUID storageId) {
            this.requestId = requestId;
            this.objectId = objectId;
            this.storageId = storageId;
        }

        public Boolean call() {
            VitamThreadUtils.getVitamSession().setRequestId(requestId);
            VitamThreadUtils.getVitamSession().setTenantId(0);
            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(CONTAINER);
            description.setWorkspaceObjectURI(objectId.getId());
            try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                storageClient.storeFileFromWorkspace("default", StorageCollectionType.OBJECTS, storageId.getId(),
                    description);
            } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException |
                StorageServerClientException e) {
                LOGGER.error("Size: " + size, e);
                return false;
            }
            Response response = null;
            try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                response = storageClient.getContainerAsync("default", storageId.getId(), StorageCollectionType.OBJECTS);
                final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
                if (status == Status.OK && response.hasEntity()) {
                    return true;
                } else {
                    LOGGER.error("Error: " + status.getReasonPhrase());
                    return false;
                }
            } catch (StorageServerClientException | StorageNotFoundException e) {
                LOGGER.error("Size: " + size, e);
                return false;
            } finally {
                storageClient.consumeAnyEntityAndClose(response);
            }
        }
    }

    @RunWithCustomExecutor
    @Test
    public void listingTest() {
        size = 500;
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(0));
        VitamThreadUtils.getVitamSession().setTenantId(0);
        for (int i = 0; i < 150; i++) {
            try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
                OBJECT_ID = GUIDFactory.newObjectGroupGUID(0).getId();
                final ObjectDescription description = new ObjectDescription();
                description.setWorkspaceContainerGUID(CONTAINER);
                description.setWorkspaceObjectURI(OBJECT_ID);
                try {
                    populateWorkspace();
                } catch (Exception e1) {
                    LOGGER.error("During populate size: " + size, e1);
                    assert (false);
                    break;
                }
                try {
                    storageClient.storeFileFromWorkspace("default", StorageCollectionType.OBJECTS, OBJECT_ID, description);
                } catch (StorageAlreadyExistsClientException | StorageNotFoundClientException |
                    StorageServerClientException e) {
                    LOGGER.error("Size: " + size, e);
                    assert (false);
                    break;
                }
            }
        }

        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient();
            VitamRequestIterator<JsonNode> result = storageClient.listContainer("default", DataCategory.OBJECT)) {
            TestCase.assertNotNull(result);
            int count = 0;
            while (result.hasNext()) {
                count++;
                TestCase.assertNotNull(result.next());
            }
            TestCase.assertEquals(150, count);
        } catch (StorageServerClientException exc) {
            Assert.fail("Should not raize an exception");
        }
    }

}
