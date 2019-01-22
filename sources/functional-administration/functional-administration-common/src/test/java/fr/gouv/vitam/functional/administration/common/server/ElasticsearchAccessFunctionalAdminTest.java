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

import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.VITAM_SEQUENCE;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.stream.Stream;

import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.AliasOrIndex;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * ElasticsearchAccessFunctionalAdminTest
 */
public class ElasticsearchAccessFunctionalAdminTest {

    private static TransportClient client;

    private static ElasticsearchAccessFunctionalAdmin elasticsearchAccessFunctionalAdmin;

    private int numberOfReplica = 2;
    private int numberOfIndices = Math.toIntExact(
        Stream.of(FunctionalAdminCollections.values())
            .filter(f -> f != VITAM_SEQUENCE)
            .count())
        * numberOfReplica;

    @ClassRule
    public static MongoRule mongoRule =
            new MongoRule(getMongoClientOptions(), "vitam-test");

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @BeforeClass
    public static void setUp() throws Exception {
        Settings settings = ElasticsearchAccess.getSettings(ElasticsearchRule.VITAM_CLUSTER);
        client = new PreBuiltTransportClient(settings);
        client.addTransportAddress(new TransportAddress(InetAddress.getByName("localhost"), ElasticsearchRule.TCP_PORT));
        List<ElasticsearchNode> nodes = new ArrayList<>();
        nodes.add(new ElasticsearchNode("localhost", ElasticsearchRule.TCP_PORT));
        elasticsearchAccessFunctionalAdmin = new ElasticsearchAccessFunctionalAdmin(ElasticsearchRule.VITAM_CLUSTER, nodes);

        FunctionalAdminCollections.beforeTestClass(mongoRule.getMongoDatabase(), PREFIX,
                new ElasticsearchAccessFunctionalAdmin(ElasticsearchRule.VITAM_CLUSTER,
                        Lists.newArrayList(new ElasticsearchNode("localhost", ElasticsearchRule.TCP_PORT))));
    }

    @After
    public void tearDown() throws IOException, VitamException {
        FunctionalAdminCollections.afterTestClass(new ElasticsearchAccessFunctionalAdmin(ElasticsearchRule.VITAM_CLUSTER,
                Lists.newArrayList(new ElasticsearchNode("localhost", ElasticsearchRule.TCP_PORT))), true);
    }

    @Test
    public void should_create_index_and_alias_when_indexing_FunctionalAdminCollection() {
        // Given, When
        createAllFunctionalAdminIndex();
        // Then
        final SortedMap<String, AliasOrIndex> aliasAndIndexLookup =
            client.admin().cluster().prepareState().execute().actionGet().getState().getMetaData()
                .getAliasAndIndexLookup();
        assertThat(aliasAndIndexLookup).hasSize(numberOfIndices);
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
        assertThat(aliasAndIndexLookup).hasSize(numberOfIndices);
    }


    private void createAllFunctionalAdminIndex() {
        for (FunctionalAdminCollections functionalAdminCollections : FunctionalAdminCollections.values()) {
            // Given
            String index = functionalAdminCollections.getName().toLowerCase();
            // When
            // Careful Not mapping for VITAM_SEQUENCE
            if (!(functionalAdminCollections.equals(VITAM_SEQUENCE))) {
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
            // When
            // Careful Not mapping for VITAM_SEQUENCE
            if (!(functionalAdminCollections.equals(VITAM_SEQUENCE))) {
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
