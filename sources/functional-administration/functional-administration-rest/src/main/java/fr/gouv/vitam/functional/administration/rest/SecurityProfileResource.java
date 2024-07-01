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
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.functional.administration.common.SecurityProfile;
import fr.gouv.vitam.functional.administration.core.security.profile.SecurityProfileService;
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
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Path("/adminmanagement/v1")
@ApplicationPath("webresources")
@Tag(name = "Functional-Administration")
public class SecurityProfileResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SecurityProfileResource.class);

    public static final String SECURITY_PROFILE_URI = "/securityprofiles";

    private static final String ADMIN_MODULE = "ADMIN_MODULE";

    private static final String SECURITY_PROFILE_JSON_IS_MANDATORY_PARAMETER =
        "The json input of security profiles is mandatory";

    private final SecurityProfileService securityProfileService;

    /**
     * @param securityProfileService
     */
    public SecurityProfileResource(SecurityProfileService securityProfileService) {
        this.securityProfileService = securityProfileService;
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
     * @param securityProfileModelList as InputStream
     * @param uri the uri info
     * @return Response jersey response
     */
    @Path(SECURITY_PROFILE_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importSecurityProfiles(List<SecurityProfileModel> securityProfileModelList, @Context UriInfo uri) {
        ParametersChecker.checkParameter(SECURITY_PROFILE_JSON_IS_MANDATORY_PARAMETER, securityProfileModelList);

        try {
            RequestResponse<SecurityProfileModel> requestResponse = securityProfileService.createSecurityProfiles(
                securityProfileModelList
            );

            if (!requestResponse.isOk()) {
                ((VitamError<SecurityProfileModel>) requestResponse).setHttpCode(
                        Response.Status.BAD_REQUEST.getStatusCode()
                    );
                return Response.status(Response.Status.BAD_REQUEST).entity(requestResponse).build();
            } else {
                return Response.created(uri.getRequestUri().normalize()).entity(requestResponse).build();
            }
        } catch (VitamException exp) {
            LOGGER.error(exp);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(getErrorEntity(Response.Status.BAD_REQUEST, exp.getMessage(), null))
                .build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Response.Status.INTERNAL_SERVER_ERROR, exp.getMessage(), null))
                .build();
        }
    }

    /**
     * Find security profiles by queryDsl
     *
     * @param queryDsl
     * @return Response
     */
    @GET
    @Path(SECURITY_PROFILE_URI)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findSecurityProfiles(JsonNode queryDsl) {
        try {
            RequestResponseOK<SecurityProfileModel> securityProfileModelList = securityProfileService
                .findSecurityProfiles(queryDsl)
                .setQuery(queryDsl);

            return Response.status(Response.Status.OK).entity(securityProfileModelList).build();
        } catch (final Exception e) {
            LOGGER.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage(), null))
                .build();
        }
    }

    /**
     * Find security profile by identifier
     *
     * @param identifier the identifier of the security profile
     * @return Response
     */
    @GET
    @Path(SECURITY_PROFILE_URI + "/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findSecurityProfileByIdentifier(@PathParam("id") String identifier) {
        try {
            final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
            parser.parse(new Select().getFinalSelect());
            parser.addCondition(QueryHelper.eq(SecurityProfile.IDENTIFIER, identifier));
            JsonNode queryDsl = parser.getRequest().getFinalSelect();

            RequestResponseOK<SecurityProfileModel> securityProfileModelList = securityProfileService
                .findSecurityProfiles(queryDsl)
                .setQuery(queryDsl);

            return Response.status(Response.Status.OK).entity(securityProfileModelList).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage(), null))
                .build();
        }
    }

    @Path(SECURITY_PROFILE_URI + "/{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSecurityProfile(@PathParam("id") String identifier, JsonNode queryDsl) {
        try {
            RequestResponse<SecurityProfileModel> requestResponse = securityProfileService.updateSecurityProfile(
                identifier,
                queryDsl
            );
            if (Response.Status.NOT_FOUND.getStatusCode() == requestResponse.getHttpCode()) {
                ((VitamError<SecurityProfileModel>) requestResponse).setHttpCode(
                        Response.Status.NOT_FOUND.getStatusCode()
                    );
                return Response.status(Response.Status.NOT_FOUND).entity(requestResponse).build();
            } else if (!requestResponse.isOk()) {
                ((VitamError<SecurityProfileModel>) requestResponse).setHttpCode(
                        Response.Status.BAD_REQUEST.getStatusCode()
                    );
                return Response.status(Response.Status.BAD_REQUEST).entity(requestResponse).build();
            } else {
                return Response.status(Response.Status.OK).entity(requestResponse).build();
            }
        } catch (VitamException exp) {
            LOGGER.error(exp);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(getErrorEntity(Response.Status.BAD_REQUEST, exp.getMessage(), null))
                .build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Response.Status.INTERNAL_SERVER_ERROR, exp.getMessage(), null))
                .build();
        }
    }

    /**
     * Delete security profile by identifier
     *
     * @param securityProfileId the identifier of the security profile
     * @return Response
     */
    Response deleteSecurityProfile(String securityProfileId) {
        try {
            RequestResponse<SecurityProfileModel> requestResponse = securityProfileService.deleteSecurityProfile(
                securityProfileId
            );
            if (Response.Status.NOT_FOUND.getStatusCode() == requestResponse.getHttpCode()) {
                return Response.status(Response.Status.NOT_FOUND).entity(requestResponse).build();
            }
            if (Response.Status.FORBIDDEN.getStatusCode() == requestResponse.getHttpCode()) {
                return Response.status(Response.Status.FORBIDDEN).entity(requestResponse).build();
            }
            if (!requestResponse.isOk()) {
                return Response.status(Response.Status.BAD_REQUEST).entity(requestResponse).build();
            }

            return Response.status(Response.Status.NO_CONTENT).entity(requestResponse).build();
        } catch (VitamException exp) {
            LOGGER.error(exp);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(getErrorEntity(Response.Status.BAD_REQUEST, exp.getMessage(), null))
                .build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(getErrorEntity(Response.Status.INTERNAL_SERVER_ERROR, exp.getMessage(), null))
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
    private VitamError<SecurityProfileModel> getErrorEntity(Response.Status status, String message, String code) {
        String aMessage = (message != null && !message.trim().isEmpty())
            ? message
            : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        String aCode = (code != null) ? code : String.valueOf(status.getStatusCode());
        return new VitamError<SecurityProfileModel>(aCode)
            .setHttpCode(status.getStatusCode())
            .setContext(ADMIN_MODULE)
            .setState("code_vitam")
            .setMessage(status.getReasonPhrase())
            .setDescription(aMessage);
    }
}
