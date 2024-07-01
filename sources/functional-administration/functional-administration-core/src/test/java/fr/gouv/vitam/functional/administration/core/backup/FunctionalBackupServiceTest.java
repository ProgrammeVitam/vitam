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
package fr.gouv.vitam.functional.administration.core.backup;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.Agencies;
import fr.gouv.vitam.functional.administration.common.VitamSequence;
import fr.gouv.vitam.functional.administration.common.config.ElasticsearchFunctionalAdminIndexManager;
import fr.gouv.vitam.functional.administration.common.counter.SequenceType;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.BackupServiceException;
import fr.gouv.vitam.functional.administration.common.exception.FunctionalBackupServiceException;
import fr.gouv.vitam.functional.administration.common.server.ElasticsearchAccessFunctionalAdmin;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollectionsTestUtils;
import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.common.guid.GUIDFactory.newEventGUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@RunWithCustomExecutor
public class FunctionalBackupServiceTest {

    public static final String DOC1_TENANT0 =
        "{\"_id\":\"aeaaaaaaaadw44zlabowqalanjdt5laaaaaq\",\"_tenant\":0,\"Name\":\"A\",\"Identifier\":\"ID-008\"}";
    public static final String DOC2_TENANT1 =
        "{\"_id\":\"aeaaaaaaaadw44zlabowqalanjdt5maaaaaq\",\"_tenant\":1,\"Name\":\"B\",\"Identifier\":\"ID-123\"}";
    public static final String SEQUENCE_DOC =
        "{\"_id\":\"aeaaaaaaaadw44zlabowqalanjdt5naaaaaq\",\"Counter\":0,\"Name\":\"A\",\"_tenant\":0}";
    public static final String BACKUP_SEQUENCE_DOC =
        "{\"_id\":\"aeaaaaaaaadw44zlabowqalanjdt5oaaaaaq\",\"Counter\":10,\"Name\":\"BACKUP_A\",\"_tenant\":0}";

    private static final String PREFIX = GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(MongoDbAccess.getMongoClientSettingsBuilder(Agencies.class));

    @ClassRule
    public static ElasticsearchRule elasticsearchRule = new ElasticsearchRule();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private BackupService backupService;

    @Mock
    private VitamCounterService vitamCounterService;

    @Mock
    private BackupLogbookManager backupLogbookManager;

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @InjectMocks
    private FunctionalBackupService functionalBackupService;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ElasticsearchFunctionalAdminIndexManager indexManager =
            FunctionalAdminCollectionsTestUtils.createTestIndexManager();
        FunctionalAdminCollectionsTestUtils.beforeTestClass(
            mongoRule.getMongoDatabase(),
            PREFIX,
            new ElasticsearchAccessFunctionalAdmin(
                ElasticsearchRule.VITAM_CLUSTER,
                Lists.newArrayList(new ElasticsearchNode(ElasticsearchRule.getHost(), ElasticsearchRule.getPort())),
                indexManager
            ),
            Lists.newArrayList(FunctionalAdminCollections.AGENCIES)
        );
    }

    @Before
    public void setUp() throws Exception {
        FunctionalAdminCollections.AGENCIES.getCollection().insertOne(new Agencies(DOC1_TENANT0));
        FunctionalAdminCollections.AGENCIES.getCollection().insertOne(new Agencies(DOC2_TENANT1));

        VitamSequence vitamSequence = new VitamSequence(Document.parse(SEQUENCE_DOC));
        given(vitamCounterService.getSequenceDocument(any(), eq(SequenceType.AGENCIES_SEQUENCE))).willReturn(
            vitamSequence
        );

        VitamSequence vitamBackupSequence = new VitamSequence(Document.parse(BACKUP_SEQUENCE_DOC));
        given(vitamCounterService.getNextBackupSequenceDocument(any(), eq(SequenceType.AGENCIES_SEQUENCE))).willReturn(
            vitamBackupSequence
        );
    }

    @AfterClass
    public static void afterClass() {
        FunctionalAdminCollectionsTestUtils.afterTestClass(
            Lists.newArrayList(FunctionalAdminCollections.AGENCIES),
            true
        );
    }

    @After
    public void cleanUp() {
        FunctionalAdminCollectionsTestUtils.afterTest(Lists.newArrayList(FunctionalAdminCollections.AGENCIES));
        mongoRule.handleAfter();
        elasticsearchRule.handleAfter();
    }

    @Test
    public void should_save_collection_and_sequence() throws Exception {
        //Given
        final FunctionalAdminCollections agencies = FunctionalAdminCollections.AGENCIES;

        List<String> savedDocCapture = new ArrayList<>();
        doAnswer(invocation -> {
            savedDocCapture.add(IOUtils.toString(((InputStream) invocation.getArguments()[0]), StandardCharsets.UTF_8));
            return null;
        })
            .when(backupService)
            .backup(any(), any(), anyString());

        VitamThreadUtils.getVitamSession().setTenantId(0);

        // When
        GUID guid = newEventGUID(0);
        functionalBackupService.saveCollectionAndSequence(guid, "STP_TEST", agencies, guid.toString());
        //Then

        ArgumentCaptor<String> hashArgCaptor = ArgumentCaptor.forClass(String.class);
        verify(backupLogbookManager).logEventSuccess(
            eq(guid),
            eq("STP_TEST"),
            hashArgCaptor.capture(),
            eq("0_" + PREFIX + "Agencies_10.json"),
            any()
        );

        String expectedDump =
            "{\"collection\":[" +
            DOC1_TENANT0 +
            "],\"sequence\":" +
            SEQUENCE_DOC +
            ",\"backup_sequence\":" +
            BACKUP_SEQUENCE_DOC +
            "}";
        String expectedDigest = new Digest(VitamConfiguration.getDefaultDigestType()).update(expectedDump).digestHex();

        assertThat(savedDocCapture).hasSize(1);
        assertThat(savedDocCapture.get(0)).isEqualTo(expectedDump);
        assertThat(hashArgCaptor.getValue()).isEqualTo(expectedDigest);
    }

    @Test
    public void should_fail_when_saving_collection() throws Exception {
        //Given
        final FunctionalAdminCollections agencies = FunctionalAdminCollections.AGENCIES;
        GUID guid = newEventGUID(0);
        willThrow(new BackupServiceException("Error Message")).given(backupService).backup(any(), any(), any());

        VitamThreadUtils.getVitamSession().setTenantId(0);

        // When / then
        assertThatThrownBy(
            () -> functionalBackupService.saveCollectionAndSequence(guid, "STP_TEST", agencies, guid.toString())
        )
            .isInstanceOf(FunctionalBackupServiceException.class)
            .withFailMessage("Error Message");
        verify(backupLogbookManager).logError(guid, "STP_TEST", "Error Message");
    }
}
