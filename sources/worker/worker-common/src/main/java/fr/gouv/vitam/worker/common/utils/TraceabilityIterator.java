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
package fr.gouv.vitam.worker.common.utils;

import com.google.common.collect.Iterables;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import org.bson.Document;

import java.util.Iterator;
import java.util.List;

import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument.EVENTS;

/**
 * traceability iterator : help to compute endDate of events and iterator size
 */
public class TraceabilityIterator implements Iterator<LogbookOperation> {

    private static final String EVENT_DATE_TIME = "evDateTime";

    private LogbookOperation lastDocument;

    private long numberOfLine;

    private final MongoCursor<LogbookOperation> mongoCursor;

    /**
     * @param mongoCursor of logbook operation
     */
    public TraceabilityIterator(MongoCursor<LogbookOperation> mongoCursor) {
        numberOfLine = 0;
        this.mongoCursor = mongoCursor;
    }

    /**
     * Returns {@code true} if the iteration has more elements. (In other words, returns {@code true} if {@link #next}
     * would return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public boolean hasNext() {
        return mongoCursor.hasNext();
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration
     */
    @Override
    public LogbookOperation next() {
        numberOfLine += 1;
        lastDocument = mongoCursor.next();
        return lastDocument;
    }

    /**
     * @return the last date of document or event
     */
    public String endDate() {
        final String evDateTime = lastDocument.getString(EVENT_DATE_TIME);
        final List<Document> events = (List<Document>) lastDocument.get(EVENTS);

        if (events != null && events.size() > 0) {
            final Document last = Iterables.getLast(events);
            final String lastEventDate = last.getString(EVENT_DATE_TIME);
            return lastEventDate.compareTo(evDateTime) > 0 ? lastEventDate : evDateTime;
        }
        return evDateTime;
    }

    /**
     * @return size of the iterator
     */
    public long getNumberOfLine() {
        return numberOfLine;
    }
}
