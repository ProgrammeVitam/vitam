package fr.gouv.vitam.metadata.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.api.config.MetaDataConfiguration;
import fr.gouv.vitam.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetaDataMaxDepthException;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.api.model.RequestResponseError;
import fr.gouv.vitam.api.model.RequestResponseOK;
import fr.gouv.vitam.api.model.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.core.MetaDataImpl;
import fr.gouv.vitam.core.MongoDbAccessFactory;
import fr.gouv.vitam.core.database.collections.DbRequest;

/**
 * Units resource REST API 
 */
@Path("/metadata/v1")
public class UnitResource {
	private static final Logger LOGGER = Logger.getLogger(UnitResource.class);
	private MetaDataImpl metaDataImpl;

    // TODO: comment
	public UnitResource(MetaDataConfiguration configuration) {
		// FIXME REVIEW should not create the implementation directly but using the method as constructor
		metaDataImpl = new MetaDataImpl(configuration, new MongoDbAccessFactory(), DbRequest::new);
		LOGGER.info("init MetaData Resource server");
	}

	/**
	 * Get unit status 
	 */
	@Path("status")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	// FIXME REVIEW should be 204
	public Response status() {
		return Response.status(Status.OK).build();
	}

	/**
	 * Create unit with json request
	 */
	@Path("units")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response insertUnit(String insertRequest) {
		Status status;
		JsonNode queryJson;
		try {
			metaDataImpl.insertUnit(insertRequest);
			// FIXME REVIEW since you parse it, parse it first then pass it to insertUnit as a Json
			queryJson = JsonHandler.getFromString(insertRequest);
		} catch (InvalidParseOperationException e) {
			LOGGER.error(e.getMessage());
			// Unprocessable Entity not implemented by Jersey
			status = Status.BAD_REQUEST;
			return Response.status(status)
					.entity(new RequestResponseError().setError(
							new VitamError(status.getStatusCode())
							.setContext("ingest")
							.setState("code_vitam")
							.setMessage(status.getReasonPhrase())
							.setDescription(status.getReasonPhrase())))
					.build();
		} catch (MetaDataNotFoundException e) {
			LOGGER.error(e.getMessage());
			status = Status.NOT_FOUND;
			return Response.status(status)
					.entity(new RequestResponseError().setError(
							new VitamError(status.getStatusCode())
							.setContext("ingest")
							.setState("code_vitam")
							.setMessage(status.getReasonPhrase())
							.setDescription(status.getReasonPhrase())))
					.build();
		} catch (MetaDataAlreadyExistException e) {
			LOGGER.error(e.getMessage());
			status = Status.CONFLICT;
			return Response.status(status)
					.entity(new RequestResponseError().setError(
							new VitamError(status.getStatusCode())
							.setContext("ingest")
							.setState("code_vitam")
							.setMessage(status.getReasonPhrase())
							.setDescription(status.getReasonPhrase())))
					.build();
		} catch (MetaDataExecutionException e) {
			LOGGER.error(e.getMessage());
			status = Status.INTERNAL_SERVER_ERROR;
			return Response.status(status)
					.entity(new RequestResponseError().setError(
							new VitamError(status.getStatusCode())
							.setContext("ingest")
							.setState("code_vitam")
							.setMessage(status.getReasonPhrase())
							.setDescription(status.getReasonPhrase())))
					.build();
		}	catch (MetaDataDocumentSizeException | MetaDataMaxDepthException e) {
			LOGGER.error(e.getMessage());
			status = Status.REQUEST_ENTITY_TOO_LARGE;
			return Response.status(status)
					.entity(new RequestResponseError().setError(
							new VitamError(status.getStatusCode())
							.setContext("ingest")
							.setState("code_vitam")
							.setMessage(status.getReasonPhrase())
							.setDescription(status.getReasonPhrase())))
					.build();
		}
		return Response.status(Status.CREATED)
				.entity(new RequestResponseOK()
						.setHits(1, 0, 1)
						.setQuery(queryJson))
				.build();
	}

}
