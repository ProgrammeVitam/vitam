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
package fr.gouv.vitam.access.external.rest;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

/**
 * Admin Management External Resource Implement
 */
@Path("/admin-external/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class AdminManagementExternalResourceImpl {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementExternalResourceImpl.class);

    /**
     * Constructor
     *
     * @param configuration config for constructing AdminManagement
     */
    public AdminManagementExternalResourceImpl() {
        LOGGER.debug("init Admin Management Resource server");
    }

    /**
     * checkDocument
     * 
     * @param collection
     * @param document
     * @return Response
     */
    @Path("/{collection}")
    @PUT
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkDocument(@PathParam("collection") String collection, InputStream document) {
        ParametersChecker.checkParameter("xmlPronom is a mandatory parameter", document);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            if (AdminCollections.FORMATS.compareTo(collection)) {
                Status status = client.checkFormat(document);
                return Response.status(status).build();
            }
            if (AdminCollections.RULES.compareTo(collection)) {
                Status status = client.checkRulesFile(document);
                return Response.status(status).build();
            }
            return Response.status(Status.NOT_FOUND).build();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            final Status status = Status.PRECONDITION_FAILED;
            return Response.status(status).entity(status).build();
        } finally {
            StreamUtils.closeSilently(document);
        }
    }

    /**
     * importDocument
     * 
     * @param collection
     * @param document
     * @return Response
     */
    @Path("/{collection}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importDocument(@PathParam("collection") String collection, InputStream document) {
        ParametersChecker.checkParameter("xmlPronom is a mandatory parameter", document);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            if (AdminCollections.FORMATS.compareTo(collection)) {
                client.importFormat(document);
                return Response.status(Status.CREATED).build();
            }
            if (AdminCollections.RULES.compareTo(collection)) {
                client.importRulesFile(document);
                return Response.status(Status.CREATED).build();
            }
            return Response.status(Status.NOT_FOUND).build();
        } catch (DatabaseConflictException e) {
            LOGGER.error(e);
            final Status status = Status.CONFLICT;
            return Response.status(status).entity(status).build();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(status).build();
        } finally {
            StreamUtils.closeSilently(document);
        }

    }

    /**
     * deleteDocuments
     * 
     * @param collection
     * @return Response
     */
    @Path("/{collection}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteDocuments(@PathParam("collection") String collection) {
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            if (AdminCollections.FORMATS.compareTo(collection)) {
                client.deleteFormat();
                return Response.status(Status.OK).build();
            }
            if (AdminCollections.RULES.compareTo(collection)) {
                client.deleteRulesFile();
                return Response.status(Status.OK).build();
            }
            return Response.status(Status.NOT_FOUND).build();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(status).build();
        }
    }

    /**
     * findDocuments
     * 
     * @param collection
     * @param select
     * @return Response
     */
    @Path("/{collection}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findDocuments(@PathParam("collection") String collection, JsonNode select) {
        
        ParametersChecker.checkParameter("select query is a mandatory parameter", select);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            if (AdminCollections.FORMATS.compareTo(collection)) {
                JsonNode result = client.getFormats(select);
                return Response.status(Status.OK).entity(result).build();
            }
            if (AdminCollections.RULES.compareTo(collection)) {
                JsonNode result = client.getRules(select);
                return Response.status(Status.OK).entity(result).build();
            }
            return Response.status(Status.NOT_FOUND).build();
        } catch (ReferentialException | IOException e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(status).build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(status).build();
        }
    }



    /**
     * findDocumentByID
     * 
     * @param collection
     * @param documentId
     * @return Response
     */
    @POST
    @Path("/{collection}/{id_document}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findDocumentByID(@PathParam("collection") String collection,
        @PathParam("id_document") String documentId) {
        ParametersChecker.checkParameter("formatId is a mandatory parameter", documentId);
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            if (AdminCollections.FORMATS.compareTo(collection)) {
                JsonNode result = client.getFormatByID(documentId);
                return Response.status(Status.OK).entity(result).build();
            }
            if (AdminCollections.RULES.compareTo(collection)) {
                JsonNode result = client.getRuleByID(documentId);
                return Response.status(Status.OK).entity(result).build();
            }

            return Response.status(Status.NOT_FOUND).build();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status).entity(status).build();
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            return Response.status(status).entity(status).build();
        }
    }

}
