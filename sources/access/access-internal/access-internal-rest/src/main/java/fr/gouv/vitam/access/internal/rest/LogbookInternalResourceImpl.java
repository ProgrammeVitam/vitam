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
package fr.gouv.vitam.access.internal.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

/**
 * AccessResourceImpl implements AccessResource
 */
@Path("/access-internal/v1")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@javax.ws.rs.ApplicationPath("webresources")
public class LogbookInternalResourceImpl {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookInternalResourceImpl.class);
    private static final String LOGBOOK_MODULE = "LOGBOOK";
    private static final String CODE_VITAM = "code_vitam";
    //TODO Extract values from DSLQueryHelper
    private static final String EVENT_ID_PROCESS = "evIdProc";
    private static final String OB_ID = "obId";

    /**
     * Default Constructor
     */
    public LogbookInternalResourceImpl() {
        LOGGER.debug("LogbookExternalResource initialized");
    }

    /***** LOGBOOK OPERATION - START *****/
    /**
     * @param operationId the operation id
     * @param queryDsl the query
     * @return the response with a specific HTTP status
     */
    @GET
    @Path("/operations/{id_op}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOperationById(@PathParam("id_op") String operationId, JsonNode queryDsl) {
        Status status;
        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(queryDsl);
            SanityChecker.checkParameter(operationId);
            final SelectParserSingle parser = new SelectParserSingle(new VarNameAdapter());
            Select select = new Select();
            parser.parse(select.getFinalSelect());
            parser.addCondition(QueryHelper.eq(EVENT_ID_PROCESS, operationId));
            queryDsl = parser.getRequest().getFinalSelect();
            final JsonNode result = client.selectOperationById(operationId, queryDsl);
            return Response.status(Status.OK).entity(result).build();
        } catch (final LogbookClientException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (InvalidCreateOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
    }


    /**
     * GET with request in body
     *
     * @param query DSL as String
     * @return Response contains a list of logbook operation
     */
    @GET
    @Path("/operations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectOperation(JsonNode query) {
        Status status;
        try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
            // Check correctness of request
            final SelectParserSingle parser = new SelectParserSingle();
            parser.parse(query);
            parser.getRequest().reset();
            final JsonNode result = client.selectOperation(query);
            return Response.status(Status.OK).entity(result).build();
        } catch (final LogbookClientNotFoundException e) {
            return Response.status(Status.OK).entity(new RequestResponseOK().toJsonNode()).build();
        } catch (final LogbookClientException e) {
            LOGGER.error(e);
            status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
    }


    /*****
     * LOGBOOK LIFE CYCLES
     *****/

    /**
     * gets the unit life cycle based on its id
     *
     * @param unitLifeCycleId the unit life cycle id
     * @param queryDsl the query
     * @return the unit life cycle
     */
    @GET
    @Path("/unitlifecycles/{id_lc}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitLifeCycleById(@PathParam("id_lc") String unitLifeCycleId, JsonNode queryDsl) {
        Status status;
        try (LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            SanityChecker.checkParameter(unitLifeCycleId);
            SanityChecker.checkJsonAll(queryDsl);
            final JsonNode result = client.selectUnitLifeCycleById(unitLifeCycleId, queryDsl);
            return Response.status(Status.OK).entity(result).build();
        } catch (final LogbookClientException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
    }

    /**
     * gets the unit life cycle based on its id
     *
     * @param queryDsl dsl query containing obId
     * @return the unit life cycle
     */
    @GET
    @Path("/unitlifecycles")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitLifeCycle(JsonNode queryDsl) {
        Status status;
        try (LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(queryDsl);
            final SelectParserSingle parser = new SelectParserSingle(new VarNameAdapter());
            Select select = new Select();
            parser.parse(select.getFinalSelect());
            parser.addCondition(QueryHelper.eq(OB_ID, queryDsl.findValue(OB_ID).asText()));
            queryDsl = parser.getRequest().getFinalSelect();
            final JsonNode result = client.selectUnitLifeCycle(queryDsl);
            return Response.status(Status.OK).entity(result).build();
        } catch (final LogbookClientException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (InvalidCreateOperationException e) {
            LOGGER.error(e);
            status = Status.BAD_REQUEST;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
    }


    /**
     * gets the object group life cycle based on its id
     *
     * @param objectGroupLifeCycleId the object group life cycle id
     * @param queryDsl the query
     * @return the object group life cycle
     */
    @GET
    @Path("/objectgrouplifecycles/{id_lc}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroupLifeCycle(@PathParam("id_lc") String objectGroupLifeCycleId, JsonNode queryDsl) {
        Status status;
        try (LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            SanityChecker.checkParameter(objectGroupLifeCycleId);
            SanityChecker.checkJsonAll(queryDsl);
            final JsonNode result = client.selectObjectGroupLifeCycleById(objectGroupLifeCycleId, queryDsl);
            return Response.status(Status.OK).entity(result).build();
        } catch (final LogbookClientException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(getErrorEntity(status)).build();
        }
    }

    /***** LIFE CYCLES - END *****/


    private VitamError getErrorEntity(Status status) {
        return new VitamError(status.name()).setContext(LOGBOOK_MODULE)

            .setHttpCode(status.getStatusCode()).setState(CODE_VITAM).setMessage(status.getReasonPhrase())
            .setDescription(status.getReasonPhrase());
    }

}
