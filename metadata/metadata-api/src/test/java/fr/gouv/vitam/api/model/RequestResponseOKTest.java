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
package fr.gouv.vitam.api.model;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.SingletonUtils;
import fr.gouv.vitam.common.json.JsonHandler;

public class RequestResponseOKTest {

    private static final int TOTAL=1;
    private static final int OFFSET=2;
    private static final int LIMIT=3;
    private static final String RESPONSE ="response";
    
    @Test
    public void testRequestResponseOK() throws Exception {
        RequestResponseOK response = new RequestResponseOK();
        assertEquals(0, response.getHits().getTotal());
        assertEquals(0, response.getHits().getOffset());
        assertEquals(0, response.getHits().getLimit());
        assertEquals(SingletonUtils.singletonList(), response.getResults());
        assertEquals(JsonHandler.createObjectNode(), response.getQuery());
       
        response.setHits(new DatabaseCursor(TOTAL, OFFSET, LIMIT));
        assertEquals(TOTAL, response.getHits().getTotal());
        assertEquals(OFFSET, response.getHits().getOffset());
        assertEquals(LIMIT, response.getHits().getLimit());
        
        response.setHits(TOTAL, OFFSET, LIMIT);
        assertEquals(TOTAL, response.getHits().getTotal());
        assertEquals(OFFSET, response.getHits().getOffset());
        assertEquals(LIMIT, response.getHits().getLimit());
     
        List<String> list = new ArrayList<>();
        list.add(RESPONSE);
        response.setResults(list);
        assertEquals(1, response.getResults().size());
        
        JsonNode query = JsonHandler.getFromString("{}");
        response.setQuery(query);
        assertEquals(query, response.getQuery());
        
    }

}
