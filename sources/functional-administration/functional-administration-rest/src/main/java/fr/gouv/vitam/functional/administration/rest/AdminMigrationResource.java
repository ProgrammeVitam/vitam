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
package fr.gouv.vitam.functional.administration.rest;

/*
 * AdminMigrationResource class
 *
 */


import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuthenticationLevel;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.rest.VitamAuthentication;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static fr.gouv.vitam.common.VitamConfiguration.getTenants;

/**
 * @deprecated use only for migration
 * migrationResource class
 */
@Path("/adminmanagement/v1")
@ApplicationPath("webresources")

public class AdminMigrationResource {
    private HashMap<Integer, String> xrequestIds;
    private AdminDataMigrationResource adminDataMigrationResource;
    private ProcessingManagementClientFactory processingManagementClientFactory;
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminMigrationResource.class);

    /**
     * @param adminDataMigrationResource adminDataMigrationResource
     */
    AdminMigrationResource(AdminDataMigrationResource adminDataMigrationResource) {
        this(adminDataMigrationResource, ProcessingManagementClientFactory.getInstance());
    }

    /**
     * AdminMigrationResource
     *
     * @param adminDataMigrationResource        adminDataMigrationResource
     * @param processingManagementClientFactory processingManagementClientFactory
     */
    @VisibleForTesting
    public AdminMigrationResource(AdminDataMigrationResource adminDataMigrationResource,
        ProcessingManagementClientFactory processingManagementClientFactory) {

        this.adminDataMigrationResource = adminDataMigrationResource;
        this.processingManagementClientFactory = processingManagementClientFactory;
        xrequestIds = new HashMap<>();

    }

    /**
     * Migration Api
     *
     * @param headers headers
     * @return Response
     */

    @POST
    @Path("/startMigration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response start(@Context HttpHeaders headers) {

        xrequestIds.clear();

        getTenants().forEach(integer -> xrequestIds.put(integer, GUIDFactory.newGUID().getId()));
        
        for (Map.Entry<Integer, String> entry : xrequestIds.entrySet()) {
            VitamThreadUtils.getVitamSession().setRequestId(entry.getValue());
            VitamThreadUtils.getVitamSession().setTenantId(entry.getKey());
            adminDataMigrationResource.migrateTo(entry.getKey());
        }

        return Response.status(Response.Status.ACCEPTED).build();
    }

    /**
     * Check migration status
     *
     * @return Response
     */
    @HEAD
    @Path("/migrationStatus")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response check() {
        ProcessingManagementClient processingManagementClient =
            processingManagementClientFactory.getClient();
        if (xrequestIds.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        for (Map.Entry<Integer, String> entry : xrequestIds.entrySet()) {
            VitamThreadUtils.getVitamSession().setTenantId(entry.getKey());
            try {
                ItemStatus operationProcessStatus =
                    processingManagementClient.getOperationProcessStatus(entry.getValue());
                // if one process is on STARTED status return accepted

                boolean isProcessFinished = operationProcessStatus.getGlobalState().equals(ProcessState.COMPLETED);

                // When FATAL occurs, the process state will be set to PAUSE and status to FATAL => To be treated manually
                boolean isProcessPauseFatal = operationProcessStatus.getGlobalState().equals(ProcessState.PAUSE) && StatusCode.FATAL.equals(operationProcessStatus.getGlobalStatus());

                if (!isProcessFinished && !isProcessPauseFatal) {
                    // At least one workflow is in progress
                    return Response.status(Response.Status.ACCEPTED).build();
                }
            } catch (WorkflowNotFoundException e) {
                LOGGER.warn("Could not find process '" + entry.getValue() + "'. Cleaned Process ?", e);
            } catch (VitamException e) {
                LOGGER.error("Could not check process status " + entry.getValue(), e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
            }

        }

        return Response.status(Response.Status.OK).build();
    }
}
