/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.common.database.api.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.mongodb.CollectionSample;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import org.apache.commons.lang3.RandomUtils;
import org.bson.Document;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 */
public class VitamMongoRepositoryTest {
    public static final String TEST_COLLECTION = "TestCollection";
    public static final String VITAM_TEST = "vitam-test";
    private static VitamMongoRepository repository;

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public MongoRule mongoRule =
        new MongoRule(VitamCollection.getMongoClientOptions(Lists.newArrayList(CollectionSample.class)),
            VITAM_TEST,
            TEST_COLLECTION);


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
            .field("Title", "Test save")
            .endObject();

        Document document = Document.parse(builder.string());
        repository.save(document);

        Optional<Document> response = repository.getByID(id, tenant);
        assertThat(response).isPresent();
        assertThat(response.get()).extracting("Title").contains("Test save");
    }

    @Test
    public void testSaveMultipleDocumentsOK() throws IOException, DatabaseException {
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field(VitamDocument.ID, GUIDFactory.newGUID().toString())
                .field(VitamDocument.TENANT_ID, 0)
                .field("Title", "Test save " + RandomUtils.nextDouble())
                .endObject();
            documents.add(Document.parse(builder.string()));
        }
        repository.save(documents);

        MongoCollection<Document> collection = mongoRule.getMongoCollection(TEST_COLLECTION);

        long count = collection.count();
        assertThat(count).isEqualTo(100);
    }

    @Test
    public void testSaveMultipleDocumentsAndPurgeDocumentsOK() throws IOException, DatabaseException {
        List<Document> documents = new ArrayList<>();
        // purge tenant 0
        for (int i = 0; i < 101; i++) {
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field(VitamDocument.ID, GUIDFactory.newGUID().toString())
                .field(VitamDocument.TENANT_ID, 0)
                .field("Title", "Test save " + RandomUtils.nextDouble())
                .endObject();
            documents.add(Document.parse(builder.string()));
        }

        // purge all
        for (int i = 0; i < 101; i++) {
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field(VitamDocument.ID, GUIDFactory.newGUID().toString())
                .field(VitamDocument.TENANT_ID, 1)
                .field("Title", "Test save " + RandomUtils.nextDouble())
                .endObject();
            documents.add(Document.parse(builder.string()));
        }
        repository.save(documents);

        MongoCollection<Document> collection = mongoRule.getMongoCollection(TEST_COLLECTION);

        long count = collection.count();
        assertThat(count).isEqualTo(202);

        // purge tenant 0
        long deleted = repository.purge(0);
        assertThat(deleted).isEqualTo(101);

        // purge all other tenants
        deleted = repository.purge();
        assertThat(deleted).isEqualTo(101);

    }

    @Test
    public void testRemoveOK() throws IOException, DatabaseException {
        String id = GUIDFactory.newGUID().toString();
        Integer tenant = 0;
        XContentBuilder builder = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, id)
            .field(VitamDocument.TENANT_ID, tenant)
            .field("Title", "Test save")
            .endObject();

        Document document = Document.parse(builder.string());
        repository.save(document);

        Optional<Document> response = repository.getByID(id, tenant);
        assertThat(response).isPresent();
        assertThat(response.get()).extracting("Title").contains("Test save");

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
            .field("Title", "Test save")
            .endObject();

        Document document = Document.parse(builder.string());
        repository.save(document);

        Optional<Document> response = repository.findByIdentifierAndTenant("FakeIdentifier", tenant);
        assertThat(response).isPresent();
        assertThat(response.get()).extracting("Title").contains("Test save");
    }

    @Test
    public void testFindByIdentifierFoundOK() throws IOException, DatabaseException {
        String id = GUIDFactory.newGUID().toString();
        Integer tenant = 0;
        XContentBuilder builder = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, id)
            .field("Identifier", "FakeIdentifier")
            .field("Title", "Test save")
            .endObject();

        Document document = Document.parse(builder.string());
        repository.save(document);

        Optional<Document> response = repository.findByIdentifier("FakeIdentifier");
        assertThat(response).isPresent();
        assertThat(response.get()).extracting("Title").contains("Test save");
    }

    @Test
    public void testFindByIdentifierAndTenantFoundEmpty() throws DatabaseException {
        Integer tenant = 0;
        Optional<Document> response = repository.findByIdentifierAndTenant("FakeIdentifier", tenant);
        assertThat(response).isEmpty();
    }

    @Test
    public void testFindByIdentifierFoundEmpty() throws DatabaseException {
        Integer tenant = 0;
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

        Document document = Document.parse(builder.string());
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
}
