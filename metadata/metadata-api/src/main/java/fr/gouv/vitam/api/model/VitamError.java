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
package fr.gouv.vitam.api.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Meta-data RequestResponseError class
 *
 */
public class VitamError {

    private int code;
    private String context;
    private String state;
    private String message;
    private String description;
    // FIXME REVIEW should be List<String> as declaration side
    private ArrayList<VitamError> errors;

    /**
     * RequestResponseError constructor
     *
     * @param code the code used to identify this error object
     **/
    public VitamError(int code) {
        this.code = code;
        // FIXME REVIEW choose either assigning an empty ArrayList (explicit empty static final List), either keeping
        // this List (and therefore add "add" method and change "set" method to clean and addAll): my preference would
        // go to fix static final empty list
        errors = new ArrayList<>();
    }

    /**
     * @param code of error as integer
     * @return the VitamError object with the code is setted
     */
    public VitamError setCode(int code) {
        this.code = code;
        return this;
    }

    /**
     * @param context of error as String
     * @return the VitamError object with the context is setted
     */
    public VitamError setContext(String context) {
        // FIXME REVIEW check illegalValue
        this.context = context;
        return this;
    }

    /**
     * @param state of error as String
     * @return the VitamError object with the error state is setted
     */
    public VitamError setState(String state) {
        // TODO REVIEW fix style
        // TODO REVIEW check illegalValue
        this.state = state;
        return this;
    }

    /**
     * @param message of error as String
     * @return the VitamError object with the error message is setted
     */
    public VitamError setMessage(String message) {
        // TODO REVIEW check illegalValue
        this.message = message;
        return this;
    }

    /**
     * @param description of error as String
     * @return the VitamError object with the description error is setted
     */
    public VitamError setDescription(String description) {
        // FIXME REVIEW check illegalValue
        this.description = description;
        return this;
    }

    /**
     * @param list of errors as List
     * @return the VitamError object with the list of errors is setted
     */
    public VitamError setErrors(List<VitamError> errors) {
        // FIXME REVIEW You cannot cast to ArrayList since argument is a List (could be whatever)
        this.errors = (ArrayList<VitamError>) errors;
        return this;
    }

    /**
     * @return the code of the VitamError object
     */
    public int getCode() {
        return code;
    }

    /**
     * @return the context of the VitamError object
     */
    public String getContext() {
        // FIXME REVIEW do not return null but empty
        return context;
    }

    /**
     * @return the state of the VitamError object
     */
    public String getState() {
        // FIXME REVIEW do not return null but empty
        return state;
    }

    /**
     * @return the message of the VitamError object
     */
    public String getMessage() {
        // FIXME REVIEW do not return null but empty
        return message;
    }

    /**
     * @return the description of the VitamError object
     */
    public String getDescription() {
        // FIXME REVIEW do not return null but empty
        return description;
    }

    /**
     * @return the errors list of the VitamError object
     */
    public List<VitamError> getErrors() {
        return errors;
    }
}
