/*******************************************************************************
 * This file is part of Vitam Project.
 * <p>
 * Copyright Vitam (2012, 2015)
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.common.model;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import fr.gouv.vitam.common.json.JsonHandler;

/**
 * Access RequestResponseError class
 *
 */
public final class VitamError {

    private int code;
    private String context;
    private String state;
    private String message;
    private String description;
    private List<VitamError> errors;

    /**
     * RequestResponseError constructor <br>
     * <br>
     * null is not allowed errors will be initialized with an immutable empty list
     *
     *
     * @param code the code used to identify this error object
     **/
    public VitamError(int code) {
        this.code = code;
        errors = Collections.<VitamError>emptyList();
    }

    /**
     * @param code of error as integer <br>
     *        null is not allowed
     * @return the VitamError object with the code is filled
     */
    public VitamError setCode(int code) {
        this.code = code;
        return this;
    }

    /**
     * @param context of error as String <br>
     *        null is not allowed
     * @return the VitamError object with the context is filled
     */
    public VitamError setContext(String context) {
        if (StringUtils.isNotBlank(context)) {
            this.context = context;
        }
        return this;
    }

    /**
     * @param state of error as String <br>
     *        null is not allowed
     * @return the VitamError object with the error state is filled
     */
    public VitamError setState(String state) {
        if (state != null) {
            this.state = state;
        }
        return this;
    }

    /**
     * @param message of error as String <br>
     *        null is not allowed
     * @return the VitamError object with the error message is filled
     */
    public VitamError setMessage(String message) {
        if (message != null) {
            this.message = message;
        }
        return this;
    }

    /**
     * @param description of error as String <br>
     *        null is not allowed
     * @return the VitamError object with the description error is filled
     */
    public VitamError setDescription(String description) {
        if (description != null) {
            this.description = description;
        }
        return this;
    }


    /**
     * @param list of errors as List
     * @return the VitamError object with the list of errors is filled
     */
    // TODO bad comment: parameter name
    public VitamError addAllErrors(List<VitamError> errors) {
        if (this.errors == null || this.errors.isEmpty()) {
            this.errors = new ArrayList<>();
        }
        this.errors.addAll(errors);
        return this;

    }



    /**
     * @return the code of the VitamError object<br>
     *         return empty string if code is null
     */
    public int getCode() {
        return code;
    }

    /**
     * @return the context of the VitamError object <br>
     *         return empty string if context is null
     */
    public String getContext() {
        if (context == null) {
            return "";
        }
        return context;
    }

    /**
     * @return the state of the VitamError object<br>
     *         return empty string if state is null
     */
    public String getState() {
        if (state == null) {
            return "";
        }
        return state;
    }

    /**
     * @return the message of the VitamError object <br>
     *         return empty string if message is null
     */
    public String getMessage() {
        if (message == null) {
            return "";
        }
        return message;
    }

    /**
     * @return the description of the VitamError object <br>
     *         return empty string if description is null
     */
    public String getDescription() {

        if (description == null) {
            return "";
        }
        return description;
    }

    /**
     * @return the errors list of the VitamError object
     *
     *
     */
    public List<VitamError> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return JsonHandler.unprettyPrint(this);
    }
}
