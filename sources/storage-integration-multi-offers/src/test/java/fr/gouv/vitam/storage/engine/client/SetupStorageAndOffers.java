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

import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorageAbstract;
import fr.gouv.vitam.common.storage.constants.StorageProvider;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.storage.offers.rest.OfferConfiguration;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fr.gouv.vitam.common.PropertiesUtils.readYaml;
import static fr.gouv.vitam.common.PropertiesUtils.writeYaml;

/**
 * Helper class for storage engine IT test initialization with 2 offers
 */
class SetupStorageAndOffers {

    private static final String JETTY_STORAGE_ADMIN = "jetty.storage.admin";
    public static final int PORT_SERVICE_WORKSPACE = 8987;
    public static final int PORT_SERVICE_STORAGE = 8583;
    public static final int PORT_ADMIN_STORAGE = 8584;
    private static final String WORKSPACE_URL = "http://localhost:" + PORT_SERVICE_WORKSPACE;

    private WorkspaceMain workspaceMain;
    private DefaultOfferMain firstOfferApplication;
    private DefaultOfferMain secondOfferApplication;
    private StorageMain storageMain;
    private String offerStorageFolder;
    private String secondStorageFolder;
    private String storageLoginDirectory;

    public void setupStorageAndTwoOffer(
        TempFolderRule tempFolder,
        String storageConfFile,
        String workspaceConfFile,
        String storageConf,
        String defaultOfferConf,
        String defaultSecondConf
    ) throws IOException, VitamApplicationServerException {
        tempFolder.create();
        File vitamTempFolder = tempFolder.newFolder();
        SystemPropertyUtil.set("vitam.tmp.folder", vitamTempFolder.getAbsolutePath());

        offerStorageFolder = tempFolder.newFolder().getAbsolutePath();
        secondStorageFolder = tempFolder.newFolder().getAbsolutePath();

        // launch workspace
        SystemPropertyUtil.set(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(PORT_SERVICE_WORKSPACE));

        final File workspaceConfigFile = PropertiesUtils.findFile(workspaceConfFile);

        fr.gouv.vitam.common.storage.StorageConfiguration workspaceConfiguration = PropertiesUtils.readYaml(
            workspaceConfigFile,
            fr.gouv.vitam.common.storage.StorageConfiguration.class
        );
        workspaceConfiguration.setStoragePath(vitamTempFolder.getAbsolutePath());

        writeYaml(workspaceConfigFile, workspaceConfiguration);

        workspaceMain = new WorkspaceMain(workspaceConfigFile.getAbsolutePath());
        workspaceMain.start();
        SystemPropertyUtil.clear(WorkspaceMain.PARAMETER_JETTY_SERVER_PORT);
        WorkspaceClientFactory.changeMode(WORKSPACE_URL);

        // First  offer
        // Sorry Hack
        //Force offer 1 to have her own folder
        File file = PropertiesUtils.findFile(storageConfFile);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            IOUtils.write("storagePath: " + offerStorageFolder + "\n", outputStream, CharsetUtils.UTF_8);
            IOUtils.write("provider: " + StorageProvider.FILESYSTEM.getValue(), outputStream, CharsetUtils.UTF_8);
        }

        SystemPropertyUtil.set(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT, 8757);
        final File offerConfig = PropertiesUtils.findFile(defaultOfferConf);
        final OfferConfiguration offerConfiguration = PropertiesUtils.readYaml(offerConfig, OfferConfiguration.class);
        List<MongoDbNode> mongoDbNodes = offerConfiguration.getMongoDbNodes();
        mongoDbNodes.get(0).setDbPort(MongoRule.getDataBasePort());
        offerConfiguration.setMongoDbNodes(mongoDbNodes);
        offerConfiguration.setStoragePath(offerStorageFolder);

        PropertiesUtils.writeYaml(offerConfig, offerConfiguration);

        firstOfferApplication = new DefaultOfferMain(offerConfig.getAbsolutePath());
        firstOfferApplication.start();
        SystemPropertyUtil.clear(DefaultOfferMain.PARAMETER_JETTY_SERVER_PORT);
        ContentAddressableStorageAbstract.disableContainerCaching();

        // Second offer
        // Sorry Hack
        //Force offer 2 to have her own folder
        file = PropertiesUtils.findFile(storageConfFile);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            IOUtils.write("storagePath: " + secondStorageFolder + "\n", outputStream, CharsetUtils.UTF_8);
            IOUtils.write("provider: " + StorageProvider.FILESYSTEM.getValue(), outputStream, CharsetUtils.UTF_8);
        }

        //
        SystemPropertyUtil.set("jetty.offer2.port", 8758);
        final File secondOfferConfig = PropertiesUtils.findFile(defaultSecondConf);
        final OfferConfiguration secondOfferConfiguration = PropertiesUtils.readYaml(
            secondOfferConfig,
            OfferConfiguration.class
        );

        List<MongoDbNode> mongoDbNodesSecond = secondOfferConfiguration.getMongoDbNodes();
        mongoDbNodesSecond.get(0).setDbPort(MongoRule.getDataBasePort());
        secondOfferConfiguration.setMongoDbNodes(mongoDbNodesSecond);

        secondOfferConfiguration.setStoragePath(secondStorageFolder);
        PropertiesUtils.writeYaml(secondOfferConfig, secondOfferConfiguration);

        secondOfferApplication = new DefaultOfferMain(secondOfferConfig.getAbsolutePath());
        secondOfferApplication.start();
        SystemPropertyUtil.clear("jetty.offer2.port");

        // launch engine
        File storageConfigurationFile = PropertiesUtils.findFile(storageConf);

        final StorageConfiguration serverConfiguration = readYaml(storageConfigurationFile, StorageConfiguration.class);

        final Pattern compiledPattern = Pattern.compile(":(\\d+)");
        final Matcher matcher = compiledPattern.matcher(serverConfiguration.getUrlWorkspace());
        if (matcher.find()) {
            final String[] seg = serverConfiguration.getUrlWorkspace().split(":(\\d+)");
            serverConfiguration.setUrlWorkspace(seg[0]);
        }
        serverConfiguration.setUrlWorkspace(serverConfiguration.getUrlWorkspace() + ":" + PORT_SERVICE_WORKSPACE);

        serverConfiguration.setZippingDirecorty(tempFolder.newFolder().getAbsolutePath());
        storageLoginDirectory = tempFolder.newFolder().getAbsolutePath();
        serverConfiguration.setLoggingDirectory(storageLoginDirectory);

        writeYaml(storageConfigurationFile, serverConfiguration);

        SystemPropertyUtil.set(StorageMain.PARAMETER_JETTY_SERVER_PORT, PORT_SERVICE_STORAGE);
        SystemPropertyUtil.set(JETTY_STORAGE_ADMIN, PORT_ADMIN_STORAGE);
        storageMain = new StorageMain(storageConf);
        storageMain.start();
        SystemPropertyUtil.clear(StorageMain.PARAMETER_JETTY_SERVER_PORT);
        SystemPropertyUtil.clear(JETTY_STORAGE_ADMIN);

        StorageClientFactory.getInstance().setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        StorageClientFactory.changeMode("http://localhost:" + PORT_SERVICE_STORAGE);
    }

    public void close() throws VitamApplicationServerException {
        if (workspaceMain != null) {
            workspaceMain.stop();
        }
        if (firstOfferApplication != null) {
            firstOfferApplication.stop();
        }
        if (secondOfferApplication != null) {
            secondOfferApplication.stop();
        }
        if (storageMain != null) {
            storageMain.stop();
        }
    }

    public void cleanOffers() throws IOException {
        FileUtils.cleanDirectory(new File(offerStorageFolder));
        FileUtils.cleanDirectory(new File(secondStorageFolder));
    }

    public String getSecondStorageFolder() {
        return secondStorageFolder;
    }

    public String getStorageLogDirectory() {
        return storageLoginDirectory;
    }
}
