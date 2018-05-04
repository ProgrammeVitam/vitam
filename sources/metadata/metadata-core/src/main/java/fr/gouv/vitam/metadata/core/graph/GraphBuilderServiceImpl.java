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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.WriteModel;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUID;
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
 * This class get units where calculated data are modified
 * Zip generated files and store the zipped file in the offer.
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
    public Map<MetadataCollections, Integer> buildGraph() throws GraphBuilderException {
        boolean tryStore = alreadyRunningLock.compareAndSet(false, true);
        final Map<MetadataCollections, Integer> map = new HashMap<>();
        map.put(MetadataCollections.UNIT, 0);
        map.put(MetadataCollections.OBJECTGROUP, 0);
        Integer totalTreatedDocuments = 0;

        if (tryStore) {
            try {
                final VitamThreadPoolExecutor executor = VitamThreadPoolExecutor.getDefaultExecutor();

                CompletableFuture<Integer>[] futures = new CompletableFuture[] {
                    CompletableFuture.supplyAsync(() -> {
                        Integer numberOfDocuments = buildGraph(MetadataCollections.UNIT, null);
                        map.put(MetadataCollections.UNIT, numberOfDocuments);
                        return numberOfDocuments;
                    }, executor)
                    ,
                    CompletableFuture.supplyAsync(() -> {
                        Integer numberOfDocuments = buildGraph(MetadataCollections.OBJECTGROUP, null);
                        map.put(MetadataCollections.OBJECTGROUP, numberOfDocuments);
                        return numberOfDocuments;
                    }, executor)
                };
                // Start async the features
                CompletableFuture<Integer> result = CompletableFuture
                    .allOf(futures)
                    .thenApply(v -> Stream.of(futures).map(CompletableFuture::join).collect(Collectors.toList()))
                    .thenApplyAsync((numberOfDocuments) -> numberOfDocuments.stream().mapToInt(o -> o).sum())
                    .exceptionally(th -> {
                        throw new RuntimeException(th);
                    });

                totalTreatedDocuments = result.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new GraphBuilderException(e);
            } finally {
                alreadyRunningLock.set(false);
            }
            LOGGER.warn("Calculate Graph of GOT/UNIT total : " + totalTreatedDocuments + " : (" + map.toString() + ")");

        } else {
            LOGGER.warn("Calculate graph is already running ...");
        }
        return map;


    }

    /**
     * Should be called only for the method tryStoreGraph
     *
     * @param metadataCollections the collection concerned by the build of the graph
     * @param queryDSL            TODO: the query that select elements subject of build of the graph
     * @return False if an exception occurs of if no unit graph was stored.
     * True if a stored zip file is created and saved in the storage.
     */
    @Override
    public Integer buildGraph(MetadataCollections metadataCollections, JsonNode queryDSL) {

        final GUID storeOperation = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setRequestId(storeOperation);
        final String containerName = storeOperation.getId();

        if (alreadyRunningLock.get()) {
            LOGGER.warn("Start calculate graph of (" + metadataCollections.name() + ") ...");

            try {
                final MongoCursor<Document> cursor = vitamRepositoryProvider
                    .getVitamMongoRepository(metadataCollections)
                    .findDocuments(exists(GRAPH_LAST_PERSISTED_DATE, false), VitamConfiguration.getBatchSize())
                    .projection(include(Unit.UP))
                    .iterator();

                List<Document> documents = new ArrayList<>();

                int totalTreatedDocuments = 0;
                while (cursor.hasNext()) {
                    documents.add(cursor.next());
                    if (!cursor.hasNext() || documents.size() >= VitamConfiguration.getBatchSize()) {
                        totalTreatedDocuments = totalTreatedDocuments + documents.size();
                        calculateGraph(metadataCollections, documents, true);
                        documents = new ArrayList<>();
                    }
                }

                LOGGER.warn("End calculate graph  of (" + metadataCollections.name() + ")");
                return totalTreatedDocuments;
            } catch (GraphBuilderException e) {
                LOGGER.error(String
                        .format("[Consistency ERROR] : Error while calculate graph of (%s)", metadataCollections.name()),
                    e);
                return 0;
            }
        } else {
            LOGGER.warn("Calculate graph of (" + metadataCollections.name() + ") is already running ...");
            return 0;
        }
    }

    /**
     * Bulk write in mongodb
     *
     * @param metaDaCollection
     * @param collection
     * @throws DatabaseException
     */
    @Override
    public void bulkUpdateMongo(MetadataCollections metaDaCollection, List<WriteModel<Document>> collection)
        throws DatabaseException {
        this.vitamRepositoryProvider.getVitamMongoRepository(metaDaCollection).update(collection);
    }

}
