package fr.gouv.vitam.api.model;

/**
 * Meta-data RequestResponseError class
 * contains error list
 *
 */
public class RequestResponseError extends RequestResponse {

	private VitamError error;

    /**
     * @return the error of the RequestResponseError
     */ 	
	public VitamError getError() {
		return error;
	}

    /**
     * RequestResponseError constructor
     * @param error
     *          the error message of type VitamError which will be setted for RequestResponseError
     */	
	public RequestResponseError setError(VitamError error) {
		this.error = error;
		return this;
	}
}
