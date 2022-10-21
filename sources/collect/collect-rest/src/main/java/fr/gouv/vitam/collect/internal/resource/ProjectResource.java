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
import fr.gouv.vitam.collect.external.dto.CriteriaProjectDto;
import fr.gouv.vitam.collect.external.dto.ProjectDto;
import fr.gouv.vitam.collect.external.dto.TransactionDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.helpers.CollectRequestResponse;
import fr.gouv.vitam.collect.internal.model.TransactionModel;
import fr.gouv.vitam.collect.internal.model.TransactionStatus;
import fr.gouv.vitam.collect.internal.service.FluxService;
import fr.gouv.vitam.collect.internal.service.MetadataService;
import fr.gouv.vitam.collect.internal.service.ProjectService;
import fr.gouv.vitam.collect.internal.service.TransactionService;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.security.rest.Secured;

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
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static fr.gouv.vitam.common.error.VitamCode.GLOBAL_EMPTY_QUERY;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_CREATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_ID_BINARY;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_ID_DELETE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_ID_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_ID_UNITS;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_QUERY_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_UPDATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_CREATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_READ;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/collect-external/v1/projects")
public class ProjectResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProjectResource.class);

    private static final String YOU_MUST_SUPPLY_PROJECTS_DATAS = "You must supply projects datas!";

    private static final String PROJECT_NOT_FOUND = "Unable to find project Id or invalid status";

    private static final String TRANSACTION_NOT_FOUND = "Unable to find transaction Id or invalid status";

    private static final String ERROR_GETTING_UNITS_BY_PROJECT_ID_MSG =
        "Error when getting units by project ID in metadata : {}";

    private static final String EMPTY_QUERY_IS_IMPOSSIBLE = "Empty query is impossible";

    private final ProjectService projectService;

    private final TransactionService transactionService;

    private final MetadataService metadataService;

    private final FluxService fluxService;

    public ProjectResource(ProjectService projectService, TransactionService transactionService,
        MetadataService metadataService, FluxService fluxService) {
        this.projectService = projectService;
        this.transactionService = transactionService;
        this.metadataService = metadataService;
        this.fluxService = fluxService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_READ, description = "Récupére la liste des projets par tenant")
    public Response getProjects() {
        try {
            List<ProjectDto> listProjects = projectService.findProjects();
            return CollectRequestResponse.toResponseOK(listProjects);
        } catch (IllegalArgumentException | CollectException e) {
            LOGGER.error("Error when fetching projects by tenant Id : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_QUERY_READ, description = "Récupérer une liste des projets par query")
    public Response searchProject(CriteriaProjectDto criteriaProjectDto) {
        try {
                ParametersChecker.checkParameter("You must supply criteria of Project!", criteriaProjectDto);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(criteriaProjectDto));
            List<ProjectDto> listProjects = projectService.searchProject(criteriaProjectDto.getQuery());
            return CollectRequestResponse.toResponseOK(listProjects);
        } catch (InvalidParseOperationException | CollectException e) {
            LOGGER.error("Error when trying to parse : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_CREATE, description = "Créer un projet avec une transaction")
    public Response initProject(ProjectDto projectDto) {
        try {
            ParametersChecker.checkParameter(YOU_MUST_SUPPLY_PROJECTS_DATAS, projectDto);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(projectDto));
            Integer tenantId = ParameterHelper.getTenantParameter();
            projectDto.setId(GUIDFactory.newGUID().getId());
            projectDto.setTenant(tenantId);
            projectService.createProject(projectDto);
            String transactionId = GUIDFactory.newRequestIdGUID(tenantId).getId();
            // FIXME : add another api to create a transaction
            transactionService.createTransactionFromProjectDto(projectDto, transactionId);
            projectDto.setTransactionId(transactionId);
            return CollectRequestResponse.toResponseOK(projectDto);
        } catch (CollectException e) {
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error when trying to parse : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_UPDATE, description = "Mise à jour d'un projet")
    public Response updateProject(ProjectDto projectDto) {
        try {
            ParametersChecker.checkParameter(YOU_MUST_SUPPLY_PROJECTS_DATAS, projectDto);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(projectDto));
            Optional<ProjectDto> projectOpt = projectService.findProject(projectDto.getId());
            if (projectOpt.isEmpty()) {
                LOGGER.error(PROJECT_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, PROJECT_NOT_FOUND);
            }
            Integer tenantId = ParameterHelper.getTenantParameter();
            projectDto.setTenant(tenantId);
            projectService.updateProject(projectDto);

            return CollectRequestResponse.toResponseOK(projectDto);
        } catch (InvalidParseOperationException | CollectException e) {
            LOGGER.error("Error when trying to parse : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }


    @Path("/{projectId}")
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_ID_READ, description = "Récupére un projet par son id")
    public Response getProjectById(@PathParam("projectId") String projectId) {
        try {
            SanityChecker.checkParameter(projectId);
            Optional<ProjectDto> projectDtoOptional = projectService.findProject(projectId);

            if (projectDtoOptional.isEmpty()) {
                LOGGER.error(PROJECT_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, PROJECT_NOT_FOUND);
            }

            Optional<TransactionModel> transactionModel = transactionService.findLastTransactionByProjectId(projectId);

            if (transactionModel.isEmpty()) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, TRANSACTION_NOT_FOUND);
            }
            ProjectDto projectDto = projectDtoOptional.get();
            projectDto.setTransactionId(transactionModel.get().getId());


            return CollectRequestResponse.toResponseOK(projectDto);
        } catch (CollectException e) {
            LOGGER.error("Error when fetching project by Id : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Error when fetching project by Id : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/{projectId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_ID_DELETE, description = "Supprime un projet par son id")
    public Response deleteProjectById(@PathParam("projectId") String projectId) {
        try {
            SanityChecker.checkParameter(projectId);
            Optional<ProjectDto> projectDto = projectService.findProject(projectId);

            if (projectDto.isEmpty()) {
                LOGGER.error(PROJECT_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, PROJECT_NOT_FOUND);
            }

            Optional<TransactionModel> transactionModel = transactionService.findLastTransactionByProjectId(projectId);

            if (transactionModel.isPresent()) {
                transactionService.deleteTransaction(transactionModel.get().getId());
            }
            projectService.deleteProjectById(projectId);

            return Response.status(Response.Status.OK).build();
        } catch (CollectException e) {
            LOGGER.error("Error when delete project by Id : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Error when delete project by Id : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/{projectId}/units")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_ID_UNITS, description = "Récupére toutes les unités archivistique d'un projet")
    @Deprecated
    public Response getUnitsByProjectId(@PathParam("projectId") String projectId, JsonNode queryDsl) {

        try {

            SanityChecker.checkParameter(projectId);
            SanityChecker.checkJsonAll(queryDsl);
            checkEmptyQuery(queryDsl);

            final Optional<TransactionModel> transaction = transactionService.findLastTransactionByProjectId(projectId);

            if (transaction.isEmpty()) {
                throw new CollectException("Could not find transaction");
            }

            JsonNode response = metadataService.selectUnits(queryDsl, transaction.get().getId());
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (CollectException e) {
            LOGGER.error(ERROR_GETTING_UNITS_BY_PROJECT_ID_MSG, e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(ERROR_GETTING_UNITS_BY_PROJECT_ID_MSG, e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (BadRequestException e) {
            LOGGER.error(EMPTY_QUERY_IS_IMPOSSIBLE, e);
            return CollectRequestResponse.toVitamError(GLOBAL_EMPTY_QUERY.getStatus(), e.getLocalizedMessage());
        }
    }

    @Path("/{projectId}/transactions")
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_READ, description = "Récupérer la dernière transaction du projet")
    public Response getAllTransactions(@PathParam("projectId") String projectId) {
        try {
            SanityChecker.checkParameter(projectId);
            final List<TransactionModel> results = transactionService.findTransactionsByProjectId(projectId);
            return CollectRequestResponse.toResponseOK(results);
        } catch (CollectException e) {
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error when trying to parse : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/{projectId}/transactions")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_CREATE, description = "Crée une transaction")
    public Response initTransaction(TransactionDto transactionDto, @PathParam("projectId") String projectId) {
        try {
            ParametersChecker.checkParameter("You must supply transaction datas!", transactionDto);
            SanityChecker.checkParameter(projectId);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(transactionDto));
            Integer tenantId = ParameterHelper.getTenantParameter();
            String requestId = GUIDFactory.newRequestIdGUID(tenantId).getId();
            transactionDto.setId(requestId);
            transactionDto.setTenant(tenantId);
            transactionService.createTransaction(transactionDto, projectId);
            return CollectRequestResponse.toResponseOK(transactionDto);
        } catch (CollectException e) {
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error when trying to parse : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }


    @Path("/{projectId}/binary")
    @POST
    @Consumes({CommonMediaType.ZIP})
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_ID_BINARY, description = "Charge les binaires d'un projet")
    public Response uploadProjectZip(@PathParam("projectId") String projectId, InputStream inputStreamObject) {
        try {
            ParametersChecker.checkParameter("You must supply a file!", inputStreamObject);
            Optional<ProjectDto> projectDtoOptional = projectService.findProject(projectId);

            if (projectDtoOptional.isEmpty()) {
                LOGGER.error(PROJECT_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, PROJECT_NOT_FOUND);
            }
            ProjectDto projectDto = projectDtoOptional.get();

            Optional<TransactionModel> transactionModel =
                transactionService.findLastTransactionByProjectId(projectDto.getId());

            if (transactionModel.isEmpty() ||
                !transactionService.checkStatus(transactionModel.get(), TransactionStatus.OPEN)) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, TRANSACTION_NOT_FOUND);
            }
            projectDto.setTransactionId(transactionModel.get().getId());

            fluxService.processStream(inputStreamObject, projectDto);

            return Response.status(OK).build();
        } catch (CollectException e) {
            LOGGER.debug("An error occurs when try to upload the ZIP: {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException e) {
            LOGGER.error("An error occurs when try to upload the ZIP: {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    private void checkEmptyQuery(JsonNode queryDsl) throws InvalidParseOperationException, BadRequestException {
        final SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(queryDsl.deepCopy());
        if (parser.getRequest().getNbQueries() == 0 && parser.getRequest().getRoots().isEmpty()) {
            throw new BadRequestException("Query cant be empty");
        }
    }
}
