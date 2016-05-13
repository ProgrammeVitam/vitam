package fr.gouv.vitam.api.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Meta-data RequestResponseOK class
 * contains hits and result objects
 *
 */
public class RequestResponseOK extends RequestResponse {
	private DatabaseCursor hits;
	private ArrayList<String> results;

	/**
	 * Empty RequestResponseError constructor
	 *
	 **/
	public RequestResponseOK() {
		this.results = new ArrayList<>();
	}

    /**
     * @return the hits of RequestResponseOK object
     */	
	public DatabaseCursor getHits() {
		return hits;
	}

	/** 
	 * @param hits as DatabaseCursor object
	 * @return RequestReponseOK with the hits are setted
	 */
	public RequestResponseOK setHits(DatabaseCursor hits) {
		this.hits = hits;
		return this;
	}

	/**
	 * @param total of units inserted/modified as integer 
	 * @param offset of unit in database as integer
	 * @param limit of unit per response as integer
	 * @return the RequestReponseOK with the hits are setted
	 */
	public RequestResponseOK setHits(int total, int offset, int limit) {
		this.hits = new DatabaseCursor(total, offset, limit);
		return this;
	}

	/**
	 * @return the result of RequestResponse as a list of String
	 */
	public List<String> getResults() {
		return results;
	}

	/**
	 * @param results as a list of String 
	 * @return the RequestReponseOK with the result is setted
	 */
	public RequestResponseOK setResults(List<String> results) {
		this.results = (ArrayList<String>) results;
		return this;
	}

	/**
	 * @return the RequestResponseOK object
	 */
	public RequestResponseOK build() {
		return this;
	}
}
