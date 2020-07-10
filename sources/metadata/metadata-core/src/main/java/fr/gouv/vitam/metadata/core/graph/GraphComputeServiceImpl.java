/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.metadata.core.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.LoadingCache;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.WriteModel;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.cache.VitamCache;
import fr.gouv.vitam.common.database.api.VitamRepositoryProvider;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.GraphComputeResponse;
import fr.gouv.vitam.common.model.GraphComputeResponse.GraphComputeAction;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.MetaDataImpl;
import fr.gouv.vitam.metadata.core.config.ElasticsearchMetadataIndexManager;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.core.graph.api.GraphComputeService;
import joptsimple.internal.Strings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Projections.include;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION.FIELDS;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.ID;

/**
 * This class compute graph for unit and object group
 *
 * Should only be called from GraphFactory
 */
public class GraphComputeServiceImpl implements GraphComputeService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(GraphComputeServiceImpl.class);
    private static GraphComputeService instance;

    private final Map<Integer, AtomicBoolean> lockers = new HashMap<>();
    private VitamCache<String, Document> cache;
    private VitamRepositoryProvider vitamRepositoryProvider;
    private MetaDataImpl metaData;
    private final ElasticsearchMetadataIndexManager indexManager;

    private String currentOperation = null;

    /**
     * @param vitamRepositoryProvider
     * @param metaData
     * @param cache
     * @param indexManager
     * @param tenants
     */
    private GraphComputeServiceImpl(
        VitamRepositoryProvider vitamRepositoryProvider,
        MetaDataImpl metaData,
        VitamCache<String, Document> cache,
        ElasticsearchMetadataIndexManager indexManager, List<Integer> tenants) {
        this.vitamRepositoryProvider = vitamRepositoryProvider;
        this.metaData = metaData;
        this.cache = cache;
        this.indexManager = indexManager;
        tenants.forEach(tenant -> lockers.put(tenant, new AtomicBoolean(false)));
    }


    /**
     * @param vitamRepositoryProvider
     * @param metaData
     * @param tenants
     * @param indexManager
     * @return GraphComputeServiceImpl
     */
    @VisibleForTesting
    public static synchronized GraphComputeService initialize(
        VitamRepositoryProvider vitamRepositoryProvider,
        MetaDataImpl metaData,
        VitamCache<String, Document> cache,
        List<Integer> tenants, ElasticsearchMetadataIndexManager indexManager) {
        if (instance == null) {
            instance = new GraphComputeServiceImpl(
                vitamRepositoryProvider,
                metaData,
                cache,
                indexManager, tenants);
        }
        return instance;
    }

    /**
     * @param vitamRepositoryProvider
     * @param metaData
     * @param indexManager
     * @return GraphComputeServiceImpl
     */
    public static synchronized GraphComputeService initialize(
        VitamRepositoryProvider vitamRepositoryProvider,
        MetaDataImpl metaData, ElasticsearchMetadataIndexManager indexManager) {
        return initialize(
            vitamRepositoryProvider,
            metaData,
            GraphComputeCache.getInstance(),
            VitamConfiguration.getTenants(), indexManager);
    }

    public static GraphComputeService getInstance() {
        return instance;
    }

    @Override
    public GraphComputeResponse computeGraph(JsonNode queryDSL) throws MetaDataException {

        Integer tenant = VitamThreadUtils.getVitamSession().getTenantId();
        AtomicBoolean lock = lockers.get(tenant);
        boolean tryBuildGraph = lock.compareAndSet(false, true);

        if (!tryBuildGraph) {
            throw new MetaDataException("Compute graph process already in progress");
        }
        try {

            GraphComputeResponse response = new GraphComputeResponse();

            ScrollSpliterator<Set<String>> scroll = executeQuery(queryDSL);


            StreamSupport.stream(scroll, false).forEach(
                item -> {
                    GraphComputeResponse stats = computeGraph(MetadataCollections.UNIT, item, true, false);
                    response.increment(stats);
                });
            return response;
        } finally {
            lock.set(false);
        }

    }

    /**
     * @param queryDSL
     * @return ScrollSpliterator
     * @throws MetaDataException
     */
    private ScrollSpliterator<Set<String>> executeQuery(JsonNode queryDSL) throws MetaDataException {
        SelectParserMultiple parser = new SelectParserMultiple();
        try {
            parser.parse(queryDSL);
        } catch (InvalidParseOperationException e) {
            throw new MetaDataException(e);
        }

        SelectMultiQuery request = parser.getRequest();
        try {

            ObjectNode projection = JsonHandler.createObjectNode();
            ObjectNode fields = JsonHandler.createObjectNode();
            fields.put(ID.exactToken(), 1);
            projection.set(FIELDS.exactToken(), fields);
            request.setProjection(projection);

        } catch (InvalidParseOperationException e) {
            throw new MetaDataException(e);
        }



        return new ScrollSpliterator<>(request,
            query -> {
                try {
                    RequestResponseOK<JsonNode> response =
                        (RequestResponseOK<JsonNode>) metaData.selectUnitsByQuery(query.getFinalSelect());

                    final RequestResponseOK<Set<String>> rr = new RequestResponseOK<>();
                    Set<String> ids = new HashSet<>();
                    Iterator<JsonNode> it = response.getResults().iterator();
                    while (it.hasNext()) {
                        String id = it.next().get(VitamFieldsHelper.id()).asText();
                        ids.add(id);
                        if (!it.hasNext() || ids.size() >= VitamConfiguration.getBatchSize()) {
                            rr.getResults().add(ids);
                            ids = new HashSet<>();
                        }

                    }
                    return rr;

                } catch (InvalidParseOperationException |
                    MetaDataExecutionException |
                    MetaDataDocumentSizeException |
                    MetaDataNotFoundException | BadRequestException |
                    VitamDBException e) {
                    // Error (KO, FATAL) are not managed. Compute graph by DSL used only for PRA (Plan de reprise d'activité)
                    // WARN: But if we want to use it in workflow, then we have to distinguish between KO and FATAL
                    throw new IllegalStateException(e);
                }
            }, VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds(), VitamConfiguration.getElasticSearchScrollLimit());
    }


    @Override
    public GraphComputeResponse computeGraph(MetadataCollections metadataCollections,
        Set<String> documentsId,
        boolean computeObjectGroupGraph, boolean invalidateComputedInheritedRules) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Start Compute graph of (%s)", metadataCollections.name()));
        }

        tryInvalidateCache();

        Set<String> concernedGots = new HashSet<>();
        GraphComputeResponse response = new GraphComputeResponse();

        if (CollectionUtils.isEmpty(documentsId)) {
            return response;
        }

        try {
            final MongoCursor<Document> cursor = vitamRepositoryProvider
                .getVitamMongoRepository(metadataCollections.getVitamCollection())
                .findDocuments(in(Unit.ID, documentsId), VitamConfiguration.getBatchSize())
                .projection(include(Unit.UP, Unit.OG, Unit.ORIGINATING_AGENCY, Unit.ORIGINATING_AGENCIES, Unit.VALID_COMPUTED_INHERITED_RULES))
                .iterator();

            List<Document> documents = new ArrayList<>();

            while (cursor.hasNext()) {
                response.increment(GraphComputeAction.valueOf(metadataCollections.name()), 1);
                Document doc = cursor.next();

                String got = doc.getString(Unit.OG);
                if (!Strings.isNullOrEmpty(got)) {
                    concernedGots.add(got);
                }

                documents.add(doc);
                if (!cursor.hasNext() || documents.size() >= VitamConfiguration.getBatchSize()) {
                    computeGraph(metadataCollections, documents, invalidateComputedInheritedRules);
                    documents = new ArrayList<>();
                }
            }

            //Compute ObjectGroup graph
            if (computeObjectGroupGraph && concernedGots.size() > 0) {
                GraphComputeResponse statsGots =
                    computeGraph(MetadataCollections.OBJECTGROUP, concernedGots, false,
                        invalidateComputedInheritedRules);
                response.increment(statsGots);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("End Compute graph of (%s)", metadataCollections.name()));
            }
        } catch (Exception e) {
            LOGGER.error(String
                    .format("[Consistency ERROR] : Error while compute graph of (%s)", metadataCollections.name()),
                e);
            String msgCause = e.getCause() == null ? "" : e.getCause().getMessage();
            response.setErrorMessage(String.format("Compute graph error (%s) cause (%s)", e.getMessage(), msgCause));
        }

        return response;
    }

    private void tryInvalidateCache() {
        String operation = VitamThreadUtils.getVitamSession().getRequestId();
        // Invalidate cache if operation change
        if (!Objects.equals(operation, currentOperation)) {
            this.currentOperation = operation;
            LOGGER.info(
                "[Graph compute] cache before invalidate : " + GraphComputeCache.getInstance().getCache().stats());
            getCache().invalidateAll();
        } else if (RandomUtils.nextInt(1, 20) % 2 == 0) {
            // If we want to see randomly cache stats. To be removed when stats not needed
            LOGGER.info("[Graph compute] cache : " + GraphComputeCache.getInstance().getCache().stats());
        }
    }

    @Override
    public void bulkUpdateMongo(MetadataCollections metaDaCollection, List<WriteModel<Document>> collection)
        throws DatabaseException {
        this.vitamRepositoryProvider.getVitamMongoRepository(metaDaCollection.getVitamCollection()).update(collection);
    }

    @Override
    public void bulkElasticsearch(MetadataCollections metaDaCollection, Set<String> collection)
        throws DatabaseException {

        if (collection.isEmpty()) {
            return;
        }

        FindIterable<Document> fit =
            this.vitamRepositoryProvider.getVitamMongoRepository(metaDaCollection.getVitamCollection())
                .findDocuments(collection, null);
        MongoCursor<Document> it = fit.iterator();
        List<Document> documents = new ArrayList<>();
        while (it.hasNext()) {
            documents.add(it.next());
        }

        if (!documents.isEmpty()) {
            bulkElasticsearch(metaDaCollection, documents);
        }
    }

    @Override
    public void bulkElasticsearch(MetadataCollections metaDaCollection, List<Document> collection)
        throws DatabaseException {
        this.vitamRepositoryProvider.getVitamESRepository(metaDaCollection.getVitamCollection(),
            indexManager.getElasticsearchIndexAliasResolver(metaDaCollection)).save(collection);
    }

    @Override
    public void close() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Shutdown GraphBuilderService executor");
        }

        executor.shutdown();
        try {
            executor.awaitTermination(10L, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOGGER.error("Error while shutting down GraphBuilderService executor", e);
            throw new VitamRuntimeException(e);
        }

    }

    @Override
    public boolean isInProgress() {
        AtomicBoolean bool = lockers.get(VitamThreadUtils.getVitamSession().getTenantId());
        return bool.get();
    }

    @Override
    public LoadingCache<String, Document> getCache() {
        return cache.getCache();
    }
}
