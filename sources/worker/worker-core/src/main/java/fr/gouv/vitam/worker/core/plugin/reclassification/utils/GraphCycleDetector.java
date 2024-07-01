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
package fr.gouv.vitam.worker.core.plugin.reclassification.utils;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper class for graph cycle detection.
 *
 * Usage :
 * - addRelations() + removeRelations() to create target graph.
 * - checkCycles() to get cycles in the graph, if any.
 */
public class GraphCycleDetector {

    private MultiValuedMap<String, String> childToParents = new HashSetValuedHashMap<>();
    private MultiValuedMap<String, String> parentToChildren = new HashSetValuedHashMap<>();

    /**
     * Add child/parent relations to the graph
     *
     * @param child the child unit id
     * @param parents the list of parent unit ids
     */
    public void addRelations(String child, Collection<String> parents) {
        childToParents.putAll(child, parents);
        for (String parent : parents) {
            parentToChildren.put(parent, child);
        }
    }

    /**
     * Remove child/parent relations from the graph
     *
     * @param child the child unit id
     * @param parents the list of parent unit ids
     */
    public void removeRelations(String child, Collection<String> parents) {
        for (String parent : parents) {
            childToParents.removeMapping(child, parent);
            parentToChildren.removeMapping(parent, child);
        }
    }

    /**
     * Check cycles among the graph.
     *
     * @return a set of unit ids having one or more cycles is any. An empty set is returned if no cycles are detected.
     */
    public Set<String> checkCycles() {
        // Remove root units recursively
        removeNodesWithoutParents();

        // Remove leaf units recursively
        removeNodesWithoutChildren();

        // Compute cycles
        return getCycles();
    }

    /**
     * Remove root units recursively.
     *
     * Removes units that have no parents, then the next level of units that have no parents and so one...
     * If at some
     */
    private void removeNodesWithoutParents() {
        while (true) {
            List<String> rootParents = parentToChildren
                .keySet()
                .stream()
                .filter(node -> !childToParents.containsKey(node))
                .collect(Collectors.toList());

            if (rootParents.isEmpty()) {
                return;
            }

            for (String rootParent : rootParents) {
                for (String child : parentToChildren.get(rootParent)) {
                    childToParents.removeMapping(child, rootParent);
                }
                parentToChildren.remove(rootParent);
            }
        }
    }

    private void removeNodesWithoutChildren() {
        while (true) {
            List<String> leaves = childToParents
                .keySet()
                .stream()
                .filter(node -> !parentToChildren.containsKey(node))
                .collect(Collectors.toList());

            if (leaves.isEmpty()) {
                return;
            }

            for (String leaf : leaves) {
                for (String child : childToParents.get(leaf)) {
                    parentToChildren.removeMapping(child, leaf);
                }
                childToParents.remove(leaf);
            }
        }
    }

    private Set<String> getCycles() {
        return childToParents.keySet();
    }

    @VisibleForTesting
    MultiValuedMap<String, String> getChildToParents() {
        return childToParents;
    }

    @VisibleForTesting
    MultiValuedMap<String, String> getParentToChildren() {
        return parentToChildren;
    }
}
