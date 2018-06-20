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

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.common.ReferentialAccessionRegisterSummaryUtil;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementRepositoryService;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;


/**
 * Admin management Raw resource REST API
 */
@Path("/adminmanagement/v1")
public class AdminManagementRawResource {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementRawResource.class);

    /**
     * Repository service.
     */
    private AdminManagementRepositoryService adminManagementRepositoryService;

    /**
     * Constructor
     * 
     * @param vitamRepositoryProvider vitam repository provider
     */
    public AdminManagementRawResource(VitamRepositoryProvider vitamRepositoryProvider) {
        this.adminManagementRepositoryService = new AdminManagementRepositoryService(vitamRepositoryProvider,
            new ReferentialAccessionRegisterSummaryUtil());
    }


    /**
     * Accession regisyer Create raw JsonNode objects
     *
     * @param accessionRegisterDetail accessionRegisterDetail
     * @return Response of CREATED
     */
    @POST
    @Path("raw/accession-register/detail")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAccessionRegisterRaw(JsonNode accessionRegisterDetail) {
        return create(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL, accessionRegisterDetail);
    }

    /**
     * Functionnal admin Create raw JsonNode object
     * 
     * @param collection functional admin collection
     * @param referential referential jsonNode
     * @return response of CREATED
     */
    private Response create(FunctionalAdminCollections collection, JsonNode referential) {
        ParametersChecker.checkParameter("referential parameters", referential);
        try {
            final Integer tenant = ParameterHelper.getTenantParameter();
            adminManagementRepositoryService.save(collection, referential, tenant);
        } catch (DatabaseException | InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.error("Referential could not be inserted in database", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.status(Response.Status.CREATED).build();
    }


    /**
     * Get accessionRegister details as raw data
     *
     * @param fields the fields to filter on
     * @return {@link Response} contains a request response json filled with accession register result
     * @see entity(java.lang.Object, java.lang.annotation.Annotation[])
     * @see #type(javax.ws.rs.core.MediaType)
     */
    @Path("raw/accession-register/detail")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccessionRegisterDetail(Map<String, String> fields) {
        return getByFields(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL, fields);
    }

    private Response getByFields(FunctionalAdminCollections collection, Map<String, String> fields) {
        ParametersChecker.checkParameter("fields parameters", fields);
        try {
            final Integer tenant = ParameterHelper.getTenantParameter();
            JsonNode document = adminManagementRepositoryService.getDocumentsByFields(collection, fields, tenant);
            RequestResponse<JsonNode> responseOK =
                new RequestResponseOK<JsonNode>().addResult(document).setHttpCode(Status.OK.getStatusCode());
            return Response.status(Status.OK).entity(responseOK).build();
        } catch (ReferentialNotFoundException e) {
            return VitamCodeHelper.toVitamError(VitamCode.REFERENTIAL_NOT_FOUND, String
                .format("Could not find document of type %s", collection.getName()))
                .toResponse();
        } catch (DatabaseException e) {
            return VitamCodeHelper.toVitamError(VitamCode.REFERENTIAL_REPOSITORY_DATABASE_ERROR, String
                .format("Technical error while trying to find document of type %s",
                    collection.getName()))
                .toResponse();
        } catch (InvalidParseOperationException e) {
            return VitamCodeHelper.toVitamError(VitamCode.REFERENTIAL_REPOSITORY_DATABASE_ERROR, String
                .format("Technical error while trying to parse document of type %s", collection.getName()))
                .toResponse();
        }
    }

}
