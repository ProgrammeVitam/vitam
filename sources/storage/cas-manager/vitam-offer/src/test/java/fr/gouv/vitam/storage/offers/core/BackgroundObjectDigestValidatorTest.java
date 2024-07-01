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
package fr.gouv.vitam.storage.offers.core;

import com.google.common.util.concurrent.Uninterruptibles;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorage;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResultEntry;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyZeroInteractions;

public class BackgroundObjectDigestValidatorTest {

    private static final String CONTAINER_NAME = "containerName";
    private static final DigestType DIGEST_TYPE = DigestType.SHA512;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private ContentAddressableStorage contentAddressableStorage;

    @Before
    public void before() {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID().getId());
    }

    @Test
    @RunWithCustomExecutor
    public void testNoObjectAdded() throws ContentAddressableStorageException {
        // Given
        BackgroundObjectDigestValidator instance = new BackgroundObjectDigestValidator(
            contentAddressableStorage,
            CONTAINER_NAME,
            DIGEST_TYPE
        );

        // When (no object added)
        instance.awaitTermination();

        // Then
        assertThat(instance.hasConflictsReported()).isFalse();
        assertThat(instance.hasTechnicalExceptionsReported()).isFalse();
        assertThat(instance.getWrittenObjects()).isEmpty();

        verifyZeroInteractions(contentAddressableStorage);
    }

    @Test
    @RunWithCustomExecutor
    public void testMultipleObjects() throws ContentAddressableStorageException {
        // Given
        BackgroundObjectDigestValidator instance = new BackgroundObjectDigestValidator(
            contentAddressableStorage,
            CONTAINER_NAME,
            DIGEST_TYPE
        );

        doNothing()
            .when(contentAddressableStorage)
            .checkObjectDigestAndStoreDigest(CONTAINER_NAME, "obj1", "digest1", DIGEST_TYPE, 1001L);
        doNothing()
            .when(contentAddressableStorage)
            .checkObjectDigestAndStoreDigest(CONTAINER_NAME, "obj2", "digest2", DIGEST_TYPE, 1001L);
        doReturn("digest3").when(contentAddressableStorage).getObjectDigest(CONTAINER_NAME, "obj3", DIGEST_TYPE, true);
        doReturn("digest4").when(contentAddressableStorage).getObjectDigest(CONTAINER_NAME, "obj4", DIGEST_TYPE, true);

        // When
        instance.addWrittenObjectToCheck("obj1", "digest1", 1001L);
        instance.addWrittenObjectToCheck("obj2", "digest2", 1002L);
        instance.addExistingWormObjectToCheck("obj3", "digest3", 1003L);
        instance.addExistingWormObjectToCheck("obj4", "digest4", 1004L);
        instance.awaitTermination();

        // Then
        assertThat(instance.hasConflictsReported()).isFalse();
        assertThat(instance.hasTechnicalExceptionsReported()).isFalse();
        assertThat(instance.getWrittenObjects())
            .usingFieldByFieldElementComparator()
            .containsExactly(
                new StorageBulkPutResultEntry("obj1", "digest1", 1001L),
                new StorageBulkPutResultEntry("obj2", "digest2", 1002L),
                new StorageBulkPutResultEntry("obj3", "digest3", 1003L),
                new StorageBulkPutResultEntry("obj4", "digest4", 1004L)
            );

        InOrder inOrder = inOrder(contentAddressableStorage);
        inOrder
            .verify(contentAddressableStorage)
            .checkObjectDigestAndStoreDigest(CONTAINER_NAME, "obj1", "digest1", DIGEST_TYPE, 1001L);
        inOrder
            .verify(contentAddressableStorage)
            .checkObjectDigestAndStoreDigest(CONTAINER_NAME, "obj2", "digest2", DIGEST_TYPE, 1002L);
        inOrder.verify(contentAddressableStorage).getObjectDigest(CONTAINER_NAME, "obj3", DIGEST_TYPE, true);
        inOrder.verify(contentAddressableStorage).getObjectDigest(CONTAINER_NAME, "obj4", DIGEST_TYPE, true);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @RunWithCustomExecutor
    public void testInvalidWrittenObjectDigest() throws ContentAddressableStorageException {
        // Given
        BackgroundObjectDigestValidator instance = new BackgroundObjectDigestValidator(
            contentAddressableStorage,
            CONTAINER_NAME,
            DIGEST_TYPE
        );

        doThrow(new ContentAddressableStorageException("error"))
            .when(contentAddressableStorage)
            .checkObjectDigestAndStoreDigest(CONTAINER_NAME, "obj1", "digest1", DIGEST_TYPE, 1001L);
        doNothing()
            .when(contentAddressableStorage)
            .checkObjectDigestAndStoreDigest(CONTAINER_NAME, "obj2", "digest2", DIGEST_TYPE, 1001L);
        doReturn("digest3").when(contentAddressableStorage).getObjectDigest(CONTAINER_NAME, "obj3", DIGEST_TYPE, true);
        doReturn("digest4").when(contentAddressableStorage).getObjectDigest(CONTAINER_NAME, "obj4", DIGEST_TYPE, true);

        // When
        instance.addWrittenObjectToCheck("obj1", "digest1", 1001L);
        instance.addWrittenObjectToCheck("obj2", "digest2", 1002L);
        instance.addExistingWormObjectToCheck("obj3", "digest3", 1003L);
        instance.addExistingWormObjectToCheck("obj4", "digest4", 1004L);
        instance.awaitTermination();

        // Then
        assertThat(instance.hasConflictsReported()).isFalse();
        assertThat(instance.hasTechnicalExceptionsReported()).isTrue();
        assertThat(instance.getWrittenObjects())
            .usingFieldByFieldElementComparator()
            .containsExactly(
                new StorageBulkPutResultEntry("obj1", "digest1", 1001L),
                new StorageBulkPutResultEntry("obj2", "digest2", 1002L),
                new StorageBulkPutResultEntry("obj3", "digest3", 1003L),
                new StorageBulkPutResultEntry("obj4", "digest4", 1004L)
            );

        InOrder inOrder = inOrder(contentAddressableStorage);
        inOrder
            .verify(contentAddressableStorage)
            .checkObjectDigestAndStoreDigest(CONTAINER_NAME, "obj1", "digest1", DIGEST_TYPE, 1001L);
        inOrder
            .verify(contentAddressableStorage)
            .checkObjectDigestAndStoreDigest(CONTAINER_NAME, "obj2", "digest2", DIGEST_TYPE, 1002L);
        inOrder.verify(contentAddressableStorage).getObjectDigest(CONTAINER_NAME, "obj3", DIGEST_TYPE, true);
        inOrder.verify(contentAddressableStorage).getObjectDigest(CONTAINER_NAME, "obj4", DIGEST_TYPE, true);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @RunWithCustomExecutor
    public void testInvalidObjectDigest() throws ContentAddressableStorageException {
        // Given
        BackgroundObjectDigestValidator instance = new BackgroundObjectDigestValidator(
            contentAddressableStorage,
            CONTAINER_NAME,
            DIGEST_TYPE
        );

        doAnswer(ags -> {
            Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
            return false;
        })
            .when(contentAddressableStorage)
            .checkObjectDigestAndStoreDigest(CONTAINER_NAME, "obj1", "digest1", DIGEST_TYPE, 1001L);
        doAnswer(ags -> {
            Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
            return false;
        })
            .when(contentAddressableStorage)
            .checkObjectDigestAndStoreDigest(CONTAINER_NAME, "obj2", "digest2", DIGEST_TYPE, 1001L);
        doAnswer(ags -> {
            Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
            return "BAD_DIGEST";
        })
            .when(contentAddressableStorage)
            .getObjectDigest(CONTAINER_NAME, "obj3", DIGEST_TYPE, true);
        doAnswer(ags -> {
            Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
            return "digest4";
        })
            .when(contentAddressableStorage)
            .getObjectDigest(CONTAINER_NAME, "obj4", DIGEST_TYPE, true);

        // When
        instance.addWrittenObjectToCheck("obj1", "digest1", 1001L);
        instance.addWrittenObjectToCheck("obj2", "digest2", 1002L);
        instance.addExistingWormObjectToCheck("obj3", "digest3", 1003L);
        instance.addExistingWormObjectToCheck("obj4", "digest4", 1004L);
        instance.awaitTermination();

        // Then
        assertThat(instance.hasConflictsReported()).isTrue();
        assertThat(instance.hasTechnicalExceptionsReported()).isFalse();
        assertThat(instance.getWrittenObjects())
            .usingFieldByFieldElementComparator()
            .containsExactly(
                new StorageBulkPutResultEntry("obj1", "digest1", 1001L),
                new StorageBulkPutResultEntry("obj2", "digest2", 1002L),
                new StorageBulkPutResultEntry("obj4", "digest4", 1004L)
            );
        InOrder inOrder = inOrder(contentAddressableStorage);
        inOrder
            .verify(contentAddressableStorage)
            .checkObjectDigestAndStoreDigest(CONTAINER_NAME, "obj1", "digest1", DIGEST_TYPE, 1001L);
        inOrder
            .verify(contentAddressableStorage)
            .checkObjectDigestAndStoreDigest(CONTAINER_NAME, "obj2", "digest2", DIGEST_TYPE, 1002L);
        inOrder.verify(contentAddressableStorage).getObjectDigest(CONTAINER_NAME, "obj3", DIGEST_TYPE, true);
        inOrder.verify(contentAddressableStorage).getObjectDigest(CONTAINER_NAME, "obj4", DIGEST_TYPE, true);
        inOrder.verifyNoMoreInteractions();
    }
}
