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
package fr.gouv.vitam.metadata.core.graph;

import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Projections.include;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.WriteModel;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.core.database.collections.VitamRepositoryProvider;
import fr.gouv.vitam.metadata.core.graph.api.GraphBuilderService;
import org.bson.Document;

/**
 * This class compute graph for unit and object group
 */
public class GraphBuilderServiceImpl implements GraphBuilderService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(GraphBuilderServiceImpl.class);

    private VitamRepositoryProvider vitamRepositoryProvider;


    private static AtomicBoolean alreadyRunningLock = new AtomicBoolean(false);

    /**
     * @param vitamRepositoryProvider
     */
    public GraphBuilderServiceImpl(VitamRepositoryProvider vitamRepositoryProvider) {
        this.vitamRepositoryProvider = vitamRepositoryProvider;
    }

    @Override
    public Map<MetadataCollections, Integer> computeGraph() throws GraphBuilderException {
        boolean tryBuildGraph = alreadyRunningLock.compareAndSet(false, true);
        final Map<MetadataCollections, Integer> map = new HashMap<>();
        map.put(MetadataCollections.UNIT, 0);
        map.put(MetadataCollections.OBJECTGROUP, 0);
        Integer totalTreatedDocuments = 0;

        if (tryBuildGraph) {
            try {
                final VitamThreadPoolExecutor executor = VitamThreadPoolExecutor.getDefaultExecutor();

                CompletableFuture.supplyAsync(() -> {
                    Integer numberOfDocuments = computeGraph(MetadataCollections.UNIT, null);
                    map.put(MetadataCollections.UNIT, numberOfDocuments);
                    return numberOfDocuments;
                }, executor).get();

                CompletableFuture.supplyAsync(() -> {
                    Integer numberOfDocuments = computeGraph(MetadataCollections.OBJECTGROUP, null);
                    map.put(MetadataCollections.OBJECTGROUP, numberOfDocuments);
                    return numberOfDocuments;
                }, executor).get();

                totalTreatedDocuments = map.get(MetadataCollections.UNIT) + map.get(MetadataCollections.OBJECTGROUP);
            } catch (InterruptedException | ExecutionException e) {
                throw new GraphBuilderException(e);
            } finally {
                alreadyRunningLock.set(false);
            }
            LOGGER.warn(
                "Compute Graph of GOT/UNIT total : " + totalTreatedDocuments + " : Stats (" + map.toString() + ")");

        } else {
            LOGGER.warn("Compute graph is already running ...");
        }
        return map;


    }

    //TODO: the query that select elements subject of build of the graph
    @Override
    public Integer computeGraph(MetadataCollections metadataCollections, JsonNode queryDSL) {

        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID().getId());

        LOGGER.warn("Start Compute graph of (" + metadataCollections.name() + ") ...");

        try {
            // TODO: 5/16/18 Refactor if queryDSL is provided
            final MongoCursor<Document> cursor = vitamRepositoryProvider
                .getVitamMongoRepository(metadataCollections)
                .findDocuments(exists(Unit.GRAPH_LAST_PERSISTED_DATE, false), VitamConfiguration.getBatchSize())
                .projection(include(Unit.UP, Unit.ORIGINATING_AGENCY))
                .iterator();

            List<Document> documents = new ArrayList<>();

            int totalTreatedDocuments = 0;
            while (cursor.hasNext()) {
                documents.add(cursor.next());
                if (!cursor.hasNext() || documents.size() >= VitamConfiguration.getBatchSize()) {
                    totalTreatedDocuments = totalTreatedDocuments + documents.size();
                    computeGraph(metadataCollections, documents, true);
                    documents = new ArrayList<>();
                }
            }

            LOGGER.warn("End Compute graph  of (" + metadataCollections.name() + ")");
            return totalTreatedDocuments;
        } catch (GraphBuilderException e) {
            LOGGER.error(String
                    .format("[Consistency ERROR] : Error while compute graph of (%s)", metadataCollections.name()),
                e);
            return 0;
        }

    }

    @Override
    public void bulkUpdateMongo(MetadataCollections metaDaCollection, List<WriteModel<Document>> collection)
        throws DatabaseException {
        this.vitamRepositoryProvider.getVitamMongoRepository(metaDaCollection).update(collection);
    }

    @Override
    public void bulkElasticsearch(MetadataCollections metaDaCollection, Set<String> collection)
        throws DatabaseException {

        if (collection.isEmpty()) {
            return;
        }

        FindIterable<Document> fit =
            this.vitamRepositoryProvider.getVitamMongoRepository(metaDaCollection).findDocuments(collection, null);
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
        this.vitamRepositoryProvider.getVitamESRepository(metaDaCollection).save(collection);
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
}
