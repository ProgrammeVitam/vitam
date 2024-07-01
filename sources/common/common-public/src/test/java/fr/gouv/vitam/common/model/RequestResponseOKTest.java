/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.util.Objects.requireNonNullElse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestResponseOKTest {

    private JsonNode query;

    private static final String ERROR_JSON =
        "{\"httpCode\":400,\"code\":\"0\",\"context\":\"context\",\"state\":\"state\"," +
        "\"message\":\"message\",\"description\":\"description\",\"errors\":" +
        "[{\"httpCode\":0,\"code\":\"1\"}]}";

    private static final String OK_JSON =
        "{\"httpCode\":200,\"$hits\":{\"total\":0,\"offset\":0,\"limit\":0,\"size\":0}," +
        "\"$results\":[],\"$facetResults\":[],\"$context\":{\"Objects\":[\"One\",\"Two\",\"Three\"]}}";

    private static final String OK_JSON_FACET =
        "{\"httpCode\":200,\"$hits\":{\"total\":0,\"offset\":0,\"limit\":0,\"size\":0}," +
        "\"$results\":[],\"$facetResults\":[{\"name\":\"mgt_facet\",\"buckets\":[{\"value\":\"str0\",\"count\":1}]}],\"$context\":{\"Objects\":[\"One\",\"Two\",\"Three\"]}}";

    @Test
    public final void testRequestResponseOKConstructor() {
        final RequestResponseOK<JsonNode> requestResponseOK = new RequestResponseOK<>();
        assertThat(requestResponseOK.getQuery()).isEmpty();
        assertThat(requestResponseOK.getResults()).isNullOrEmpty();
    }

    @Test
    public final void testSetRequestResponseOKAttributes() throws IOException {
        ObjectTest objectTest = new ObjectTest();
        objectTest.addResult("One");
        objectTest.addResult("Two");
        objectTest.addResult("Three");

        FacetBucket bucket = new FacetBucket("str0", 1);

        final String json = "{\"Objects\" : [\"One\", \"Two\", \"Three\"]}";
        query = new ObjectMapper().readTree(json);
        final RequestResponseOK<ObjectTest> requestResponseOK = new RequestResponseOK<>(query);
        requestResponseOK.addAllResults(new ArrayList<>());
        requestResponseOK.addAllFacetResults(
            Collections.singletonList(new FacetResult("mgt_facet", Collections.singletonList(bucket)))
        );
        requestResponseOK.setHttpCode(Status.OK.getStatusCode());
        assertThat(requestResponseOK.getQuery()).isNotEmpty();
        assertThat(requestResponseOK.getResults()).isNotNull().isEmpty();

        assertEquals(OK_JSON_FACET, JsonHandler.unprettyPrint(requestResponseOK));
        try {
            final RequestResponseOK<ObjectTest> copy = JsonHandler.getFromStringAsTypeReference(
                JsonHandler.unprettyPrint(requestResponseOK),
                new TypeReference<>() {}
            );
            assertEquals(requestResponseOK.getQuery(), copy.getQuery());
        } catch (final InvalidParseOperationException e) {
            fail("should not failed");
        }
        requestResponseOK.addResult(objectTest);
        requestResponseOK.addResult(objectTest);
        requestResponseOK.setHits(2, 0, 2, 2);
        assertEquals(
            "{\"httpCode\":200,\"$hits\":{\"total\":2,\"offset\":0,\"limit\":2,\"size\":2}," +
            "\"$results\":[{\"Objects\":[\"One\",\"Two\",\"Three\"]},{\"Objects\":[\"One\",\"Two\",\"Three\"]}]," +
            "\"$facetResults\":[{\"name\":\"mgt_facet\",\"buckets\":[{\"value\":\"str0\",\"count\":1}]}]," +
            "\"$context\":{\"Objects\":[\"One\",\"Two\",\"Three\"]}}",
            JsonHandler.unprettyPrint(requestResponseOK)
        );
        requestResponseOK.setHits(2, 0, 4, 2);
        assertEquals(
            "{\"httpCode\":200,\"$hits\":{\"total\":2,\"offset\":0,\"limit\":4,\"size\":2}," +
            "\"$results\":[{\"Objects\":[\"One\",\"Two\",\"Three\"]},{\"Objects\":[\"One\",\"Two\",\"Three\"]}]," +
            "\"$facetResults\":[{\"name\":\"mgt_facet\",\"buckets\":[{\"value\":\"str0\",\"count\":1}]}]," +
            "\"$context\":{\"Objects\":[\"One\",\"Two\",\"Three\"]}}",
            JsonHandler.unprettyPrint(requestResponseOK)
        );
        try {
            final RequestResponseOK<ObjectTest> copy = JsonHandler.getFromStringAsTypeReference(
                JsonHandler.unprettyPrint(requestResponseOK),
                new TypeReference<>() {}
            );
            assertEquals(requestResponseOK.getQuery(), copy.getQuery());
        } catch (final InvalidParseOperationException e) {
            fail("should not failed");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public final void testRequestResponseOKAddNull() {
        final RequestResponseOK<JsonNode> requestResponseOK = new RequestResponseOK<>();
        requestResponseOK.addAllResults(null);
    }

    @Test
    public void testOtherPojo() {
        StatusCode code = StatusCode.FATAL;
        assertTrue(code.isGreaterOrEqualToFatal());
        assertTrue(code.isGreaterOrEqualToKo());
        code = StatusCode.KO;
        assertFalse(code.isGreaterOrEqualToFatal());
        assertTrue(code.isGreaterOrEqualToKo());
        code = StatusCode.WARNING;
        assertFalse(code.isGreaterOrEqualToFatal());
        assertFalse(code.isGreaterOrEqualToKo());
    }

    @Test
    public void testFromResponse() throws InvalidParseOperationException, InvalidFormatException {
        ObjectTest objectTest = new ObjectTest();
        objectTest.addResult("One");
        objectTest.addResult("Two");
        objectTest.addResult("Three");

        final String json = "{\"Objects\" : [\"One\", \"Two\", \"Three\"]}";
        query = JsonHandler.getFromString(json);

        final RequestResponseOK<ObjectTest> requestResponseOK = new RequestResponseOK<>();
        requestResponseOK.setQuery(query);
        requestResponseOK.addAllResults(Lists.newArrayList());

        Response response = getOutboundResponse(
            Status.OK,
            requestResponseOK.toString(),
            MediaType.APPLICATION_JSON,
            null
        );
        RequestResponse<JsonNode> requestResponse = RequestResponse.parseFromResponse(response);
        assertEquals(OK_JSON, JsonHandler.unprettyPrint(requestResponse));
        assertTrue(requestResponse.isOk());
        response = getOutboundResponse(Status.OK, requestResponseOK.toString(), MediaType.APPLICATION_JSON, null);
        requestResponse = RequestResponse.parseRequestResponseOk(response);
        assertEquals(OK_JSON, JsonHandler.unprettyPrint(requestResponse));

        final VitamError<JsonNode> error = new VitamError<>("0");
        error.setMessage("message");
        error.setDescription("description");
        error.setState("state");
        error.setContext("context");
        error.addAllErrors(Collections.singletonList(new VitamError<>("1")));
        response = getOutboundResponse(Status.BAD_REQUEST, error.toString(), MediaType.APPLICATION_JSON, null);
        requestResponse = RequestResponse.parseFromResponse(response);
        assertEquals(ERROR_JSON, JsonHandler.unprettyPrint(requestResponse));
        assertFalse(requestResponse.isOk());
        response = getOutboundResponse(Status.BAD_REQUEST, error.toString(), MediaType.APPLICATION_JSON, null);
        requestResponse = RequestResponse.parseVitamError(response);
        assertEquals(ERROR_JSON, JsonHandler.unprettyPrint(requestResponse));

        response = getOutboundResponse(Status.BAD_GATEWAY, null, MediaType.APPLICATION_JSON, null);
        requestResponse = RequestResponse.parseFromResponse(response);
        assertTrue(requestResponse instanceof VitamError);
        assertEquals(Status.BAD_GATEWAY.getStatusCode(), requestResponse.getHttpCode());
        assertEquals("", ((VitamError<JsonNode>) requestResponse).getCode());
        assertFalse(requestResponse.isOk());

        // Bad response
        response = getOutboundResponse(Status.BAD_GATEWAY, "{ \"notcorrect\": 1}", MediaType.APPLICATION_JSON, null);
        try {
            requestResponse = RequestResponse.parseFromResponse(response);
            System.err.println(requestResponse.toString());
            fail("Should raized an exception");
        } catch (final IllegalStateException e) {
            // Correct
        }
    }

    public static Response getOutboundResponse(
        Status status,
        Object entity,
        String contentType,
        Map<String, String> headers
    ) {
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        final Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status.getStatusCode());
        when(response.readEntity(ArgumentMatchers.<Class<Object>>any())).thenReturn(requireNonNullElse(entity, ""));
        boolean contentTypeFound = false;
        if (!Strings.isNullOrEmpty(contentType)) {
            when(response.getHeaderString(HttpHeaders.CONTENT_TYPE)).thenReturn(contentType);
            contentTypeFound = true;
        }
        if (headers != null) {
            for (final Entry<String, String> entry : headers.entrySet()) {
                when(response.getHeaderString(entry.getKey())).thenReturn(entry.getValue());
            }
        }
        if (!contentTypeFound) {
            when(response.getHeaderString(HttpHeaders.CONTENT_TYPE)).thenReturn(MediaType.APPLICATION_JSON);
        }
        return response;
    }

    private static class ObjectTest {

        @JsonProperty("Objects")
        private List<String> results = new ArrayList<>();

        public List<String> getResults() {
            return results;
        }

        public void setResults(List<String> results) {
            this.results = results;
        }

        public void addResult(String s) {
            results.add(s);
        }
    }
}
