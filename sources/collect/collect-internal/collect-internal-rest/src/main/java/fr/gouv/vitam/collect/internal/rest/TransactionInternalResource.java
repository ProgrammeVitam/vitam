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
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.common.exception.CollectRequestResponse;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.collect.internal.core.common.TransactionStatus;
import fr.gouv.vitam.collect.internal.core.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.core.service.FluxService;
import fr.gouv.vitam.collect.internal.core.service.MetadataService;
import fr.gouv.vitam.collect.internal.core.service.ProjectService;
import fr.gouv.vitam.collect.internal.core.service.SipService;
import fr.gouv.vitam.collect.internal.core.service.TransactionService;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.apache.commons.io.FileUtils;

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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

@Path("/collect-internal/v1/transactions")
public class TransactionInternalResource {
    public static final String ERROR_WHILE_TRYING_TO_SAVE_UNITS = "Error while trying to save units";
    public static final String SIP_INGEST_OPERATION_CAN_T_PROVIDE_A_NULL_OPERATION_GUID =
        "SIP ingest operation can't provide a null operationGuid";
    public static final String SIP_GENERATED_MANIFEST_CAN_T_BE_NULL = "SIP generated manifest can't be null";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransactionInternalResource.class);
    private static final String TRANSACTION_NOT_FOUND = "Unable to find transaction Id or invalid status";
    private static final String PROJECT_NOT_FOUND = "Unable to find project Id or invalid status";
    private static final String OPI = "#opi";
    private static final String ID = "#id";
    private static final String UNIT_TYPE = "#unitType";
    private static final String INGEST = "INGEST";

    private final TransactionService transactionService;
    private final MetadataService metadataService;
    private final SipService sipService;
    private final FluxService fluxService;
    private final ProjectService projectService;

    public TransactionInternalResource(TransactionService transactionService, SipService sipService,
        MetadataService metadataService, FluxService fluxService, ProjectService projectService) {
        this.transactionService = transactionService;
        this.sipService = sipService;
        this.metadataService = metadataService;
        this.fluxService = fluxService;
        this.projectService = projectService;
    }

    @Path("/{transactionId}")
    @GET
    @Produces(APPLICATION_JSON)
    public Response getTransactionById(@PathParam("transactionId") String transactionId) {
        try {
            SanityChecker.checkParameter(transactionId);

            Optional<TransactionModel> transactionModel = transactionService.findTransaction(transactionId);

            if (transactionModel.isEmpty()) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, TRANSACTION_NOT_FOUND);
            }

            TransactionDto transactionDto =
                CollectHelper.convertTransactionModelToTransactionDto(transactionModel.get());


            return CollectRequestResponse.toResponseOK(transactionDto);
        } catch (CollectInternalException e) {
            LOGGER.error("Error when get transaction by Id : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Error when get transaction by Id : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateTransaction(TransactionDto transactionDto) {
        try {
            ParametersChecker.checkParameter("You must supply transaction data!", transactionDto);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(transactionDto));
            Optional<TransactionModel> transactionModel = transactionService.findTransaction(transactionDto.getId());
            if (transactionModel.isEmpty()) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return CollectRequestResponse.toVitamError(NOT_FOUND, TRANSACTION_NOT_FOUND);
            }

            // TODO : Move setting internal Fields to the service
            Integer tenantId = ParameterHelper.getTenantParameter();
            transactionDto.setTenant(tenantId);
            transactionDto.setStatus(transactionModel.get().getStatus().name());
            transactionDto.setProjectId(transactionModel.get().getProjectId());
            transactionDto.setCreationDate(transactionModel.get().getCreationDate());
            transactionDto.setLastUpdate(LocalDateUtil.now().toString());
            transactionService.replaceTransaction(transactionDto);

            return CollectRequestResponse.toResponseOK(transactionDto);
        } catch (CollectInternalException e) {
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Error when trying to parse : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }


    @Path("/{transactionId}")
    @DELETE
    @Produces(APPLICATION_JSON)
    public Response deleteTransactionById(@PathParam("transactionId") String transactionId) {
        try {
            SanityChecker.checkParameter(transactionId);

            Optional<TransactionModel> transactionModel = transactionService.findTransaction(transactionId);

            if (transactionModel.isEmpty()) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, TRANSACTION_NOT_FOUND);
            }

            transactionService.deleteTransaction(transactionModel.get().getId());
            return Response.status(Response.Status.OK).build();
        } catch (CollectInternalException e) {
            LOGGER.error("Error when delete transaction by Id : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("Error when delete transaction by Id : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/units")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
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
            unitObjectNode.put(ID, GUIDFactory.newUnitGUID(VitamThreadUtils.getVitamSession().getTenantId()).getId());
            unitObjectNode.put(OPI, transactionId);
            unitObjectNode.put(UNIT_TYPE, INGEST);
            JsonNode savedUnitJsonNode = metadataService.saveArchiveUnit(unitObjectNode);

            if (savedUnitJsonNode == null) {
                LOGGER.error(ERROR_WHILE_TRYING_TO_SAVE_UNITS);
                return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, ERROR_WHILE_TRYING_TO_SAVE_UNITS);
            }
            return CollectRequestResponse.toResponseOK(unitObjectNode);
        } catch (CollectInternalException | InvalidParseOperationException e) {
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    /**
     * select Unit
     *
     * @param jsonQuery as String { $query : query}
     */
    @Path("/{transactionId}/units")
    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response selectUnits(@PathParam("transactionId") String transactionId, JsonNode jsonQuery) {
        try {
            final JsonNode results = metadataService.selectUnits(jsonQuery, transactionId);
            return Response.status(Response.Status.OK).entity(results).build();
        } catch (CollectInternalException e) {
            LOGGER.error("Error when getting units in metadata : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/close")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response closeTransaction(@PathParam("transactionId") String transactionId) {
        try {
            SanityChecker.checkParameter(transactionId);
            transactionService.closeTransaction(transactionId);
            return Response.status(OK).build();
        } catch (CollectInternalException e) {
            LOGGER.error("An error occurs when try to close transaction : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (InvalidParseOperationException | IllegalArgumentException e) {
            LOGGER.error("An error occurs when try to close transaction : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/abort")
    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response abortTransaction(@PathParam("transactionId") String transactionId) {
        try {
            SanityChecker.checkParameter(transactionId);
            transactionService.abortTransaction(transactionId);
            return Response.status(OK).build();
        } catch (CollectInternalException e) {
            LOGGER.error("An error occurs when try to close transaction : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (InvalidParseOperationException | IllegalArgumentException e) {
            LOGGER.error("An error occurs when try to close transaction : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/reopen")
    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response reopenTransaction(@PathParam("transactionId") String transactionId) {
        try {
            SanityChecker.checkParameter(transactionId);
            transactionService.reopenTransaction(transactionId);
            return Response.status(OK).build();
        } catch (CollectInternalException e) {
            LOGGER.error("An error occurs when try to close transaction : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (InvalidParseOperationException | IllegalArgumentException e) {
            LOGGER.error("An error occurs when try to close transaction : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/send")
    @POST
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response generateSip(@PathParam("transactionId") String transactionId) throws
        CollectInternalException {
        TransactionModel transaction = null;
        InputStream sipInputStream = null;
        try {
            SanityChecker.checkParameter(transactionId);
            Optional<TransactionModel> transactionModel = transactionService.findTransaction(transactionId);
            if (transactionModel.isEmpty() ||
                !transactionService.checkStatus(transactionModel.get(), TransactionStatus.READY)) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, TRANSACTION_NOT_FOUND);
            }
            transaction = transactionModel.get();
            transactionService.isTransactionContentEmpty(transaction.getId());
            transactionService.changeStatusTransaction(TransactionStatus.SENDING, transaction);
            String digest = sipService.generateSip(transaction);
            if (digest == null) {
                LOGGER.error(SIP_GENERATED_MANIFEST_CAN_T_BE_NULL);
                transactionService.changeStatusTransaction(TransactionStatus.KO, transaction);
                return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, SIP_GENERATED_MANIFEST_CAN_T_BE_NULL);
            }
            sipInputStream = sipService.getIngestedFileFromWorkspace(transaction);
            if (sipInputStream == null) {
                throw new CollectInternalException("Can't fetch SIP file from Collect workspace!");
            }
            return Response.ok(sipInputStream).build();
        } catch (CollectInternalException e) {
            LOGGER.error("An error occurs when try to generate SIP : {}", e);
            transactionService.changeStatusTransaction(TransactionStatus.KO, transaction);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("An error occurs when try to generate SIP : {}", e);
            transactionService.changeStatusTransaction(TransactionStatus.KO, transaction);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (Exception e) {
            if (sipInputStream != null) {
                StreamUtils.closeSilently(sipInputStream);
            }
            LOGGER.error("Error when ingesting  transaction   ", e);
            return CollectRequestResponse.toVitamError(Response.Status.INTERNAL_SERVER_ERROR,
                e.getLocalizedMessage());
        }
    }


    @Path("/{transactionId}/units")
    @PUT
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(APPLICATION_JSON)
    public Response updateUnits(@PathParam("transactionId") String transactionId, InputStream is) {
        try {
            ParametersChecker.checkParameter("DOCUMENT_IS_MANDATORY", is);
            SanityChecker.checkParameter(transactionId);

            Optional<TransactionModel> transactionModel = transactionService.findTransaction(transactionId);
            if (transactionModel.isEmpty() ||
                !transactionService.checkStatus(transactionModel.get(), TransactionStatus.OPEN)) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, TRANSACTION_NOT_FOUND);
            }
            TransactionModel transaction = transactionModel.get();

            Optional<ProjectDto> projectDto = projectService.findProject(transaction.getProjectId());
            if (projectDto.isEmpty()) {
                LOGGER.error(PROJECT_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, PROJECT_NOT_FOUND);
            }

            final String requestId = VitamThreadUtils.getVitamSession().getRequestId();
            File file = PropertiesUtils.fileFromTmpFolder(String.format("metadata_%s.csv", requestId));

            // Check Html Pattern
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                StreamUtils.copy(is, fileOutputStream);
                if (file.length() == 0) {
                    throw new IllegalArgumentException("Empty file");
                }
                SanityChecker.checkHTMLFile(file);


                try (InputStream sanityStream = new FileInputStream(file)) {
                    fluxService.updateUnits(transaction.getId(), sanityStream,
                        !Objects.isNull(projectDto.get().getUnitUp()));
                }
            } finally {
                FileUtils.deleteQuietly(file);
            }



            return Response.ok(new RequestResponseOK<>()).build();
        } catch (CollectInternalException | IOException e) {
            LOGGER.error("An error occurs when try to update metadata : {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error("An error occurs when try to update metadata : {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

    @Path("/{transactionId}/upload")
    @POST
    @Consumes({CommonMediaType.ZIP})
    public Response uploadTransactionZip(@PathParam("transactionId") String transactionId,
        InputStream inputStreamObject) {
        try {
            ParametersChecker.checkParameter("You must supply a file!", inputStreamObject);

            Optional<TransactionModel> transactionModel = transactionService.findTransaction(transactionId);


            if (transactionModel.isEmpty() ||
                !transactionService.checkStatus(transactionModel.get(), TransactionStatus.OPEN)) {
                LOGGER.error(TRANSACTION_NOT_FOUND);
                return CollectRequestResponse.toVitamError(NOT_FOUND, TRANSACTION_NOT_FOUND);
            }
            Optional<ProjectDto> projectDto = projectService.findProject(transactionModel.get().getProjectId());

            if (projectDto.isEmpty()) {
                LOGGER.error(PROJECT_NOT_FOUND);
                return CollectRequestResponse.toVitamError(BAD_REQUEST, PROJECT_NOT_FOUND);
            }
            fluxService.processStream(inputStreamObject,
                CollectHelper.convertTransactionModelToTransactionDto(transactionModel.get()), projectDto.get());

            return Response.ok().build();
        } catch (CollectInternalException e) {
            LOGGER.error("An error occurs when try to upload the ZIP: {}", e);
            return CollectRequestResponse.toVitamError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } catch (IllegalArgumentException e) {
            LOGGER.error("An error occurs when try to upload the ZIP: {}", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }

}
