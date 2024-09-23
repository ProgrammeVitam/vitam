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

package fr.gouv.vitam.profile;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.rest.AccessExternalMain;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchIndexAlias;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.time.LogicalClockRule;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.external.rest.IngestExternalMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static org.assertj.core.api.Assertions.assertThat;

public class EndToEndProfileIT extends VitamRuleRunner {

    private static final Integer tenantId = 0;

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        EndToEndProfileIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            ProcessManagementMain.class,
            AccessInternalMain.class,
            IngestInternalMain.class,
            StorageMain.class,
            DefaultOfferMain.class,
            BatchReportMain.class,
            AccessExternalMain.class,
            IngestExternalMain.class
        )
    );

    @Rule
    public LogicalClockRule logicalClock = new LogicalClockRule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        final String configSiegfriedPath = PropertiesUtils.getResourcePath(
            "integration-ingest-internal/format-identifiers.conf"
        ).toString();
        FormatIdentifierFactory.getInstance().changeConfigurationFile(configSiegfriedPath);
        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        handleAfterClass();
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @After
    public void afterTest() {
        VitamThreadUtils.getVitamSession().setContractId("aName");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");

        ProcessDataAccessImpl.getInstance().clearWorkflow();
        runAfterMongo(
            Sets.newHashSet(
                MetadataCollections.UNIT.getName(),
                MetadataCollections.OBJECTGROUP.getName(),
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName(),
                FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName(),
                FunctionalAdminCollections.PROFILE.getName(),
                LogbookCollections.OPERATION.getName(),
                LogbookCollections.LIFECYCLE_UNIT.getName(),
                LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
                LogbookCollections.LIFECYCLE_OBJECTGROUP.getName(),
                LogbookCollections.LIFECYCLE_UNIT_IN_PROCESS.getName()
            )
        );

        runAfterEs(
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.UNIT.getName(), 1),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(MetadataCollections.OBJECTGROUP.getName(), 1),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(LogbookCollections.OPERATION.getName(), 1),
            ElasticsearchIndexAlias.ofCrossTenantCollection(
                FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getName()
            ),
            ElasticsearchIndexAlias.ofCrossTenantCollection(
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getName()
            ),
            ElasticsearchIndexAlias.ofMultiTenantCollection(FunctionalAdminCollections.PROFILE.getName(), 0),
            ElasticsearchIndexAlias.ofMultiTenantCollection(FunctionalAdminCollections.PROFILE.getName(), 1)
        );
    }

    @Before
    public void setUpBefore() {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(0));
    }

    @Test
    @RunWithCustomExecutor
    public void shouldSaveProfileWhenIdentifierHasSpaces() {
        final VitamContext context = new VitamContext(tenantId)
            .setApplicationSessionId("ApplicationSessionId")
            .setAccessContract("aName3");

        try (
            final AdminExternalClient client = AdminExternalClientFactory.getInstance().getClient();
            final InputStream noticesInputStream = PropertiesUtils.getResourceAsStream(
                "profile/profiles_with_spaces.json"
            );
            final InputStream profileInputStream = PropertiesUtils.getResourceAsStream("profile/profile.rng")
        ) {
            final RequestResponse<ObjectNode> createNoticePayload = (RequestResponse<ObjectNode>) client.createProfiles(
                context,
                noticesInputStream
            );
            assertThat(createNoticePayload.getStatus()).isIn(200, 201);

            final RequestResponseOK<ObjectNode> response = (RequestResponseOK<ObjectNode>) createNoticePayload;
            final List<ObjectNode> profileModels = response.getResults();
            assertThat(profileModels).isNotNull();
            assertThat(profileModels).isNotEmpty();

            final ObjectNode profileModel = profileModels.get(0);
            final String SPACE = " ";
            final String identifier = profileModel.get(ProfileModel.TAG_IDENTIFIER).asText();
            assertThat(identifier).contains(SPACE);

            final RequestResponse<ProfileModel> importProfilePayload = (RequestResponse<
                    ProfileModel
                >) client.createProfileFile(context, identifier, profileInputStream);
            assertThat(importProfilePayload.getStatus()).isIn(200, 201);
        } catch (AccessExternalClientException | InvalidParseOperationException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
