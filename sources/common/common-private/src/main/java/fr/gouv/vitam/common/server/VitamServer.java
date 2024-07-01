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
package fr.gouv.vitam.common.server;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.xml.XmlConfiguration;

/**
 * Interface for every Vitam REST Server
 */
public interface VitamServer extends VitamServerInterface {
    /**
     * Default message
     */
    String SERVER_CAN_NOT_START = "%s application server can not start. ";
    /**
     * Default message
     */
    String CONFIG_FILE_IS_A_MANDATORY_ARGUMENT = "Config file %s is a mandatory argument. ";
    /**
     * Default message
     */
    String CONFIGURATION_IS_A_MANDATORY_ARGUMENT = "Configuration %s is a mandatory argument. ";
    /**
     * Default message
     */
    String CAN_CONFIGURE_SERVER = "Can not config %s server. ";
    /**
     * Default message
     */
    String SERVER_START_WITH_JETTY_CONFIG = "%s application server start with jetty config. ";

    /**
     * Configure the server with one handler
     *
     * @param applicationHandler
     * @throws VitamApplicationServerException
     */
    void configure(Handler applicationHandler) throws VitamApplicationServerException;

    /**
     * Start the server with implicit Join
     *
     * @throws VitamApplicationServerException
     */
    void startAndJoin() throws VitamApplicationServerException;

    /**
     * @return true if Server is configured
     */
    boolean isConfigured();

    /**
     * @return the associated handler
     */
    Handler getHandler();

    /**
     * retrieve the jettyServer
     *
     * @return the jetty server
     */
    Server getServer();

    /**
     * get the configuration of jetty
     *
     * @return the config
     */
    XmlConfiguration getServerConfiguration();

    /**
     * check if vitam server is started
     *
     * @return true if jetty server is started
     */
    boolean isStarted();

    /**
     * check if vitam server is stopped
     *
     * @return true if jetty server is stopped
     */
    boolean isStopped();

    /**
     * stop the jetty server
     *
     * @throws VitamApplicationServerException
     */
    void stop() throws VitamApplicationServerException;

    /**
     * start the jetty server without join (Junit only)
     *
     * @throws VitamApplicationServerException
     */
    void start() throws VitamApplicationServerException;

    /**
     * set jetty handler
     *
     * @param handler the handler to set
     */
    void setHandler(Handler handler);
}
