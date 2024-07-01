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
package fr.gouv.vitam.processing.distributor.api;

import com.fasterxml.jackson.core.type.TypeReference;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.WorkerFamilyNotFoundException;
import fr.gouv.vitam.processing.common.model.WorkerBean;
import fr.gouv.vitam.processing.common.model.WorkerRemoteConfiguration;
import fr.gouv.vitam.processing.distributor.core.WorkerFamilyManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Manage the parallelism calls to worker in the same distributor
 */
public interface IWorkerManager {
    /**
     * VitamLogger
     */
    VitamLogger LOGGER = VitamLoggerFactory.getInstance(IWorkerManager.class);
    /**
     * Path to database
     */
    String WORKER_DB_PATH = "worker.db";

    /**
     * Do the initialization Load worker from worker.db
     */
    default void initialize() throws IOException {
        if (getWorkerDbFile().exists()) {
            loadWorkerList(getWorkerDbFile());
        } else {
            LOGGER.warn("No worker list serialization file : " + getWorkerDbFile().getName());
        }
    }

    /**
     * To load a registered worker list
     *
     * @param registerWorkerFile the register worker file
     */
    default void loadWorkerList(File registerWorkerFile) throws IOException {
        // Load the list of worker from database
        // for now it is a file content json data
        try {
            List<WorkerBean> workerBeans = JsonHandler.getFromFileAsTypeReference(
                registerWorkerFile,
                new TypeReference<List<WorkerBean>>() {}
            );
            for (WorkerBean workerBean : workerBeans) {
                String workerId = workerBean.getWorkerId();
                String familyId = workerBean.getFamily();
                // Ignore if the familyId or the workerId is null
                if (familyId == null || workerId == null) {
                    // Mandatory argument missing : Continue with the next worker
                    LOGGER.error(
                        "Mandatory arguments missing: family = " +
                        familyId +
                        ", worker= " +
                        workerId +
                        ". Continue with next workers"
                    );
                    continue;
                }
                WorkerRemoteConfiguration config = workerBean.getConfiguration();
                if (checkStatusWorker(config.getServerHost(), config.getServerPort())) {
                    try {
                        registerWorker(workerBean);
                    } catch (IOException e) {
                        // IOException ignored at this point because a global marshallDB will be done below.
                        LOGGER.warn("Error while save worker to file", e);
                    }
                }
            }
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Cannot load worker list from database.", e);
            // Ignore the rest of the file
            return;
        }

        // Backup worker list to file
        marshallToDB();
    }

    boolean checkStatusWorker(String serverHost, int serverPort);

    /**
     * To register a worker in the processing
     *
     * @param familyId : family of this worker
     * @param workerId : ID of the worker
     * @param workerInformation : Worker Json representation
     * @throws ProcessingBadRequestException if cannot register worker to family
     * @throws InvalidParseOperationException if worker description is not well-formed
     */
    void registerWorker(String familyId, String workerId, WorkerBean workerInformation)
        throws ProcessingBadRequestException, IOException;

    /**
     * Register a worker
     *
     * @param workerBean the worker description as a WorkerBean object
     * @throws IOException thrown when IOException occurs while read/save worker backup file
     */
    void registerWorker(WorkerBean workerBean) throws IOException;

    /**
     * To unregister a worker in the processing
     *
     * @param familyId : family of this worker
     * @param workerId : ID of the worker
     * @throws WorkerFamilyNotFoundException : when the family is unknown
     * @throws IOException if IOException occurs
     */
    void unregisterWorker(String familyId, String workerId) throws WorkerFamilyNotFoundException, IOException;

    /**
     * Marshall to Database
     */
    void marshallToDB() throws IOException;

    /**
     * Find a worker by its family
     *
     * @param workerFamily the worker family
     * @return a WorkerFamilyManager object
     */
    WorkerFamilyManager findWorkerBy(String workerFamily);

    File getWorkerDbFile();
}
