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
package fr.gouv.vitam.logbook.rest;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jhades.JHades;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.JunitHelper.ElasticsearchTestConfiguration;
import fr.gouv.vitam.common.server.VitamServerFactory;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbAccessFactory;


public class LogbookApplicationTest {
    private static final String SHOULD_NOT_RAIZED_AN_EXCEPTION = "Should not raized an exception";

    private static final String LOGBOOK_CONF = "logbook-test.conf";
    private static final String DATABASE_HOST = "localhost";
    private static final String DATABASE_NAME = "vitam-test";
    private static LogbookDbAccess mongoDbAccess;
    private static MongodExecutable mongodExecutable;
    private static MongodProcess mongod;
    private static int databasePort;
    private static int serverPort;
    private static int oldPort;

    // ES
    @ClassRule
    public static TemporaryFolder esTempFolder = new TemporaryFolder();
    private final static String ES_CLUSTER_NAME = "vitam-cluster";
    private final static String ES_HOST_NAME = "localhost";
    private static ElasticsearchTestConfiguration config = null;

    private static JunitHelper junitHelper;
    private static File logbook;
    private static LogbookConfiguration realLogbook;

    private static final int TENANT_ID = 0;
    private static final List<Integer> tenantList = Arrays.asList(0);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Identify overlapping in particular jsr311
        new JHades().overlappingJarsReport();

        junitHelper = JunitHelper.getInstance();
        databasePort = junitHelper.findAvailablePort();
        logbook = PropertiesUtils.findFile(LOGBOOK_CONF);
        realLogbook = PropertiesUtils.readYaml(logbook, LogbookConfiguration.class);
        realLogbook.getMongoDbNodes().get(0).setDbPort(databasePort);
        final MongodStarter starter = MongodStarter.getDefaultInstance();
        mongodExecutable = starter.prepare(new MongodConfigBuilder()
            .withLaunchArgument("--enableMajorityReadConcern")
            .version(Version.Main.PRODUCTION)
            .net(new Net(databasePort, Network.localhostIsIPv6()))
            .build());
        mongod = mongodExecutable.start();
        // ES
        try {
            config = JunitHelper.startElasticsearchForTest(esTempFolder, ES_CLUSTER_NAME);
        } catch (final VitamApplicationServerException e1) {
            assumeTrue(false);
        }
        realLogbook.getElasticsearchNodes().get(0).setTcpPort(config.getTcpPort());

        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode(DATABASE_HOST, databasePort));
        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode(ES_HOST_NAME, config.getTcpPort()));
        LogbookConfiguration logbookConfiguration =
            new LogbookConfiguration(nodes, DATABASE_NAME, ES_CLUSTER_NAME, esNodes);
        logbookConfiguration.setTenants(tenantList);
        mongoDbAccess = LogbookMongoDbAccessFactory.create(logbookConfiguration);
        serverPort = junitHelper.findAvailablePort();
        // TODO P1 verifier la compatibilité avec les tests parallèles sur jenkins

        oldPort = VitamServerFactory.getDefaultPort();
        VitamServerFactory.setDefaultPort(serverPort);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        mongoDbAccess.close();
        if (config != null) {
            JunitHelper.stopElasticsearchForTest(config);
        }
        mongod.stop();
        mongodExecutable.stop();
        junitHelper.releasePort(serverPort);
        junitHelper.releasePort(databasePort);
        VitamServerFactory.setDefaultPort(oldPort);
        JunitHelper.unsetJettyPortSystemProperty();
    }

    @Test
    public final void testFictiveLaunch() {
        try {
            new LogbookApplication(realLogbook);
        } catch (final IllegalStateException e) {
            fail(SHOULD_NOT_RAIZED_AN_EXCEPTION);
        }
    }


    @Test(expected = IllegalStateException.class)
    public final void shouldRaiseException() throws VitamException {
        new LogbookApplication((String) null);
    }

}
