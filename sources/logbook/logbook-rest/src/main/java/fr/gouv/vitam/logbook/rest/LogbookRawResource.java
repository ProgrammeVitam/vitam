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

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.server.config.ElasticsearchLogbookIndexManager;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookRepositoryService;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/logbook/v1")
@Tag(name = "Logbook")
public class LogbookRawResource {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookRawResource.class);

    /**
     * LogbookRepository service.
     */
    private final LogbookRepositoryService logbookRepositoryService;

    public LogbookRawResource(
        VitamRepositoryProvider vitamRepositoryProvider,
        ElasticsearchLogbookIndexManager indexManager
    ) {
        this.logbookRepositoryService = new LogbookRepositoryService(vitamRepositoryProvider, indexManager);
    }

    /**
     * Lifecycle Unit Bulk Create raw JsonNode objects
     *
     * @param logbookLifecycles Lifecycle Unit Logbooks as list of jsonNodes
     * @return Response of CREATED
     */
    @POST
    @Path("raw/unitlifecycles/bulk")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createLifeCycleUnitBulkRaw(List<JsonNode> logbookLifecycles) {
        return createLifecycleBulk(LogbookCollections.LIFECYCLE_UNIT, logbookLifecycles);
    }

    /**
     * Lifecycle object group Bulk Create raw JsonNode objects
     *
     * @param logbookLifecycles Lifecycle objectGroup Logbooks as list of jsonNodes
     * @return Response of CREATED
     */
    @POST
    @Path("raw/objectgrouplifecycles/bulk")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createLifeCycleObjectGroupBulkRaw(List<JsonNode> logbookLifecycles) {
        return createLifecycleBulk(LogbookCollections.LIFECYCLE_OBJECTGROUP, logbookLifecycles);
    }

    /**
     * Lifecycle Bulk Create raw JsonNode objects
     *
     * @param collection lifecycle collection
     * @param logbookLifecycles Lifecycle Logbooks as list of jsonNodes
     * @return Response of CREATED
     */
    private Response createLifecycleBulk(LogbookCollections collection, List<JsonNode> logbookLifecycles) {
        ParametersChecker.checkParameter("Logbook parameters", logbookLifecycles);
        try {
            logbookRepositoryService.saveBulk(collection, logbookLifecycles);
        } catch (DatabaseException e) {
            LOGGER.error("Lifecycles could not be inserted in database", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.status(Response.Status.CREATED).build();
    }
}
