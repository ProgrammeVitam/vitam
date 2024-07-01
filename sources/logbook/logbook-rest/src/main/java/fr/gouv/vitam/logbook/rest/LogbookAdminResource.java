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
package fr.gouv.vitam.logbook.rest;

import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AuthenticationLevel;
import fr.gouv.vitam.common.security.rest.VitamAuthentication;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.administration.core.api.LogbookCheckConsistencyService;
import fr.gouv.vitam.logbook.administration.core.impl.LogbookCheckConsistencyServiceImpl;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookCheckResult;
import fr.gouv.vitam.logbook.common.server.config.LogbookConfiguration;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/logbook/v1")
@Tag(name = "Logbook")
public class LogbookAdminResource {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookAdminResource.class);

    private final String CHECK_LOGBOOK_COHERENCE_URI = "/checklogbook";

    /**
     * logbookCoherenceCheck service.
     */
    private LogbookCheckConsistencyService checkLogbookService;

    public LogbookAdminResource(VitamRepositoryProvider vitamRepositoryProvider, LogbookConfiguration configuration) {
        this.checkLogbookService = new LogbookCheckConsistencyServiceImpl(configuration, vitamRepositoryProvider);
    }

    /**
     * API to access and lanch the Check logbook coherence service.<br/>
     *
     * @return OK or error
     */
    @Path(CHECK_LOGBOOK_COHERENCE_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @VitamAuthentication(authentLevel = AuthenticationLevel.BASIC_AUTHENT)
    public Response checkLogbookCoherence() {
        LOGGER.debug("Starting Check logbook coherence service :");
        try {
            LogbookCheckResult response = checkLogbookService.logbookCoherenceCheckByTenant(
                VitamThreadUtils.getVitamSession().getTenantId()
            );
            return Response.ok().entity(response).build();
        } catch (VitamException exc) {
            Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
            VitamError error = VitamCodeHelper.toVitamError(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR, exc.getMessage());
            return Response.status(status).entity(error).build();
        }
    }
}
