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
package fr.gouv.vitam.access.model;

import java.io.Serializable;

import javax.ws.rs.FormParam;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Unit Request DTO class
 */
@XmlRootElement
public class UnitRequestDTO implements Serializable {


    /**
     *
     */
    private static final long serialVersionUID = -5548063526597112032L;
    @FormParam("queryDsl")
    private String queryDsl;

    /**
     * Default constructor
     */
    public UnitRequestDTO() {
        /**
         * Empty Constructor
         */
    }

    /**
     *
     * @param queryDsl to associate with UnitRequestDTO
     */
    public UnitRequestDTO(String queryDsl) {
        this.queryDsl = queryDsl;
    }

    /**
     *
     * @return a query dsl
     */
    public String getQueryDsl() {
        return queryDsl;
    }

    /**
     * setter for a queryDsl
     *
     * @param queryDsl
     */
    public void setQueryDsl(String queryDsl) {
        this.queryDsl = queryDsl;
    }
}
