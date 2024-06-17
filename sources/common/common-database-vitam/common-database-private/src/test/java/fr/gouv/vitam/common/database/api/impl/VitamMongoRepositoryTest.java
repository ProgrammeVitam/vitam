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
package fr.gouv.vitam.common.database.api.impl;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.api.VitamRepositoryStatus;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.database.server.mongodb.CollectionSample;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import org.apache.commons.lang3.RandomUtils;
import org.bson.Document;
import org.elasticsearch.common.Strings;
import org.elasticsearch.xcontent.XContentBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.elasticsearch.xcontent.XContentFactory.jsonBuilder;

/**
 *
 */
public class VitamMongoRepositoryTest {

    private static final String TEST_COLLECTION = "VitamMongoRepository" + GUIDFactory.newGUID().getId();
    private static final String TITLE = "Title";
    private static final String TEST_SAVE = "Test save ";
    private static VitamMongoRepository repository;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(CollectionSample.class),
        TEST_COLLECTION
    );

    @Before
    public void setUpBeforeClass() throws Exception {
        repository = new VitamMongoRepository(mongoRule.getMongoCollection(TEST_COLLECTION));
    }

    @Test
    public void testSaveOneDocumentAndGetByIDOK() throws IOException, DatabaseException {
        String id = GUIDFactory.newGUID().toString();
        Integer tenant = 0;
        XContentBuilder builder = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, id)
            .field(VitamDocument.TENANT_ID, tenant)
            .field(TITLE, TEST_SAVE)
            .endObject();

        Document document = Document.parse(Strings.toString(builder));
        repository.save(document);

        Optional<Document> response = repository.getByID(id, tenant);
        assertThat(response).isPresent();
        assertThat(response.get()).extracting(TITLE).contains(TEST_SAVE);
    }

    @Test
    public void testSaveOrUpdateOneDocumentAndGetByIDOK() throws IOException, DatabaseException {
        String id = GUIDFactory.newGUID().toString();
        Integer tenant = 0;
        XContentBuilder builder = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, id)
            .field(VitamDocument.TENANT_ID, tenant)
            .field(TITLE, TEST_SAVE)
            .endObject();

        Document document = Document.parse(Strings.toString(builder));
        VitamRepositoryStatus result = repository.saveOrUpdate(document);

        assertThat(VitamRepositoryStatus.CREATED.equals(result)).isTrue();
        Optional<Document> response = repository.getByID(id, tenant);
        assertThat(response).isPresent();
        assertThat(response.get()).extracting(TITLE).contains(TEST_SAVE);

        builder = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, id)
            .field(VitamDocument.TENANT_ID, tenant)
            .field(TITLE, "Test othersave")
            .endObject();

        document = Document.parse(Strings.toString(builder));
        result = repository.saveOrUpdate(document);

        assertThat(VitamRepositoryStatus.UPDATED.equals(result)).isTrue();
        response = repository.getByID(id, tenant);
        assertThat(response).isPresent();
        assertThat(response.get()).extracting(TITLE).contains("Test othersave");
    }

    @Test
    public void testSaveMultipleDocumentsOK() throws IOException, DatabaseException {
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field(VitamDocument.ID, GUIDFactory.newGUID().toString())
                .field(VitamDocument.TENANT_ID, 0)
                .field(TITLE, TEST_SAVE + RandomUtils.nextDouble())
                .endObject();
            documents.add(Document.parse(Strings.toString(builder)));
        }
        repository.save(documents);

        MongoCollection<Document> collection = mongoRule.getMongoCollection(TEST_COLLECTION);

        long count = collection.countDocuments();
        assertThat(count).isEqualTo(100);
    }

    @Test
    public void testSaveOrUpdateMultipleDocumentsOK() throws IOException, DatabaseException {
        MongoCollection<Document> collection = mongoRule.getMongoCollection(TEST_COLLECTION);

        // inserts
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field(VitamDocument.ID, 1000 + i)
                .field(VitamDocument.TENANT_ID, 0)
                .field("Title", "Test save " + i)
                .endObject();
            documents.add(Document.parse(Strings.toString(builder)));
        }
        repository.saveOrUpdate(documents);

        long count = collection.countDocuments();
        assertThat(count).isEqualTo(100);

        // updates
        List<Document> updatedDocuments = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            // change the title
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field(VitamDocument.ID, 1000 + i)
                .field(VitamDocument.TENANT_ID, 0)
                .field("Title", "Test save updated")
                .endObject();
            updatedDocuments.add(Document.parse(Strings.toString(builder)));
        }
        for (int i = 50; i < 100; i++) {
            // same document
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field(VitamDocument.ID, 1000 + i)
                .field(VitamDocument.TENANT_ID, 0)
                .field("Title", "Test save " + i)
                .endObject();
            updatedDocuments.add(Document.parse(Strings.toString(builder)));
        }
        repository.saveOrUpdate(updatedDocuments);

        count = collection.countDocuments();
        assertThat(count).isEqualTo(100);
        count = collection.countDocuments(Filters.eq("Title", "Test save updated"));
        assertThat(count).isEqualTo(50);
        assertThat(collection.find(Filters.eq(VitamDocument.ID, 1000 + 1)).first().get("Title")).isEqualTo(
            "Test save updated"
        );
    }

    @Test
    public void testBulkUpdateMultipleDocumentsOK() throws IOException, DatabaseException {
        MongoCollection<Document> collection = mongoRule.getMongoCollection(TEST_COLLECTION);
        String date = LocalDateUtil.nowFormatted();

        // inserts
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            if (i == 1) {
                date = LocalDateUtil.nowFormatted();
            }
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field(VitamDocument.ID, 1000 + i)
                .field(VitamDocument.TENANT_ID, 0)
                .field("Title", "Test save " + i)
                .field("Description", "Description _ " + i)
                .field("_glpd", date)
                .endObject();
            documents.add(Document.parse(Strings.toString(builder)));
        }

        repository.saveOrUpdate(documents);

        long count = collection.countDocuments();
        assertThat(count).isEqualTo(2);

        // updates

        List<WriteModel<Document>> updates = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String doc = Strings.toString(
                jsonBuilder()
                    .startObject()
                    .field(VitamDocument.ID, 1000 + i)
                    .field(VitamDocument.TENANT_ID, 0)
                    .field("Title", "Test save update")
                    .endObject()
            );
            Document data = new Document("$set", Document.parse(doc));
            updates.add(
                new UpdateOneModel<>(
                    and(eq(ID, 1000 + i)),
                    data,
                    new UpdateOptions().upsert(true).bypassDocumentValidation(true)
                )
            );
        }

        repository.update(updates);

        count = collection.countDocuments();

        assertThat(count).isEqualTo(3);

        MongoCursor<Document> cursor = collection.find().iterator();
        while (cursor.hasNext()) {
            System.err.println(BsonHelper.stringify(cursor.next()));
        }

        updates = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String doc = Strings.toString(
                jsonBuilder()
                    .startObject()
                    .field(VitamDocument.ID, 1000 + i)
                    .field(VitamDocument.TENANT_ID, 0)
                    .field("Title", "Test save update")
                    .endObject()
            );
            Document data = new Document("$set", Document.parse(doc));
            updates.add(
                new UpdateOneModel<>(and(eq(ID, 1000 + i), eq("_glpd", date)), data, new UpdateOptions().upsert(true))
            );
        }

        try {
            repository.update(updates);
            fail("should throw duplicate key MongoBulkWriteException");
        } catch (DatabaseException e) {
            assertThat(e.getCause()).isInstanceOf(MongoBulkWriteException.class);
            MongoBulkWriteException err = (MongoBulkWriteException) e.getCause();
            assertThat(err.getMessage()).contains("duplicate key");
        }
    }

    @Test
    public void testSaveMultipleDocumentsAndPurgeDocumentsOK() throws IOException, DatabaseException {
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field(VitamDocument.ID, GUIDFactory.newGUID().toString())
                .field(VitamDocument.TENANT_ID, 0)
                .field(TITLE, TEST_SAVE + RandomUtils.nextDouble())
                .endObject();
            documents.add(Document.parse(Strings.toString(builder)));
        }
        repository.save(documents);

        MongoCollection<Document> collection = mongoRule.getMongoCollection(TEST_COLLECTION);

        long count = collection.countDocuments();
        assertThat(count).isEqualTo(101);

        // purge tenant 0
        long deleted = repository.purge(0);
        assertThat(deleted).isEqualTo(101);

        // purge all other tenants
        deleted = repository.purge();
        assertThat(deleted).isEqualTo(0);
    }

    @Test
    public void testRemoveOK() throws IOException, DatabaseException {
        String id = GUIDFactory.newGUID().toString();
        Integer tenant = 0;
        XContentBuilder builder = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, id)
            .field(VitamDocument.TENANT_ID, tenant)
            .field(TITLE, TEST_SAVE)
            .endObject();

        Document document = Document.parse(Strings.toString(builder));
        repository.save(document);

        Optional<Document> response = repository.getByID(id, tenant);
        assertThat(response).isPresent();
        assertThat(response.get()).extracting(TITLE).contains(TEST_SAVE);

        repository.remove(id, tenant);
        response = repository.getByID(id, tenant);
        assertThat(response).isEmpty();
    }

    @Test(expected = DatabaseException.class)
    public void testRemoveNotExists() throws DatabaseException {
        String id = GUIDFactory.newGUID().toString();
        Integer tenant = 0;
        repository.remove(id, tenant);
    }

    @Test
    public void testGetByIDNotExistsOK() throws DatabaseException {
        Optional<Document> response = repository.getByID(GUIDFactory.newGUID().toString(), 0);
        assertThat(response).isEmpty();
    }

    @Test
    public void testFindByIdentifierAndTenantFoundOK() throws IOException, DatabaseException {
        String id = GUIDFactory.newGUID().toString();
        Integer tenant = 0;
        XContentBuilder builder = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, id)
            .field(VitamDocument.TENANT_ID, tenant)
            .field("Identifier", "FakeIdentifier")
            .field(TITLE, "Test save")
            .endObject();

        Document document = Document.parse(Strings.toString(builder));
        repository.save(document);

        Optional<Document> response = repository.findByIdentifierAndTenant("FakeIdentifier", tenant);
        assertThat(response).isPresent();
        assertThat(response.get()).extracting(TITLE).contains("Test save");
    }

    @Test
    public void testFindByIdentifierFoundOK() throws IOException, DatabaseException {
        String id = GUIDFactory.newGUID().toString();
        XContentBuilder builder = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, id)
            .field("Identifier", "FakeIdentifier")
            .field(TITLE, TEST_SAVE)
            .endObject();

        Document document = Document.parse(Strings.toString(builder));
        repository.save(document);

        Optional<Document> response = repository.findByIdentifier("FakeIdentifier");
        assertThat(response).isPresent();
        assertThat(response.get()).extracting(TITLE).contains(TEST_SAVE);
    }

    @Test
    public void testFindByIdentifierAndTenantFoundEmpty() throws DatabaseException {
        Integer tenant = 0;
        Optional<Document> response = repository.findByIdentifierAndTenant("FakeIdentifier", tenant);
        assertThat(response).isEmpty();
    }

    @Test
    public void testFindByIdentifierFoundEmpty() throws DatabaseException {
        Optional<Document> response = repository.findByIdentifier("FakeIdentifier");
        assertThat(response).isEmpty();
    }

    @Test
    public void testRemoveByNameAndTenantOK() throws IOException, DatabaseException {
        String id = GUIDFactory.newGUID().toString();
        Integer tenant = 0;
        XContentBuilder builder = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, id)
            .field(VitamDocument.TENANT_ID, tenant)
            .field("Identifier", "FakeIdentifier")
            .field("Name", "FakeName")
            .field("Title", "Test save")
            .endObject();

        Document document = Document.parse(Strings.toString(builder));
        repository.save(document);

        Optional<Document> response = repository.findByIdentifierAndTenant("FakeIdentifier", tenant);
        assertThat(response).isPresent();
        assertThat(response.get()).extracting("Title").contains("Test save");

        repository.removeByNameAndTenant("FakeName", tenant);
        response = repository.findByIdentifierAndTenant("FakeIdentifier", tenant);
        assertThat(response).isEmpty();
    }

    @Test(expected = DatabaseException.class)
    public void testRemoveByNameAndTenantNotExistingOK() throws DatabaseException {
        Integer tenant = 0;
        repository.removeByNameAndTenant("FakeName", tenant);
    }

    @Test
    public void testFindDocuments() throws Exception {
        // Given
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field(VitamDocument.ID, GUIDFactory.newGUID().toString())
                .field(VitamDocument.TENANT_ID, 0)
                .field(TITLE, TEST_SAVE + i + " " + RandomUtils.nextDouble())
                .endObject();
            documents.add(Document.parse(Strings.toString(builder)));
        }
        // When
        repository.save(documents);
        FindIterable<Document> iterable = repository.findDocuments(2, 0);
        MongoCursor<Document> cursor;
        cursor = iterable.iterator();
        List<Document> docs = getDocuments(cursor, 2);
        // Then
        assertThat(docs.size()).isEqualTo(2);
        while (!docs.isEmpty()) {
            docs = getDocuments(cursor, 2);
        }
        assertThat(docs.size()).isEqualTo(0);
    }

    @Test
    public void testFindByFieldsDocuments() throws Exception {
        // Given
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field(VitamDocument.ID, GUIDFactory.newGUID().toString())
                .field(VitamDocument.TENANT_ID, 0)
                .field(TITLE, TEST_SAVE + i + " " + RandomUtils.nextDouble())
                .field("TestField", String.valueOf(i))
                .field("OtherTestField", String.valueOf("Toto"))
                .endObject();
            documents.add(Document.parse(Strings.toString(builder)));
        }
        // When
        repository.save(documents);
        Map<String, String> filter = new HashMap<>();
        filter.put("OtherTestField", "Toto");
        filter.put("TestField", "5");
        FindIterable<Document> iterable = repository.findByFieldsDocuments(filter, 5, 0);
        MongoCursor<Document> cursor;
        cursor = iterable.iterator();
        List<Document> docs = getDocuments(cursor, 5);
        assertThat(docs.get(0).get("TestField")).isEqualTo("5");
        assertThat(docs.get(0).get("OtherTestField")).isEqualTo("Toto");
        // Then
        assertThat(docs.size()).isEqualTo(1);
        while (!docs.isEmpty()) {
            docs = getDocuments(cursor, 1);
        }
        assertThat(docs.size()).isEqualTo(0);
    }

    @Test
    public void testFindByEmptyFieldsDocuments() throws Exception {
        // Given
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field(VitamDocument.ID, GUIDFactory.newGUID().toString())
                .field(VitamDocument.TENANT_ID, 0)
                .field(TITLE, TEST_SAVE + i + " " + RandomUtils.nextDouble())
                .field("TestField", String.valueOf(i))
                .field("OtherTestField", String.valueOf("Toto"))
                .endObject();
            documents.add(Document.parse(Strings.toString(builder)));
        }
        // When
        repository.save(documents);
        Map<String, String> filter = new HashMap<>();
        FindIterable<Document> iterable = repository.findByFieldsDocuments(filter, 5, 0);
        MongoCursor<Document> cursor;
        cursor = iterable.iterator();
        List<Document> docs = getDocuments(cursor, 5);
        assertThat(docs.get(0).get("TestField")).isEqualTo("0");
        assertThat(docs.get(0).get("OtherTestField")).isEqualTo("Toto");
        // Then
        assertThat(docs.size()).isEqualTo(5);
        while (!docs.isEmpty()) {
            docs = getDocuments(cursor, 1);
        }
        assertThat(docs.size()).isEqualTo(0);
    }

    private List<Document> getDocuments(MongoCursor<Document> cursor, int batchSize) {
        int cpt = 0;
        List<Document> documents = new ArrayList<>();
        while (cpt < batchSize && cursor.hasNext()) {
            documents.add(cursor.next());
            cpt++;
        }
        return documents;
    }
}
