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
package fr.gouv.vitam.processing.distributor.core;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.WorkerFamilyNotFoundException;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.distributor.api.IWorkerManager;
import fr.gouv.vitam.worker.client.WorkerClient;
import fr.gouv.vitam.worker.client.WorkerClientConfiguration;
import fr.gouv.vitam.worker.client.WorkerClientFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * WorkerManager class contains methods to manage workers
 */
public class WorkerManager implements IWorkerManager {

    /**
     * Default queue size
     * FIXME : why 15 ? Why not configurable
     */
    public static final int QUEUE_SIZE = 15;
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerManager.class);
    private final ConcurrentMap<String, WorkerFamilyManager> workersFamily;

    private WorkerClientFactory workerClientFactory = null;

    /**
     * Constructor
     */
    public WorkerManager() {
        workersFamily = new ConcurrentHashMap<>();
    }

    public WorkerManager(WorkerClientFactory workerClientFactory) {
        workersFamily = new ConcurrentHashMap<>();
        this.workerClientFactory = workerClientFactory;
    }

    @Override
    public synchronized void marshallToDB() throws IOException {
        if (!getWorkerDbFile().exists()) {
            if (Files.notExists(Paths.get(VitamConfiguration.getVitamDataFolder()))) {
                Files.createDirectories(Paths.get(VitamConfiguration.getVitamDataFolder()));
            }
            Files.createFile(getWorkerDbFile().toPath());
        }

        List<WorkerBean> registeredWorkers = new ArrayList<>();

        for (Map.Entry<String, WorkerFamilyManager> family : workersFamily.entrySet()) {
            for (Map.Entry<String, WorkerExecutor> worker : family.getValue().getWorkers().entrySet()) {
                WorkerBean workerBean = worker.getValue().getWorkerBean();
                registeredWorkers.add(workerBean);
            }
        }

        try {
            JsonHandler.writeAsFile(registeredWorkers, getWorkerDbFile());
        } catch (InvalidParseOperationException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void registerWorker(String familyId, String workerId, WorkerBean workerInformation)
        throws ProcessingBadRequestException, IOException {
        ParametersChecker.checkParameter("All arguments are required", familyId, workerId, workerInformation);

        if (!workerInformation.getFamily().equals(familyId)) {
            throw new ProcessingBadRequestException("Cannot register a worker of another family!");
        } else {
            workerInformation.setWorkerId(workerId);
        }

        registerWorker(workerInformation);
    }

    @Override
    public void registerWorker(WorkerBean workerBean) throws IOException {
        workersFamily.compute(workerBean.getFamily(), (key, workerManager) -> {
            if (workerManager == null) {
                workerManager = new WorkerFamilyManager(workerBean.getFamily(), QUEUE_SIZE);
            }
            workerManager.registerWorker(workerBean);
            return workerManager;
        });
        marshallToDB();
    }

    @Override
    public void unregisterWorker(String workerFamily, String worker) throws WorkerFamilyNotFoundException, IOException {
        final WorkerFamilyManager workerManager = workersFamily.get(workerFamily);
        if (workerManager == null) {
            throw new WorkerFamilyNotFoundException("Worker : " + worker + " not found in the family :" + workerFamily);
        }
        workerManager.unregisterWorker(worker);

        marshallToDB();
    }

    @Override
    public boolean checkStatusWorker(String serverHost, int serverPort) {
        if (null == workerClientFactory) {
            WorkerClientConfiguration workerClientConfiguration = new WorkerClientConfiguration(serverHost, serverPort);
            WorkerClientFactory.changeMode(workerClientConfiguration);
            return checkStatus(serverHost, serverPort, WorkerClientFactory.getInstance(workerClientConfiguration));
        } else {
            return checkStatus(serverHost, serverPort, workerClientFactory);
        }
    }

    private boolean checkStatus(String serverHost, int serverPort, WorkerClientFactory workerClientFactory) {
        try (WorkerClient workerClient = workerClientFactory.getClient()) {
            workerClient.checkStatus();
            return true;
        } catch (Exception e) {
            LOGGER.error("Worker server [" + serverHost + ":" + serverPort + "] is not active.", e);
            return false;
        }
    }

    @Override
    public WorkerFamilyManager findWorkerBy(String workerFamily) {
        return workersFamily.get(workerFamily);
    }

    @Override
    public File getWorkerDbFile() {
        return PropertiesUtils.fileFromDataFolder(WORKER_DB_PATH);
    }
}
