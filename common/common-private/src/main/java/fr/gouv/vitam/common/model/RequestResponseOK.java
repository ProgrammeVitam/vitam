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

import fr.gouv.vitam.common.ParametersChecker;


/**
 * Access RequestResponseOK class contains list of results<br>
 * default results : is an empty list (immutable)
 *
 */
public final class RequestResponseOK extends RequestResponse {

    private List<String> results;

    /**
     * Empty RequestResponseError constructor
     *
     **/
    public RequestResponseOK() {
        results = Collections.<String>emptyList();
    }

    /**
     * Add list of results
     *
     * @param resultList the list of results
     * @return RequestResponseOK with mutable results list of String
     */
    public RequestResponseOK addAllResults(List<String> resultList) {
        ParametersChecker.checkParameter("Result list is a mandatory parameter", resultList);
        if (results == null || results.isEmpty()) {
            results = new ArrayList<>();
        }        
        results.addAll(resultList);    
        return this;
    }


    /**
     * @return the result of RequestResponse as a list of String
     */
    public List<String> getResults() {
        return results;
    }

}
