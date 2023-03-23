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
package fr.gouv.vitam.collect.external.external.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.collect.common.dto.ObjectDto;
import fr.gouv.vitam.collect.common.exception.CollectRequestResponse;
import fr.gouv.vitam.collect.internal.client.CollectInternalClient;
import fr.gouv.vitam.collect.internal.client.CollectInternalClientFactory;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.security.rest.Secured;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import io.swagger.v3.oas.annotations.tags.Tag;

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
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;


@Path("/collect-external/v1")
@Tag(name = "Collect-External")
public class CollectMetadataExternalResource extends ApplicationStatusResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CollectMetadataExternalResource.class);

    private static final String PREDICATES_FAILED_EXCEPTION = "Predicates Failed Exception ";
    public static final String ERROR_WHEN_CREATING_AN_OBJECT_GROUP_UNIT_BY_ID =
        "Error when creating an object group unit by id    ";
    public static final String ERROR_WHEN_FETCHING_OBJECT_BY_IF = "Error when fetching object by if    ";
    private final CollectInternalClientFactory collectInternalClientFactory;

    /**
     * Constructor CollectExternalResource
     */
    CollectMetadataExternalResource() {
        this(CollectInternalClientFactory.getInstance());
    }

    @VisibleForTesting
    CollectMetadataExternalResource(CollectInternalClientFactory collectInternalClientFactory) {
        this.collectInternalClientFactory = collectInternalClientFactory;
    }

    @Path("/units/{unitId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_UNIT_ID_READ, description = "Récupére une unité archivistique par ID")
    public Response getUnitById(@PathParam("unitId") String unitId) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(unitId);
            RequestResponse<JsonNode> response = client.getUnitById(unitId);
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when fetching unit by id    ", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(PRECONDITION_FAILED).build();
        }
    }


    @Path("/units/{unitId}/objects/{usage}/{version}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_OBJECT_UPSERT, description = "Upload un groupe d'objet")
    public Response createObjectGroup(@PathParam("unitId") String unitId, @PathParam("usage") String usageString,
        @PathParam("version") Integer version, ObjectDto objectDto) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(unitId);
            SanityChecker.checkParameter(usageString);
            ParametersChecker.checkParameter("You must supply object data !", objectDto);
            RequestResponse<JsonNode> response =
                client.addObjectGroup(unitId, version, JsonHandler.toJsonNode(objectDto), usageString);
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (final VitamClientException e) {
            LOGGER.error(ERROR_WHEN_CREATING_AN_OBJECT_GROUP_UNIT_BY_ID, e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(PRECONDITION_FAILED).build();
        }
    }

    @Path("/objects/{gotId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_OBJECT_READ, description = "Récupére un groupe d'objet")
    public Response getObjectById(@PathParam("gotId") String gotId) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(gotId);
            RequestResponse<JsonNode> response = client.getObjectById(gotId);
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (final VitamClientException e) {
            LOGGER.error(ERROR_WHEN_FETCHING_OBJECT_BY_IF, e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(PRECONDITION_FAILED).build();
        }
    }

    @Path("/units/{unitId}/objects/{usage}/{version}/binary")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_BINARY_UPSERT, description = "Crée ou met à jour un binaire d'un usage/version")
    public Response upload(@PathParam("unitId") String unitId, @PathParam("usage") String usageString,
        @PathParam("version") Integer version, InputStream uploadedInputStream) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(unitId);
            SanityChecker.checkParameter(usageString);
            SanityChecker.checkParameter(String.valueOf(version.intValue()));
            ParametersChecker.checkParameter("usage({}), unitId({}) or version({}) can't be null", unitId, usageString,
                version);
            ParametersChecker.checkParameter("You must supply a file!", uploadedInputStream);
            RequestResponse<JsonNode> requestResponse =
                client.addBinary(unitId, version, uploadedInputStream, usageString);
            return Response.status(Response.Status.OK).entity(requestResponse).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when adding binary    ", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(PRECONDITION_FAILED).build();
        }
    }

    @Path("/units/{unitId}/objects/{usage}/{version}/binary")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = TRANSACTION_BINARY_READ, description = "Télécharge un usage/version du binaire d'un groupe d'objets")
    public Response download(@PathParam("unitId") String unitId, @PathParam("usage") String usageString,
        @PathParam("version") Integer version) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(unitId);
            SanityChecker.checkParameter(usageString);
            SanityChecker.checkParameter(String.valueOf(version.intValue()));
            ParametersChecker.checkParameter("usage({}), unitId({}) or version({}) can't be null", unitId, usageString,
                version);
            return client.getObjectStreamByUnitId(unitId, usageString, version);
        } catch (final VitamClientException e) {
            LOGGER.error("Error when downloading object ", e);
            return Response.status(BAD_REQUEST).build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(PRECONDITION_FAILED).build();
        }
    }
    
}
