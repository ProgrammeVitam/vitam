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
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.graph.GraphUtils;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.core.graph.GraphBuilderException;
import fr.gouv.vitam.metadata.core.graph.GraphRelation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.bson.Document;

/**
 * This class get units where calculated data are modified
 * Zip generated files and store the zipped file in the offer.
 */
public interface GraphBuilderService {

    String $_SET = "$set";
    Integer START_DEPTH = 1;

    LoadingCache<String, Document> unitCache =
        CacheBuilder
            .newBuilder()
            .maximumSize(VitamConfiguration.getMaxCacheEntries())
            .recordStats()
            .softValues()
            .expireAfterAccess(VitamConfiguration.getExpireCacheEntriesDelay(), TimeUnit.MINUTES)
            .build(new CacheLoader<String, Document>() {
                @Override
                public Document load(String key) {
                    return (Document) MetadataCollections.UNIT.getCollection().find(eq(Unit.ID, key))
                        .projection(include(Unit.UP, Unit.ORIGINATING_AGENCY, Unit.ORIGINATING_AGENCIES))
                        .first();
                }

                @Override
                public Map<String, Document> loadAll(Iterable<? extends String> keys) {
                    Map<String, Document> docs = new HashMap<>();
                    MongoCursor<Document> it = MetadataCollections.UNIT.getCollection().find(in(Unit.ID, keys))
                        .projection(include(Unit.UP, Unit.ORIGINATING_AGENCY, Unit.ORIGINATING_AGENCIES)).iterator();
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
        List<String> allUps =
            documents
                .stream()
                .map(o -> (List<String>) o.get(Unit.UP, List.class))
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        try {
            ImmutableMap<String, Document> docs = unitCache.getAll(allUps);
            while (!allUps.isEmpty()) {

                allUps = docs
                    .values()
                    .stream()
                    .map(o -> (List<String>) o.get(Unit.UP, List.class))
                    .filter(CollectionUtils::isNotEmpty)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

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

        List<WriteModel<Document>> updateOneModels;
        try {
            switch (metadataCollections) {
                case UNIT:
                    updateOneModels = documents.stream().map(o -> computeUnitGraph(o)).collect(Collectors.toList());
                    break;
                case OBJECTGROUP:
                    updateOneModels =
                        documents.stream().map(o -> computeObjectGroupGraph(o)).collect(Collectors.toList());
                    break;
                default:
                    throw new GraphBuilderException("Collection (" + metadataCollections + ") not supported");
            }
        } catch (VitamRuntimeException e) {
            throw new GraphBuilderException(e.getCause());
        }


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
     * Create update model for Unit
     *
     * @param document
     * @return UpdateOneModel for Unit
     * @throws GraphBuilderException
     */
    default UpdateOneModel<Document> computeUnitGraph(Document document)
        throws VitamRuntimeException {

        List<GraphRelation> stackOrderedGraphRels = new ArrayList<>();
        List<String> ups = document.get(Unit.UP, List.class);
        String unitId = document.get(Unit.ID, String.class);
        String originatingAgency = document.get(Unit.ID, String.class);
        if (null != ups) {
            computeUnitGraphUsingDirectParents(stackOrderedGraphRels, unitId, originatingAgency, ups, START_DEPTH);
        }

        // Calculate other information
        // _graph, _us, _uds, _max, _min, _sps, _us_sps
        Set<String> graph = new HashSet<>();
        Set<String> us = new HashSet<>();
        Set<String> sps = new HashSet<>();
        Map<String, Set<String>> us_sp = new HashMap<>();
        MultiValuedMap<Integer, String> uds = new HashSetValuedHashMap<>();

        Integer min = 1;
        Integer max = 1;
        for (GraphRelation ugr : stackOrderedGraphRels) {

            graph.add(GraphUtils.createGraphRelation(ugr.getUnitOriginatingAgency(), ugr.getParentOriginatingAgency()));
            us.add(ugr.getParent());
            sps.add(ugr.getUnitOriginatingAgency());
            sps.add(ugr.getParentOriginatingAgency());

            Set<String> ussp = us_sp.get(ugr.getParentOriginatingAgency());
            if (null == ussp) {
                ussp = new HashSet<>();
                us_sp.put(ugr.getParentOriginatingAgency(), ussp);
            }

            ussp.add(ugr.getParent());

            /*
             * Parents depth [depth: [guid]]
             */
            uds.put(ugr.getDepth(), ugr.getParentOriginatingAgency());

            max = max < ugr.getDepth() ? ugr.getDepth() : max;
        }


        // Remove GUIDs having a multiple depths from the depths collections and keep only those in the lower depth
        Map<Integer, Collection<String>> parentsDepths = uds.asMap();
        Set<Integer> depths = new TreeSet<>(parentsDepths.keySet());
        Collection<String> parents = new HashSet<>();
        depths.forEach(o -> {
            Collection<String> currentParents = parentsDepths.get(o);
            currentParents.removeAll(parents);
            parents.addAll(currentParents);
        });

        Document update = new Document(Unit.UNITUPS, us)
            .append(Unit.UNITDEPTHS, parentsDepths)
            .append(Unit.ORIGINATING_AGENCIES, sps)
            .append(Unit.PARENT_ORIGINATING_AGENCIES, us_sp)
            .append(Unit.MINDEPTH, min)
            .append(Unit.MAXDEPTH, max + 1) // +1 because if no parent _max==1 if one parent _max==2
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

        Set<String> sps = new HashSet();
        String gotId = document.get(ObjectGroup.ID, String.class);
        List<String> ups = document.get(ObjectGroup.UP, List.class);
        if (null != ups) {
            computeObjectGroupGraph(sps, ups);
        }

        final Document data = new Document($_SET, new Document(ObjectGroup.ORIGINATING_AGENCIES, sps)
            .append(ObjectGroup.GRAPH_LAST_PERSISTED_DATE,
                LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now())));

        return new UpdateOneModel<>(eq(ObjectGroup.ID, gotId), data, new UpdateOptions().upsert(false));
    }


    /**
     * @param graphRels
     * @param unitId
     * @param originatingAgency
     * @param ups
     * @param currentDepth      the current depth, initially equals to 1
     * @throws ExecutionException
     */
    static void computeUnitGraphUsingDirectParents(List<GraphRelation> graphRels, String unitId,
        String originatingAgency, List<String> ups, Integer currentDepth)
        throws VitamRuntimeException {
        if (null == ups || ups.isEmpty()) {
            return;
        }

        final ImmutableMap<String, Document> units;
        try {
            units = unitCache.getAll(ups);
        } catch (ExecutionException e) {
            throw new VitamRuntimeException(e);
        }

        final Integer nextDepth = currentDepth + 1;
        for (Map.Entry<String, Document> unitParent : units.entrySet()) {
            Document au = unitParent.getValue();

            GraphRelation ugr = new GraphRelation(unitId, originatingAgency, unitParent.getKey(),
                unitParent.getValue().get(Unit.ORIGINATING_AGENCY, String.class), currentDepth);

            if (graphRels.contains(ugr)) {
                break;
            }

            graphRels.add(ugr);

            computeUnitGraphUsingDirectParents(graphRels,
                unitParent.getKey(),
                au.get(Unit.ORIGINATING_AGENCY, String.class),
                au.get(Unit.UP, List.class), nextDepth);
        }

    }


    /**
     * @param originatingAgencies
     * @param ups
     * @throws ExecutionException
     */
    default void computeObjectGroupGraph(Set<String> originatingAgencies, List<String> ups)
        throws VitamRuntimeException {
        if (null == ups || ups.isEmpty()) {
            return;
        }

        final ImmutableMap<String, Document> units;
        try {
            units = unitCache.getAll(ups);
        } catch (ExecutionException e) {
            throw new VitamRuntimeException(e);
        }

        for (Map.Entry<String, Document> unit : units.entrySet()) {
            Document au = unit.getValue();
            originatingAgencies.addAll(au.get(Unit.ORIGINATING_AGENCIES, List.class));
        }
    }


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
