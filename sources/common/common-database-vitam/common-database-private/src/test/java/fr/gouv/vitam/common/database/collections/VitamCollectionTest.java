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
package fr.gouv.vitam.common.database.collections;

import com.mongodb.ReadConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexSettings;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.CollectionSample;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWithCustomExecutor
public class VitamCollectionTest {

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    public static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(CollectionSample.class),
        PREFIX + CollectionSample.class.getSimpleName()
    );

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule(
        ElasticsearchIndexAlias.ofCrossTenantCollection(PREFIX + CollectionSample.class.getSimpleName()).getName()
    );

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private static ElasticsearchAccess esClient;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final List<ElasticsearchNode> nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode(ElasticsearchRule.getHost(), elasticsearchRule.getPort()));

        esClient = new ElasticsearchAccess(elasticsearchRule.getClusterName(), nodes);
        esClient.createIndexAndAliasIfAliasNotExists(
            ElasticsearchIndexAlias.ofCrossTenantCollection(PREFIX + CollectionSample.class.getSimpleName()),
            new ElasticsearchIndexSettings(2, 2, () -> "{}")
        );
    }

    @AfterClass
    public static void tearDownAfterClass() throws DatabaseException {
        mongoRule.handleAfterClass();
        elasticsearchRule.purgeIndices();
        esClient.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldCreateVitamCollection() {
        final List<Class<?>> classList = new ArrayList<>();
        classList.add(CollectionSample.class);
        VitamDescriptionResolver vitamDescriptionResolver = new VitamDescriptionResolver(Collections.emptyList());
        final VitamCollection vitamCollection = VitamCollectionHelper.getCollection(
            CollectionSample.class,
            true,
            false,
            PREFIX + CollectionSample.class.getSimpleName(),
            vitamDescriptionResolver
        );

        assertEquals(vitamCollection.getClasz(), CollectionSample.class);
        vitamCollection.initialize(esClient);
        assertEquals(esClient, vitamCollection.getEsClient());
        vitamCollection.initialize(mongoRule.getMongoDatabase(), true);
        assertEquals("majority", mongoRule.getMongoDatabase().getWriteConcern().getWString());
        assertEquals(null, mongoRule.getMongoDatabase().getWriteConcern().getJournal());
        assertEquals(ReadConcern.MAJORITY, mongoRule.getMongoDatabase().getReadConcern());
        final MongoCollection<CollectionSample> collection = (MongoCollection<
                CollectionSample
            >) vitamCollection.getCollection();
        String guid = GUIDFactory.newGUID().toString();
        final CollectionSample test = new CollectionSample(new Document("_id", guid));
        collection.insertOne(test);
        assertEquals(1, collection.countDocuments());
        MongoCursor<CollectionSample> iterable = collection.find().iterator();
        assertTrue(iterable.hasNext());
        CollectionSample sample = iterable.next();
        assertEquals(guid, sample.getId());
    }
}
