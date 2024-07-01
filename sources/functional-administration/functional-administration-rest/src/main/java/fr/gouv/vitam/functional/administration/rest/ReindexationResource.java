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

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.collections.VitamCollectionHelper;
import fr.gouv.vitam.common.database.index.model.ReindexationOK;
import fr.gouv.vitam.common.database.index.model.ReindexationResult;
import fr.gouv.vitam.common.database.index.model.SwitchIndexResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexSettings;
import fr.gouv.vitam.common.database.server.elasticsearch.IndexationHelper;
import fr.gouv.vitam.common.error.ServiceName;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;

import javax.validation.Valid;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Path("/adminmanagement/v1")
@ApplicationPath("webresources")
@Tag(name = "Functional-Administration")
public class ReindexationResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReindexationResource.class);
    private static final String OPTIONS_IS_MANDATORY_PARAMETER = "Parameters are mandatory";

    private static final String REINDEXATION_EXCEPTION_MSG =
        "ERROR: Exception has been thrown when reindexing Vitam collections: ";

    private static final String REINDEX_URI = "/reindex";
    private static final String ALIASES_URI = "/alias";

    /**
     * logbookOperationsClientFactory
     */
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;

    /**
     * metaDataClientFactory
     */
    private final MetaDataClientFactory metaDataClientFactory;

    private final IndexationHelper indexationHelper;

    private final ElasticsearchFunctionalAdminIndexManager indexManager;

    @VisibleForTesting
    public ReindexationResource(
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        MetaDataClientFactory metaDataClientFactory,
        IndexationHelper indexationHelper,
        ElasticsearchFunctionalAdminIndexManager indexManager
    ) {
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.metaDataClientFactory = metaDataClientFactory;
        this.indexationHelper = indexationHelper;
        this.indexManager = indexManager;
        LOGGER.debug("init Reindexation Resource server");
    }

    public ReindexationResource(ElasticsearchFunctionalAdminIndexManager indexManager) {
        this.indexManager = indexManager;
        logbookOperationsClientFactory = LogbookOperationsClientFactory.getInstance();
        metaDataClientFactory = MetaDataClientFactory.getInstance();
        this.indexationHelper = IndexationHelper.getInstance();
        LOGGER.debug("init Reindexation Resource server");
    }

    /**
     * Reindex a collection
     *
     * @param indexParameters parameters specifying what to reindex
     * @return Response
     */
    @Path(REINDEX_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reindex(@Valid List<IndexParameters> indexParameters) {
        ParametersChecker.checkParameter(OPTIONS_IS_MANDATORY_PARAMETER, indexParameters);

        List<ReindexationResult> results = new ArrayList<>();
        // call the reindexation service
        for (IndexParameters index : indexParameters) {
            ReindexationResult indexationResult;
            if (VitamCollectionHelper.isLogbookCollection(index.getCollectionName())) {
                indexationResult = reindexLogbookCollection(index);
            } else if (VitamCollectionHelper.isMetadataCollection(index.getCollectionName())) {
                indexationResult = reindexMasterDataCollection(index);
            } else {
                indexationResult = reindexFunctionalAdminCollection(index);
            }
            results.add(indexationResult);
        }

        boolean atLeastOneOK = results
            .stream()
            .anyMatch(indexationResult -> CollectionUtils.isNotEmpty(indexationResult.getIndexOK()));

        boolean atLeastOneKO = results
            .stream()
            .anyMatch(indexationResult -> CollectionUtils.isNotEmpty(indexationResult.getIndexKO()));

        if (atLeastOneKO && !atLeastOneOK) {
            final Status returnedStatus = Status.INTERNAL_SERVER_ERROR;
            return Response.status(returnedStatus)
                .entity(
                    new VitamError(returnedStatus.name())
                        .setHttpCode(returnedStatus.getStatusCode())
                        .setContext(ServiceName.FUNCTIONAL_ADMINISTRATION.getName())
                        .setState("code_vitam")
                        .setMessage(JsonHandler.unprettyPrint(results))
                        .setDescription("Internal error.")
                )
                .build();
        } else if (atLeastOneKO) {
            return Response.status(Status.ACCEPTED)
                .entity(
                    new RequestResponseOK<ReindexationResult>()
                        .addAllResults(results)
                        .setHttpCode(Status.ACCEPTED.getStatusCode())
                )
                .build();
        } else {
            return Response.status(Status.CREATED)
                .entity(
                    new RequestResponseOK<ReindexationResult>()
                        .addAllResults(results)
                        .setHttpCode(Status.CREATED.getStatusCode())
                )
                .build();
        }
    }

    private ReindexationResult reindexLogbookCollection(IndexParameters indexParameters) {
        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {
            return logbookOperationsClient.reindex(indexParameters);
        } catch (Exception e) {
            LOGGER.error(REINDEXATION_EXCEPTION_MSG, e);
            return indexationHelper.getFullKOResult(indexParameters, REINDEXATION_EXCEPTION_MSG);
        }
    }

    private ReindexationResult reindexMasterDataCollection(IndexParameters indexParameters) {
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            return JsonHandler.getFromJsonNode(metaDataClient.reindex(indexParameters), ReindexationResult.class);
        } catch (Exception e) {
            LOGGER.error(REINDEXATION_EXCEPTION_MSG, e);
            return indexationHelper.getFullKOResult(indexParameters, REINDEXATION_EXCEPTION_MSG);
        }
    }

    private ReindexationResult reindexFunctionalAdminCollection(IndexParameters indexParameters) {
        // Reindex of the given collection
        FunctionalAdminCollections collectionToReindex;
        try {
            collectionToReindex = FunctionalAdminCollections.valueOf(indexParameters.getCollectionName());
        } catch (IllegalArgumentException ex) {
            String message = String.format(
                "Try to reindex an unknown collection %s",
                indexParameters.getCollectionName()
            );
            LOGGER.error(message, ex);
            return indexationHelper.getFullKOResult(indexParameters, message);
        }

        if (CollectionUtils.isNotEmpty(indexParameters.getTenants())) {
            LOGGER.warn("Ignoring tenant list for collection " + collectionToReindex);
        }

        MongoCollection<Document> mongoCollection = collectionToReindex.getCollection();

        try {
            ElasticsearchIndexAlias indexAlias =
                this.indexManager.getElasticsearchIndexAliasResolver(collectionToReindex).resolveIndexName(null);
            ElasticsearchIndexSettings indexSettings =
                this.indexManager.getElasticsearchIndexSettings(collectionToReindex);

            ReindexationOK reindexResult = indexationHelper.reindex(
                mongoCollection,
                collectionToReindex.getEsClient(),
                indexAlias,
                indexSettings,
                collectionToReindex.getElasticsearchCollection(),
                null,
                null
            );

            ReindexationResult indexationResult = new ReindexationResult();
            indexationResult.setCollectionName(indexParameters.getCollectionName());
            indexationResult.setIndexOK(Collections.singletonList(reindexResult));
            return indexationResult;
        } catch (Exception exc) {
            LOGGER.error("Cannot reindex collection " + collectionToReindex.name() + ". Unexpected error");
            return indexationHelper.getFullKOResult(indexParameters, exc.getMessage());
        }
    }

    /**
     * Switch indexes
     *
     * @param switchIndexParameters specifying how to switch indexes
     * @return Response
     */
    @Path(ALIASES_URI)
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response switchIndexes(@Valid List<SwitchIndexParameters> switchIndexParameters) {
        ParametersChecker.checkParameter(OPTIONS_IS_MANDATORY_PARAMETER, switchIndexParameters);
        List<SwitchIndexResult> results = new ArrayList<>();
        // call the switch service
        switchIndexParameters.forEach(switchIndex -> {
            try {
                SwitchIndexResult switchIndexResult;
                if (VitamCollectionHelper.isLogbookCollection(switchIndex.getCollectionName())) {
                    switchIndexResult = switchLogbookCollectionAlias(switchIndex);
                } else if (VitamCollectionHelper.isMetadataCollection(switchIndex.getCollectionName())) {
                    switchIndexResult = switchMetadataCollectionAlias(switchIndex);
                } else {
                    switchIndexResult = switchFunctionalAdminCollection(switchIndex);
                }

                results.add(switchIndexResult);
            } catch (Exception e) {
                String message = String.format(
                    "Error while switching indexes for alias '%s' into '%s'",
                    switchIndex.getAlias(),
                    switchIndex.getIndexName()
                );
                LOGGER.error(message, e);
                results.add(indexationHelper.getKOResult(switchIndex, message));
            }
        });

        boolean atLeastOneOK = results
            .stream()
            .anyMatch(switchIndexResult -> switchIndexResult.getStatusCode() == StatusCode.OK);

        boolean atLeastOneKO = results
            .stream()
            .anyMatch(switchIndexResult -> switchIndexResult.getStatusCode() != StatusCode.OK);

        if (atLeastOneKO && !atLeastOneOK) {
            final Status returnedStatus = Status.INTERNAL_SERVER_ERROR;
            return Response.status(returnedStatus)
                .entity(
                    new VitamError(returnedStatus.name())
                        .setHttpCode(returnedStatus.getStatusCode())
                        .setContext(ServiceName.FUNCTIONAL_ADMINISTRATION.getName())
                        .setState("code_vitam")
                        .setMessage(JsonHandler.unprettyPrint(results))
                        .setDescription("Internal error.")
                )
                .build();
        } else if (atLeastOneKO) {
            return Response.status(Status.ACCEPTED)
                .entity(
                    new RequestResponseOK<SwitchIndexResult>()
                        .addAllResults(results)
                        .setHttpCode(Status.ACCEPTED.getStatusCode())
                )
                .build();
        } else {
            return Response.status(Status.OK)
                .entity(
                    new RequestResponseOK<SwitchIndexResult>()
                        .addAllResults(results)
                        .setHttpCode(Status.OK.getStatusCode())
                )
                .build();
        }
    }

    private SwitchIndexResult switchFunctionalAdminCollection(SwitchIndexParameters switchIndex)
        throws DatabaseException {
        ElasticsearchIndexAlias alias = ElasticsearchIndexAlias.ofFullIndexName(switchIndex.getAlias());
        ElasticsearchIndexAlias newIndex = ElasticsearchIndexAlias.ofFullIndexName(switchIndex.getIndexName());

        return indexationHelper.switchIndex(alias, newIndex, FunctionalAdminCollections.ACCESS_CONTRACT.getEsClient()); //We need an Es client, we take this one
    }

    private SwitchIndexResult switchLogbookCollectionAlias(SwitchIndexParameters switchIndex) {
        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {
            return logbookOperationsClient.switchIndexes(switchIndex);
        } catch (Exception e) {
            String message = "Error while switching indexes on logbook collection " + switchIndex.getAlias();
            LOGGER.error(message, e);
            return indexationHelper.getKOResult(switchIndex, message);
        }
    }

    private SwitchIndexResult switchMetadataCollectionAlias(SwitchIndexParameters switchIndex) {
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            return metaDataClient.switchIndexes(switchIndex);
        } catch (Exception e) {
            String message = "Error while switching indexes on metadata collection " + switchIndex.getAlias();
            LOGGER.error(message, e);
            return indexationHelper.getKOResult(switchIndex, message);
        }
    }
}
