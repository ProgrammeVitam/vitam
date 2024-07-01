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
package fr.gouv.vitam.common.client;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;

public class VitamRequestBuilder {

    private static final Runnable NOOP = () -> {};

    private final String httpMethod;

    private String path;
    private String baseUrl;
    private MediaType contentType;
    private MediaType accept;
    private Object body;
    private MultivaluedMap<String, Object> headers;
    private Map<String, String> queryParams;

    private Runnable beforeExecRequest = NOOP;
    private boolean chunckedMode = false;

    private VitamRequestBuilder(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public static VitamRequestBuilder get() {
        return new VitamRequestBuilder(GET);
    }

    public static VitamRequestBuilder post() {
        return new VitamRequestBuilder(POST);
    }

    public static VitamRequestBuilder put() {
        return new VitamRequestBuilder(PUT);
    }

    public static VitamRequestBuilder delete() {
        return new VitamRequestBuilder(DELETE);
    }

    public static VitamRequestBuilder head() {
        return new VitamRequestBuilder(HEAD);
    }

    public static VitamRequestBuilder options() {
        return new VitamRequestBuilder(OPTIONS);
    }

    public void runBeforeExecRequest() {
        beforeExecRequest.run();
    }

    public VitamRequestBuilder withBaseUrl(String url) {
        this.baseUrl = Objects.requireNonNull(url);
        return this;
    }

    public VitamRequestBuilder withNoBaseUrl() {
        this.baseUrl = null;
        return this;
    }

    public VitamRequestBuilder withJson() {
        this.contentType = APPLICATION_JSON_TYPE;
        this.accept = APPLICATION_JSON_TYPE;
        return this;
    }

    public VitamRequestBuilder withOctet() {
        this.contentType = APPLICATION_OCTET_STREAM_TYPE;
        this.accept = APPLICATION_OCTET_STREAM_TYPE;
        return this;
    }

    public VitamRequestBuilder withJsonOctet() {
        this.contentType = APPLICATION_JSON_TYPE;
        this.accept = APPLICATION_OCTET_STREAM_TYPE;
        return this;
    }

    public VitamRequestBuilder withJsonContentType() {
        this.contentType = APPLICATION_JSON_TYPE;
        return this;
    }

    public VitamRequestBuilder withXMLContentType() {
        this.contentType = APPLICATION_XML_TYPE;
        return this;
    }

    public VitamRequestBuilder withContentType(MediaType contentType) {
        this.contentType = Objects.requireNonNull(contentType, "cannot set a null ContentType");
        return this;
    }

    public VitamRequestBuilder withOctetContentType() {
        this.contentType = APPLICATION_OCTET_STREAM_TYPE;
        return this;
    }

    public VitamRequestBuilder withNoContentType() {
        this.contentType = null;
        return this;
    }

    public VitamRequestBuilder withJsonAccept() {
        this.accept = APPLICATION_JSON_TYPE;
        return this;
    }

    public VitamRequestBuilder withOctetAccept() {
        this.accept = APPLICATION_OCTET_STREAM_TYPE;
        return this;
    }

    public VitamRequestBuilder withXMLAccept() {
        this.accept = APPLICATION_XML_TYPE;
        return this;
    }

    public VitamRequestBuilder withAccept(MediaType accept) {
        this.accept = Objects.requireNonNull(accept);
        return this;
    }

    public VitamRequestBuilder withBody(Object body, String failCheckBodyMessage) {
        if (Objects.isNull(body)) {
            throw new IllegalArgumentException(failCheckBodyMessage);
        }
        this.body = body;
        return this;
    }

    public VitamRequestBuilder withBody(Object body) {
        this.body = Objects.requireNonNull(body, "Body cannot be null.");
        return this;
    }

    public VitamRequestBuilder withBefore(Runnable beforeExecRequest) {
        this.beforeExecRequest = Objects.requireNonNull(beforeExecRequest);
        return this;
    }

    public VitamRequestBuilder withHeaders(MultivaluedMap<String, Object> headers) {
        this.headers = Objects.requireNonNull(headers);
        return this;
    }

    public VitamRequestBuilder withHeader(String key, Object value) {
        if (this.headers == null) {
            this.headers = new MultivaluedHashMap<>();
        }
        this.headers.add(Objects.requireNonNull(key), Objects.requireNonNull(value));
        return this;
    }

    public VitamRequestBuilder withHeaderReplaceExisting(String key, Object value) {
        if (value == null) {
            return this;
        }
        if (this.headers == null) {
            this.headers = new MultivaluedHashMap<>();
        }
        this.headers.putSingle(Objects.requireNonNull(key), value);
        return this;
    }

    public VitamRequestBuilder withHeaderIgnoreNull(String key, Object value) {
        if (value == null) {
            return this;
        }
        if (this.headers == null) {
            this.headers = new MultivaluedHashMap<>();
        }
        this.headers.add(Objects.requireNonNull(key), value);
        return this;
    }

    public VitamRequestBuilder withPath(String path) {
        this.path = Objects.requireNonNull(path);
        return this;
    }

    public VitamRequestBuilder withQueryParams(Map<String, String> queryParams) {
        if (!GET.equals(httpMethod)) {
            throw new IllegalArgumentException(
                String.format("Cannot use query params with something different that 'GET', here '%s'.", httpMethod)
            );
        }
        this.queryParams = Objects.requireNonNull(queryParams);
        return this;
    }

    public VitamRequestBuilder withQueryParam(String key, String value) {
        if (!GET.equals(httpMethod)) {
            throw new IllegalArgumentException(
                String.format("Cannot use query params with something different that 'GET', here '%s'.", httpMethod)
            );
        }
        if (this.queryParams == null) {
            this.queryParams = new HashMap<>();
        }
        this.queryParams.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
        return this;
    }

    public VitamRequestBuilder withChunckedMode(boolean chunckedMode) {
        this.chunckedMode = chunckedMode;
        return this;
    }

    public Map<String, String> getQueryParams() {
        if (Objects.isNull(queryParams)) {
            return Collections.emptyMap();
        }
        return queryParams;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public MediaType getContentType() {
        return contentType;
    }

    public MediaType getAccept() {
        return accept;
    }

    public Object getBody() {
        return body;
    }

    public MultivaluedMap<String, Object> getHeaders() {
        if (Objects.isNull(headers)) {
            return new MultivaluedHashMap<>(Collections.emptyMap());
        }
        return headers;
    }

    public String getPath() {
        return path;
    }

    public boolean isChunckedMode() {
        return chunckedMode;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
