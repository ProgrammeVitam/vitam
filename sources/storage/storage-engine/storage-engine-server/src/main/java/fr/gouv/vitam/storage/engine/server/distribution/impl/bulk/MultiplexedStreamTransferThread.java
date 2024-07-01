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

import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutRequest;
import fr.gouv.vitam.storage.driver.model.StorageBulkPutResult;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;

public class MultiplexedStreamTransferThread implements Callable<StorageBulkPutResult> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MultiplexedStreamTransferThread.class);

    private final int tenantId;
    private final DataCategory dataCategory;
    private final List<String> objectIds;
    private final InputStream inputStream;
    private final long size;
    private final Driver driver;
    private final StorageOffer storageOffer;
    private final DigestType digestType;
    private final String requestId;

    public MultiplexedStreamTransferThread(
        int tenantId,
        String requestId,
        DataCategory dataCategory,
        List<String> objectIds,
        InputStream inputStream,
        long size,
        Driver driver,
        StorageOffer storageOffer,
        DigestType digestType
    ) {
        this.tenantId = tenantId;
        this.requestId = requestId;
        this.dataCategory = dataCategory;
        this.objectIds = objectIds;
        this.inputStream = inputStream;
        this.size = size;
        this.driver = driver;
        this.storageOffer = storageOffer;
        this.digestType = digestType;
    }

    @Override
    public StorageBulkPutResult call() throws Exception {
        String initialThreadId = Thread.currentThread().getName();
        try {
            Thread.currentThread().setName(initialThreadId + "-BulkTransferThread-" + storageOffer.getId());
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            VitamThreadUtils.getVitamSession().setRequestId(requestId);
            return storeInOffer();
        } catch (Exception ex) {
            LOGGER.error("An error occurred during bulk transfer to offer " + this.storageOffer.getId(), ex);
            throw ex;
        } finally {
            IOUtils.closeQuietly(this.inputStream);
            Thread.currentThread().setName(initialThreadId);
        }
    }

    private StorageBulkPutResult storeInOffer() throws Exception {
        try (Connection connection = driver.connect(storageOffer.getId())) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            StorageBulkPutRequest storageBulkPutRequest = new StorageBulkPutRequest(
                this.tenantId,
                this.dataCategory.getFolder(),
                this.objectIds,
                this.digestType,
                this.inputStream,
                this.size
            );
            LOGGER.debug("Bulk put request {}", storageBulkPutRequest);

            StorageBulkPutResult bulkPutObjectResult = connection.bulkPutObjects(storageBulkPutRequest);
            LOGGER.debug("Bulk put result {}", bulkPutObjectResult);

            return bulkPutObjectResult;
        }
    }
}
