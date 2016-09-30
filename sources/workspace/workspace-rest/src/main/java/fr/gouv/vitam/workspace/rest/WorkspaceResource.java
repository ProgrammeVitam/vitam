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
import javax.ws.rs.HeaderParam;
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

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.workspace.api.config.StorageConfiguration;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageZipException;
import fr.gouv.vitam.workspace.api.model.ContainerInformation;
import fr.gouv.vitam.workspace.common.Entry;
import fr.gouv.vitam.workspace.common.ErrorMessage;
import fr.gouv.vitam.workspace.common.RequestResponseError;
import fr.gouv.vitam.workspace.common.VitamError;
import fr.gouv.vitam.workspace.core.ContentAddressableStorageAbstract;
import fr.gouv.vitam.workspace.core.filesystem.FileSystem;


/**
 * The Workspace Resource.
 */
@Path("/workspace/v1")
public class WorkspaceResource {

    private static final VitamLogger LOGGER =

        VitamLoggerFactory.getInstance(WorkspaceResource.class);

    private final ContentAddressableStorageAbstract workspace;

    /**
     * Constructor used to configure a workspace
     *
     * @param configuration the storage config
     */
    public WorkspaceResource(StorageConfiguration configuration) {
        super();
        // FIXME REVIEW this implements directly the Filesystem implementation while it should not! You should have a
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
    public Response status() {
        return Response.status(Status.OK).build();
    }

    /**
     * creates a container into the workspace *
     *
     * @param container as entry json
     * @return Response
     */
    @Path("containers")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createContainer(Entry container) {
        // FIXME REVIEW should be changed to POST /containers/{containername}

        try {
            ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
                container);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(container));

            workspace.createContainer(container.getName());
        } catch (final IllegalArgumentException | InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ContentAddressableStorageAlreadyExistException e) {
            LOGGER.error(e);
            return Response.status(Status.CONFLICT).entity(container.getName()).build();
        }

        return Response.status(Status.CREATED).entity(container.getName()).build();
    }

    /**
     * deletes a container in the workspace
     *
     * @param containerName path param of container name
     * @return Response
     */
    @Path("/containers/{containerName}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteContainer(@PathParam("containerName") String containerName) {
        // FIXME REVIEW true by default ? SHould not be! need to test if container is empty

        try {
            workspace.deleteContainer(containerName, true);

        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.NOT_FOUND).entity(containerName).build();
        }

        return Response.status(Status.NO_CONTENT).entity(containerName).build();
    }

    /**
     * checks if a container exists in the workspace
     *
     * @param containerName path param for container name
     * @return Response
     */
    @Path("/containers/{containerName}")
    @HEAD
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response isExistingContainer(@PathParam("containerName") String containerName) {
        final boolean exists = workspace.isExistingContainer(containerName);
        if (exists) {
            return Response.status(Status.OK).entity(containerName).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }

    }

    /**
     * creates a folder into a container
     *
     * @param containerName path param of container name
     * @param folder entry param of folder
     * @return Response
     */
    @Path("containers/{containerName}/folders")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFolder(@PathParam("containerName") String containerName, Entry folder) {
        // FIXME REVIEW should be changed to POST /containers/{containername}/folders/{foldername}
        try {
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(folder));
            workspace.createFolder(containerName, folder.getName());
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ContentAddressableStorageAlreadyExistException e) {
            LOGGER.error(e);
            return Response.status(Status.CONFLICT).entity(containerName + "/" + folder.getName()).build();
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.NOT_FOUND).entity(containerName + "/" + folder.getName()).build();
        }

        return Response.status(Status.CREATED).entity(containerName + "/" + folder.getName()).build();
    }

    /**
     * deletes a folder in a container
     *
     * @param containerName path param for container name
     * @param folderName path param for folder name
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
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.NOT_FOUND).entity(containerName + "/" + folderName).build();
        }

        return Response.status(Status.NO_CONTENT).entity(containerName + "/" + folderName).build();
    }

    /**
     * checks if a folder exists in a container
     *
     * @param containerName path param for container name
     * @param folderName path param for folder name
     * @return Response
     */
    @Path("/containers/{containerName}/folders/{folderName}")
    @HEAD
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response isExistingFolder(@PathParam("containerName") String containerName,
        @PathParam("folderName") String folderName) {

        final boolean exists = workspace.isExistingFolder(containerName, folderName);
        if (exists) {
            return Response.status(Status.OK).entity(containerName + "/" + folderName).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }

    }

    /**
     * puts an object into a container
     *
     * @param stream data input stream
     * @param header method for entry data
     * @param objectName name of data object
     * @param containerName name of container
     * @return Response
     */
    @Path("containers/{containerName}/objects")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    // FIXME REVIEW change to correct API (no FormData)
    public Response putObject(@FormDataParam("object") InputStream stream,
        @FormDataParam("object") FormDataContentDisposition header, @FormDataParam("objectName") String objectName,
        @PathParam("containerName") String containerName) {
        try {
            workspace.putObject(containerName, objectName, stream);
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.NOT_FOUND).entity(containerName).build();
        } catch (final ContentAddressableStorageException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(containerName).build();
        }

        return Response.status(Status.CREATED).entity(containerName + "/" + objectName).build();
    }

    /**
     * Deletes an objects in a container *
     *
     * @param containerName container name
     * @param objectName object name
     * @return Response
     */
    @Path("containers/{containerName}/objects/{objectName:.*}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteObject(@PathParam("containerName") String containerName,
        @PathParam("objectName") String objectName) {

        try {
            workspace.deleteObject(containerName, objectName);
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.NOT_FOUND).entity(containerName).build();
        }

        return Response.status(Status.NO_CONTENT).entity(containerName).build();
    }

    /**
     * gets an objects from a container in the workspace
     *
     * @param containerName name of container
     * @param objectName name of object
     * @return Response
     * @throws IOException when there is an error of get object
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
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.NOT_FOUND).entity(containerName).build();
        } catch (final ContentAddressableStorageException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(containerName).build();
        }

        return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM).header("Content-Length", stream.available())
            .header("Content-Disposition", "attachment; filename=\"" + objectName + "\"").build();
    }

    /**
     * gets an objects from a container in the workspace
     *
     * @param containerName name of container
     * @param objectName name of object
     * @return Response
     * @throws IOException when there is an error of get object
     */
    @Path("containers/{containerName}/objects/{objectName:.*}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjectInformation(@PathParam("containerName") String containerName,
        @PathParam("objectName") String objectName) throws IOException {
        JsonNode jsonResultNode;
        try {
            jsonResultNode = workspace.getObjectInformation(containerName, objectName);
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.NOT_FOUND).entity(containerName).build();
        } catch (final ContentAddressableStorageException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(containerName).build();
        }

        return Response.status(Status.OK).entity(jsonResultNode).build();
    }

    /**
     * checks if a object exists in an container or compute object Digest
     *
     * @param containerName name of container
     * @param objectName name of object
     * @param algo path parameter of algo
     * @return Response
     */
    @Path("/containers/{containerName}/objects/{objectName:.*}")
    @HEAD
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response computeObjectDigest(@PathParam("containerName") String containerName,
        @PathParam("objectName") String objectName, @HeaderParam("X-digest-algorithm") String algo) {

        try {
            ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
                containerName, objectName);
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        if (algo != null) {
            LOGGER.debug("X-digest-algorithm : " + algo);
            String messageDigest = null;
            try {
                messageDigest = workspace.computeObjectDigest(containerName, objectName, DigestType.fromValue(algo));
            } catch (final ContentAddressableStorageNotFoundException e) {
                LOGGER.error(e);
                return Response.status(Status.NOT_FOUND)
                    .header("X-digest-algorithm", algo)
                    .entity(containerName + "/" + objectName).build();
            } catch (final ContentAddressableStorageException e) {
                LOGGER.error(e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .header("X-digest-algorithm", algo)
                    .entity(containerName + "/" + objectName).build();
            }

            return Response.status(Status.OK)
                .header("X-digest-algorithm", algo)
                .header("X-digest", messageDigest).build();
        } else {
            final boolean exists = workspace.isExistingObject(containerName, objectName);
            if (exists) {
                return Response.status(Status.OK).entity(containerName + "/" + objectName).build();
            } else {
                return Response.status(Status.NOT_FOUND).build();
            }
        }
    }

    /**
     * unzip a sip into the workspace
     *
     * @param stream data input stream
     * @param containerName name of container
     * @param folderName name of folder
     * @return Response
     */
    @Path("containers/{containerName}/folders/{folderName}")
    @PUT
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unzipObject(InputStream stream,
        @PathParam("containerName") String containerName,
        @PathParam("folderName") String folderName) {

        try {
            ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
                containerName, folderName);
        } catch (IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        try {
            workspace.unzipObject(containerName, folderName, stream);
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(e);
            return Response.status(Status.NOT_FOUND).entity(containerName).build();
        } catch (final ContentAddressableStorageAlreadyExistException e) {
            LOGGER.error(e);
            return Response.status(Status.CONFLICT).entity(containerName).build();
        } catch (final ContentAddressableStorageZipException e) {
            LOGGER.error(e);
            Status status = Status.BAD_REQUEST;
            // TODO : For now it is generic code "0000" since vitam error code have not been defined
            return Response.status(status)
                .entity(new RequestResponseError().setError(
                    new VitamError(status.getStatusCode())
                        .setContext("WORKSPACE")
                        .setState("vitam_code")
                        .setMessage(status.getReasonPhrase())
                        .setDescription(status.getReasonPhrase())))
                .build();

        } catch (final ContentAddressableStorageException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(containerName).build();
        }

        return Response.status(Status.CREATED).entity(containerName).build();
    }

    /**
     * gets the list of object from folder
     *
     * @param containerName name of container
     * @param folderName name of folder
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

        } catch (final ContentAddressableStorageNotFoundException eNotFoundException) {
            LOGGER.error(eNotFoundException);
            return Response.status(Status.NOT_FOUND).entity(containerName).build();
        } catch (final ContentAddressableStorageException eAddressableStorageException) {
            LOGGER.error(eAddressableStorageException);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Collections.<URI>emptyList()).build();
        }

        if (uriList == null || uriList.isEmpty()) {
            return Response.status(Status.NO_CONTENT).entity(Collections.<URI>emptyList()).build();
        }
        return Response.status(Status.OK).entity(uriList).build();

    }

    /**
     * Get container information like capacity
     *
     * @param containerName the container name
     * @return a Json with usableSpace and usedSpace information
     */
    @Path("/container/{containerName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getContainerInformation(@PathParam("containerName") String containerName) {
        try {
            ContainerInformation containerInformation = workspace.getContainerInformation(containerName);
            return Response.status(Status.OK).entity(containerInformation).build();
        } catch (ContentAddressableStorageNotFoundException exc) {
            LOGGER.error(exc);
            return Response.status(Status.NOT_FOUND).entity(containerName).build();
        }
    }
}

