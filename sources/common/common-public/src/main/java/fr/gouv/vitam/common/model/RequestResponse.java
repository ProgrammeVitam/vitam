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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract RequestResponse for all request response in Vitam
 */
public abstract class RequestResponse<T> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RequestResponse.class);

    @JsonProperty("httpCode")
    private int httpCode;

    @JsonIgnore
    private Map<String, String> vitamHeaders = new HashMap<>();

    /**
     * @return the httpCode
     */
    @JsonGetter("httpCode")
    public int getHttpCode() {
        return this.httpCode;
    }

    /**
     * @param httpCode the httpCode to set
     * @return this
     */
    @JsonSetter("httpCode")
    public RequestResponse<T> setHttpCode(int httpCode) {
        this.httpCode = httpCode;
        return this;
    }

    @JsonIgnore
    public int getStatus() {
        return httpCode;
    }

    /**
     * @return True if this RequestResponse is an Ok response
     */
    @JsonIgnore
    public boolean isOk() {
        return this instanceof RequestResponseOK;
    }

    @JsonIgnore
    public RequestResponse<T> addHeader(String key, String value) {
        this.vitamHeaders.put(key, value);
        return this;
    }

    @JsonIgnore
    public String getHeaderString(String key) {
        return this.vitamHeaders.get(key);
    }

    @JsonIgnore
    public Map<String, String> getVitamHeaders() {
        return this.vitamHeaders;
    }

    @JsonIgnore
    public void unSetVitamHeaders() {
        this.vitamHeaders = null;
    }

    @Override
    public String toString() {
        return JsonHandler.unprettyPrint(this);
    }

    /**
     * @return the Json representation
     * @throws IllegalStateException if JsonNode parse exception occurred
     */
    @JsonIgnore
    public JsonNode toJsonNode() {
        try {
            return JsonHandler.getFromString(toString());
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new IllegalStateException(e);
        }
    }

    @JsonIgnore
    public RequestResponse<T> parseHeadersFromResponse(Response response) {
        if (response != null && response.getHeaders() != null) {
            for (final String key : response.getHeaders().keySet()) {
                // Copy Vitam Header starts with X-in the response
                if (key.startsWith("X-") || key.startsWith("x-")) {
                    this.vitamHeaders.put(key, response.getHeaderString(key));
                }
            }
        }
        if (response != null) {
            this.setHttpCode(response.getStatus());
        }
        return this;
    }

    /**
     * Parser the response for a RequestResponse object.<br/>
     * <br/>
     * Might return an empty VitamError in case response is empty with only the HttpCode set and the Code set to empty
     * String.
     *
     * @param response to parse in RequestResponse
     * @return The associate RequestResponseOk or VitamError
     * @throws IllegalStateException if the response cannot be parsed to one of the two model
     */
    @JsonIgnore
    public static RequestResponse<JsonNode> parseFromResponse(Response response) throws IllegalStateException {
        return parseFromResponse(response, JsonNode.class);
    }

    /**
     * Parser the response for a RequestResponse object.<br/>
     * <br/>
     * Might return an empty VitamError in case response is empty with only the HttpCode set and the Code set to empty
     * String.
     *
     * @param response to parse in RequestResponse
     * @return The associate RequestResponseOk or VitamError
     * @throws IllegalStateException if the response cannot be parsed to one of the two model
     */
    @JsonIgnore
    public static <T> RequestResponse<T> parseFromResponse(Response response, Class<T> clazz)
        throws IllegalStateException {
        final String result = response.readEntity(String.class);
        if (result != null && !result.isEmpty()) {
            if (result.contains("$hits")) {
                try {
                    final RequestResponse<T> ret = RequestResponseOK.getFromJsonNode(
                        JsonHandler.getFromString(result),
                        clazz
                    );
                    return ret.parseHeadersFromResponse(response);
                } catch (final InvalidParseOperationException e) {
                    // Issue, trying RequestResponseOK model
                    LOGGER.warn("Issue while decoding RequestResponseOk", e);
                }
            } else if (result.contains("httpCode")) {
                try {
                    final VitamError<T> error = JsonHandler.getFromStringAsTypeReference(
                        result,
                        new TypeReference<>() {}
                    );
                    return error.parseHeadersFromResponse(response);
                } catch (final InvalidParseOperationException | InvalidFormatException e) {
                    // Issue, while trying VitamError model
                    LOGGER.warn("Issue while decoding VitamError", e);
                }
            }
            throw new IllegalStateException("Cannot parse the response");
        }
        return VitamError.newVitamError(clazz)
            .setCode("")
            .setHttpCode(response.getStatus())
            .addHeader(GlobalDataRest.X_REQUEST_ID, response.getHeaderString(GlobalDataRest.X_REQUEST_ID));
    }

    /**
     * @param response to parse in RequestResponse
     * @return the RequestResponseOk
     * @throws InvalidParseOperationException if JsonNode parse exception occurred
     */
    @JsonIgnore
    @SuppressWarnings("unused")
    public static <T> RequestResponseOK<T> parseRequestResponseOk(Response response, Class<T> clasz)
        throws InvalidParseOperationException {
        final RequestResponseOK<T> ret = RequestResponseOK.getFromJsonNode(
            JsonHandler.getFromString(response.readEntity(String.class)),
            clasz
        );
        ret.parseHeadersFromResponse(response);
        return ret;
    }

    /**
     * @param response to parse in RequestResponse
     * @return the RequestResponseOk
     * @throws InvalidParseOperationException if JsonNode parse exception occurred
     */
    @JsonIgnore
    public static RequestResponseOK<JsonNode> parseRequestResponseOk(Response response)
        throws InvalidParseOperationException {
        return RequestResponse.parseRequestResponseOk(response, JsonNode.class);
    }

    /**
     * @param response to parse in RequestResponse
     * @return the VitamError
     * @throws InvalidParseOperationException if JsonNode parse exception occurred
     */
    @JsonIgnore
    @SuppressWarnings("unused")
    public static <T> VitamError<T> parseVitamError(Response response, Class<T> clasz)
        throws InvalidParseOperationException {
        final VitamError<T> error = VitamError.getFromJsonNode(
            JsonHandler.getFromString(response.readEntity(String.class)),
            clasz
        );
        error.parseHeadersFromResponse(response);
        return error;
    }

    /**
     * @param response to parse in RequestResponse
     * @return the VitamError
     * @throws InvalidParseOperationException if JsonNode parse exception occurred
     */
    @JsonIgnore
    public static VitamError<JsonNode> parseVitamError(Response response) throws InvalidParseOperationException {
        return VitamError.parseVitamError(response, JsonNode.class);
    }

    /**
     * Check if the JsonNode is a RequestResponse and OK
     *
     * @param requestResponseAsJsonNode as request response as a JsonNode
     * @return true if JsonNode contains httpCode as 2xx or 3xx, false if httpCode as 4xx or 5xx
     * @throws IllegalStateException if JsonNode is not a valid instance of requestResponse
     */
    @JsonIgnore
    public static boolean isRequestResponseOk(JsonNode requestResponseAsJsonNode) throws IllegalStateException {
        if (requestResponseAsJsonNode.has("httpCode") && requestResponseAsJsonNode.get("httpCode").isInt()) {
            int httpCode = requestResponseAsJsonNode.get("httpCode").asInt();
            Status.Family family = Status.Family.familyOf(httpCode);
            switch (family) {
                case SUCCESSFUL:
                case REDIRECTION:
                    return true;
                case CLIENT_ERROR:
                case SERVER_ERROR:
                    return false;
                default:
                    break;
            }
        }
        throw new IllegalStateException("Response is not a valid instance of RequestResponse");
    }

    /**
     * transform a RequestResponse to a standard response
     *
     * @return Response
     */
    public abstract Response toResponse();
}
