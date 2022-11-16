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

package fr.gouv.vitam.collect.internal.resource;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.collect.external.dto.ObjectDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.helpers.CollectRequestResponse;
import fr.gouv.vitam.collect.internal.model.CollectUnitModel;
import fr.gouv.vitam.collect.internal.service.CollectService;
import fr.gouv.vitam.collect.internal.service.MetadataService;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.security.rest.Secured;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_BINARY_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_BINARY_UPSERT;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_OBJECT_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_OBJECT_UPSERT;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_UNIT_ID_READ;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/collect-external/v1")
public class CollectMetadataResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CollectMetadataResource.class);

    private final MetadataService metadataService;
    private final CollectService collectService;

    public CollectMetadataResource(MetadataService metadataService, CollectService collectService) {
        this.metadataService = metadataService;
        this.collectService = collectService;
    }

    @Path("/units/{unitId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_UNIT_ID_READ, description = "Récupére une unité archivistique")
    public Response getUnitById(@PathParam("unitId") String unitId) {
        try {
            SanityChecker.checkParameter(unitId);
            final RequestResponseOK<JsonNode> response =
                RequestResponseOK.getFromJsonNode(metadataService.selectUnitById(unitId));
            return response.toResponse();
        } catch (CollectException e) {
            LOGGER.error("Error when fetching unit in metadata : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Error when fetching unit in metadata : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }


    @Path("/units/{unitId}/objects/{usage}/{version}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_OBJECT_UPSERT, description = "Crée ou met à jour un groupe d'objets")
    public Response uploadObjectGroup(@PathParam("unitId") String unitId, @PathParam("usage") String usageString,
        @PathParam("version") Integer version, ObjectDto objectDto) {
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
        } catch (CollectException e) {
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
    @Secured(permission = TRANSACTION_OBJECT_READ, description = "Récupére un groupe d'objet")
    public Response getObjectById(@PathParam("gotId") String gotId) {

        try {
            SanityChecker.checkParameter(gotId);
            final RequestResponseOK<JsonNode> response =
                RequestResponseOK.getFromJsonNode(metadataService.selectObjectGroupById(gotId, true));
            return response.toResponse();
        } catch (CollectException e) {
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
    @Secured(permission = TRANSACTION_BINARY_UPSERT, description = "Crée ou met à jour un binaire d'un usage/version")
    public Response upload(@PathParam("unitId") String unitId, @PathParam("usage") String usageString,
        @PathParam("version") Integer version, InputStream uploadedInputStream) throws CollectException {
        try {
            SanityChecker.checkParameter(unitId);
            SanityChecker.checkParameter(usageString);
            SanityChecker.checkParameter(String.valueOf(version.intValue()));
            ParametersChecker.checkParameter("You must supply a file!", uploadedInputStream);

            DataObjectVersionType usage = CollectHelper.fetchUsage(usageString);
            ParametersChecker.checkParameter("usage({}), unitId({}) or version({}) can't be null", unitId, usage,
                version);

            CollectUnitModel archiveUnitModel = collectService.getArchiveUnitModel(unitId);
            DbObjectGroupModel dbObjectGroupModel = collectService.getDbObjectGroup(archiveUnitModel);
            collectService.addBinaryInfoToQualifier(dbObjectGroupModel, usage, version, uploadedInputStream);

            return Response.status(OK).build();
        } catch (CollectException e) {
            // TODO : Manage rollback -> delete file ?
            LOGGER.debug("An error occurs when try to fetch data from database : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("An error occurs when try to fetch data from database : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }

    }

    @Path("/units/{unitId}/objects/{usage}/{version}/binary")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = TRANSACTION_BINARY_READ, description = "Télécharge un usage/version du binaire d'un groupe d'objets")
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
        } catch (CollectException e) {
            LOGGER.debug("An error occurs when try to fetch binary from database : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("An error occurs when try to fetch binary from database : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (StorageNotFoundException e) {
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

}
