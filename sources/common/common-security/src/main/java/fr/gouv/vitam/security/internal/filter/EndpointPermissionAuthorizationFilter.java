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
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.security.internal.exception.VitamSecurityException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Handles permission based access authorization for REST endpoints.
 */
public class EndpointPermissionAuthorizationFilter implements ContainerRequestFilter {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        EndpointPermissionAuthorizationFilter.class
    );

    private final String permission;

    private AdminManagementClient adminManagementClient;

    /**
     * Constructor with permission to filter
     *
     * @param permission the permission to filter
     */
    public EndpointPermissionAuthorizationFilter(String permission) {
        this.permission = permission;
        this.adminManagementClient = AdminManagementClientFactory.getInstance().getClient();
    }

    /**
     * Contructor for tests
     *
     * @param permission
     * @param adminManagementClient
     */
    @VisibleForTesting
    public EndpointPermissionAuthorizationFilter(String permission, AdminManagementClient adminManagementClient) {
        this.permission = permission;
        this.adminManagementClient = adminManagementClient;
    }

    /**
     * Checks authorization filter based of the current security profile permission set.
     *
     * @param requestContext the invocation context
     * @throws IOException
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String securityProfileIdentifier = VitamThreadUtils.getVitamSession().getSecurityProfileIdentifier();

        try {
            // Get security profile by identifier stored in VitamSession
            RequestResponse<SecurityProfileModel> securityProfileResponse =
                adminManagementClient.findSecurityProfileByIdentifier(securityProfileIdentifier);

            SecurityProfileModel securityProfile;
            if (securityProfileResponse.isOk()) {
                securityProfile = ((RequestResponseOK<SecurityProfileModel>) securityProfileResponse).getFirstResult();
            } else {
                LOGGER.error("Could not retrieve security profile by identifier " + securityProfileIdentifier);
                VitamError vitamError = (VitamError) securityProfileResponse;
                requestContext.abortWith(
                    Response.status(vitamError.getHttpCode())
                        .entity(vitamError)
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .build()
                );
                return;
            }

            // Check for full access security profile
            if (securityProfile.getFullAccess()) {
                LOGGER.debug("Full access granted.");
                return;
            }

            // Check for matching security profile permission
            if (securityProfile.getPermissions() != null && securityProfile.getPermissions().contains(permission)) {
                LOGGER.debug("Access granted with permission " + permission);
                return;
            }

            LOGGER.warn("Access denied for permission " + permission);
            throw new VitamSecurityException("Access denied.");
        } catch (
            InvalidParseOperationException
            | AdminManagementClientServerException
            | ReferentialNotFoundException
            | VitamSecurityException e
        ) {
            LOGGER.error("An error occured during authorization filter check", e);
            final VitamError vitamError = generateVitamError(e, permission);

            requestContext.abortWith(
                Response.status(vitamError.getHttpCode())
                    .entity(vitamError)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build()
            );
            return;
        }
    }

    /**
     * Generate Vitam Error
     *
     * @param e
     * @param permission
     * @return
     */
    private VitamError generateVitamError(Exception e, String permission) {
        final VitamError vitamError;

        String description = e.getMessage();
        if (Strings.isNullOrEmpty(description)) {
            description = StringUtils.getClassName(e);
        }

        if (!org.apache.commons.lang3.StringUtils.isBlank(permission) && permission.equals("units:update")) {
            vitamError = new VitamError(
                VitamCodeHelper.getCode(VitamCode.INTERNAL_SECURITY_MASS_UPDATE_AUTHORIZATION_REJECTED)
            );

            vitamError
                .setContext(ServerIdentity.getInstance().getJsonIdentity())
                .setMessage(VitamCode.INTERNAL_SECURITY_MASS_UPDATE_AUTHORIZATION_REJECTED.getMessage())
                .setDescription(description)
                .setState(VitamCode.INTERNAL_SECURITY_MASS_UPDATE_AUTHORIZATION_REJECTED.name())
                .setHttpCode(
                    VitamCode.INTERNAL_SECURITY_MASS_UPDATE_AUTHORIZATION_REJECTED.getStatus().getStatusCode()
                );
        } else {
            vitamError = new VitamError(VitamCodeHelper.getCode(VitamCode.INTERNAL_SECURITY_UNAUTHORIZED));

            vitamError
                .setContext(ServerIdentity.getInstance().getJsonIdentity())
                .setMessage(VitamCode.INTERNAL_SECURITY_UNAUTHORIZED.getMessage())
                .setDescription(description)
                .setState(VitamCode.INTERNAL_SECURITY_UNAUTHORIZED.name())
                .setHttpCode(VitamCode.INTERNAL_SECURITY_UNAUTHORIZED.getStatus().getStatusCode());
        }

        return vitamError;
    }

    public String getPermission() {
        return permission;
    }
}
