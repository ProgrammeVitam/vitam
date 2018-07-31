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
package fr.gouv.vitam.metadata.core.database.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.metadata.core.MetaDataImpl;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MongoDbAccessMetadataImplTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private static final String DEFAULT_MONGO =
        "AccessionRegisterDetail\n" + "AccessionRegisterSummary\n" + "Unit\n" + "ObjectGroup\n" +
            "Unit Document{{v=2, key=Document{{_id=1}}, name=_id_, ns=Vitam-DB.Unit}}\n" +
            "Unit Document{{v=2, key=Document{{_id=hashed}}, name=_id_hashed, ns=Vitam-DB.Unit}}\n" +
            "ObjectGroup Document{{v=2, key=Document{{_id=1}}, name=_id_, ns=Vitam-DB.ObjectGroup}}\n" +
            "ObjectGroup Document{{v=2, key=Document{{_id=hashed}}, name=_id_hashed, ns=Vitam-DB.ObjectGroup}}\n";

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @ClassRule
    public static MongoRule mongoRule =
        new MongoRule(MongoDbAccessMetadataImpl.getMongoClientOptions(), "Vitam-DB",
            MetadataCollections.UNIT.getName(),
            MetadataCollections.OBJECTGROUP.getName());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule =
        new ElasticsearchRule(org.assertj.core.util.Files.newTemporaryFolder(),
            MetadataCollections.UNIT.getName().toLowerCase() + "_0",
            MetadataCollections.UNIT.getName().toLowerCase() + "_1",
            MetadataCollections.OBJECTGROUP.getName().toLowerCase() + "_0",
            MetadataCollections.OBJECTGROUP.getName().toLowerCase() + "_1"
        );

    static final List<Integer> tenantList = Arrays.asList(0);
    private static ElasticsearchAccessMetadata esClient;


    static MongoDbAccessMetadataImpl mongoDbAccess;


    @BeforeClass
    public static void setupOne() throws IOException, VitamException {


        final List<ElasticsearchNode> nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode("localhost", elasticsearchRule.getTcpPort()));

        esClient = new ElasticsearchAccessMetadata(elasticsearchRule.getClusterName(), nodes);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongoRule.handleAfter();
        elasticsearchRule.handleAfter();
    }

    @Test
    public void givenMongoDbAccessConstructorWhenCreateWithRecreateThenAddDefaultCollections() {
        mongoDbAccess =
            new MongoDbAccessMetadataImpl(mongoRule.getMongoClient(), mongoRule.getMongoDatabase().getName(), true,
                esClient, tenantList);
        assertThat(mongoDbAccess.getInfo()).isEqualTo(DEFAULT_MONGO);
        assertThat(MetadataCollections.UNIT.getName()).isEqualTo("Unit");
        assertThat(MetadataCollections.OBJECTGROUP.getName()).isEqualTo("ObjectGroup");
        assertThat(MongoDbAccessMetadataImpl.getUnitSize()).isEqualTo(0);
        assertThat(MongoDbAccessMetadataImpl.getObjectGroupSize()).isEqualTo(0);
    }

    @Test
    public void givenMongoDbAccessConstructorWhenCreateWithoutRecreateThenAddNothing() {
        mongoDbAccess =
            new MongoDbAccessMetadataImpl(mongoRule.getMongoClient(), mongoRule.getMongoDatabase().getName(), false,
                esClient, tenantList);
        assertEquals(DEFAULT_MONGO, mongoDbAccess.getInfo());
    }

    @Test
    public void givenMongoDbAccessWhenFlushOnDisKThenDoNothing() {
        mongoDbAccess =
            new MongoDbAccessMetadataImpl(mongoRule.getMongoClient(), mongoRule.getMongoDatabase().getName(), false,
                esClient, tenantList);
        mongoDbAccess.flushOnDisk();
    }

    @Test
    public void should_aggregate_unit_per_operation_id_and_originating_agency() throws Exception {
        // Given
        final MongoCollection unit = MetadataCollections.UNIT.getCollection();

        final MetaDataImpl metaData = new MetaDataImpl(mongoDbAccess);

        final String operationId = "1234";
        unit.insertOne(
            new Document("_id", "1").append("_ops", Arrays.asList(operationId))
                .append("_opi", Arrays.asList(operationId))
                .append("_sp", "sp2")
                .append("_sps", Arrays.asList("sp1", "sp2")));
        unit.insertOne(
            new Document("_id", "2").append("_ops", Arrays.asList(operationId))
                .append("_opi", Arrays.asList(operationId))
                .append("_sp", "sp1")
                .append("_sps", Arrays.asList("sp1")));
        unit.insertOne(
            new Document("_id", "3").append("_ops", Arrays.asList("otherOperationId"))
                .append("_opi", Arrays.asList("otherOperationId"))
                .append("_sps", Arrays.asList("sp2")));
        // When
        final List<Document> documents = metaData.selectOwnAccessionRegisterOnUnitByOperationId(operationId);

        // Then
        assertThat(documents).containsExactlyInAnyOrder(new Document("_id", "sp1").append("count", 1),
            new Document("_id", "sp2").append("count", 1));

    }

    @Test
    @RunWithCustomExecutor
    public void should_aggregate_object_group_per_operation_id_and_originating_agency() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        // Given
        final MongoCollection objectGroup = MetadataCollections.OBJECTGROUP.getCollection();

        final MetaDataImpl metaData = new MetaDataImpl(mongoDbAccess);

        final String operationId = "aedqaaaaacgbcaacaar3kak4tr2o3wqaaaaq";
        objectGroup.insertOne(new ObjectGroup(JsonHandler.getFromInputStream(getClass().getResourceAsStream(
            "/object_sp1_1.json"))));
        objectGroup.insertOne(new ObjectGroup(JsonHandler.getFromInputStream(getClass().getResourceAsStream(
            "/object_sp1_sp2_2.json"))));
        objectGroup.insertOne(
            new ObjectGroup(JsonHandler.getFromInputStream(getClass().getResourceAsStream("/object_sp2.json"))));
        objectGroup.insertOne(new ObjectGroup(JsonHandler.getFromInputStream(getClass().getResourceAsStream(
            "/object_sp2_4.json"))));
        objectGroup.insertOne(new ObjectGroup(JsonHandler.getFromInputStream(getClass().getResourceAsStream(
            "/object_other_operation_id.json"))));
        // When
        final List<Document> documents = metaData.selectOwnAccessionRegisterOnObjectGroupByOperationId(operationId);

        // Then
        assertThat(documents).containsExactlyInAnyOrder(
            new Document("originatingAgency", "sp1")
                .append("qualifierVersionOpi", "aedqaaaaacgbcaacaar3kak4tr2o3wqaaaaq")
                .append("totalSize", 200).append("totalGOT", 1).append("totalObject", 3),
            new Document("originatingAgency", "sp2")
                .append("qualifierVersionOpi", "aedqaaaaacgbcaacaar3kak4tr2o3wqaaaaq")
                .append("totalSize", 380).append("totalGOT", 3).append("totalObject", 6));
    }

}
