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

package fr.gouv.vitam.common.security.waf;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.owasp.esapi.ESAPI;

/**
 * Wrapper cross-site scripting(XSS)
 */
public class XSSWrapper extends HttpServletRequestWrapper {

    private static final String HTTP_PARAMETER_VALUE = "HTTPParameterValue";
    private static final String HTTP_PARAMETER_NAME = "HTTPParameterName";
    private static final String HTTP_HEADER_NAME = "HTTPHeaderName";
    private static final String HTTP_HEADER_VALUE = "HTTPHeaderValue";
    private static final int REQUEST_LIMIT = 10000;


    /**
     * Constructor XSSWrapper
     *
     * @param servletRequest
     */
    public XSSWrapper(HttpServletRequest servletRequest) {
        super(servletRequest);
    }

    /**
     * Sanitize headers and parameters
     *
     * @return boolean
     */
    public boolean sanitize() {
        return sanitizeParam() | sanitizeHeader();

    }

    private boolean sanitizeHeader() {
        boolean isInfected = false;
        final Enumeration<String> headers = super.getHeaderNames();
        if (headers != null) {
            while (headers.hasMoreElements()) {
                final String header = headers.nextElement();
                final String headerValue = super.getHeader(header);
                isInfected = isInfected | isStringInfected(header, HTTP_HEADER_NAME);
                isInfected = isInfected | isStringInfected(headerValue, HTTP_HEADER_VALUE);
            }
        }
        return isInfected;
    }

    private boolean sanitizeParam() {
        boolean isInfected = false;
        final Enumeration<String> params = super.getParameterNames();
        if (params != null) {
            while (params.hasMoreElements()) {
                final String param = params.nextElement();
                final String paramValue = super.getParameter(param);
                isInfected = isInfected | isStringInfected(param, HTTP_PARAMETER_NAME);
                isInfected = isInfected | isStringInfected(paramValue, HTTP_PARAMETER_VALUE);
            }
        }
        return isInfected;
    }

    /**
     * Find out XSS by ESAPI validator
     *
     * @param value of string
     * @param validator name declared in ESAPI.properties
     * @return boolean
     */
    private static boolean isStringInfected(String value, String validator) {
        return !ESAPI.validator().isValidInput(validator, value, validator, REQUEST_LIMIT, true);
    }
}
