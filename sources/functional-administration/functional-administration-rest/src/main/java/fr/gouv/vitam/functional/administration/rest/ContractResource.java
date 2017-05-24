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

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AccessContractModel;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.functional.administration.client.model.IngestContractModel;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.contract.api.ContractService;
import fr.gouv.vitam.functional.administration.contract.core.AccessContractImpl;
import fr.gouv.vitam.functional.administration.contract.core.IngestContractImpl;
import fr.gouv.vitam.functional.administration.counter.VitamCounterService;

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

/**
 * FormatManagementResourceImpl implements AccessResource
 */
@Path("/adminmanagement/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class ContractResource {

    static final String INGEST_CONTRACTS_URI = "/contracts";
    static final String ACCESS_CONTRACTS_URI = "/accesscontracts";
    static final String UPDATE_ACCESS_CONTRACT_URI = "/accesscontract";
    static final String UPDATE_INGEST_CONTRACTS_URI = "/contract";


    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ContractResource.class);
    private static final String INGEST_CONTRACT_JSON_IS_MANDATORY_PATAMETER =
        "The json input of ingest contracts is mandatory";
    private static final String ACCESS_CONTRACT_JSON_IS_MANDATORY_PATAMETER =
        "The json input of access contracts is mandatory";

    private final MongoDbAccessAdminImpl mongoAccess;
    private final VitamCounterService vitamCounterService;
    /**
     *
     * @param mongoAccess
     */
    public ContractResource(MongoDbAccessAdminImpl mongoAccess, VitamCounterService vitamCounterService ) throws VitamException {
        this.mongoAccess = mongoAccess;
        this.vitamCounterService = vitamCounterService;
        LOGGER.debug("init Admin Management Resource server");
    }


    /**
     * Import a set of ingest contracts after passing the validation steps. If all the contracts are valid, they are
     * stored in the collection and indexed. </BR> The input is invalid in the following situations : </BR>
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
     * @return Response jersey response
     */
    @Path(INGEST_CONTRACTS_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importContracts(List<IngestContractModel> ingestContractModelList, @Context UriInfo uri) {
        ParametersChecker.checkParameter(INGEST_CONTRACT_JSON_IS_MANDATORY_PATAMETER, ingestContractModelList);

        try (ContractService<IngestContractModel> ingestContract = new IngestContractImpl(mongoAccess,
            vitamCounterService)) {
            RequestResponse requestResponse = ingestContract.createContracts(ingestContractModelList);

            if (!requestResponse.isOk()) {
                ((VitamError) requestResponse).setHttpCode(Status.BAD_REQUEST.getStatusCode());
                return Response.status(Status.BAD_REQUEST).entity(requestResponse).build();
            } else {

                return Response.created(uri.getRequestUri().normalize()).entity(requestResponse).build();
            }


        } catch (VitamException exp) {
            LOGGER.error(exp);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, exp.getMessage(), null)).build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exp.getMessage(), null)).build();
        }
    }

    /**
     * Find ingest contracts by queryDsl
     * 
     * @param queryDsl
     * @return
     */
    @GET
    @Path(INGEST_CONTRACTS_URI)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findIngestContracts(JsonNode queryDsl) {

        try (ContractService<IngestContractModel> ingestContract = new IngestContractImpl(mongoAccess,vitamCounterService)) {

            final List<IngestContractModel> ingestContractModelList = ingestContract.findContracts(queryDsl);

            return Response.status(Status.OK)
                .entity(
                    new RequestResponseOK().addAllResults(ingestContractModelList))
                .build();

        } catch (ReferentialException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage(), null)).build();
        }
    }

    /**
     * Import a set of contracts access after passing the validation steps. If all the contracts are valid, they are
     * stored in the collection and indexed. </BR> The input is invalid in the following situations : </BR>
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
     * @return Response jersey response
     */
    @Path(ACCESS_CONTRACTS_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importAccessContracts(List<AccessContractModel> accessContractModelList, @Context UriInfo uri) {
        ParametersChecker.checkParameter(ACCESS_CONTRACT_JSON_IS_MANDATORY_PATAMETER, accessContractModelList);
        try (ContractService<AccessContractModel> accessContract = new AccessContractImpl(mongoAccess,
            vitamCounterService)) {
            RequestResponse requestResponse = accessContract.createContracts(accessContractModelList);

            if (!requestResponse.isOk()) {
                ((VitamError) requestResponse).setHttpCode(Status.BAD_REQUEST.getStatusCode());
                return Response.status(Status.BAD_REQUEST).entity(requestResponse).build();
            } else {

                return Response.created(uri.getRequestUri().normalize()).entity(requestResponse).build();
            }


        } catch (VitamException exp) {
            LOGGER.error(exp);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, exp.getMessage(), null)).build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exp.getMessage(), null)).build();
        }
    }


    @Path(UPDATE_ACCESS_CONTRACT_URI + "/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAccessContract(@PathParam("id") String contractId, JsonNode queryDsl) {
        try (ContractService<AccessContractModel> accessContract = new AccessContractImpl(mongoAccess,
            vitamCounterService)) {
            RequestResponse requestResponse = accessContract.updateContract(contractId, queryDsl);
            if (!requestResponse.isOk()) {
                ((VitamError) requestResponse).setHttpCode(Status.BAD_REQUEST.getStatusCode());
                return Response.status(Status.BAD_REQUEST).entity(requestResponse).build();
            } else {

                return Response.status(Status.OK).entity(requestResponse).build();
            }
        } catch (VitamException exp) {
            LOGGER.error(exp);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, exp.getMessage(), null)).build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exp.getMessage(), null)).build();
        }
    }

    @Path(UPDATE_INGEST_CONTRACTS_URI + "/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateIngestContract(@PathParam("id") String contractId, JsonNode queryDsl) {
        try (ContractService<IngestContractModel> ingestContract = new IngestContractImpl(mongoAccess,
            vitamCounterService)) {
            RequestResponse requestResponse = ingestContract.updateContract(contractId, queryDsl);
            if (!requestResponse.isOk()) {
                ((VitamError) requestResponse).setHttpCode(Status.BAD_REQUEST.getStatusCode());
                return Response.status(Status.BAD_REQUEST).entity(requestResponse).build();
            } else {

                return Response.status(Status.OK).entity(requestResponse).build();
            }
        } catch (VitamException exp) {
            LOGGER.error(exp);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, exp.getMessage(), null)).build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exp.getMessage(), null)).build();
        }
    }


    /**
     * find access contracts by queryDsl
     * 
     * @return
     */
    @Path(ACCESS_CONTRACTS_URI)
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAccessContracts(JsonNode queryDsl) {
        try (ContractService<AccessContractModel> accessContract = new AccessContractImpl(mongoAccess,
            vitamCounterService)) {

            final List<AccessContractModel> accessContractModelList = accessContract.findContracts(queryDsl);

            return Response
                .status(Status.OK)
                .entity(
                    new RequestResponseOK().setHits(accessContractModelList.size(), 0, accessContractModelList.size())
                        .addAllResults(accessContractModelList))
                .build();

        } catch (ReferentialException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null)).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage(), null)).build();
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
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        String aCode = (code != null) ? code : String.valueOf(status.getStatusCode());
        return new VitamError(aCode).setHttpCode(status.getStatusCode()).setContext("ADMIN_MODULE")
            .setState("code_vitam").setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }

}
