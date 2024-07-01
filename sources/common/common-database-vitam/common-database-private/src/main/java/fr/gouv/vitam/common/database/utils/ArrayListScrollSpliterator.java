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
package fr.gouv.vitam.common.database.utils;

import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.model.DatabaseCursor;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * ScrollSpliterator
 *
 * @param <T>
 */
public class ArrayListScrollSpliterator<T> extends AbstractSpliterator<List<T>> {

    private final SelectMultiQuery query;
    private final Function<SelectMultiQuery, RequestResponse<List<T>>> repository;
    private final int scrollTimeout;
    private final int limit;
    private long size;
    private DatabaseCursor hits;
    private RequestResponseOK<List<T>> requestResponse;
    private Iterator<List<T>> results;
    private String scrollId;

    /**
     * Constructor
     *
     * @param query the select query
     * @param repository the repository
     * @param scrollTimeout scroll timeout
     * @param limit the limit
     */
    public ArrayListScrollSpliterator(
        SelectMultiQuery query,
        Function<SelectMultiQuery, RequestResponse<List<T>>> repository,
        int scrollTimeout,
        int limit
    ) {
        super(Long.MAX_VALUE, DISTINCT | SIZED | NONNULL);
        this.query = query;
        this.repository = repository;
        this.scrollTimeout = scrollTimeout;
        this.limit = limit;
        this.size = 0;
        this.scrollId = "START";
    }

    @Override
    public boolean tryAdvance(Consumer<? super List<T>> action) {
        if (requestResponse == null) {
            executeQuery();
        }
        if (results.hasNext()) {
            applyAndIncrementSize(action);
            return true;
        }
        if (size < hits.getTotal()) {
            executeQuery();
            applyAndIncrementSize(action);
            return true;
        }
        return false;
    }

    private void applyAndIncrementSize(Consumer<? super List<T>> action) {
        List<T> next = results.next();
        action.accept(next);
        this.size += next.size();
    }

    @Override
    public long estimateSize() {
        if (requestResponse == null) {
            executeQuery();
        }
        return hits.getTotal();
    }

    private void executeQuery() {
        query.setScrollFilter(scrollId, scrollTimeout, limit);
        requestResponse = (RequestResponseOK<List<T>>) repository.apply(query);
        hits = requestResponse.getHits();
        results = requestResponse.getResults().iterator();
        scrollId = hits.getScrollId();
    }
}
