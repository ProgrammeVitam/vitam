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
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.storage.api.ContentAddressableStorage;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.common.storage.utils.MetadatasObjectResult;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageCompressedFileException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageZipException;
import fr.gouv.vitam.workspace.api.model.ContainerInformation;


/**
 * Workspace client which calls rest services
 * <p>
 * FIXME design : is it normal that the workspaceClient extends ContentAddressableStorage ?
 */
public class WorkspaceClient extends DefaultClient implements ContentAddressableStorage {

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


    @Override
    public void createContainer(String containerName)
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException {

        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, CONTAINERS + containerName, null,
                MediaType.APPLICATION_JSON_TYPE, false);
            if (Status.CREATED.getStatusCode() == response.getStatus()) {
                LOGGER.debug(containerName + ": " + Response.Status.CREATED.getReasonPhrase());
            } else if (Status.CONFLICT.getStatusCode() == response.getStatus()) {
                LOGGER.error(ErrorMessage.CONTAINER_ALREADY_EXIST.getMessage());
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

    @Override
    public void purgeContainer(String containerName) {
        // FIXME P1
    }


    @Override
    public void deleteContainer(String containerName, boolean recursive)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        Response response = null;
        try {
            MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_RECURSIVE, recursive);
            response = performRequest(HttpMethod.DELETE, CONTAINERS + containerName, headers,
                MediaType.APPLICATION_JSON_TYPE, false);

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

    @Override
    public boolean isExistingContainer(String containerName) throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        Response response = null;
        try {
            response = performRequest(HttpMethod.HEAD, CONTAINERS + containerName, null,
                MediaType.APPLICATION_JSON_TYPE, false);
            return Response.Status.OK.getStatusCode() == response.getStatus();
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }


    @Override
    public long countObjects(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, CONTAINERS + containerName + "/count", null,
                MediaType.APPLICATION_JSON_TYPE, false);
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

    @Override
    public void createFolder(String containerName, String folderName)
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);
        Response response = null;
        try {
            response = performRequest(HttpMethod.POST, CONTAINERS + containerName + FOLDERS + folderName, null,
                MediaType.APPLICATION_JSON_TYPE, false);
            if (Status.CREATED.getStatusCode() == response.getStatus()) {
                LOGGER.debug(containerName + "/" + folderName + ": " + Response.Status.CREATED.getReasonPhrase());
            } else if (Status.CONFLICT.getStatusCode() == response.getStatus()) {
                LOGGER.error(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
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

    @Override
    public void deleteFolder(String containerName, String folderName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.DELETE, CONTAINERS + containerName + FOLDERS + folderName, null,
                    MediaType.APPLICATION_JSON_TYPE, false);

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

    @Override
    public boolean isExistingFolder(String containerName, String folderName)
        throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);
        Response response = null;
        try {
            response = performRequest(HttpMethod.HEAD, CONTAINERS + containerName + FOLDERS + folderName, null,
                MediaType.APPLICATION_JSON_TYPE, false);
            return Response.Status.OK.getStatusCode() == response.getStatus();
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
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

    @Override
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

    @Override
    public Response getObjectAsync(String containerName, String objectName, AsyncResponse asyncResponse)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, CONTAINERS + containerName + OBJECTS + objectName, null,
                MediaType.APPLICATION_OCTET_STREAM_TYPE);
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
                final ResponseBuilder responseBuilder =
                    Response.status(response.getStatus()).type(MediaType.APPLICATION_OCTET_STREAM);
                helper.writeResponse(responseBuilder);
                return response;
            }
            if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
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

    @Override
    public void deleteObject(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {

        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.DELETE, CONTAINERS + containerName + OBJECTS + objectName, null,
                    MediaType.APPLICATION_JSON_TYPE, false);

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

    @Override
    public boolean isExistingObject(String containerName, String objectName)
        throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        Response response = null;
        try {
            response = performRequest(HttpMethod.HEAD, CONTAINERS + containerName + OBJECTS + objectName, null,
                MediaType.APPLICATION_JSON_TYPE, false);
            return Response.Status.OK.getStatusCode() == response.getStatus();
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public List<URI> getListUriDigitalObjectFromFolder(String containerName, String folderName)
        throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, folderName);
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, CONTAINERS + containerName + FOLDERS + folderName, null,
                MediaType.APPLICATION_JSON_TYPE, false);

            if (response != null && Response.Status.OK.getStatusCode() == response.getStatus()) {
                return response.readEntity(new GenericType<List<URI>>() {
                    // Empty
                });
            } else {
                if (response != null) {
                    LOGGER.error(response.getStatusInfo().getReasonPhrase());
                }
                return Collections.<URI>emptyList();
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public void uncompressObject(String containerName, String folderName, String archiveType,
        InputStream inputStreamObject)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageAlreadyExistException, ContentAddressableStorageCompressedFileException,
        ContentAddressableStorageZipException {
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
                        LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
                        throw new ContentAddressableStorageNotFoundException(
                            ErrorMessage.OBJECT_NOT_FOUND.getMessage());
                    } else if (Status.CONFLICT.getStatusCode() == response.getStatus()) {
                        LOGGER.error(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
                        throw new ContentAddressableStorageAlreadyExistException(
                            ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
                    } else if (Status.BAD_REQUEST.getStatusCode() == response.getStatus() &&
                        "application/json".equals(response.getHeaderString("Content-Type"))) {
                        LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                        throw new ContentAddressableStorageZipException(
                            ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                    } else {
                        LOGGER.error(response.getStatusInfo().getReasonPhrase());
                        throw new ContentAddressableStorageServerException(
                            ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
                    }
                } catch (final VitamClientInternalException e) {
                    LOGGER.error(INTERNAL_SERVER_ERROR2, e);
                    throw new ContentAddressableStorageServerException(e);
                } finally {
                    consumeAnyEntityAndClose(response);
                }

            } else {
                LOGGER.error(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
                throw new ContentAddressableStorageAlreadyExistException(
                    ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
            }

        } else {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
            throw new ContentAddressableStorageNotFoundException(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
        }
    }

    @Override
    public String computeObjectDigest(String containerName, String objectName, DigestType algo)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageException {

        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName, algo);

        Response response = null;
        try {
            final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_DIGEST_ALGORITHM, algo.getName());
            response =
                performRequest(HttpMethod.HEAD, CONTAINERS + containerName + OBJECTS + objectName, null,
                    MediaType.APPLICATION_JSON_TYPE, false);

            if (Response.Status.OK.getStatusCode() == response.getStatus()) {
                return response.getHeaderString(GlobalDataRest.X_DIGEST);
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
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public ContainerInformation getContainerInformation(String containerName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.GET, CONTAINERS + containerName, null,
                    MediaType.APPLICATION_JSON_TYPE, false);
            if (Response.Status.OK.getStatusCode() == response.getStatus()) {
                return response.readEntity(ContainerInformation.class);
            } else {
                LOGGER.error(response.getStatusInfo().getReasonPhrase());
                throw new ContentAddressableStorageNotFoundException(response.getStatusInfo().getReasonPhrase());
            }
        } catch (final VitamClientInternalException e) {
            LOGGER.error(INTERNAL_SERVER_ERROR2, e);
            throw new ContentAddressableStorageServerException(e);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public JsonNode getObjectInformation(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName, objectName);
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.GET, CONTAINERS + containerName + OBJECTS + objectName, null,
                    MediaType.APPLICATION_JSON_TYPE, false);

            if (Response.Status.OK.getStatusCode() == response.getStatus()) {
                return response.readEntity(JsonNode.class);
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
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public boolean checkObject(String containerName, String objectId, String digest,
        DigestType digestAlgorithm) throws ContentAddressableStorageException {
        String offerDigest = computeObjectDigest(containerName, objectId, digestAlgorithm);
        return offerDigest.equals(digest);
    }


    @Override
    public MetadatasObjectResult getObjectMetadatas(String tenantId, String type, String objectId) {
        // FIXME implémente dans workspace 
        return new MetadatasObjectResult();
    }

}
