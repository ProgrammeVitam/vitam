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
package fr.gouv.vitam.processing.management.rest;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.server.VitamServerLifeCycle;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.distributor.api.IWorkerManager;
import fr.gouv.vitam.processing.distributor.api.ProcessDistributor;
import fr.gouv.vitam.processing.distributor.core.AsyncResourceCleaner;
import fr.gouv.vitam.processing.distributor.core.AsyncResourcesMonitor;
import fr.gouv.vitam.processing.distributor.core.ProcessDistributorImpl;
import fr.gouv.vitam.processing.distributor.core.WorkerManager;
import fr.gouv.vitam.processing.distributor.rest.ProcessDistributorResource;
import fr.gouv.vitam.processing.management.api.ProcessManagement;
import fr.gouv.vitam.worker.client.WorkerClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * As restEasy application is lazy load, and some classes are needed for both Jetty server and resteasy configuration
 * This Factory helps to make bridge between jetty server and resteasy configuration.
 */
public class VitamApplicationInitializr {

    private static final VitamApplicationInitializr INSTANCE = new VitamApplicationInitializr();
    private Set<Object> singletons = new HashSet<>();
    private VitamServerLifeCycle vitamServerLifeCycle;
    private CommonBusinessApplication commonBusinessApplication;

    /**
     * Get an instance of VitamApplicationInitializr
     *
     * @return VitamApplicationInitializr
     */
    public static VitamApplicationInitializr get() {
        return INSTANCE;
    }

    /**
     * Initialize all resource using the given configuration file
     *
     * @param configurationFile
     */
    public void initialize(String configurationFile) {
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            final ServerConfiguration configuration = PropertiesUtils.readYaml(yamlIS, ServerConfiguration.class);
            commonBusinessApplication = new CommonBusinessApplication();

            WorkspaceClientFactory.changeMode(configuration.getUrlWorkspace());

            IWorkerManager workerManager = new WorkerManager();
            workerManager.initialize();

            AsyncResourcesMonitor asyncResourcesMonitor = new AsyncResourcesMonitor(configuration);
            AsyncResourceCleaner asyncResourceCleaner = new AsyncResourceCleaner(configuration);

            ProcessDistributor processDistributor = new ProcessDistributorImpl(
                workerManager,
                asyncResourcesMonitor,
                asyncResourceCleaner,
                configuration
            );

            ProcessManagementResource processManagementResource = new ProcessManagementResource(
                configuration,
                processDistributor
            );
            ProcessDistributorResource processDistributorResource = new ProcessDistributorResource(workerManager);
            vitamServerLifeCycle = new VitamServerLifeCycle(processManagementResource.getProcessLifeCycle());
            singletons = new HashSet<>();
            singletons.addAll(commonBusinessApplication.getResources());
            singletons.add(processManagementResource);
            singletons.add(processDistributorResource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    public void initialize(
        ServerConfiguration serverConfiguration,
        WorkerClientFactory workerClientFactory,
        ProcessManagement processManagement
    ) throws IOException {
        commonBusinessApplication = new CommonBusinessApplication();

        IWorkerManager workerManager = new WorkerManager(workerClientFactory);
        workerManager.initialize();

        ProcessManagementResource processManagementResource = new ProcessManagementResource(
            processManagement,
            serverConfiguration
        );
        ProcessDistributorResource processDistributorResource = new ProcessDistributorResource(workerManager);
        vitamServerLifeCycle = new VitamServerLifeCycle(processManagementResource.getProcessLifeCycle());

        singletons = new HashSet<>();
        singletons.addAll(commonBusinessApplication.getResources());
        singletons.add(processManagementResource);
        singletons.add(processDistributorResource);
    }

    /**
     * Return the set of registered resources
     *
     * @return singletons
     */
    public Set<Object> getSingletons() {
        return singletons;
    }

    /**
     * Return the vitamServerLifeCycle to be added to jetty server
     *
     * @return vitamServerLifeCycle
     */
    public VitamServerLifeCycle getVitamServerLifeCycle() {
        return vitamServerLifeCycle;
    }

    /**
     * Used by @see BusinessApplication to get a registred classes
     *
     * @return commonBusinessApplication
     */
    public CommonBusinessApplication getCommonBusinessApplication() {
        return commonBusinessApplication;
    }
}
