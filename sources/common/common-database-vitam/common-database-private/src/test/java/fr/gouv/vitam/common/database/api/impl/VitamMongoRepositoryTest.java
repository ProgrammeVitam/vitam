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

import com.fasterxml.jackson.databind.node.ObjectNode;
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
import fr.gouv.vitam.common.database.server.mongodb.CollectionSample;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mongo.MongoRule;
import org.apache.commons.lang3.RandomUtils;
import org.bson.Document;
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
        ObjectNode data = JsonHandler.createObjectNode()
            .put(VitamDocument.ID, id)
            .put(VitamDocument.TENANT_ID, tenant)
            .put(TITLE, TEST_SAVE);

        Document document = Document.parse(JsonHandler.unprettyPrint(data));
        repository.save(document);

        Optional<Document> response = repository.getByID(id, tenant);
        assertThat(response).isPresent();
        assertThat(response.get().getString(TITLE)).contains(TEST_SAVE);
    }

    @Test
    public void testSaveOrUpdateOneDocumentAndGetByIDOK() throws IOException, DatabaseException {
        String id = GUIDFactory.newGUID().toString();
        Integer tenant = 0;
        ObjectNode data = JsonHandler.createObjectNode()
            .put(VitamDocument.ID, id)
            .put(VitamDocument.TENANT_ID, tenant)
            .put(TITLE, TEST_SAVE);
        Document document = Document.parse(JsonHandler.unprettyPrint(data));
        VitamRepositoryStatus result = repository.saveOrUpdate(document);

        assertThat(VitamRepositoryStatus.CREATED.equals(result)).isTrue();
        Optional<Document> response = repository.getByID(id, tenant);
        assertThat(response).isPresent();
        assertThat(response.get().getString(TITLE)).contains(TEST_SAVE);

        data = JsonHandler.createObjectNode()
            .put(VitamDocument.ID, id)
            .put(VitamDocument.TENANT_ID, tenant)
            .put(TITLE, "Test othersave");

        document = Document.parse(JsonHandler.unprettyPrint(data));
        result = repository.saveOrUpdate(document);

        assertThat(VitamRepositoryStatus.UPDATED.equals(result)).isTrue();
        response = repository.getByID(id, tenant);
        assertThat(response).isPresent();
        assertThat(response.get().getString(TITLE)).contains("Test othersave");
    }

    @Test
    public void testSaveMultipleDocumentsOK() throws IOException, DatabaseException {
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ObjectNode data = JsonHandler.createObjectNode()
                .put(VitamDocument.ID, GUIDFactory.newGUID().toString())
                .put(VitamDocument.TENANT_ID, 0)
                .put(TITLE, TEST_SAVE + RandomUtils.nextDouble());

            documents.add(Document.parse(JsonHandler.unprettyPrint(data)));
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
            ObjectNode data = JsonHandler.createObjectNode()
                .put(VitamDocument.ID, 1000 + i)
                .put(VitamDocument.TENANT_ID, 0)
                .put("Title", "Test save " + i);
            documents.add(Document.parse(JsonHandler.unprettyPrint(data)));
        }
        repository.saveOrUpdate(documents);

        long count = collection.countDocuments();
        assertThat(count).isEqualTo(100);

        // updates
        List<Document> updatedDocuments = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            // change the title
            ObjectNode data = JsonHandler.createObjectNode()
                .put(VitamDocument.ID, 1000 + i)
                .put(VitamDocument.TENANT_ID, 0)
                .put("Title", "Test save updated");
            updatedDocuments.add(Document.parse(JsonHandler.unprettyPrint(data)));
        }
        for (int i = 50; i < 100; i++) {
            // same document
            ObjectNode data = JsonHandler.createObjectNode()
                .put(VitamDocument.ID, 1000 + i)
                .put(VitamDocument.TENANT_ID, 0)
                .put("Title", "Test save " + i);
            updatedDocuments.add(Document.parse(JsonHandler.unprettyPrint(data)));
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
            ObjectNode data = JsonHandler.createObjectNode()
                .put(VitamDocument.ID, 1000 + i)
                .put(VitamDocument.TENANT_ID, 0)
                .put("Title", "Test save " + i)
                .put("Description", "Description _ " + i)
                .put("_glpd", date);
            documents.add(Document.parse(JsonHandler.unprettyPrint(data)));
        }

        repository.saveOrUpdate(documents);

        long count = collection.countDocuments();
        assertThat(count).isEqualTo(2);

        // updates

        List<WriteModel<Document>> updates = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ObjectNode doc = JsonHandler.createObjectNode()
                .put(VitamDocument.ID, 1000 + i)
                .put(VitamDocument.TENANT_ID, 0)
                .put("Title", "Test save update");
            Document data = new Document("$set", Document.parse(JsonHandler.unprettyPrint(doc)));
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

        updates = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ObjectNode doc = JsonHandler.createObjectNode()
                .put(VitamDocument.ID, 1000 + i)
                .put(VitamDocument.TENANT_ID, 0)
                .put("Title", "Test save update");
            Document data = new Document("$set", Document.parse(JsonHandler.unprettyPrint(doc)));
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
            ObjectNode data = JsonHandler.createObjectNode()
                .put(VitamDocument.ID, GUIDFactory.newGUID().toString())
                .put(VitamDocument.TENANT_ID, 0)
                .put(TITLE, TEST_SAVE + RandomUtils.nextDouble());
            documents.add(Document.parse(JsonHandler.unprettyPrint(data)));
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
        ObjectNode data = JsonHandler.createObjectNode()
            .put(VitamDocument.ID, id)
            .put(VitamDocument.TENANT_ID, tenant)
            .put(TITLE, TEST_SAVE);

        Document document = Document.parse(JsonHandler.unprettyPrint(data));
        repository.save(document);

        Optional<Document> response = repository.getByID(id, tenant);
        assertThat(response).isPresent();
        assertThat(response.get().getString(TITLE)).contains(TEST_SAVE);

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
        ObjectNode data = JsonHandler.createObjectNode()
            .put(VitamDocument.ID, id)
            .put(VitamDocument.TENANT_ID, tenant)
            .put("Identifier", "FakeIdentifier")
            .put(TITLE, "Test save");

        Document document = Document.parse(JsonHandler.unprettyPrint(data));
        repository.save(document);

        Optional<Document> response = repository.findByIdentifierAndTenant("FakeIdentifier", tenant);
        assertThat(response).isPresent();
        assertThat(response.get().getString(TITLE)).contains("Test save");
    }

    @Test
    public void testFindByIdentifierFoundOK() throws IOException, DatabaseException {
        String id = GUIDFactory.newGUID().toString();
        ObjectNode data = JsonHandler.createObjectNode()
            .put(VitamDocument.ID, id)
            .put("Identifier", "FakeIdentifier")
            .put(TITLE, TEST_SAVE);

        Document document = Document.parse(JsonHandler.unprettyPrint(data));
        repository.save(document);

        Optional<Document> response = repository.findByIdentifier("FakeIdentifier");
        assertThat(response).isPresent();
        assertThat(response.get().getString(TITLE)).contains(TEST_SAVE);
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
        ObjectNode data = JsonHandler.createObjectNode()
            .put(VitamDocument.ID, id)
            .put(VitamDocument.TENANT_ID, tenant)
            .put("Identifier", "FakeIdentifier")
            .put("Name", "FakeName")
            .put("Title", "Test save");

        Document document = Document.parse(JsonHandler.unprettyPrint(data));
        repository.save(document);

        Optional<Document> response = repository.findByIdentifierAndTenant("FakeIdentifier", tenant);
        assertThat(response).isPresent();
        assertThat(response.get().getString("Title")).contains("Test save");

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
            ObjectNode data = JsonHandler.createObjectNode()
                .put(VitamDocument.ID, GUIDFactory.newGUID().toString())
                .put(VitamDocument.TENANT_ID, 0)
                .put(TITLE, TEST_SAVE + i + " " + RandomUtils.nextDouble());
            documents.add(Document.parse(JsonHandler.unprettyPrint(data)));
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
            ObjectNode data = JsonHandler.createObjectNode()
                .put(VitamDocument.ID, GUIDFactory.newGUID().toString())
                .put(VitamDocument.TENANT_ID, 0)
                .put(TITLE, TEST_SAVE + i + " " + RandomUtils.nextDouble())
                .put("TestField", String.valueOf(i))
                .put("OtherTestField", String.valueOf("Toto"));
            documents.add(Document.parse(JsonHandler.unprettyPrint(data)));
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
            ObjectNode data = JsonHandler.createObjectNode()
                .put(VitamDocument.ID, GUIDFactory.newGUID().toString())
                .put(VitamDocument.TENANT_ID, 0)
                .put(TITLE, TEST_SAVE + i + " " + RandomUtils.nextDouble())
                .put("TestField", String.valueOf(i))
                .put("OtherTestField", String.valueOf("Toto"));
            documents.add(Document.parse(JsonHandler.unprettyPrint(data)));
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
