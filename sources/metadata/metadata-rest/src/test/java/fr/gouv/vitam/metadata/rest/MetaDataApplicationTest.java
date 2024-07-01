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
package fr.gouv.vitam.metadata.rest;

import com.google.common.collect.Lists;
import com.mongodb.client.MongoClient;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.metadata.core.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.core.mapping.MappingLoader;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.fail;

public class MetaDataApplicationTest {

    static final int tenantId = 0;
    static final List<Integer> tenantList = List.of(tenantId);

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private static MetaDataConfiguration config;

    private static final String JETTY_CONFIG = "jetty-config-test.xml";

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(Unit.class, ObjectGroup.class)
    );

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    private MongoClient mongoClient = mongoRule.getMongoClient();

    /**
     *
     */
    @Before
    public void setUp() {
        List<ElasticsearchNode> esNodes = Lists.newArrayList(
            new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())
        );

        final List<MongoDbNode> mongo_nodes = new ArrayList<>();
        mongo_nodes.add(new MongoDbNode("localhost", MongoRule.getDataBasePort()));
        config = new MetaDataConfiguration(
            mongo_nodes,
            MongoRule.VITAM_DB,
            ElasticsearchRule.VITAM_CLUSTER,
            esNodes,
            new MappingLoader(Collections.emptyList())
        );
        VitamConfiguration.setTenants(tenantList);
        config.setJettyConfig(JETTY_CONFIG);
        config.setUrlProcessing("http://processing.service.consul:8203/");
        config.setContextPath("/metadata");
    }

    @Test
    public final void testFictiveLaunch() throws IOException {
        try {
            File configurationFile = tempFolder.newFile();
            PropertiesUtils.writeYaml(configurationFile, config);
            new MetadataMain(configurationFile.getAbsolutePath());
        } catch (final IllegalStateException e) {
            fail("should not raized an exception");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public final void shouldRaiseException() throws VitamException {
        new MetadataMain((String) null);
    }
}
