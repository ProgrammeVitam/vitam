package fr.gouv.vitam.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

public class MetaDataClient {

	private Client client;
	private String url;
	private static final String RESOURCE_PATH = "/metadata/v1";

	
	/**
	 * @param url
	 *            of metadata server
	 */
	public MetaDataClient(String url) {
		super();
		client = ClientBuilder.newClient();
		this.url = url + RESOURCE_PATH;
	}

	/**
	 * @param query
	 *            as String
	 * @return : response as String
	 * @throws InvalidParseOperationException 
	 */
	public String insert(String insertQuery) throws InvalidParseOperationException {
		Response response = client.target(url).path("units").request(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.post(Entity.entity(insertQuery, MediaType.APPLICATION_JSON), Response.class);

		if (response.getStatus() == Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
			throw new MetaDataExecutionException("Internal Server Error");
		} else if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
			throw new MetaDataNotFoundException("Not Found Exception");
		} else if (response.getStatus() == Status.CONFLICT.getStatusCode()) {
			throw new MetaDataAlreadyExistException("Data Already Exists");
		} else if (response.getStatus() == Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()) {
			throw new MetaDataDocumentSizeException("Document Size is Too Large");
		} else if (response.getStatus() == Status.BAD_REQUEST.getStatusCode()) {
            throw new InvalidParseOperationException("Invalid Parse Operation");
        }		
		
		return response.readEntity(String.class);
	}

	/**
	 * @return : status of metadata server 200 : server is alive
	 */
	public Response status() {
		return client.target(url).path("status").request().get();
	}

}