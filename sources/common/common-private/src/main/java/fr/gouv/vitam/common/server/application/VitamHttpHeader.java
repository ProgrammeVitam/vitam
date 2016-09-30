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

package fr.gouv.vitam.common.server.application;

import fr.gouv.vitam.common.GlobalDataRest;

/**
 * Enum use to represent possible HTTP header for Vitam application. Also define a regular expression to check if values
 * from HTTP headers are right
 *
 */
public enum VitamHttpHeader {

    /**
     * The X_STRATEGY_ID header, used in HEAD requests to ask for a particular strategy
     * TODO : change regex to be more precise (something like ^\s*\w+$ or \s*\w+\s*$)
     */
    STRATEGY_ID(GlobalDataRest.X_STRATEGY_ID, ".+"),
    /**
     * The X_STRATEGY_ID header, used in requests to use a particular strategy
     * TODO : change regex to be more precise (something like ^\s*\w+$ or \s*\w+\s*$)
     */
    TENANT_ID(GlobalDataRest.X_TENANT_ID, ".+"),
    /**
     * The X-Http-Method-Override header, used in requests to handle unsupported Http methods with body
     */
    METHOD_OVERRIDE(GlobalDataRest.X_HTTP_METHOD_OVERRIDE, "get|patch|delete"),
    /**
     * The X-Qualifier header, used to get an object
     */
    QUALIFIER(GlobalDataRest.X_QUALIFIER, ".+"),
    /**
     * The X-Version header, used to get an object
     */
    VERSION(GlobalDataRest.X_VERSION, "[0-9]+");

    private String name;
    private String regExp;

    /**
     * Constructor
     *
     * @param name the HTTP header name
     * @param regExp the regular expression to validate header values
     */
    VitamHttpHeader(String name, String regExp) {
        this.name = name;
        this.regExp = regExp;
    }

    /**
     * Get the header name
     *
     * @return the header name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the regular expression to validate header values
     *
     * @return the regular expression
     */
    public String getRegExp() {
        return this.regExp;
    }

    /**
     * Get VitamHttpHeader from name
     *
     * @param headerName the wanted header name
     * @return the header if exists, null otherwise
     */
    public static VitamHttpHeader get(String headerName) {
        for (VitamHttpHeader v : values()) {
            if (v.getName().equals(headerName)) {
                return v;
            }
        }
        return null;
    }

}
