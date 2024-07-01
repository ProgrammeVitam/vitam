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
package fr.gouv.vitam.logbook.rest;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.logbook.common.server.config.ElasticsearchLogbookIndexManager;
import fr.gouv.vitam.logbook.common.server.config.LogbookConfiguration;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollectionsTestUtils;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookElasticsearchAccess;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LogbookApplicationAuthenticationTest {

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder());

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private static final String LOGBOOK_CONF = "logbook-auth-test.conf";

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static File logbook;
    private static LogbookConfiguration realLogbook;

    private static String configurationFile;
    private static final ElasticsearchLogbookIndexManager indexManager =
        LogbookCollectionsTestUtils.createTestIndexManager(Arrays.asList(0, 1), Collections.emptyMap());

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        List<ElasticsearchNode> esNodes = Lists.newArrayList(
            new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())
        );

        LogbookCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            PREFIX,
            new LogbookElasticsearchAccess(ElasticsearchRule.VITAM_CLUSTER, esNodes, indexManager)
        );

        logbook = PropertiesUtils.findFile(LOGBOOK_CONF);
        realLogbook = PropertiesUtils.readYaml(logbook, LogbookConfiguration.class);
        realLogbook.getMongoDbNodes().get(0).setDbPort(mongoRule.getDataBasePort());
        realLogbook.getElasticsearchNodes().get(0).setHttpPort(ElasticsearchRule.PORT);

        File file = temporaryFolder.newFile();
        configurationFile = file.getAbsolutePath();
        PropertiesUtils.writeYaml(file, realLogbook);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        LogbookCollectionsTestUtils.afterTestClass(indexManager, true);
        VitamClientFactory.resetConnections();
    }

    @Test
    public void testApplicationLaunch() {
        new LogbookMain(configurationFile);
    }
}
