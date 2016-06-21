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
 * Meta-data RequestResponseOK class contains hits and result objects
 *
 */
public class RequestResponseOK extends RequestResponse {
    private DatabaseCursor hits;
    // FIXME REVIEW should be List<String> as declaration side
    private ArrayList<String> results;

    /**
     * Empty RequestResponseError constructor
     *
     **/
    public RequestResponseOK() {
        // FIXME REVIEW choose either assigning an empty ArrayList (explicit empty static final List), either keeping
        // this List (and therefore add "add" method and change "set" method to clean and addAll): my preference would
        // go to fix static final empty list
        results = new ArrayList<>();
    }

    /**
     * @return the hits of RequestResponseOK object
     */
    public DatabaseCursor getHits() {
        // FIXME REVIEW do not return null but empty
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
        hits = new DatabaseCursor(total, offset, limit);
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
        // FIXME REVIEW You cannot cast to ArrayList since argument is a List (could be whatever)
        this.results = (ArrayList<String>) results;
        return this;
    }

    /**
     * @return the RequestResponseOK object
     */
    // FIXME REVIEW what is the purpose?
    public RequestResponseOK build() {
        return this;
    }
}
