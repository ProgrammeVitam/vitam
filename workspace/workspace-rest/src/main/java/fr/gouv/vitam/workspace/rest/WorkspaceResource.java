package fr.gouv.vitam.workspace.rest;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import fr.gouv.vitam.workspace.common.Entry;
import fr.gouv.vitam.workspace.api.config.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.core.ContentAddressableStorageImpl;
import fr.gouv.vitam.workspace.core.FileSystem;

// TODO REVIEW licence header is missing

/**
 * The Workspace Resource.
 *
 */
@Path("/workspace/v1")
public class WorkspaceResource {
    private static final Logger LOGGER = Logger.getLogger(WorkspaceResource.class);
    private ContentAddressableStorageImpl workspace;

    // TODO REVIEW comment
    public WorkspaceResource(StorageConfiguration configuration) {
        super();
        // FIXME REVIEW this implements directly the Filesystem implementation while it should not! You should have a Factory/Helper to create the right one, ignoring here what is the chosen implementation.
        workspace = new FileSystem(configuration);
        LOGGER.info("init Workspace Resource server");
    }

    @Path("status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    // FIXME REVIEW should returns 204
    public Response status() {
        return Response.status(Status.OK).build();
    }

    @Path("containers")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createContainer(Entry container) {

        try {
            workspace.createContainer(container.getName());
        } catch (ContentAddressableStorageAlreadyExistException e) {
            LOGGER.error(e.getMessage());
            return Response.status(Status.CONFLICT).entity(container.getName()).build();
        }

        return Response.status(Status.CREATED).entity(container.getName()).build();
    }

    @Path("/containers/{containerName}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteContainer(@PathParam("containerName") String containerName) {
        // FIXME REVIEW true by default ? SHould not be!

        try {
            workspace.deleteContainer(containerName, true);
        } catch (ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e.getMessage());
            return Response.status(Status.NOT_FOUND).entity(containerName).build();
        }

        return Response.status(Status.NO_CONTENT).entity(containerName).build();
    }

    @Path("/containers/{containerName}")
    @HEAD
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response containerExists(@PathParam("containerName") String containerName) {
        boolean exists = workspace.containerExists(containerName);
        if (exists) {
            return Response.status(Status.OK).entity(containerName).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }

    }

    @Path("containers/{containerName}/folders")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFolder(@PathParam("containerName") String containerName, Entry folder) {

        try {
            workspace.createFolder(containerName, folder.getName());
        } catch (ContentAddressableStorageAlreadyExistException e) {
            LOGGER.error(e.getMessage());
            return Response.status(Status.CONFLICT).entity(containerName + "/" + folder.getName()).build();
        } catch (ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e.getMessage());
            return Response.status(Status.NOT_FOUND).entity(containerName + "/" + folder.getName()).build();
        }

        return Response.status(Status.CREATED).entity(containerName + "/" + folder.getName()).build();
    }

    @Path("/containers/{containerName}/folders/{folderName}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFolder(@PathParam("containerName") String containerName,
            @PathParam("folderName") String folderName) {

        try {
            workspace.deleteFolder(containerName, folderName);
        } catch (ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e.getMessage());
            return Response.status(Status.NOT_FOUND).entity(containerName + "/" + folderName).build();
        }

        return Response.status(Status.NO_CONTENT).entity(containerName + "/" + folderName).build();
    }

    @Path("/containers/{containerName}/folders/{folderName}")
    @HEAD
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response folderExists(@PathParam("containerName") String containerName,
            @PathParam("folderName") String folderName) {

        boolean exists = workspace.folderExists(containerName, folderName);
        if (exists) {
            return Response.status(Status.OK).entity(containerName + "/" + folderName).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }

    }

    @Path("containers/{containerName}/objects")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putObject(@FormDataParam("object") InputStream stream,
            @FormDataParam("object") FormDataContentDisposition header, @FormDataParam("objectName") String objectName,
            @PathParam("containerName") String containerName) {
        try {
            workspace.putObject(containerName, objectName, stream);
        } catch (ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e.getMessage());
            return Response.status(Status.NOT_FOUND).entity(containerName).build();
        } catch (ContentAddressableStorageException e) {
            LOGGER.error(e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(containerName).build();
        }

        return Response.status(Status.CREATED).entity(containerName + "/" + objectName).build();
    }

    @Path("containers/{containerName}/objects/{objectName}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteObject(@PathParam("containerName") String containerName,
            @PathParam("objectName") String objectName) {

        try {
            workspace.deleteObject(containerName, objectName);
        } catch (ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e.getMessage());
            return Response.status(Status.NOT_FOUND).entity(containerName).build();
        }

        return Response.status(Status.NO_CONTENT).entity(containerName).build();
    }

    @Path("containers/{containerName}/objects/{objectName:.*}")
    @GET
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getObject(@PathParam("containerName") String containerName,
            @PathParam("objectName") String objectName) throws IOException {
        InputStream stream = null;
        try {
            stream = workspace.getObject(containerName, objectName);
        } catch (ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e.getMessage());
            return Response.status(Status.NOT_FOUND).entity(containerName).build();
        } catch (ContentAddressableStorageException e) {
            LOGGER.error(e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(containerName).build();
        }

        return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM).header("Content-Length", stream.available())
                .header("Content-Disposition", "attachment; filename=\"" + objectName + "\"").build();
    }

    @Path("/containers/{containerName}/objects/{objectName}")
    @HEAD
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response objectExists(@PathParam("containerName") String containerName,
            @PathParam("objectName") String objectName) {

        boolean exists = workspace.objectExists(containerName, objectName);
        if (exists) {
            return Response.status(Status.OK).entity(containerName + "/" + objectName).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }

    }
}
