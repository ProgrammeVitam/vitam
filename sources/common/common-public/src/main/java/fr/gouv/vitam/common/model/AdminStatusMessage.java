/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Default Administration Status message
 */
public class AdminStatusMessage {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminStatusMessage.class);

    @JsonProperty("serverIdentity")
    private JsonNode serverIdentity;

    @JsonProperty("status")
    private boolean status;

    @JsonProperty("detail")
    private ObjectNode detail;

    @JsonProperty("componentsVersions")
    private JsonNode componentsVersions;

    /**
     * Empty constructor
     */
    public AdminStatusMessage() {
        // Empty
    }

    /**
     * Constructor from ServerIdentity
     *
     * @param serverIdentity containing ServerName, ServerRole, Global PlatformId
     * @param status True if the status is OK, else False
     * @param detail the detailed status if any
     * @param componentsVersions
     */
    public AdminStatusMessage(JsonNode serverIdentity, boolean status, ObjectNode detail, JsonNode componentsVersions) {
        setServerIdentity(serverIdentity);
        setStatus(status);
        setDetail(detail);
        setComponentsVersions(componentsVersions);
    }

    /**
     * @return the serverIdentity
     */
    public JsonNode getServerIdentity() {
        return serverIdentity;
    }

    /**
     * @param serverIdentity the serverIdentity to set
     * @return this
     */
    public AdminStatusMessage setServerIdentity(JsonNode serverIdentity) {
        this.serverIdentity = serverIdentity;
        return this;
    }

    /**
     * @return the status
     */
    public boolean getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     * @return this
     */
    public AdminStatusMessage setStatus(boolean status) {
        this.status = status;
        return this;
    }

    /**
     * @return the detail
     */
    public ObjectNode getDetail() {
        return detail;
    }

    /**
     * @param detail the detail to set
     * @return this
     */
    public AdminStatusMessage setDetail(ObjectNode detail) {
        this.detail = detail;
        return this;
    }

    /**
     * @return componentsVersions as JsonNode
     */
    public JsonNode getComponentsVersions() {
        return componentsVersions;
    }

    /**
     * @param componentsVersions
     */
    public void setComponentsVersions(JsonNode componentsVersions) {
        this.componentsVersions = componentsVersions;
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
