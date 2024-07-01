/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.server.application.VitamHttpHeader;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.StorageConfiguration;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.common.stream.MultiplexedStreamWriter;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageCompressedFileException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ZipFilesNameNotAllowedException;
import fr.gouv.vitam.workspace.api.model.FileParams;
import fr.gouv.vitam.workspace.api.model.TimeToLive;
import fr.gouv.vitam.workspace.common.CompressInformation;
import fr.gouv.vitam.workspace.common.WorkspaceFileSystem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.compress.archivers.ArchiveException;

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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.model.WorkspaceConstants.FREESPACE;
import static fr.gouv.vitam.common.stream.StreamUtils.consumeAnyEntityAndClose;

@Path("/v1")
@Tag(name = "Worker")
public class WorkspaceResource extends ApplicationStatusResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkspaceResource.class);

    private static final String FOLDER_NAME = "folderName";
    private static final String OBJECT_NAME = "objectName";
    private static final String CONTAINER_NAME = "containerName";

    private final WorkspaceFileSystem workspace;

    /**
     * Constructor used to configure a workspace
     *
     * @param configuration the storage config
     */
    WorkspaceResource(StorageConfiguration configuration) {
        try {
            workspace = new WorkspaceFileSystem(configuration);
        } catch (IOException ex) {
            LOGGER.error("cannot load WorkspaceFileSystem : ", ex);
            throw new IllegalStateException(ex);
        }
        LOGGER.info("init Workspace Resource server");
    }

    /**
     * retrieve free space
     *
     * @return free space percentage
     */
    @Path("/" + FREESPACE)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get workspace free space", description = "obtenir de l'espace libre du dossier workspace")
    public Response getFreespacePercent() {
        JsonNode result = JsonHandler.createObjectNode().put(FREESPACE, workspace.getWorkspaceFreeSpace());
        return Response.ok(result).build();
    }

    /**
     * creates a container into the workspace
     *
     * @param containerName as path param
     * @return Response
     */
    @Path("/containers/{containerName}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create one container", description = "Permet de créer un nouveau container")
    public Response createContainer(@PathParam(CONTAINER_NAME) String containerName) {
        try {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
                containerName
            );
            workspace.checkWorkspaceContainerSanity(containerName);
            workspace.createContainer(containerName);
            return Response.status(Status.CREATED).build();
        } catch (IllegalPathException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ContentAddressableStorageAlreadyExistException e) {
            LOGGER.info(ErrorMessage.CONTAINER_ALREADY_EXIST.getMessage() + containerName);
            return Response.status(Status.CREATED).build();
        } catch (Exception e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * deletes a container in the workspace
     *
     * @param containerName path param of container name
     * @param recursive true if the container should be deleted recursively
     * @return Response
     */
    @Path("/containers/{containerName}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete container by name", description = "Permet de supprimer un container")
    public Response deleteContainer(
        @PathParam(CONTAINER_NAME) String containerName,
        @HeaderParam(GlobalDataRest.X_RECURSIVE) boolean recursive
    ) {
        try {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
                containerName
            );
            workspace.checkWorkspaceContainerSanity(containerName);
            workspace.deleteContainer(containerName, recursive);
        } catch (IllegalPathException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName + " => " + e.getMessage());
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(Status.NO_CONTENT).build();
    }

    /**
     * deletes old file in a container in the workspace
     */
    @Path("/containers/{containerName}/old_files")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete object this from container", description = "Permet de supprimer un objet du container")
    public Response purgeOldFilesInContainer(@PathParam(CONTAINER_NAME) String containerName, TimeToLive timeToLive) {
        try {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
                containerName
            );
            workspace.checkWorkspaceContainerSanity(containerName);
            workspace.purgeOldFilesInContainer(containerName, timeToLive);
            return Response.status(Status.NO_CONTENT).build();
        } catch (IllegalPathException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName + " => " + e.getMessage());
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * checks if a container exists in the workspace
     *
     * @param containerName path param for container name
     * @return Response
     */
    @Path("/containers/{containerName}")
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "test if this container exists", description = "Permet de tester l'existence du Container")
    public Response isExistingContainer(@PathParam(CONTAINER_NAME) String containerName) {
        try {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
                containerName
            );
            workspace.checkWorkspaceContainerSanity(containerName);
            final boolean exists = workspace.isExistingContainer(containerName);
            if (exists) {
                return Response.status(Status.OK).build();
            } else {
                return Response.status(Status.NOT_FOUND).build();
            }
        } catch (IllegalPathException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final Exception e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get container information like capacity
     *
     * @param containerName the container name
     * @return a Json with usableSpace information
     */
    @Path("/container/{containerName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get container informations", description = "Permet d'accéder aux informations d'un container")
    public Response getContainerInformation(@PathParam(CONTAINER_NAME) String containerName) {
        try {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
                containerName
            );
            workspace.checkWorkspaceContainerSanity(containerName);
            final ContainerInformation containerInformation = workspace.getContainerInformation(containerName);
            return Response.status(Status.OK).entity(containerInformation).build();
        } catch (IllegalPathException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ContentAddressableStorageNotFoundException exc) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName, exc);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * creates a folder into a container
     *
     * @param containerName path param of container name
     * @param folderName path param of folder
     * @return Response
     */
    @Path("/containers/{containerName}/folders/{folderName:.*}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "create operation folder", description = "Permet de créer un sous dossier dans le container")
    public Response createFolder(
        @PathParam(CONTAINER_NAME) String containerName,
        @PathParam(FOLDER_NAME) String folderName
    ) {
        try {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
                containerName,
                folderName
            );
            workspace.checkWorkspaceDirSanity(containerName, folderName);
            workspace.createFolder(containerName, folderName);
            return Response.status(Status.CREATED).entity(containerName + "/" + folderName).build();
        } catch (IllegalPathException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ContentAddressableStorageAlreadyExistException e) {
            LOGGER.info(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage() + containerName + "/" + folderName, e);
            return Response.status(Status.CREATED).entity(containerName + "/" + folderName).build();
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(ErrorMessage.FOLDER_NOT_FOUND.getMessage() + containerName + "/" + folderName, e);
            return Response.status(Status.NOT_FOUND).entity(containerName + "/" + folderName).build();
        } catch (final Exception e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * deletes a folder in a container
     *
     * @param containerName path param for container name
     * @param folderName path param for folder name
     * @return Response
     */
    @Path("/containers/{containerName}/folders/{folderName:.*}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "delete operation folder",
        description = "Permet de supprimer un sous dossier dans le container"
    )
    public Response deleteFolder(
        @PathParam(CONTAINER_NAME) String containerName,
        @PathParam(FOLDER_NAME) String folderName
    ) {
        try {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
                containerName,
                folderName
            );
            workspace.checkWorkspaceDirSanity(containerName, folderName);
            workspace.deleteFolder(containerName, folderName);
        } catch (IllegalPathException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(ErrorMessage.FOLDER_NOT_FOUND.getMessage() + containerName + "/" + folderName, e);
            return Response.status(Status.NOT_FOUND).entity(containerName + "/" + folderName).build();
        } catch (final Exception e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
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
    @Path("/containers/{containerName}/folders/{folderName:.*}")
    @HEAD
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "test if operation folder exists",
        description = "Permet de vérifier l'existance d'un sous dossier dans le container"
    )
    public Response isExistingFolder(
        @PathParam(CONTAINER_NAME) String containerName,
        @PathParam(FOLDER_NAME) String folderName
    ) {
        try {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
                containerName,
                folderName
            );
            workspace.checkWorkspaceDirSanity(containerName, folderName);
            final boolean exists = workspace.isExistingFolder(containerName, folderName);
            if (exists) {
                return Response.status(Status.OK).entity(containerName + "/" + folderName).build();
            } else {
                return Response.status(Status.NOT_FOUND).build();
            }
        } catch (IllegalPathException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final Exception e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * uncompress a sip into the workspace
     *
     * @param stream data input stream
     * @param containerName name of container
     * @param folderName name of folder
     * @param archiveType the type of archive
     * @return Response
     */
    @Path("/containers/{containerName}/folders/{folderName:.*}")
    @PUT
    @Consumes(
        { CommonMediaType.ZIP, CommonMediaType.XGZIP, CommonMediaType.GZIP, CommonMediaType.TAR, CommonMediaType.BZIP2 }
    )
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "upload zip or tar into that folder",
        description = "Permet d'uploader un ZIP ou TAR avec un unzip/untar automatique sous ce folder, incluant la création de sous-folders"
    )
    public Response uncompressObject(
        InputStream stream,
        @PathParam(CONTAINER_NAME) String containerName,
        @PathParam(FOLDER_NAME) String folderName,
        @HeaderParam(HttpHeaders.CONTENT_TYPE) String archiveType
    ) {
        try {
            workspace.checkWorkspaceDirSanity(containerName, folderName);
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
                containerName,
                folderName
            );
            workspace.uncompressObject(containerName, folderName, archiveType, stream);
            return Response.status(Status.CREATED).build();
        } catch (final IllegalArgumentException | IllegalPathException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage() + containerName, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final ContentAddressableStorageAlreadyExistException e) {
            LOGGER.info(ErrorMessage.CONTAINER_ALREADY_EXIST.getMessage() + containerName, e);
            return Response.status(Status.CONFLICT).build();
        } catch (final ZipFilesNameNotAllowedException e) {
            LOGGER.error(e);
            final VitamError vitamError = getVitamError(VitamCode.WORKSPACE_NOT_ACCEPTABLE_FILES, e.getMessage());
            return Response.status(Status.NOT_ACCEPTABLE).entity(vitamError).build();
        } catch (final ContentAddressableStorageCompressedFileException e) {
            LOGGER.error(e);
            final Status status = Status.BAD_REQUEST;
            final VitamError vitamError = getVitamError(VitamCode.WORKSPACE_BAD_REQUEST, e.getMessage());
            return Response.status(status).entity(vitamError).build();
        } catch (final ContentAddressableStorageException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            StreamUtils.closeSilently(stream);
        }
    }

    private VitamError getVitamError(VitamCode vitamCode, String msg) {
        return new VitamError(vitamCode.name())
            .setMessage(msg)
            .setState("ko")
            .setHttpCode(vitamCode.getStatus().getStatusCode())
            .setDescription(msg)
            .setContext(vitamCode.getService().getName());
    }

    /**
     * zip a specific folder into a other directory
     *
     * @param containerName
     * @return
     */
    @Path("/containers/{containerName}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "upload zip or tar into that folder",
        description = "Permet d'uploader un ZIP ou TAR avec un unzip/untar automatique sous ce folder, incluant la création de sous-folders"
    )
    public Response compress(@PathParam(CONTAINER_NAME) String containerName, CompressInformation compressInformation) {
        try {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
                containerName,
                compressInformation
            );
            workspace.checkWorkspaceContainerSanity(containerName);
            workspace.compress(
                containerName,
                compressInformation.getFiles(),
                compressInformation.getOutputFile(),
                compressInformation.getOutputContainer()
            );
            return Response.status(Status.CREATED).build();
        } catch (IOException | IllegalPathException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (ArchiveException e) {
            LOGGER.error(e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    /**
     * gets the list of URI of object from folder
     *
     * @param containerName name of container
     * @param folderName name of folder
     * @return Response
     */
    @Path("/containers/{containerName}/folders/{folderName:.*}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "get object list from folder",
        description = "Permet de récupérer la liste des objets du dossier du container"
    )
    public Response getUriDigitalObjectListByFolder(
        @PathParam(CONTAINER_NAME) String containerName,
        @PathParam(FOLDER_NAME) String folderName
    ) {
        List<URI> uriList;
        try {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
                containerName,
                folderName
            );
            workspace.checkWorkspaceDirSanity(containerName, folderName);
            uriList = workspace.getListUriDigitalObjectFromFolder(containerName, folderName);
        } catch (IllegalPathException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ContentAddressableStorageNotFoundException eNotFoundException) {
            LOGGER.error(ErrorMessage.FOLDER_NOT_FOUND.getMessage() + containerName + "/" + folderName);
            return Response.status(Status.NOT_FOUND).entity(containerName + "/" + folderName).build();
        } catch (final ContentAddressableStorageException eAddressableStorageException) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), eAddressableStorageException);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Collections.<URI>emptyList()).build();
        }

        if (uriList == null || uriList.isEmpty()) {
            return Response.status(Status.NO_CONTENT).entity(Collections.<URI>emptyList()).build();
        }
        return Response.status(Status.OK).entity(uriList).build();
    }

    @Path("/containers/{containerName}/folders/{folderName:.*}/filesWithParams")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get Map of files with params from folder",
        description = "Permet de récupérer une Map des objets du dossier du container avec ses propres paramétres"
    )
    public Response getFilesWithParamsFromFolder(
        @PathParam(CONTAINER_NAME) String containerName,
        @PathParam(FOLDER_NAME) String folderName
    ) {
        Map<String, FileParams> filesWithParamsMap;
        try {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
                containerName,
                folderName
            );
            workspace.checkWorkspaceDirSanity(containerName, folderName);
            filesWithParamsMap = workspace.getFilesWithParamsFromFolder(containerName, folderName);
        } catch (IllegalPathException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ContentAddressableStorageNotFoundException eNotFoundException) {
            LOGGER.error(ErrorMessage.FOLDER_NOT_FOUND.getMessage() + containerName + "/" + folderName);
            return Response.status(Status.NOT_FOUND).entity(containerName + "/" + folderName).build();
        } catch (final ContentAddressableStorageException eAddressableStorageException) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), eAddressableStorageException);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Collections.<URI>emptyList()).build();
        }

        if (filesWithParamsMap == null || filesWithParamsMap.isEmpty()) {
            return Response.status(Status.NO_CONTENT).entity(Collections.<URI>emptyList()).build();
        }
        return Response.status(Status.OK).entity(filesWithParamsMap).build();
    }

    /**
     * puts an object into a container
     *
     * @param stream data input stream
     * @param objectName name of data object
     * @param containerName name of container
     * @return Response
     */
    @Path("/containers/{containerName}/objects/{objectName:.*}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "create new object in container",
        description = "Permet de créer un nouvel objet dans le container"
    )
    public Response putObject(
        InputStream stream,
        @PathParam(CONTAINER_NAME) String containerName,
        @PathParam(OBJECT_NAME) String objectName
    ) {
        try {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
                containerName,
                objectName
            );
            workspace.checkWorkspaceFileSanity(containerName, objectName);
            workspace.putObject(containerName, objectName, stream);
            return Response.status(Status.CREATED).entity(containerName + "/" + objectName).build();
        } catch (IllegalPathException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND.getMessage() + containerName, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final ContentAddressableStorageException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            StreamUtils.closeSilently(stream);
        }
    }

    /**
     * puts an atomic object into a container
     *
     * @param stream data input stream
     * @param objectName name of data object
     * @param containerName name of container
     * @return Response
     */
    @Path("/atomic_containers/{containerName}/objects/{objectName:.*}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "create new atomic object in container",
        description = "Permet de créer un nouvel objet atomique dans le container"
    )
    public Response putAtomicObject(
        InputStream stream,
        @PathParam(CONTAINER_NAME) String containerName,
        @PathParam(OBJECT_NAME) String objectName,
        @HeaderParam(GlobalDataRest.X_CONTENT_LENGTH) long size
    ) {
        try {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
                containerName,
                objectName
            );
            workspace.checkWorkspaceFileSanity(containerName, objectName);
            if (size < 0L) {
                throw new IllegalArgumentException("Invalid stream size " + size);
            }
            workspace.putAtomicObject(containerName, objectName, stream, size);
            return Response.status(Status.CREATED).entity(containerName + "/" + objectName).build();
        } catch (IllegalPathException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND.getMessage() + containerName, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final ContentAddressableStorageException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            StreamUtils.closeSilently(stream);
        }
    }

    /**
     * Deletes an objects in a container *
     *
     * @param containerName container name
     * @param objectName object name
     * @return Response
     */
    @Path("/containers/{containerName}/objects/{objectName:.*}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "delete an object in container",
        description = "Permet de supprimer un objet dans le container"
    )
    public Response deleteObject(
        @PathParam(CONTAINER_NAME) String containerName,
        @PathParam(OBJECT_NAME) String objectName
    ) {
        try {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
                containerName,
                objectName
            );
            workspace.checkWorkspaceFileSanity(containerName, objectName);
            workspace.deleteObject(containerName, objectName);
        } catch (IllegalPathException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND.getMessage() + containerName, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(Status.NO_CONTENT).build();
    }

    /**
     * gets an objects from a container in the workspace
     *
     * @param containerName name of container
     * @param objectName name of object
     * @return response
     */
    @Path("/containers/{containerName}/objects/{objectName:.*}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(
        summary = "retrieve an object in container",
        description = "Permet de récupérer un objet dans le container"
    )
    public Response getObject(
        @PathParam(CONTAINER_NAME) String containerName,
        @PathParam(OBJECT_NAME) String objectName,
        @HeaderParam(GlobalDataRest.X_CHUNK_OFFSET) Long chunkOffset,
        @HeaderParam(GlobalDataRest.X_CHUNK_MAX_SIZE) Long maxChunkSize
    ) {
        return getObjectResponse(containerName, objectName, chunkOffset, maxChunkSize);
    }

    /**
     * gets objects from a container in the workspace as a multiplexed stream
     *
     * @param containerName name of container
     * @param objectURIs the list of document Uris
     * @return response
     */
    @Path("/containers/{containerName}/objects")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "retrieve an object in container as multiplexed stream",
        description = "Permet de récupérer un objet dans le container"
    )
    public Response getBulkObjects(@PathParam(CONTAINER_NAME) String containerName, List<String> objectURIs) {
        // Check file existence & compute total stream size
        List<Long> fileSizes = new ArrayList<>();
        for (String objectId : objectURIs) {
            try {
                workspace.checkWorkspaceFileSanity(containerName, objectId);
                JsonNode objectInformation = workspace.getObjectInformation(containerName, objectId);
                fileSizes.add(objectInformation.get("size").asLong());
            } catch (IllegalPathException e) {
                LOGGER.error(e);
                return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
            } catch (final ContentAddressableStorageNotFoundException e) {
                LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND.getMessage() + containerName + " / " + objectId, e);
                return Response.status(Status.NOT_FOUND).build();
            } catch (final ContentAddressableStorageException e) {
                LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
        long totalStreamSize = MultiplexedStreamWriter.getTotalStreamSize(fileSizes);

        // Return response as StreamingOutput instance
        StreamingOutput streamingOutput = output -> {
            try {
                MultiplexedStreamWriter multiplexedStreamWriter = new MultiplexedStreamWriter(output);

                // Write object contents
                for (String objectURI : objectURIs) {
                    Response objResponse = null;
                    try {
                        objResponse = workspace.getObject(containerName, objectURI, null, null);

                        long size = Long.parseLong(
                            objResponse.getHeaderString(VitamHttpHeader.X_CONTENT_LENGTH.getName())
                        );
                        try (InputStream inputStream = (InputStream) objResponse.getEntity()) {
                            multiplexedStreamWriter.appendEntry(size, inputStream);
                        }
                    } finally {
                        if (objResponse != null) {
                            consumeAnyEntityAndClose(objResponse);
                        }
                    }
                }

                multiplexedStreamWriter.appendEndOfFile();
            } catch (Exception e) {
                LOGGER.error("Could not return bulk objects", e);
                throw new WebApplicationException("Could not return bulk objects", e);
            }
        };

        return Response.ok(streamingOutput).header(VitamHttpHeader.X_CONTENT_LENGTH.getName(), totalStreamSize).build();
    }

    /**
     * gets an objects from a container in the workspace
     *
     * @param containerName name of container
     * @param objectName name of object
     * @return Response
     * @throws IOException when there is an error of get object
     */
    @Path("/containers/{containerName}/objects/{objectName:.*}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "retrieve an object in container as json",
        description = "Permet de récupérer un objet dans le container"
    )
    public Response getObjectInformation(
        @PathParam(CONTAINER_NAME) String containerName,
        @PathParam(OBJECT_NAME) String objectName
    ) {
        JsonNode jsonResultNode;
        try {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
                containerName,
                objectName
            );
            workspace.checkWorkspaceFileSanity(containerName, objectName);
            jsonResultNode = workspace.getObjectInformation(containerName, objectName);
        } catch (final IllegalPathException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND.getMessage() + containerName, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final ContentAddressableStorageException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
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
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "check if object in container exists",
        description = "Permet de vérifier l'existance de l'objet dans le container"
    )
    public Response computeObjectDigest(
        @PathParam(CONTAINER_NAME) String containerName,
        @PathParam(OBJECT_NAME) String objectName,
        @HeaderParam(GlobalDataRest.X_DIGEST_ALGORITHM) String algo
    ) {
        try {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
                containerName,
                objectName
            );
            workspace.checkWorkspaceFileSanity(containerName, objectName);
        } catch (IllegalPathException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

        if (algo != null) {
            LOGGER.debug(GlobalDataRest.X_DIGEST_ALGORITHM + " : " + algo);
            String messageDigest;
            try {
                messageDigest = workspace.computeObjectDigest(containerName, objectName, DigestType.fromValue(algo));
            } catch (final ContentAddressableStorageNotFoundException e) {
                LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND.getMessage() + containerName, e);
                return Response.status(Status.NOT_FOUND).header(GlobalDataRest.X_DIGEST_ALGORITHM, algo).build();
            } catch (final ContentAddressableStorageException e) {
                LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .header(GlobalDataRest.X_DIGEST_ALGORITHM, algo)
                    .build();
            }

            return Response.status(Status.OK)
                .header(GlobalDataRest.X_DIGEST_ALGORITHM, algo)
                .header(GlobalDataRest.X_DIGEST, messageDigest)
                .build();
        } else {
            try {
                boolean exists = workspace.isExistingObject(containerName, objectName);

                if (exists) {
                    return Response.status(Status.OK).build();
                } else {
                    return Response.status(Status.NOT_FOUND).build();
                }
            } catch (final Exception e) {
                LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    private Response getObjectResponse(String containerName, String objectName, Long chunkOffset, Long maxChunkSize) {
        try {
            ParametersChecker.checkParameter(
                ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
                containerName,
                objectName
            );

            workspace.checkWorkspaceFileSanity(containerName, objectName);
            Response response = workspace.getObject(containerName, objectName, chunkOffset, maxChunkSize);

            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM);
            headers.put(
                VitamHttpHeader.X_CONTENT_LENGTH.getName(),
                response.getHeaderString(VitamHttpHeader.X_CONTENT_LENGTH.getName())
            );
            headers.put(
                VitamHttpHeader.X_CHUNK_LENGTH.getName(),
                response.getHeaderString(VitamHttpHeader.X_CHUNK_LENGTH.getName())
            );
            return new VitamAsyncInputStreamResponse(response, Status.OK, headers);
        } catch (IllegalPathException | IllegalArgumentException e) {
            LOGGER.error(e);
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final ContentAddressableStorageNotFoundException e) {
            LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND.getMessage() + containerName, e);
            return Response.status(Status.NOT_FOUND).build();
        } catch (final ContentAddressableStorageException e) {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }
}
