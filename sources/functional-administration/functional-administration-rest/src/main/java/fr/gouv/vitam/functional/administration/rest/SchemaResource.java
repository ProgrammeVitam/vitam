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

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.administration.schema.SchemaInputModel;
import fr.gouv.vitam.common.model.administration.schema.SchemaModel;
import fr.gouv.vitam.common.model.administration.schema.SchemaResponse;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.core.schema.SchemaService;
import fr.gouv.vitam.functional.administration.utils.ResponseErrorUtils;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.List;

@Path("/adminmanagement/v1")
@ApplicationPath("webresources")
@Tag(name = "Functional-Administration")
public class SchemaResource {

    private static final String FUNCTIONAL_ADMINISTRATION_MODULE = "FUNCTIONAL_ADMINISTRATION_MODULE";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SchemaResource.class);
    public static final String UNIT_SCHEMA_URI = "/schema/unit";
    public static final String OBJECTGROUP_SCHEMA_URI = "/schema/objectgroup";
    private static final String SCHEMA_JSON_IS_MANDATORY_PARAMETER =
        "The json input of external schema type is mandatory";

    private final SchemaService schemaService;

    public SchemaResource(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    /**
     * Api to return unit schema
     *
     * @return
     */
    @Path(UNIT_SCHEMA_URI)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response unitSchema() {
        try {
            LOGGER.info(" retrieving unit schema elements");
            List<SchemaResponse> unitSchema = schemaService.findUnitSchema();
            return Response.ok(unitSchema).build();
        } catch (
            InvalidParseOperationException | IOException | ReferentialException | InvalidCreateOperationException e
        ) {
            LOGGER.error("Cannot retrieve unit schema ", e);
            return Response.serverError().build();
        }
    }

    @Path(OBJECTGROUP_SCHEMA_URI)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response objectGroupSchema() {
        try {
            LOGGER.info(" retrieving object group schema elements");
            List<SchemaResponse> objectGroupSchema = schemaService.findObjectGroupInternalSchema();
            return Response.ok(objectGroupSchema).build();
        } catch (InvalidParseOperationException | IOException | ReferentialException e) {
            LOGGER.error("Cannot retrieve object group schema ", e);
            return Response.serverError().build();
        }
    }

    /**
     * Import a set of external schema. </BR>
     *
     * The input is invalid in the following situations : </BR>
     * <ul>
     * <li>The json is invalid</li>
     * <li>The json contains an already used path</li>
     * <li>One or more mandatory field is missing</li>
     * <li>A field has an invalid format</li>
     * </ul>
     *
     * @param externalSchemaList as InputStream
     * @param uri the uri info
     * @return Response
     */
    @Path(UNIT_SCHEMA_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importExternalSchemaElements(List<SchemaInputModel> externalSchemaList, @Context UriInfo uri) {
        ParametersChecker.checkParameter(SCHEMA_JSON_IS_MANDATORY_PARAMETER, externalSchemaList);

        try {
            RequestResponse<SchemaModel> requestResponse = schemaService.importExternalSchemaElements(
                externalSchemaList
            );

            if (!requestResponse.isOk()) {
                return Response.status(requestResponse.getHttpCode()).entity(requestResponse).build();
            } else {
                return Response.accepted(uri.getRequestUri().normalize()).entity(requestResponse).build();
            }
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    ResponseErrorUtils.getErrorEntity(
                        Response.Status.INTERNAL_SERVER_ERROR,
                        exp.getMessage(),
                        FUNCTIONAL_ADMINISTRATION_MODULE
                    )
                )
                .build();
        }
    }

    @Path(UNIT_SCHEMA_URI)
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUnitExternalSchemas(List<String> paths) {
        ParametersChecker.checkParameter("The unit external schema paths list is mandatory", paths);

        try {
            Integer tenantId = ParameterHelper.getTenantParameter();
            schemaService.checkAndDeleteExternalSchemaElementsByPaths(
                paths,
                tenantId.equals(VitamConfiguration.getAdminTenant())
            );
            return Response.status(Response.Status.OK).build();
        } catch (BadRequestException exp) {
            LOGGER.error("Bad Request Error {}", exp);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(
                    ResponseErrorUtils.getErrorEntity(
                        Response.Status.BAD_REQUEST,
                        exp.getMessage(),
                        FUNCTIONAL_ADMINISTRATION_MODULE
                    )
                )
                .build();
        } catch (Exception exp) {
            LOGGER.error("Unexpected server error {}", exp);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    ResponseErrorUtils.getErrorEntity(
                        Response.Status.INTERNAL_SERVER_ERROR,
                        exp.getMessage(),
                        FUNCTIONAL_ADMINISTRATION_MODULE
                    )
                )
                .build();
        }
    }
}
