/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

package fr.gouv.vitam.common.error;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;


/**
 * VitamError class
 */
public class VitamError extends RequestResponse {


    @JsonProperty("code")
    private String code;
    @JsonProperty("context")
    private String context;
    @JsonProperty("state")
    private String state;
    @JsonProperty("message")
    private String message;
    @JsonProperty("description")
    private String description;
    @JsonProperty("errors")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<VitamError> errors;

    protected VitamError() {
        // For Json builder
    }

    /**
     * VitamError constructor
     *
     * @param code the code used to identify this error object
     **/
    public VitamError(String code) {
        this.code = code;
        errors = new ArrayList<>();
    }
    
    /**
     * @param code of error as integer
     * @return the VitamError object with the code is setted
     */
    @JsonSetter("code")
    public VitamError setCode(String code) {
        this.code = code;
        return this;
    }

    /**
     * @param httpCode the httpCode to set
     * @return this
     */
    @Override
    public VitamError setHttpCode(int httpCode) {
        super.setHttpCode(httpCode);
        return this;
    }


    /**
     * @param context of error as String
     * @return the VitamError object with the context is setted
     */
    @JsonSetter("context")
    public VitamError setContext(String context) {
        ParametersChecker.checkParameter("context is a mandatory parameter", context);
        this.context = context;
        return this;
    }

    /**
     * @param state of error as String
     * @return the VitamError object with the error state is setted
     */
    @JsonSetter("state")
    public VitamError setState(String state) {
        ParametersChecker.checkParameter("state is a mandatory parameter", state);
        this.state = state;
        return this;
    }

    /**
     * @param message of error as String
     * @return the VitamError object with the error message is setted
     */
    @JsonSetter("message")
    public VitamError setMessage(String message) {
        ParametersChecker.checkParameter("message is a mandatory parameter", message);
        this.message = message;
        return this;
    }

    /**
     * @param description of error as String
     * @return the VitamError object with the description error is setted
     */
    @JsonSetter("description")
    public VitamError setDescription(String description) {
        ParametersChecker.checkParameter("description is a mandatory parameter", description);
        this.description = description;
        return this;
    }

    /**
     * @param errors errors as List
     * @return the VitamError object with the list of errors is setted
     */
    @JsonSetter("errors")
    public VitamError addAllErrors(List<VitamError> errors) {
        ParametersChecker.checkParameter("errors list is a mandatory parameter", errors);
        if (this.errors == null) {
            this.errors = errors;
        } else {
            this.errors.addAll(errors);
        }
        return this;
    }


    /**
     * @param error one error
     * @return the VitamError object with the list of errors is setted
     */
    @JsonIgnore
    public VitamError addToErrors(VitamError error) {
        ParametersChecker.checkParameter("error is a mandatory parameter", error);
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);

        return this;
    }


    /**
     * @return the code of the VitamError object
     */
    public String getCode() {
        return code;
    }

    /**
     * @return the context of the VitamError object
     */
    @JsonGetter("context")
    public String getContext() {
        return context;
    }

    /**
     * @return the state of the VitamError object
     */
    @JsonGetter("state")
    public String getState() {
        return state;
    }

    /**
     * @return the message of the VitamError object
     */
    @JsonGetter("message")
    public String getMessage() {
        return message;
    }

    /**
     * @return the description of the VitamError object
     */
    @JsonGetter("description")
    public String getDescription() {
        return description;
    }

    /**
     * @return the errors list of the VitamError object
     */
    @JsonGetter("errors")
    public List<VitamError> getErrors() {
        return errors;
    }

    /**
     * @param node of vitam error in format JsonNode
     * @return the corresponding VitamError
     * @throws InvalidParseOperationException if parse JsonNode node exception occurred
     */
    public static VitamError getFromJsonNode(JsonNode node) throws InvalidParseOperationException {
        return JsonHandler.getFromJsonNode(node, VitamError.class);
    }

    /**
     * transform a RequestResponse to a standard response
     *
     * @return Response
     */
    @Override
    public Response toResponse() {
        final ResponseBuilder resp = Response.status(getStatus()).entity(JsonHandler.unprettyPrint(this));
        final Map<String, String> vitamHeaders = getVitamHeaders();
        for (final String key : vitamHeaders.keySet()) {
            resp.header(key, getHeaderString(key));
        }

        unSetVitamHeaders();
        return resp.build();
    }

    /**
     * transform a RequestResponse to a stream response
     *
     * @return Response
     */
    public Response toStreamResponse() {
        InputStream entity;
        try {
            entity = JsonHandler.writeToInpustream(this);
        } catch (InvalidParseOperationException e) {
            entity = new ByteArrayInputStream("{ 'message' : 'Invalid VitamError message' }".getBytes());
        }
        ResponseBuilder resp = Response.status(getStatus()).entity(entity);
        final Map<String, String> vitamHeaders = getVitamHeaders();
        for (final String key : vitamHeaders.keySet()) {
            resp.header(key, getHeaderString(key));
        }

        unSetVitamHeaders();
        return resp.build();
    }
}
