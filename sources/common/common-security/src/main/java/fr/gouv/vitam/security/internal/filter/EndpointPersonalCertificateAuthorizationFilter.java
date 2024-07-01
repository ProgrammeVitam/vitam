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
package fr.gouv.vitam.security.internal.filter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.security.internal.client.InternalSecurityClient;
import fr.gouv.vitam.security.internal.client.InternalSecurityClientFactory;
import fr.gouv.vitam.security.internal.common.exception.InternalSecurityException;
import fr.gouv.vitam.security.internal.common.exception.PersonalCertificateException;
import fr.gouv.vitam.security.internal.common.model.IsPersonalCertificateRequiredModel;
import fr.gouv.vitam.security.internal.common.service.ParsedCertificate;
import fr.gouv.vitam.security.internal.exception.VitamSecurityException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Handles personal certificate access authorization for REST endpoints.
 */
public class EndpointPersonalCertificateAuthorizationFilter implements ContainerRequestFilter {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        EndpointPersonalCertificateAuthorizationFilter.class
    );

    private final String permission;

    private InternalSecurityClient internalSecurityClient;

    /**
     * Constructor with permission to filter
     *
     * @param permission the permission to filter
     */
    public EndpointPersonalCertificateAuthorizationFilter(String permission) {
        this.permission = permission;
        this.internalSecurityClient = InternalSecurityClientFactory.getInstance().getClient();
    }

    /**
     * Contructor for tests
     *
     * @param permission
     * @param internalSecurityClient
     */
    @VisibleForTesting
    public EndpointPersonalCertificateAuthorizationFilter(
        String permission,
        InternalSecurityClient internalSecurityClient
    ) {
        this.permission = permission;
        this.internalSecurityClient = internalSecurityClient;
    }

    /**
     * Checks authorization filter based of the current security profile permission set.
     *
     * @param requestContext the invocation context
     * @throws IOException
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            byte[] certificate = extractPersonalCertificate(requestContext);

            if (certificate != null) {
                VitamThreadUtils.getVitamSession()
                    .setPersonalCertificate(ParsedCertificate.parseCertificate(certificate).getCertificateHash());
            }

            boolean isPersonalCertificateRequired = getIsPersonalCertificateRequired();

            if (!isPersonalCertificateRequired) {
                LOGGER.debug("Personal certificate check skipped for permission {0}", permission);
                return;
            }
            LOGGER.debug("Personal certificate check required for permission {0}", permission);
            internalSecurityClient.checkPersonalCertificate(certificate, permission);
        } catch (
            InternalSecurityException
            | VitamClientInternalException
            | VitamSecurityException
            | PersonalCertificateException e
        ) {
            LOGGER.error("An error occured during authorization filter check", e);
            final VitamError vitamError = generateVitamError(e);

            requestContext.abortWith(
                Response.status(vitamError.getHttpCode())
                    .entity(vitamError)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build()
            );
            return;
        }
    }

    private byte[] extractPersonalCertificate(ContainerRequestContext requestContext) {
        String base64Certificate = requestContext.getHeaderString(GlobalDataRest.X_PERSONAL_CERTIFICATE);
        return base64Certificate != null ? BaseXx.getFromBase64(base64Certificate) : null;
    }

    private boolean getIsPersonalCertificateRequired() throws VitamClientInternalException, InternalSecurityException {
        // A cache may be used (later) for this call
        IsPersonalCertificateRequiredModel isPersonalCertificateRequired =
            internalSecurityClient.isPersonalCertificateRequiredByPermission(permission);

        switch (isPersonalCertificateRequired.getResponse()) {
            case REQUIRED_PERSONAL_CERTIFICATE:
                return true;
            case IGNORED_PERSONAL_CERTIFICATE:
                return false;
            case ERROR_UNKNOWN_PERMISSION:
                throw new IllegalStateException(
                    "Unknown permission " +
                    permission +
                    ". Please add permission in security-internal personal certificate permission configuration."
                );
            default:
                throw new IllegalStateException("Unexpected response " + isPersonalCertificateRequired);
        }
    }

    /**
     * Generate Vitam Error
     *
     * @param e
     * @return
     */
    private VitamError generateVitamError(Exception e) {
        final VitamError vitamError = new VitamError(VitamCodeHelper.getCode(VitamCode.INTERNAL_SECURITY_UNAUTHORIZED));

        String description = e.getMessage();
        if (Strings.isNullOrEmpty(description)) {
            description = StringUtils.getClassName(e);
        }
        vitamError
            .setContext(ServerIdentity.getInstance().getJsonIdentity())
            .setMessage(VitamCode.INTERNAL_SECURITY_UNAUTHORIZED.getMessage())
            .setDescription(description)
            .setState(VitamCode.INTERNAL_SECURITY_UNAUTHORIZED.name())
            .setHttpCode(VitamCode.INTERNAL_SECURITY_UNAUTHORIZED.getStatus().getStatusCode());
        return vitamError;
    }

    public String getPermission() {
        return permission;
    }
}
