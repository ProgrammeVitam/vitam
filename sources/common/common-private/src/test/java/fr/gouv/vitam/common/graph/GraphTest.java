/*******************************************************************************
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
 *******************************************************************************/

package fr.gouv.vitam.common.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;

public class GraphTest {



    @Test
    public void test_graph() throws Exception {

        File file = PropertiesUtils.getResourceFile("ingest_tree.json");
        JsonNode json = JsonHandler.getFromFile(file);
        Graph g = new Graph(json);
        assertNotNull(g.getGraphWithLongestPaths());
        assertEquals(g.getGraphWithLongestPaths().toString(),
            "{0=[ID027], 1=[ID030, ID031, ID028], 2=[ID029], 3=[ID032]}");
    }


    @Test
    public void test_graph_cyc() throws Exception {

        File file = PropertiesUtils.getResourceFile("ingest_cyc_2.json");
        JsonNode json = JsonHandler.getFromFile(file);
        Graph g = new Graph(json);
        assertNotNull(g.getGraphWithLongestPaths());

    }
}
