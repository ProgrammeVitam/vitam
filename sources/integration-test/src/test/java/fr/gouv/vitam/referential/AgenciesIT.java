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
package fr.gouv.vitam.referential;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.Agencies;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the reconstruction services. <br/>
 */
public class AgenciesIT extends VitamRuleRunner {

    private static final String AGENCY_PATH_1 = "referential/agencies_1.csv";
    private static final String AGENCY_PATH_2 = "referential/agencies_2.csv";
    private static final int TENANT_0 = 0;

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        AgenciesIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            StorageMain.class,
            DefaultOfferMain.class,
            AdminManagementMain.class
        )
    );

    @BeforeClass
    public static void beforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
    }

    @AfterClass
    public static void afterClass() {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setup() {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_0));
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
    }

    @After
    public void tearDown() {
        runAfter();
    }

    @Test
    @RunWithCustomExecutor
    public void should_import_agencies() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_0));

        OffsetRepository offsetRepository;

        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(
            mongoRule.getMongoClient(),
            mongoRule.getMongoDatabase().getName()
        );
        offsetRepository = new OffsetRepository(mongoDbAccess);

        offsetRepository.createOrUpdateOffset(
            TENANT_0,
            VitamConfiguration.getDefaultStrategy(),
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
            0L
        );

        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            client.importAgenciesFile(PropertiesUtils.getResourceAsStream(AGENCY_PATH_1), "agencies_1.csv");
        }

        VitamThreadUtils.getVitamSession().setTenantId(TENANT_0);
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(TENANT_0));

        // When
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            client.importAgenciesFile(PropertiesUtils.getResourceAsStream(AGENCY_PATH_2), "agencies_2.csv");
        }

        // Then
        List<AgenciesModel> agencies = new ArrayList<>();
        FunctionalAdminCollections.AGENCIES.<Agencies>getVitamCollection()
            .getCollection()
            .find()
            .map(d -> new AgenciesModel(d.getIdentifier(), d.getName(), d.getDescription(), d.getTenantId()))
            .forEach((Consumer<AgenciesModel>) agencies::add);

        assertThat(agencies)
            .extracting(AgenciesModel::getIdentifier)
            .containsOnly("AGG-00001", "AGG-00002", "AGG-00003", "AGG-00004", "AGG-00005", "AGG-00006");
        assertThat(agencies)
            .extracting(AgenciesModel::getName)
            .containsOnly("agency 1", "agency 2", "agency 3", "agency 4", "agency 5", "agency 6");
        assertThat(agencies)
            .extracting(AgenciesModel::getDescription)
            .containsOnly("BLOU---1", "BLOU---2", "BLOU---3", "BLOU---4", "BLOU---5", "BLOU---6");
        assertThat(agencies)
            .extracting(AgenciesModel::getTenant)
            .containsOnly(TENANT_0, TENANT_0, TENANT_0, TENANT_0, TENANT_0, TENANT_0);
    }
}
