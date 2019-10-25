/*
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
 */
package fr.gouv.vitam.functional.administration.rest;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.metadata.client.MetaDataClient;
import org.bson.Document;

import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.MongoCollection;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.collections.VitamCollectionHelper;
import fr.gouv.vitam.common.database.index.model.IndexationResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.database.server.elasticsearch.IndexationHelper;
import fr.gouv.vitam.common.database.server.elasticsearch.model.ElasticsearchCollections;
import fr.gouv.vitam.common.error.ServiceName;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;

/**
 * ReindexationResource
 */
@Path("/adminmanagement/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class ReindexationResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReindexationResource.class);
    private static final String OPTIONS_IS_MANDATORY_PATAMETER =
        "Parameters are mandatory";

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
    /**
     * Constructor
     *
     * @param logbookOperationsClientFactory
     * @param metaDataClientFactory
     */
    @VisibleForTesting
    public ReindexationResource(LogbookOperationsClientFactory logbookOperationsClientFactory,
        MetaDataClientFactory metaDataClientFactory, IndexationHelper indexationHelper) {
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.metaDataClientFactory = metaDataClientFactory;
        this.indexationHelper = indexationHelper;
        LOGGER.debug("init Reindexation Resource server");
    }


    /**
     * Default Constructor
     * 
     */
    public ReindexationResource() {
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
        ParametersChecker.checkParameter(OPTIONS_IS_MANDATORY_PATAMETER, indexParameters);
        List<IndexationResult> results = new ArrayList<IndexationResult>();
        AtomicBoolean atLeastOneKO = new AtomicBoolean(false);
        AtomicBoolean atLeastOneOK = new AtomicBoolean(false);
        // call the reindexation service
        indexParameters.forEach(index -> {
            try (MetaDataClient metaDataClient = metaDataClientFactory.getClient(); LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()){
                if (VitamCollectionHelper.isLogbookCollection(index.getCollectionName())) {
                    results.add(JsonHandler.getFromJsonNode(logbookOperationsClient.reindex(index), IndexationResult.class));
                    atLeastOneOK.set(true);
                } else if (VitamCollectionHelper.isMetadataCollection(index.getCollectionName())) {
                    results.add(JsonHandler.getFromJsonNode(metaDataClient.reindex(index), IndexationResult.class));
                    atLeastOneOK.set(true);
                } else {
                    // Reindex of the given collection
                    FunctionalAdminCollections collectionToReindex;
                    try {
                        collectionToReindex =
                            FunctionalAdminCollections.valueOf(index.getCollectionName());

                        boolean isMultiTenant =
                            FunctionalAdminCollections.isCollectionMultiTenant(index.getCollectionName());

                        if (isMultiTenant && index.getTenants() != null && index.getTenants().size() > 0) {
                            String message = String
                                .format("Try to reindex a multi tenant collection %s on multiple indexes", index
                                    .getCollectionName());
                            results.add(indexationHelper.getFullKOResult(index, message));
                            atLeastOneKO.set(true);
                        } else {
                            MongoCollection<Document> mongoCollection = collectionToReindex.getCollection();
                            try (InputStream mappingStream =
                                ElasticsearchCollections.valueOf(index.getCollectionName().toUpperCase())
                                    .getMappingAsInputStream()) {

                                results.add(indexationHelper.reindex(mongoCollection, collectionToReindex.getName(), collectionToReindex.getEsClient(),
                                    index.getTenants(), mappingStream));

                                atLeastOneOK.set(true);
                            } catch (IOException exc) {
                                LOGGER.error("Cannot get {} elastic search mapping for tenants {}",
                                    collectionToReindex.name(),
                                    index.getTenants().stream().map(Object::toString)
                                        .collect(Collectors.joining(", ")));
                                results.add(indexationHelper.getFullKOResult(index, exc.getMessage()));
                                atLeastOneKO.set(true);
                            }
                        }
                    } catch (IllegalArgumentException ex) {
                        String message = String.format("Try to reindex an unknown collection %s", index
                            .getCollectionName());
                        results.add(indexationHelper.getFullKOResult(index, message));
                        atLeastOneKO.set(true);
                        LOGGER.error(message, ex);
                    }
                }

            } catch (LogbookClientServerException | MetaDataClientServerException |
                MetaDataNotFoundException | InvalidParseOperationException e) {
                results.add(indexationHelper.getFullKOResult(index, REINDEXATION_EXCEPTION_MSG));
                atLeastOneKO.set(true);
                LOGGER.error(REINDEXATION_EXCEPTION_MSG, e);
            }
        });
        if (atLeastOneKO.get() && !atLeastOneOK.get()) {
            final Status returnedStatus = Status.INTERNAL_SERVER_ERROR;
            Response response = Response.status(returnedStatus)
                .entity(new VitamError(returnedStatus.name()).setHttpCode(returnedStatus.getStatusCode())
                    .setContext(ServiceName.FUNCTIONAL_ADMINISTRATION.getName())
                    .setState("code_vitam")
                    .setMessage(JsonHandler.unprettyPrint(results))
                    .setDescription("Internal error."))
                .build();
            return response;
        } else if (atLeastOneKO.get() && atLeastOneOK.get()) {

            return Response.status(Status.ACCEPTED).entity(new RequestResponseOK()
                .addAllResults(results).setHttpCode(Status.ACCEPTED.getStatusCode())).build();
        } else {
            return Response.status(Status.CREATED).entity(new RequestResponseOK()
                .addAllResults(results).setHttpCode(Status.CREATED.getStatusCode())).build();
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
        ParametersChecker.checkParameter(OPTIONS_IS_MANDATORY_PATAMETER, switchIndexParameters);
        List<IndexationResult> results = new ArrayList<IndexationResult>();
        AtomicBoolean atLeastOneKO = new AtomicBoolean(false);
        AtomicBoolean atLeastOneOK = new AtomicBoolean(false);
        // call the switch service
        switchIndexParameters.forEach(switchIndex -> {
            try (MetaDataClient metaDataClient = metaDataClientFactory.getClient(); LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()){

                if (VitamCollectionHelper.isLogbookCollection(switchIndex.getAlias())) {
                    String[] splits = switchIndex.getIndexName().split("_");
                    switchIndex.setAlias(splits[0] + "_" + splits[1]);
                    results.add(
                        JsonHandler.getFromJsonNode(logbookOperationsClient.switchIndexes(switchIndex), IndexationResult.class));
                    atLeastOneOK.set(true);
                } else if (VitamCollectionHelper.isMetadataCollection(switchIndex.getAlias())) {
                    String[] splits = switchIndex.getIndexName().split("_");
                    switchIndex.setAlias(splits[0] + "_" + splits[1]);
                    results.add(
                        JsonHandler.getFromJsonNode(metaDataClient.switchIndexes(switchIndex), IndexationResult.class));
                    atLeastOneOK.set(true);
                } else {
                    try {

                        indexationHelper.switchIndex(switchIndex.getIndexName().split("_")[0],
                            switchIndex.getIndexName().toLowerCase(),
                            FunctionalAdminCollections.ACCESS_CONTRACT.getEsClient());//We need an Es client, we take this one
                        atLeastOneOK.set(true);
                    } catch (IllegalArgumentException e) {
                        String message = String.format("Try to switch indexes on unknown collection %s", switchIndex
                            .getAlias());
                        results.add(indexationHelper.getKOResult(switchIndex, message));
                        atLeastOneKO.set(true);
                        LOGGER.error(message, e);
                    }
                }
            } catch (DatabaseException | LogbookClientServerException | InvalidParseOperationException |
                MetaDataClientServerException | MetaDataNotFoundException e) {
                atLeastOneKO.set(true);
                String message = String.format("Error while switching indexes on collection %s", switchIndex
                    .getAlias());
                results.add(indexationHelper.getKOResult(switchIndex, message));
                LOGGER.error(message, e);
            }
        });
        if (atLeastOneKO.get() && !atLeastOneOK.get()) {
            final Status returnedStatus = Status.INTERNAL_SERVER_ERROR;
            Response response = Response.status(returnedStatus)
                .entity(new VitamError(returnedStatus.name()).setHttpCode(returnedStatus.getStatusCode())
                    .setContext(ServiceName.FUNCTIONAL_ADMINISTRATION.getName())
                    .setState("code_vitam")
                    .setMessage(JsonHandler.unprettyPrint(results))
                    .setDescription("Internal error."))
                .build();
            return response;
        } else if (atLeastOneKO.get() && atLeastOneOK.get()) {
            return Response.status(Status.ACCEPTED).entity(new RequestResponseOK()
                .addAllResults(results).setHttpCode(Status.ACCEPTED.getStatusCode())).build();
        } else {
            return Response.status(Status.OK).entity(new RequestResponseOK()
                .addAllResults(results).setHttpCode(Status.OK.getStatusCode())).build();
        }

    }

}
