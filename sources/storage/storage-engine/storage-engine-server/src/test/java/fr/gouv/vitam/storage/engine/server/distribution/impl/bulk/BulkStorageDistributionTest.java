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
package fr.gouv.vitam.storage.engine.server.distribution.impl.bulk;

import com.google.common.collect.ImmutableMap;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import fr.gouv.vitam.storage.engine.server.storagelog.StorageLog;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookParameterName;
import fr.gouv.vitam.storage.engine.server.storagelog.parameters.StorageLogbookParameters;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWithCustomExecutor
public class BulkStorageDistributionTest {

    private static final String REQUESTER = "requester";
    private static final DigestType DIGEST_TYPE = DigestType.SHA512;

    private static final String STRATEGY = "default";
    private static final int ATTEMPT = 1;

    private static final int TENANT_ID = 2;
    private static final DataCategory DATA_CATEGORY = DataCategory.UNIT;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private StorageLog storageLogService;

    @Mock
    Map<String, Driver> driverMap;

    @Mock
    Map<String, StorageOffer> storageOfferMap;

    @Mock
    private BulkPutTransferManager bulkPutTransferManager;

    private BulkStorageDistribution instance;

    @Before
    public void before() {
        String requestId = GUIDFactory.newGUID().toString();
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(requestId);

        instance = new BulkStorageDistribution(3, storageLogService, DIGEST_TYPE, bulkPutTransferManager, 0, 30);
    }

    @Test
    public void bulkCreateFromWorkspaceWithRetriesSuccess() throws Exception {
        // Given;
        String workspaceContainer = "workspaceContainer";
        List<String> workspaceUris = Arrays.asList("uri1", "uri2", "uri3");
        List<String> objectNames = Arrays.asList("obj1", "obj2", "obj3");
        List<String> offerIds = Arrays.asList("offer1", "offer2");

        List<ObjectInfo> objectInfos = Arrays.asList(
            new ObjectInfo("obj1", "digest1", 1L),
            new ObjectInfo("obj2", "digest2", 2L),
            new ObjectInfo("obj3", "digest3", 3L)
        );

        ImmutableMap<String, OfferBulkPutStatus> statusByOfferIds = ImmutableMap.of(
            "offer1",
            OfferBulkPutStatus.OK,
            "offer2",
            OfferBulkPutStatus.OK
        );

        when(
            bulkPutTransferManager.bulkSendDataToOffers(
                workspaceContainer,
                STRATEGY,
                ATTEMPT,
                TENANT_ID,
                DATA_CATEGORY,
                offerIds,
                driverMap,
                storageOfferMap,
                workspaceUris,
                objectNames
            )
        ).thenReturn(new BulkPutResult(objectInfos, statusByOfferIds));

        // When
        Map<String, String> digests = instance.bulkCreateFromWorkspaceWithRetries(
            STRATEGY,
            TENANT_ID,
            offerIds,
            driverMap,
            storageOfferMap,
            DATA_CATEGORY,
            workspaceContainer,
            workspaceUris,
            objectNames,
            REQUESTER
        );

        // Then

        checkResult(objectInfos, digests, "offer1 attempt 1 : OK, offer2 attempt 1 : OK", "OK");
    }

    @Test
    public void bulkCreateFromWorkspaceWithRetriesKoThenSuccess() throws Exception {
        // Given;
        String workspaceContainer = "workspaceContainer";
        List<String> workspaceUris = Arrays.asList("uri1", "uri2", "uri3");
        List<String> objectNames = Arrays.asList("obj1", "obj2", "obj3");
        List<String> offerIds = Arrays.asList("offer1", "offer2");

        List<ObjectInfo> objectInfos = Arrays.asList(
            new ObjectInfo("obj1", "digest1", 1L),
            new ObjectInfo("obj2", "digest2", 2L),
            new ObjectInfo("obj3", "digest3", 3L)
        );

        ImmutableMap<String, OfferBulkPutStatus> statusByOfferIds1 = ImmutableMap.of(
            "offer1",
            OfferBulkPutStatus.OK,
            "offer2",
            OfferBulkPutStatus.KO
        );

        when(
            bulkPutTransferManager.bulkSendDataToOffers(
                workspaceContainer,
                STRATEGY,
                1,
                TENANT_ID,
                DATA_CATEGORY,
                offerIds,
                driverMap,
                storageOfferMap,
                workspaceUris,
                objectNames
            )
        ).thenReturn(new BulkPutResult(objectInfos, statusByOfferIds1));

        ImmutableMap<String, OfferBulkPutStatus> statusByOfferIds2 = ImmutableMap.of("offer2", OfferBulkPutStatus.OK);

        when(
            bulkPutTransferManager.bulkSendDataToOffers(
                workspaceContainer,
                STRATEGY,
                2,
                TENANT_ID,
                DATA_CATEGORY,
                Collections.singletonList("offer2"),
                driverMap,
                storageOfferMap,
                workspaceUris,
                objectNames
            )
        ).thenReturn(new BulkPutResult(objectInfos, statusByOfferIds2));

        // When
        Map<String, String> digests = instance.bulkCreateFromWorkspaceWithRetries(
            STRATEGY,
            TENANT_ID,
            offerIds,
            driverMap,
            storageOfferMap,
            DATA_CATEGORY,
            workspaceContainer,
            workspaceUris,
            objectNames,
            REQUESTER
        );

        // Then
        checkResult(objectInfos, digests, "offer1 attempt 1 : OK, offer2 attempt 1 : KO, offer2 attempt 2 : OK", "OK");
    }

    @Test
    public void bulkCreateFromWorkspaceWithRetriesKoThenKoThenKo() throws Exception {
        // Given;
        String workspaceContainer = "workspaceContainer";
        List<String> workspaceUris = Arrays.asList("uri1", "uri2", "uri3");
        List<String> objectNames = Arrays.asList("obj1", "obj2", "obj3");
        List<String> offerIds = Arrays.asList("offer1", "offer2");

        List<ObjectInfo> objectInfos = Arrays.asList(
            new ObjectInfo("obj1", "digest1", 1L),
            new ObjectInfo("obj2", "digest2", 2L),
            new ObjectInfo("obj3", "digest3", 3L)
        );

        ImmutableMap<String, OfferBulkPutStatus> statusByOfferIds1 = ImmutableMap.of(
            "offer1",
            OfferBulkPutStatus.OK,
            "offer2",
            OfferBulkPutStatus.KO
        );

        when(
            bulkPutTransferManager.bulkSendDataToOffers(
                workspaceContainer,
                STRATEGY,
                1,
                TENANT_ID,
                DATA_CATEGORY,
                offerIds,
                driverMap,
                storageOfferMap,
                workspaceUris,
                objectNames
            )
        ).thenReturn(new BulkPutResult(objectInfos, statusByOfferIds1));

        ImmutableMap<String, OfferBulkPutStatus> statusByOfferIds2And3 = ImmutableMap.of(
            "offer2",
            OfferBulkPutStatus.KO
        );

        when(
            bulkPutTransferManager.bulkSendDataToOffers(
                workspaceContainer,
                STRATEGY,
                2,
                TENANT_ID,
                DATA_CATEGORY,
                Collections.singletonList("offer2"),
                driverMap,
                storageOfferMap,
                workspaceUris,
                objectNames
            )
        ).thenReturn(new BulkPutResult(objectInfos, statusByOfferIds2And3));

        when(
            bulkPutTransferManager.bulkSendDataToOffers(
                workspaceContainer,
                STRATEGY,
                3,
                TENANT_ID,
                DATA_CATEGORY,
                Collections.singletonList("offer2"),
                driverMap,
                storageOfferMap,
                workspaceUris,
                objectNames
            )
        ).thenReturn(new BulkPutResult(objectInfos, statusByOfferIds2And3));

        // When / Then

        assertThatThrownBy(
            () ->
                instance.bulkCreateFromWorkspaceWithRetries(
                    STRATEGY,
                    TENANT_ID,
                    offerIds,
                    driverMap,
                    storageOfferMap,
                    DATA_CATEGORY,
                    workspaceContainer,
                    workspaceUris,
                    objectNames,
                    REQUESTER
                )
        ).isInstanceOf(StorageException.class);

        // Then
        checkResult(
            objectInfos,
            null,
            "offer1 attempt 1 : OK, offer2 attempt 1 : KO, offer2 attempt 2 : KO, offer2 attempt 3 : KO",
            "KO"
        );
    }

    @Test
    public void bulkCreateFromWorkspaceWithRetriesBlockerFailure() throws Exception {
        // Given;
        String workspaceContainer = "workspaceContainer";
        List<String> workspaceUris = Arrays.asList("uri1", "uri2", "uri3");
        List<String> objectNames = Arrays.asList("obj1", "obj2", "obj3");
        List<String> offerIds = Arrays.asList("offer1", "offer2");

        List<ObjectInfo> objectInfos = Arrays.asList(
            new ObjectInfo("obj1", "digest1", 1L),
            new ObjectInfo("obj2", "digest2", 2L),
            new ObjectInfo("obj3", "digest3", 3L)
        );

        ImmutableMap<String, OfferBulkPutStatus> statusByOfferIds1 = ImmutableMap.of(
            "offer1",
            OfferBulkPutStatus.OK,
            "offer2",
            OfferBulkPutStatus.BLOCKER
        );

        when(
            bulkPutTransferManager.bulkSendDataToOffers(
                workspaceContainer,
                STRATEGY,
                ATTEMPT,
                TENANT_ID,
                DATA_CATEGORY,
                offerIds,
                driverMap,
                storageOfferMap,
                workspaceUris,
                objectNames
            )
        ).thenReturn(new BulkPutResult(objectInfos, statusByOfferIds1));

        // When / Then
        assertThatThrownBy(
            () ->
                instance.bulkCreateFromWorkspaceWithRetries(
                    STRATEGY,
                    TENANT_ID,
                    offerIds,
                    driverMap,
                    storageOfferMap,
                    DATA_CATEGORY,
                    workspaceContainer,
                    workspaceUris,
                    objectNames,
                    REQUESTER
                )
        ).isInstanceOf(StorageException.class);

        // Then
        checkResult(objectInfos, null, "offer1 attempt 1 : OK, offer2 attempt 1 : BLOCKER", "KO");
    }

    private void checkResult(
        List<ObjectInfo> objectInfos,
        Map<String, String> digests,
        String logEvents,
        String outcome
    ) throws IOException {
        if (digests != null) {
            assertThat(digests).isEqualTo(
                objectInfos.stream().collect(toMap(ObjectInfo::getObjectId, ObjectInfo::getDigest))
            );
        }

        ArgumentCaptor<StorageLogbookParameters> storageLogbookParameterCapture = ArgumentCaptor.forClass(
            StorageLogbookParameters.class
        );
        verify(storageLogService, times(3)).appendWriteLog(eq(TENANT_ID), storageLogbookParameterCapture.capture());
        List<StorageLogbookParameters> storageLogbookParameters = storageLogbookParameterCapture.getAllValues();
        assertThat(storageLogbookParameters).hasSize(objectInfos.size());

        for (int i = 0; i < objectInfos.size(); i++) {
            ObjectInfo objectInfo = objectInfos.get(i);
            checkResult(
                storageLogbookParameters.get(i).getMapParameters(),
                objectInfo.getObjectId(),
                objectInfo.getDigest(),
                objectInfo.getSize(),
                logEvents,
                outcome
            );
        }
    }

    private void checkResult(
        Map<StorageLogbookParameterName, String> params,
        String id,
        String digest,
        long size,
        String logEvents,
        String outcome
    ) {
        assertThat(params.get(StorageLogbookParameterName.objectIdentifier)).isEqualTo(id);
        assertThat(params.get(StorageLogbookParameterName.size)).isEqualTo("" + size);
        assertThat(params.get(StorageLogbookParameterName.outcome)).isEqualTo(outcome);
        assertThat(params.get(StorageLogbookParameterName.dataCategory)).isEqualTo(DATA_CATEGORY.getFolder());
        assertThat(params.get(StorageLogbookParameterName.digest)).isEqualTo(digest);
        assertThat(params.get(StorageLogbookParameterName.digestAlgorithm)).isEqualTo(DIGEST_TYPE.getName());
        assertThat(params.get(StorageLogbookParameterName.agentIdentifierRequester)).isEqualTo(REQUESTER);
        assertThat(params.get(StorageLogbookParameterName.xRequestId)).isEqualTo(
            VitamThreadUtils.getVitamSession().getRequestId()
        );
        assertThat(params.get(StorageLogbookParameterName.eventType)).isEqualTo("CREATE");
        assertThat(params.get(StorageLogbookParameterName.agentIdentifiers)).isEqualTo(logEvents);
    }
}
