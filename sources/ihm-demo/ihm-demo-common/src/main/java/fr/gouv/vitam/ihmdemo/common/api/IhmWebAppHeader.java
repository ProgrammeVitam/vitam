/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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
 */
package fr.gouv.vitam.ihmdemo.common.api;

import fr.gouv.vitam.common.GlobalDataRest;

import javax.ws.rs.core.HttpHeaders;

/**
 * Enum use to represent possible HTTP header for Vitam application. Also define a regular expression to check if values
 * from HTTP headers are right
 */
public enum IhmWebAppHeader {
    /**
     * The X_LIMIT header, used to get an object
     */
    LIMIT(IhmDataRest.X_LIMIT, "[0-9]+"),
    /**
     * The X_OFFSET header, used to get an object
     */
    OFFSET(IhmDataRest.X_OFFSET, "[0-9]+"),
    /**
     * The X_TOTAL header, used to get an object
     */
    TOTAL(IhmDataRest.X_TOTAL, "[0-9]+"),
    /**
     * The X_REQUEST_ID header, used to get an object
     */
    REQUEST_ID(GlobalDataRest.X_REQUEST_ID, "[a-z0-9]+"),
    /**
     * The COOKIE header, used to get an object
     */
    COOKIE(HttpHeaders.COOKIE, "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");

    private String name;
    private String regExp;

    /**
     * Constructor
     *
     * @param name the HTTP header name
     * @param regExp the regular expression to validate header values
     */
    IhmWebAppHeader(String name, String regExp) {
        this.name = name;
        this.regExp = regExp;
    }

    /**
     * Get the header name
     *
     * @return the header name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the regular expression to validate header values
     *
     * @return the regular expression
     */
    public String getRegExp() {
        return regExp;
    }

    /**
     * Get VitamHttpHeader from name
     *
     * @param headerName the wanted header name
     * @return the header if exists, null otherwise
     */
    public static IhmWebAppHeader get(String headerName) {
        for (final IhmWebAppHeader v : values()) {
            if (v.getName().equals(headerName)) {
                return v;
            }
        }
        return null;
    }
}
