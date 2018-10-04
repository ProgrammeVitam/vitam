/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.metadata.core.graph.api;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Projections.include;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.mongodb.Function;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.graph.GraphUtils;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.core.graph.GraphBuilderException;
import fr.gouv.vitam.metadata.core.graph.GraphRelation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;

/**
 * This class get units where calculated data are modified
 * Zip generated files and store the zipped file in the offer.
 */
public interface GraphBuilderService extends VitamAutoCloseable {

    String $_SET = "$set";
    Integer START_DEPTH = 1;

    Integer concurrencyLevel = Math.max(Runtime.getRuntime().availableProcessors(), 16);
    ExecutorService executor = Executors.newFixedThreadPool(concurrencyLevel);

    LoadingCache<String, Document> unitCache =
        CacheBuilder
            .newBuilder()
            .maximumSize(VitamConfiguration.getMaxCacheEntries())
            .recordStats()
            .softValues()
            .concurrencyLevel(concurrencyLevel)
            .expireAfterAccess(VitamConfiguration.getExpireCacheEntriesDelay(), TimeUnit.SECONDS)
            .build(new CacheLoader<String, Document>() {
                @Override
                public Document load(String key) {
                    return (Document) MetadataCollections.UNIT.getCollection().find(eq(Unit.ID, key))
                        .projection(include(Unit.UP, Unit.ORIGINATING_AGENCY, Unit.ORIGINATING_AGENCIES, Unit.UNITUPS))
                        .first();
                }

                @Override
                public Map<String, Document> loadAll(Iterable<? extends String> keys) {
                    Map<String, Document> docs = new HashMap<>();
                    MongoCursor<Document> it = MetadataCollections.UNIT.getCollection().find(in(Unit.ID, keys))
                        .projection(include(Unit.UP, Unit.ORIGINATING_AGENCY, Unit.ORIGINATING_AGENCIES, Unit.UNITUPS)).iterator();
                    while (it.hasNext()) {
                        final Document doc = it.next();
                        docs.put(doc.get(Unit.ID, String.class), doc);
                    }
                    return docs;
                }
            });


    /**
     * If no graph builder in progress, try to start one
     * Should be exposed in the API
     *
     * @return the map of collection:number of treated documents
     */
    Map<MetadataCollections, Integer> computeGraph() throws GraphBuilderException;



    /**
     * Should be called only for the method tryStoreGraph
     *
     * @param metadataCollections the collection concerned by the build of the graph
     * @param queryDSL            the query dsl to select units subject of computing graph
     * @return False if an exception occurs of if no unit graph was stored.
     * True if a stored zip file is created and saved in the storage.
     */
    Integer computeGraph(MetadataCollections metadataCollections, JsonNode queryDSL);



    /**
     * Get data from database and pre-populate unitCache
     *
     * @param documents
     * @throws GraphBuilderException
     */
    default void preLoadCache(List<Document> documents) throws GraphBuilderException {
        Set<String> allUps =
            documents
                .stream()
                .map(o -> (List<String>) o.get(Unit.UP, List.class))
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        try {
            ImmutableMap<String, Document> docs = unitCache.getAll(allUps);
            while (!allUps.isEmpty()) {

                allUps = docs
                    .values()
                    .stream()
                    .map(o -> (List<String>) o.get(Unit.UP, List.class))
                    .filter(CollectionUtils::isNotEmpty)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());

                if (!allUps.isEmpty()) {
                    docs = unitCache.getAll(allUps);
                }
            }

        } catch (ExecutionException e) {
            throw new GraphBuilderException(e);
        }
    }


    /**
     * Generic method to calculate graph for unit and object group
     *
     * @param metadataCollections the type the collection (Unit or ObjectGroup)
     * @param documents           the concerning collection of documents
     * @param prePopulateCache    if true pre-populate cache
     * @throws GraphBuilderException
     */
    default void computeGraph(MetadataCollections metadataCollections, List<Document> documents,
        boolean prePopulateCache)
        throws GraphBuilderException {

        if (prePopulateCache) {
            preLoadCache(documents);
        }

        Function<Document, WriteModel<Document>> func;
        try {
            switch (metadataCollections) {
                case UNIT:
                    func = this::computeUnitGraph;
                    break;
                case OBJECTGROUP:
                    func = this::computeObjectGroupGraph;
                    break;
                default:
                    throw new GraphBuilderException("Collection (" + metadataCollections + ") not supported");
            }

            CompletableFuture<WriteModel<Document>>[] features = documents.stream()
                .map(o -> CompletableFuture.supplyAsync(() -> func.apply(o), executor))
                .toArray(CompletableFuture[]::new);


            CompletableFuture<List<WriteModel<Document>>> result = CompletableFuture.allOf(features).
                thenApply(v ->
                    Stream.of(features)
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
            List<WriteModel<Document>> updateOneModels = result.get();


            if (!updateOneModels.isEmpty()) {
                try {
                    this.bulkUpdateMongo(metadataCollections, updateOneModels);
                    // Re-Index all documents
                    this.bulkElasticsearch(metadataCollections,
                        documents.stream().map(o -> o.get(Unit.ID, String.class)).collect(
                            Collectors.toSet()));
                } catch (DatabaseException e) {
                    // Rollback in MongoDB and Elasticsearch
                    throw new GraphBuilderException(e);
                }
            }

        } catch (VitamRuntimeException e) {
            throw new GraphBuilderException(e.getCause());
        } catch (InterruptedException | ExecutionException e) {
            throw new GraphBuilderException(e);
        }


    }


    /**
     * Create update model for Unit
     *
     * @param document
     * @return UpdateOneModel for Unit
     * @throws GraphBuilderException
     */
    default UpdateOneModel<Document> computeUnitGraph(Document document)
        throws VitamRuntimeException {

        List<GraphRelation> stackOrderedGraphRels = new ArrayList<>();
        List<String> up = document.get(Unit.UP, List.class);
        String unitId = document.get(Unit.ID, String.class);
        String originatingAgency = document.get(Unit.ORIGINATING_AGENCY, String.class);

        computeUnitGraphUsingDirectParents(stackOrderedGraphRels, unitId, up, START_DEPTH);

        // Calculate other information
        // _graph, _us, _uds, _max, _min, _sps, _us_sps
        Set<String> graph = new HashSet<>();
        Set<String> us = new HashSet<>();
        Set<String> sps = new HashSet<>();
        Map<String, Set<String>> us_sp = new HashMap<>();
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
                Set<String> ussp = us_sp.get(parentOriginatingAgency);
                if (null == ussp) {
                    ussp = new HashSet<>();
                    us_sp.put(parentOriginatingAgency, ussp);
                }

                ussp.add(ugr.getParent());
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
        Integer max_minus_one = 0;
        for (Integer o : udsMap.keySet()) {
            Collection<String> currentParents = udsMap.get(o);
            if (!currentParents.isEmpty()) {
                parentsDepths.put(String.valueOf(o), currentParents);
                max_minus_one = max_minus_one < o ? o : max_minus_one;
            }
        }

        // +1 because if no parent _max==1 if one parent _max==2
        Integer max = max_minus_one + 1;

        /*
         * If the current document is present in the cache ?
         * Then two options:
         *      - invalidate cache for this key
         *      - update cache
         * Why ?
         * the cache will be re-used to compute graph of ObjectGroup
         *
         * In this case, we will just update cache. because invalidation must be done
         */

        if (null != unitCache.getIfPresent(unitId)) {
            document.put(Unit.ORIGINATING_AGENCIES, new ArrayList<>(sps));
            document.put(Unit.UNITUPS, new ArrayList<>(us));
            unitCache.put(unitId, document);
        }

        Document update = new Document(Unit.ID, unitId)
            .append(Unit.UNITUPS, us)
            .append(Unit.UNITDEPTHS, parentsDepths)
            .append(Unit.ORIGINATING_AGENCIES, sps)
            .append(Unit.PARENT_ORIGINATING_AGENCIES, us_sp)
            .append(Unit.MINDEPTH, min)
            .append(Unit.MAXDEPTH, max)
            .append(Unit.GRAPH, graph)
            .append(Unit.GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));

        Document data = new Document($_SET, update);
        return new UpdateOneModel<>(eq(Unit.ID, unitId), data, new UpdateOptions().upsert(false));

    }


    /**
     * Create update model for ObjectGroup
     *
     * @param document
     * @return UpdateOneModel for ObjectGroup
     * @throws GraphBuilderException
     */
    default UpdateOneModel<Document> computeObjectGroupGraph(Document document)
        throws VitamRuntimeException {
        Set<String> sps = new HashSet<>();
        Set<String> unitParents = new HashSet<>();
        String gotId = document.get(ObjectGroup.ID, String.class);
        List<String> up = document.get(ObjectGroup.UP, List.class);

        computeObjectGroupGraph(sps, unitParents, up);

        // Add current _up to _us
        if (null != up) {
            unitParents.addAll(up);
        }


        String originatingAgency = document.get(ObjectGroup.ORIGINATING_AGENCY, String.class);
        if (StringUtils.isNotEmpty(originatingAgency)) {
            sps.add(originatingAgency);
        }
        final Document data = new Document($_SET, new Document(ObjectGroup.ORIGINATING_AGENCIES, sps)
            .append(Unit.UNITUPS, unitParents)
            .append(ObjectGroup.GRAPH_LAST_PERSISTED_DATE,
                LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now())));

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
    default void computeUnitGraphUsingDirectParents(List<GraphRelation> graphRels, String unitId,
        List<String> up, int currentDepth)
        throws VitamRuntimeException {
        if (null == up || up.isEmpty()) {
            return;
        }

        final Map<String, Document> units;
        try {
            units = unitCache.getAll(up);
        } catch (ExecutionException e) {
            throw new VitamRuntimeException(e);
        }

        final int nextDepth = currentDepth + 1;
        for (Map.Entry<String, Document> unitParent : units.entrySet()) {
            Document parentUnit = unitParent.getValue();

            GraphRelation ugr = new GraphRelation(unitId, unitParent.getKey(),
                unitParent.getValue().get(Unit.ORIGINATING_AGENCY, String.class), currentDepth);

            if (graphRels.contains(ugr)) {
                // Relation (unit_id , unitParent.getKey()) already treated
                // Means unit_id already visited, then  continue is unnecessary. beak is the best choice for performance
                break;
            }

            graphRels.add(ugr);

            // Recall the same method, but no for each parent unit of the current unit_id
            computeUnitGraphUsingDirectParents(graphRels,
                unitParent.getKey(),
                parentUnit.get(Unit.UP, List.class), nextDepth);
        }

    }



    /**
     * For ObjectGroup, we only get graph data (sps) from only unit represents (up)
     * We do not loop over all parent of parent until root units
     * As not concurrence expected, no problem of inconsistency,
     * Else, if parallel compute is needed, then, we have to loop over all units (until root units) or to implements optimistic lock on _glpd
     *
     * @param originatingAgencies
     * @param unitParents
     * @param up
     * @throws VitamRuntimeException
     */
    default void computeObjectGroupGraph(Set<String> originatingAgencies, Set<String> unitParents, List<String> up)
        throws VitamRuntimeException {

        if (null == up || up.isEmpty()) {
            return;
        }

        final Map<String, Document> units;
        try {
            units = unitCache.getAll(up);
        } catch (ExecutionException e) {
            throw new VitamRuntimeException(e);
        }

        for (Map.Entry<String, Document> unit : units.entrySet()) {
            Document au = unit.getValue();
            List agencies = au.get(Unit.ORIGINATING_AGENCIES, List.class);
            if (CollectionUtils.isNotEmpty(agencies)) {
                originatingAgencies.addAll(agencies);
            }

            List parents = au.get(Unit.UNITUPS, List.class);
            if (CollectionUtils.isNotEmpty(parents)) {
                unitParents.addAll(parents);
            }
        }
    }



    /**
     * Bulk write in mongodb
     *
     * @param metaDaCollection
     * @param collection
     * @throws DatabaseException
     */
    void bulkUpdateMongo(MetadataCollections metaDaCollection, List<WriteModel<Document>> collection)
        throws DatabaseException;


    /**
     * Bulk save in elasticsearch
     *
     * @param metaDaCollection
     * @param collection       of id of documents
     * @throws DatabaseException
     */
    void bulkElasticsearch(MetadataCollections metaDaCollection, Set<String> collection)
        throws DatabaseException;

    /**
     * Bulk save in elasticsearch
     *
     * @param metaDaCollection
     * @param collection       of documents
     * @throws DatabaseException
     */
    void bulkElasticsearch(MetadataCollections metaDaCollection, List<Document> collection)
        throws DatabaseException;
}
