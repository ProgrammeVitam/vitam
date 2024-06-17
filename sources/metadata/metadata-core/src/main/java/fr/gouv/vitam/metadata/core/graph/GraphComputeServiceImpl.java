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
package fr.gouv.vitam.metadata.core.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterators;
import com.mongodb.Function;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import fr.gouv.vitam.common.LocalDateUtil;
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
import fr.gouv.vitam.common.graph.GraphUtils;
import fr.gouv.vitam.common.iterables.SpliteratorIterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.GraphComputeResponse;
import fr.gouv.vitam.common.model.GraphComputeResponse.GraphComputeAction;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.thread.ExecutorUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.core.MetaDataImpl;
import fr.gouv.vitam.metadata.core.config.ElasticsearchMetadataIndexManager;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MetadataDocument;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.core.graph.api.GraphComputeService;
import fr.gouv.vitam.metadata.core.model.MetadataResult;
import joptsimple.internal.Strings;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Projections.include;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION.FIELDS;
import static fr.gouv.vitam.common.database.parser.query.ParserTokens.PROJECTIONARGS.ID;

/**
 * This class compute graph for unit and object group
 *
 * Should only be called from GraphFactory
 */
public class GraphComputeServiceImpl implements GraphComputeService, VitamAutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(GraphComputeServiceImpl.class);
    private static final String $_SET = "$set";
    private static final String $_INC = "$inc";
    private static final String $_UNSET = "$unset";
    private static final int START_DEPTH = 1;

    private static GraphComputeService instance;

    private static final Integer concurrencyLevel = Math.max(Runtime.getRuntime().availableProcessors(), 32);
    private static final ExecutorService executor = ExecutorUtils.createScalableBatchExecutorService(concurrencyLevel);

    private final Map<Integer, AtomicBoolean> lockers = new HashMap<>();
    private final VitamCache<String, Document> cache;
    private final VitamRepositoryProvider vitamRepositoryProvider;
    private final MetaDataImpl metaData;
    private final ElasticsearchMetadataIndexManager indexManager;

    private String currentOperation = null;

    private GraphComputeServiceImpl(
        VitamRepositoryProvider vitamRepositoryProvider,
        MetaDataImpl metaData,
        VitamCache<String, Document> cache,
        ElasticsearchMetadataIndexManager indexManager,
        List<Integer> tenants
    ) {
        this.vitamRepositoryProvider = vitamRepositoryProvider;
        this.metaData = metaData;
        this.cache = cache;
        this.indexManager = indexManager;
        tenants.forEach(tenant -> lockers.put(tenant, new AtomicBoolean(false)));
    }

    public static synchronized GraphComputeService initialize(
        VitamRepositoryProvider vitamRepositoryProvider,
        MetaDataImpl metaData,
        ElasticsearchMetadataIndexManager indexManager
    ) {
        if (instance == null) {
            instance = new GraphComputeServiceImpl(
                vitamRepositoryProvider,
                metaData,
                GraphComputeCache.getInstance(),
                indexManager,
                VitamConfiguration.getTenants()
            );
        }
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

        LOGGER.info("Starting metadata graph computation for tenant " + tenant);
        try {
            GraphComputeResponse response = new GraphComputeResponse();
            Iterator<String> unitIdsIterator = executeQuery(queryDSL);
            Iterator<List<String>> bulkUnitIdsIterator = Iterators.partition(
                unitIdsIterator,
                VitamConfiguration.getBatchSize()
            );

            bulkUnitIdsIterator.forEachRemaining(bulkUnitIds -> {
                GraphComputeResponse stats = computeGraph(MetadataCollections.UNIT, bulkUnitIds, true, false);
                response.increment(stats);
                LOGGER.info(
                    "Metadata graph computation in progress. Done " +
                    response.getUnitCount() +
                    " units / " +
                    response.getGotCount() +
                    " object groups"
                );
            });

            LOGGER.info("Metadata graph computation completed");
            return response;
        } finally {
            lock.set(false);
        }
    }

    private Iterator<String> executeQuery(JsonNode queryDSL) throws MetaDataException {
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

        ScrollSpliterator<JsonNode> scrollSpliterator = new ScrollSpliterator<>(
            request,
            query -> {
                try {
                    final MetadataResult metadataResult = metaData.selectUnitsByQuery(query.getFinalSelect());
                    return new RequestResponseOK<JsonNode>(metadataResult.getQuery())
                        .addAllResults(metadataResult.getResults())
                        .addAllFacetResults(metadataResult.getFacetResults())
                        .setHits(metadataResult.getHits());
                } catch (
                    InvalidParseOperationException
                    | MetaDataExecutionException
                    | MetaDataDocumentSizeException
                    | MetaDataNotFoundException
                    | BadRequestException
                    | VitamDBException e
                ) {
                    // Error (KO, FATAL) are not managed. Compute graph by DSL used only for PRA (Plan de reprise d'activité)
                    // WARN: But if we want to use it in workflow, then we have to distinguish between KO and FATAL
                    throw new IllegalStateException(e);
                }
            },
            VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds(),
            VitamConfiguration.getElasticSearchScrollLimit()
        );

        return Iterators.transform(
            new SpliteratorIterator<>(scrollSpliterator),
            doc -> doc.get(VitamFieldsHelper.id()).asText()
        );
    }

    @Override
    public GraphComputeResponse computeGraph(
        MetadataCollections metadataCollections,
        Collection<String> documentsId,
        boolean computeObjectGroupGraph,
        boolean invalidateComputedInheritedRules
    ) {
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
                .projection(
                    include(
                        Unit.UP,
                        Unit.OG,
                        Unit.ORIGINATING_AGENCY,
                        Unit.ORIGINATING_AGENCIES,
                        Unit.VALID_COMPUTED_INHERITED_RULES
                    )
                )
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
                GraphComputeResponse statsGots = computeGraph(
                    MetadataCollections.OBJECTGROUP,
                    concernedGots,
                    false,
                    invalidateComputedInheritedRules
                );
                response.increment(statsGots);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("End Compute graph of (%s)", metadataCollections.name()));
            }
        } catch (Exception e) {
            LOGGER.error(
                String.format("[Consistency ERROR] : Error while compute graph of (%s)", metadataCollections.name()),
                e
            );
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
                "[Graph compute] cache before invalidate : " + GraphComputeCache.getInstance().getCache().stats()
            );
            getCache().invalidateAll();
        } else if (RandomUtils.nextInt(1, 20) % 2 == 0) {
            // If we want to see randomly cache stats. To be removed when stats not needed
            LOGGER.info("[Graph compute] cache : " + GraphComputeCache.getInstance().getCache().stats());
        }
    }

    private void bulkUpdateMongo(MetadataCollections metaDaCollection, List<WriteModel<Document>> collection)
        throws DatabaseException {
        this.vitamRepositoryProvider.getVitamMongoRepository(metaDaCollection.getVitamCollection()).update(collection);
    }

    private void bulkElasticsearch(MetadataCollections metaDaCollection, Set<String> collection)
        throws DatabaseException {
        if (collection.isEmpty()) {
            return;
        }

        FindIterable<Document> fit =
            this.vitamRepositoryProvider.getVitamMongoRepository(metaDaCollection.getVitamCollection()).findDocuments(
                    collection,
                    null
                );
        MongoCursor<Document> it = fit.iterator();
        List<Document> documents = new ArrayList<>();
        while (it.hasNext()) {
            documents.add(it.next());
        }

        if (!documents.isEmpty()) {
            bulkElasticsearch(metaDaCollection, documents);
        }
    }

    private void bulkElasticsearch(MetadataCollections metaDaCollection, List<Document> collection)
        throws DatabaseException {
        this.vitamRepositoryProvider.getVitamESRepository(
                metaDaCollection.getVitamCollection(),
                indexManager.getElasticsearchIndexAliasResolver(metaDaCollection)
            ).save(collection);
    }

    /**
     * Get data from database and pre-populate unitCache
     *
     * @param documents
     * @throws MetaDataException
     */
    private void preLoadCache(List<Document> documents) throws MetaDataException {
        try {
            Collection<Document> currentDocuments = documents;

            while (true) {
                Set<String> parentUnitIds = currentDocuments
                    .stream()
                    .map(o -> o.getList(Unit.UP, String.class))
                    .filter(CollectionUtils::isNotEmpty)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());

                if (parentUnitIds.isEmpty()) {
                    break;
                }

                currentDocuments = getCache().getAll(parentUnitIds).values();
            }
        } catch (ExecutionException e) {
            throw new MetaDataException(e);
        }
    }

    /**
     * Generic method to calculate graph for unit and object group
     *
     * @param metadataCollections the type the collection (Unit or ObjectGroup)
     * @param documents the concerning collection of documents
     * @param invalidateComputedInheritedRules
     * @throws MetaDataException
     */
    private void computeGraph(
        MetadataCollections metadataCollections,
        List<Document> documents,
        boolean invalidateComputedInheritedRules
    ) throws MetaDataException {
        preLoadCache(documents);

        Function<Document, WriteModel<Document>> func;
        try {
            switch (metadataCollections) {
                case UNIT:
                    func = document -> computeUnitGraph(document, invalidateComputedInheritedRules);
                    break;
                case OBJECTGROUP:
                    func = this::computeObjectGroupGraph;
                    break;
                default:
                    throw new MetaDataException("Collection (" + metadataCollections + ") not supported");
            }

            final Integer scopedTenant = VitamThreadUtils.getVitamSession().getTenantId();
            final String scopedXRequestId = VitamThreadUtils.getVitamSession().getRequestId();

            // Create a batch of CompletableFuture.
            CompletableFuture<WriteModel<Document>>[] features = documents
                .stream()
                .map(o ->
                    CompletableFuture.supplyAsync(
                        () -> {
                            VitamThreadUtils.getVitamSession().setTenantId(scopedTenant);
                            VitamThreadUtils.getVitamSession().setRequestId(scopedXRequestId);
                            return func.apply(o);
                        },
                        executor
                    ))
                .toArray(CompletableFuture[]::new);

            CompletableFuture<List<WriteModel<Document>>> result = CompletableFuture.allOf(features).thenApply(
                v -> Stream.of(features).map(CompletableFuture::join).collect(Collectors.toList())
            );
            List<WriteModel<Document>> updateOneModels = result.get();

            if (!updateOneModels.isEmpty()) {
                try {
                    this.bulkUpdateMongo(metadataCollections, updateOneModels);
                    // Re-Index all documents
                    this.bulkElasticsearch(
                            metadataCollections,
                            documents.stream().map(o -> o.getString(Unit.ID)).collect(Collectors.toSet())
                        );
                } catch (DatabaseException e) {
                    // Rollback in MongoDB and Elasticsearch
                    throw new MetaDataException(e);
                }
            }
        } catch (VitamRuntimeException e) {
            throw new MetaDataException(e.getCause());
        } catch (InterruptedException | ExecutionException e) {
            throw new MetaDataException(e);
        }
    }

    /**
     * Create update model for Unit
     *
     * @param document
     * @return UpdateOneModel for Unit
     * @throws MetaDataException
     */
    private UpdateOneModel<Document> computeUnitGraph(Document document, boolean invalidateComputedInheritedRules)
        throws VitamRuntimeException {
        List<GraphRelation> stackOrderedGraphRels = new ArrayList<>();
        List<String> up = document.getList(Unit.UP, String.class);
        String unitId = document.getString(Unit.ID);
        String originatingAgency = document.getString(Unit.ORIGINATING_AGENCY);

        computeUnitGraphUsingDirectParents(stackOrderedGraphRels, unitId, up, START_DEPTH);

        // Calculate other information
        // _graph, _us, _uds, _max, _min, _sps
        Set<String> graph = new HashSet<>();
        Set<String> us = new HashSet<>();
        Set<String> sps = new HashSet<>();
        MultiValuedMap<Integer, String> uds = new HashSetValuedHashMap<>();

        if (StringUtils.isNotEmpty(originatingAgency)) {
            sps.add(originatingAgency);
        }

        for (GraphRelation ugr : stackOrderedGraphRels) {
            graph.add(GraphUtils.createGraphRelation(ugr.getUnit(), ugr.getParent()));
            us.add(ugr.getParent());

            String parentOriginatingAgency = ugr.getParentOriginatingAgency();

            if (StringUtils.isNotEmpty(parentOriginatingAgency)) {
                sps.add(parentOriginatingAgency);
            }

            /*
             * Parents depth [depth: [guid]]
             */
            uds.put(ugr.getDepth(), ugr.getParent());
        }

        // MongoDB do not accept number as key of map, so convert Integer to String
        Map<String, Collection<String>> parentsDepths = new HashMap<>();
        Map<Integer, Collection<String>> udsMap = uds.asMap();
        Integer min = 1;
        int max_minus_one = 0;
        for (Integer o : udsMap.keySet()) {
            Collection<String> currentParents = udsMap.get(o);
            if (!currentParents.isEmpty()) {
                parentsDepths.put(String.valueOf(o), currentParents);
                max_minus_one = max_minus_one < o ? o : max_minus_one;
            }
        }
        // +1 because if no parent _max==1 if one parent _max==2
        Integer max = max_minus_one + 1;

        Document update = new Document(Unit.ID, unitId)
            .append(Unit.UNITUPS, us)
            .append(Unit.UNITDEPTHS, parentsDepths)
            .append(Unit.ORIGINATING_AGENCIES, sps)
            .append(Unit.MINDEPTH, min)
            .append(Unit.MAXDEPTH, max)
            .append(Unit.GRAPH, graph)
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted());

        Document data = new Document($_SET, update).append($_INC, new Document(MetadataDocument.ATOMIC_VERSION, 1));

        if (invalidateComputedInheritedRules) {
            if (document.getBoolean(Unit.VALID_COMPUTED_INHERITED_RULES, false)) {
                update.append(Unit.VALID_COMPUTED_INHERITED_RULES, false);
                data.append($_UNSET, new Document(Unit.COMPUTED_INHERITED_RULES, null));
            }
        }

        return new UpdateOneModel<>(eq(Unit.ID, unitId), data, new UpdateOptions().upsert(false));
    }

    /**
     * Create update model for ObjectGroup
     *
     * @param document
     * @return UpdateOneModel for ObjectGroup
     * @throws MetaDataException
     */
    private UpdateOneModel<Document> computeObjectGroupGraph(Document document) throws VitamRuntimeException {
        Set<String> sps = new HashSet<>();
        Set<String> us = new HashSet<>();
        String gotId = document.getString(ObjectGroup.ID);
        List<String> up = document.getList(ObjectGroup.UP, String.class);

        String originatingAgency = document.getString(Unit.ORIGINATING_AGENCY);
        if (StringUtils.isNotEmpty(originatingAgency)) {
            sps.add(originatingAgency);
        }

        List<String> currentUpUnitIds = up;
        while (CollectionUtils.isNotEmpty(currentUpUnitIds)) {
            us.addAll(currentUpUnitIds);

            Collection<Document> parentUnits;
            try {
                parentUnits = getCache().getAll(currentUpUnitIds).values();
            } catch (ExecutionException e) {
                throw new VitamRuntimeException(e);
            }

            parentUnits
                .stream()
                .map(o -> o.getString(Unit.ORIGINATING_AGENCY))
                .filter(StringUtils::isNotEmpty)
                .forEach(sps::add);

            currentUpUnitIds = parentUnits
                .stream()
                .map(o -> o.getList(Unit.UP, String.class))
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        }

        final Document data = new Document(
            $_SET,
            new Document(ObjectGroup.ORIGINATING_AGENCIES, sps)
                .append(Unit.UNITUPS, us)
                .append(ObjectGroup.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.nowFormatted())
        ).append($_INC, new Document(MetadataDocument.ATOMIC_VERSION, 1));

        return new UpdateOneModel<>(eq(ObjectGroup.ID, gotId), data, new UpdateOptions().upsert(false));
    }

    /**
     * Recursive method that compute graph using only _up
     * With global (by reference variable graphRels, we get all needed informations from all parent of the given unit unitId.
     *
     * @param graphRels
     * @param unitId
     * @param up
     * @param currentDepth the current depth, initially equals to 1
     * @throws ExecutionException
     */
    private void computeUnitGraphUsingDirectParents(
        List<GraphRelation> graphRels,
        String unitId,
        List<String> up,
        int currentDepth
    ) throws VitamRuntimeException {
        if (null == up || up.isEmpty()) {
            return;
        }

        final Map<String, Document> units;
        try {
            units = getCache().getAll(up);
        } catch (ExecutionException e) {
            throw new VitamRuntimeException(e);
        }

        final int nextDepth = currentDepth + 1;
        for (Map.Entry<String, Document> unitParent : units.entrySet()) {
            Document parentUnit = unitParent.getValue();

            GraphRelation ugr = new GraphRelation(
                unitId,
                unitParent.getKey(),
                parentUnit.getString(Unit.ORIGINATING_AGENCY),
                currentDepth
            );

            if (graphRels.contains(ugr)) {
                // Relation (unit_id , unitParent.getKey()) already processed
                // Means unit_id already visited, then  continue is unnecessary. beak is the best choice for performance
                break;
            }

            graphRels.add(ugr);

            // Invoke the same method with parent units
            computeUnitGraphUsingDirectParents(
                graphRels,
                unitParent.getKey(),
                parentUnit.getList(Unit.UP, String.class),
                nextDepth
            );
        }
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

    private LoadingCache<String, Document> getCache() {
        return cache.getCache();
    }
}
