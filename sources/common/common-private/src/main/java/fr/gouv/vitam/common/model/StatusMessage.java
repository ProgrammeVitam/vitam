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
package fr.gouv.vitam.common.model;

import fr.gouv.vitam.common.ServerIdentityInterface;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Default Status message (at that time)
 */
public class StatusMessage {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StatusMessage.class);

    private String name;
    private String role;
    private int pid;

    /**
     * Empty constructor
     */
    public StatusMessage() {
        // Empty
    }

    /**
     * Constructor from ServerIdentity
     *
     * @param serverIdentity containing ServerName, ServerRole, Global PlatformId
     */
    public StatusMessage(ServerIdentityInterface serverIdentity) {
        setName(serverIdentity.getName());
        setRole(serverIdentity.getRole());
        setPid(serverIdentity.getGlobalPlatformId());
    }

    /**
     * @return the server name
     */
    public final String getName() {
        return name;
    }

    protected final void setName(String name) {
        this.name = name;
    }

    /**
     * @return the server role
     */
    public final String getRole() {
        return role;
    }

    protected final void setRole(String role) {
        this.role = role;
    }

    /**
     * @return the unique int identifier for this server
     */
    public final int getPid() {
        return pid;
    }

    protected final void setPid(int pid) {
        this.pid = pid;
    }

    @Override
    public String toString() {
        try {
            return JsonHandler.writeAsString(this);
        } catch (final InvalidParseOperationException e) {
            LOGGER.warn(e);
            return "unknownStatusMessage";
        }
    }
}
