package fr.gouv.vitam.storage.engine.server.distribution.impl;

import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
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

    private static final String OFFER_ID = "OfferId";

    @Test
    public void testTransferThread() throws Exception {

        // Given
        byte[] data = "test-date".getBytes();
        Digest globalDigest = new Digest(DigestType.SHA512);
        globalDigest.update(data);


        InputStream is = mock(InputStream.class);
        Driver driver = mock(Driver.class);
        StorageOffer storageOffer = mock(StorageOffer.class);
        Connection connection = mock(Connection.class);
        doReturn(OFFER_ID).when(storageOffer).getId();
        doReturn(connection).when(driver).connect(OFFER_ID);
        StoragePutResult result =
            new StoragePutResult(2, DataCategory.UNIT.getFolder(), "ob1", "ob1", globalDigest.digestHex(), data.length);
        doReturn(result).when(connection).putObject(any(StoragePutRequest.class));

        OfferReference offerReference = new OfferReference(OFFER_ID);
        StorageOfferProvider offerProvider = mock(StorageOfferProvider.class);
        doReturn(storageOffer).when(offerProvider).getStorageOffer(OFFER_ID);

        StoragePutRequest storagePutRequest = new StoragePutRequest(
            2, DataCategory.UNIT.getFolder(), "ob1", DigestType.SHA512.getName(), is);

        TransferThread transferThread =
            new TransferThread(driver, offerReference, storagePutRequest, globalDigest, data.length, offerProvider);

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
    public void testTransferThreadWhenDigestMismatchThenThrowStorageInconsistentStateException() throws Exception {

        // Given
        byte[] data = "test-date".getBytes();
        Digest globalDigest = new Digest(DigestType.SHA512);
        globalDigest.update(data);


        InputStream is = mock(InputStream.class);
        Driver driver = mock(Driver.class);
        StorageOffer storageOffer = mock(StorageOffer.class);
        Connection connection = mock(Connection.class);
        doReturn(OFFER_ID).when(storageOffer).getId();
        doReturn(connection).when(driver).connect(OFFER_ID);
        StoragePutResult result =
            new StoragePutResult(2, DataCategory.UNIT.getFolder(), "ob1", "ob1", "BAD_DIGEST", data.length);
        doReturn(result).when(connection).putObject(any(StoragePutRequest.class));

        OfferReference offerReference = new OfferReference(OFFER_ID);
        StorageOfferProvider offerProvider = mock(StorageOfferProvider.class);
        doReturn(storageOffer).when(offerProvider).getStorageOffer(OFFER_ID);

        StoragePutRequest storagePutRequest = new StoragePutRequest(
            2, DataCategory.UNIT.getFolder(), "ob1", DigestType.SHA512.getName(), is);

        TransferThread transferThread =
            new TransferThread(driver, offerReference, storagePutRequest, globalDigest, data.length, offerProvider);

        // When / Then
        assertThatThrownBy(transferThread::call)
            .isInstanceOf(StorageInconsistentStateException.class);

        verify(connection).close();
        verify(is).close();
    }

    @Test
    public void testTransferThreadWithErrorDuringConnection() throws Exception {

        // Given
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
            2, DataCategory.UNIT.getFolder(), "ob1", DigestType.SHA512.getName(), is);

        TransferThread transferThread =
            new TransferThread(driver, offerReference, storagePutRequest, globalDigest, 200L, offerProvider);

        // When / Then
        assertThatThrownBy(transferThread::call)
            .isEqualTo(ex);

        verify(is).close();
    }

    @Test
    public void testTransferThreadWithErrorDuringTransfer() throws Exception {

        // Given
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
            2, DataCategory.UNIT.getFolder(), "ob1", DigestType.SHA512.getName(), is);

        TransferThread transferThread =
            new TransferThread(driver, offerReference, storagePutRequest, globalDigest, 200L, offerProvider);

        // When / When
        assertThatThrownBy(transferThread::call)
            .isEqualTo(ex);

        verify(connection).close();
        verify(is).close();
    }
}
