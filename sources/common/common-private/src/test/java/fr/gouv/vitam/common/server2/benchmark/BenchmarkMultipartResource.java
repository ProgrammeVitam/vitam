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
package fr.gouv.vitam.common.server2.benchmark;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server2.application.resources.ApplicationStatusResource;

/**
 * Benchmark Resource
 */
@Path("/benchmark")
@javax.ws.rs.ApplicationPath("webresources")
public class BenchmarkMultipartResource extends ApplicationStatusResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BenchmarkMultipartResource.class);

    /**
     * Constructor IngestExternalResource
     *
     * @param ingestExternalConfiguration
     */
    public BenchmarkMultipartResource(BenchmarkConfiguration configuration) {
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

    /**
     * upload using POST
     *
     * @param stream data input stream
     * @param header method for entry data
     * @return Response
     */
    @Path("upload" + HttpMethod.POST)
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.WILDCARD)
    public long uploadPost(InputStream stream) {
        return JunitHelper.consumeInputStream(stream);
    }

    /**
     * upload using PUT
     *
     * @param stream data input stream
     * @param header method for entry data
     * @return Response
     */
    @Path("upload" + HttpMethod.PUT)
    @PUT
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.WILDCARD)
    public long uploadPut(InputStream stream) {
        return JunitHelper.consumeInputStream(stream);
    }

    /**
     * upload using GET
     *
     * @param stream data input stream
     * @param header method for entry data
     * @return Response
     */
    @Path("upload" + HttpMethod.GET)
    @GET
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.WILDCARD)
    public long uploadGet(InputStream stream) {
        return JunitHelper.consumeInputStream(stream);
    }

    /**
     * upload using DELETE
     *
     * @param stream data input stream
     * @param header method for entry data
     * @return Response
     */
    @Path("upload" + HttpMethod.DELETE)
    @DELETE
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.WILDCARD)
    public long uploadDelete(InputStream stream) {
        return JunitHelper.consumeInputStream(stream);
    }

    /**
     * multipart using POST
     * 
     * @param sipDisposition
     * @param stream data input stream
     * @param check
     * @return Response
     */
    @Path("multipart" + HttpMethod.POST)
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.MULTIPART_FORM_DATA)
    public MultiPart multipartPost(@FormDataParam("sip") FormDataContentDisposition sipDisposition,
        @FormDataParam("sip") InputStream stream, @FormDataParam("check") String check) {
        final long size = JunitHelper.consumeInputStream(stream);
        final FakeInputStream inputStream = new FakeInputStream(size, true);
        final MultiPart multiPart = new MultiPart();
        multiPart
            .bodyPart(new StreamDataBodyPart("sipback", inputStream, sipDisposition.getFileName()));
        return multiPart;
    }
}
