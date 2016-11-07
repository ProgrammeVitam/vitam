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
package fr.gouv.vitam.functional.administration.common.server;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server2.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.RegisterValueDetail;

public class MongoDbAccessAdminImplTest {

    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient mongoClient;
    static JunitHelper junitHelper;
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitamtest";
    static final String COLLECTION_NAME = "FileFormat";
    static final String COLLECTION_RULES = "FileRules";
    private static final String ACCESSION_REGISTER_DETAIL_COLLECTION = "AccessionRegisterDetail";
    private static final String AGENCY = "Agency";

    static int port;
    static MongoDbAccessAdminImpl mongoAccess;
    static FileFormat file;
    static FileRules fileRules;
    static AccessionRegisterDetail register;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(port, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        mongoAccess = MongoDbAccessAdminFactory.create(
            new DbConfigurationImpl(DATABASE_HOST, port, DATABASE_NAME));

        final List<String> testList = new ArrayList<>();
        testList.add("test1");

        file = new FileFormat()
            .setCreatedDate("now")
            .setExtension(testList)
            .setMimeType(testList)
            .setName("name")
            .setPriorityOverIdList(testList)
            .setPronomVersion("pronom version")
            .setPUID("puid")
            .setVersion("version");

        fileRules = new FileRules()
            .setRuleId("APK-485")
            .setRuleType("testList")
            .setRuleDescription("testList")
            .setRuleDuration("10")
            .setRuleMeasurement("Annee");

        RegisterValueDetail initialValue = new RegisterValueDetail().setTotal(1).setDeleted(0).setRemained(1);
        register = new AccessionRegisterDetail()
            .setObjectSize(initialValue)
            .setOriginatingAgency(AGENCY)
            .setId(AGENCY)
            .setSubmissionAgency(AGENCY)
            .setStartDate("startDate")
            .setEndDate("endDate")
            .setTotalObjectGroups(initialValue)
            .setTotalObjects(initialValue)
            .setTotalUnits(initialValue);

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(port);
    }

    @Test
    public void testImplementFunction() throws Exception {
        final JsonNode jsonNode = JsonHandler.getFromString(file.toJson());
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonNode);
        mongoAccess.insertDocuments(arrayNode, FunctionalAdminCollections.FORMATS);
        assertEquals("FileFormat", FunctionalAdminCollections.FORMATS.getName());
        final MongoClient client = new MongoClient(new ServerAddress(DATABASE_HOST, port));
        final MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);
        assertEquals(1, collection.count());
        final Select select = new Select();
        select.setQuery(eq("Name", "name"));
        final MongoCursor<FileFormat> fileList =
            (MongoCursor<FileFormat>) mongoAccess.select(select.getFinalSelect(), FunctionalAdminCollections.FORMATS);
        final FileFormat f1 = fileList.next();
        final String id = f1.getString("_id");
        final FileFormat f2 = (FileFormat) mongoAccess.getDocumentById(id, FunctionalAdminCollections.FORMATS);
        assertEquals(f2, f1);
        mongoAccess.deleteCollection(FunctionalAdminCollections.FORMATS);
        assertEquals(0, collection.count());
        fileList.close();
        client.close();
    }

    @Test
    public void testRulesFunction() throws Exception {
        final JsonNode jsonNode = JsonHandler.getFromString(fileRules.toJson());
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        arrayNode.add(jsonNode);
        mongoAccess.insertDocuments(arrayNode, FunctionalAdminCollections.RULES);
        assertEquals("FileRules", FunctionalAdminCollections.RULES.getName());
        final MongoClient client = new MongoClient(new ServerAddress(DATABASE_HOST, port));
        final MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(COLLECTION_RULES);
        assertEquals(1, collection.count());
        final Select select = new Select();
        select.setQuery(eq("RuleId", "APK-485"));
        final MongoCursor<FileRules> fileList =
            (MongoCursor<FileRules>) mongoAccess.select(select.getFinalSelect(), FunctionalAdminCollections.RULES);
        final FileRules f1 = fileList.next();
        assertEquals("APK-485", f1.getString("RuleId"));
        final String id = f1.getString("RuleId");
        final FileRules f2 = (FileRules) mongoAccess.getDocumentById(id, FunctionalAdminCollections.RULES);
        mongoAccess.deleteCollection(FunctionalAdminCollections.RULES);
        assertEquals(0, collection.count());
        fileList.close();
        client.close();
    }
    
    @Test
    public void testAccessionRegister() throws Exception {
        final JsonNode jsonNode = JsonHandler.toJsonNode(register);
        mongoAccess.insertDocument(jsonNode, FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL);
        assertEquals(ACCESSION_REGISTER_DETAIL_COLLECTION, FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName());
        final MongoClient client = new MongoClient(new ServerAddress(DATABASE_HOST, port));
        final MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(ACCESSION_REGISTER_DETAIL_COLLECTION);
        assertEquals(1, collection.count());
        Map<String, Object> updateMap = new HashMap<>();
        updateMap.put(AccessionRegisterSummary.TOTAL_OBJECTGROUPS, 1);
        mongoAccess.updateDocumentByMap(updateMap, jsonNode, FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL, UPDATEACTION.SET);
        mongoAccess.deleteCollection(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL);
        assertEquals(0, collection.count());
        client.close();
    }
}
