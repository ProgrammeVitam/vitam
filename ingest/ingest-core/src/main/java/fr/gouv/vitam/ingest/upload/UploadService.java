package fr.gouv.vitam.ingest.upload;

import fr.gouv.vitam.ingest.core.exception.IngestException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

public interface UploadService {


    /**
     * Upload service a received SIP from a SIA
     * @param uploadedInputStream
     * @return Response
     * @throws IOException, if inputstream is null
     */
    public Response uploadSipAsStream(InputStream uploadedInputStream, FormDataContentDisposition fileDetail) throws IngestException;


    /**
     * Upload service a received SIP from a SIA with a name associated to the SIP
     * @param uploadedInputStream
     * @param fileDetail
     * @param sipName
     * @return
     * @throws IngestException
     */
    public Response uploadSipAsStream(InputStream uploadedInputStream, FormDataContentDisposition fileDetail, String sipName) throws Exception;


    /**
     *
     * @return
     */
    public Response status();
}
