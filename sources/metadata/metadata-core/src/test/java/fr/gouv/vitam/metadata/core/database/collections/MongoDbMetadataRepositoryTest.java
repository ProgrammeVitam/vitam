package fr.gouv.vitam.metadata.core.database.collections;

import com.google.common.collect.Lists;
import com.mongodb.client.MongoCollection;

import com.mongodb.client.model.Updates;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.VERSION;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataCollections.UNIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import java.util.HashMap;
import java.util.Map;

public class MongoDbMetadataRepositoryTest {

    public static final int TENANT_ID = 0;

    public static final String PREFIX = GUIDFactory.newGUID().getId();


    @Rule
    public MongoRule mongoRule =
        new MongoRule(VitamCollection.getMongoClientOptions(Lists.newArrayList(Unit.class)),
            PREFIX + UNIT.getName());

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private MongoDbMetadataRepository<Unit> unitMongoDbMetadataRepository;

    @Before
    public void setUp() throws Exception {
        unitMongoDbMetadataRepository =
            new MongoDbMetadataRepository<>(() -> mongoRule.getMongoCollection(PREFIX + UNIT.getName(), Unit.class));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void should_store_many_document_in_bulk_mode() throws Exception {
        // Given
        String id1 = GUIDFactory.newUnitGUID(TENANT_ID).toString();
        String id2 = GUIDFactory.newUnitGUID(TENANT_ID).toString();
        Unit unit1 = createUnit(id1);
        Unit unit2 = createUnit(id2);

        // When
        unitMongoDbMetadataRepository.insert(Lists.newArrayList(unit1, unit2));

        // Then
        MongoCollection<Document> mongoCollection = mongoRule.getMongoCollection(PREFIX + UNIT.getName());
        assertThat(mongoCollection.countDocuments()).isEqualTo(2);
        assertThat(mongoCollection.find())
            .extracting("_id", VitamDocument.TENANT_ID, VERSION)
            .containsExactly(tuple(id1, TENANT_ID, 0), tuple(id2, TENANT_ID, 0));
    }

    @Test
    @RunWithCustomExecutor
    public void should_update_many_document_in_bulk_mode() throws Exception {
        // Given
        String id1 = GUIDFactory.newUnitGUID(TENANT_ID).toString();
        String id2 = GUIDFactory.newUnitGUID(TENANT_ID).toString();
        Unit unit1 = createUnit(id1);
        Unit unit2 = createUnit(id2);

        unitMongoDbMetadataRepository.insert(Lists.newArrayList(unit1, unit2));

        Map<String, Bson> updates = new HashMap<>();
        updates.put(id1, Updates.set("title", "unit1"));
        updates.put(id2, Updates.set("title", "unit2"));

        // When
        unitMongoDbMetadataRepository.update(updates);

        // Then
        MongoCollection<Document> mongoCollection = mongoRule.getMongoCollection(PREFIX + UNIT.getName());
        assertThat(mongoCollection.countDocuments()).isEqualTo(2);
        assertThat(mongoCollection.find())
            .extracting("title")
            .containsExactly("unit1", "unit2");
    }

    @Test
    @RunWithCustomExecutor
    public void should_fail_when_store_two_document_with_same_id() throws Exception {
        // Given
        String id1 = GUIDFactory.newUnitGUID(TENANT_ID).toString();
        String id3 = GUIDFactory.newUnitGUID(TENANT_ID).toString();
        Unit unit1 = createUnit(id1);
        Unit unit2 = createUnit(id1);
        Unit unit3 = createUnit(id3);

        // When
        assertThatThrownBy(() -> unitMongoDbMetadataRepository.insert(Lists.newArrayList(unit1, unit2, unit3)))
            .isInstanceOf(MetaDataAlreadyExistException.class).hasMessageContaining(id1);
    }


    @Test
    @RunWithCustomExecutor
    public void should_delete_many_document_in_bulk_mode() throws Exception {
        // Given
        String id1 = GUIDFactory.newUnitGUID(TENANT_ID).toString();
        String id2 = GUIDFactory.newUnitGUID(TENANT_ID).toString();

        Unit unit1 = createUnit(id1);
        Unit unit2 = createUnit(id2);

        unitMongoDbMetadataRepository.insert(Lists.newArrayList(unit1, unit2));

        unitMongoDbMetadataRepository.delete(Lists.newArrayList(unit1, unit2));

        MongoCollection<Document> mongoCollection = mongoRule.getMongoCollection(PREFIX + UNIT.getName());


        assertThat(mongoCollection.find()).isEmpty();
    }

    private Unit createUnit(String id) {
        Unit unit1 = new Unit();
        unit1.put(Unit.ID, id);
        return unit1;
    }

}
