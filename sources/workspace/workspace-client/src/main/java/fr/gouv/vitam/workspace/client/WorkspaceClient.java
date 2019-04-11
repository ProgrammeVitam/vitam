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
package fr.gouv.vitam.workspace.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.MetadatasObject;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.storage.ContainerInformation;
import fr.gouv.vitam.common.storage.cas.container.api.MetadatasStorageObject;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageZipException;
import fr.gouv.vitam.workspace.api.exception.ZipFilesNameNotAllowedException;
import fr.gouv.vitam.workspace.common.CompressInformation;


/**
 * Workspace client which calls rest services
 */
public class WorkspaceClient extends DefaultClient {

    private static final String INTERNAL_SERVER_ERROR2 = "Internal Server Error";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkspaceClient.class);
    private static final String OBJECTS = "/objects/";
    private static final String FOLDERS = "/folders/";
    private static final String CONTAINERS = "/containers/";

    /**
     * Instantiates a workspace client with a factory
     *
     * @param factory
     */
    WorkspaceClient(WorkspaceClientFactory factory) {
        super(factory);
    }

    /**
     * Create container
     * 
     * @param containerName the container name
     * @throws ContentAddressableStorageAlreadyExistException in case the container already exists
     * @throws ContentAddressableStorageServerException in case of any other error
     */
    public void createContainer(String containerName)
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException {

        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, CONTAINERS + containerName, null,
                MediaType.APPLICATION_JSON_TYPE);
            if (Status.CREATED.getStatusCode() == response.getStatus()) {
                LOGGER.debug(containerName + ": " + Response.Status.CREATED.getReasonPhrase());
            } else if (Status.CONFLICT.getStatusCode() == response.getStatus()) {
                LOGGER.info(ErrorMessage.CONTAINER_ALREADY_EXIST.getMessage());
                throw new ContentAddressableStorageAlreadyExistException(
                    ErrorMessage.CONTAINER_ALREADY_EXIST.getMessage());
            } else {
                LOGGER.error(response.getStatusInfo().getReasonPhrase());
                throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    /**
     * Delete container
     * 
     * @param containerName the container name
     * @param recursive true if should be deleted recursively
     * @throws ContentAddressableStorageNotFoundException if the container could not be found
     * @throws ContentAddressableStorageServerException in case of any other error
     */
    public void deleteContainer(String containerName, boolean recursive)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        Response response = null;
        try {
            MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_RECURSIVE, recursive);
            response = performRequest(HttpMethod.DELETE, CONTAINERS + containerName, headers,
                MediaType.APPLICATION_JSON_TYPE);

            if (Response.Status.NO_CONTENT.getStatusCode() == response.getStatus()) {
                LOGGER.debug(containerName + ": " + Response.Status.NO_CONTENT.getReasonPhrase());
            } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
            } else {
                LOGGER.error(response.getStatusInfo().getReasonPhrase());
                throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Check if container exists
     * 
     * @param containerName the container name
     * @return true if it exists, false if not
     * @throws ContentAddressableStorageServerException in case of any error
     */
    public boolean isExistingContainer(String containerName) throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        Response response = null;
        try {
            response = performRequest(HttpMethod.HEAD, CONTAINERS + containerName, null,
                MediaType.APPLICATION_JSON_TYPE);
            return Response.Status.OK.getStatusCode() == response.getStatus();
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Count the number of object in a container
     * 
     * @param containerName the container name
     * @return the number of objects
     * @throws ContentAddressableStorageNotFoundException in case the container could not be found
     * @throws ContentAddressableStorageServerException in case of any other error
     */
    public long countObjects(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, CONTAINERS + containerName + "/count", null,
                MediaType.APPLICATION_JSON_TYPE);
            if (Response.Status.OK.getStatusCode() == response.getStatus()) {
                JsonNode node = response.readEntity(JsonNode.class);
                return node.get("objectNumber").asLong();
            } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                LOGGER.error(ErrorMessage.FOLDER_NOT_FOUND.getMessage());
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.FOLDER_NOT_FOUND.getMessage());
            } else {
                LOGGER.error(response.getStatusInfo().getReasonPhrase());
                throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Create a folder
     * 
     * @param containerName the container name
     * @param folderName the folder name
     * @throws ContentAddressableStorageAlreadyExistException in case the folder already exists
     * @throws ContentAddressableStorageServerException in case of any other error
     */
    public void createFolder(String containerName, String folderName)
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, CONTAINERS + containerName + FOLDERS + folderName, null,
                MediaType.APPLICATION_JSON_TYPE);
            if (Status.CREATED.getStatusCode() == response.getStatus()) {
                LOGGER.debug(containerName + "/" + folderName + ": " + Response.Status.CREATED.getReasonPhrase());
            } else if (Status.CONFLICT.getStatusCode() == response.getStatus()) {
                LOGGER.warn(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
                throw new ContentAddressableStorageAlreadyExistException(
                    ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
            } else {
                LOGGER.error(response.getStatusInfo().getReasonPhrase());
                throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Delete folder
     * 
     * @param containerName the container name
     * @param folderName the folder name
     * @throws ContentAddressableStorageNotFoundException if the folder does not exist
     * @throws ContentAddressableStorageServerException in case of any other error
     */
    public void deleteFolder(String containerName, String folderName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.DELETE, CONTAINERS + containerName + FOLDERS + folderName, null,
                    MediaType.APPLICATION_JSON_TYPE);

            if (Response.Status.NO_CONTENT.getStatusCode() == response.getStatus()) {
                LOGGER.debug(containerName + "/" + folderName + ": " + Response.Status.NO_CONTENT.getReasonPhrase());
            } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                LOGGER.error(ErrorMessage.FOLDER_NOT_FOUND.getMessage());
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.FOLDER_NOT_FOUND.getMessage());
            } else {
                LOGGER.error(response.getStatusInfo().getReasonPhrase());
                throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Check if folder exists
     * 
     * @param containerName the container name
     * @param folderName the folder name
     * @return true if it exists, false if not
     * @throws ContentAddressableStorageServerException in case of error
     */
    public boolean isExistingFolder(String containerName, String folderName)
        throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);
        Response response = null;
        try {
            response = performRequest(HttpMethod.HEAD, CONTAINERS + containerName + FOLDERS + folderName, null,
                MediaType.APPLICATION_JSON_TYPE);
            return Response.Status.OK.getStatusCode() == response.getStatus();
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Put object
     * 
     * @param containerName the container name
     * @param objectName the object name
     * @param stream the input stream to be put
     * @throws ContentAddressableStorageServerException in case of error
     */
    public void putObject(String containerName, String objectName, InputStream stream)
        throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);

        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, CONTAINERS + containerName + OBJECTS + objectName, null, stream,
                MediaType.APPLICATION_OCTET_STREAM_TYPE,
                MediaType.APPLICATION_JSON_TYPE);

            if (Status.CREATED.getStatusCode() == response.getStatus()) {
                LOGGER.debug(containerName + "/" + objectName + ": " + Response.Status.CREATED.getReasonPhrase());
            } else {
                LOGGER.error(response.getStatusInfo().getReasonPhrase());
                throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }

    }

    /**
     * Get Object
     * 
     * @param containerName the container name
     * @param objectName the object name
     * @return the original Response
     * @throws ContentAddressableStorageNotFoundException in case the object couldnt be found
     * @throws ContentAddressableStorageServerException in case of any other error
     */
    public Response getObject(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, CONTAINERS + containerName + OBJECTS + objectName, null,
                MediaType.APPLICATION_OCTET_STREAM_TYPE);
            if (Response.Status.OK.getStatusCode() == response.getStatus()) {
                return response;
            } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
            } else {
                LOGGER.error(response.getStatusInfo().getReasonPhrase());
                throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);

        } finally {
            if (response != null && response.getStatus() != Status.OK.getStatusCode()) {
                consumeAnyEntityAndClose(response);
            }
        }
    }

    /**
     * Delete object
     * 
     * @param containerName the name of the container
     * @param objectName the name of object
     * @throws ContentAddressableStorageNotFoundException in case the object could not be found
     * @throws ContentAddressableStorageServerException in case of any other error
     */
    public void deleteObject(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {

        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.DELETE, CONTAINERS + containerName + OBJECTS + objectName, null,
                    MediaType.APPLICATION_JSON_TYPE);

            if (Response.Status.NO_CONTENT.getStatusCode() == response.getStatus()) {
                LOGGER.debug(containerName + "/" + objectName + ": " + Response.Status.NO_CONTENT.getReasonPhrase());
            } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
            } else {
                LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Check if obejct is existing
     * 
     * @param containerName the container name
     * @param objectName the object name
     * @return true if it exist, false if not
     * @throws ContentAddressableStorageServerException in case of error
     */
    public boolean isExistingObject(String containerName, String objectName)
        throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        Response response = null;
        try {
            response = performRequest(HttpMethod.HEAD, CONTAINERS + containerName + OBJECTS + objectName, null,
                MediaType.APPLICATION_JSON_TYPE);
            return Response.Status.OK.getStatusCode() == response.getStatus();
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Get List of digital object
     * 
     * @param containerName the container name
     * @param folderName the folder name
     * @return a list of URI
     * @throws ContentAddressableStorageServerException in case of error
     */
    public RequestResponse<List<URI>> getListUriDigitalObjectFromFolder(String containerName, String folderName)
        throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, CONTAINERS + containerName + FOLDERS + folderName, null,
                MediaType.APPLICATION_JSON_TYPE);

            if (response != null && Response.Status.OK.getStatusCode() == response.getStatus()) {
                return new RequestResponseOK().addResult(response.readEntity(new GenericType<List<URI>>() {
                    // Empty
                }));
            } else {
                if (response != null) {
                    LOGGER.debug(response.getStatusInfo().getReasonPhrase());
                }
                return new RequestResponseOK().addResult(Collections.<URI>emptyList());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.debug(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Compress
     * 
     * @param containerName the container name
     * @param compressInformation information on compression
     * @throws ContentAddressableStorageServerException
     */
    public void compress(String containerName, CompressInformation compressInformation)
        throws ContentAddressableStorageServerException {
        ParametersChecker
            .checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);

        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, CONTAINERS + containerName, null,
                compressInformation, MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
        } catch (VitamClientInternalException e) {
            LOGGER.debug(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }


    }

    /**
     * Uncompress object
     * 
     * @param containerName the name of the container
     * @param folderName the folder name
     * @param archiveType the archive type
     * @param inputStreamObject the input stream to be uncompress
     * @throws ContentAddressableStorageException in case of error
     */
    public void uncompressObject(String containerName, String folderName, String archiveType,
        InputStream inputStreamObject)
        throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName, archiveType);
        LOGGER.debug("-- Begin uncompress object in container:" + containerName + "/archiveType:" +
            archiveType);

        if (isExistingContainer(containerName)) {
            if (!isExistingFolder(containerName, folderName)) {
                Response response = null;
                try {
                    response =
                        performRequest(HttpMethod.PUT, CONTAINERS + containerName + FOLDERS + folderName, null,
                            inputStreamObject, CommonMediaType.valueOf(archiveType),
                            MediaType.APPLICATION_JSON_TYPE, true);

                    if (Response.Status.CREATED.getStatusCode() == response.getStatus()) {
                        LOGGER.debug(containerName + File.separator + folderName + " : " +
                            Response.Status.CREATED.getReasonPhrase());
                    } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                        LOGGER.debug(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
                        throw new ContentAddressableStorageNotFoundException(
                            ErrorMessage.OBJECT_NOT_FOUND.getMessage());
                    } else if (Status.CONFLICT.getStatusCode() == response.getStatus()) {
                        LOGGER.warn(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
                        throw new ContentAddressableStorageAlreadyExistException(
                            ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
                    } else if (Status.NOT_ACCEPTABLE.getStatusCode() == response.getStatus()) {
                        LOGGER.debug(ErrorMessage.FOLDER_OR_FILE_NAME_NOT_ALLOWED.getMessage());
                        RequestResponse requestResponse = RequestResponse.parseFromResponse(response);
                        if (!requestResponse.isOk()) {
                            VitamError vitamError = (VitamError) requestResponse;
                            throw new ZipFilesNameNotAllowedException(vitamError.getMessage());
                        }

                        throw new ZipFilesNameNotAllowedException("File or folder name not allowed");

                    } else if (Status.BAD_REQUEST.getStatusCode() == response.getStatus() &&
                        "application/json".equals(response.getHeaderString("Content-Type"))) {
                        LOGGER.warn(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                        throw new ContentAddressableStorageZipException(
                            ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                    } else {
                        LOGGER.debug(response.getStatusInfo().getReasonPhrase());
                        throw new ContentAddressableStorageServerException(
                            ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                    }
                } catch (final VitamClientInternalException e) {
                    LOGGER.debug(INTERNAL_SERVER_ERROR2, e);
                    throw new ContentAddressableStorageServerException(e);
                } finally {
                    consumeAnyEntityAndClose(response);
                }

            } else {
                LOGGER.warn(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
                throw new ContentAddressableStorageAlreadyExistException(
                    ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
            }

        } else {
            LOGGER.debug(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
            throw new ContentAddressableStorageNotFoundException(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
        }
    }

    /**
     * Compute digest for an object
     * 
     * @param containerName the container name
     * @param objectName the object name
     * @param algo the digest type
     * @return the computed digest
     * @throws ContentAddressableStorageException in case of error
     */
    public RequestResponse<String> computeObjectDigest(String containerName, String objectName, DigestType algo)
        throws ContentAddressableStorageException {

        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName, algo);

        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_DIGEST_ALGORITHM, algo.getName());
            response =
                performRequest(HttpMethod.HEAD, CONTAINERS + containerName + OBJECTS + objectName, null,
                    MediaType.APPLICATION_JSON_TYPE);

            if (Response.Status.OK.getStatusCode() == response.getStatus()) {
                return new RequestResponseOK().addResult(response.getHeaderString(GlobalDataRest.X_DIGEST));
            } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                LOGGER.debug(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
            } else {
                LOGGER.debug(response.getStatusInfo().getReasonPhrase());
                throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.debug(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * 
     * @param containerName the container name
     * @return information on the container
     * @throws ContentAddressableStorageNotFoundException in case the container could not be found
     * @throws ContentAddressableStorageServerException in case of any other error
     */
    public RequestResponse<ContainerInformation> getContainerInformation(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.GET, CONTAINERS + containerName, null,
                    MediaType.APPLICATION_JSON_TYPE);
            if (Response.Status.OK.getStatusCode() == response.getStatus()) {
                return new RequestResponseOK().addResult(response.readEntity(ContainerInformation.class));
            } else {
                LOGGER.debug(response.getStatusInfo().getReasonPhrase());
                throw new ContentAddressableStorageNotFoundException(response.getStatusInfo().getReasonPhrase());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.debug(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Get Object Information
     * 
     * @param containerName the container name
     * @param objectName the object name
     * @return object information
     * @throws ContentAddressableStorageNotFoundException in case the object couldnt be found
     * @throws ContentAddressableStorageServerException in case of any other error
     */
    public RequestResponse<JsonNode> getObjectInformation(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.GET, CONTAINERS + containerName + OBJECTS + objectName, null,
                    MediaType.APPLICATION_JSON_TYPE);

            if (Response.Status.OK.getStatusCode() == response.getStatus()) {
                return new RequestResponseOK().addResult(response.readEntity(JsonNode.class));
            } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
                LOGGER.debug(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
            } else {
                LOGGER.debug(response.getStatusInfo().getReasonPhrase());
                throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.debug(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    /**
     * Check object
     * 
     * @param containerName the name of the container
     * @param objectId the object id
     * @param digest the digest
     * @param digestAlgorithm the digest algorithm
     * @return true if the object matches
     * @throws ContentAddressableStorageException in case the object couldnt be found
     */
    public boolean checkObject(String containerName, String objectId, String digest,
        DigestType digestAlgorithm)
        throws ContentAddressableStorageException {
        String offerDigest = computeObjectDigest(containerName, objectId, digestAlgorithm)
            .toJsonNode().get("$results").get(0).asText();
        return offerDigest.equals(digest);
    }
}
