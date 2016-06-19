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

package fr.gouv.vitam.common.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;

/**
 * Basic implementation of a vitam server using embedded jetty as underlying app server
 *
 */
public class BasicVitamServer implements VitamServer {
    private static final String A_PROBLEM_OCCURRED_WHILE_ATTEMPTING_TO_START_THE_SERVER =
        "A problem occurred while attempting to start the server";
    private final int port;
    private Handler handler;
    private final Server server;
    private boolean configured = false;

    /**
     * A Vitam server can only be instantiated with a given port to listen to
     *
     * @param port the port to listen to (must be a valid logical port number)
     * @throws IllegalArgumentException if port <= 0
     */
    protected BasicVitamServer(int port) {
        ParametersChecker.checkValue("You must provide a valid port number", port, 1);
        this.port = port;
        server = new Server(port);
    }

    @Override
    public void configure(Handler applicationHandler) throws VitamApplicationServerException {
        if (applicationHandler == null) {
            throw new VitamApplicationServerException("You must provide a handler to give to the server");
        }
        setHandler(applicationHandler);
        getServer().setHandler(applicationHandler);
        setConfigured(true);
    }

    @Override
    public void run() throws VitamApplicationServerException {
        start();
        try {
            getServer().join();
        } catch (final Exception exc) {
            throw new VitamApplicationServerException(A_PROBLEM_OCCURRED_WHILE_ATTEMPTING_TO_START_THE_SERVER, exc);
        }
    }

    /**
     * For Junit tests, starts only, not join
     *
     * @throws VitamApplicationServerException
     */
    public void start() throws VitamApplicationServerException {
        if (!isConfigured()) {
            throw new VitamApplicationServerException("You must configure the server before running");
        }
        try {
            getServer().start();
        } catch (final Exception exc) {
            throw new VitamApplicationServerException(A_PROBLEM_OCCURRED_WHILE_ATTEMPTING_TO_START_THE_SERVER, exc);
        }
    }

    /**
     * For Junit tests, stops the server
     *
     * @throws VitamApplicationServerException
     */
    public void stop() throws VitamApplicationServerException {
        if (!isConfigured()) {
            throw new VitamApplicationServerException("You must configure the server before running");
        }
        try {
            getServer().stop();
        } catch (final Exception exc) {
            throw new VitamApplicationServerException(A_PROBLEM_OCCURRED_WHILE_ATTEMPTING_TO_START_THE_SERVER, exc);
        }
    }

    /**
     * Retrieving the underlying jetty server is restricted to sub-implementations only
     *
     * @return the underlying jetty server
     */
    protected Server getServer() {
        return server;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }

    protected void setConfigured(boolean configured) {
        this.configured = configured;
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    protected void setHandler(Handler handler) {
        ParametersChecker.checkParameter("Handler must not be nul", handler);
        this.handler = handler;
    }
}
