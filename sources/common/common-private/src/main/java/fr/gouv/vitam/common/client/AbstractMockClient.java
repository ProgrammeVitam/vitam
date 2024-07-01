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
package fr.gouv.vitam.common.client;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Abstract Mock common client
 */
public class AbstractMockClient implements MockOrRestClient {

    @Override
    public void checkStatus() {
        // Nothing to do
    }

    @Override
    public void checkStatus(MultivaluedHashMap<String, Object> headers) throws VitamApplicationServerException {}

    @Override
    public void close() {
        // Nothing to do
    }

    @Override
    public String getResourcePath() {
        return "/";
    }

    @Override
    public String getServiceUrl() {
        return "http://localhost:8080";
    }

    @Override
    public void consumeAnyEntityAndClose(Response response) {
        // Nothing to do
    }

    /**
     * Fake Inbound Response for Mock client support
     */
    public static class FakeInboundResponse extends Response {

        private final Response response;

        /**
         * @param status
         * @param entity
         * @param mediaType
         * @param headers
         */
        public FakeInboundResponse(
            Status status,
            Object entity,
            MediaType mediaType,
            MultivaluedHashMap<String, Object> headers
        ) {
            final ResponseBuilder builder = Response.status(status);
            if (entity != null) {
                builder.entity(entity);
                if (mediaType != null) {
                    builder.type(mediaType);
                }
            }
            if (headers != null) {
                for (final Entry<String, List<Object>> entry : headers.entrySet()) {
                    for (final Object value : entry.getValue()) {
                        builder.header(entry.getKey(), value);
                    }
                }
            }
            response = builder.build();
        }

        @Override
        public int getStatus() {
            return response.getStatus();
        }

        @Override
        public StatusType getStatusInfo() {
            return response.getStatusInfo();
        }

        @Override
        public Object getEntity() {
            return response.getEntity();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T readEntity(Class<T> entityType) {
            return (T) response.getEntity();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T readEntity(GenericType<T> entityType) {
            return (T) response.getEntity();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
            return (T) response.getEntity();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
            return (T) response.getEntity();
        }

        @Override
        public boolean hasEntity() {
            return response.hasEntity();
        }

        @Override
        public boolean bufferEntity() {
            return response.bufferEntity();
        }

        @Override
        public void close() {
            response.close();
        }

        @Override
        public MediaType getMediaType() {
            return response.getMediaType();
        }

        @Override
        public Locale getLanguage() {
            return response.getLanguage();
        }

        @Override
        public int getLength() {
            return response.getLength();
        }

        @Override
        public Set<String> getAllowedMethods() {
            return response.getAllowedMethods();
        }

        @Override
        public Map<String, NewCookie> getCookies() {
            return response.getCookies();
        }

        @Override
        public EntityTag getEntityTag() {
            return response.getEntityTag();
        }

        @Override
        public Date getDate() {
            return response.getDate();
        }

        @Override
        public Date getLastModified() {
            return response.getLastModified();
        }

        @Override
        public URI getLocation() {
            return response.getLocation();
        }

        @Override
        public Set<Link> getLinks() {
            return response.getLinks();
        }

        @Override
        public boolean hasLink(String relation) {
            return response.hasLink(relation);
        }

        @Override
        public Link getLink(String relation) {
            return response.getLink(relation);
        }

        @Override
        public Builder getLinkBuilder(String relation) {
            return response.getLinkBuilder(relation);
        }

        @Override
        public MultivaluedMap<String, Object> getMetadata() {
            return response.getMetadata();
        }

        @Override
        public MultivaluedMap<String, String> getStringHeaders() {
            return response.getStringHeaders();
        }

        @Override
        public String getHeaderString(String name) {
            return response.getHeaderString(name);
        }
    }
}
