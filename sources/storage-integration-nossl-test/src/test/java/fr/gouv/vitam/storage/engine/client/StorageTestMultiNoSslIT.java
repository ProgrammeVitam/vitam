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
package fr.gouv.vitam.storage.engine.client;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.storage.offers.rest.OfferConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileOutputStream;
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StorageTestMultiNoSslIT {

    private static final int NB_MULTIPLE_THREADS = 100;

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageTestMultiNoSslIT.class);

    private static final String WORKSPACE_CONF = "storage-test/workspace.conf";
    private static final String DEFAULT_OFFER_CONF = "storage-test/storage-default-offer-nossl.conf";
    private static final String STORAGE_CONF = "storage-test/storage-engine.conf";

    // ports are overriden
    private static int workspacePort = 8987;
    private static int defaultOfferPort = 8575;
    private static int storageEnginePort = 8583;

    private static WorkspaceMain workspaceMain;
    private static WorkspaceClient workspaceClient;
    private static DefaultOfferMain defaultOfferApplication;
    private static StorageMain storageMain;
    private static StorageClient storageClient;

    private static final String OFFER_FOLDER = "offer";
    private static final String WORKSPACE_FOLDER = "workspace";
    private static final String CONTAINER = "object";
    private static String OBJECT_ID;
    private static int size = 500;
    private static TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder());

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        OfferCollections.OFFER_LOG.setPrefix(GUIDFactory.newGUID().getId());
        OfferCollections.OFFER_SEQUENCE.setPrefix(GUIDFactory.newGUID().getId());
        mongoRule.addCollectionToBePurged(OfferCollections.OFFER_LOG.getName());
        mongoRule.addCollectionToBePurged(OfferCollections.OFFER_SEQUENCE.getName());

        // workspace
        workspacePort = JunitHelper.getInstance().findAvailablePort();
        SystemPropertyUtil.set(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT, workspacePort);
        workspaceMain = new WorkspaceMain(WORKSPACE_CONF);
        workspaceMain.start();
        SystemPropertyUtil.clear(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT);

        WorkspaceClientFactory.changeMode("http://localhost:" + workspacePort);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();

        // offer
        defaultOfferPort = JunitHelper.getInstance().findAvailablePort();
        SystemPropertyUtil.set(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT, defaultOfferPort);
        final File offerConfig = PropertiesUtils.findFile(DEFAULT_OFFER_CONF);
        final OfferConfiguration offerConfiguration = PropertiesUtils.readYaml(offerConfig, OfferConfiguration.class);
        List<MongoDbNode> mongoDbNodes = offerConfiguration.getMongoDbNodes();
        mongoDbNodes.get(0).setDbPort(MongoRule.getDataBasePort());
        offerConfiguration.setMongoDbNodes(mongoDbNodes);
        PropertiesUtils.writeYaml(offerConfig, offerConfiguration);
        defaultOfferApplication = new DefaultOfferMain(DEFAULT_OFFER_CONF);
        defaultOfferApplication.start();
        SystemPropertyUtil.clear(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT);
        ContentAddressableStorageAbstract.disableContainerCaching();

        // storage engine
        File storageConfigurationFile = PropertiesUtils.findFile(STORAGE_CONF);
        final StorageConfiguration serverConfiguration = PropertiesUtils.readYaml(
            storageConfigurationFile,
            StorageConfiguration.class
        );
        final Pattern compiledPattern = Pattern.compile(":(\\d+)");
        final Matcher matcher = compiledPattern.matcher(serverConfiguration.getUrlWorkspace());
        if (matcher.find()) {
            final String seg[] = serverConfiguration.getUrlWorkspace().split(":(\\d+)");
            serverConfiguration.setUrlWorkspace(seg[0]);
        }
        serverConfiguration.setUrlWorkspace(
            serverConfiguration.getUrlWorkspace() + ":" + Integer.toString(workspacePort)
        );

        folder.create();
        serverConfiguration.setZippingDirecorty(folder.newFolder().getAbsolutePath());
        serverConfiguration.setLoggingDirectory(folder.newFolder().getAbsolutePath());

        PropertiesUtils.writeYaml(storageConfigurationFile, serverConfiguration);

        File staticOffersFile = PropertiesUtils.findFile("static-offer.json");
        ArrayNode staticOffers = (ArrayNode) JsonHandler.getFromFile(staticOffersFile);
        ObjectNode defaultOffer = (ObjectNode) staticOffers.get(0);
        defaultOffer.put("baseUrl", "http://localhost:" + defaultOfferPort);
        try (FileOutputStream outputStream = new FileOutputStream(staticOffersFile)) {
            IOUtils.write(JsonHandler.prettyPrint(staticOffers), outputStream, CharsetUtils.UTF_8);
        }

        // prepare storage
        storageEnginePort = JunitHelper.getInstance().findAvailablePort();
        SystemPropertyUtil.set(StorageMain.PARAMETER_JETTY_SERVER_PORT, storageEnginePort);
        storageMain = new StorageMain(STORAGE_CONF);
        storageMain.start();
        SystemPropertyUtil.clear(StorageMain.PARAMETER_JETTY_SERVER_PORT);

        StorageClientFactory.getInstance().setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        StorageClientFactory.changeMode("http://localhost:" + storageEnginePort);
        storageClient = StorageClientFactory.getInstance().getClient();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        mongoRule.handleAfter();
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
        workspaceMain.stop();
        JunitHelper.getInstance().releasePort(workspacePort);
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
        JunitHelper.getInstance().releasePort(defaultOfferPort);
        storageMain.stop();
        JunitHelper.getInstance().releasePort(storageEnginePort);
        folder.delete();
        VitamClientFactory.resetConnections();
    }

    private static void afterTest() {
        cleanWorkspace();
        mongoRule.handleAfter();
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
        try {
            workspaceClient.createContainer(CONTAINER);
        } catch (ContentAddressableStorageServerException e) {
            // nothing
        }

        try (FakeInputStream fis = new FakeInputStream(size, true, true)) {
            workspaceClient.putObject(CONTAINER, OBJECT_ID, fis);
        }
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
        try {
            storageClient.storeFileFromWorkspace(
                VitamConfiguration.getDefaultStrategy(),
                DataCategory.OBJECT,
                GUIDFactory.newObjectGroupGUID(0).getId(),
                description
            );
        } catch (
            StorageAlreadyExistsClientException | StorageNotFoundClientException | StorageServerClientException e
        ) {
            LOGGER.error("Size: " + size, e);
            assert (false);
        }

        // see other test for full listing, here, we only have one object !
        try {
            CloseableIterator<ObjectEntry> result = storageClient.listContainer(
                VitamConfiguration.getDefaultStrategy(),
                null,
                DataCategory.OBJECT
            );
            TestCase.assertNotNull(result);
            Assert.assertTrue(result.hasNext());
            ObjectEntry node = result.next();
            TestCase.assertNotNull(node);
            Assert.assertFalse(result.hasNext());
        } catch (StorageClientException exc) {
            Assert.fail("Should not raize an exception");
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
                storageClient.storeFileFromWorkspace(
                    VitamConfiguration.getDefaultStrategy(),
                    DataCategory.OBJECT,
                    OBJECT_ID,
                    description
                );
            } catch (
                StorageAlreadyExistsClientException | StorageNotFoundClientException | StorageServerClientException e
            ) {
                LOGGER.error("Size: " + size, e);
                assert (false);
                break;
            }

            // see other test for full listing, here, we only have one object !
            try {
                CloseableIterator<ObjectEntry> result = storageClient.listContainer(
                    VitamConfiguration.getDefaultStrategy(),
                    null,
                    DataCategory.OBJECT
                );
                TestCase.assertNotNull(result);
                Assert.assertTrue(result.hasNext());
                ObjectEntry node = result.next();
                TestCase.assertNotNull(node);
                Assert.assertFalse(result.hasNext());
            } catch (StorageClientException exc) {
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
        } catch (ContentAddressableStorageServerException e) {
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
                try {
                    storageClient.storeFileFromWorkspace(
                        VitamConfiguration.getDefaultStrategy(),
                        DataCategory.OBJECT,
                        storageId.getId(),
                        description
                    );
                } catch (
                    StorageAlreadyExistsClientException
                    | StorageNotFoundClientException
                    | StorageServerClientException e
                ) {
                    LOGGER.error("Size: " + size, e);
                    return false;
                }
                Response response = null;
                try {
                    response = storageClient.getContainerAsync(
                        VitamConfiguration.getDefaultStrategy(),
                        storageId.getId(),
                        DataCategory.OBJECT,
                        AccessLogUtils.getNoLogAccessLog()
                    );
                    final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
                    if (status == Status.OK && response.hasEntity()) {
                        return true;
                    } else {
                        LOGGER.error("Error: " + status.getReasonPhrase());
                        return false;
                    }
                } catch (
                    StorageServerClientException
                    | StorageNotFoundException
                    | StorageUnavailableDataFromAsyncOfferClientException e
                ) {
                    LOGGER.error("Size: " + size, e);
                    return false;
                } finally {
                    storageClient.consumeAnyEntityAndClose(response);
                }
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
                storageClient.storeFileFromWorkspace(
                    VitamConfiguration.getDefaultStrategy(),
                    DataCategory.OBJECT,
                    OBJECT_ID,
                    description
                );
            } catch (
                StorageAlreadyExistsClientException | StorageNotFoundClientException | StorageServerClientException e
            ) {
                LOGGER.error("Size: " + size, e);
                assert (false);
                break;
            }
        }

        try (
            CloseableIterator<ObjectEntry> result = storageClient.listContainer(
                VitamConfiguration.getDefaultStrategy(),
                null,
                DataCategory.OBJECT
            )
        ) {
            TestCase.assertNotNull(result);
            int count = 0;
            while (result.hasNext()) {
                count++;
                TestCase.assertNotNull(result.next());
            }
            TestCase.assertEquals(150, count);
        } catch (StorageClientException exc) {
            Assert.fail("Should not raize an exception");
        }
    }

    @RunWithCustomExecutor
    public void listingTestErrorWhenContainerNotFound() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(99);
        try (
            CloseableIterator<ObjectEntry> objectEntryCloseableIterator = storageClient.listContainer(
                VitamConfiguration.getDefaultStrategy(),
                null,
                DataCategory.OBJECT
            )
        ) {
            assertFalse(objectEntryCloseableIterator.hasNext());
        }
    }
}
