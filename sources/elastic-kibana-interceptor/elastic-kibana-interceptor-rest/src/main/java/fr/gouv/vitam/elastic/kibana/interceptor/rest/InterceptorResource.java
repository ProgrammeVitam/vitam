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
package fr.gouv.vitam.elastic.kibana.interceptor.rest;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * InterceptorResource : intercept request between elastic and kibana replace underscore by sharp and suppress
 * undesired header
 */
@Path("/")
public class InterceptorResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(InterceptorResource.class);
    private static final String UTF_8 = "UTF-8";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String KEEP_ALIVE = "keep-alive";
    private static final String HOST = "Host";
    private static final String CONNECTION = "Connection";
    private static final String TRANSFER_ENCODING = "Transfer-encoding";
    private InterceptorConfiguration interceptorConfiguration;

    /**
     * Constructor
     */
    public InterceptorResource(InterceptorConfiguration interceptorConfiguration) {
        LOGGER.info("InterceptorResource initialized");
        this.interceptorConfiguration = interceptorConfiguration;
    }

    /**
     * Juste Filter the request not the response
     *
     * @param url url
     * @param info UriInfo
     * @param req HttpServletRequest
     * @param headers HttpHeaders
     * @return elasticSearch response.
     */
    @HEAD
    @Path("{url: .*}")
    public Response handleHeadElasticRequestFromKibana(
        @Context UriInfo info,
        @PathParam("url") String url,
        @Context HttpServletRequest req,
        @Context HttpHeaders headers
    ) {
        LOGGER.debug("Head sur " + req.getRequestURL());

        String urlEs = getUrlEs(req);
        ResteasyWebTarget target = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).build().target(urlEs);

        // Add Query Params to the request
        for (Map.Entry<String, List<String>> entry : info.getQueryParameters().entrySet()) {
            for (String value : entry.getValue()) {
                target = target.queryParam(entry.getKey(), value);
            }
        }

        Invocation.Builder request = target.request();
        // Set Header and Suppress undesired filter from the request
        setHeaderAndSuppressUndesiredOne(headers, request);
        return request.build(HttpMethod.HEAD).invoke();
    }

    private void setHeaderAndSuppressUndesiredOne(@Context HttpHeaders headers, Invocation.Builder request) {
        headers
            .getRequestHeaders()
            .forEach((key, value1) -> {
                LOGGER.debug("Header key " + key + "Header value " + value1);
                for (String value : value1) {
                    if (
                        !CONTENT_LENGTH.equals(key) &&
                        !KEEP_ALIVE.equals(key) &&
                        !HOST.equals(key) &&
                        !CONNECTION.equals(key) &&
                        !TRANSFER_ENCODING.equalsIgnoreCase(key)
                    ) {
                        request.header(key, value);
                    }
                }
            });
    }

    private String getUrlEs(@Context HttpServletRequest req) {
        StringBuilder urlEs = new StringBuilder("http://")
            .append(interceptorConfiguration.getElasticsearchNodes().get(0).getHostName())
            .append(":")
            .append(interceptorConfiguration.getElasticsearchNodes().get(0).getHttpPort())
            .append(req.getRequestURI());
        LOGGER.debug("urlEs " + urlEs);
        return urlEs.toString();
    }

    /**
     * Filter kibana request and response
     *
     * @param url url
     * @param info UriInfo
     * @param req HttpServletRequest
     * @param headers HttpHeaders
     * @return the given elasticsearch Response filtered with sharp
     * @throws IOException IOException
     */
    @GET
    @POST
    @OPTIONS
    @DELETE
    @PUT
    @Path("{url: .*}")
    public Response process(
        @PathParam("url") String url,
        @Context UriInfo info,
        @Context HttpServletRequest req,
        @Context HttpHeaders headers
    ) throws IOException {
        LOGGER.debug(req.getMethod() + " sur " + req.getRequestURL());
        ReplacePatternUtils replacePatternUtils = new ReplacePatternUtils(interceptorConfiguration.getWhitelist());
        String urlEs = getUrlEs(req);
        ResteasyWebTarget target = ((ResteasyClientBuilder) ClientBuilder.newBuilder()).build().target(urlEs);
        // Query Params
        for (Map.Entry<String, List<String>> entry : info.getQueryParameters().entrySet()) {
            for (String value : entry.getValue()) {
                target = target.queryParam(entry.getKey(), value);
            }
        }

        Invocation.Builder request = target.request();
        //Set Header and Suppress undesired filter from the request
        setHeaderAndSuppressUndesiredOne(headers, request);

        Response response;
        if (headers.getMediaType() != null) {
            ServletInputStream inputStream = req.getInputStream();
            String requestBodyWithoutSharp = replacePatternUtils.replaceSharpByUnderscore(
                IOUtils.toString(inputStream, UTF_8)
            );
            response = request
                .build(
                    req.getMethod(),
                    Entity.entity(IOUtils.toInputStream(requestBodyWithoutSharp, UTF_8), headers.getMediaType())
                )
                .invoke();
        } else {
            response = request.build(req.getMethod()).invoke();
        }

        Map<String, Object> responseHeader = new HashMap<>();
        response
            .getHeaders()
            .forEach((key, value1) -> {
                for (Object value : value1) {
                    if (
                        !CONTENT_LENGTH.equalsIgnoreCase(key) &&
                        !KEEP_ALIVE.equalsIgnoreCase(key) &&
                        !HOST.equalsIgnoreCase(key) &&
                        !CONNECTION.equalsIgnoreCase(key) &&
                        !TRANSFER_ENCODING.equalsIgnoreCase(key)
                    ) {
                        responseHeader.put(key, value);
                    }
                }
            });
        String entity = response.readEntity(String.class);
        Response.ResponseBuilder responseWithoutHeader;
        responseWithoutHeader = Response.status(response.getStatus()).entity(
            replacePatternUtils.replaceUnderscoreBySharp(entity)
        );
        for (String key : responseHeader.keySet()) {
            responseWithoutHeader.header(key, responseHeader.get(key));
        }
        return responseWithoutHeader.build();
    }
}
