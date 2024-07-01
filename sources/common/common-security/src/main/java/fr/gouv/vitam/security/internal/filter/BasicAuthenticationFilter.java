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
import fr.gouv.vitam.common.security.rest.VitamAuthentication;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

/**
 * Handles REST endpoints's access based on the basic authentication.
 * Registers for each {@link VitamAuthentication}. <br/>
 */
@PreMatching
@Priority(Priorities.HEADER_DECORATOR + 50)
public class BasicAuthenticationFilter implements DynamicFeature {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BasicAuthenticationFilter.class);

    /**
     * VitamAdmin configuration.
     */
    private DefaultVitamApplicationConfiguration configuration;

    /**
     * contructor with configuration.
     *
     * @param configuration
     */
    public BasicAuthenticationFilter(DefaultVitamApplicationConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Register endpoint authentication filters for @VitamAuthentication endpoint resource. <br/>
     *
     * @param resourceInfo
     * @param featureContext
     */
    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
        // retrieve @VitamAuthentication annotation.
        VitamAuthentication authentication = resourceInfo.getResourceMethod().getAnnotation(VitamAuthentication.class);
        if (authentication == null) {
            LOGGER.debug(
                String.format(
                    "Ignore @%s non annotated method %s for the class %s",
                    VitamAuthentication.class.getName(),
                    resourceInfo.getResourceMethod().getName(),
                    resourceInfo.getResourceClass().getName()
                )
            );
            return;
        }

        LOGGER.debug(
            String.format(
                "Registering VitamAuthentication filters with '%s' authentication level for %s annotated method %s.%s",
                authentication.authentLevel(),
                VitamAuthentication.class.getName(),
                resourceInfo.getResourceClass().getName(),
                resourceInfo.getResourceMethod().getName()
            )
        );

        // register the authentication filter.
        featureContext.register(
            new EndpointAuthenticationFilter(authentication.authentLevel(), configuration),
            Priorities.AUTHORIZATION + 10
        );
    }
}
