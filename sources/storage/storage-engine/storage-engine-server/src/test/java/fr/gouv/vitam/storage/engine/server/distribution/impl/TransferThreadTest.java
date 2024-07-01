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
package fr.gouv.vitam.storage.engine.server.distribution.impl;

import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.StoragePutRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutResult;
import fr.gouv.vitam.storage.engine.common.exception.StorageInconsistentStateException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.referential.StorageOfferProvider;
import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TransferThreadTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    private static final String OFFER_ID = "OfferId";

    @Test
    @RunWithCustomExecutor
    public void testTransferThread() throws Exception {
        // Given
        String requestId = GUIDFactory.newGUID().getId();
        byte[] data = "test-date".getBytes();
        Digest globalDigest = new Digest(DigestType.SHA512);
        globalDigest.update(data);

        InputStream is = mock(InputStream.class);
        Driver driver = mock(Driver.class);
        StorageOffer storageOffer = mock(StorageOffer.class);
        Connection connection = mock(Connection.class);
        doReturn(OFFER_ID).when(storageOffer).getId();
        doReturn(connection).when(driver).connect(OFFER_ID);
        StoragePutResult result = new StoragePutResult(
            2,
            DataCategory.UNIT.getFolder(),
            "ob1",
            "ob1",
            globalDigest.digestHex(),
            data.length
        );
        doReturn(result).when(connection).putObject(any(StoragePutRequest.class));

        OfferReference offerReference = new OfferReference(OFFER_ID);
        StorageOfferProvider offerProvider = mock(StorageOfferProvider.class);
        doReturn(storageOffer).when(offerProvider).getStorageOffer(OFFER_ID);

        StoragePutRequest storagePutRequest = new StoragePutRequest(
            2,
            DataCategory.UNIT.getFolder(),
            "ob1",
            DigestType.SHA512.getName(),
            is
        );

        TransferThread transferThread = new TransferThread(
            2,
            requestId,
            driver,
            offerReference,
            storagePutRequest,
            globalDigest,
            data.length,
            offerProvider
        );

        // When
        ThreadResponseData call = transferThread.call();

        // Then
        assertThat(call.getStatus()).isEqualTo(Response.Status.CREATED);
        assertThat(call.getObjectGuid()).isEqualTo("ob1");
        assertThat(call.getResponse().getGuid()).isEqualTo("ob1");
        verify(connection).close();
        verify(is).close();
    }

    @Test
    @RunWithCustomExecutor
    public void testTransferThreadWhenDigestMismatchThenThrowStorageInconsistentStateException() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(0);
        String requestId = GUIDFactory.newGUID().getId();
        byte[] data = "test-date".getBytes();
        Digest globalDigest = new Digest(DigestType.SHA512);
        globalDigest.update(data);

        InputStream is = mock(InputStream.class);
        Driver driver = mock(Driver.class);
        StorageOffer storageOffer = mock(StorageOffer.class);
        Connection connection = mock(Connection.class);
        doReturn(OFFER_ID).when(storageOffer).getId();
        doReturn(connection).when(driver).connect(OFFER_ID);
        StoragePutResult result = new StoragePutResult(
            2,
            DataCategory.UNIT.getFolder(),
            "ob1",
            "ob1",
            "BAD_DIGEST",
            data.length
        );
        doReturn(result).when(connection).putObject(any(StoragePutRequest.class));

        OfferReference offerReference = new OfferReference(OFFER_ID);
        StorageOfferProvider offerProvider = mock(StorageOfferProvider.class);
        doReturn(storageOffer).when(offerProvider).getStorageOffer(OFFER_ID);

        StoragePutRequest storagePutRequest = new StoragePutRequest(
            2,
            DataCategory.UNIT.getFolder(),
            "ob1",
            DigestType.SHA512.getName(),
            is
        );

        TransferThread transferThread = new TransferThread(
            2,
            requestId,
            driver,
            offerReference,
            storagePutRequest,
            globalDigest,
            data.length,
            offerProvider
        );

        // When / Then
        assertThatThrownBy(transferThread::call).isInstanceOf(StorageInconsistentStateException.class);

        verify(connection).close();
        verify(is).close();
    }

    @Test
    @RunWithCustomExecutor
    public void testTransferThreadWithErrorDuringConnection() throws Exception {
        // Given
        String requestId = GUIDFactory.newGUID().getId();
        Digest globalDigest = mock(Digest.class);

        InputStream is = mock(InputStream.class);
        Driver driver = mock(Driver.class);
        StorageOffer storageOffer = mock(StorageOffer.class);
        doReturn(OFFER_ID).when(storageOffer).getId();
        Exception ex = mock(StorageDriverException.class);
        doThrow(ex).when(driver).connect("OfferId");

        OfferReference offerReference = new OfferReference(OFFER_ID);
        StorageOfferProvider offerProvider = mock(StorageOfferProvider.class);
        doReturn(storageOffer).when(offerProvider).getStorageOffer(OFFER_ID);

        StoragePutRequest storagePutRequest = new StoragePutRequest(
            2,
            DataCategory.UNIT.getFolder(),
            "ob1",
            DigestType.SHA512.getName(),
            is
        );

        TransferThread transferThread = new TransferThread(
            2,
            requestId,
            driver,
            offerReference,
            storagePutRequest,
            globalDigest,
            200L,
            offerProvider
        );

        // When / Then
        assertThatThrownBy(transferThread::call).isEqualTo(ex);

        verify(is).close();
    }

    @Test
    @RunWithCustomExecutor
    public void testTransferThreadWithErrorDuringTransfer() throws Exception {
        // Given
        String requestId = GUIDFactory.newGUID().getId();
        Digest globalDigest = mock(Digest.class);

        InputStream is = mock(InputStream.class);
        Driver driver = mock(Driver.class);
        StorageOffer storageOffer = mock(StorageOffer.class);
        Connection connection = mock(Connection.class);
        doReturn(OFFER_ID).when(storageOffer).getId();
        doReturn(connection).when(driver).connect(OFFER_ID);

        Exception ex = mock(StorageDriverException.class);
        doThrow(ex).when(connection).putObject(any(StoragePutRequest.class));

        OfferReference offerReference = new OfferReference(OFFER_ID);
        StorageOfferProvider offerProvider = mock(StorageOfferProvider.class);
        doReturn(storageOffer).when(offerProvider).getStorageOffer(OFFER_ID);

        StoragePutRequest storagePutRequest = new StoragePutRequest(
            2,
            DataCategory.UNIT.getFolder(),
            "ob1",
            DigestType.SHA512.getName(),
            is
        );

        TransferThread transferThread = new TransferThread(
            2,
            requestId,
            driver,
            offerReference,
            storagePutRequest,
            globalDigest,
            200L,
            offerProvider
        );

        // When / When
        assertThatThrownBy(transferThread::call).isEqualTo(ex);

        verify(connection).close();
        verify(is).close();
    }
}
