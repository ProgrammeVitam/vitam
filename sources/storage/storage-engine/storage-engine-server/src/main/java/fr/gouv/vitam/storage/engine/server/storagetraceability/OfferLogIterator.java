/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/
package fr.gouv.vitam.storage.engine.server.storagetraceability;

import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Implementation of TraceabilityIterator for Storage.
 *
 * Iterate over OfferLog for the traceability
 */
public class OfferLogIterator implements Iterator<String> {

    private final String strategyId;
    private final StorageDistribution distribution;
    private final Order order;
    private final DataCategory dataCategory;
    private final int bufferLimit;
    private Long lastOffset;

    private List<OfferLog> buffer;
    private int nextPos = 0;
    private boolean endOfStream = false;

    public OfferLogIterator(String strategyId, Order order, DataCategory dataCategory,
        StorageDistribution distribution, int bufferLimit) {

        this.strategyId = strategyId;
        this.order = order;
        this.dataCategory = dataCategory;
        this.distribution = distribution;
        this.bufferLimit = bufferLimit;
        this.lastOffset = null;
    }

    @Override
    public boolean hasNext() {

        if (this.endOfStream) {
            return false;
        }

        if (this.buffer == null || this.nextPos >= this.buffer.size()) {
            loadResultBuffer();
        }

        return !this.endOfStream;
    }

    private void loadResultBuffer() {

        try {
            RequestResponse<OfferLog> response = this.distribution.getOfferLogs(this.strategyId,
                this.dataCategory, this.lastOffset, this.bufferLimit, this.order);

            if (!response.isOk()) {
                throw new StorageException("Could not list offer log");
            }

            this.buffer = ((RequestResponseOK<OfferLog>) response).getResults();
            this.nextPos = 0;

            if (this.buffer.isEmpty()) {
                this.endOfStream = true;
            } else {

                switch (this.order) {

                    case ASC:
                        this.lastOffset = this.buffer.get(this.buffer.size() - 1).getSequence() + 1;
                        break;
                    case DESC:
                        this.lastOffset = this.buffer.get(this.buffer.size() - 1).getSequence() - 1;
                        break;
                    default:
                        throw new IllegalStateException("Invalid order " + this.order);
                }
            }
        } catch (StorageException e) {
            throw new VitamRuntimeException(e);
        }
    }

    @Override
    public String next() {

        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        OfferLog offerLog = this.buffer.get(nextPos);
        nextPos++;

        return offerLog.getFileName();
    }
}
