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
package fr.gouv.vitam.common.security.filter;

import com.google.common.base.Strings;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.security.codec.URLCodec;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Authorization Filter Helper
 */
public class AuthorizationFilterHelper {

    private static final String ARGUMENT_MUST_NOT_BE_NULL = "Argument must not be null";

    private AuthorizationFilterHelper() {
        // Empty
    }

    /**
     * @param httpMethod
     * @param url
     * @return X-Platform-Id and X-Timestamp Headers
     */
    public static Map<String, String> getAuthorizationHeaders(String httpMethod, String url) {
        ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, httpMethod, url);
        if (!Strings.isNullOrEmpty(VitamConfiguration.getSecret())) {
            final long currentTime = LocalDateUtil.currentTimeMillis() / 1000;
            return getAuthorizationHeaders(
                httpMethod,
                url,
                Long.toString(currentTime),
                VitamConfiguration.getSecret(),
                VitamConfiguration.getSecurityDigestType()
            );
        }
        return Collections.emptyMap();
    }

    /**
     * @param httpMethod
     * @param url
     * @param timestamp
     * @param secret
     * @param digestType
     * @return X-Platform-Id and X-Timestamp Headers
     */
    public static Map<String, String> getAuthorizationHeaders(
        String httpMethod,
        String url,
        String timestamp,
        String secret,
        DigestType digestType
    ) {
        ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, httpMethod, url, timestamp, secret, digestType);

        final Map<String, String> headers = new HashMap<>();
        headers.put(GlobalDataRest.X_TIMESTAMP, timestamp);
        headers.put(GlobalDataRest.X_PLATFORM_ID, URLCodec.encodeURL(httpMethod, url, timestamp, secret, digestType));

        return headers;
    }
}
