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
package fr.gouv.vitam.common.server.application.resources;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AdminStatusMessage;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * AdminStatusResource : Manage Admin Functionality through Admin URI
 */
@Path(VitamConfiguration.ADMIN_PATH)
@Consumes("application/json")
@Produces("application/json")
public class AdminStatusResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminStatusResource.class);
    /**
     * Status for Administration resource path
     */
    public static final String STATUS_URL = VitamConfiguration.STATUS_URL;
    /**
     * Autotest for Administration resource path
     */
    public static final String AUTOTEST_URL = VitamConfiguration.AUTOTEST_URL;
    public static final String VERSION_URL = VitamConfiguration.VERSION_URL;
    public static final String METRIC_URL = VitamConfiguration.METRIC_URL;
    private final VitamStatusService statusService;
    private final VitamServiceRegistry autotestService;

    /**
     * Constructor AdminStatusResource using implicit BasicVitamStatusServiceImpl
     */
    public AdminStatusResource() {
        this(new BasicVitamStatusServiceImpl());
    }

    /**
     * Constructor AdminStatusResource
     *
     * @param statusService
     */
    public AdminStatusResource(VitamStatusService statusService) {
        this(statusService, new VitamServiceRegistry().register(statusService));
    }

    /**
     * Constructor AdminStatusResource
     *
     * @param statusService
     * @param autotestService
     */
    public AdminStatusResource(VitamStatusService statusService, VitamServiceRegistry autotestService) {
        this.statusService = statusService;
        this.autotestService = autotestService;
    }

    /**
     * Constructor AdminStatusResource
     *
     * @param autotestService
     */
    public AdminStatusResource(VitamServiceRegistry autotestService) {
        this(new BasicVitamStatusServiceImpl(), autotestService);
    }

    /**
     * Return a response status
     *
     * @return Response containing the status of the service in AdminStatusMessage form
     */
    @Path(STATUS_URL)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response adminStatus() {
        try {
            final AdminStatusMessage message = new AdminStatusMessage(
                JsonHandler.toJsonNode(ServerIdentity.getInstance()),
                statusService.getResourcesStatus(),
                statusService.getAdminStatus(),
                JsonHandler.toJsonNode(VersionHelper.getVersionSummary())
            );
            if (message.getStatus()) {
                return Response.ok(message, MediaType.APPLICATION_JSON).build();
            } else {
                return Response.status(Status.SERVICE_UNAVAILABLE).entity(message).build();
            }
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.SERVICE_UNAVAILABLE).build();
        }
    }

    /**
     * Return a response version infos (for all nodes)
     *
     * @return Response containing the status of the service in AdminStatusMessage form
     */
    @Path(VERSION_URL)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response adminVersion() {
        try {
            return Response.ok(
                JsonHandler.toJsonNode(VersionHelper.getVersionDetailedInfo()),
                MediaType.APPLICATION_JSON
            ).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.SERVICE_UNAVAILABLE).build();
        }
    }

    /**
     * @return the full status including dependencies in VitamError form
     */
    @Path(AUTOTEST_URL)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response adminAutotest() {
        ObjectNode status;
        status = autotestService.getAutotestStatus();
        return Response.status(status.get("httpCode").asInt()).entity(status).build();
    }

    @Path(METRIC_URL)
    @GET
    @Produces(TextFormat.CONTENT_TYPE_004)
    public Response prometheusMetrics() {
        return Response.ok()
            .type(TextFormat.CONTENT_TYPE_004)
            .entity(
                (StreamingOutput) output -> {
                    try (final Writer writer = new OutputStreamWriter(output)) {
                        TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
                    }
                }
            )
            .build();
    }
}
