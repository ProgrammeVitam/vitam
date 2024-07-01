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

import fr.gouv.vitam.common.exception.CycleFoundException;

import java.util.Stack;

/**
 * DirectedCycle Class
 */
public class DirectedCycle {

    // has vertex v been marked
    private final boolean[] marked;
    // previous vertex on path to v
    private final int[] edgeTo;
    // is vertex on the stack?
    private final boolean[] onStack;
    // directed cycle (or null if no such cycle)
    private Stack<Integer> cycle;

    private boolean isCyclic;

    /**
     * DirectedCycle a constructor :fired when a cycle is found.
     *
     * @param graph the DirectedCycle
     * @throws CycleFoundException
     */
    public DirectedCycle(DirectedGraph graph) {
        marked = new boolean[graph.getVertices()];
        onStack = new boolean[graph.getVertices()];
        edgeTo = new int[graph.getVertices()];
        // FIXME P1 n² or even worth => while merging Graph and DIrectedGraph, you can have the "real" roots (from
        // Graph)
        // so using it
        for (int v = 0; v < graph.getVertices(); v++) {
            if (!marked[v] && cycle == null) {
                depthFirstSearch(graph, v);
            }
        }
    }

    /**
     * depthFirstSearch is a method for traversing or searching graph data structures. One starts at the root and
     * explores as far as possible along each branch.
     *
     * @param graph
     * @param root
     * @throws CycleFoundException
     */
    private void depthFirstSearch(DirectedGraph graph, int root) {
        // TODO P1 : the case of graphs which are not strongly connected must be managed
        onStack[root] = true;
        marked[root] = true;
        for (final int w : graph.adj(root)) {
            // short circuit if directed cycle found
            if (cycle != null) {
                return;
            } else if (!marked[w]) {
                edgeTo[w] = root;
                depthFirstSearch(graph, w);
            } else if (onStack[w]) {
                // trace back directed cycle
                // FIXME P1 you reallocate memory (stack) how many times ??? Clean such as GC is not under pressure
                // FIXME P1 If I understand correctly, once here, we have a cycle (cycle != null), so why doing the next
                // computation ?
                cycle = new Stack<>();
                for (int x = root; x != w; x = edgeTo[x]) {
                    cycle.push(x);
                }
                cycle.push(w);
                cycle.push(root);
                if (check()) {
                    isCyclic = true;
                    return;
                }
            }
        }
        onStack[root] = false;
    }

    /**
     * Does the DirectedCycle have a directed cycle
     *
     * @return <tt>true</tt> if the Graph has a directed cycle, <tt>false</tt> otherwise
     */
    public boolean hasCycle() {
        return cycle != null;
    }

    /**
     * Returns a cycle if the Graph has a directed cycle, and <tt>null</tt> otherwise.
     *
     * @return a cycle (as an iterable) if the DirectedCycle has a cycle, and <tt>null</tt> otherwise
     */
    private Iterable<Integer> cycle() {
        return cycle;
    }

    /**
     * certify that Graph has a cycle if it reports one
     *
     * @return boolean
     */
    private boolean check() {
        if (hasCycle()) {
            // verify cycle
            int first = -1;
            int last = -1;
            for (final int v : cycle()) {
                if (first == -1) {
                    first = v;
                }
                last = v;
            }
            // graph don't have a directed cycle
            if (first != last) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * isCyclic know of a graph is cyclic or not
     *
     * @return boolean
     */
    public boolean isCyclic() {
        return isCyclic;
    }

    /**
     * Return the directed cycle if isCyclic
     *
     * @return the directed cycle or null
     */
    public Stack<Integer> getCycle() {
        return cycle;
    }
}
