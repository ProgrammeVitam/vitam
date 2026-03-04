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
import fr.gouv.vitam.collect.common.dto.CriteriaProjectDto;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.common.exception.CollectInternalInvalidRequestException;
import fr.gouv.vitam.collect.common.exception.CollectInternalMultipleErrorsDetailsException;
import fr.gouv.vitam.collect.common.exception.CollectInternalSingleErrorsDetailException;
import fr.gouv.vitam.collect.common.exception.CollectRequestResponse;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.collect.internal.core.service.FluxService;
import fr.gouv.vitam.collect.internal.core.service.MetadataService;
import fr.gouv.vitam.collect.internal.core.service.ProjectService;
import fr.gouv.vitam.collect.internal.core.service.TransactionService;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static fr.gouv.vitam.common.error.VitamCode.GLOBAL_EMPTY_QUERY;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

@Path("/collect-internal/v1/projects")
public class ProjectInternalResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProjectInternalResource.class);

    private static final String YOU_MUST_SUPPLY_PROJECTS_DATAS = "You must supply projects datas!";

    public static final String PROJECT_NOT_FOUND = "Unable to find project Id or invalid status";

    private static final String ERROR_GETTING_UNITS_BY_PROJECT_ID_MSG =
        "Error when getting units by project ID in metadata : {}";

    private static final String EMPTY_QUERY_IS_IMPOSSIBLE = "Empty query is impossible";
    public static final String VIRTUAL_TX = "VIRTUAL_TX_";

    private final ProjectService projectService;

    private final FluxService fluxService;

    private final TransactionService transactionService;

    private final MetadataService metadataService;

    public ProjectInternalResource(
        ProjectService projectService,
        FluxService fluxService,
        TransactionService transactionService,
        MetadataService metadataService
    ) {
        this.projectService = projectService;
        this.fluxService = fluxService;
        this.transactionService = transactionService;
        this.metadataService = metadataService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProjects() {
        try {
            final List<ProjectDto> listProjects = projectService.searchProject(null);
            return CollectRequestResponse.toResponseOK(listProjects);
        } catch (IllegalArgumentException | CollectInternalException e) {
            LOGGER.error("Error when fetching projects by tenant Id :", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchProject(CriteriaProjectDto criteriaProjectDto) {
        try {
            ParametersChecker.checkParameter("You must supply criteria of Project!", criteriaProjectDto);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(criteriaProjectDto));
            final List<ProjectDto> listProjects = projectService.searchProject(criteriaProjectDto);
            return CollectRequestResponse.toResponseOK(listProjects);
        } catch (InvalidParseOperationException | CollectInternalException | IllegalArgumentException e) {
            LOGGER.error("Error when trying to parse :", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response initProject(ProjectDto projectDto) {
        try {
            ParametersChecker.checkParameter(YOU_MUST_SUPPLY_PROJECTS_DATAS, projectDto);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(projectDto));
            ProjectDto createdProject = projectService.createProject(projectDto);
            return CollectRequestResponse.toResponseOK(createdProject);
        } catch (CollectInternalException e) {
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Error when trying to parse :", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProject(ProjectDto projectDto) {
        try {
            ParametersChecker.checkParameter(YOU_MUST_SUPPLY_PROJECTS_DATAS, projectDto);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(projectDto));
            Optional<ProjectDto> projectOpt = projectService.findProject(projectDto.getId());
            if (projectOpt.isEmpty()) {
                LOGGER.error(PROJECT_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, PROJECT_NOT_FOUND);
            }
            ProjectDto updatedProject = projectService.updateProject(projectDto, projectOpt.get());

            return CollectRequestResponse.toResponseOK(updatedProject);
        } catch (InvalidParseOperationException | CollectInternalException | IllegalArgumentException e) {
            LOGGER.error("Error when trying to parse :", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/{projectId}")
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProjectById(@PathParam("projectId") String projectId) {
        try {
            SanityChecker.checkParameter(projectId);
            Optional<ProjectDto> projectOpt = projectService.findProject(projectId);

            if (projectOpt.isEmpty()) {
                LOGGER.error(PROJECT_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, PROJECT_NOT_FOUND);
            }
            return CollectRequestResponse.toResponseOK(projectOpt.get());
        } catch (CollectInternalException e) {
            LOGGER.error("Error when fetching project by Id :", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Error when fetching project by Id :", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/{projectId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteProjectById(@PathParam("projectId") String projectId) {
        try {
            SanityChecker.checkParameter(projectId);
            Optional<ProjectDto> projectOpt = projectService.findProject(projectId);

            if (projectOpt.isEmpty()) {
                LOGGER.error(PROJECT_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, PROJECT_NOT_FOUND);
            }

            Optional<TransactionModel> transactionModel = transactionService.findLastTransactionByProjectId(projectId);

            if (transactionModel.isPresent()) {
                transactionService.deleteTransaction(transactionModel.get().getId());
            }
            projectService.deleteProjectById(projectId);

            return Response.status(Response.Status.OK).build();
        } catch (CollectInternalException e) {
            LOGGER.error("Error when delete project by Id :", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Error when delete project by Id :", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/{projectId}/units")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public Response getUnitsByProjectId(@PathParam("projectId") String projectId, JsonNode queryDsl) {
        try {
            SanityChecker.checkParameter(projectId);
            SanityChecker.checkJsonAll(queryDsl);
            checkEmptyQuery(queryDsl);

            final Optional<TransactionModel> transaction = transactionService.findLastTransactionByProjectId(projectId);

            if (transaction.isEmpty()) {
                throw new CollectInternalException("Could not find transaction");
            }

            List<JsonNode> units = metadataService
                .selectUnitsByTransactionId(queryDsl, transaction.get().getId())
                .getResults();
            return Response.status(Response.Status.OK)
                .entity(new RequestResponseOK<JsonNode>().addAllResults(units))
                .build();
        } catch (CollectInternalException e) {
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
    public Response getAllTransactions(@PathParam("projectId") String projectId) {
        try {
            SanityChecker.checkParameter(projectId);
            final List<TransactionDto> results = transactionService.findTransactionsByProjectId(projectId);
            return CollectRequestResponse.toResponseOK(results);
        } catch (CollectInternalException e) {
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Error when trying to parse :", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/{projectId}/transactions")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response initTransaction(TransactionDto transactionDto, @PathParam("projectId") String projectId) {
        try {
            ParametersChecker.checkParameter("You must supply transaction datas!", transactionDto);
            SanityChecker.checkParameter(projectId);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(transactionDto));
            Integer tenantId = ParameterHelper.getTenantParameter();
            String requestId = GUIDFactory.newRequestIdGUID(tenantId).getId();
            transactionDto.setId(requestId);
            transactionDto.setTenant(tenantId);
            Optional<ProjectDto> projectOpt = projectService.findProject(projectId);
            if (projectOpt.isEmpty()) {
                LOGGER.error(PROJECT_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, PROJECT_NOT_FOUND);
            }
            transactionService.createTransaction(transactionDto, projectOpt.get());
            return CollectRequestResponse.toResponseOK(transactionDto);
        } catch (CollectInternalException e) {
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Error when trying to parse :", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/{projectId}/upload")
    @POST
    @Consumes({ CommonMediaType.ZIP })
    @Produces(APPLICATION_JSON)
    public Response uploadZipToProject(
        @PathParam("projectId") String projectId,
        InputStream inputStreamObject,
        @HeaderParam(GlobalDataRest.X_ENCODING) @Nullable String encoding
    ) {
        try {
            ParametersChecker.checkParameter("You must supply a ZIP input stream body!", inputStreamObject);
            SanityChecker.checkParameter(projectId);
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("An error occurs when try to upload the ZIP:", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }

        try {
            Optional<ProjectDto> projectOpt = projectService.findProject(projectId);
            if (projectOpt.isEmpty()) {
                LOGGER.error(PROJECT_NOT_FOUND);
                return CollectRequestResponse.toVitamError(NOT_FOUND, PROJECT_NOT_FOUND);
            }
            String batchId = VitamThreadUtils.getVitamSession().getRequestId();
            // Use projectId to ensure the virtual transactionId is reused within the same project
            String virtualTransactionId = VIRTUAL_TX + projectId;

            fluxService.processStream(inputStreamObject, projectId, virtualTransactionId, encoding, null);

            fluxService.moveObjectsFromBatchToTransaction(batchId, virtualTransactionId);
            return Response.ok(new RequestResponseOK<>().addResult(virtualTransactionId)).build();
        } catch (CollectInternalSingleErrorsDetailException | CollectInternalMultipleErrorsDetailsException e) {
            LOGGER.error("An error occurs when try to upload the ZIP:", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage(), e.getErrorsDetailsList());
        } catch (CollectInternalInvalidRequestException | IllegalArgumentException e) {
            LOGGER.error("An error occurs when try to upload the ZIP:", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (CollectInternalException e) {
            LOGGER.error("An error occurs when try to upload the ZIP:", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
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
