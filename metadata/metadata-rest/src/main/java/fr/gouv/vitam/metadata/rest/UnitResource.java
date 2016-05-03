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

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.api.config.MetaDataConfiguration;
import fr.gouv.vitam.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.core.MetaDataImpl;

@Path("/v1/data")
public class UnitResource {
	private static final Logger LOGGER = Logger.getLogger(UnitResource.class);
	private MetaDataImpl metaDataImpl;

	public UnitResource(MetaDataConfiguration configuration) {
		super();
		metaDataImpl = new MetaDataImpl(configuration);
		LOGGER.info("init MetaData Resource server");
	}

	@Path("status")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response status() {
		return Response.status(Status.OK).build();
	}

	@Path("units")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response insertUnit(String insertRequest) {

		try {
			metaDataImpl.insertUnit(insertRequest);
		} catch (InvalidParseOperationException e) {
			LOGGER.error(e.getMessage());
			return Response.status(Status.BAD_REQUEST).entity(insertRequest).build();
		} catch (MetaDataNotFoundException e) {
			LOGGER.error(e.getMessage());
			return Response.status(Status.NOT_FOUND).entity(insertRequest).build();
		} catch (MetaDataAlreadyExistException e) {
			LOGGER.error(e.getMessage());
			return Response.status(Status.CONFLICT).entity(insertRequest).build();
		} catch (MetaDataExecutionException e) {
			LOGGER.error(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(insertRequest).build();
		}

		return Response.status(Status.CREATED).entity(insertRequest).build();
	}

}
