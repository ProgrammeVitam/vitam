/*******************************************************************************
 * This file is part of Vitam Project.
 * <p>
 * Copyright Vitam (2012, 2015)
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.common.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RequestResponseErrorTest {

    private JsonNode query;
    private VitamError error;

    private static final String JSON_REQUEST_RESPONSE_ERROR =
        "{\"Code\":0,\"Context\":\"\",\"State\":\"\",\"Message\":\"\",\"Description\":\"\"," +
            "\"Errors\":[{\"Code\":1,\"Context\":\"\",\"State\":\"\",\"Message\":\"\",\"Description\":\"\"}]}";

    @Test
    public final void testRequestResponseErrorConstructor() {
        final RequestResponseError requestResponseError = new RequestResponseError();
        assertThat(requestResponseError.getError()).isNotNull();
        assertThat(requestResponseError.getQuery()).isEmpty();
        assertEquals("", requestResponseError.toString());
    }

    @Test
    public final void testSetRequestResponseErrorAttributes()
        throws JsonProcessingException, IOException {
        final String json = "{\"objects\" : [\"One\", \"Two\", \"Three\"]}";
        query = new ObjectMapper().readTree(json).get("objects");
        error = new VitamError(0);
        final RequestResponseError requestResponseError = new RequestResponseError();
        requestResponseError.setQuery(query);
        requestResponseError.setError(error);
        assertThat(requestResponseError.getQuery()).isNotEmpty();
        assertThat(requestResponseError.getError()).isNotNull();
    }

    @Test
    public void testRequestResponseErrorString() throws Exception {
        final String json = "{\"objects\" : [\"One\", \"Two\", \"Three\"]}";
        query = new ObjectMapper().readTree(json).get("objects");
        error = new VitamError(0);
        final RequestResponseError requestResponseError = new RequestResponseError();
        requestResponseError.setQuery(query);
        requestResponseError.setError(error);
        error.addAllErrors(Collections.singletonList(new VitamError(1)));
        assertEquals(JSON_REQUEST_RESPONSE_ERROR, requestResponseError.toString());
    }

}
