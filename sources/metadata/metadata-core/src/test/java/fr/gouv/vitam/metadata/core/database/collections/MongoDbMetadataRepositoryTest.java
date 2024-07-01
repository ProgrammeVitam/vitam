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
package fr.gouv.vitam.metadata.core.database.collections;

import com.google.common.collect.Lists;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.VERSION;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataCollections.UNIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.groups.Tuple.tuple;

public class MongoDbMetadataRepositoryTest {

    public static final int TENANT_ID = 0;

    public static final String PREFIX = GUIDFactory.newGUID().getId();

    @Rule
    public MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(Unit.class),
        PREFIX + UNIT.getName()
    );

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private MongoDbMetadataRepository<Unit> unitMongoDbMetadataRepository;

    @Before
    public void setUp() throws Exception {
        unitMongoDbMetadataRepository = new MongoDbMetadataRepository<>(
            () -> mongoRule.getMongoCollection(PREFIX + UNIT.getName(), Unit.class)
        );
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
    public void should_ignore_duplicates_during_bulk_insert() throws Exception {
        // Given
        String id1 = GUIDFactory.newUnitGUID(TENANT_ID).toString();
        String id2 = GUIDFactory.newUnitGUID(TENANT_ID).toString();
        String id3 = GUIDFactory.newUnitGUID(TENANT_ID).toString();
        Unit unit1 = createUnit(id1);
        Unit unit2 = createUnit(id2);
        Unit unit3 = createUnit(id3);

        // When
        unitMongoDbMetadataRepository.insert(Lists.newArrayList(unit1, unit2));
        unitMongoDbMetadataRepository.insert(Lists.newArrayList(unit1, unit2, unit3));

        // Then
        MongoCollection<Document> mongoCollection = mongoRule.getMongoCollection(PREFIX + UNIT.getName());
        assertThat(mongoCollection.countDocuments()).isEqualTo(3);
        assertThat(mongoCollection.find())
            .extracting("_id", VitamDocument.TENANT_ID, VERSION)
            .containsExactly(tuple(id1, TENANT_ID, 0), tuple(id2, TENANT_ID, 0), tuple(id3, TENANT_ID, 0));
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
        assertThat(mongoCollection.find()).extracting("title").containsExactly("unit1", "unit2");
    }

    @Test
    @RunWithCustomExecutor
    public void should_skip_when_store_two_document_with_same_id() {
        // Given
        String id1 = GUIDFactory.newUnitGUID(TENANT_ID).toString();
        String id3 = GUIDFactory.newUnitGUID(TENANT_ID).toString();
        Unit unit1 = createUnit(id1);
        Unit unit2 = createUnit(id1);
        Unit unit3 = createUnit(id3);

        // When
        assertThatCode(
            () -> unitMongoDbMetadataRepository.insert(Lists.newArrayList(unit1, unit2, unit3))
        ).doesNotThrowAnyException();
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
