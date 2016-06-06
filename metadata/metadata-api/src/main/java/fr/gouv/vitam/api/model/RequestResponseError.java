package fr.gouv.vitam.api.model;

/**
 * Meta-data RequestResponseError class
 * contains error list
 *
 */
// TODO REVIEW Fix comment with a correct vision (either adding <br> either adding ':'
// TODO REVIEW should be final
public class RequestResponseError extends RequestResponse {

	private VitamError error;

    /**
     * @return the error of the RequestResponseError
     */ 	
	public VitamError getError() {
		// TODO REVIEW do not return null but empty
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
