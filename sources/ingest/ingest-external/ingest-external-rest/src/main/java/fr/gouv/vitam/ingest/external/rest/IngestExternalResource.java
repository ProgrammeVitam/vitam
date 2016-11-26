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
package fr.gouv.vitam.ingest.external.rest;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.ingest.external.api.IngestExternalException;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import fr.gouv.vitam.ingest.external.core.AtrKoBuilder;
import fr.gouv.vitam.ingest.external.core.IngestExternalImpl;

/**
 * The Ingest External Resource
 */
@Path("/ingest-external/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class IngestExternalResource extends ApplicationStatusResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalResource.class);
    private final IngestExternalConfiguration ingestExternalConfiguration;

    /**
     * Constructor IngestExternalResource
     *
     * @param ingestExternalConfiguration
     *
     */
    public IngestExternalResource(IngestExternalConfiguration ingestExternalConfiguration) {
        this.ingestExternalConfiguration = ingestExternalConfiguration;
        LOGGER.info("init Ingest External Resource server");
    }

    /**
     * upload the file in local
     *
     * @param uploadedInputStream data input stream
     * @param asyncResponse
     */
    @Path("ingests")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    // TODO P2 : add file name
    public void upload(InputStream uploadedInputStream, @Suspended final AsyncResponse asyncResponse) {
        VitamThreadPoolExecutor.getDefaultExecutor().execute(() -> uploadAsync(asyncResponse, uploadedInputStream));
    }

    private void uploadAsync(final AsyncResponse asyncResponse, InputStream uploadedInputStream) {
        try {
            // TODO ? ParametersChecker.checkParameter("HTTP Request must contains stream", uploadedInputStream);
            final IngestExternalImpl ingestExtern = new IngestExternalImpl(ingestExternalConfiguration);
            ingestExtern.upload(uploadedInputStream, asyncResponse);
        } catch (final IngestExternalException exc) {
            LOGGER.error(exc);
            try {
                AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                    Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity(AtrKoBuilder.buildAtrKo(GUIDFactory.newRequestIdGUID(0).getId(),
                            "ArchivalAgencyToBeDefined", "TransferringAgencyToBeDefined",
                            "PROCESS_SIP_UNITARY", exc.getMessage(), StatusCode.FATAL))
                        .type(MediaType.APPLICATION_XML_TYPE).build());
            } catch (final IngestExternalException e) {
                // Really bad
                LOGGER.error(e);
                AsyncInputStreamHelper.writeErrorAsyncResponse(asyncResponse,
                    Response.status(Status.INTERNAL_SERVER_ERROR).build());
            }
        } finally {
            StreamUtils.closeSilently(uploadedInputStream);
        }
    }

}
