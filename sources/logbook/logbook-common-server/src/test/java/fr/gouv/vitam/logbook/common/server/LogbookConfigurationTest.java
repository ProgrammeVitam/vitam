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
package fr.gouv.vitam.logbook.common.server;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.logbook.common.server.config.DedicatedTenantConfiguration;
import fr.gouv.vitam.logbook.common.server.config.DefaultCollectionConfiguration;
import fr.gouv.vitam.logbook.common.server.config.GroupedTenantConfiguration;
import fr.gouv.vitam.logbook.common.server.config.LogbookConfiguration;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class LogbookConfigurationTest {

    private static final String HOST = "host";
    private static final int PORT = 1234;
    private static final String DB_NAME = "dbNameTest";
    private static final String JETTY_CONF = "jettyConfig";
    private static final String P2_FILE = "tsa.p12";
    private static final String P2_PASSWORD = "1234";
    private static final String WORKSPACE_URL = "http://localhost:8082";
    private static final String PROCESSING_URL = "http://localhost:8087";

    private static final String CLUSTER_NAME = "cluster-vitam";
    private static final String HOST_NAME = "localhost";
    private static int TCP_PORT = 9300;

    @Test
    public void testSetterGetter() {
        final List<MongoDbNode> mongo_nodes = new ArrayList<>();
        mongo_nodes.add(new MongoDbNode(HOST, PORT));
        final LogbookConfiguration config1 = new LogbookConfiguration();
        config1.setMongoDbNodes(mongo_nodes);
        assertEquals(config1.getMongoDbNodes().get(0).getDbHost(), HOST);
        assertEquals(config1.getMongoDbNodes().get(0).getDbPort(), PORT);
        assertEquals(config1.setDbName(DB_NAME).getDbName(), DB_NAME);
        assertEquals(JETTY_CONF, config1.setJettyConfig(JETTY_CONF).getJettyConfig());
        assertEquals(CLUSTER_NAME, config1.setClusterName(CLUSTER_NAME).getClusterName());
        config1.setP12LogbookFile(P2_FILE);
        assertEquals(P2_FILE, config1.getP12LogbookFile());
        config1.setP12LogbookPassword(P2_PASSWORD);
        assertEquals(P2_PASSWORD, config1.getP12LogbookPassword());
        config1.setWorkspaceUrl(WORKSPACE_URL);
        assertEquals(WORKSPACE_URL, config1.getWorkspaceUrl());
        config1.setProcessingUrl(PROCESSING_URL);
        assertEquals(PROCESSING_URL, config1.getProcessingUrl());
        List<ElasticsearchNode> esNodes = Lists.newArrayList(
            new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())
        );
        assertEquals(1, config1.setElasticsearchNodes(esNodes).getElasticsearchNodes().size());

        final LogbookConfiguration config2 = new LogbookConfiguration(mongo_nodes, DB_NAME, CLUSTER_NAME, esNodes);
        assertEquals(config2.getMongoDbNodes().get(0).getDbHost(), HOST);
        assertEquals(config2.getMongoDbNodes().get(0).getDbPort(), PORT);
        assertEquals(config2.getDbName(), DB_NAME);
        assertEquals(config2.getClusterName(), CLUSTER_NAME);
        assertEquals(config2.getElasticsearchNodes().size(), 1);
    }

    @Test
    public void testElasticsearchIndexationConfigurationLoading() throws Exception {
        LogbookConfiguration config;
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream("./logbook_test_config.yml")) {
            config = PropertiesUtils.readYaml(yamlIS, LogbookConfiguration.class);
        }

        assertThat(config.getLogbookTenantIndexation()).isNotNull();

        DefaultCollectionConfiguration defaultCollectionConfiguration = config
            .getLogbookTenantIndexation()
            .getDefaultCollectionConfiguration();
        assertThat(defaultCollectionConfiguration).isNotNull();
        assertThat(defaultCollectionConfiguration.getLogbookoperation()).isNotNull();
        assertThat(defaultCollectionConfiguration.getLogbookoperation().getNumberOfShards()).isEqualTo(2);
        assertThat(defaultCollectionConfiguration.getLogbookoperation().getNumberOfReplicas()).isEqualTo(10);

        List<DedicatedTenantConfiguration> dedicatedTenantConfiguration = config
            .getLogbookTenantIndexation()
            .getDedicatedTenantConfiguration();
        assertThat(dedicatedTenantConfiguration).isNotNull();
        assertThat(dedicatedTenantConfiguration).hasSize(1);
        assertThat(dedicatedTenantConfiguration.get(0)).isNotNull();
        assertThat(dedicatedTenantConfiguration.get(0).getTenants()).isEqualTo("10-20");
        assertThat(dedicatedTenantConfiguration.get(0).getLogbookoperation()).isNotNull();
        assertThat(dedicatedTenantConfiguration.get(0).getLogbookoperation().getNumberOfShards()).isEqualTo(3);
        assertThat(dedicatedTenantConfiguration.get(0).getLogbookoperation().getNumberOfReplicas()).isEqualTo(11);

        List<GroupedTenantConfiguration> groupedTenantConfiguration = config
            .getLogbookTenantIndexation()
            .getGroupedTenantConfiguration();
        assertThat(groupedTenantConfiguration).isNotNull();
        assertThat(groupedTenantConfiguration).hasSize(1);
        assertThat(groupedTenantConfiguration.get(0)).isNotNull();
        assertThat(groupedTenantConfiguration.get(0).getName()).isEqualTo("grp1");
        assertThat(groupedTenantConfiguration.get(0).getTenants()).isEqualTo("21-22");
        assertThat(groupedTenantConfiguration.get(0).getLogbookoperation()).isNotNull();
        assertThat(groupedTenantConfiguration.get(0).getLogbookoperation().getNumberOfShards()).isEqualTo(4);
        assertThat(groupedTenantConfiguration.get(0).getLogbookoperation().getNumberOfReplicas()).isEqualTo(12);
    }
}
