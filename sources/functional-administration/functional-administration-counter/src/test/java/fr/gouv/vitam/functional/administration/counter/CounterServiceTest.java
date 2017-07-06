/**
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
 */
package fr.gouv.vitam.functional.administration.counter;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;

import static org.assertj.core.api.Assertions.assertThat;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


public class CounterServiceTest {


    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor());

    private static final Integer TENANT_ID = 1;

    static JunitHelper junitHelper;
    static final String COLLECTION_NAME = "AccessContract";
    static final String DATABASE_HOST = "localhost";
    static final String DATABASE_NAME = "vitam-test";
    static MongodExecutable mongodExecutable;
    static MongodProcess mongod;
    static MongoClient client;
    private static MongoDbAccessAdminImpl dbImpl;
    static int mongoPort;
    static VitamCounterService vitamCounterService;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        junitHelper = JunitHelper.getInstance();
        mongoPort = junitHelper.findAvailablePort();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(mongoPort, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        client = new MongoClient(new ServerAddress(DATABASE_HOST, mongoPort));
        List tenants  = new ArrayList<>();
        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, mongoPort));
        dbImpl = MongoDbAccessAdminFactory.create(new DbConfigurationImpl(nodes, DATABASE_NAME));
        tenants.add(new Integer(TENANT_ID));
        vitamCounterService = new VitamCounterService(dbImpl, tenants);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(mongoPort);
        client.close();

    }
    @After
    public void afterTest() {
        final MongoCollection<Document> collection = client.getDatabase(DATABASE_NAME).getCollection(COLLECTION_NAME);
        collection.deleteMany(new Document());
    }

    @Test
    @RunWithCustomExecutor
    public void testSequences() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        String ic = vitamCounterService.getNextSequenceAsString(TENANT_ID, SequenceType.INGEST_CONTRACT_SEQUENCE.getName());
        String ac = vitamCounterService.getNextSequenceAsString(TENANT_ID, SequenceType.ACCESS_CONTRACT_SEQUENCE.getName());
        String pr = vitamCounterService.getNextSequenceAsString(TENANT_ID, SequenceType.PROFILE_SEQUENCE.getName());
        assertThat(ic).isEqualTo("IC-000001");
        assertThat(ac).isEqualTo("AC-000001");
        assertThat(pr).isEqualTo("PR-000001");


        ic = vitamCounterService.getNextSequenceAsString(TENANT_ID, SequenceType.INGEST_CONTRACT_SEQUENCE.getName());
        ac = vitamCounterService.getNextSequenceAsString(TENANT_ID,  SequenceType.ACCESS_CONTRACT_SEQUENCE.getName());
        ic = vitamCounterService.getNextSequenceAsString(TENANT_ID, SequenceType.INGEST_CONTRACT_SEQUENCE.getName());
        pr = vitamCounterService.getNextSequenceAsString(TENANT_ID, SequenceType.PROFILE_SEQUENCE.getName());

        assertThat(ic).isEqualTo("IC-000003");
        assertThat(ac).isEqualTo("AC-000002");
        assertThat(pr).isEqualTo("PR-000002");
        assertThat(vitamCounterService.getSequence(TENANT_ID, SequenceType.PROFILE_SEQUENCE.getName())).isEqualTo(2);



    }
    @Test(expected = ReferentialException.class)
    @RunWithCustomExecutor
    public void testError() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        String ic = vitamCounterService.getNextSequenceAsString(TENANT_ID, "AB");

    }
}
