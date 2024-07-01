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

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.GraphComputeResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.model.ReconstructionRequestItem;
import fr.gouv.vitam.metadata.api.model.ReconstructionResponseItem;
import fr.gouv.vitam.metadata.core.config.ElasticsearchMetadataIndexManager;
import fr.gouv.vitam.metadata.core.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.graph.StoreGraphService;
import fr.gouv.vitam.metadata.core.metrics.MetadataReconstructionMetricsCache;
import fr.gouv.vitam.metadata.core.reconstruction.ReconstructionService;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Path("/v1")
@Tag(name = "Metadata")
public class MetadataReconstructionResource {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetadataManagementResource.class);

    public static final String OBJECTGROUP = "OBJECTGROUP";
    public static final String UNIT = "UNIT";

    private static final String UNIT_OBJECTGROUP = UNIT + "_" + OBJECTGROUP;

    private static final String RECONSTRUCTION_URI = "/reconstruction";
    private static final String STORE_GRAPH_URI = "/storegraph";
    private static final String PURGE_GRAPH_ONLY_DOCUMENTS_URI = "/purgeGraphOnlyDocuments";
    private static final String STORE_GRAPH_PROGRESS_URI = "/storegraph/progress";

    /**
     * Error/Exceptions messages.
     */
    private static final String RECONSTRUCTION_JSON_MANDATORY_PARAMETERS_MSG =
        "the Json input of reconstruction's parameters is mandatory.";
    private static final String RECONSTRUCTION_EXCEPTION_MSG =
        "ERROR: Exception has been thrown when reconstructing Vitam collections: ";
    private static final String STORE_GRAPH_EXCEPTION_MSG = "ERROR: Exception has been thrown when sotre graph: ";
    private static final String ERROR_MSG = "{\"ErrorMsg\":\"";

    private final ReconstructionService reconstructionService;
    private final StoreGraphService storeGraphService;

    MetadataReconstructionResource(
        VitamRepositoryProvider vitamRepositoryProvider,
        OffsetRepository offsetRepository,
        MetaDataConfiguration configuration,
        ElasticsearchMetadataIndexManager indexManager
    ) {
        this(
            new ReconstructionService(
                vitamRepositoryProvider,
                offsetRepository,
                indexManager,
                new MetadataReconstructionMetricsCache(
                    configuration.getReconstructionMetricsCacheDurationInMinutes(),
                    TimeUnit.MINUTES
                )
            ),
            new StoreGraphService(vitamRepositoryProvider),
            configuration
        );
    }

    @VisibleForTesting
    MetadataReconstructionResource(
        ReconstructionService reconstructionService,
        StoreGraphService storeGraphService,
        MetaDataConfiguration configuration
    ) {
        this.reconstructionService = reconstructionService;
        this.storeGraphService = storeGraphService;

        ProcessingManagementClientFactory.changeConfigurationUrl(configuration.getUrlProcessing());
    }

    /**
     * API to access and launch the Vitam reconstruction service for metadata.<br/>
     *
     * @param reconstructionItems list of reconstruction request items
     * @return the response
     */
    @Path(RECONSTRUCTION_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reconstructCollection(List<ReconstructionRequestItem> reconstructionItems) {
        ParametersChecker.checkParameter(RECONSTRUCTION_JSON_MANDATORY_PARAMETERS_MSG, reconstructionItems);

        List<ReconstructionResponseItem> responses = new ArrayList<>();
        if (!reconstructionItems.isEmpty()) {
            LOGGER.debug(
                String.format(
                    "Starting reconstruction Vitam service with the json parameters : (%s)",
                    reconstructionItems
                )
            );

            reconstructionItems.forEach(item -> {
                LOGGER.debug(
                    String.format(
                        "Starting reconstruction for the collection {%s} on the tenant (%s) with (%s) elements",
                        item.getCollection(),
                        item.getTenant(),
                        item.getLimit()
                    )
                );
                try {
                    responses.add(reconstructionService.reconstruct(item));
                } catch (IllegalArgumentException e) {
                    LOGGER.error(RECONSTRUCTION_EXCEPTION_MSG, e);
                    responses.add(new ReconstructionResponseItem(item, StatusCode.KO));
                }
            });
        }
        return Response.ok(new RequestResponseOK<ReconstructionResponseItem>().addAllResults(responses)).build();
    }

    /**
     * API to access and launch the Vitam store graph service for metadata.<br/>
     *
     * @return the response
     */
    @Path(STORE_GRAPH_URI)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response storeGraph() {
        try {
            VitamThreadUtils.getVitamSession().initIfAbsent(VitamConfiguration.getAdminTenant());

            VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
            Map<MetadataCollections, Integer> map = this.storeGraphService.tryStoreGraph();
            return Response.ok().entity(map).build();
        } catch (Exception e) {
            LOGGER.error(STORE_GRAPH_EXCEPTION_MSG, e);
            return Response.serverError().entity(ERROR_MSG + e.getMessage() + "\"}").build();
        }
    }

    /**
     * Check if store graph is in progress.<br/>
     *
     * @return the response
     */
    @Path(STORE_GRAPH_PROGRESS_URI)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response storeGraphInProgress() {
        VitamThreadUtils.getVitamSession().initIfAbsent(VitamConfiguration.getAdminTenant());

        boolean inProgress = this.storeGraphService.isInProgress();
        if (inProgress) {
            LOGGER.info("Store graph in progress ...");
            return Response.ok("{\"msg\": \"Store graph in progress ...\"}").build();
        } else {
            LOGGER.info("No active store graph");
            return Response.status(Response.Status.NOT_FOUND).entity("{\"msg\": \"No active store graph\"}").build();
        }
    }

    /**
     * API to purge documents reconstructed but having only graph data
     * This will remove all documents older than a configured delay (deleteIncompleteReconstructedUnitDelay) in vitam conf
     *
     * @return the response
     */
    @Path(PURGE_GRAPH_ONLY_DOCUMENTS_URI + "/{collection:" + UNIT + "|" + OBJECTGROUP + "|" + UNIT_OBJECTGROUP + "}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response purgeReconstructedDocumentsWithGraphOnlyData(
        @PathParam("collection") GraphComputeResponse.GraphComputeAction action
    ) {
        try {
            switch (action) {
                case UNIT:
                    reconstructionService.purgeReconstructedDocumentsWithGraphOnlyData(MetadataCollections.UNIT);
                    break;
                case OBJECTGROUP:
                    reconstructionService.purgeReconstructedDocumentsWithGraphOnlyData(MetadataCollections.OBJECTGROUP);
                    break;
                case UNIT_OBJECTGROUP:
                    reconstructionService.purgeReconstructedDocumentsWithGraphOnlyData(MetadataCollections.UNIT);
                    reconstructionService.purgeReconstructedDocumentsWithGraphOnlyData(MetadataCollections.OBJECTGROUP);
                    break;
                default:
                    throw new IllegalArgumentException("Not implemented action :" + action);
            }

            return Response.ok().build();
        } catch (Exception e) {
            LOGGER.error("Could not purge reconstructed documents with graph only data", e);
            return Response.serverError().entity(ERROR_MSG + e.getMessage() + "\"}").build();
        }
    }
}
