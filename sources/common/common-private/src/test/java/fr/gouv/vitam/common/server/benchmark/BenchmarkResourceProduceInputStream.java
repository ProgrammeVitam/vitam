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
package fr.gouv.vitam.common.server.benchmark;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;

/**
 * Benchmark Resource for output InputStream
 */
@Path("/benchmark")
@javax.ws.rs.ApplicationPath("webresources")
public class BenchmarkResourceProduceInputStream extends ApplicationStatusResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BenchmarkResourceProduceInputStream.class);

    public static final String DOWNLOAD = "/download";
    public static final String DOWNLOAD_INDIRECT = "/downloadindirect";
    public static long size = 10000000L;

    /**
     * Constructor IngestExternalResource
     *
     * @param ingestExternalConfiguration
     */
    public BenchmarkResourceProduceInputStream(BenchmarkConfiguration configuration) {
        LOGGER.info("init Ingest External Resource server");
    }

    /**
     * Return a response status using Head
     *
     * @return Response
     */
    @Path(STATUS_URL + HttpMethod.HEAD)
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    public Response statusHead() {
        return Response.status(Status.NO_CONTENT).build();
    }

    /**
     * Return a response status using Head
     *
     * @return Response
     */
    @Path(STATUS_URL + HttpMethod.OPTIONS)
    @OPTIONS
    @Produces(MediaType.APPLICATION_JSON)
    public Response statusOptions() {
        return Response.status(Status.NO_CONTENT).build();
    }

    private void buildReponse(AsyncResponse asyncResponse) {
        new AsyncInputStreamHelper(asyncResponse, new FakeInputStream(size, true))
            .writeResponse(Response.ok());
    }

    private void buildReponse(AsyncResponse asyncResponse, Response response) {
        new AsyncInputStreamHelper(asyncResponse, response)
            .writeResponse(Response.ok());
    }

    /**
     * Download using POST
     *
     * @param asyncResponse AsyncResponse model
     */
    @Path(DOWNLOAD + HttpMethod.POST)
    @POST
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.WILDCARD)
    public void downloadPost(@Suspended final AsyncResponse asyncResponse) {
        LOGGER.warn("Start: " + StringUtils.getClassName(Thread.currentThread()));
        VitamThreadPoolExecutor.getDefaultExecutor().execute(new Runnable() {

            @Override
            public void run() {
                LOGGER.warn("Start: " + StringUtils.getClassName(Thread.currentThread()));
                buildReponse(asyncResponse);
            }
        });
    }

    /**
     * Download using PUT
     *
     * @param asyncResponse AsyncResponse model
     */
    @Path(DOWNLOAD + HttpMethod.PUT)
    @PUT
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.WILDCARD)
    public void downloadPut(@Suspended final AsyncResponse asyncResponse) {
        LOGGER.warn("Start: " + StringUtils.getClassName(Thread.currentThread()));
        VitamThreadPoolExecutor.getDefaultExecutor().execute(new Runnable() {

            @Override
            public void run() {
                LOGGER.warn("Start: " + StringUtils.getClassName(Thread.currentThread()));
                buildReponse(asyncResponse);
            }
        });
    }

    /**
     * Download using GET
     *
     * @param asyncResponse AsyncResponse model
     */
    @Path(DOWNLOAD + HttpMethod.GET)
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.WILDCARD)
    public void downloadGet(@Suspended final AsyncResponse asyncResponse) {
        LOGGER.warn("Start: " + StringUtils.getClassName(Thread.currentThread()));
        VitamThreadPoolExecutor.getDefaultExecutor().execute(new Runnable() {

            @Override
            public void run() {
                LOGGER.warn("Start: " + StringUtils.getClassName(Thread.currentThread()));
                buildReponse(asyncResponse);
            }
        });
    }

    /**
     * Download indirect using POST
     *
     * @param asyncResponse AsyncResponse model
     * @throws VitamClientInternalException
     */
    @Path(DOWNLOAD_INDIRECT + HttpMethod.POST)
    @POST
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.WILDCARD)
    public void downloadPostIndirect(@Suspended final AsyncResponse asyncResponse) throws VitamClientInternalException {
        LOGGER.warn("Start: " + StringUtils.getClassName(Thread.currentThread()));
        VitamThreadPoolExecutor.getDefaultExecutor().execute(new Runnable() {

            @Override
            public void run() {
                LOGGER.warn("Start: " + StringUtils.getClassName(Thread.currentThread()));
                final String method = HttpMethod.POST;
                Response response = null;
                try (final BenchmarkClientRest client =
                    BenchmarkClientFactory.getInstance().getClient()) {
                    response = client.performRequest(method, BenchmarkResourceProduceInputStream.DOWNLOAD + method,
                        null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
                    buildReponse(asyncResponse, response);
                } catch (final VitamClientInternalException e) {
                    AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                        Response.status(Status.INTERNAL_SERVER_ERROR).build());
                }
            }
        });
    }

    /**
     * Download indirect using PUT
     *
     * @param asyncResponse AsyncResponse model
     * @throws VitamClientInternalException
     */
    @Path(DOWNLOAD_INDIRECT + HttpMethod.PUT)
    @PUT
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.WILDCARD)
    public void downloadPutIndirect(@Suspended final AsyncResponse asyncResponse) throws VitamClientInternalException {
        LOGGER.warn("Start: " + StringUtils.getClassName(Thread.currentThread()));
        VitamThreadPoolExecutor.getDefaultExecutor().execute(new Runnable() {

            @Override
            public void run() {
                LOGGER.warn("Start: " + StringUtils.getClassName(Thread.currentThread()));
                final String method = HttpMethod.PUT;
                Response response = null;
                try (final BenchmarkClientRest client =
                    BenchmarkClientFactory.getInstance().getClient()) {
                    response = client.performRequest(method, BenchmarkResourceProduceInputStream.DOWNLOAD + method,
                        null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
                    buildReponse(asyncResponse, response);
                } catch (final VitamClientInternalException e) {
                    AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                        Response.status(Status.INTERNAL_SERVER_ERROR).build());
                }
            }
        });
    }

    /**
     * Download indirect using GET
     *
     * @param asyncResponse AsyncResponse model
     * @throws VitamClientInternalException
     */
    @Path(DOWNLOAD_INDIRECT + HttpMethod.GET)
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.WILDCARD)
    public void downloadGetIndirect(@Suspended final AsyncResponse asyncResponse) throws VitamClientInternalException {
        LOGGER.warn("Start: " + StringUtils.getClassName(Thread.currentThread()));
        VitamThreadPoolExecutor.getDefaultExecutor().execute(new Runnable() {

            @Override
            public void run() {
                LOGGER.warn("Start: " + StringUtils.getClassName(Thread.currentThread()));
                final String method = HttpMethod.GET;
                Response response = null;
                try (final BenchmarkClientRest client =
                    BenchmarkClientFactory.getInstance().getClient()) {
                    response = client.performRequest(method, BenchmarkResourceProduceInputStream.DOWNLOAD + method,
                        null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
                    buildReponse(asyncResponse, response);
                } catch (final VitamClientInternalException e) {
                    AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                        Response.status(Status.INTERNAL_SERVER_ERROR).build());
                }
            }
        });
    }
}
