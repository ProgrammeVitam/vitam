/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.functional.administration.rest;


import com.fasterxml.jackson.core.type.TypeReference;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.administration.GriffinModel;
import fr.gouv.vitam.common.model.administration.PreservationScenarioModel;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.griffin.GriffinService;
import fr.gouv.vitam.functional.administration.griffin.PreservationScenarioService;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

@Path("/adminmanagement/v1")
@ApplicationPath("webresources")
public class GriffinResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(GriffinResource.class);
    private final MongoDbAccessAdminImpl mongoAccess;

    private final VitamCounterService vitamCounterService;

    private final FunctionalBackupService functionalBackupService;

    private final PreservationScenarioService preservationScenarioService;
    private final GriffinService griffinService;

    GriffinResource(
        MongoDbAccessAdminImpl mongoAccess,
        VitamCounterService vitamCounterService,
        FunctionalBackupService functionalBackupService,
        PreservationScenarioService preservationScenarioService, GriffinService griffinService) {
        this.mongoAccess = mongoAccess;
        this.vitamCounterService = vitamCounterService;
        this.functionalBackupService = functionalBackupService;
        this.preservationScenarioService = preservationScenarioService;
        this.griffinService = griffinService;

    }

    GriffinResource(MongoDbAccessAdminImpl mongoAccess, VitamCounterService vitamCounterService,
        FunctionalBackupService functionalBackupService) {

        this(mongoAccess, vitamCounterService, functionalBackupService, new PreservationScenarioService(mongoAccess),
            new GriffinService(mongoAccess));
    }



    @POST
    @Path("/importGriffins")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importGriffin(List<GriffinModel> griffinModelList, @Context UriInfo uri) {
        try {

            griffinService.importGriffin(griffinModelList);

        } catch (ReferentialException e) {

            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage())).build();

        } catch (Exception e) {
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage())).build();
        }

        return Response.ok().build();
    }

    @POST
    @Path("/importPreservationScenarios")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importPreservationScenario(List<PreservationScenarioModel> preservationScenarioModel, @Context UriInfo uri) {

        try {
            preservationScenarioService.importScenarios(preservationScenarioModel);

        } catch (ReferentialException e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage())).build();

        } catch (Exception e) {
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage())).build();
        }
        return Response.ok().build();
    }

    private VitamError getErrorEntity(Status status, String message) {

        String statusName = (status.getReasonPhrase() != null) ? status.toString() : status.name();

        String aMessage =
            ((message != null) && !message.trim().isEmpty()) ? message : statusName;

        String aCode = String.valueOf(status.getStatusCode());

        return new VitamError(aCode).setHttpCode(status.getStatusCode()).setContext("ADMIN_MODULE")
            .setState("code_vitam").setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }


    private static void runInVitamThread(Runnable r) {


        Thread thread = VitamThreadFactory.getInstance().newThread(() -> {
            VitamThreadUtils.getVitamSession().setTenantId(1);
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID());
            try {
                r.run();
            } catch (Throwable e) {
                System.err.println(e);
            }
        });

        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
