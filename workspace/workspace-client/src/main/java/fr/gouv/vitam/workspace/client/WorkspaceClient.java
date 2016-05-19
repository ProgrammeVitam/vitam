package fr.gouv.vitam.workspace.client;

import java.io.InputStream;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import fr.gouv.vitam.workspace.api.ContentAddressableStorage;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.common.Entry;
import fr.gouv.vitam.workspace.common.ErrorMessage;
import fr.gouv.vitam.workspace.common.ParametersChecker;

// TODO REVIEW missing licence header
// TODO REVIEW missing comments
// TODO REVIEW missing explicit exception throwing in signatures

public class WorkspaceClient implements ContentAddressableStorage {

    private static final Logger LOGGER = Logger.getLogger(WorkspaceClient.class);
    private static final String RESOURCE_PATH = "/workspace/v1";
    
    private final String serviceUrl;
    private final Client client;

    public WorkspaceClient(String serviceUrl) {
        this.serviceUrl = serviceUrl+RESOURCE_PATH;

        ClientConfig config = new ClientConfig();
        config.register(JacksonJsonProvider.class);
        config.register(JacksonFeature.class);
        config.register(MultiPartFeature.class);
        client = ClientBuilder.newClient(config);
    }

    @Override
    public void createContainer(String containerName) {
        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);

        Response response = client.target(serviceUrl).path("/containers").request()
                .post(Entity.json(new Entry(containerName)));
        if (Status.CREATED.getStatusCode() == response.getStatus()) {
            LOGGER.info(containerName + ": " + Response.Status.CREATED.getReasonPhrase());
        } else if (Status.CONFLICT.getStatusCode() == response.getStatus()) {
            LOGGER.error(ErrorMessage.CONTAINER_ALREADY_EXIST.getMessage());
            throw new ContentAddressableStorageAlreadyExistException(ErrorMessage.CONTAINER_ALREADY_EXIST.getMessage());
        } else {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
            throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
        }

    }

    @Override
    public void purgeContainer(String containerName) {
        // TODO
    }

    @Override
    public void deleteContainer(String containerName) {
        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);

        Response response = client.target(serviceUrl).path("/containers/" + containerName).request().delete();

        if (Response.Status.NO_CONTENT.getStatusCode() == response.getStatus()) {
            LOGGER.info(containerName + ": " + Response.Status.NO_CONTENT.getReasonPhrase());
        } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
            LOGGER.error(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
            throw new ContentAddressableStorageNotFoundException(ErrorMessage.CONTAINER_NOT_FOUND.getMessage());
        } else {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR);
            throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    @Override
    public void deleteContainer(String containerName, boolean recursive) {
        // TODO
    }

    @Override
    public boolean containerExists(String containerName) {
        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_NAME_IS_A_MANDATORY_PARAMETER.getMessage(), containerName);
        Response response = client.target(serviceUrl).path("/containers/" + containerName).request().head();
        return (Response.Status.OK.getStatusCode() == response.getStatus());
    }

    @Override
    public void createFolder(String containerName, String folderName) {
        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(), containerName, folderName);
        Response response = client.target(serviceUrl).path("/containers/" + containerName + "/folders").request()
                .post(Entity.json(new Entry(folderName)));
        if (Status.CREATED.getStatusCode() == response.getStatus()) {
            LOGGER.info(containerName + "/" + folderName + ": " + Response.Status.CREATED.getReasonPhrase());
        } else if (Status.CONFLICT.getStatusCode() == response.getStatus()) {
            LOGGER.error(ErrorMessage.FOLDER_ALREADY_EXIST);
            throw new ContentAddressableStorageAlreadyExistException(ErrorMessage.FOLDER_ALREADY_EXIST.getMessage());
        } else {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR);
            throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    @Override
    public void deleteFolder(String containerName, String folderName) {
        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(), containerName, folderName);
        Response response = client.target(serviceUrl).path("/containers/" + containerName + "/folders/" + folderName)
                .request().delete();

        if (Response.Status.NO_CONTENT.getStatusCode() == response.getStatus()) {
            LOGGER.info(containerName + "/" + folderName + ": " + Response.Status.NO_CONTENT.getReasonPhrase());
        } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
            LOGGER.error(ErrorMessage.FOLDER_NOT_FOUND);
            throw new ContentAddressableStorageNotFoundException(ErrorMessage.FOLDER_NOT_FOUND.getMessage());
        } else {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR);
            throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
        }
    }

    @Override
    public boolean folderExists(String containerName, String folderName) {
        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(), containerName, folderName);
        Response response = client.target(serviceUrl).path("/containers/" + containerName + "/folders/" + folderName)
                .request().head();
        return (Response.Status.OK.getStatusCode() == response.getStatus());
    }
    // TODO REVIEW m=we might change the contract of the implementation later on (POST on /objects/name directly in order to prevent multipart) 
    @Override
    public void putObject(String containerName, String objectName, InputStream stream) {
        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(), containerName, objectName);

        FormDataMultiPart multiPart = new FormDataMultiPart();

        multiPart.bodyPart(new FormDataBodyPart("objectName", objectName, MediaType.TEXT_PLAIN_TYPE));
        multiPart.bodyPart(
                new StreamDataBodyPart("object", stream, objectName, MediaType.APPLICATION_OCTET_STREAM_TYPE));

        Response response = client.target(serviceUrl).path("/containers/" + containerName + "/objects/").request()
                .post(Entity.entity(multiPart, MediaType.MULTIPART_FORM_DATA_TYPE));

        if (Status.CREATED.getStatusCode() == response.getStatus()) {
            LOGGER.info(containerName + "/" + objectName + ": " + Response.Status.CREATED.getReasonPhrase());
        } else {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR);
            throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
        }

    }

    @Override
    public InputStream getObject(String containerName, String objectName) {
        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(), containerName, objectName);
        Response response = client.target(serviceUrl).path("/containers/" + containerName + "/objects/" + objectName)
                .request().get();

        if (Response.Status.OK.getStatusCode() == response.getStatus()) {
            return response.readEntity(InputStream.class);
        } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
            LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND);
            throw new ContentAddressableStorageNotFoundException(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
        } else {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR);
            throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
        }

    }

    @Override
    public void deleteObject(String containerName, String objectName) {
        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_OBJECT_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(), containerName, objectName);
        Response response = client.target(serviceUrl).path("/containers/" + containerName + "/objects/" + objectName)
                .request().delete();

        if (Response.Status.NO_CONTENT.getStatusCode() == response.getStatus()) {
            LOGGER.info(containerName + "/" + objectName + ": " + Response.Status.NO_CONTENT.getReasonPhrase());
        } else if (Response.Status.NOT_FOUND.getStatusCode() == response.getStatus()) {
            LOGGER.error(ErrorMessage.OBJECT_NOT_FOUND);
            throw new ContentAddressableStorageNotFoundException(ErrorMessage.OBJECT_NOT_FOUND.getMessage());
        } else {
            LOGGER.error(ErrorMessage.INTERNAL_SERVER_ERROR);
            throw new ContentAddressableStorageServerException(ErrorMessage.INTERNAL_SERVER_ERROR.getMessage());
        }

    }

    @Override
    public boolean objectExists(String containerName, String objectName) {
        ParametersChecker.checkParamater(ErrorMessage.CONTAINER_FOLDER_NAMES_ARE_A_MANDATORY_PARAMETER.getMessage(), containerName, objectName);
        Response response = client.target(serviceUrl).path("/containers/" + containerName + "/objects/" + objectName)
                .request().head();
        return (Response.Status.OK.getStatusCode() == response.getStatus());
    }

}
