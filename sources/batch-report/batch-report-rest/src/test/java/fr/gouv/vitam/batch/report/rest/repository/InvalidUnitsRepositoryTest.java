package fr.gouv.vitam.batch.report.rest.repository;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InvalidUnitsRepositoryTest {

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock private SimpleMongoDBAccess simpleMongoDBAccess;
    @Mock private MongoDatabase mongoDatabase;
    @Mock private MongoCollection<Document> mongoCollection;
    @Mock private AggregateIterable<Document> aggregateIterable;
    @Mock private MongoCursor<Document> mongoCursor;

    @Captor private ArgumentCaptor<ArrayList<WriteModel<Document>>> captorBulkWrite;
    @Captor private ArgumentCaptor<Bson> captorDeleteMany;
    @Captor private ArgumentCaptor<ArrayList<Bson>> captorAggregate;

    private InvalidUnitsRepository invalidUnitsRepository;

    @Before
    public void setUp() {
        when(mongoDatabase.getCollection(anyString())).thenReturn(mongoCollection);
        when(simpleMongoDBAccess.getMongoDatabase()).thenReturn(mongoDatabase);
        invalidUnitsRepository = new InvalidUnitsRepository(simpleMongoDBAccess, "InvalidUnits");
    }

    @Test
    public void bulkAppendUnits_should_call_bulkWrite_correctly() {
        when(mongoCollection.bulkWrite(anyList())).thenReturn(null);

        String processId = "processId1";
        List<String> unitList = new ArrayList<>();
        unitList.add("unit1");
        unitList.add("unit2");

        List<WriteModel<Document>> expectedUpdate = new ArrayList<>();
        for(String unit : unitList) {
            Document doc = new Document("_id", unit);
            doc.append("processId", processId);
            UpdateOneModel<Document> update = new UpdateOneModel<>(doc, new Document("$set", doc), new UpdateOptions().upsert(true));
            expectedUpdate.add(update);
        }

        invalidUnitsRepository.bulkAppendUnits(unitList, processId);
        verify(mongoCollection).bulkWrite(captorBulkWrite.capture());
        ArrayList<WriteModel<Document>> update = captorBulkWrite.getValue();
        assertEquals(update.toString(), expectedUpdate.toString());
    }

    @Test
    public void deleteUnitsAndProgeny_should_call_deleteMany_correctly() {
        when(mongoCollection.deleteMany(any(Bson.class))).thenReturn(null);

        String processId = "processId1";
        Bson expectedFilter = eq("processId", processId);

        invalidUnitsRepository.deleteUnitsAndProgeny(processId);
        verify(mongoCollection).deleteMany(captorDeleteMany.capture());
        Bson actualFilter = captorDeleteMany.getValue();

        assertEquals(expectedFilter.toString(), actualFilter.toString());
    }

    @Test
    public void findUnitsByProcessId_should_call_aggregate_correctly() {
        when(mongoCollection.aggregate(anyList())).thenReturn(aggregateIterable);
        when(aggregateIterable.allowDiskUse(anyBoolean())).thenReturn(aggregateIterable);
        when(aggregateIterable.iterator()).thenReturn(mongoCursor);

        String processId = "processId1";
        Bson expected = match(eq("processId", processId));

        invalidUnitsRepository.findUnitsByProcessId(processId);
        verify(mongoCollection).aggregate(captorAggregate.capture());
        List<Bson> actual = captorAggregate.getValue();

        assertThat(actual.size()).isEqualTo(1);
        assertEquals(actual.get(0).toString(), expected.toString());
    }
}