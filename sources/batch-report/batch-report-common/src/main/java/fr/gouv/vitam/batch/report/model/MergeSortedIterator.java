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
package fr.gouv.vitam.batch.report.model;

import fr.gouv.vitam.common.ParametersChecker;
import org.apache.commons.collections4.iterators.PeekingIterator;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;


/**
 * This Iterator take two sorted iterators and merge them.
 * Loop over iterators and use comparator in order to take items that are equals.
 * The merge is done using the function mergeFunction
 *
 * @param <A>
 * @param <E>
 */
public class MergeSortedIterator<A, E> implements Iterator<E> {

    private PeekingIterator<A> one;
    private PeekingIterator<A> two;
    private BiFunction<A, A, E> mergeFunction;
    private Comparator<A> comparator;

    /**
     * @param one The first sorted iterator
     * @param two The second sorted iterator
     * @param comparator The comparator that compare items of iterators
     * @param mergeFunction The function that merge elements of iterators
     */
    public MergeSortedIterator(Iterator<A> one, Iterator<A> two, Comparator<A> comparator,
        BiFunction<A, A, E> mergeFunction) {
        ParametersChecker.checkParameter("All params are required", one, two, comparator, mergeFunction);
        this.one = PeekingIterator.peekingIterator(one);
        this.two = PeekingIterator.peekingIterator(two);
        this.mergeFunction = mergeFunction;
        this.comparator = comparator;
    }

    @Override
    public boolean hasNext() {
        return one.hasNext() || two.hasNext();
    }

    @Override
    public E next() {

        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        int compare = comparator.compare(one.peek(), two.peek());

        if (compare == 0) {

            return mergeFunction.apply(one.next(), two.next());

        }

        if (compare > 0) {

            return mergeFunction.apply(one.next(), null);

        }


        return mergeFunction.apply(null, two.next());
    }
}
