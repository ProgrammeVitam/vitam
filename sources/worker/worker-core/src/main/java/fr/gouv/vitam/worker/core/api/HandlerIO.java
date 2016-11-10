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
package fr.gouv.vitam.worker.core.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.IOParameter;
import fr.gouv.vitam.processing.common.model.ProcessingUri;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * Handler input and output parameter
 */
public class HandlerIO implements VitamAutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(HandlerIO.class);
    /**
     * Not Enough Param
     */
    public static final String NOT_ENOUGH_PARAM = "Input/Output io parameter list is not enough";
    /**
     * Not Conform Param
     */
    public static final String NOT_CONFORM_PARAM = "Input/Output io parameter is not correct";
    private static final String HANDLER_INPUT_NOT_FOUND = "Handler input not found exception: ";

    private final List<Object> input = new ArrayList<>();
    private final List<ProcessingUri> output = new ArrayList<>();
    private final String containerName;
    private final String workerId;
    private final File localDirectory;
    private final Map<String, Object> memoryMap = new HashMap<>();


    /**
     * Constructor with local root path
     * 
     * @param containerName
     * @param workerId
     */
    public HandlerIO(String containerName, String workerId) {
        this.containerName = containerName;
        this.workerId = workerId;
        this.localDirectory = PropertiesUtils.fileFromTmpFolder(containerName + "_" + workerId);
        localDirectory.mkdirs();
    }

    /**
     * Add Input parameters
     * 
     * @param list
     * @throws IllegalArgumentException if an error occurs
     */
    public void addInIOParameters(List<IOParameter> list) {
        for (final IOParameter in : list) {
            switch (in.getUri().getPrefix()) {
                case WORKSPACE:
                    try {
                        // TODO P1 : remove optional when lazy file loading is implemented
                        input.add(findFileFromWorkspace(in.getUri().getPath(),
                            in.getOptional()));
                        break;
                    } catch (final FileNotFoundException e) {
                        throw new IllegalArgumentException(HANDLER_INPUT_NOT_FOUND + in.getUri().getPath(), e);
                    }
                case MEMORY:
                    input.add(memoryMap.get(in.getUri().getPath()));
                    break;
                case VALUE:
                    input.add(in.getUri().getPath());
                    break;
                default:
                    throw new IllegalArgumentException(
                        HANDLER_INPUT_NOT_FOUND + in.getUri().getPrefix() + ":" + in.getUri().getPath());
            }
        }
    }

    /**
     * Add Output parameters
     * 
     * @param list
     * @throws IllegalArgumentException if an error occurs
     */
    public void addOutIOParameters(List<IOParameter> list) {
        for (final IOParameter out : list) {
            switch (out.getUri().getPrefix()) {
                case WORKSPACE:
                case MEMORY:
                    output.add(out.getUri());
                    break;
                default:
                    throw new IllegalArgumentException("Handler Output not conform: " + out.getUri());
            }
        }
    }

    /**
     * Reset after each Action
     */
    public void reset() {
        input.clear();
        output.clear();
    }

    /**
     * Clear the HandlerIO, including temporary files and directories at the end of the step Workflow execution
     * 
     * @throws IOException
     */
    @Override
    public void close() {
        reset();
        memoryMap.clear();
        if (!FileUtil.deleteRecursive(localDirectory)) {
            LOGGER.warn("Cannot clear the temporary directory: " + localDirectory);
        }
    }

    /**
     * @return list of input
     */
    public List<Object> getInput() {
        return input;
    }

    /**
     * Return one Object from input
     * 
     * @param rank
     * @return the rank-th object
     */
    public Object getInput(int rank) {
        return input.get(rank);
    }

    /**
     * @return list of output
     */
    public List<ProcessingUri> getOutput() {
        return output;
    }

    /**
     * Return one ProcessingUri from output
     * 
     * @param rank
     * @return the rank-th ProcessingUri
     */
    public ProcessingUri getOutput(int rank) {
        return output.get(rank);
    }

    /**
     * Add one output result (no delete)
     * 
     * @param rank the position in the output
     * @param object the result to store (WORKSPACE to workspace and must be a File, MEMORY to memory whatever it is)
     * @return this
     * @throws ProcessingException
     * @throws IllegalArgumentException
     */
    public HandlerIO addOuputResult(int rank, Object object) throws ProcessingException {
        return addOuputResult(rank, object, false);
    }

    /**
     * Add one output result
     * 
     * @param rank the position in the output
     * @param object the result to store (WORKSPACE to workspace and must be a File, MEMORY to memory whatever it is)
     * @param deleteLocal if true, will delete the local file in case of WORKSPACE only
     * @return this
     * @throws ProcessingException
     * @throws IllegalArgumentException
     */
    public HandlerIO addOuputResult(int rank, Object object, boolean deleteLocal) throws ProcessingException {
        ProcessingUri uri = output.get(rank);
        if (uri == null) {
            throw new IllegalArgumentException(HANDLER_INPUT_NOT_FOUND + rank);
        }
        switch (uri.getPrefix()) {
            case MEMORY:
                memoryMap.put(uri.getPath(), object);
                break;
            case VALUE:
                // Ignore
                break;
            case WORKSPACE:
                if (!(object instanceof File)) {
                    throw new ProcessingException("Not a File but WORKSPACE out parameter: " + uri);
                }
                transferFileToWorkspace(uri.getPath(), (File) object, deleteLocal);
                break;
            default:
                throw new IllegalArgumentException(HANDLER_INPUT_NOT_FOUND + uri);

        }
        return this;
    }

    /**
     * 
     * @return the container Name
     */
    public String getContainerName() {
        return containerName;
    }

    /**
     * 
     * @return the worker Id
     */
    public String getWorkerId() {
        return workerId;
    }

    /**
     * @return the localPathRoot
     */
    public File getLocalPathRoot() {
        return localDirectory;
    }

    /**
     * 
     * @param name
     * @return a File pointing to a local path in Tmp directory under protected Worker instance space
     */
    public File getNewLocalFile(String name) {
        File file = new File(localDirectory.getAbsolutePath() + "/" + name);
        file.getParentFile().mkdirs();
        return file;
    }

    /**
     * Check if input and output have the very same number of elements and for Input the associated types
     * 
     * @param outputNumber the number of outputArguments
     * @param clasz the list of Class that should be in the InputParameters
     * @return true if everything ok
     */
    public boolean checkHandlerIO(int outputNumber, List<Class<?>> clasz) {
        if (getInput().size() != clasz.size() || getOutput().size() != outputNumber) {
            return false;
        }
        for (int i = 0; i < getInput().size(); i++) {
            Object object = getInput(i);
            if (object == null || !object.getClass().equals(clasz.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Helper to write a file to Workspace<br/>
     * <br/>
     * To be used when not specified within the Output Parameters
     * 
     * @param workspacePath path within the workspath, without the container (implicit)
     * @param sourceFile the source file to write
     * @param toDelete if True, will delete the local file
     * @throws ProcessingException
     */
    public void transferFileToWorkspace(String workspacePath, File sourceFile, boolean toDelete)
        throws ProcessingException {
        try {
            ParametersChecker.checkParameter("Workspace path is a mandatory parameter", workspacePath);
            ParametersChecker.checkParameter("Source file is a mandatory parameter", sourceFile);
        } catch (IllegalArgumentException e) {
            throw new ProcessingException(e);
        }
        if (!sourceFile.canRead()) {
            throw new ProcessingException("Cannot found source file: " + sourceFile);
        }
        try (WorkspaceClient client = WorkspaceClientFactory.getInstance().getClient();
            FileInputStream inputStream = new FileInputStream(sourceFile)) {
            client.putObject(containerName, workspacePath, inputStream);
            if (toDelete && !sourceFile.delete()) {
                LOGGER.warn("File could not be deleted: " + sourceFile);
            }
        } catch (IOException e) {
            throw new ProcessingException("Cannot found or read source file: " + sourceFile, e);
        } catch (ContentAddressableStorageServerException e) {
            throw new ProcessingException("Cannot write file to workspace: " + containerName + "/" + workspacePath, e);
        }
    }

    /**
     * Get the File associated with this filename, trying in this order: as fullpath, as in Vitam Config Folder, as
     * Resources file
     * 
     * @param containerName container name
     * @param objectName object name
     * @param workerId worker id
     * @param optional if file is optional
     * @return file if found, if not found, null if optional
     * @throws FileNotFoundException if file is not found and not optional
     */
    private final File findFileFromWorkspace(String objectName, boolean optional) throws FileNotFoundException {
        // First try as full path
        File file = null;
        // TODO P1 : this optional situation would be treated later when lazy file loading is implemented
        if (optional) {
            try {
                file = getFileFromWorkspace(objectName);
            } catch (final ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException |
                IOException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                file = null;
            }
            if (file != null && !file.exists()) {
                file = null;
            }
        } else {
            try (WorkspaceClient client = WorkspaceClientFactory.getInstance().getClient()) {
                file = getFileFromWorkspace(objectName);
            } catch (final ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException |
                IOException e) {
                // need to rewrite the exception
                LOGGER.error(e);
                throw new FileNotFoundException("File not found: " + objectName);
            }
            if (!file.exists()) {
                throw new FileNotFoundException("File not found: " + objectName);
            }
        }
        return file;
    }

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
    // TODO P2: could add a sort of cache list that could be clean without cleaning other parameters (for handler
    // parallel)
    public File getFileFromWorkspace(String objectName)
        throws IOException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException {
        File file = getNewLocalFile(objectName);
        if (!file.exists()) {
            try (InputStream inputStream = getInputStreamFromWorkspace(objectName)) {
                StreamUtils.copy(inputStream, new FileOutputStream(file));
                return file;
            }
        }
        return file;
    }

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
    public InputStream getInputStreamFromWorkspace(String objectName)
        throws IOException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException {
        File file = getNewLocalFile(objectName);
        if (!file.exists()) {
            try (WorkspaceClient client = WorkspaceClientFactory.getInstance().getClient()) {
                return client.getObject(containerName, objectName);
            }
        }
        return new FileInputStream(file);
    }

    /**
     * Helper to delete a local file<br/>
     * <br/>
     * To be used when not specified within the Input/Output parameters
     * 
     * @param objectName
     * @return True if deleted
     */
    // TODO P2: could add a sort of cache list that could be clean without cleaning other parameters (for handler
    // parallel)
    public boolean deleteLocalFile(String objectName) {
        File file = getNewLocalFile(objectName);
        if (file.exists()) {
            return file.delete();
        }
        return true;
    }
}
