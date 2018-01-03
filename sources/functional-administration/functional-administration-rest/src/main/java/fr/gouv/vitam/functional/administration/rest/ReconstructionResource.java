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

import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuthenticationLevel;
import fr.gouv.vitam.common.security.rest.VitamAuthentication;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.functional.administration.common.VitamRepositoryProvider;
import fr.gouv.vitam.functional.administration.common.ReconstructionItem;
import fr.gouv.vitam.functional.administration.common.api.ReconstructionService;
import fr.gouv.vitam.functional.administration.common.impl.ReconstructionServiceImpl;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;

/**
 * reconstruction Service.
 */
@Path("/adminmanagement/v1")
@ApplicationPath("webresources")
public class ReconstructionResource {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReconstructionResource.class);

    /**
     * Error/Exceptions messages.
     */
    private static final String RECONSTRUCTION_JSON_MONDATORY_PARAMETERS_MSG =
        "the Json input of reconstruction's parameters is mondatory.";
    private static final String BASIC_AUTHENTICATION_MONDATORY_MSG =
        "VitamAuthentication failed! The reconstruction service needs Basic-VitamAuthentication.";
    private static final String RECONSTRUCTION_EXCEPTION_MSG =
        "ERROR: Exception has been thrown when reconstructing Vitam collections: ";

    /**
     * VitamAdmin configuration.
     */
    private AdminManagementConfiguration configuration;

    /**
     * Reconstruction service.
     */
    private ReconstructionService reconstructionService;

    /**
     * Reconstruction factory.
     */
    private VitamRepositoryProvider vitamRepositoryProvider;

    public ReconstructionResource(AdminManagementConfiguration adminManagementConfig,
        VitamRepositoryProvider vitamRepositoryProvider) {
        this(adminManagementConfig,new ReconstructionServiceImpl(adminManagementConfig, vitamRepositoryProvider));
    }

    @VisibleForTesting
    public ReconstructionResource(AdminManagementConfiguration adminManagementConfig,
        ReconstructionService reconstructionService) {
        this.configuration = adminManagementConfig;
        this.reconstructionService = reconstructionService;
    }

    /**
     * API to access and lanch the Vitam reconstruction service.<br/>
     *
     * @param headers
     * @param reconstructionItems
     * @return the response
     */
    @Path("/reconstruction")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response reconstructCollections(@Context HttpHeaders headers,
        @Valid List<ReconstructionItem> reconstructionItems) {

        // validate basic authentication
        HttpHeaderHelper.checkVitamHeaders(headers);
        ParametersChecker
            .checkParameter(BASIC_AUTHENTICATION_MONDATORY_MSG, headers.getRequestHeader(HttpHeaders.AUTHORIZATION));

        ParametersChecker.checkParameter(RECONSTRUCTION_JSON_MONDATORY_PARAMETERS_MSG, reconstructionItems);
        LOGGER.debug(String
            .format("Starting reconstruction Vitam service with the json parameters : (%s)", reconstructionItems));

        // reconstruction of list of collection on a given list of tenants
        reconstructionItems.forEach(r -> {
            LOGGER.debug(String.format("Starting reconstruction for the collection {%s} on the tenants : (%s)",
                r.getCollection(), r.getTenants()));
            try {
                // reconstruction of the given collection on the given list of tenants
                reconstructionService.reconstruct(r.getCollection(), r.getTenants().stream().toArray(Integer[]::new));
            } catch (DatabaseException e) {
                LOGGER.error(RECONSTRUCTION_EXCEPTION_MSG, e);
            }
        });

        return Response.ok().build();
    }
}
