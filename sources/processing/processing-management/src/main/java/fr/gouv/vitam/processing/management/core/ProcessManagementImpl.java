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
package fr.gouv.vitam.processing.management.core;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.engine.api.ProcessEngine;
import fr.gouv.vitam.processing.engine.core.ProcessEngineImplFactory;
import fr.gouv.vitam.processing.management.api.ProcessManagement;

/**
 * ProcessManagementImpl implementation of ProcessManagement API
 */
// FIXME REVIEW add a factory plus constructor and class as package protected
public class ProcessManagementImpl implements ProcessManagement {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProcessManagementImpl.class);
    private final ProcessEngine processEngine;
    private ServerConfiguration serverConfig;

    /**
     * constructor of ProcessManagementImpl
     *
     * @param serverConfig configuration of process engine server
     */
    public ProcessManagementImpl(ServerConfiguration serverConfig) {
        ParametersChecker.checkParameter("Server config cannot be null", serverConfig);
        /**
         * inject process engine
         */
        this.serverConfig = serverConfig;
        processEngine = new ProcessEngineImplFactory().create();
    }

    /**
     * set the server configuration
     *
     * @param serverConfig configuration of process engine server
     * @return ProcessManagementImpl instance with serverConfig is setted
     */
    public ProcessManagementImpl setServerConfig(ServerConfiguration serverConfig) {
        ParametersChecker.checkParameter("Server config cannot be null", serverConfig);
        this.serverConfig = serverConfig;
        return this;
    }

    /**
     * get the server configuration
     *
     * @return serverConfig of type ServerConfiguration
     */
    public ServerConfiguration getServerConfig() {
        return serverConfig;
    }

    /**
     * submitWorkflow implemente submitWorkflow of ProcessManagement API see params and return in ProcessManagement API
     * class
     */
    @Override
    public ItemStatus submitWorkflow(WorkerParameters workParams, String workflowId) throws ProcessingException {
        ItemStatus response;
        workParams.setUrlMetadata(serverConfig.getUrlMetada());
        workParams.setUrlWorkspace(serverConfig.getUrlWorkspace());
        try {
            response = processEngine.startWorkflow(workParams, workflowId);
        } catch (final WorkflowNotFoundException e) {
            LOGGER.error("WorkflowNotFoundException");
            throw new WorkflowNotFoundException(workflowId, e);
        } catch (final IllegalArgumentException e) {
            LOGGER.error("IllegalArgumentException");
            throw new IllegalArgumentException(workflowId, e);
        } catch (final ProcessingException e) {
            LOGGER.error("ProcessingException");
            throw new ProcessingException(workflowId, e);
        }

        return response;
    }
}
