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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.stream.XMLStreamException;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server2.application.resources.ApplicationStatusResource;
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
    private IngestExternalConfiguration ingestExternalConfiguration;

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
     * @param stream data input stream
     * @param header method for entry data
     * @return Response
     * @throws XMLStreamException
     */
    // TODO P2 : add file name
    @Path("ingests")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public Response upload(InputStream stream) throws XMLStreamException {
        Response response;
        try {
            IngestExternalImpl ingestExtern = new IngestExternalImpl(ingestExternalConfiguration);
            response = ingestExtern.upload(stream);
        } catch (final IngestExternalException e) {
            LOGGER.error(e);
            final Status status = Status.INTERNAL_SERVER_ERROR;
            try {
                return Response.status(status)
                    .entity(AtrKoBuilder.buildAtrKo(GUIDFactory.newRequestIdGUID(0).getId(), "Unknown", "Unknown", e.getMessage()))
                    .type(MediaType.APPLICATION_XML_TYPE)
                    .build();
            } catch (IngestExternalException e1) {
                // Really bad
                LOGGER.error(e1);
                return Response.status(status).build();
            }
        }
        // FIXME P0 Move to Async
        return Response.status(response.getStatus()).entity(response.getEntity())
            .header(GlobalDataRest.X_REQUEST_ID, response.getHeaderString(GlobalDataRest.X_REQUEST_ID)).build();

    }

}
