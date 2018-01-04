/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.engine.client;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.CollectionBackupModel;
import fr.gouv.vitam.functional.administration.common.VitamRepositoryFactory;
import fr.gouv.vitam.functional.administration.common.VitamRepositoryProvider;
import fr.gouv.vitam.functional.administration.common.api.RestoreBackupService;
import fr.gouv.vitam.functional.administration.common.impl.ReconstructionServiceImpl;
import fr.gouv.vitam.functional.administration.common.impl.RestoreBackupServiceImpl;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferMain;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.apache.commons.io.FileUtils;
import org.jhades.JHades;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.gouv.vitam.common.PropertiesUtils.readYaml;
import static fr.gouv.vitam.common.PropertiesUtils.writeYaml;

/**
 * Integration tests for the reconstruction services. <br/>
 */
public class ReconstructionIT {

    /**
     * Vitam logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReconstructionIT.class);

    private static DefaultOfferMain defaultOfferApplication;
    private static final String DEFAULT_OFFER_CONF = "storage-test/storage-default-offer-ssl.conf";
    private static final String OFFER_FOLDER = "offer";
    private static StorageMain storageMain;
    private static StorageClient storageClient;
    private static final String STORAGE_CONF = "storage-test/storage-engine.conf";
    private static final String BACKUP_COPY_FOLDER = "backup";
    private static final int TENANT_ID = 0;
    private static final String STRATEGY_ID = "default";
    private static String CONTAINER = "0_rules";
    private static String containerName = "";
    private static WorkspaceMain workspaceMain;
    private static WorkspaceClient workspaceClient;
    private static int workspacePort = 8987;
    private static final String WORKSPACE_CONF = "storage-test/workspace.conf";
    private static final String ADMIN_MANAGEMENT_CONF = "functional-administration-test.conf";
    static TemporaryFolder folder = new TemporaryFolder();

    private static RestoreBackupService recoverBuckupService;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();


    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @BeforeClass
    public static void setupBeforeClass() throws Exception {

        containerName = String.format("%s_%s", TENANT_ID, StorageCollectionType.BACKUP.toString().toLowerCase());
        recoverBuckupService = new RestoreBackupServiceImpl();

        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        // prepare workspace
        workspaceMain = new WorkspaceMain(WORKSPACE_CONF);
        workspaceMain.start();

        WorkspaceClientFactory.changeMode("http://localhost:" + workspacePort);
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();

        // prepare offer
        final fr.gouv.vitam.common.storage.StorageConfiguration offerConfiguration =
            readYaml(PropertiesUtils.findFile(DEFAULT_OFFER_CONF),
                fr.gouv.vitam.common.storage.StorageConfiguration.class);
        defaultOfferApplication = new DefaultOfferMain(DEFAULT_OFFER_CONF);
        defaultOfferApplication.start();

        // storage engine
        File storageConfigurationFile = PropertiesUtils.findFile(STORAGE_CONF);
        final StorageConfiguration serverConfiguration = readYaml(storageConfigurationFile, StorageConfiguration.class);
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

        writeYaml(storageConfigurationFile, serverConfiguration);

        // prepare storage
        storageMain = new StorageMain(STORAGE_CONF);
        storageMain.start();

        StorageClientFactory.getInstance().setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        storageClient = StorageClientFactory.getInstance().getClient();
    }

    @AfterClass
    public static void afterClass() throws Exception {

        // Ugly style but necessary because this is the folder representing the workspace
        File workspaceFolder = new File(CONTAINER);
        if (workspaceFolder.exists()) {
            try {
                // if clean workspace delete did not work
                FileUtils.cleanDirectory(workspaceFolder);
                FileUtils.deleteDirectory(workspaceFolder);
            } catch (Exception e) {
                LOGGER.error("ERROR: Exception has been thrown when cleanning workspace:", e);
            }
        }
        workspaceClient.close();
        workspaceMain.stop();
        File offerFolder = new File(OFFER_FOLDER);
        if (offerFolder.exists()) {
            try {
                // if clean offer delete did not work
                FileUtils.cleanDirectory(offerFolder);
                FileUtils.deleteDirectory(offerFolder);
            } catch (Exception e) {
                LOGGER.error("ERROR: Exception has been thrown when cleanning offer:", e);
            }
        }
        storageClient.close();
        defaultOfferApplication.stop();
        storageMain.stop();
        folder.delete();
    }

    @Before
    public void setup() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        // create a container on the Workspace
        workspaceClient.createContainer(containerName);
    }

    @After
    public void tearDown() throws Exception {
        // delete the container on the Workspace
        workspaceClient.deleteContainer(containerName, true);
        // clean offers
        cleanOffers();
    }

    @Test
    @RunWithCustomExecutor
    public void testReconstructionOk() throws Exception {

        // create and save some backup files for reconstruction.
        prepareBackupStorage();

        final File adminConfig = PropertiesUtils.findFile(ADMIN_MANAGEMENT_CONF);
        final AdminManagementConfiguration configuration =
            PropertiesUtils.readYaml(adminConfig, AdminManagementConfiguration.class);

        final VitamRepositoryProvider vitamRepository = VitamRepositoryFactory.getInstance();

        ReconstructionServiceImpl reconstructionService =
            new ReconstructionServiceImpl(configuration, vitamRepository,
                new RestoreBackupServiceImpl());

        reconstructionService.reconstruct(FunctionalAdminCollections.RULES, TENANT_ID);


    }

    /**
     * Creation and storage of backup files copies.
     *
     * @throws Exception
     */
    private void prepareBackupStorage() throws Exception {

        // create and store different backup copies from the backup folder.
        File backupFolder = PropertiesUtils.findFile(BACKUP_COPY_FOLDER);
        LOGGER.debug(String.format("Start storage backup copies -> Folder : %s", backupFolder));
        if (backupFolder.exists()) {
            LOGGER.debug(String.format("Storage of %s backup copies.", backupFolder.list().length));
            Arrays.stream(backupFolder.listFiles()).forEach(f -> storeBackupCopies(f));
        }
    }

    /**
     * Save backup copies of files.
     *
     * @param file
     */
    private void storeBackupCopies(final File file) {
        final String extension = "json";
        try (InputStream in = new BufferedInputStream(new FileInputStream(file));) {
            final String uri = file.getName();

            // add the object on the container on the Workspace
            workspaceClient.putObject(containerName, uri, in);

            // store on the storage
            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(containerName);
            description.setWorkspaceObjectURI(uri);
            storageClient.storeFileFromWorkspace(STRATEGY_ID, StorageCollectionType.BACKUP, uri, description);

        } catch (Exception e) {
            LOGGER.error("ERROR: Exception has been thrown when storing the backup copy.", e);
        }
    }

    /**
     * Clean offers content.
     */
    private static void cleanOffers() {
        // ugly style but we don't have the digest here
        File directory = new File(OFFER_FOLDER + "/" + containerName);
        try {
            FileUtils.cleanDirectory(directory);
            FileUtils.deleteDirectory(directory);
        } catch (IOException | IllegalArgumentException e) {
            LOGGER.error("ERROR: Exception has been thrown when cleaning offers.", e);
        }
    }

}
