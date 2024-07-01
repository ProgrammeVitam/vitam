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
package fr.gouv.vitam.common.collection;

import java.util.Iterator;
import java.util.function.Function;

/**
 * Helper class for CloseableIterators
 */
public final class CloseableIteratorUtils {

    private CloseableIteratorUtils() {}

    /**
     * Maps a CloseableIterator from un input type T to an output type R using a mapper function.
     */
    public static <T, R> CloseableIterator<R> map(
        CloseableIterator<T> closeableIterator,
        Function<? super T, ? extends R> mapper
    ) {
        return new CloseableIterator<R>() {
            @Override
            public boolean hasNext() {
                return closeableIterator.hasNext();
            }

            @Override
            public R next() {
                return mapper.apply(closeableIterator.next());
            }

            @Override
            public void close() {
                closeableIterator.close();
            }
        };
    }

    /**
     * Converts an Iterator to a CloseableIterator.
     * The close methods does nothing.
     */
    public static <E> CloseableIterator<E> toCloseableIterator(Iterator<E> iterator) {
        return new CloseableIteratorWrapper<>(iterator);
    }

    /**
     * Converts an Iterable to a CloseableIterator.
     * The close methods does nothing.
     */
    public static <E> CloseableIterator<E> toCloseableIterator(Iterable<E> iterable) {
        return new CloseableIteratorWrapper<>(iterable.iterator());
    }

    private static class CloseableIteratorWrapper<E> implements CloseableIterator<E> {

        private final Iterator<E> iterator;

        private CloseableIteratorWrapper(Iterator<E> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public E next() {
            return iterator.next();
        }

        @Override
        public void close() {
            // NOP
        }
    }
}
