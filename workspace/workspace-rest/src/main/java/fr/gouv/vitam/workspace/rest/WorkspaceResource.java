/**
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
 */
package fr.gouv.vitam.workspace.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.workspace.api.config.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.common.Entry;
import fr.gouv.vitam.workspace.core.ContentAddressableStorageImpl;
import fr.gouv.vitam.workspace.core.FileSystem;


/**
 * The Workspace Resource.
 *
 */
@Path("/workspace/v1")
public class WorkspaceResource {

    private static final VitamLogger LOGGER =

        VitamLoggerFactory.getInstance(WorkspaceResource.class);

    private ContentAddressableStorageImpl workspace;

    // TODO REVIEW comment
    /**
     * Constructor used to configure a workspace
     * 
     * @param configuration
     */
    public WorkspaceResource(StorageConfiguration configuration) {
        super();
        // TODO this implements directly the Filesystem implementation while it should not! You should have a
        // Factory/Helper to create the right one, ignoring here what is the chosen implementation.
        workspace = new FileSystem(configuration);
        LOGGER.info("init Workspace Resource server");
    }

    /**
     * Return a response status
     * 
     * @return Response
     */
    @Path("status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    // TODO should returns 204
    public Response status() {
        return Response.status(Status.OK).build();
    }

    /**
     * creates a container into the workspace *
     * 
     * @param container
     * @return Response
     */
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

    /**
     * deletes a container in the workspace
     * 
     * @param containerName
     * @return Response
     */
    @Path("/containers/{containerName}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteContainer(@PathParam("containerName") String containerName) {
        // TODO true by default ? SHould not be!

        try {
            workspace.deleteContainer(containerName, true);
        } catch (ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e.getMessage());
            return Response.status(Status.NOT_FOUND).entity(containerName).build();
        }

        return Response.status(Status.NO_CONTENT).entity(containerName).build();
    }

    /**
     * checks if a container exists in the workspace
     * 
     * @param containerName
     * @return Response
     */
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

    /**
     * creates a folder into a container
     * 
     * @param containerName
     * @param folder
     * @return Response
     */
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

    /**
     * deletes a folder in a container
     * 
     * @param containerName
     * @param folderName
     * @return Response
     */
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

    /**
     * checks if a folder exists in a container
     * 
     * @param containerName
     * @param folderName
     * @return Response
     */
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

    /**
     * puts an object into a container
     * 
     * @param stream
     * @param header
     * @param objectName
     * @param containerName
     * @return Response
     */
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

    /**
     * Deletes an objects in a container *
     * 
     * @param containerName
     * @param objectName
     * @return
     */
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

    /**
     * gets an objects from a container in the workspace
     * 
     * @param containerName
     * @param objectName
     * @return Response
     * @throws IOException
     */
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

    /**
     * checks if a object exists in an container
     * 
     * @param containerName
     * @return Response
     */
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

    /**
     * unzip a sip into the workspace
     * 
     * @param stream
     * @param header
     * @param containerName
     * @return Response
     */
    @Path("containers/{containerName}/objects")
    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unzipSip(@FormDataParam("object") InputStream stream,
        @FormDataParam("object") FormDataContentDisposition header,
        @PathParam("containerName") String containerName) {
        try {
            workspace.unzipSipObject(containerName, stream);
        } catch (ContentAddressableStorageAlreadyExistException e) {
            LOGGER.error(e.getMessage());
            return Response.status(Status.CONFLICT).entity(containerName).build();
        } catch (ContentAddressableStorageException e) {
            LOGGER.error(e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(containerName).build();
        }

        return Response.status(Status.CREATED).entity(containerName).build();
    }

    /**
     * gets the list of object from folder
     * 
     * @param containerName
     * @param folderName
     * @return Response
     */
    @Path("/containers/{containerName}/folders/{folderName}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUriDigitalObjectListByFolder(@PathParam("containerName") String containerName,
        @PathParam("folderName") String folderName) {

        List<URI> uriList = null;
        try {
            uriList = workspace.getListUriDigitalObjectFromFolder(containerName, folderName);

        } catch (ContentAddressableStorageNotFoundException eNotFoundException) {
            LOGGER.error(eNotFoundException.getMessage());
            return Response.status(Status.NOT_FOUND).entity(containerName).build();
        } catch (

        ContentAddressableStorageException eAddressableStorageException) {
            LOGGER.error(eAddressableStorageException.getMessage());

            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Collections.<URI>emptyList()).build();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Collections.<URI>emptyList()).build();

        }

        if (uriList == null || uriList.isEmpty()) {
            return Response.status(Status.NO_CONTENT).entity(Collections.<URI>emptyList()).build();
        }
        return Response.status(Status.OK).entity(uriList).build();

    }

}
