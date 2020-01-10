package fr.gouv.vitam.worker.common;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCyclesClientHelper;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.core.exception.WorkerspaceQueueException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

/**
 * Interface of HandlerIO for all Handlers
 */
public interface HandlerIO extends VitamAutoCloseable {

    /**
     * Add Input parameters
     *
     * @param list
     * @throws IllegalArgumentException if an error occurs
     */
    void addInIOParameters(List<IOParameter> list);

    /**
     * Add Output parameters
     *
     * @param list
     * @throws IllegalArgumentException if an error occurs
     */
    void addOutIOParameters(List<IOParameter> list);

    /**
     * Reset after each Action
     */
    void reset();

    /**
     * @return list of input
     */
    List<Object> getInput();

    /**
     * Return one Object from input
     *
     * @param rank
     * @return the rank-th object
     */
    Object getInput(int rank);

    /**
     * @return list of output
     */
    List<ProcessingUri> getOutput();

    /**
     * Return one ProcessingUri from output
     *
     * @param rank
     * @return the rank-th ProcessingUri
     */
    ProcessingUri getOutput(int rank);

    HandlerIO addOutputResult(int rank, Object object) throws ProcessingException;

    /**
     * Add one output result (no delete)
     *
     * @param rank the position in the output
     * @param object the result to store (WORKSPACE to workspace and must be a File, MEMORY to memory whatever it is)
     * @param asyncIO asynchronously send to the workspace
     * @return this
     * @throws ProcessingException
     * @throws IllegalArgumentException
     */
    HandlerIO addOutputResult(int rank, Object object, boolean asyncIO) throws ProcessingException;

    /**
     * Add one output result
     *
     * @param rank the position in the output
     * @param object the result to store (WORKSPACE to workspace and must be a File, MEMORY to memory whatever it is)
     * @param deleteLocal if true, will delete the local file in case of WORKSPACE only
     * @param asyncIO asynchronously send to the workspace
     * @return this
     * @throws ProcessingException
     * @throws IllegalArgumentException
     */
    HandlerIO addOutputResult(int rank, Object object, boolean deleteLocal, boolean asyncIO) throws ProcessingException;

    /**
     *
     * @return the container Name
     */
    String getContainerName();

    /**
     *
     * @return the worker Id
     */
    String getWorkerId();

    /**
     * @return the localPathRoot
     */
    File getLocalPathRoot();

    /**
     *
     * @param name
     * @return a File pointing to a local path in Tmp directory under protected Worker instance space
     */
    File getNewLocalFile(String name);

    /**
     * @param containerName
     * @param folderName
     * @return List<URI> a list of uri of object in SIP folder
     * @throws ProcessingException
     */
    List<URI> getUriList(String containerName, String folderName) throws ProcessingException;

    /**
     * Check if input and output have the very same number of elements and for Input the associated types
     *
     * @param outputNumber the number of outputArguments
     * @param clasz the list of Class that should be in the InputParameters
     * @return true if everything ok
     */
    boolean checkHandlerIO(int outputNumber, List<Class<?>> clasz);

    boolean isExistingFileInWorkspace(String workspacePath) throws ProcessingException;

    /**
     * Helper to write a file to Workspace<br/>
     * <br/>
     * To be used when not specified within the Output Parameters
     *
     * @param workspacePath path within the workspath, without the container (implicit)
     * @param sourceFile the source file to write
     * @param toDelete if True, will delete the local file
     * @param asyncIO asynchronously send to the workspace
     * @throws ProcessingException
     */
    void transferFileToWorkspace(String workspacePath, File sourceFile, boolean toDelete, boolean asyncIO)
        throws ProcessingException;

    void transferAtomicFileToWorkspace(String workspacePath, File sourceFile)
        throws ProcessingException;

    /**
     * Helper to write an InputStream to Workspace<br/>
     * <br/>
     * To be used when not specified within the Output Parameters
     *
     * @param workspacePath path within the workspath, without the container (implicit)
     * @param inputStream the source InputStream to write
     * @param asyncIO asynchronously send to the workspace
     * @throws ProcessingException
     */
    void transferInputStreamToWorkspace(String workspacePath, InputStream inputStream, Path filePath, boolean asyncIO)
        throws ProcessingException;

    /**
     * Helper to load a file from Workspace (or local cache) and save it into local cache.<br/>
     * <br/>
     * To be used when not specified within the Input parameters
     *
     * @param objectName
     * @return file if found
     * @throws IOException
     * @throws ContentAddressableStorageNotFoundException
     * @throws ContentAddressableStorageServerException
     */
    // TODO P2: could transfer a sort of cache list that could be clean without cleaning other parameters (for handler
    // parallel)
    File getFileFromWorkspace(String objectName)
        throws IOException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException;

    /**
     * Helper to get an InputStream (using local cache if possible) from Workspace<br/>
     * <br/>
     * To be used when not specified within the Input parameters
     *
     * @param objectName
     * @return the InputStream
     * @throws IOException
     * @throws ContentAddressableStorageNotFoundException
     * @throws ContentAddressableStorageServerException
     */
    InputStream getInputStreamFromWorkspace(String objectName)
        throws IOException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException;

    /**
     * Helper to get an InputStream (without cache) from Workspace<br/>
     * <br/>
     * To be used when not specified within the Input parameters
     *
     * @param objectName
     * @return the InputStream
     * @throws ContentAddressableStorageNotFoundException
     * @throws ContentAddressableStorageServerException
     */
    Response getInputStreamNoCachedFromWorkspace(String objectName)
        throws ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException;

    /**
     * Consume any entity and close response
     *
     * @param response
     */
    void consumeAnyEntityAndClose(Response response);

    /**
     * Retrieve a json file as a {@link JsonNode} from the workspace.
     *
     * @param jsonFilePath path in workspace of the json File
     * @return JsonNode of the json file
     * @throws ProcessingException throws when error occurs
     */
    JsonNode getJsonFromWorkspace(String jsonFilePath) throws ProcessingException;

    /**
     * Helper to delete a local file<br/>
     * <br/>
     * To be used when not specified within the Input/Output parameters
     *
     * @param objectName
     * @return True if deleted
     */
    // TODO P2: could transfer a sort of cache list that could be clean without cleaning other parameters (for handler
    // parallel)
    boolean deleteLocalFile(String objectName);

    /**
     *
     * @return the HandlerIO LifecycleClient
     */
    LogbookLifeCyclesClient getLifecyclesClient();

    /**
     *
     * @return the helper for bulk lifecycle for LifecyclesClient
     */
    LogbookLifeCyclesClientHelper getHelper();

    /**
     *
     *
     * Helper to convert and write a file to Workspace<br/>
     * <br/>
     *
     * @param collectionName : collection type
     * @param workspacePath path within the workspacepath, without the container (implicit)
     * @param jsonNode the json file to write
     * @param toDelete if True, will delete the local file
     * @param asyncIO asynchronously send to the workspace
     * @throws ProcessingException
     */
    void transferJsonToWorkspace(String collectionName, String workspacePath, JsonNode jsonNode, boolean toDelete,
        boolean asyncIO)
        throws ProcessingException;

    /**
     * @param container
     * @param folderName
     * @param archiveMimeType
     * @param uploadedInputStream
     * @param asyncIO asynchronously send and unzip file to/in the workspace
     * @throws ContentAddressableStorageException
     */
    void unzipInputStreamOnWorkspace(String container, String folderName, String archiveMimeType,
        InputStream uploadedInputStream, boolean asyncIO)
        throws ContentAddressableStorageException;

    /**
     * If true then start async manager, if false then waitEndOfTransfer and stop asyncManager
     *
     * @param asyncIo
     */
    void enableAsync(boolean asyncIo) throws WorkerspaceQueueException;


    /**
     * Remove a specific folder
     *
     * @param folderName the folderName to delete
     * @return true if the folder was removed, false if it does not exist
     * @throws ContentAddressableStorageException when storage error occurs
     */
    boolean removeFolder(String folderName) throws ContentAddressableStorageException;

    void setCurrentObjectId(String currentObjectId);

    WorkspaceClientFactory getWorkspaceClientFactory();
}
