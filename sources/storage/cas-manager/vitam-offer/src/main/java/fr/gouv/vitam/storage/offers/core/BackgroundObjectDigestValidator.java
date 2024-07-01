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

import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogLevel;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorage;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResultEntry;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackgroundObjectDigestValidator {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BackgroundObjectDigestValidator.class);

    private final AlertService alertService = new AlertServiceImpl();

    private final ContentAddressableStorage defaultStorage;
    private final ExecutorService executor;
    private final String containerName;
    private final DigestType digestType;
    private final int tenantId;
    private final String requestId;

    private final List<StorageBulkPutResultEntry> writtenObjects = Collections.synchronizedList(new ArrayList<>());

    private final AtomicBoolean conflictsReported = new AtomicBoolean(false);
    private final AtomicBoolean technicalExceptionsReported = new AtomicBoolean(false);

    public BackgroundObjectDigestValidator(
        ContentAddressableStorage defaultStorage,
        String containerName,
        DigestType digestType
    ) {
        this.tenantId = VitamThreadUtils.getVitamSession().getTenantId();
        this.requestId = VitamThreadUtils.getVitamSession().getRequestId();
        this.defaultStorage = defaultStorage;
        this.containerName = containerName;
        this.digestType = digestType;
        this.executor = Executors.newFixedThreadPool(1, VitamThreadFactory.getInstance());
    }

    public void addWrittenObjectToCheck(String objectName, String objectDigest, long size) {
        executor.submit(() -> checkObjectDigestTask(objectName, objectDigest, size));
    }

    public void addExistingWormObjectToCheck(String objectName, String objectDigest, long size) {
        executor.submit(() -> checkRewritableObjectDigestTask(objectName, objectDigest, size));
    }

    private void checkObjectDigestTask(String objectId, String objectDigest, long size) {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(requestId);

        try {
            this.writtenObjects.add(new StorageBulkPutResultEntry(objectId, objectDigest, size));

            defaultStorage.checkObjectDigestAndStoreDigest(containerName, objectId, objectDigest, digestType, size);
            LOGGER.debug("Digest validation / persistence succeeded for Object {}/{}", containerName, objectId);
        } catch (ContentAddressableStorageException e) {
            LOGGER.error("Could not compute digest of object " + containerName + "/" + objectId, e);
            this.technicalExceptionsReported.set(true);
        } catch (RuntimeException e) {
            LOGGER.error(
                "Unexpected error occurred during digest validation or persistence of object " +
                containerName +
                "/" +
                objectId,
                e
            );
            this.technicalExceptionsReported.set(true);
        }
    }

    private void checkRewritableObjectDigestTask(String objectId, String objectDigest, long size) {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setRequestId(requestId);

        try {
            // Check actual object digest (without cache for full checkup)
            String actualObjectDigest = this.defaultStorage.getObjectDigest(containerName, objectId, digestType, true);

            if (objectDigest.equals(actualObjectDigest)) {
                // Mark the object as written for idempotency
                this.writtenObjects.add(new StorageBulkPutResultEntry(objectId, objectDigest, size));

                LOGGER.warn(
                    "Non rewritable object updated with same content. Ignoring duplicate. Object Id {}/{}",
                    containerName,
                    objectId
                );
            } else {
                this.conflictsReported.set(true);
                alertService.createAlert(
                    VitamLogLevel.ERROR,
                    String.format(
                        "Object with id %s (%s) already exists and cannot be updated. Existing file digest=%s, input digest=%s",
                        objectId,
                        containerName,
                        actualObjectDigest,
                        objectDigest
                    )
                );
                throw new NonUpdatableContentAddressableStorageException(
                    "Object with id " +
                    objectId +
                    " already exists " +
                    "and cannot be updated. Existing object digest: " +
                    actualObjectDigest +
                    ". Digest of new objet to write " +
                    objectDigest
                );
            }
        } catch (NonUpdatableContentAddressableStorageException e) {
            LOGGER.error("Attempt to override object " + containerName + "/" + objectId, e);
            this.conflictsReported.set(true);
        } catch (ContentAddressableStorageException e) {
            LOGGER.error("Could not compute digest of object " + containerName + "/" + objectId, e);
            this.technicalExceptionsReported.set(true);
        } catch (RuntimeException e) {
            LOGGER.error(
                "Unexpected error occurred during digest validation of object " + containerName + "/" + objectId,
                e
            );
            this.technicalExceptionsReported.set(true);
        }
    }

    public void awaitTermination() throws ContentAddressableStorageException {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Executor termination failed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ContentAddressableStorageException("Thread interrupted", e);
        }
    }

    public boolean hasConflictsReported() {
        return this.conflictsReported.get();
    }

    public boolean hasTechnicalExceptionsReported() {
        return this.technicalExceptionsReported.get();
    }

    public List<StorageBulkPutResultEntry> getWrittenObjects() {
        return Collections.unmodifiableList(writtenObjects);
    }
}
