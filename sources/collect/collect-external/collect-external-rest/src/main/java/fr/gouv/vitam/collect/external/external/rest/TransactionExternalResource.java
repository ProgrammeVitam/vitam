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
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.common.exception.CollectRequestResponse;
import fr.gouv.vitam.collect.internal.client.CollectInternalClient;
import fr.gouv.vitam.collect.internal.client.CollectInternalClientFactory;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.dsl.schema.Dsl;
import fr.gouv.vitam.common.dsl.schema.DslSchema;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.security.rest.Secured;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
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
import java.io.InputStream;

import static fr.gouv.vitam.common.ParametersChecker.checkParameter;
import static fr.gouv.vitam.common.model.ProcessAction.RESUME;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_ABORT;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_CLOSE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_ID_DELETE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_ID_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_ID_UNITS_UPDATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_REOPEN;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_SEND;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_UNIT_CREATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_UNIT_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_UNIT_WITH_INHERITED_RULES_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_UPDATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.TRANSACTION_ZIP_CREATE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;


@Path("/collect-external/v1/transactions")
@Tag(name = "Collect-External")
public class TransactionExternalResource extends ApplicationStatusResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransactionExternalResource.class);
    private static final String PREDICATES_FAILED_EXCEPTION = "Predicates Failed Exception ";
    private static final String YOU_MUST_SUPPLY_TRANSACTION_DATA = "You must supply transaction data!";
    public static final String ERROR_WHEN_UPDATE_TRANSACTION__ = "Error when update transaction  ";
    private final CollectInternalClientFactory collectInternalClientFactory;
    private final IngestExternalClientFactory ingestExternalClientFactory;

    /**
     * Constructor CollectExternalResource
     */
    TransactionExternalResource() {
        this(CollectInternalClientFactory.getInstance(), IngestExternalClientFactory.getInstance());
    }

    @VisibleForTesting
    TransactionExternalResource(CollectInternalClientFactory collectInternalClientFactory,
        IngestExternalClientFactory ingestExternalClientFactory) {
        this.collectInternalClientFactory = collectInternalClientFactory;
        this.ingestExternalClientFactory = ingestExternalClientFactory;
    }

    @Path("/{transactionId}")
    @GET
    @Produces(APPLICATION_JSON)
    @Secured(permission = TRANSACTION_ID_READ, description = "retourner la transaction par son id")
    public Response getTransactionById(@PathParam("transactionId") String transactionId) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(transactionId);
            ParametersChecker.checkParameter("You must supply a transaction id !", transactionId);
            RequestResponse<JsonNode> response = client.getTransactionById(transactionId);
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when getting transaction  ", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }
    }

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_UPDATE, description = "Mise à jour d'une transaction")
    public Response updateTransaction(TransactionDto transactionDto) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            ParametersChecker.checkParameter(YOU_MUST_SUPPLY_TRANSACTION_DATA, transactionDto);
            RequestResponse<JsonNode> response = client.updateTransaction(transactionDto);
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (final VitamClientException e) {
            LOGGER.error(ERROR_WHEN_UPDATE_TRANSACTION__, e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        }
    }


    @Path("/{transactionId}")
    @DELETE
    @Produces(APPLICATION_JSON)
    @Secured(permission = TRANSACTION_ID_DELETE, description = "Supprime une transaction par son id")
    public Response deleteTransactionById(@PathParam("transactionId") String transactionId) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(transactionId);
            client.deleteTransactionById(transactionId);
            return Response.status(Response.Status.OK).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when delete transaction   ", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }
    }


    @Path("/{transactionId}/units")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = TRANSACTION_UNIT_CREATE, description = "Crée une unité archivistique et la rattache à la transaction courante")
    public Response uploadArchiveUnit(@PathParam("transactionId") String transactionId, JsonNode unitJsonNode) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(transactionId);
            SanityChecker.checkJsonAll(unitJsonNode);
            RequestResponse<JsonNode> response = client.uploadArchiveUnit(unitJsonNode, transactionId);
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when uploading unit   ", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
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
    @Secured(permission = TRANSACTION_UNIT_READ, description = "Récupére toutes les unités archivistique")
    public Response selectUnits(@PathParam("transactionId") String transactionId,
        @Dsl(value = DslSchema.SELECT_MULTIPLE) JsonNode jsonQuery) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(transactionId);
            SanityChecker.checkJsonAll(jsonQuery);
            RequestResponse<JsonNode> response = client.getUnitsByTransaction(transactionId, jsonQuery);
            return Response.ok(response).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when selecting units by transaction   ", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }
    }

    @Path("/{transactionId}/close")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = TRANSACTION_CLOSE, description = "Ferme une transaction")
    public Response closeTransaction(@PathParam("transactionId") String transactionId) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(transactionId);
            client.closeTransaction(transactionId);
            return Response.status(Response.Status.OK).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when closing transaction   ", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }
    }

    @Path("/{transactionId}/abort")
    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = TRANSACTION_ABORT, description = "Abandonner une transaction")
    public Response abortTransaction(@PathParam("transactionId") String transactionId) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(transactionId);
            client.abortTransaction(transactionId);
            return Response.status(Response.Status.OK).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when abort transaction   ", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }
    }

    @Path("/{transactionId}/reopen")
    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Secured(permission = TRANSACTION_REOPEN, description = "Rouvrir une transaction")
    public Response reopenTransaction(@PathParam("transactionId") String transactionId) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(transactionId);
            client.reopenTransaction(transactionId);
            return Response.status(Response.Status.OK).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when reopen transaction   ", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }
    }

    @Path("/{transactionId}/send")
    @POST
    @Consumes(APPLICATION_JSON)
    @Secured(permission = TRANSACTION_SEND, description = "Envoi vers VITAM la transaction")
    public Response generateAndSendSip(@PathParam("transactionId") String transactionId) {
        try (CollectInternalClient collectClient = collectInternalClientFactory.getClient();
            IngestExternalClient clientIngest = ingestExternalClientFactory.getClient()) {
            SanityChecker.checkParameter(transactionId);
            LOGGER.info("Preparing SIP transaction to workspace");
            InputStream responseStream = collectClient.generateSip(transactionId);
            RequestResponse<Void> response = clientIngest.ingest(new VitamContext(ParameterHelper.getTenantParameter()),
                responseStream, DEFAULT_WORKFLOW.name(), RESUME.name());
            collectClient.attachVitamOperationId(transactionId, response.getHeaderString(GlobalDataRest.X_REQUEST_ID));
            collectClient.changeTransactionStatus(transactionId, TransactionStatus.SENT);
            LOGGER.info("SIP sent with success ");
            return Response.ok().build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        } catch (Exception ex) {
            LOGGER.error("Error when ingesting  transaction   ", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    @Path("/{transactionId}/units")
    @PUT
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(APPLICATION_JSON)
    @Secured(permission = TRANSACTION_ID_UNITS_UPDATE, description = "Mettre à jour les unités archivistiques")
    public Response updateUnits(@PathParam("transactionId") String transactionId, InputStream inputStream) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(transactionId);
            checkParameter("You must supply a file!", inputStream);
            RequestResponse<JsonNode> response = client.updateUnits(transactionId, inputStream);
            return Response.status(Response.Status.OK).entity(response).build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when updating units   ", e);
            return CollectRequestResponse.toVitamError(BAD_REQUEST, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }
    }

    @Path("/{transactionId}/upload")
    @POST
    @Consumes({CommonMediaType.ZIP})
    @Produces(APPLICATION_JSON)
    @Secured(permission = TRANSACTION_ZIP_CREATE, description = "Charge les binaires d'une transaction")
    public Response uploadTransactionZip(@PathParam("transactionId") String transactionId,
        InputStream inputStreamObject) {
        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(transactionId);
            checkParameter("You must supply a file!", inputStreamObject);
            client.uploadTransactionZip(transactionId, inputStreamObject);
            return Response.ok().build();
        } catch (final VitamClientException e) {
            LOGGER.error("Error when uploading transaction Zip   ", e);
            return CollectRequestResponse.toVitamError(Response.Status.BAD_REQUEST, e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        }
    }

    @Path("/{transactionId}/unitsWithInheritedRules")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(permission = TRANSACTION_UNIT_WITH_INHERITED_RULES_READ, description = "Récupérer la liste des unités archivistiques avec leurs règles de gestion héritées")
    public Response selectUnitsWithInheritedRules(@PathParam("transactionId") String transactionId,
        @Dsl(value = DslSchema.SELECT_MULTIPLE) JsonNode queryJson) {

        try (CollectInternalClient client = collectInternalClientFactory.getClient()) {
            SanityChecker.checkParameter(transactionId);
            RequestResponse<JsonNode> result = client.selectUnitsWithInheritedRules(transactionId, queryJson);
            int st = result.isOk() ? Response.Status.OK.getStatusCode() : result.getHttpCode();
            return Response.status(st).entity(result).build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(PREDICATES_FAILED_EXCEPTION, e);
            return Response.status(Response.Status.PRECONDITION_FAILED).build();
        } catch (VitamClientException e) {
            LOGGER.error("Error when selecting Units With Inherited Rules ", e);
            return Response.status(BAD_REQUEST).build();
        }
    }
}