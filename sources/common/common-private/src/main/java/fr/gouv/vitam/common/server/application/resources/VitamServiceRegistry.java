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
package fr.gouv.vitam.common.server.application.resources;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.configuration.DatabaseConnection;

import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;

/**
 * VItam Service Registry that contains all dependencies for the current Application
 */
public class VitamServiceRegistry {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamServiceRegistry.class);

    private static final String SERVICE_IS_UNAVAILABLE = " service is unavailable";
    private static final String SERVICE_IS_AVAILABLE = " service is available";

    private final List<VitamClientFactoryInterface<?>> clientFactories = new ArrayList<>();
    private final List<VitamClientFactoryInterface<?>> clientOptionalFactories = new ArrayList<>();
    private final List<DatabaseConnection> databaseFactories = new ArrayList<>();
    private VitamStatusService applicationStatus = new BasicVitamStatusServiceImpl();

    /**
     * Constructor
     */
    public VitamServiceRegistry() {
        // Empty
    }

    /**
     * Register one Client factory
     *
     * @param factory Http Client Factory
     * @return this
     */
    public VitamServiceRegistry register(VitamClientFactoryInterface<?> factory) {
        if (factory != null) {
            clientFactories.add(factory);
        }
        return this;
    }

    /**
     * Register one Optional Client factory
     *
     * @param factory optional Http Client Factory
     * @return this
     */
    public VitamServiceRegistry registerOptional(VitamClientFactoryInterface<?> factory) {
        if (factory != null) {
            clientOptionalFactories.add(factory);
        }
        return this;
    }

    /**
     * Register one Database
     *
     * @param database database connection
     * @return this
     */
    public VitamServiceRegistry register(DatabaseConnection database) {
        if (database != null) {
            databaseFactories.add(database);
        }
        return this;
    }

    /**
     * Register the Status service of this application (unique)
     *
     * @param service
     * @return this
     */
    public VitamServiceRegistry register(VitamStatusService service) {
        if (service != null) {
            applicationStatus = service;
        }
        return this;
    }

    /**
     * @return the number of registered services, including itself
     */
    public int getRegisteredServices() {
        return databaseFactories.size() + clientFactories.size() + clientOptionalFactories.size() + 1;
    }

    /**
     * Get the resource overall status, except optional services
     *
     * return the overall status of this component with the constraint delay of less than 10ms.
     *
     * @return boolean
     */
    public boolean getResourcesStatus() {
        for (final VitamClientFactoryInterface<?> factory : clientFactories) {
            try (MockOrRestClient client = factory.getClient()) {
                client.checkStatus();
            } catch (final VitamApplicationServerException e) {
                LOGGER.info("Can't connect to factory: " + factory.toString(), e);
                return false;
            }
        }
        for (final DatabaseConnection database : databaseFactories) {
            if (!database.checkConnection()) {
                LOGGER.info("Can't connect to database: " + database.toString());
                return false;
            }
        }
        return applicationStatus.getResourcesStatus();
    }

    /**
     * Check all the registered dependencies, except optional
     *
     * @param retry the number retry in case of unavailability
     * @param retryDelay the delay in ms between each retry
     * @throws VitamApplicationServerException if any of the dependencies are unavailable
     * @throws InterruptedException
     */
    public void checkDependencies(int retry, long retryDelay)
        throws VitamApplicationServerException, InterruptedException {
        for (int i = 0; i < retry; i++) {
            if (getResourcesStatus()) {
                return;
            }
            Thread.sleep(retryDelay);
        }
        final String status =
            "Dependencies in error after " + retry + " checks : " + JsonHandler.prettyPrint(getAutotestStatus());
        LOGGER.error(status);
        throw new VitamApplicationServerException(status);
    }

    /**
     * Get full Autotest status, including optional services
     *
     * return the overall status of this component with the constraint delay of less than 10ms and shall return by
     * default empty JsonNode.
     *
     * @return ServerIdentity
     */
    public ObjectNode getAutotestStatus() {
        final VitamError status = new VitamError("000000")
            .setDescription(ServerIdentity.getInstance().getName())
            .setContext(ServerIdentity.getInstance().getRole());
        int test = 0;
        boolean globalStatus = true;
        final List<VitamError> list = new ArrayList<>();
        for (final VitamClientFactoryInterface<?> factory : clientFactories) {
            test++;
            final String name = StringUtils.getClassName(factory);
            final VitamError sub = new VitamError(Integer.toString(test)).setContext(name);
            try (MockOrRestClient client = factory.getClient()) {
                client.checkStatus();
                sub
                    .setDescription(name + SERVICE_IS_AVAILABLE)
                    .setHttpCode(Status.OK.getStatusCode())
                    .setMessage("Sub" + SERVICE_IS_AVAILABLE)
                    .setState(Status.OK.getReasonPhrase());
            } catch (final VitamApplicationServerException e) {
                LOGGER.warn(
                    "Can't connect to factory: [" + name + "] " + factory.getServiceUrl() + "\n\t" + e.getMessage(),
                    e
                );
                sub
                    .setDescription(name + SERVICE_IS_UNAVAILABLE)
                    .setHttpCode(Status.SERVICE_UNAVAILABLE.getStatusCode())
                    .setMessage("Sub" + SERVICE_IS_UNAVAILABLE)
                    .setState(Status.SERVICE_UNAVAILABLE.getReasonPhrase());
                globalStatus = false;
            }
            list.add(sub);
        }
        for (final VitamClientFactoryInterface<?> factory : clientOptionalFactories) {
            test++;
            final String name = StringUtils.getClassName(factory);
            final VitamError sub = new VitamError(Integer.toString(test)).setContext(name);
            try (MockOrRestClient client = factory.getClient()) {
                client.checkStatus();
                sub
                    .setDescription(name + SERVICE_IS_AVAILABLE)
                    .setHttpCode(Status.OK.getStatusCode())
                    .setMessage("Optional Sub" + SERVICE_IS_AVAILABLE)
                    .setState(Status.OK.getReasonPhrase());
            } catch (final VitamApplicationServerException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                LOGGER.warn(
                    "Can't connect to Optional factory: [" +
                    name +
                    "] " +
                    factory.getServiceUrl() +
                    "\n\t" +
                    e.getMessage()
                );
                sub
                    .setDescription(name + SERVICE_IS_UNAVAILABLE)
                    .setHttpCode(Status.SERVICE_UNAVAILABLE.getStatusCode())
                    .setMessage("Optional Sub" + SERVICE_IS_UNAVAILABLE)
                    .setState(Status.SERVICE_UNAVAILABLE.getReasonPhrase());
                // Do not change globalStatus
            }
            list.add(sub);
        }
        for (final DatabaseConnection database : databaseFactories) {
            test++;
            final String name = StringUtils.getClassName(database);
            final VitamError sub = new VitamError(Integer.toString(test)).setContext(name);
            if (database.checkConnection()) {
                sub
                    .setDescription(name + SERVICE_IS_AVAILABLE)
                    .setHttpCode(Status.OK.getStatusCode())
                    .setMessage("Sub" + SERVICE_IS_AVAILABLE)
                    .setState(Status.OK.getReasonPhrase());
            } else {
                LOGGER.warn("Can't connect to database: [" + name + "] " + database.toString());
                sub
                    .setDescription(name + SERVICE_IS_UNAVAILABLE)
                    .setHttpCode(Status.SERVICE_UNAVAILABLE.getStatusCode())
                    .setMessage("Sub" + SERVICE_IS_UNAVAILABLE)
                    .setState(Status.SERVICE_UNAVAILABLE.getReasonPhrase());
                globalStatus = false;
            }
            list.add(sub);
        }
        test++;
        final VitamError sub = new VitamError(Integer.toString(test)).setContext(status.getContext());
        if (applicationStatus.getResourcesStatus()) {
            sub
                .setHttpCode(Status.OK.getStatusCode())
                .setMessage("Internal" + SERVICE_IS_AVAILABLE)
                .setDescription(status.getDescription() + SERVICE_IS_AVAILABLE)
                .setState(Status.OK.getReasonPhrase());
        } else {
            LOGGER.warn(sub.getContext() + SERVICE_IS_UNAVAILABLE);
            sub
                .setHttpCode(Status.SERVICE_UNAVAILABLE.getStatusCode())
                .setMessage("Internal" + SERVICE_IS_UNAVAILABLE)
                .setDescription(status.getDescription() + SERVICE_IS_UNAVAILABLE)
                .setState(Status.SERVICE_UNAVAILABLE.getReasonPhrase());
        }
        list.add(sub);
        status.addAllErrors(list);
        if (globalStatus) {
            status
                .setDescription("All services are available")
                .setHttpCode(Status.OK.getStatusCode())
                .setMessage("All services are available")
                .setState(Status.OK.getReasonPhrase());
        } else {
            status
                .setDescription("Some services are unavailable")
                .setHttpCode(Status.SERVICE_UNAVAILABLE.getStatusCode())
                .setMessage("Some services are unavailable")
                .setState(Status.SERVICE_UNAVAILABLE.getReasonPhrase());
        }
        return (ObjectNode) status.toJsonNode();
    }
}
