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
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.ApplicationStatusResource;
import fr.gouv.vitam.common.server.application.BasicVitamStatusServiceImpl;
import fr.gouv.vitam.ingest.external.api.IngestExternalException;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import fr.gouv.vitam.ingest.external.common.model.response.IngestExternalError;
import fr.gouv.vitam.ingest.external.core.IngestExternalImpl;

/**
 * The Ingest External Resource
 */
@Path("/ingest-ext/v1")
public class IngestExternalResource extends ApplicationStatusResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalResource.class);
    private IngestExternalImpl ingestExtern;

    /**
     * Constructor IngestExternalResource
     * 
     * @param ingestExternalConfiguration
     */
    public IngestExternalResource(IngestExternalConfiguration ingestExternalConfiguration) {
        super(new BasicVitamStatusServiceImpl());
        ingestExtern = new IngestExternalImpl(ingestExternalConfiguration);
        LOGGER.info("init Ingest External Resource server");
    }

    /**
     * upload the file in local TODO : add file name
     * 
     * @param stream, data input stream
     * @param header, method for entry data
     * @return Response
     * @throws XMLStreamException 
     */
    @Path("upload")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_XML)
    public Response upload(InputStream stream) throws XMLStreamException{
        Response response;
        try {
            response = ingestExtern.upload(stream);
        } catch (IngestExternalException e) {
            LOGGER.error(e.getMessage());
            Status status = Status.INTERNAL_SERVER_ERROR;
            return Response.status(status)
                .entity(new IngestExternalError(status.getStatusCode())
                    .setContext("ingest")
                    .setState("Error")
                    .setMessage("The ingest external server error")
                    .setDescription(
                        "The application 'Xxxx' requested an ingest operation and this operation has errors."))
                .build();
        }
        //TODO Fix ByteArray vs Close vs AsyncResponse
        return Response.status(Status.OK).entity(response.getEntity()) 
            .header(GlobalDataRest.X_REQUEST_ID, response.getHeaderString(GlobalDataRest.X_REQUEST_ID)).build();
    }    

}
