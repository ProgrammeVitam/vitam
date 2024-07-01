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
package fr.gouv.vitam.common.iterables;

import org.apache.commons.collections4.CollectionUtils;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Iterator that loads data in chunks, and return single entries
 */
public abstract class BulkBufferingEntryIterator<T> implements Iterator<T> {

    private final int bufferSize;

    private List<T> buffer;
    private int nextPos = 0;
    private boolean endOfStream = false;

    public BulkBufferingEntryIterator(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    @Override
    public boolean hasNext() {
        if (this.endOfStream) {
            return false;
        }

        if (this.buffer == null) {
            // First invocation
            load();
        } else if (this.nextPos >= this.buffer.size()) {
            // No more items in current buffer

            if (this.buffer.size() < bufferSize) {
                // Current buffer is incomplete ==> no more data
                this.endOfStream = true;
            } else {
                load();
            }
        }

        return !this.endOfStream;
    }

    private void load() {
        this.buffer = loadNextChunk(this.bufferSize);
        this.nextPos = 0;
        this.endOfStream = CollectionUtils.isEmpty(this.buffer);
    }

    /**
     * Loads a chunk of the specified size.
     *
     * @return List with next entries to process. Returned list must be the size specified, unless end of data is reached.
     */
    protected abstract List<T> loadNextChunk(int chunkSize);

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        T item = this.buffer.get(nextPos);
        nextPos++;
        return item;
    }
}
