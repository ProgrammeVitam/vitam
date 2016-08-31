package fr.gouv.vitam.ingest.external.client;

import java.io.InputStream;

import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.ingest.external.api.IngestExternalException;

/**
 * Ingest external interface
 */
public interface IngestExternalClient {
    /**
     * ingest upload file in local
     * TODO : add file name
     * @throws IngestExternalException 
     */
    void upload(InputStream stream) throws IngestExternalException;
    
    /**
     * Get the status from the service
     *
     * @return the Message status
     */
    Status status();
}
