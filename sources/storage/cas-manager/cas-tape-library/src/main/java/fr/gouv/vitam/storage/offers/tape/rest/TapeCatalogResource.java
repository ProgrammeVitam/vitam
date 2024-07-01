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
package fr.gouv.vitam.storage.offers.tape.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.database.server.query.QueryCriteria;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.offers.tape.TapeLibraryFactory;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Path("/offer/v1")
@ApplicationPath("webresources")
@Tag(name = "Tape")
public class TapeCatalogResource extends ApplicationStatusResource {

    private static final String MISSING_THE_TAPE_ID = "Missing the tape ID or wrong ID";
    private static final String MISSING_THE_SEARCH_CRITERIA = "Missing or wrong search criteria";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeCatalogResource.class);

    private final TapeCatalogService tapeCatalogService;

    /**
     * Constructor
     *
     * @param tapeCatalogService
     */
    @VisibleForTesting
    public TapeCatalogResource(TapeCatalogService tapeCatalogService) {
        LOGGER.debug("TapeCatalogService initialized");
        this.tapeCatalogService = tapeCatalogService;
    }

    /**
     * TapeLibraryFactory should be already initialized
     */
    public TapeCatalogResource() {
        LOGGER.debug("TapeCatalogService initialized");
        this.tapeCatalogService = TapeLibraryFactory.getInstance().getTapeCatalogService();
    }

    /**
     * Get a tape model from catalog
     *
     * @param tapeId
     * @return a tape model from catalog
     */
    @GET
    @Path("/tapecatalog/{tapeId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "return tape model from catalog",
        description = "Permet de récupérer la cassette de stockage à partir du catalogue"
    )
    public Response getTape(@PathParam("tapeId") String tapeId) {
        try {
            if (Strings.isNullOrEmpty(tapeId)) {
                LOGGER.error(MISSING_THE_TAPE_ID);
                return Response.status(Status.BAD_REQUEST).build();
            }

            final RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<>();

            TapeCatalog tapeCatalog = tapeCatalogService.findById(tapeId);
            if (tapeCatalog == null) {
                LOGGER.error(String.format("Tape with id %s not found", tapeId));
                return Response.status(Status.NOT_FOUND).build();
            }
            responseOK.addAllResults(Arrays.asList(JsonHandler.toJsonNode(tapeCatalog)));
            LOGGER.debug("Result {}", responseOK);
            return Response.status(Status.OK).entity(responseOK).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).build();
        }
    }

    /**
     * Get a list of tape model from catalog
     *
     * @param criteria
     * @return a list of tape model from catalog
     */
    @GET
    @Path("/tapecatalog/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "return tapes list from catalog",
        description = "Permet de récupérer la liste des cassettes de stockage à partir du catalogue"
    )
    public Response getTapes(List<QueryCriteria> criteria) {
        try {
            if (criteria == null || criteria.isEmpty()) {
                LOGGER.error(MISSING_THE_SEARCH_CRITERIA);
                return Response.status(Status.BAD_REQUEST).build();
            }

            final RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<>();
            responseOK.addAllResults(Arrays.asList(JsonHandler.toJsonNode(tapeCatalogService.find(criteria))));
            LOGGER.debug("Result {}", responseOK);
            return Response.status(Status.OK).entity(responseOK).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).build();
        }
    }

    /**
     * replaces existing tape model by id.
     *
     * @param
     * @return
     */
    @PUT
    @Path("/tapecatalog/tapeId")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "replace tape model by id",
        description = "Permet de remplacer entièrement une cassette de stockage à partir d'un id"
    )
    public Response replaceTape(@PathParam("tapeId") String tapeId, TapeCatalog tapeCatalog) {
        try {
            if (Strings.isNullOrEmpty(tapeId)) {
                LOGGER.error(MISSING_THE_TAPE_ID);
                return Response.status(Status.BAD_REQUEST).build();
            }

            boolean replaced = tapeCatalogService.replace(tapeCatalog);
            if (!replaced) {
                return Response.status(Status.NOT_MODIFIED).build();
            }
            return Response.status(Status.OK).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).build();
        }
    }

    /**
     * updates existing tape model by id.
     *
     * @param
     * @return
     */
    @PUT
    @Path("/tapecatalog/tapeId")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "update tape model by id",
        description = "Permet de modifier une cassette de stockage à partir d'un id"
    )
    public Response updateTape(@PathParam("tapeId") String tapeId, Map<String, Object> fields) {
        try {
            if (Strings.isNullOrEmpty(tapeId)) {
                LOGGER.error(MISSING_THE_TAPE_ID);
                return Response.status(Status.BAD_REQUEST).build();
            }

            if (fields == null || fields.isEmpty()) {
                LOGGER.error(MISSING_THE_SEARCH_CRITERIA);
                return Response.status(Status.BAD_REQUEST).build();
            }

            boolean updated = tapeCatalogService.update(tapeId, fields);
            if (!updated) {
                return Response.status(Status.NOT_MODIFIED).build();
            }
            return Response.status(Status.OK).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).build();
        }
    }

    /**
     * Creates a tape model.
     *
     * @param
     * @return
     */
    @POST
    @Path("/tapecatalog/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "create a tape model", description = "Permet de créer une cassette de stockage")
    public Response createTape(TapeCatalog tapeCatalog) {
        try {
            final RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<>();

            tapeCatalogService.create(tapeCatalog);
            return Response.status(Status.OK).entity(responseOK).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).build();
        }
    }
}
