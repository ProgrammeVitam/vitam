/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 * 
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Graph contains Directed Acyclic Graph
 */
public class Graph {


    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(Graph.class);

    private Vertex[] vertices;
    private int size;
    private int maxSize;
    private StackHelper stack;
    int count = 0;

    // map id_xml index
    BidiMap<Integer, String> indexMapping;

    // set of roots
    Set<String> roots;
    // longest path
    Map<Integer, Set<String>> longestsPath;

    /**
     * Graph constructor
     * 
     * @param JsonGraph { "ID027" : { }, "ID028" : { "_up" : [ "ID027" ] }, "ID029" : { "_up" : [ "ID028" ] }}
     */
    public Graph(JsonNode JsonGraph) {
        Iterator<Entry<String, JsonNode>> iterator = JsonGraph.fields();
        roots = new HashSet<>();
        indexMapping = new DualHashBidiMap<Integer, String>();
        this.maxSize = 0;
        // number of vertice
        while (iterator.hasNext()) {
            maxSize++;
            iterator.next();
        }
        // create vertices
        vertices = new Vertex[maxSize];
        LOGGER.info("maxSize:" + maxSize);
        stack = new StackHelper(maxSize);
        for (int i = 0; i < maxSize; i++) {
            addVertex(i + 1);
        }

        // parse json to create graph

        Iterator<Entry<String, JsonNode>> levelIterator = JsonGraph.fields();
        while (levelIterator.hasNext()) {
            Entry<String, JsonNode> cycle = levelIterator.next();

            String idChild = cycle.getKey();
            JsonNode up = cycle.getValue();
            // create mappping
            // TODO create add method (will check if map index to xml id exist)

            addMapIdToIndex(idChild);
            // indexMapping.put(count, idChild);

            if (up != null && up.size() > 0) {
                final JsonNode arrNode = up.get("_up");

                for (final JsonNode idParent : arrNode) {
                    // System.out.println(_idParent);

                    addEdge(getIndex(idParent.textValue()), getIndex(idChild));

                    LOGGER.info("source:" + idParent);
                    LOGGER.info("destin:" + idChild);

                }

            } else {
                roots.add(idChild);
            }
        }

    }

    private int addMapIdToIndex(String idXml) {
        if (indexMapping != null) {
            BidiMap<String, Integer> xmlIdToIndex = indexMapping.inverseBidiMap();
            if (!xmlIdToIndex.containsKey(idXml)) {
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
    public void addVertex(int data) {
        vertices[size++] = new Vertex(data);
    }

    /**
     * add edge
     * 
     * @param source
     * @param destination
     */
    public void addEdge(int source, int destination) {
        vertices[source - 1].adj = new Neighbour(destination - 1, vertices[source - 1].adj);
    }

    /**
     * Vertex class
     */
    public class Vertex {
        int data;
        Neighbour adj;
        int cost = Integer.MIN_VALUE;
        State state = State.NEW;

        public Vertex(int data) {
            this.data = data;
        }
    }
    /**
     * Stack class
     * 
     */
    public class StackHelper {
        Vertex[] stack;
        int maxSize;
        int size;

        /**
         * 
         * @param maxSize
         */
        public StackHelper(int maxSize) {
            this.maxSize = maxSize;
            stack = new Vertex[maxSize];
        }

        /**
         * 
         * @param data
         */
        public void push(Vertex data) {
            stack[size++] = data;
        }

        /**
         * 
         * @return vertex
         */
        public Vertex pop() {
            return stack[--size];
        }

        /**
         * 
         * @return
         */
        public boolean isEmpty() {
            return size == 0;
        }
    }
    /**
     * 
     */
    public enum State {
        NEW, VISITED
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
     * 
     * @param source
     * @return {@link Map} : longest path for list of child
     */
    private Map<Integer, Integer> findLongestsPath(int source) {
        applyTopologicalSort();
        Map<Integer, Integer> longestsPathMap = new HashMap<Integer, Integer>();
        vertices[source - 1].cost = 0;
        while (!stack.isEmpty()) {
            Vertex u = stack.pop();
            if (u.cost != Integer.MIN_VALUE) {
                Neighbour temp = u.adj;
                while (temp != null) {
                    Vertex v = vertices[temp.index];
                    if (v.cost < (temp.weight + u.cost)) {
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
        Map<Integer, Integer> allLongestsPath = new HashMap<Integer, Integer>();
        for (String rootXmlId : roots) {
            // find longest path for different roots
            Map<Integer, Integer> longestsPathMap = findLongestsPath(getIndex(rootXmlId));
            if (allLongestsPath.isEmpty()) {
                // if empty put all distances
                allLongestsPath.putAll(longestsPathMap);
            } else {
                // we verify the longest path
                for (Map.Entry<Integer, Integer> e : longestsPathMap.entrySet()) {
                    Integer key = e.getKey();
                    Integer value = e.getValue();
                    LOGGER.info("key" + key + "------------ value:" + indexMapping.get(key));
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

        this.longestsPath = new HashMap<Integer, Set<String>>();
        Map<Integer, Integer> paths = findAllLongestsPath(roots);
        if (paths != null) {
            for (Map.Entry<Integer, Integer> e : paths.entrySet()) {
                Integer unitId = e.getKey();
                Integer level = e.getValue();
                if (longestsPath.containsKey(level) && longestsPath.get(level) != null) {
                    // add value
                    longestsPath.get(level).add(indexMapping.get(unitId));

                } else {
                    Set<String> units = new HashSet<String>();
                    units.add(indexMapping.get(unitId));
                    longestsPath.put(level, units);
                }
            }
        }
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
            Vertex v = vertices[temp.index];
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
            if (indexMapping.containsValue(id)) {
                BidiMap<String, Integer> xmlIdToIndex = indexMapping.inverseBidiMap();
                key = xmlIdToIndex.get(id);
            } else {
                key = addMapIdToIndex(id);
            }

            return key;
        }
        return key;
    }



}


