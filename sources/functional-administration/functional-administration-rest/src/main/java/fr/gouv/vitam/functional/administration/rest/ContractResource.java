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
package fr.gouv.vitam.functional.administration.rest;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.core.contract.AccessContractImpl;
import fr.gouv.vitam.functional.administration.core.contract.ContractService;
import fr.gouv.vitam.functional.administration.core.contract.IngestContractImpl;
import fr.gouv.vitam.functional.administration.core.contract.ManagementContractImpl;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Path("/adminmanagement/v1")
@ApplicationPath("webresources")
@Tag(name = "Functional-Administration")
public class ContractResource {

    private static final String ADMIN_MODULE = "ADMIN_MODULE";
    static final String INGEST_CONTRACTS_URI = "/ingestcontracts";
    static final String ACCESS_CONTRACTS_URI = "/accesscontracts";
    static final String MANAGEMENT_CONTRACTS_URI = "/managementcontracts";
    static final String UPDATE_ACCESS_CONTRACTS_URI = "/accesscontracts";
    static final String UPDATE_INGEST_CONTRACTS_URI = "/ingestcontracts";
    static final String UPDATE_MANAGEMENT_CONTRACTS_URI = "/managementcontracts";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ContractResource.class);
    private static final String INGEST_CONTRACT_JSON_IS_MANDATORY_PATAMETER =
        "The json input of ingest contracts is mandatory";
    private static final String ACCESS_CONTRACT_JSON_IS_MANDATORY_PATAMETER =
        "The json input of access contracts is mandatory";

    private final MongoDbAccessAdminImpl mongoAccess;
    private final VitamCounterService vitamCounterService;

    /**
     * @param mongoAccess
     */
    public ContractResource(MongoDbAccessAdminImpl mongoAccess, VitamCounterService vitamCounterService) {
        this.mongoAccess = mongoAccess;
        this.vitamCounterService = vitamCounterService;
        LOGGER.debug("init Admin Management Resource server");
    }

    /**
     * Import a set of ingest contracts after passing the validation steps. If all the contracts are valid, they are
     * stored in the collection and indexed. </BR>
     * The input is invalid in the following situations : </BR>
     * <ul>
     * <li>The json is invalid</li>
     * <li>The json contains 2 ore many contracts having the same name</li>
     * <li>One or more mandatory field is missing</li>
     * <li>A field has an invalid format</li>
     * <li>One or many contracts elready exist in the database</li>
     * </ul>
     *
     * @param ingestContractModelList as InputStream
     * @param uri the uri info
     * @return Response
     */
    @Path(INGEST_CONTRACTS_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importContracts(List<IngestContractModel> ingestContractModelList, @Context UriInfo uri) {
        ParametersChecker.checkParameter(INGEST_CONTRACT_JSON_IS_MANDATORY_PATAMETER, ingestContractModelList);

        try (
            ContractService<IngestContractModel> ingestContract = new IngestContractImpl(
                mongoAccess,
                vitamCounterService
            )
        ) {
            RequestResponse<IngestContractModel> requestResponse = ingestContract.createContracts(
                ingestContractModelList
            );

            if (!requestResponse.isOk()) {
                ((VitamError) requestResponse).setHttpCode(Status.BAD_REQUEST.getStatusCode());
                return Response.status(Status.BAD_REQUEST).entity(requestResponse).build();
            } else {
                return Response.created(uri.getRequestUri().normalize()).entity(requestResponse).build();
            }
        } catch (VitamException exp) {
            LOGGER.error(exp);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, exp.getMessage(), null))
                .build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exp.getMessage(), null))
                .build();
        }
    }

    /**
     * Find ingest contracts by queryDsl
     *
     * @param queryDsl
     * @return Response
     */
    @GET
    @Path(INGEST_CONTRACTS_URI)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findIngestContracts(JsonNode queryDsl) {
        try (
            ContractService<IngestContractModel> ingestContract = new IngestContractImpl(
                mongoAccess,
                vitamCounterService
            )
        ) {
            final RequestResponseOK<IngestContractModel> ingestContractModelList = ingestContract
                .findContracts(queryDsl)
                .setQuery(queryDsl);

            return Response.status(Status.OK).entity(ingestContractModelList).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage(), null))
                .build();
        }
    }

    /**
     * Import a set of contracts access after passing the validation steps. If all the contracts are valid, they are
     * stored in the collection and indexed. </BR>
     * The input is invalid in the following situations : </BR>
     * <ul>
     * <li>The json is invalid</li>
     * <li>The json contains 2 ore many contracts having the same name</li>
     * <li>One or more mandatory field is missing</li>
     * <li>A field has an invalid format</li>
     * <li>One or many contracts already exist in the database</li>
     * </ul>
     *
     * @param accessContractModelList
     * @param uri
     * @return Response
     */
    @Path(ACCESS_CONTRACTS_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importAccessContracts(List<AccessContractModel> accessContractModelList, @Context UriInfo uri) {
        ParametersChecker.checkParameter(ACCESS_CONTRACT_JSON_IS_MANDATORY_PATAMETER, accessContractModelList);
        try (
            ContractService<AccessContractModel> accessContract = new AccessContractImpl(
                mongoAccess,
                vitamCounterService
            )
        ) {
            RequestResponse<AccessContractModel> requestResponse = accessContract.createContracts(
                accessContractModelList
            );

            if (!requestResponse.isOk()) {
                ((VitamError<AccessContractModel>) requestResponse).setHttpCode(Status.BAD_REQUEST.getStatusCode());
                return Response.status(Status.BAD_REQUEST).entity(requestResponse).build();
            } else {
                return Response.created(uri.getRequestUri().normalize()).entity(requestResponse).build();
            }
        } catch (VitamException exp) {
            LOGGER.error(exp);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, exp.getMessage(), null))
                .build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exp.getMessage(), null))
                .build();
        }
    }

    @Path(UPDATE_ACCESS_CONTRACTS_URI + "/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAccessContract(@PathParam("id") String contractId, JsonNode queryDsl) {
        try (
            ContractService<AccessContractModel> accessContract = new AccessContractImpl(
                mongoAccess,
                vitamCounterService
            )
        ) {
            RequestResponse<AccessContractModel> requestResponse = accessContract.updateContract(contractId, queryDsl);
            if (Response.Status.NOT_FOUND.getStatusCode() == requestResponse.getHttpCode()) {
                ((VitamError<AccessContractModel>) requestResponse).setHttpCode(Status.NOT_FOUND.getStatusCode());
                return Response.status(Status.NOT_FOUND).entity(requestResponse).build();
            } else if (!requestResponse.isOk()) {
                ((VitamError<AccessContractModel>) requestResponse).setHttpCode(Status.BAD_REQUEST.getStatusCode());
                return Response.status(Status.BAD_REQUEST).entity(requestResponse).build();
            } else {
                return Response.status(Status.OK).entity(requestResponse).build();
            }
        } catch (VitamException exp) {
            LOGGER.error(exp);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, exp.getMessage(), null))
                .build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exp.getMessage(), null))
                .build();
        }
    }

    @Path(UPDATE_INGEST_CONTRACTS_URI + "/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateIngestContract(@PathParam("id") String contractId, JsonNode queryDsl) {
        try (
            ContractService<IngestContractModel> ingestContract = new IngestContractImpl(
                mongoAccess,
                vitamCounterService
            )
        ) {
            RequestResponse<IngestContractModel> requestResponse = ingestContract.updateContract(contractId, queryDsl);
            if (Response.Status.NOT_FOUND.getStatusCode() == requestResponse.getHttpCode()) {
                ((VitamError) requestResponse).setHttpCode(Status.NOT_FOUND.getStatusCode());
                return Response.status(Status.NOT_FOUND).entity(requestResponse).build();
            } else if (!requestResponse.isOk()) {
                ((VitamError) requestResponse).setHttpCode(Status.BAD_REQUEST.getStatusCode());
                return Response.status(Status.BAD_REQUEST).entity(requestResponse).build();
            } else {
                return Response.status(Status.OK).entity(requestResponse).build();
            }
        } catch (VitamException exp) {
            LOGGER.error(exp);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, exp.getMessage(), null))
                .build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exp.getMessage(), null))
                .build();
        }
    }

    /**
     * find access contracts by queryDsl
     *
     * @param queryDsl
     * @return Response
     */
    @Path(ACCESS_CONTRACTS_URI)
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAccessContracts(JsonNode queryDsl) {
        try (
            ContractService<AccessContractModel> accessContract = new AccessContractImpl(
                mongoAccess,
                vitamCounterService
            )
        ) {
            final RequestResponseOK<AccessContractModel> accessContractModelList = accessContract
                .findContracts(queryDsl)
                .setQuery(queryDsl);
            return Response.status(Status.OK).entity(accessContractModelList).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage(), null))
                .build();
        }
    }

    /**
     * Import a set of management contracts after passing the validation steps. If all the contracts are valid, they are
     * stored in the collection and indexed. </BR>
     * The input is invalid in the following situations : </BR>
     * <ul>
     * <li>The json is invalid</li>
     * <li>The json contains 2 ore many contracts having the same name</li>
     * <li>One or more mandatory field is missing</li>
     * <li>A field has an invalid format</li>
     * <li>One or many contracts already exist in the database</li>
     * <li>One or many of the storage strategies are invalid</li>
     * </ul>
     *
     * @param managementContractModelList
     * @param uri
     * @return Response
     */
    @Path(MANAGEMENT_CONTRACTS_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importManagementContracts(
        List<ManagementContractModel> managementContractModelList,
        @Context UriInfo uri
    ) {
        ParametersChecker.checkParameter(ACCESS_CONTRACT_JSON_IS_MANDATORY_PATAMETER, managementContractModelList);
        try (
            ContractService<ManagementContractModel> managementContract = new ManagementContractImpl(
                mongoAccess,
                vitamCounterService
            )
        ) {
            RequestResponse<ManagementContractModel> requestResponse = managementContract.createContracts(
                managementContractModelList
            );

            if (!requestResponse.isOk()) {
                ((VitamError) requestResponse).setHttpCode(Status.BAD_REQUEST.getStatusCode());
                return Response.status(Status.BAD_REQUEST).entity(requestResponse).build();
            } else {
                return Response.created(uri.getRequestUri().normalize()).entity(requestResponse).build();
            }
        } catch (VitamException exp) {
            LOGGER.error(exp);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, exp.getMessage(), null))
                .build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exp.getMessage(), null))
                .build();
        }
    }

    /**
     * find management contracts by queryDsl
     *
     * @param queryDsl
     * @return Response
     */
    @Path(MANAGEMENT_CONTRACTS_URI)
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findManagementContracts(JsonNode queryDsl) {
        try (
            ContractService<ManagementContractModel> managementContract = new ManagementContractImpl(
                mongoAccess,
                vitamCounterService
            )
        ) {
            final RequestResponseOK<ManagementContractModel> managementContractModelList = managementContract
                .findContracts(queryDsl)
                .setQuery(queryDsl);
            return Response.status(Status.OK).entity(managementContractModelList).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage(), null))
                .build();
        }
    }

    @Path(UPDATE_MANAGEMENT_CONTRACTS_URI + "/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateManagementContract(@PathParam("id") String contractId, JsonNode queryDsl) {
        try (
            ContractService<ManagementContractModel> managementContract = new ManagementContractImpl(
                mongoAccess,
                vitamCounterService
            )
        ) {
            RequestResponse<ManagementContractModel> requestResponse = managementContract.updateContract(
                contractId,
                queryDsl
            );
            if (Response.Status.NOT_FOUND.getStatusCode() == requestResponse.getHttpCode()) {
                ((VitamError) requestResponse).setHttpCode(Status.NOT_FOUND.getStatusCode());
                return Response.status(Status.NOT_FOUND).entity(requestResponse).build();
            } else if (!requestResponse.isOk()) {
                ((VitamError) requestResponse).setHttpCode(Status.BAD_REQUEST.getStatusCode());
                return Response.status(Status.BAD_REQUEST).entity(requestResponse).build();
            } else {
                return Response.status(Status.OK).entity(requestResponse).build();
            }
        } catch (VitamException exp) {
            LOGGER.error(exp);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, exp.getMessage(), null))
                .build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exp.getMessage(), null))
                .build();
        }
    }

    /**
     * Construct the error following input
     *
     * @param status Http error status
     * @param message The functional error message, if absent the http reason phrase will be used instead
     * @param code The functional error code, if absent the http code will be used instead
     * @return
     */
    private VitamError getErrorEntity(Status status, String message, String code) {
        String aMessage = (message != null && !message.trim().isEmpty())
            ? message
            : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        String aCode = (code != null) ? code : String.valueOf(status.getStatusCode());
        return new VitamError(aCode)
            .setHttpCode(status.getStatusCode())
            .setContext(ADMIN_MODULE)
            .setState("code_vitam")
            .setMessage(status.getReasonPhrase())
            .setDescription(aMessage);
    }
}
