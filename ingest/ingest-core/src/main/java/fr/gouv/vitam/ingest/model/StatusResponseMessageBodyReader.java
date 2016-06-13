
package fr.gouv.vitam.ingest.model;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Providers;

/**
 * Created by bsui on 12/05/16.
 */
@Consumes("application/json")
public class StatusResponseMessageBodyReader implements MessageBodyReader<StatusResponseDTO> {


    public StatusResponseMessageBodyReader(@Context Providers providers) {}

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return StatusResponseDTO.class == type;
    }

    @Override
    public StatusResponseDTO readFrom(Class<StatusResponseDTO> aClass, Type type, Annotation[] annotations,
        MediaType mediaType, MultivaluedMap<String, String> multivaluedMap, InputStream inputStream)
        throws IOException, WebApplicationException {
        return null;
    }
}
