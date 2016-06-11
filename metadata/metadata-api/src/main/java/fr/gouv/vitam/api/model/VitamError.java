package fr.gouv.vitam.api.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Meta-data RequestResponseError class
 *
 */
// TODO REVIEW should be final
public class VitamError {

    private int code;
    private String context;
    private String state;
    private String message;
    private String description;
    // TODO REVIEW should be List<String> as declaration side
    private ArrayList<VitamError> errors;

    /**
     * RequestResponseError constructor
     *
     * @param code the code used to identify this error object
     **/
    public VitamError(int code) {
        this.code = code;
        // TODO REVIEW choose either assigning an empty ArrayList (explicit empty static final List), either keeping
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
        // TODO REVIEW check illegalValue
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
        // TODO REVIEW check illegalValue
        this.description = description;
        return this;
    }

    /**
     * @param list of errors as List
     * @return the VitamError object with the list of errors is setted
     */
    public VitamError setErrors(List<VitamError> errors) {
        // TODO REVIEW You cannot cast to ArrayList since argument is a List (could be whatever)
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
        // TODO REVIEW do not return null but empty
        return context;
    }

    /**
     * @return the state of the VitamError object
     */
    public String getState() {
        // TODO REVIEW do not return null but empty
        return state;
    }

    /**
     * @return the message of the VitamError object
     */
    public String getMessage() {
        // TODO REVIEW do not return null but empty
        return message;
    }

    /**
     * @return the description of the VitamError object
     */
    public String getDescription() {
        // TODO REVIEW do not return null but empty
        return description;
    }

    /**
     * @return the errors list of the VitamError object
     */
    public List<VitamError> getErrors() {
        return errors;
    }
}
