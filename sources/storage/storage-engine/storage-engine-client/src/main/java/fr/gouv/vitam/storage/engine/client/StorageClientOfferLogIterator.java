/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.storage.engine.client;

import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.iterables.BulkBufferingEntryIterator;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * Implementation of TraceabilityIterator for Storage.
 *
 * Iterate over OfferLog for the traceability
 */
public class StorageClientOfferLogIterator extends BulkBufferingEntryIterator<OfferLog> {

    private final String strategyId;
    private final String offerId;
    private final Order order;
    private final DataCategory dataCategory;
    private final StorageClientFactory storageClientFactory;
    private final int chunkSize;
    private Long lastOffset;

    public StorageClientOfferLogIterator(StorageClientFactory storageClientFactory, String strategyId, String offerId, Order order,
        DataCategory dataCategory,
        int chunkSize, Long startOffset) {
        super(chunkSize);

        this.strategyId = strategyId;
        this.offerId = offerId;
        this.order = order;
        this.dataCategory = dataCategory;
        this.storageClientFactory = storageClientFactory;
        this.chunkSize = chunkSize;
        this.lastOffset = startOffset;
    }

    @Override
    protected List<OfferLog> loadNextChunk(int chunkSize) {

        try (StorageClient storageClient = this.storageClientFactory.getClient()) {
            RequestResponse<OfferLog> response = storageClient.getOfferLogs(this.strategyId,
                this.offerId, this.dataCategory, this.lastOffset, this.chunkSize, this.order);

            if (!response.isOk()) {
                throw new VitamRuntimeException("Could not list offer log");
            }

            List<OfferLog> buffer = ((RequestResponseOK<OfferLog>) response).getResults();

            if (!CollectionUtils.isEmpty(buffer)) {

                switch (this.order) {

                    case ASC:
                        this.lastOffset = buffer.get(buffer.size() - 1).getSequence() + 1;
                        break;
                    case DESC:
                        this.lastOffset = buffer.get(buffer.size() - 1).getSequence() - 1;
                        break;
                    default:
                        throw new IllegalStateException("Invalid order " + this.order);
                }
            }
            return buffer;

        } catch (StorageServerClientException e) {
            throw new VitamRuntimeException(e);
        }
    }
}
