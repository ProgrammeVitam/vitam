/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.storage;

import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.stream.StreamUtils;
import org.apache.commons.collections4.iterators.PeekingIterator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ObjectEntryReader implements CloseableIterator<ObjectEntry> {

    private static final String EMPTY_ENTRY = "{}";

    private final InputStream inputStream;
    private final PeekingIterator<String> iterator;

    public ObjectEntryReader(InputStream inputStream) {
        this.inputStream = inputStream;
        this.iterator = new PeekingIterator<>(
            new BufferedReader(new InputStreamReader(inputStream)).lines().iterator()
        );
    }

    @Override
    public boolean hasNext() {
        // Last entry is a special EOF entry with null objectId.
        // If EOF is encountered before the EOF entry, then the stream is broken.
        if (!iterator.hasNext()) {
            throw new RuntimeException("Premature EOF");
        }
        String nextEntry = iterator.peek();
        return !EMPTY_ENTRY.equals(nextEntry);
    }

    @Override
    public ObjectEntry next() {
        if (!hasNext()) {
            throw new IllegalStateException("No more entries to read");
        }
        try {
            return JsonHandler.getFromString(iterator.next(), ObjectEntry.class);
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        StreamUtils.closeSilently(inputStream);
    }
}
