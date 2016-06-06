package fr.gouv.vitam.api.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Meta-data RequestResponseOK class
 * contains hits and result objects
 *
 */
// TODO REVIEW Fix comment with a correct vision (either adding <br> either adding ':'
// TODO REVIEW should be final
public class RequestResponseOK extends RequestResponse {
	private DatabaseCursor hits;
	// TODO REVIEW should be List<String> as declaration side
	private ArrayList<String> results;

	/**
	 * Empty RequestResponseError constructor
	 *
	 **/
	public RequestResponseOK() {
		// TODO REVIEW choose either assigning an empty ArrayList (explicit empty static final List), either keeping this List (and therefore add "add" method and change "set" method to clean and addAll): my preference would go to fix static final empty list 
		this.results = new ArrayList<>();
	}

    /**
     * @return the hits of RequestResponseOK object
     */	
	public DatabaseCursor getHits() {
		// TODO REVIEW do not return null but empty
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
		// TODO REVIEW You cannot cast to ArrayList since argument is a List (could be whatever)
		this.results = (ArrayList<String>) results;
		return this;
	}

	/**
	 * @return the RequestResponseOK object
	 */
	// TODO REVIEW what is the purpose?
	public RequestResponseOK build() {
		return this;
	}
}
