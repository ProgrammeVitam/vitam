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
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.functional.administration.core.archiveunitprofiles.ArchiveUnitProfileService;
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
public class ArchiveUnitProfileResource {

    private static final String FUNCTIONAL_ADMINISTRATION_MODULE = "FUNCTIONAL_ADMINISTRATION_MODULE";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ArchiveUnitProfileResource.class);
    public static final String ARCHIVE_UNIT_PROFILE_URI = "/archiveunitprofiles";

    private static final String ARCHIVE_UNIT_PROFILE_JSON_IS_MANDATORY_PATAMETER =
        "The json input of archvie unit profile type is mandatory";
    private static final String DSL_QUERY_IS_MANDATORY_PATAMETER = "The dsl query is mandatory";

    private final ArchiveUnitProfileService archiveUnitProfileService;

    /**
     * @param archiveUnitProfileService
     */
    public ArchiveUnitProfileResource(ArchiveUnitProfileService archiveUnitProfileService) {
        this.archiveUnitProfileService = archiveUnitProfileService;
        LOGGER.debug("init Admin Management Resource server");
    }

    /**
     * Import a set of DocumentTypes. If all the DocumentTypes are valid, they will be stored in the collection and indexed. </BR>
     * The input is invalid in the following situations : </BR>
     * <ul>
     * <li>The json is invalid</li>
     * <li>The json contains 2 ore many document types having the same identifier</li>
     * <li>One or more mandatory field is missing</li>
     * <li>A field has an invalid format</li>
     * <li>One or many document type already exist in the database</li>
     * </ul>
     *
     * @param archiveUnitProfileModelList as InputStream
     * @param uri the uri info
     * @return Response
     */
    @Path(ARCHIVE_UNIT_PROFILE_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createProfiles(List<ArchiveUnitProfileModel> archiveUnitProfileModelList, @Context UriInfo uri) {
        ParametersChecker.checkParameter(ARCHIVE_UNIT_PROFILE_JSON_IS_MANDATORY_PATAMETER, archiveUnitProfileModelList);

        try {
            RequestResponse<ArchiveUnitProfileModel> requestResponse =
                archiveUnitProfileService.createArchiveUnitProfiles(archiveUnitProfileModelList);

            if (!requestResponse.isOk()) {
                requestResponse.setHttpCode(Status.BAD_REQUEST.getStatusCode());
                return Response.status(Status.BAD_REQUEST).entity(requestResponse).build();
            }

            return Response.created(uri.getRequestUri().normalize()).entity(requestResponse).build();
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
     * Update metadata of the archive unit profile
     *
     * @param profileMetadataId profile ID to update
     * @param queryDsl update query
     * @return Response
     */
    @Path(ARCHIVE_UNIT_PROFILE_URI + "/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProfileFile(@PathParam("id") String profileMetadataId, JsonNode queryDsl) {
        ParametersChecker.checkParameter(ARCHIVE_UNIT_PROFILE_JSON_IS_MANDATORY_PATAMETER, profileMetadataId);
        ParametersChecker.checkParameter(DSL_QUERY_IS_MANDATORY_PATAMETER, queryDsl);

        try {
            SanityChecker.checkParameter(profileMetadataId);
            RequestResponse<ArchiveUnitProfileModel> requestResponse =
                archiveUnitProfileService.updateArchiveUnitProfile(profileMetadataId, queryDsl);
            if (Response.Status.NOT_FOUND.getStatusCode() == requestResponse.getHttpCode()) {
                ((VitamError<ArchiveUnitProfileModel>) requestResponse).setHttpCode(Status.NOT_FOUND.getStatusCode());
                return Response.status(Status.NOT_FOUND).entity(requestResponse).build();
            } else if (!requestResponse.isOk()) {
                ((VitamError<ArchiveUnitProfileModel>) requestResponse).setHttpCode(Status.BAD_REQUEST.getStatusCode());
                return Response.status(Status.BAD_REQUEST).entity(requestResponse).build();
            } else {
                return Response.status(Status.OK).entity(requestResponse).build();
            }
        } catch (VitamException e) {
            // FIXME : Proper error management
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST)
                .entity(getErrorEntity(Status.BAD_REQUEST, e.getMessage(), null))
                .build();
        } catch (Exception e) {
            LOGGER.error("Unexpected server error {}", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage(), null))
                .build();
        }
    }

    /**
     * Find archive unit profiles by queryDsl
     *
     * @param queryDsl
     * @return Response
     */
    @GET
    @Path(ARCHIVE_UNIT_PROFILE_URI)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findProfiles(JsonNode queryDsl) {
        try {
            final RequestResponseOK<ArchiveUnitProfileModel> profileModelList = archiveUnitProfileService
                .findArchiveUnitProfiles(queryDsl)
                .setQuery(queryDsl);

            return Response.status(Status.OK).entity(profileModelList).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Status.INTERNAL_SERVER_ERROR, e.getMessage(), null))
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
    private VitamError<ArchiveUnitProfileModel> getErrorEntity(Status status, String message, String code) {
        String aMessage = (message != null && !message.trim().isEmpty())
            ? message
            : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        String aCode = (code != null) ? code : String.valueOf(status.getStatusCode());
        return new VitamError<ArchiveUnitProfileModel>(aCode)
            .setHttpCode(status.getStatusCode())
            .setContext(FUNCTIONAL_ADMINISTRATION_MODULE)
            .setState("ko")
            .setMessage(status.getReasonPhrase())
            .setDescription(aMessage);
    }
}
