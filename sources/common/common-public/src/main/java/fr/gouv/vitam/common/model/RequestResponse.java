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
package fr.gouv.vitam.common.model;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Abstract RequestResponse for all request response in Vitam
 *
 */
public abstract class RequestResponse {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RequestResponse.class);

    /**
     * @return True if this RequestResponse is an Ok response
     */
    @JsonIgnore
    public boolean isOk() {
        return this instanceof RequestResponseOK;
    }

    @Override
    public String toString() {
        return JsonHandler.unprettyPrint(this);
    }

    /**
     * 
     * @return the Json representation
     * @throws IllegalStateException
     */
    @JsonIgnore
    public JsonNode toJsonNode() {
        try {
            return JsonHandler.getFromString(this.toString());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * Parser the response for a RequestResponse object.<br/>
     * <br/>
     * Might return an empty VitamError in case response is empty with only the HttpCode set and the Code set to empty
     * String.
     * 
     * @param response
     * @return The associate RequestResponseOk or VitamError
     * @throws IllegalStateException if the response cannot be parsed to one of the two model
     */
    @JsonIgnore
    public static RequestResponse parseFromResponse(Response response) throws IllegalStateException {
        String result = response.readEntity(String.class);
        if (result != null && !result.isEmpty()) {
            if (result.contains("$hits")) {
                try {
                    return JsonHandler.getFromString(result, RequestResponseOK.class);
                } catch (InvalidParseOperationException e) {
                    // Issue, trying VitamError model
                    LOGGER.warn("Issue while decoding RequestResponseOk", e);
                }
            } else if (result.contains("httpCode")) {
                try {
                    return JsonHandler.getFromString(result, VitamError.class);
                } catch (InvalidParseOperationException e) {
                    // Issue, while trying VitamError model
                    LOGGER.warn("Issue while decoding VitamError", e);
                }
            }
            throw new IllegalStateException("Cannot parse the response");
        }
        return new VitamError("UnknownCode").setHttpCode(response.getStatus()).setCode("");
    }

    /**
     * 
     * @param response
     * @return the RequestResponseOk
     * @throws InvalidParseOperationException 
     */
    @JsonIgnore
    public static RequestResponseOK parseRequestResponseOk(Response response) throws InvalidParseOperationException {
        return JsonHandler.getFromString(response.readEntity(String.class), RequestResponseOK.class);
    }

    /**
     * 
     * @param response
     * @return the VitamError
     * @throws InvalidParseOperationException 
     */
    @JsonIgnore
    public static VitamError parseVitamError(Response response) throws InvalidParseOperationException {
        return JsonHandler.getFromString(response.readEntity(String.class), VitamError.class);
    }
}
