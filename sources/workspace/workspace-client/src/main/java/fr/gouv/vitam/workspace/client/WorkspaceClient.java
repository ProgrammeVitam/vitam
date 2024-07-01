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
package fr.gouv.vitam.workspace.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.storage.constants.ErrorMessage;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageBadRequestException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotAcceptableException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageZipException;
import fr.gouv.vitam.workspace.api.exception.ZipFilesNameNotAllowedException;
import fr.gouv.vitam.workspace.api.model.FileParams;
import fr.gouv.vitam.workspace.api.model.TimeToLive;
import fr.gouv.vitam.workspace.common.CompressInformation;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.GlobalDataRest.X_DIGEST;
import static fr.gouv.vitam.common.GlobalDataRest.X_DIGEST_ALGORITHM;
import static fr.gouv.vitam.common.GlobalDataRest.X_RECURSIVE;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.delete;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.get;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.head;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.post;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.put;
import static fr.gouv.vitam.common.model.WorkspaceConstants.FREESPACE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

public class WorkspaceClient extends DefaultClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkspaceClient.class);

    private static final String OBJECTS = "/objects/";
    private static final String FOLDERS = "/folders/";
    private static final String OLD_FILES = "/old_files";
    private static final String CONTAINERS = "/containers/";
    private static final String ATOMIC_CONTAINERS = "/atomic_containers/";
    private static final String FILES_WITH_PARAMS = "/filesWithParams";
    private static final String FREESPACE_URL = "/" + FREESPACE;

    private static final GenericType<List<URI>> URI_LIST_TYPE = new GenericType<>() {};
    private static final GenericType<Map<String, FileParams>> FILES_MAP_TYPE = new GenericType<>() {};

    WorkspaceClient(WorkspaceClientFactory factory) {
        super(factory);
    }

    public JsonNode getFreespacePercent() throws VitamClientException {
        try (Response response = make(get().withPath(FREESPACE_URL).withJsonAccept())) {
            Response.StatusType status = response.getStatusInfo();
            if (SUCCESSFUL.equals(status.getFamily())) {
                return JsonHandler.getFromInputStream(response.readEntity(InputStream.class));
            }
            throw new VitamClientException("Could not retrieve workspace free space");
        } catch (VitamClientInternalException | InvalidParseOperationException e) {
            throw new VitamClientException(e);
        }
    }

    public void createContainer(String containerName) throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );
        try (Response response = make(post().withPath(CONTAINERS + containerName).withJsonAccept())) {
            check(response);
        } catch (
            VitamClientInternalException
            | ContentAddressableStorageNotFoundException
            | ContentAddressableStorageAlreadyExistException
            | ContentAddressableStorageNotAcceptableException
            | ContentAddressableStorageBadRequestException e
        ) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    public void deleteContainer(String containerName, boolean deleteRecursive)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );
        try (
            Response response = make(
                delete().withPath(CONTAINERS + containerName).withHeader(X_RECURSIVE, deleteRecursive).withJsonAccept()
            )
        ) {
            check(response);
        } catch (
            VitamClientInternalException
            | ContentAddressableStorageAlreadyExistException
            | ContentAddressableStorageNotAcceptableException
            | ContentAddressableStorageBadRequestException e
        ) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    public boolean isExistingContainer(String containerName) throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );
        try (Response response = make(head().withPath(CONTAINERS + containerName).withJsonAccept())) {
            check(response);
            return true;
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageAlreadyExistException e) {
            LOGGER.info(e);
            return false;
        } catch (
            VitamClientInternalException
            | ContentAddressableStorageNotAcceptableException
            | ContentAddressableStorageBadRequestException e
        ) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    public void createFolder(String containerName, String folderName)
        throws ContentAddressableStorageAlreadyExistException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            folderName
        );
        try (
            Response response = make(
                post().withPath(CONTAINERS + containerName + FOLDERS + folderName).withJsonAccept()
            )
        ) {
            check(response);
        } catch (
            ContentAddressableStorageNotFoundException
            | VitamClientInternalException
            | ContentAddressableStorageNotAcceptableException
            | ContentAddressableStorageBadRequestException e
        ) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    public void deleteFolder(String containerName, String folderName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            folderName
        );
        try (
            Response response = make(
                delete().withPath(CONTAINERS + containerName + FOLDERS + folderName).withJsonAccept()
            )
        ) {
            check(response);
        } catch (
            VitamClientInternalException
            | ContentAddressableStorageAlreadyExistException
            | ContentAddressableStorageNotAcceptableException
            | ContentAddressableStorageBadRequestException e
        ) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    public boolean isExistingFolder(String containerName, String folderName)
        throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            folderName
        );
        try (
            Response response = make(
                head().withPath(CONTAINERS + containerName + FOLDERS + folderName).withJsonAccept()
            )
        ) {
            check(response);
            return true;
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageBadRequestException e) {
            LOGGER.info(e);
            return false;
        } catch (
            VitamClientInternalException
            | ContentAddressableStorageNotAcceptableException
            | ContentAddressableStorageAlreadyExistException e
        ) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    public void putObject(String containerName, String objectName, InputStream stream)
        throws ContentAddressableStorageServerException {
        this.putObject(containerName, objectName, (Object) stream);
    }

    public void putObject(String containerName, String objectName, Object object)
        throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectName
        );
        VitamRequestBuilder request = post()
            .withPath(CONTAINERS + containerName + OBJECTS + objectName)
            .withBody(object)
            .withContentType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .withJsonAccept();
        try (Response response = make(request)) {
            check(response);
        } catch (
            VitamClientInternalException
            | ContentAddressableStorageNotFoundException
            | ContentAddressableStorageAlreadyExistException
            | ContentAddressableStorageNotAcceptableException
            | ContentAddressableStorageBadRequestException e
        ) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    public void putAtomicObject(String containerName, String objectName, InputStream stream, long size)
        throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectName
        );
        if (size < 0) {
            throw new IllegalArgumentException("Invalid size " + size);
        }
        VitamRequestBuilder request = post()
            .withPath(ATOMIC_CONTAINERS + containerName + OBJECTS + objectName)
            .withHeader(GlobalDataRest.X_CONTENT_LENGTH, size)
            .withBody(stream)
            .withContentType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .withJsonAccept();
        try (Response response = make(request)) {
            check(response);
        } catch (
            VitamClientInternalException
            | ContentAddressableStorageNotFoundException
            | ContentAddressableStorageAlreadyExistException
            | ContentAddressableStorageNotAcceptableException
            | ContentAddressableStorageBadRequestException e
        ) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    public Response getObject(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectName
        );
        Response response = null;
        try {
            response = make(get().withPath(CONTAINERS + containerName + OBJECTS + objectName).withOctetAccept());
            check(response);
            return response;
        } catch (
            VitamClientInternalException
            | ContentAddressableStorageAlreadyExistException
            | ContentAddressableStorageNotAcceptableException
            | ContentAddressableStorageBadRequestException e
        ) {
            throw new ContentAddressableStorageServerException(e);
        } finally {
            if (response != null && !SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                response.close();
            }
        }
    }

    public Response getObject(String containerName, String objectName, long offset, Long maxChunkSize)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectName
        );
        Response response = null;
        try {
            VitamRequestBuilder request = get()
                .withPath(CONTAINERS + containerName + OBJECTS + objectName)
                .withHeader(GlobalDataRest.X_CHUNK_OFFSET, offset)
                .withHeaderIgnoreNull(GlobalDataRest.X_CHUNK_MAX_SIZE, maxChunkSize)
                .withOctetAccept();
            response = make(request);
            check(response);
            return response;
        } catch (
            VitamClientInternalException
            | ContentAddressableStorageAlreadyExistException
            | ContentAddressableStorageNotAcceptableException
            | ContentAddressableStorageBadRequestException e
        ) {
            throw new ContentAddressableStorageServerException(e);
        } finally {
            if (response != null && !SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                response.close();
            }
        }
    }

    public Response bulkGetObjects(String containerName, List<String> objectURIs)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectURIs
        );
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            objectURIs.toArray()
        );
        Response response = null;
        try {
            response = make(
                get().withPath(CONTAINERS + containerName + "/objects").withBody(objectURIs).withJsonOctet()
            );
            check(response);
            return response;
        } catch (
            VitamClientInternalException
            | ContentAddressableStorageAlreadyExistException
            | ContentAddressableStorageNotAcceptableException
            | ContentAddressableStorageBadRequestException e
        ) {
            throw new ContentAddressableStorageServerException(e);
        } finally {
            if (response != null && !SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                response.close();
            }
        }
    }

    public void deleteObject(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectName
        );
        try (
            Response response = make(
                delete().withPath(CONTAINERS + containerName + OBJECTS + objectName).withJsonAccept()
            )
        ) {
            check(response);
        } catch (
            VitamClientInternalException
            | ContentAddressableStorageAlreadyExistException
            | ContentAddressableStorageNotAcceptableException
            | ContentAddressableStorageBadRequestException e
        ) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    public boolean isExistingObject(String containerName, String objectName)
        throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectName
        );
        try (
            Response response = make(
                head().withPath(CONTAINERS + containerName + OBJECTS + objectName).withJsonAccept()
            )
        ) {
            check(response);
            return true;
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageAlreadyExistException e) {
            LOGGER.info(e);
            return false;
        } catch (
            VitamClientInternalException
            | ContentAddressableStorageNotAcceptableException
            | ContentAddressableStorageBadRequestException e
        ) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    public RequestResponse<List<URI>> getListUriDigitalObjectFromFolder(String containerName, String folderName)
        throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            folderName
        );
        try (
            Response response = make(get().withPath(CONTAINERS + containerName + FOLDERS + folderName).withJsonAccept())
        ) {
            check(response);
            List<URI> uris = response.readEntity(URI_LIST_TYPE);
            return new RequestResponseOK().addResult(uris == null ? Collections.<URI>emptyList() : uris);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageAlreadyExistException e) {
            LOGGER.info(e);
            return new RequestResponseOK().addResult(Collections.<URI>emptyList());
        } catch (
            VitamClientInternalException
            | ContentAddressableStorageNotAcceptableException
            | ContentAddressableStorageBadRequestException e
        ) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    public RequestResponse<Map<String, FileParams>> getFilesWithParamsFromFolder(
        String containerName,
        String folderName
    ) throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            folderName
        );
        try (
            Response response = make(
                get().withPath(CONTAINERS + containerName + FOLDERS + folderName + FILES_WITH_PARAMS).withJsonAccept()
            )
        ) {
            check(response);
            Map filesMap = response.readEntity(FILES_MAP_TYPE);
            return new RequestResponseOK().addResult(filesMap == null ? Collections.emptyMap() : filesMap);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageAlreadyExistException e) {
            LOGGER.info(e);
            return new RequestResponseOK().addResult(Collections.emptyMap());
        } catch (
            VitamClientInternalException
            | ContentAddressableStorageNotAcceptableException
            | ContentAddressableStorageBadRequestException e
        ) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    public void compress(String containerName, CompressInformation compressInformation)
        throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(),
            containerName
        );
        try (
            Response response = make(
                post().withPath(CONTAINERS + containerName).withBody(compressInformation).withJson()
            )
        ) {
            check(response);
        } catch (
            VitamClientInternalException
            | ContentAddressableStorageNotFoundException
            | ContentAddressableStorageAlreadyExistException
            | ContentAddressableStorageNotAcceptableException
            | ContentAddressableStorageBadRequestException e
        ) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    public void uncompressObject(
        String containerName,
        String folderName,
        String archiveType,
        InputStream inputStreamObject
    ) throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            folderName,
            archiveType
        );
        if (!isExistingContainer(containerName)) {
            LOGGER.debug(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
            throw new ContentAddressableStorageNotFoundException(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
        }
        if (isExistingFolder(containerName, folderName)) {
            LOGGER.warn(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
            throw new ContentAddressableStorageAlreadyExistException(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
        }
        VitamRequestBuilder request = put()
            .withPath(CONTAINERS + containerName + FOLDERS + folderName)
            .withBody(inputStreamObject)
            .withContentType(CommonMediaType.valueOf(archiveType))
            .withJsonAccept()
            .withChunckedMode(true);
        try (Response response = make(request)) {
            check(response);
        } catch (ContentAddressableStorageBadRequestException e) {
            throw new ContentAddressableStorageZipException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage(), e);
        } catch (ContentAddressableStorageNotAcceptableException e) {
            throw new ZipFilesNameNotAllowedException("File or folder name not allowed", e);
        } catch (VitamClientInternalException e) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    public String computeObjectDigest(String containerName, String objectName, DigestType algo)
        throws ContentAddressableStorageException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectName,
            algo
        );
        try (
            Response response = make(
                head()
                    .withPath(CONTAINERS + containerName + OBJECTS + objectName)
                    .withHeader(X_DIGEST_ALGORITHM, algo.getName())
                    .withJsonAccept()
            )
        ) {
            check(response);
            return response.getHeaderString(X_DIGEST);
        } catch (VitamClientInternalException e) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    RequestResponse<JsonNode> getObjectInformation(String containerName, String objectName)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        ParametersChecker.checkParameter(
            ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(),
            containerName,
            objectName
        );
        try (
            Response response = make(get().withPath(CONTAINERS + containerName + OBJECTS + objectName).withJsonAccept())
        ) {
            check(response);
            return new RequestResponseOK().addResult(response.readEntity(JsonNode.class));
        } catch (
            VitamClientInternalException
            | ContentAddressableStorageAlreadyExistException
            | ContentAddressableStorageNotAcceptableException
            | ContentAddressableStorageBadRequestException e
        ) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    boolean checkObject(String containerName, String objectId, String digest, DigestType digestAlgorithm)
        throws ContentAddressableStorageException {
        return computeObjectDigest(containerName, objectId, digestAlgorithm).equals(digest);
    }

    public void purgeOldFilesInContainer(String containerName, TimeToLive timeToLive)
        throws ContentAddressableStorageServerException {
        ParametersChecker.checkParameter("Mandatory parameters", containerName, timeToLive);
        try (
            Response response = make(
                delete().withPath(CONTAINERS + containerName + OLD_FILES).withBody(timeToLive).withJson()
            )
        ) {
            check(response);
        } catch (
            ContentAddressableStorageNotAcceptableException
            | ContentAddressableStorageBadRequestException
            | ContentAddressableStorageNotFoundException
            | ContentAddressableStorageAlreadyExistException
            | VitamClientInternalException e
        ) {
            throw new ContentAddressableStorageServerException(e);
        }
    }

    private void check(Response response)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException, ContentAddressableStorageAlreadyExistException, ContentAddressableStorageNotAcceptableException, ContentAddressableStorageBadRequestException {
        Status status = response.getStatusInfo().toEnum();
        if (SUCCESSFUL.equals(status.getFamily())) {
            return;
        }

        switch (status) {
            case NOT_FOUND:
                throw new ContentAddressableStorageNotFoundException(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
            case CONFLICT:
                throw new ContentAddressableStorageAlreadyExistException(
                    ErrorMessage.FOLDER_ALREADY_EXIST.getMessage()
                );
            case BAD_REQUEST:
                throw new ContentAddressableStorageBadRequestException(ErrorMessage.BAD_REQUEST.getMessage());
            case NOT_ACCEPTABLE:
                throw new ContentAddressableStorageNotAcceptableException(
                    ErrorMessage.NOT_ACCEPTABLE_FILES.getMessage()
                );
            default:
                throw new ContentAddressableStorageServerException(
                    String.format(
                        "Response in error with status '%d' and reason '%s'.",
                        status.getStatusCode(),
                        status.getReasonPhrase()
                    )
                );
        }
    }
}
