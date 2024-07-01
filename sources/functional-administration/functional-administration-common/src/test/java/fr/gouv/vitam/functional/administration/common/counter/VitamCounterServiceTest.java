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
package fr.gouv.vitam.functional.administration.common.counter;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.VitamSequence;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class VitamCounterServiceTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final Integer TENANT_ID = 1;

    private static MongoDbAccessAdminImpl dbImpl;
    static VitamCounterService vitamCounterService;

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder(VitamSequence.class));

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        FunctionalAdminCollections.VITAM_SEQUENCE.getVitamCollection()
            .setName(
                PREFIX + FunctionalAdminCollections.VITAM_SEQUENCE.getVitamCollection().getClasz().getSimpleName()
            );
        List tenants = new ArrayList<>();
        final List<MongoDbNode> nodes = new ArrayList<>();
        nodes.add(new MongoDbNode("localhost", mongoRule.getDataBasePort()));
        dbImpl = MongoDbAccessAdminFactory.create(
            new DbConfigurationImpl(nodes, mongoRule.getMongoDatabase().getName()),
            Collections::emptyList,
            FunctionalAdminCollectionsTestUtils.createTestIndexManager()
        );
        tenants.add(new Integer(TENANT_ID));
        Map<Integer, List<String>> listEnableExternalIdentifiers = new HashMap<>();
        List<String> list_tenant0 = new ArrayList<>();
        List<String> list_tenant1 = new ArrayList<>();
        list_tenant0.add("INGEST_CONTRACT");
        list_tenant0.add("PROFILE");
        list_tenant0.add("CONTEXT");
        list_tenant1.add("ACCESS_CONTRACT");
        list_tenant1.add("PROFILE");
        list_tenant1.add("CONTEXT");
        listEnableExternalIdentifiers.put(0, list_tenant0);
        listEnableExternalIdentifiers.put(1, list_tenant1);
        VitamConfiguration.setTenants(tenants);
        VitamConfiguration.setAdminTenant(TENANT_ID);

        vitamCounterService = new VitamCounterService(dbImpl, tenants, listEnableExternalIdentifiers);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        FunctionalAdminCollections.VITAM_SEQUENCE.getCollection().drop();
        VitamClientFactory.resetConnections();
    }

    @Test
    @RunWithCustomExecutor
    public void testSequences() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        String ic = vitamCounterService.getNextSequenceAsString(TENANT_ID, SequenceType.INGEST_CONTRACT_SEQUENCE);
        String ac = vitamCounterService.getNextSequenceAsString(TENANT_ID, SequenceType.ACCESS_CONTRACT_SEQUENCE);
        String pr = vitamCounterService.getNextSequenceAsString(TENANT_ID, SequenceType.PROFILE_SEQUENCE);
        assertThat(ic).isEqualTo("IC-000001");
        assertThat(ac).isEqualTo("AC-000001");
        assertThat(pr).isEqualTo("PR-000001");

        ic = vitamCounterService.getNextSequenceAsString(TENANT_ID, SequenceType.INGEST_CONTRACT_SEQUENCE);
        ac = vitamCounterService.getNextSequenceAsString(TENANT_ID, SequenceType.ACCESS_CONTRACT_SEQUENCE);
        ic = vitamCounterService.getNextSequenceAsString(TENANT_ID, SequenceType.INGEST_CONTRACT_SEQUENCE);
        pr = vitamCounterService.getNextSequenceAsString(TENANT_ID, SequenceType.PROFILE_SEQUENCE);

        vitamCounterService.getNextBackupSequenceDocument(TENANT_ID, SequenceType.INGEST_CONTRACT_SEQUENCE);
        vitamCounterService.getNextBackupSequenceDocument(TENANT_ID, SequenceType.SECURITY_PROFILE_SEQUENCE);
        Integer backUpSequence = vitamCounterService
            .getNextBackupSequenceDocument(TENANT_ID, SequenceType.INGEST_CONTRACT_SEQUENCE)
            .getCounter();
        assertThat(ic).isEqualTo("IC-000003");
        assertThat(ac).isEqualTo("AC-000002");
        assertThat(pr).isEqualTo("PR-000002");
        assertThat(vitamCounterService.getSequence(TENANT_ID, SequenceType.PROFILE_SEQUENCE)).isEqualTo(2);

        assertThat(
            vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
                SequenceType.ACCESS_CONTRACT_SEQUENCE.getCollection(),
                1
            )
        ).isTrue();
        assertThat(
            vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
                SequenceType.ACCESS_CONTRACT_SEQUENCE.getCollection(),
                0
            )
        ).isFalse();
        assertThat(
            vitamCounterService.isSlaveFunctionnalCollectionOnTenant(SequenceType.RULES_SEQUENCE.getCollection(), 0)
        ).isFalse();
        assertThat(
            vitamCounterService.isSlaveFunctionnalCollectionOnTenant(SequenceType.AGENCIES_SEQUENCE.getCollection(), 0)
        ).isFalse();
        assertThat(
            vitamCounterService.isSlaveFunctionnalCollectionOnTenant(
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY,
                0
            )
        ).isFalse();

        assertThat(backUpSequence).isEqualTo(2);

        VitamSequence test = vitamCounterService.getSequenceDocument(TENANT_ID, SequenceType.PROFILE_SEQUENCE);
    }

    @Test(expected = IllegalArgumentException.class)
    @RunWithCustomExecutor
    public void testError() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        String ic = vitamCounterService.getNextSequenceAsString(TENANT_ID, SequenceType.valueOf("ABB"));
    }
}
