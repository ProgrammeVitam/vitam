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

package fr.gouv.vitam.collect.internal.rest;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.collect.common.dto.ObjectDto;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.common.exception.CollectRequestResponse;
import fr.gouv.vitam.collect.internal.core.common.CollectUnitModel;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.collect.internal.core.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.core.service.CollectService;
import fr.gouv.vitam.collect.internal.core.service.MetadataService;
import fr.gouv.vitam.collect.internal.core.service.TransactionService;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/collect-internal/v1")
public class CollectMetadataInternalResource extends ApplicationStatusResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CollectMetadataInternalResource.class);

    private final MetadataService metadataService;
    private final CollectService collectService;
    private final TransactionService transactionService;

    public CollectMetadataInternalResource(MetadataService metadataService, CollectService collectService, TransactionService transactionService) {
        this.metadataService = metadataService;
        this.collectService = collectService;
        this.transactionService = transactionService;
    }

    @Path("/units/{unitId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitById(@PathParam("unitId") String unitId) {
        try {
            JsonNode response = metadataService.selectUnitById(unitId);
            return CollectRequestResponse.toResponseOK(response);
        } catch (CollectInternalException e) {
            LOGGER.error("Error when fetching unit in metadata : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException e) {
            LOGGER.error("Error when fetching unit in metadata : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }


    @Path("/units/{unitId}/objects/{usage}/{version}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadObjectGroup(@PathParam("unitId") String unitId, @PathParam("usage") String usageString,
        @PathParam("version") Integer version, @Valid ObjectDto objectDto) {
        try {
            SanityChecker.checkParameter(unitId);
            SanityChecker.checkParameter(usageString);
            ParametersChecker.checkParameter("You must supply object datas!", objectDto);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(objectDto));

            DataObjectVersionType usage = CollectHelper.fetchUsage(usageString);
            ParametersChecker.checkParameter("usage({}), unitId({}) or version({}) can't be null", unitId, usage,
                version);

            CollectUnitModel archiveUnitModel = collectService.getArchiveUnitModel(unitId);
            ObjectDto savedObjectDto =
                collectService.updateOrSaveObjectGroup(archiveUnitModel, usage, version, objectDto);

            return CollectRequestResponse.toResponseOK(savedObjectDto);
        } catch (CollectInternalException e) {
            LOGGER.error("Error while trying to save objects : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Error while trying to save objects : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/objects/{gotId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectById(@PathParam("gotId") String gotId) {

        try {
            SanityChecker.checkParameter(gotId);
            JsonNode objectGroup = metadataService.selectObjectGroupById(gotId);
            return CollectRequestResponse.toResponseOK(objectGroup);
        } catch (CollectInternalException e) {
            LOGGER.error("Error when fetching object in metadata : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Error when fetching object in metadata : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/units/{unitId}/objects/{usage}/{version}/binary")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response upload(@PathParam("unitId") String unitId, @PathParam("usage") String usageString,
        @PathParam("version") Integer version, InputStream uploadedInputStream) throws CollectInternalException {
        try {
            SanityChecker.checkParameter(unitId);
            SanityChecker.checkParameter(usageString);
            SanityChecker.checkParameter(String.valueOf(version.intValue()));
            ParametersChecker.checkParameter("You must supply a file!", uploadedInputStream);

            DataObjectVersionType usage = CollectHelper.fetchUsage(usageString);
            ParametersChecker.checkParameter("usage({}), unitId({}) or version({}) can't be null", unitId, usage,
                version);

            CollectUnitModel archiveUnitModel = collectService.getArchiveUnitModel(unitId);


            if (archiveUnitModel == null){
                throw new CollectInternalException("UA not found");
            }
            if (archiveUnitModel.getOpi() != null && !archiveUnitModel.getOpi().isBlank()) {
                TransactionModel uaTransaction =
                    transactionService.findTransaction(archiveUnitModel.getOpi()).orElse(null);
                if (uaTransaction == null){
                    throw new CollectInternalException("Transaction Id not found");
                } else {
                    if(!uaTransaction.getStatus().equals(TransactionStatus.OPEN))
                        throw new CollectInternalException("Invalid transaction status");
                }
            } else {
                throw new CollectInternalException("Operation Id not found");
            }

            DbObjectGroupModel dbObjectGroupModel = collectService.getDbObjectGroup(archiveUnitModel);
            collectService.addBinaryInfoToQualifier(dbObjectGroupModel, usage, version, uploadedInputStream);

            return Response.status(OK).build();
        } catch (CollectInternalException e) {
            // TODO : Manage rollback -> delete file ?
            LOGGER.debug("An error occurs when try to fetch data from database : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("An error occurs when try to fetch data from database : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }

    }

    @Path("/units/{unitId}/objects/{usage}/{version}/binary")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response download(@PathParam("unitId") String unitId, @PathParam("usage") String usageString,
        @PathParam("version") Integer version) {
        try {
            SanityChecker.checkParameter(unitId);
            SanityChecker.checkParameter(usageString);
            SanityChecker.checkParameter(String.valueOf(version.intValue()));

            DataObjectVersionType usage = CollectHelper.fetchUsage(usageString);
            ParametersChecker.checkParameter("usage({}), unitId({}) or version({}) can't be null", unitId, usage,
                version);

            CollectUnitModel archiveUnitModel = collectService.getArchiveUnitModel(unitId);
            collectService.getDbObjectGroup(archiveUnitModel);
            return collectService.getBinaryByUsageAndVersion(archiveUnitModel, usage, version);
        } catch (CollectInternalException e) {
            LOGGER.debug("An error occurs when try to fetch binary from database : {}", e);
            return Response.status(INTERNAL_SERVER_ERROR).build();
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("An error occurs when try to fetch binary from database : {}", e);
            return Response.status(INTERNAL_SERVER_ERROR).build();
        } catch (StorageNotFoundException e) {
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }
}
