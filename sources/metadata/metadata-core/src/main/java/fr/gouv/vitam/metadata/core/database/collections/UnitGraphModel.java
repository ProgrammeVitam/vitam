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
package fr.gouv.vitam.metadata.core.database.collections;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.graph.GraphUtils.createGraphRelation;

/**
 * unit graph model.
 */
public class UnitGraphModel {

    private final String id;

    private final Set<String> up = new HashSet<>();

    private final Set<String> us = new HashSet<>();

    private final String originatingAgency;

    private final Set<String> originatingAgencies = new HashSet<>();

    private final SetMultimap<Integer, String> uds = HashMultimap.create();

    private int maxDepth = 1;

    private final Set<String> graph = new HashSet<>();

    /**
     * @param id unit id of unit
     * @param originatingAgency originating agency of unit
     */
    public UnitGraphModel(String id, String originatingAgency) {
        this.id = id;
        this.originatingAgency = originatingAgency;
        if (originatingAgency != null) {
            this.originatingAgencies.add(originatingAgency);
        }
    }

    /**
     * @param unit
     */
    public UnitGraphModel(Unit unit) {
        this(unit.getId(), unit.getSp());
    }

    /**
     * Add graph parent information into current graph. Compute all graph information from current au.
     *
     * @param parent graph information.
     */
    public void addParent(UnitGraphModel parent) {
        up.add(parent.id);
        us.add(parent.id);
        us.addAll(parent.us);
        originatingAgencies.addAll(parent.originatingAgencies);

        SetMultimap<Integer, String> parentUds = parent.uds;

        for (Map.Entry<Integer, String> entry : parentUds.entries()) {
            uds.put(entry.getKey() + 1, entry.getValue());
        }
        uds.put(1, parent.id);

        graph.add(createGraphRelation(id, parent.id));
        graph.addAll(parent.graph);

        maxDepth = uds.keys().stream().max(Comparator.naturalOrder()).orElse(0) + 1;
    }

    public String id() {
        return id;
    }

    public Set<String> parents() {
        return up;
    }

    public Set<String> ancestors() {
        return us;
    }

    public String originatingAgency() {
        return originatingAgency;
    }

    public Set<String> originatingAgencies() {
        return originatingAgencies;
    }

    public Map<String, Collection<String>> unitDepths() {
        return uds
            .asMap()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(i -> Integer.toString(i.getKey()), Map.Entry::getValue));
    }

    public int minDepth() {
        return 1;
    }

    public int maxDepth() {
        return maxDepth;
    }

    public Set<String> graph() {
        return graph;
    }
}
