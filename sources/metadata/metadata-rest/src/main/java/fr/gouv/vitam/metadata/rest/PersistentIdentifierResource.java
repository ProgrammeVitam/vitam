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

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.metadata.api.model.PersistentIdentifierReconstructionRequest;
import fr.gouv.vitam.metadata.core.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.core.reconstruction.domain.OffsetManager;
import fr.gouv.vitam.metadata.core.reconstruction.domain.PersistentIdentifierReconstructionManager;
import fr.gouv.vitam.metadata.core.reconstruction.model.PurgedPersistentIdentifier;
import fr.gouv.vitam.metadata.core.reconstruction.repository.PersistentIdentifierRepository;
import fr.gouv.vitam.metadata.core.reconstruction.repository.ReconstructionResponse;
import fr.gouv.vitam.metadata.core.reconstruction.service.PersistentIdentifierReconstructionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static fr.gouv.vitam.metadata.core.reconstruction.repository.ReconstructionResponse.ReconstructionStatus.FAILURE;

@Path("/v1")
@Tag(name = "Metadata")
public class PersistentIdentifierResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PersistentIdentifierResource.class);

    private static final String PERSISTENT_IDENTIFIER_RECONSTRUCTION_URI = "/reconstruction-persistent-identifier";
    private static final String PERSISTENT_IDENTIFIER_URI = "/persistentIdentifier";
    private static final String PERSISTENT_IDENTIFIER_RECONSTRUCTION_JSON_MANDATORY_PARAMETERS_MSG =
        "the Json input of persistent identifier reconstruction's parameters is mandatory.";

    private final PersistentIdentifierReconstructionService persistentIdentifierReconstructionService;
    private final PersistentIdentifierRepository persistentIdentifierRepository;

    PersistentIdentifierResource(
        PersistentIdentifierReconstructionManager persistentIdentifierReconstructionManager,
        OffsetManager offsetManager,
        MetaDataConfiguration metaDataConfiguration,
        PersistentIdentifierRepository persistentIdentifierRepository
    ) {
        this.persistentIdentifierRepository = persistentIdentifierRepository;
        this.persistentIdentifierReconstructionService = new PersistentIdentifierReconstructionService(
            offsetManager,
            persistentIdentifierReconstructionManager,
            metaDataConfiguration
        );
    }

    /**
     * API to access and launch the Vitam reconstruction service for Persistent Identifiers.<br/>
     *
     * @param requestItem request item
     * @return the response
     */
    @Path(PERSISTENT_IDENTIFIER_RECONSTRUCTION_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reconstructPersistentIdentifiers(PersistentIdentifierReconstructionRequest requestItem) {
        ParametersChecker.checkParameter(
            PERSISTENT_IDENTIFIER_RECONSTRUCTION_JSON_MANDATORY_PARAMETERS_MSG,
            requestItem
        );

        LOGGER.debug(
            String.format("Starting reconstruction Vitam service with the json parameters : (%s)", requestItem)
        );

        ReconstructionResponse reconstructionResponse;
        try {
            reconstructionResponse = persistentIdentifierReconstructionService.reconstruct(requestItem);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Persistent identifier reconstruction failed", e);
            reconstructionResponse = new ReconstructionResponse.Builder().status(FAILURE).build();
        }

        switch (reconstructionResponse.status) {
            case SUCCESS:
                return Response.ok(
                    new RequestResponseOK<ReconstructionResponse>().addResult(reconstructionResponse)
                ).build();
            default:
                return Response.serverError()
                    .entity(new RequestResponseOK<ReconstructionResponse>().addResult(reconstructionResponse))
                    .build();
        }
    }

    /**
     * API to get purged persistent identifiers<br/>
     *
     * @param persistentIdentifier persistent identifier
     * @param type Purged collection type
     * @return the response
     */
    @Path("/purgedPersistentIdentifier/{persistentIdentifier:.+}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPersistentIdentifiers(
        @PathParam("persistentIdentifier") String persistentIdentifier,
        @QueryParam("type") @Nullable String type
    ) {
        ParametersChecker.checkParameter(
            PERSISTENT_IDENTIFIER_RECONSTRUCTION_JSON_MANDATORY_PARAMETERS_MSG,
            persistentIdentifier
        );
        final Integer tenant = ParameterHelper.getTenantParameter();
        try {
            final List<PurgedPersistentIdentifier> purgedPersistentIdentifiers =
                persistentIdentifierRepository.findByPersistentIdentifierAndTenant(persistentIdentifier, tenant, type);

            return Response.ok().entity(purgedPersistentIdentifiers).build();
        } catch (DatabaseException e) {
            LOGGER.error(
                "Internal Server error : cannot retrieve persistent identifiers > persistent identifier : {}, tenant {} ",
                persistentIdentifier,
                tenant
            );
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
