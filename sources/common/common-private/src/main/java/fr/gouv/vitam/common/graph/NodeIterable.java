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
package fr.gouv.vitam.common.graph;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * NodeIterable class
 *
 * @param <Item>
 */
// FIXME P1 use standard class from Java (25 years of experience)
public class NodeIterable<Item> implements Iterable<Item> {

    private Node<Item> first;
    // number of elements
    private int n;

    // FIXME P1 missing a "clear" (memory pressure) or a default clear while getting next (help the GC)

    // helper linked list class
    private static class Node<Item> {

        private Item item;
        private Node<Item> next;
    }

    /**
     * Initializes an empty bag.
     */
    public NodeIterable() {
        first = null;
        n = 0;
    }

    /**
     * Returns true if this bag is empty.
     *
     * @return <tt>true</tt> if this bag is empty; <tt>false</tt> otherwise
     */
    public boolean isEmpty() {
        return first == null;
    }

    /**
     * Returns the number of items in this bag.
     *
     * @return the number of items in this bag
     */
    public int size() {
        return n;
    }

    /**
     * Adds the item to this bag.
     *
     * @param item the item to add to this bag
     */
    public void add(Item item) {
        final Node<Item> oldfirst = first;
        first = new Node<>();
        first.item = item;
        first.next = oldfirst;
        n++;
    }

    /**
     * Returns an iterator that iterates over the items in this bag in arbitrary order.
     *
     * @return an iterator that iterates over the items in this bag in arbitrary order
     */
    @Override
    public Iterator<Item> iterator() {
        return new ListIterator<>(first);
    }

    // an iterator, doesn't implement remove() since it's optional
    private class ListIterator<E> implements Iterator<E> {

        private Node<E> current;

        /**
         * @param first
         */
        public ListIterator(Node<E> first) {
            current = first;
        }

        /**
         * check if has next
         */
        @Override
        public boolean hasNext() {
            return current != null;
        }

        /**
         * unsupported methods remove
         *
         * @throws UnsupportedOperationException
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * iterate if has next
         */
        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            final E item = current.item;
            current = current.next;
            return item;
        }
    }
}
