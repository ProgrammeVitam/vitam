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
package fr.gouv.vitam.storage.engine.server.rest.writeprotection;

import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.resources.AdminStatusResource;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

/**
 * Scanner that prevents Write APIs to be invoked on secondary site.
 * All resource methods must be annotated with the {@link WriteProtection} annotation.
 */
@PreMatching
@Priority(Priorities.HEADER_DECORATOR)
public class WriteProtectionScanner implements DynamicFeature {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WriteProtectionScanner.class);

    private final AlertService alertService;
    private final boolean isReadOnly;

    public WriteProtectionScanner(AlertService alertService, boolean isReadOnly) {
        this.alertService = alertService;
        this.isReadOnly = isReadOnly;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        // Scanning method
        LOGGER.debug(
            "Scanning resource method " +
            resourceInfo.getResourceClass().getName() +
            " . " +
            resourceInfo.getResourceMethod().getName()
        );

        if (
            resourceInfo.getResourceMethod().getDeclaringClass() == ApplicationStatusResource.class ||
            resourceInfo.getResourceMethod().getDeclaringClass() == AdminStatusResource.class
        ) {
            LOGGER.debug(
                "Ignore write protection for " +
                resourceInfo.getResourceClass().getName() +
                "." +
                resourceInfo.getResourceMethod().getName()
            );
            return;
        }

        WriteProtection writeProtection = resourceInfo.getResourceMethod().getAnnotation(WriteProtection.class);

        // Ensure all resources have WriteProtection annotation
        if (writeProtection == null) {
            throw new IllegalStateException(
                "Missing @" +
                WriteProtection.class.getName() +
                " annotation for method " +
                resourceInfo.getResourceClass().getName() +
                " . " +
                resourceInfo.getResourceMethod().getName()
            );
        }

        if (!writeProtection.value()) {
            LOGGER.debug(
                "Operation does not require write protection  " +
                resourceInfo.getResourceClass().getName() +
                "." +
                resourceInfo.getResourceMethod().getName()
            );
            return;
        }

        if (!isReadOnly) {
            LOGGER.debug(
                "No write protection required since Write operations are allowed for method " +
                resourceInfo.getResourceClass().getName() +
                " . " +
                resourceInfo.getResourceMethod().getName()
            );
            return;
        }

        LOGGER.debug(
            String.format(
                "Filter API calls to method %s.%s",
                resourceInfo.getResourceClass().getName(),
                resourceInfo.getResourceMethod().getName()
            )
        );
        context.register(new WriteProtectionForbiddenFilter(alertService), Priorities.AUTHORIZATION);
    }
}
