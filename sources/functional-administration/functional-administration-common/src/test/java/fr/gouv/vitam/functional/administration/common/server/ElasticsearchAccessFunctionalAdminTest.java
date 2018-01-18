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

import com.carrotsearch.hppc.cursors.ObjectCursor;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.junit.JunitHelper;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.AliasOrIndex;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ElasticsearchAccessFunctionalAdminTest
 */
public class ElasticsearchAccessFunctionalAdminTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private TransportClient client;

    private ElasticsearchAccessFunctionalAdmin elasticsearchAccessFunctionalAdmin;
    private static JunitHelper.ElasticsearchTestConfiguration config;

    @Before
    public void setUp() throws Exception {
        int tcpPort = JunitHelper.getInstance().findAvailablePort();
        int httPort = JunitHelper.getInstance().findAvailablePort();
        String clusterName = "elasticsearch-data";
        config = JunitHelper.startElasticsearchForTest(temporaryFolder, clusterName, tcpPort, httPort);
        Settings settings = ElasticsearchAccess.getSettings(clusterName);
        client = new PreBuiltTransportClient(settings);
        client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), tcpPort));
        List<ElasticsearchNode> nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode("localhost", tcpPort));
        elasticsearchAccessFunctionalAdmin = new ElasticsearchAccessFunctionalAdmin(clusterName, nodes);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (config != null) {
            JunitHelper.stopElasticsearchForTest(config);
        }
    }

    @Test
    public void should_create_index_and_alias_when_indexing_FunctionalAdminCollection() {
        // Given, When
        createAllFunctionalAdminIndex();
        // Then
        final SortedMap<String, AliasOrIndex> aliasAndIndexLookup =
            client.admin().cluster().prepareState().execute().actionGet().getState().getMetaData()
                .getAliasAndIndexLookup();
        assertThat(aliasAndIndexLookup.size()).isEqualTo(20);
    }

    @Test
    public void should_not_recreate_alias_when_indexing_FunctionalAdminCollection() {
        // Given
        createAllFunctionalAdminIndex();
        // When
        createAllFunctionalAdminIndex();
        // Then
        final SortedMap<String, AliasOrIndex> aliasAndIndexLookup =
            client.admin().cluster().prepareState().execute().actionGet().getState().getMetaData()
                .getAliasAndIndexLookup();
        //TODO: Refactor when switch alias is implement.
        assertThat(aliasAndIndexLookup.size()).isEqualTo(20);
    }


    private void createAllFunctionalAdminIndex() {
        for (FunctionalAdminCollections functionalAdminCollections : FunctionalAdminCollections.values()) {
            // Given
            String index = functionalAdminCollections.getName().toLowerCase();
            // When
            // Careful Not mapping for VITAM_SEQUENCE
            if (!(functionalAdminCollections.equals(FunctionalAdminCollections.VITAM_SEQUENCE))) {
                elasticsearchAccessFunctionalAdmin.addIndex(functionalAdminCollections);
                GetAliasesResponse actualIndex =
                    client.admin().indices().getAliases(new GetAliasesRequest().indices(index))
                        .actionGet();
                // Then
                for (ObjectCursor<String> stringObjectCursor : actualIndex.getAliases().keys()) {
                    assertThat(stringObjectCursor.value).contains(index);
                }
                assertThat(actualIndex.getAliases().size()).isEqualTo(1);
            }
        }
    }

    @Test
    public void should_delete_index_and_not_alias_when_delete_index()
        throws Exception {
        // Given
        createAllFunctionalAdminIndex();
        for (FunctionalAdminCollections functionalAdminCollections : FunctionalAdminCollections.values()) {

            String index = functionalAdminCollections.getName().toLowerCase();
            // When
            // Careful Not mapping for VITAM_SEQUENCE
            if (!(functionalAdminCollections.equals(FunctionalAdminCollections.VITAM_SEQUENCE))) {
                elasticsearchAccessFunctionalAdmin.deleteIndex(functionalAdminCollections);
            }
        }
        final SortedMap<String, AliasOrIndex> aliasAndIndexLookup =
            client.admin().cluster().prepareState().execute().actionGet().getState().getMetaData()
                .getAliasAndIndexLookup();
        // Then
        //TODO: Refactor when switch alias is implement.
        assertThat(aliasAndIndexLookup.size()).isEqualTo(0);
    }
}
