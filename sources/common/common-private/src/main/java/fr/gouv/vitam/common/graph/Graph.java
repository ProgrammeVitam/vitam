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

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Graph contains Directed Acyclic Graph
 */
public class Graph {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(Graph.class);

    private final Vertex[] vertices;
    private int size;
    private final int maxSize;
    private final Deque<Vertex> stack;
    private int count = 0;

    // map id_xml index
    private final BidiMap<Integer, String> indexMapping;

    // set of roots
    private final Set<String> roots;
    // longest path
    private Map<Integer, Set<String>> longestsPath;

    /**
     * Graph constructor
     *
     * @param jsonGraph { "ID027" : { }, "ID028" : { "_up" : [ "ID027" ] }, "ID029" : { "_up" : [ "ID028" ] }}
     */
    public Graph(JsonNode jsonGraph) {
        roots = new HashSet<>();
        indexMapping = new DualHashBidiMap<>();
        maxSize = jsonGraph.size();
        // number of vertice
        LOGGER.debug("maxSize:" + maxSize);
        vertices = new Vertex[maxSize];
        stack = new ArrayDeque<>(maxSize);
        for (int i = 0; i < maxSize; i++) {
            addVertex(i + 1);
        }
        // parse json to create graph
        final Iterator<Entry<String, JsonNode>> levelIterator = jsonGraph.fields();
        while (levelIterator.hasNext()) {
            final Entry<String, JsonNode> cycle = levelIterator.next();
            final String idChild = cycle.getKey();
            final JsonNode up = cycle.getValue();
            // create mappping
            addMapIdToIndex(idChild);

            if (up != null && up.size() > 0) {
                final JsonNode arrNode = up.get("_up");

                for (final JsonNode idParent : arrNode) {
                    addEdge(getIndex(idParent.textValue()), getIndex(idChild));

                    LOGGER.debug("source:" + idParent);
                    LOGGER.debug("destin:" + idChild);
                }
            } else {
                roots.add(idChild);
            }
        }
    }

    private int addMapIdToIndex(String idXml) {
        if (indexMapping != null) {
            // FIXME P1 since called many times, better to assign one for all this inverseBidiMap to a private variable
            final BidiMap<String, Integer> xmlIdToIndex = indexMapping.inverseBidiMap();
            if (xmlIdToIndex.get(idXml) == null) {
                count++;
                indexMapping.put(count, idXml);
            }
        }
        return count;
    }

    /**
     * add vertex
     *
     * @param data
     */
    private void addVertex(int data) {
        vertices[size++] = new Vertex(data);
    }

    /**
     * add edge
     *
     * @param source
     * @param destination
     */
    private void addEdge(int source, int destination) {
        vertices[source - 1].adj = new Neighbour(destination - 1, vertices[source - 1].adj);
    }

    /**
     * Vertex class
     */
    public class Vertex {

        int data;
        Neighbour adj;
        int cost = 0;
        State state = State.NEW;

        /**
         * Vertex constructor
         *
         * @param data
         */
        public Vertex(int data) {
            this.data = data;
        }
    }

    /**
     * state enum
     */
    public enum State {
        NEW,
        VISITED,
    }

    /**
     * Neighbour class
     */
    public class Neighbour {

        int index;
        Neighbour next;
        int weight = 1;

        Neighbour(int index, Neighbour next) {
            this.index = index;
            this.next = next;
        }
    }

    /**
     * @param source
     * @return {@link Map} : longest path for list of child
     */
    private Map<Integer, Integer> findLongestsPath(int source) {
        applyTopologicalSort();
        final Map<Integer, Integer> longestsPathMap = new HashMap<>();
        vertices[source - 1].cost = 0;
        while (!stack.isEmpty()) {
            final Vertex u = stack.pop();
            if (u.cost != Integer.MIN_VALUE) {
                Neighbour temp = u.adj;
                while (temp != null) {
                    final Vertex v = vertices[temp.index];
                    if (v.cost < temp.weight + u.cost) {
                        v.cost = temp.weight + u.cost;
                    }
                    temp = temp.next;
                }
            }
        }
        // put longest path in the map
        for (int i = 0; i < maxSize; i++) {
            longestsPathMap.put(i + 1, vertices[i].cost);
        }
        LOGGER.debug("Longest path from " + longestsPath);
        return longestsPathMap;
    }

    /**
     * create all longest path (or level stack: will be respected when indexing the units)
     *
     * @param roots
     * @return Map<Integer, Integer> :longest path for different roots
     */
    private Map<Integer, Integer> findAllLongestsPath(Set<String> roots) {
        final Map<Integer, Integer> allLongestsPath = new HashMap<>();
        for (final String rootXmlId : roots) {
            // find longest path for different roots
            final Map<Integer, Integer> longestsPathMap = findLongestsPath(getIndex(rootXmlId));
            if (allLongestsPath.isEmpty()) {
                // if empty put all distances
                allLongestsPath.putAll(longestsPathMap);
            } else {
                // we verify the longest path
                for (final Map.Entry<Integer, Integer> e : longestsPathMap.entrySet()) {
                    final Integer key = e.getKey();
                    final Integer value = e.getValue();
                    LOGGER.debug("key" + key + "------------ value:" + indexMapping.get(key));
                    if (allLongestsPath.containsKey(key) && allLongestsPath.get(key) < value) {
                        // replace old value
                        allLongestsPath.put(key, value);
                    }
                }
            }
        }

        return allLongestsPath;
    }

    /**
     * create level stack: the longest path for different roots
     *
     * @return {@link Map}
     */
    public Map<Integer, Set<String>> getGraphWithLongestPaths() {
        longestsPath = new HashMap<>();
        Map<Integer, Integer> paths = findAllLongestsPath(roots);
        if (paths != null) {
            for (final Map.Entry<Integer, Integer> e : paths.entrySet()) {
                final Integer unitId = e.getKey();
                final Integer level = e.getValue();
                if (longestsPath.containsKey(level) && longestsPath.get(level) != null) {
                    // add value
                    longestsPath.get(level).add(indexMapping.get(unitId));
                } else {
                    final Set<String> units = new HashSet<>();
                    units.add(indexMapping.get(unitId));
                    longestsPath.put(level, units);
                }
            }
        }
        paths = null;
        return longestsPath;
    }

    private void applyTopologicalSort() {
        for (int i = 0; i < maxSize; i++) {
            if (vertices[i].state != State.VISITED) {
                depthFirstSearch(vertices[i]);
            }
        }
    }

    private void depthFirstSearch(Vertex u) {
        Neighbour temp = u.adj;
        u.state = State.VISITED;
        while (temp != null) {
            final Vertex v = vertices[temp.index];
            if (v.state == State.NEW) {
                depthFirstSearch(v);
            }
            temp = temp.next;
        }
        stack.push(u);
    }

    private int getIndex(String id) {
        int key = 0;
        if (indexMapping != null) {
            // check value if exist
            if (indexMapping.containsValue(id)) {
                final BidiMap<String, Integer> xmlIdToIndex = indexMapping.inverseBidiMap();
                key = xmlIdToIndex.get(id);
            } else {
                key = addMapIdToIndex(id);
            }

            return key;
        }
        return key;
    }
}
