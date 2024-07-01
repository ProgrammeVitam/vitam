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
import fr.gouv.vitam.collect.common.dto.CriteriaProjectDto;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
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
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.security.rest.Secured;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_CREATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_ID_DELETE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_ID_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_ID_TRANSACTIONS;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_QUERY_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_UPDATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_CREATE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.fromStatusCode;

@Path("/collect-external/v1/projects")
@Tag(name = "Collect")
public class ProjectExternalResource extends ApplicationStatusResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProjectExternalResource.class);

    private static final String YOU_MUST_SUPPLY_PROJECTS_DATA = "You must supply projects data!";

    private static final String PREDICATES_FAILED_EXCEPTION = "Predicates Failed Exception ";

    private static final String PROJECT_NOT_FOUND = "Unable to find project Id or invalid status";

    private final CollectInternalClientFactory collectInternalClientFactory;

    ProjectExternalResource() {
        this(CollectInternalClientFactory.getInstance());
    }

    @VisibleForTesting
    ProjectExternalResource(CollectInternalClientFactory collectInternalClientFactory) {
        this.collectInternalClientFactory = collectInternalClientFactory;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_READ, description = "Récupère la liste des projets par tenant")
    public Response getProjects() {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            RequestResponse<JsonNode> response = client.getProjects();
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when fetching projects  ", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_QUERY_READ, description = "Récupérer une liste des projets par query")
    public Response searchProject(CriteriaProjectDto criteriaProjectDto) {
        final JsonNode criteriaProjectJsonNode;

        try {
            criteriaProjectJsonNode = JsonHandler.toJsonNode(criteriaProjectDto);
        } catch (InvalidParseOperationException e) {
            final String message = "An error occurred while converting the DTO to a JsonNode";
            LOGGER.error(message);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, message);
        }

        try {
            SanityChecker.checkJsonAll(criteriaProjectJsonNode);
        } catch (InvalidParseOperationException e) {
            final String message = e.getLocalizedMessage();
            LOGGER.error("Input seems contains insane data: {}", message);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, message);
        }

        ParametersChecker.checkParameter("You must supply criteria of Project!", criteriaProjectDto);

        final RequestResponseOK<JsonNode> listProjectsResponse;
        try (final CollectInternalClient client = collectInternalClientFactory.getClient()) {
            listProjectsResponse = client.searchProject(criteriaProjectDto);
        } catch (VitamClientException e) {
            final String message = e.getLocalizedMessage();
            LOGGER.error("Client error : {}", message);
            return CollectRequestResponse.toVitamError(
                fromStatusCode(e.getVitamError().getStatus()),
                e.getVitamError().getMessage()
            );
        }

        return Response.status(Response.Status.OK).entity(listProjectsResponse).build();
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_CREATE, description = "Créer un projet avec une transaction")
    public Response initProject(ProjectDto projectDto) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            ParametersChecker.checkParameter(YOU_MUST_SUPPLY_PROJECTS_DATA, projectDto);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(projectDto));
            RequestResponse<JsonNode> response = client.initProject(projectDto);
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when init project  ", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }
    }

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_UPDATE, description = "Mise à jour d'un projet")
    public Response updateProject(ProjectDto projectDto) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            ParametersChecker.checkParameter(YOU_MUST_SUPPLY_PROJECTS_DATA, projectDto);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(projectDto));
            RequestResponse<JsonNode> response = client.updateProject(projectDto);
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when updating projects  ", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }
    }

    @Path("/{projectId}")
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_ID_READ, description = "Récupère un projet par son id")
    public Response getProjectById(@PathParam("projectId") String projectId) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(projectId);
            RequestResponse<JsonNode> projectResponse = client.getProjectById(projectId);
            return Response.status(Response.Status.OK).entity(projectResponse).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when fetching project   ", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, PROJECT_NOT_FOUND);
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }
    }

    @Path("/{projectId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_ID_DELETE, description = "Supprime un projet par son id")
    public Response deleteProjectById(@PathParam("projectId") String projectId) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(projectId);
            client.deleteProjectById(projectId);
            return Response.status(Response.Status.OK).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when deleting project   ", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }
    }

    /* Not exposed by the client , we keep Code for future usage
    @Path("/{projectId}/units")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_ID_UNITS, description = "Récupère toutes les unités archivistique d'un projet")
    @Deprecated */
    public Response getUnitsByProjectId(@PathParam("projectId") String projectId, JsonNode queryDsl) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(projectId);
            SanityChecker.checkJsonAll(queryDsl);
            RequestResponse<JsonNode> projectResponse = client.getUnitsByProjectId(projectId, queryDsl);
            return Response.status(Response.Status.OK).entity(projectResponse).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when get units by project  ", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }
    }

    @Path("/{projectId}/transactions")
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_ID_TRANSACTIONS, description = "Récupérer la liste des transactions du projet")
    public Response getAllTransactions(@PathParam("projectId") String projectId) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(projectId);
            RequestResponse<JsonNode> transactionsResponse = client.getTransactionByProjectId(projectId);
            return Response.status(Response.Status.OK).entity(transactionsResponse).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when get transactions by project  ", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }
    }

    @Path("/{projectId}/transactions")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_CREATE, description = "Crée une transaction")
    public Response initTransaction(TransactionDto transactionDto, @PathParam("projectId") String projectId) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            ParametersChecker.checkParameter("You must supply transaction data !", transactionDto);
            SanityChecker.checkParameter(projectId);
            RequestResponse<JsonNode> transactionsResponse = client.initTransaction(transactionDto, projectId);
            return Response.status(Response.Status.OK).entity(transactionsResponse).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when init transactions by project  ", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }
    }
}
