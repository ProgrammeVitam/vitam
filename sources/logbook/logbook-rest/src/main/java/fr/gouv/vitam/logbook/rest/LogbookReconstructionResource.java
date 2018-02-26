package fr.gouv.vitam.logbook.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.annotations.VisibleForTesting;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuthenticationLevel;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.security.rest.VitamAuthentication;
import fr.gouv.vitam.logbook.common.model.reconstruction.ReconstructionRequestItem;
import fr.gouv.vitam.logbook.common.model.reconstruction.ReconstructionResponseItem;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.VitamRepositoryProvider;
import fr.gouv.vitam.logbook.common.server.reconstruction.ReconstructionService;

/**
 * Logbook reconstruction resource.
 */
@Path("/logbook/v1")
public class LogbookReconstructionResource {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookReconstructionResource.class);

    private final String RECONSTRUCTION_OPERATIONS_URI = "/reconstruction/operations";

    /**
     * Error/Exceptions messages.
     */
    private static final String RECONSTRUCTION_JSON_MONDATORY_PARAMETERS_MSG =
        "the Json input of reconstruction's parameters is mondatory.";
    private static final String RECONSTRUCTION_EXCEPTION_MSG =
        "ERROR: Exception has been thrown when reconstructing Vitam collections: ";

    /**
     * Reconstruction service.
     */
    private ReconstructionService reconstructionService;

    /**
     * Constructor
     * 
     * @param vitamRepositoryProvider vitamRepositoryProvider
     */
    public LogbookReconstructionResource(
        VitamRepositoryProvider vitamRepositoryProvider) {
        this.reconstructionService = new ReconstructionService(vitamRepositoryProvider);
    }

    /**
     * Constructor for tests
     * 
     * @param reconstructionService reconstructionService
     */
    @VisibleForTesting
    public LogbookReconstructionResource(ReconstructionService reconstructionService) {
        this.reconstructionService = reconstructionService;
    }

    /**
     * API to access and launch the Vitam reconstruction service for logbook operations.<br/>
     *
     * @param reconstructionItems list of reconstruction request items
     * @return the response
     */
    @Path(RECONSTRUCTION_OPERATIONS_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response reconstructCollection(List<ReconstructionRequestItem> reconstructionItems) {
        ParametersChecker.checkParameter(RECONSTRUCTION_JSON_MONDATORY_PARAMETERS_MSG, reconstructionItems);

        List<ReconstructionResponseItem> responses = new ArrayList<>();
        if (!reconstructionItems.isEmpty()) {
            LOGGER.debug(String
                .format("Starting reconstruction Vitam service with the json parameters : (%s)", reconstructionItems));

            reconstructionItems.forEach(item -> {
                LOGGER.debug(String.format(
                    "Starting reconstruction for the collection {%s} on the tenant (%s) from the offset (%s) with (%s) elements",
                    LogbookCollections.OPERATION.name(), item.getTenant(), item.getOffset(), item.getLimit()));
                try {
                    responses.add(reconstructionService.reconstruct(item));
                } catch (DatabaseException | IllegalArgumentException e) {
                    LOGGER.error(RECONSTRUCTION_EXCEPTION_MSG, e);
                    responses.add(new ReconstructionResponseItem(item, StatusCode.KO));
                }
            });
        }

        return Response.ok().entity(responses).build();
    }
}
