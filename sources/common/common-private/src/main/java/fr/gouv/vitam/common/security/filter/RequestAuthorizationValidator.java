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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.codec.URLCodec;

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Authorization Wrapper
 */
public class RequestAuthorizationValidator {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(RequestAuthorizationValidator.class);

    private final ThrottlingAlertService warningTimestampNotificationHandler;
    private final ThrottlingAlertService criticalTimestampNotificationHandler;

    public RequestAuthorizationValidator() {
        this(new AlertServiceImpl());
    }

    @VisibleForTesting
    RequestAuthorizationValidator(AlertService alertService) {
        this.warningTimestampNotificationHandler = new ThrottlingAlertService(
            alertService,
            "Timestamp check failed. Please ensure NTP service is properly configured on all Vitam servers",
            VitamConfiguration.getRequestTimeAlertThrottlingDelay()
        );
        this.criticalTimestampNotificationHandler = new ThrottlingAlertService(
            alertService,
            "Critical Timestamp check failed. Please ensure NTP service is properly configured on all Vitam servers",
            VitamConfiguration.getRequestTimeAlertThrottlingDelay()
        );
    }

    /**
     * check Headers X-Platform-Id and X-Timestamp
     *
     * @return boolean
     */
    public boolean checkAuthorizationHeaders(HttpServletRequest request) {
        // FIXME : getRequestURI() may contain extra content (query string & co) that may bypass authorization checks
        if (
            request.getRequestURI().startsWith(VitamConfiguration.ADMIN_PATH) ||
            request.getRequestURI().endsWith(VitamConfiguration.STATUS_URL)
        ) {
            return true;
        }
        return checkHeadersValues(request);
    }

    private boolean checkHeadersValues(HttpServletRequest request) {
        final String platformId = request.getHeader(GlobalDataRest.X_PLATFORM_ID);
        final String timestamp = request.getHeader(GlobalDataRest.X_TIMESTAMP);

        if (Strings.isNullOrEmpty(platformId) || Strings.isNullOrEmpty(timestamp)) {
            LOGGER.error(
                String.format(
                    "Illegal request. Missing %s and/or %s headers",
                    GlobalDataRest.X_PLATFORM_ID,
                    GlobalDataRest.X_TIMESTAMP
                )
            );
            return false;
        }

        return checkTimestamp(timestamp) && checkPlatformId(request, platformId, timestamp);
    }

    /**
     * @param timestamp
     * @return True if timestamp is conformed
     */
    private boolean checkTimestamp(String timestamp) {
        final long currentEpoch = LocalDateUtil.currentTimeMillis() / 1000;
        final long requestEpoch = Long.parseLong(timestamp);
        long timeShift = Math.abs(currentEpoch - requestEpoch);

        if (timeShift <= VitamConfiguration.getAcceptableRequestTime()) {
            return true;
        }

        if (timeShift <= VitamConfiguration.getCriticalRequestTime()) {
            LOGGER.error("Timestamp check failed. " + timeShift + "s");
            this.warningTimestampNotificationHandler.reportAlert();
            return true;
        }

        LOGGER.error("Critical timestamp check failure. " + timeShift + "s");
        this.criticalTimestampNotificationHandler.reportAlert();
        return false;
    }

    /**
     * @param platformId
     * @param timestamp
     * @return True if platformId is conformed
     */
    private boolean checkPlatformId(HttpServletRequest request, String platformId, String timestamp) {
        String uri = request.getRequestURI();
        uri = URLDecoder.decode(uri, StandardCharsets.UTF_8);
        final String httpMethod = request.getMethod();
        final String code = URLCodec.encodeURL(
            httpMethod,
            uri,
            timestamp,
            VitamConfiguration.getSecret(),
            VitamConfiguration.getSecurityDigestType()
        );
        if (code.equals(platformId)) {
            return true;
        }

        LOGGER.error("PlatformId check failed");
        return false;
    }
}
