/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.common.security.filter;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.google.common.base.Strings;

import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.codec.URLCodec;

/**
 * Authorization Wrapper
 */
public class AuthorizationWrapper extends HttpServletRequestWrapper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AuthorizationWrapper.class);
    private static final String ARGUMENT_MUST_NOT_BE_NULL = "Argument must not be null";

    /**
     * @param request
     */
    public AuthorizationWrapper(HttpServletRequest request) {        
        super(request);
    }

    /**
     * check Headers X-Platform-Id and X-Timestamp
     *
     * @return boolean
     */
    public boolean checkAuthorizationHeaders() {
        if (getRequestURI().startsWith(VitamConfiguration.ADMIN_PATH) ||
            getRequestURI().endsWith(VitamConfiguration.STATUS_URL)) {
            return true;
        }
        if (!Strings.isNullOrEmpty(VitamConfiguration.getSecret())) {
            final Enumeration<String> headerNames = getHeaderNames();
            final Map<String, String> headersValues = new HashMap<>();
            if (headerNames != null) {

                headersValues.put(GlobalDataRest.X_PLATFORM_ID, getHeader(GlobalDataRest.X_PLATFORM_ID));
                headersValues.put(GlobalDataRest.X_TIMESTAMP, getHeader(GlobalDataRest.X_TIMESTAMP));

                return checkHeadersValues(headersValues);
            }
        }

        return false;

    }

    /**
     * @param headersValues
     * @return
     */
    private boolean checkHeadersValues(Map<String, String> headersValues) {
        ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, headersValues);
        final String platformId = headersValues.get(GlobalDataRest.X_PLATFORM_ID);
        final String timestamp = headersValues.get(GlobalDataRest.X_TIMESTAMP);
        if (Strings.isNullOrEmpty(platformId) || Strings.isNullOrEmpty(timestamp)) {
            return false;
        } else {
            return checkTimestamp(timestamp) && checkPlatformId(platformId, timestamp);
        }
    }

    /**
     * @param timestamp
     * @return True if timestamp is conformed
     */
    private boolean checkTimestamp(String timestamp) {
        ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, timestamp);
        final long currentEpoch = System.currentTimeMillis() / 1000;
        final long requestEpoch = Long.parseLong(timestamp);
        if (Math.abs(currentEpoch - requestEpoch) <= VitamConfiguration.getAcceptableRequestTime()) {
            return true;
        }

        LOGGER.error("Timestamp check failed");
        return false;
    }

    /**
     * @param platformId
     * @param timestamp
     * @return True if platformId is conformed
     */
    private boolean checkPlatformId(String platformId, String timestamp) {
        ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, platformId, timestamp);
        String uri = getRequestURI();
        try {
            uri = URLDecoder.decode(uri, CharsetUtils.UTF_8);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("UnsupportedEncodingException ", e);
            return false;
        }
        final String httpMethod = getMethod();
        final String code = URLCodec.encodeURL(httpMethod, uri, timestamp, VitamConfiguration.getSecret(),
            VitamConfiguration.getSecurityDigestType());
        if (code.equals(platformId)) {
            return true;
        }

        LOGGER.error("PlatformId check failed");
        return false;
    }

}
