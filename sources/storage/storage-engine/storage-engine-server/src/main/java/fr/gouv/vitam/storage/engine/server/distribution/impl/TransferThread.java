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

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.StoragePutRequest;
import fr.gouv.vitam.storage.driver.model.StoragePutResult;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageInconsistentStateException;
import fr.gouv.vitam.storage.engine.common.referential.StorageOfferProvider;
import fr.gouv.vitam.storage.engine.common.referential.StorageOfferProviderFactory;
import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.core.Response;
import java.util.concurrent.Callable;

/**
 * Thread Future used to send stream to one offer
 */
public class TransferThread implements Callable<ThreadResponseData> {

    public static final String TIMEOUT_TEST = "timeoutTest";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransferThread.class);

    private static final StorageOfferProvider OFFER_PROVIDER = StorageOfferProviderFactory.getDefaultProvider();

    private final int tenantId;
    private final String requestId;
    private final StorageOfferProvider offerProvider;
    private final Driver driver;
    private final OfferReference offerReference;
    private final StoragePutRequest request;
    private final Digest globalDigest;
    private final Long size;

    private static boolean IS_JUNIT_MODE = false;

    /**
     * Default constructor
     *
     * @param tenantId
     * @param requestId
     * @param driver thre diver
     * @param offerReference the offer reference to put object
     * @param request the request to put object
     * @param globalDigest the globalDigest associated with the stream
     */
    public TransferThread(
        int tenantId,
        String requestId,
        Driver driver,
        OfferReference offerReference,
        StoragePutRequest request,
        Digest globalDigest,
        long size
    ) {
        this(tenantId, requestId, driver, offerReference, request, globalDigest, size, OFFER_PROVIDER);
    }

    @VisibleForTesting
    public TransferThread(
        int tenantId,
        String requestId,
        Driver driver,
        OfferReference offerReference,
        StoragePutRequest request,
        Digest globalDigest,
        long size,
        StorageOfferProvider offerProvider
    ) {
        this.tenantId = tenantId;
        this.requestId = requestId;
        ParametersChecker.checkParameter("Driver cannot be null", driver);
        ParametersChecker.checkParameter("OfferReference cannot be null", offerReference);
        ParametersChecker.checkParameter("PutObjectRequest cannot be null", request);
        ParametersChecker.checkParameter("GlobalDigest cannot be null", globalDigest);
        this.driver = driver;
        this.offerReference = offerReference;
        this.request = request;
        this.globalDigest = globalDigest;
        this.size = size;
        this.offerProvider = offerProvider;
    }

    /**
     * Allow to check timeout in Junit
     *
     * @param mode if true allow to implement timeout using GUID to "timeoutTest"
     */
    public static void setJunitMode(boolean mode) {
        IS_JUNIT_MODE = mode;
    }

    // TODO: Manage interruption (if possible)
    @Override
    public ThreadResponseData call() throws StorageException, StorageDriverException, InterruptedException {
        String initialThreadId = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName(initialThreadId + "-TransferThread-" + offerReference.getId());
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(requestId);
            return storeInOffer();
        } catch (Exception ex) {
            LOGGER.error("An error occurred during transfer to offer " + this.offerReference.getId(), ex);
            throw ex;
        } finally {
            IOUtils.closeQuietly(this.request.getDataStream());
            Thread.currentThread().setName(initialThreadId);
        }
    }

    private ThreadResponseData storeInOffer() throws StorageException, StorageDriverException, InterruptedException {
        if (IS_JUNIT_MODE && request.getGuid().equals(TIMEOUT_TEST) && request.getTenantId() == 0) {
            LOGGER.info("Sleep for Junit test");
            Thread.sleep(100);
            return null;
        }
        LOGGER.debug(request.toString());
        final StorageOffer offer = this.offerProvider.getStorageOffer(offerReference.getId());
        ThreadResponseData response;
        try (Connection connection = driver.connect(offer.getId())) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            // ugly way to get digest from stream
            // TODO: How to do the cleaner ?
            // TODO: remove this, check is offer size (#1851) !
            StoragePutRequest putObjectRequest = new StoragePutRequest(
                request.getTenantId(),
                request.getType(),
                request.getGuid(),
                request.getDigestAlgorithm(),
                request.getDataStream()
            );
            putObjectRequest.setSize(this.size);
            StoragePutResult putObjectResult = connection.putObject(putObjectRequest);
            LOGGER.debug(putObjectRequest.toString());
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            // Check digest against offer
            if (!globalDigest.digestHex().equals(putObjectResult.getDigestHashBase16())) {
                LOGGER.error(
                    "Digest invalid for tenant: {} offer: {} id: {}",
                    ParameterHelper.getTenantParameter(),
                    offer.getId(),
                    request.getGuid()
                );
                throw new StorageInconsistentStateException(
                    "[Driver:" +
                    driver.getName() +
                    "] Content " +
                    "digest invalid in offer id : '" +
                    offer.getId() +
                    "' for object " +
                    request.getGuid() +
                    " sent digest: " +
                    globalDigest.digestHex() +
                    " offer digest: " +
                    putObjectResult.getDigestHashBase16()
                );
            }
            response = new ThreadResponseData(
                new StoragePutResult(
                    putObjectResult.getTenantId(),
                    putObjectResult.getType(),
                    putObjectResult.getGuid(),
                    putObjectResult.getDistantObjectId(),
                    globalDigest.digestHex(),
                    putObjectResult.getObjectSize()
                ),
                Response.Status.CREATED,
                request.getGuid()
            );
        }
        return response;
    }
}
