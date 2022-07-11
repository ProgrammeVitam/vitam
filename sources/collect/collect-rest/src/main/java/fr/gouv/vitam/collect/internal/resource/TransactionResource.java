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
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.collect.external.dto.IngestDto;
import fr.gouv.vitam.collect.external.dto.ObjectGroupDto;
import fr.gouv.vitam.collect.external.dto.ProjectDto;
import fr.gouv.vitam.collect.external.dto.TransactionDto;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.helpers.CollectRequestResponse;
import fr.gouv.vitam.collect.internal.model.ProjectModel;
import fr.gouv.vitam.collect.internal.model.TransactionModel;
import fr.gouv.vitam.collect.internal.model.TransactionStatus;
import fr.gouv.vitam.collect.internal.service.CollectService;
import fr.gouv.vitam.collect.internal.service.FluxService;
import fr.gouv.vitam.collect.internal.service.ProjectService;
import fr.gouv.vitam.collect.internal.service.SipService;
import fr.gouv.vitam.collect.internal.service.TransactionService;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.dsl.schema.Dsl;
import fr.gouv.vitam.common.dsl.schema.DslSchema;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.security.rest.EndpointInfo;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.security.rest.Secured;
import fr.gouv.vitam.common.security.rest.Unsecured;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
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
import java.util.stream.Collectors;

import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_CREATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_ID_BINARY;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_ID_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_ID_UNITS;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.PROJECT_UPDATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_BINARY_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_BINARY_UPSERT;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_CLOSE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_CREATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_ID_UNITS;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_OBJECT_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_OBJECT_UPSERT;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_SEND;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_UNIT_CREATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_UNIT_ID_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_UNIT_READ;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/collect-external/v1")
public class TransactionResource extends ApplicationStatusResource {
    public static final String ERROR_WHILE_TRYING_TO_SAVE_UNITS = "Error while trying to save units";
    public static final String SIP_INGEST_OPERATION_CAN_T_PROVIDE_A_NULL_OPERATION_GUIID =
        "SIP ingest operation can't provide a null operationGuiid";
    public static final String SIP_GENERATED_MANIFEST_CAN_T_BE_NULL = "SIP generated manifest can't be null";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransactionResource.class);
    private static final String TRANSACTION_NOT_FOUND = "Unable to find transaction Id or invalid status";
    private static final String PROJECT_NOT_FOUND = "Unable to find project Id or invalid status";
    private static final String OPI = "#opi";
    private static final String ID = "#id";
    private static final String ERROR_GETTING_UNITS_BY_PROJECT_ID_MSG =
        "Error when getting units by project ID in metadata : {}";
    private final SecureEndpointRegistry secureEndpointRegistry;
    private final TransactionService transactionService;
    private final ProjectService projectService;
    private final CollectService collectService;
    private final SipService sipService;
    private final FluxService fluxService;

    public TransactionResource(
        SecureEndpointRegistry secureEndpointRegistry,
        TransactionService transactionService, CollectService collectService,
        SipService sipService, ProjectService projectService, FluxService fluxService) {
        this.secureEndpointRegistry = secureEndpointRegistry;
        this.transactionService = transactionService;
        this.collectService = collectService;
        this.sipService = sipService;
        this.projectService = projectService;
        this.fluxService = fluxService;
    }

    /**
     * Récupère la liste des endpoints de la resource
     *
     * @return response
     */
    @Path("/")
    @OPTIONS
    @Produces(MediaType.APPLICATION_JSON)
    @Unsecured()
    public Response listResourceEndpoints() {
        String resourcePath = TransactionResource.class.getAnnotation(Path.class).value();
        List<EndpointInfo> securedEndpointList = this.secureEndpointRegistry.getEndPointsByResourcePath(resourcePath);
        return Response.status(Response.Status.OK).entity(securedEndpointList).build();
    }

    @Path("/projects")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_CREATE, description = "Créer un projet avec une transaction")
    public Response initProject(ProjectDto projectDto) {
        try {
            ParametersChecker.checkParameter("You must supply projects datas!", projectDto);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(projectDto));
            Integer tenantId = ParameterHelper.getTenantParameter();
            projectDto.setId(CollectService.createRequestId());
            projectDto.setTenant(tenantId);
            projectService.createProject(projectDto);
            String transactionId = CollectService.createRequestId();
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

    @Path("/projects")
    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_UPDATE, description = "Mise à jour d'un projet")
    public Response updateProject(ProjectDto projectDto) {
        try {
            ParametersChecker.checkParameter("You must supply projects datas!", projectDto);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(projectDto));
            Optional<ProjectModel> projectModel = projectService.findProject(projectDto.getId());
            if (projectModel.isEmpty()) {
                LOGGER.error(PROJECT_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, PROJECT_NOT_FOUND);
            }
            Integer tenantId = ParameterHelper.getTenantParameter();
            projectDto.setTenant(tenantId);
            projectService.replaceProject(projectDto);

            return CollectRequestResponse.toResponseOK(projectDto);
        } catch (CollectException e) {
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error when trying to parse : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/projects/{projectId}")
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_ID_READ, description = "Récupére un projet par son id")
    public Response getProjectById(@PathParam("projectId") String projectId) {
        try {
            SanityChecker.checkParameter(projectId);
            Optional<ProjectModel> projectModel = projectService.findProject(projectId);

            if (projectModel.isEmpty()) {
                LOGGER.error(PROJECT_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, PROJECT_NOT_FOUND);
            }

            Optional<TransactionModel> transactionModel = transactionService.findTransactionByProjectId(projectId);

            if (transactionModel.isEmpty()) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, TRANSACTION_NOT_FOUND);
            }
            ProjectDto projectDto = CollectHelper.convertProjectModeltoProjectDto(projectModel.get());
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


    @Path("/projects")
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_READ, description = "Récupére la liste des projets par tenant")
    public Response getProjects() {
        try {
            Integer tenantId = ParameterHelper.getTenantParameter();
            List<ProjectModel> listProjects = projectService.findProjectsByTenant(tenantId);
            List<ProjectDto> projectDtoList =
                listProjects.stream().map(CollectHelper::convertProjectModeltoProjectDto)
                    .collect(Collectors.toList());

            return CollectRequestResponse.toResponseOK(projectDtoList);
        } catch (CollectException e) {
            LOGGER.error("Error when fetching projects by tenant Id : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException e) {
            LOGGER.error("Error when fetching projects by tenant Id : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/transactions")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_CREATE, description = "Crée une transaction")
    public Response initTransaction(TransactionDto transactionDto) {
        try {
            ParametersChecker.checkParameter("You must supply transaction datas!", transactionDto);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(transactionDto));
            Integer tenantId = ParameterHelper.getTenantParameter();
            String requestId = CollectService.createRequestId();
            transactionDto.setId(requestId);
            transactionDto.setTenant(tenantId);
            transactionService.createTransaction(transactionDto);
            return CollectRequestResponse.toResponseOK(transactionDto);
        } catch (CollectException e) {
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error("Error when trying to parse : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/transactions/{transactionId}/units")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_UNIT_CREATE, description = "Crée une unité archivistique et la rattache à la transaction courante")
    public Response uploadArchiveUnit(@PathParam("transactionId") String transactionId, JsonNode unitJsonNode) {

        try {
            SanityChecker.checkParameter(transactionId);
            SanityChecker.checkJsonAll(unitJsonNode);

            Optional<TransactionModel> transactionModel = transactionService.findTransaction(transactionId);

            if (transactionModel.isEmpty() ||
                !transactionService.checkStatus(transactionModel.get(), TransactionStatus.OPEN)) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, TRANSACTION_NOT_FOUND);
            }

            ObjectNode unitObjectNode = JsonHandler.getFromJsonNode(unitJsonNode, ObjectNode.class);
            unitObjectNode.put(ID, CollectService.createRequestId());
            unitObjectNode.put(OPI, transactionId);
            JsonNode savedUnitJsonNode = collectService.saveArchiveUnitInMetaData(unitObjectNode);

            if (savedUnitJsonNode == null) {
                LOGGER.error(ERROR_WHILE_TRYING_TO_SAVE_UNITS);
                return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, ERROR_WHILE_TRYING_TO_SAVE_UNITS);
            }
            return CollectRequestResponse.toResponseOK(unitObjectNode);
        } catch (CollectException | InvalidParseOperationException e) {
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @Path("/units/{unitId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_UNIT_ID_READ, description = "Récupére une unité archivistique")
    public Response getUnitById(@PathParam("unitId") String unitId) {

        try {
            SanityChecker.checkParameter(unitId);
            JsonNode response = collectService.getUnitByIdInMetaData(unitId);
            return Response.status(OK).entity(response).build();
        } catch (CollectException e) {
            LOGGER.error("Error when fetching unit in metadata : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Error when fetching unit in metadata : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    /**
     * select Unit
     *
     * @param jsonQuery as String { $query : query}
     */
    @Path("/units")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_UNIT_READ, description = "Récupére toutes les unités archivistique")
    public Response selectUnits(@Dsl(value = DslSchema.SELECT_MULTIPLE) JsonNode jsonQuery) {
        try {
            return CollectRequestResponse.toResponseOK(collectService.selectUnits(jsonQuery));
        } catch (CollectException e) {
            LOGGER.error("Error when getting units in metadata : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @Path("/transactions/{transactionId}/units")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_ID_UNITS, description = "Récupére toutes les unités archivistique d'une transaction")
    public Response getUnitsByTransaction(@PathParam("transactionId") String transactionId) {

        try {
            SanityChecker.checkParameter(transactionId);
            JsonNode response = collectService.getUnitsByTransactionIdInMetaData(transactionId);
            return CollectRequestResponse.toResponseOK(response);
        } catch (CollectException e) {
            LOGGER.error("Error when getting units in metadata : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Error when getting units in metadata : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }


    @Path("/units/{unitId}/objects/{usage}/{version}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_OBJECT_UPSERT, description = "Crée ou met à jour un groupe d'objets")
    public Response uploadObjectGroup(@PathParam("unitId") String unitId,
        @PathParam("usage") String usageString,
        @PathParam("version") Integer version,
        ObjectGroupDto objectGroupDto) {
        try {
            SanityChecker.checkParameter(unitId);
            SanityChecker.checkParameter(usageString);
            ParametersChecker.checkParameter("You must supply object datas!", objectGroupDto);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(objectGroupDto));

            DataObjectVersionType usage = CollectHelper.fetchUsage(usageString);
            collectService.checkParameters(unitId, usage, version);
            ArchiveUnitModel archiveUnitModel = collectService.getArchiveUnitModel(unitId);
            ObjectGroupDto savedObjectGroupDto =
                collectService.saveObjectGroupInMetaData(archiveUnitModel, usage, version, objectGroupDto);

            return CollectRequestResponse.toResponseOK(savedObjectGroupDto);
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
            JsonNode response = collectService.getObjectGroupByIdInMetaData(gotId);
            return Response.status(OK).entity(response).build();
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
    public Response upload(@PathParam("unitId") String unitId,
        @PathParam("usage") String usageString,
        @PathParam("version") Integer version,
        InputStream uploadedInputStream) throws CollectException {
        try {
            SanityChecker.checkParameter(unitId);
            SanityChecker.checkParameter(usageString);
            SanityChecker.checkParameter(String.valueOf(version.intValue()));
            ParametersChecker.checkParameter("You must supply a file!", uploadedInputStream);

            DataObjectVersionType usage = CollectHelper.fetchUsage(usageString);
            collectService.checkParameters(unitId, usage, version);
            ArchiveUnitModel archiveUnitModel = collectService.getArchiveUnitModel(unitId);
            DbObjectGroupModel dbObjectGroupModel = collectService.getDbObjectGroup(archiveUnitModel);
            collectService.addBinaryInfoToQualifier(dbObjectGroupModel, usage, version, uploadedInputStream, null);

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
    public Response download(@PathParam("unitId") String unitId,
        @PathParam("usage") String usageString,
        @PathParam("version") Integer version) {
        try {
            SanityChecker.checkParameter(unitId);
            SanityChecker.checkParameter(usageString);
            SanityChecker.checkParameter(String.valueOf(version.intValue()));

            DataObjectVersionType usage = CollectHelper.fetchUsage(usageString);
            collectService.checkParameters(unitId, usage, version);
            ArchiveUnitModel archiveUnitModel = collectService.getArchiveUnitModel(unitId);
            collectService.getDbObjectGroup(archiveUnitModel);
            return collectService.getBinaryByUsageAndVersion(archiveUnitModel, usage, version);
        } catch (CollectException e) {
            LOGGER.debug("An error occurs when try to fetch binary from database : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("n error occurs when try to fetch binary from database : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (StorageNotFoundException e) {
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }

    }


    @Path("/transactions/{transactionId}/close")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_CLOSE, description = "Ferme une transaction")
    public Response closeTransaction(@PathParam("transactionId") String transactionId) {
        try {
            SanityChecker.checkParameter(transactionId);
            transactionService.closeTransaction(transactionId);
            return Response.status(OK).build();
        } catch (CollectException e) {
            LOGGER.error("An error occurs when try to close transaction : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (InvalidParseOperationException | IllegalArgumentException e) {
            LOGGER.error("An error occurs when try to close transaction : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/transactions/{transactionId}/send")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_SEND, description = "Envoi vers VITAM la transaction")
    public Response generateAndSendSip(@PathParam("transactionId") String transactionId) {

        try {
            SanityChecker.checkParameter(transactionId);

            Optional<TransactionModel> transactionModel = transactionService.findTransaction(transactionId);
            if (transactionModel.isEmpty() ||
                !transactionService.checkStatus(transactionModel.get(), TransactionStatus.CLOSE)) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, TRANSACTION_NOT_FOUND);
            }

            String digest = sipService.generateSip(transactionModel.get());
            if (digest == null) {
                LOGGER.error(SIP_GENERATED_MANIFEST_CAN_T_BE_NULL);
                return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, SIP_GENERATED_MANIFEST_CAN_T_BE_NULL);
            }

            final String operationGuiid = sipService.ingest(transactionModel.get(), digest);
            if (operationGuiid == null) {
                LOGGER.error(SIP_INGEST_OPERATION_CAN_T_PROVIDE_A_NULL_OPERATION_GUIID);
                return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR,
                    SIP_INGEST_OPERATION_CAN_T_PROVIDE_A_NULL_OPERATION_GUIID);
            }

            TransactionModel currentTransactionModel = transactionModel.get();
            currentTransactionModel.setStatus(TransactionStatus.SENT);
            transactionService.replaceTransaction(currentTransactionModel);

            return CollectRequestResponse.toResponseOK(new IngestDto(operationGuiid));
        } catch (CollectException e) {
            LOGGER.error("An error occurs when try to generate SIP : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error("An error occurs when try to generate SIP : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/projects/{projectId}/binary")
    @POST
    @Consumes({CommonMediaType.ZIP})
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_ID_BINARY, description = "Charge les binaires d'un projet")
    public Response uploadProjectZip(@PathParam("projectId") String projectId, InputStream inputStreamObject) {
        try {
            ParametersChecker.checkParameter("You must supply a file!", inputStreamObject);
            if (inputStreamObject == null) {
                LOGGER.error(ErrorMessage.STREAM_IS_NULL.getMessage());
                return Response.status(BAD_REQUEST).build();
            }
            Optional<ProjectModel> projectModel = projectService.findProject(projectId);

            if (projectModel.isEmpty()) {
                LOGGER.error(PROJECT_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, PROJECT_NOT_FOUND);
            }
            ProjectDto projectDto = CollectHelper.convertProjectModeltoProjectDto(projectModel.get());

            Optional<TransactionModel> transactionModel =
                transactionService.findTransactionByProjectId(projectDto.getId());

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

    @Path("/projects/{projectId}/units")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = PROJECT_ID_UNITS, description = "Récupére toutes les unités archivistique d'un projet")
    public Response getUnitsByProjectId(@PathParam("projectId") String projectId) {

        try {
            SanityChecker.checkParameter(projectId);
            JsonNode response = collectService.getUnitsByProjectId(projectId);
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (CollectException e) {
            LOGGER.error(ERROR_GETTING_UNITS_BY_PROJECT_ID_MSG, e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(ERROR_GETTING_UNITS_BY_PROJECT_ID_MSG, e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }
}
