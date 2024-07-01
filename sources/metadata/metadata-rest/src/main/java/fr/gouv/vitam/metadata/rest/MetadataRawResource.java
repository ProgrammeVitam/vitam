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
package fr.gouv.vitam.metadata.rest;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataRepositoryService;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;

@Path("/v1")
@Tag(name = "Metadata")
public class MetadataRawResource {

    /**
     * Repository service.
     */
    private MetadataRepositoryService metadataRepositoryService;

    /**
     * Constructor
     *
     * @param vitamRepositoryProvider vitam repository provider
     */
    public MetadataRawResource(VitamRepositoryProvider vitamRepositoryProvider) {
        this.metadataRepositoryService = new MetadataRepositoryService(vitamRepositoryProvider);
    }

    /**
     * Get Unit as raw data
     *
     * @param unitId the unit id to get
     * @return {@link Response} contains a request response json filled with unit result
     */
    @Path("/raw/units/{id_unit}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnitById(@PathParam("id_unit") String unitId) {
        return getById(MetadataCollections.UNIT, unitId);
    }

    /**
     * Get ObjectGroup as raw data
     *
     * @param objectGroupId the objectGroup ID to get
     * @return {@link Response} contains a request response json filled with object group result
     */
    @Path("/raw/objectgroups/{id_og}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectGroupById(@PathParam("id_og") String objectGroupId) {
        return getById(MetadataCollections.OBJECTGROUP, objectGroupId);
    }

    private Response getById(MetadataCollections collection, String id) {
        try {
            final Integer tenant = ParameterHelper.getTenantParameter();
            JsonNode document = metadataRepositoryService.getDocumentById(collection, id, tenant);
            RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
                .addResult(document)
                .setHttpCode(Status.OK.getStatusCode());
            return Response.status(Status.OK).entity(responseOK).build();
        } catch (MetaDataNotFoundException e) {
            return VitamCodeHelper.toVitamError(
                VitamCode.METADATA_NOT_FOUND,
                String.format("Could not find document of type %s", collection.getName())
            ).toResponse();
        } catch (DatabaseException e) {
            return VitamCodeHelper.toVitamError(
                VitamCode.METADATA_REPOSITORY_DATABASE_ERROR,
                String.format("Technical error while trying to find document of type %s", collection.getName())
            ).toResponse();
        } catch (InvalidParseOperationException e) {
            return VitamCodeHelper.toVitamError(
                VitamCode.METADATA_REPOSITORY_DATABASE_ERROR,
                String.format("Technical error while trying to parse document of type %s", collection.getName())
            ).toResponse();
        }
    }

    /**
     * Get Units as raw data
     *
     * @return {@link Response} contains a request response json filled with unit result
     */
    @Path("/raw/units")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBulkRawUnitByIds(JsonNode body) {
        return getByIds(MetadataCollections.UNIT, body);
    }

    /**
     * Get ObjectGroups as raw data
     *
     * @return {@link Response} contains a request response json filled with object group result
     */
    @Path("/raw/objectgroups")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBulkRawObjectGroupsByIds(JsonNode body) {
        return getByIds(MetadataCollections.OBJECTGROUP, body);
    }

    private Response getByIds(MetadataCollections collection, JsonNode idsJson) {
        try {
            final Integer tenant = ParameterHelper.getTenantParameter();
            List<String> ids = JsonHandler.getFromJsonNode(idsJson, List.class);
            List<JsonNode> documents = metadataRepositoryService.getDocumentsByIds(collection, ids, tenant);
            RequestResponse<JsonNode> responseOK = new RequestResponseOK<JsonNode>()
                .addAllResults(documents)
                .setHttpCode(Status.OK.getStatusCode());
            return Response.status(Status.OK).entity(responseOK).build();
        } catch (InvalidParseOperationException e) {
            return VitamCodeHelper.toVitamError(
                VitamCode.METADATA_REPOSITORY_DATABASE_ERROR,
                String.format("Technical error while trying to parse document of type %s", collection.getName())
            ).toResponse();
        }
    }
}
