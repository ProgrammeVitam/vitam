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
package fr.gouv.vitam.common.database.utils;

import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;

import java.util.Iterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Lifecycle Spliterator
 */
public class LifecyclesSpliterator<T> extends AbstractSpliterator<T> {

    private Select query;
    private Function<Select, RequestResponse<T>> repository;
    private int offset;
    private int limit;
    private RequestResponseOK<T> requestResponse;
    private Iterator<T> results;
    private long size;

    public LifecyclesSpliterator(Select query, Function<Select, RequestResponse<T>> repository, int offset, int limit) {
        super(Long.MAX_VALUE, DISTINCT | SIZED | NONNULL);
        this.query = query;
        this.repository = repository;
        this.offset = offset;
        this.limit = limit;
        this.size = 0;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (requestResponse == null) {
            executeQuery();
        }
        if (results.hasNext()) {
            applyAndIncrementSize(action);
            return true;
        }

        // TODO : check if size < requestResponse.getHits().getTotal() after fixing total value in LogbookMongoDbAccessImpl.selectExecute
        executeQuery();
        if (results.hasNext()) {
            applyAndIncrementSize(action);
            return true;
        }

        return false;
    }

    private void applyAndIncrementSize(Consumer<? super T> action) {
        action.accept(results.next());
        this.size += 1;
    }


    private void executeQuery() {
        query.setLimitFilter(offset, limit);
        requestResponse = (RequestResponseOK<T>) repository.apply(query);
        results = requestResponse.getResults().iterator();
        offset += limit;

    }

}
