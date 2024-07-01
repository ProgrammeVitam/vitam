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

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.rest.Secured;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

/**
 * Handles permission based access authorization for REST endpoints.
 * <p>
 * Registers for each {@link Secured}-annotated endpoint with authorization filter.
 */
@PreMatching
@Priority(Priorities.HEADER_DECORATOR + 40) // must go after InternalSecurityFilter
public class AuthorizationFilter implements DynamicFeature {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AuthorizationFilter.class);

    /**
     * Registers endpoint authorization filters for each @Secured endpoint resource.
     * Invoked for each endpoint resource.
     *
     * @param resourceInfo
     * @param context
     */
    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        // Retrieve @Secure annotation
        Secured securedAnnotation = resourceInfo.getResourceMethod().getAnnotation(Secured.class);
        if (securedAnnotation == null) {
            LOGGER.debug(
                String.format(
                    "Ignoring Non-@%s annotated method %s.%s",
                    Secured.class.getName(),
                    resourceInfo.getResourceClass().getName(),
                    resourceInfo.getResourceMethod().getName()
                )
            );
            return;
        }

        LOGGER.debug(
            String.format(
                "Registering authorization filters with '%s' permission for %s annotated method %s.%s",
                securedAnnotation.permission(),
                Secured.class.getName(),
                resourceInfo.getResourceClass().getName(),
                resourceInfo.getResourceMethod().getName()
            )
        );

        // Must go after ExternalHeaderIdContainerFilter
        context.register(
            new EndpointAdminOnlyAuthorizationFilter(securedAnnotation.isAdminOnly()),
            Priorities.AUTHORIZATION + 10
        );
        // Must go after EndpointPermissionAuthorizationFilter
        context.register(
            new EndpointPermissionAuthorizationFilter(securedAnnotation.permission().getPermission()),
            Priorities.AUTHORIZATION + 20
        );
        // Must go after EndpointPermissionAuthorizationFilter
        context.register(
            new EndpointPersonalCertificateAuthorizationFilter(securedAnnotation.permission().getPermission()),
            Priorities.AUTHORIZATION + 30
        );
    }
}
