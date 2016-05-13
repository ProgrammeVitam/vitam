package fr.gouv.vitam.api.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Meta-data RequestResponseError class
 *
 */
public class VitamError  {

	private int code;
	private String context;
	private String state;
	private String message;
	private String description;
	private ArrayList<VitamError> errors;

	/**
	 * RequestResponseError constructor
	 *
	 * @param code the code used to identify this error object
	 **/
	public VitamError(int code) {
		this.code = code;
		this.errors = new ArrayList<>();
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
		this.context = context;
		return this;
	}

    /**
     * @param state of error as String
     * @return the VitamError object with the error state is setted 
     */	public VitamError setState(String state) {
		this.state = state;
		return this;
	}

     /**
      * @param message of error as String
      * @return the VitamError object with the error message is setted 
      */     
	public VitamError setMessage(String message) {
		this.message = message;
		return this;
	}

    /**
     * @param description of error as String
     * @return the VitamError object with the description error is setted 
     */	
	public VitamError setDescription(String description) {
		this.description = description;
		return this;
	}

    /**
     * @param list of errors as List
     * @return the VitamError object with the list of errors is setted 
     */	
	public VitamError setErrors(List<VitamError> errors) {
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
		return context;
	}

    /**
     * @return the state of the VitamError object
     */	
	public String getState() {
		return state;
	}

    /**
     * @return the message of the VitamError object
     */	
	public String getMessage() {
		return message;
	}

    /**
     * @return the description of the VitamError object
     */	
	public String getDescription() {
		return description;
	}

    /**
     * @return the errors list of the VitamError object
     */	
	public List<VitamError> getErrors() {
		return errors;
	}
}
