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
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.core.ontologies.OntologyService;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
public class OntologyResource {

    private static final String FUNCTIONAL_ADMINISTRATION_MODULE = "FUNCTIONAL_ADMINISTRATION_MODULE";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(OntologyResource.class);
    private static final String ONTOLOGY_URI = "/ontologies";
    private static final String ONTOLOGY_CACHE_URI = "/ontologies/cache";

    private static final String ONTOLOGY_JSON_IS_MANDATORY_PATAMETER = "The json input of ontology type is mandatory";

    private final OntologyService ontologyService;

    /**
     * @param ontologyService
     */
    public OntologyResource(OntologyService ontologyService) {
        this.ontologyService = ontologyService;
        LOGGER.debug("init Ontology Resource server");
    }

    /**
     * Import a set of ontologies metadata. </BR>
     * If all the ontologies are valid, they will be stored in the ontology collection and indexed.
     * The input is invalid in the following situations : </BR>
     * <ul>
     * <li>The json is invalid</li>
     * <li>The json contains an already used identifier</li>
     * <li>One or more mandatory field is missing</li>
     * <li>A field has an invalid format</li>
     * </ul>
     *
     * @param ontologyModelList as InputStream
     * @param uri the uri info
     * @return Response
     */
    @Path(ONTOLOGY_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importOntologies(
        @HeaderParam(GlobalDataRest.FORCE_UPDATE) boolean forceUpdate,
        List<OntologyModel> ontologyModelList,
        @Context UriInfo uri
    ) {
        ParametersChecker.checkParameter(ONTOLOGY_JSON_IS_MANDATORY_PATAMETER, ontologyModelList);

        try {
            RequestResponse<OntologyModel> requestResponse = ontologyService.importOntologies(
                forceUpdate,
                ontologyModelList
            );

            if (!requestResponse.isOk()) {
                return Response.status(requestResponse.getHttpCode()).entity(requestResponse).build();
            } else {
                return Response.created(uri.getRequestUri().normalize()).entity(requestResponse).build();
            }
        } catch (VitamException exp) {
            LOGGER.error(exp);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, exp.getMessage()))
                .build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, exp.getMessage()))
                .build();
        }
    }

    /**
     * Find ontologies by queryDsl
     *
     * @param queryDsl
     * @return Response
     */
    @GET
    @Path(ONTOLOGY_URI)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findOntologies(JsonNode queryDsl) {
        try {
            final RequestResponseOK<OntologyModel> ontologyModelList = ontologyService
                .findOntologies(queryDsl)
                .setQuery(queryDsl);

            return Response.status(Status.OK).entity(ontologyModelList).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage()))
                .build();
        }
    }

    /**
     * Find ontologies for cache by queryDsl
     *
     * @param queryDsl
     * @return Response
     */
    @GET
    @Path(ONTOLOGY_CACHE_URI)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findOntologiesForCache(JsonNode queryDsl) {
        try {
            final RequestResponseOK<OntologyModel> ontologyModelList = ontologyService
                .findOntologiesForCache(queryDsl)
                .setQuery(queryDsl);

            return Response.status(Status.OK).entity(ontologyModelList).build();
        } catch (ReferentialException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage()))
                .build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage()))
                .build();
        }
    }

    /**
     * Construct the error following input
     *
     * @param status Http error status
     * @param message The functional error message, if absent the http reason phrase will be used instead
     * @return
     */
    private VitamError<OntologyModel> getErrorEntity(Status status, String message) {
        String aMessage = (message != null && !message.trim().isEmpty())
            ? message
            : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        return new VitamError<OntologyModel>(String.valueOf(status.getStatusCode()))
            .setHttpCode(status.getStatusCode())
            .setContext(FUNCTIONAL_ADMINISTRATION_MODULE)
            .setState("ko")
            .setMessage(status.getReasonPhrase())
            .setDescription(aMessage);
    }
}
