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
package fr.gouv.vitam.worker.core.impl;

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

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCyclesClientHelper;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.IOParameter;
import fr.gouv.vitam.processing.common.model.ProcessingUri;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * Handler input and output parameter
 */
public class HandlerIOImpl implements VitamAutoCloseable, HandlerIO {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(HandlerIOImpl.class);
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
    private final WorkspaceClient client;
    private final LogbookLifeCyclesClient lifecyclesClient;
    private final LogbookLifeCyclesClientHelper helper;

    /**
     * Constructor with local root path
     * 
     * @param containerName
     * @param workerId
     */
    public HandlerIOImpl(String containerName, String workerId) {
        this.containerName = containerName;
        this.workerId = workerId;
        this.localDirectory = PropertiesUtils.fileFromTmpFolder(containerName + "_" + workerId);
        localDirectory.mkdirs();
        client = WorkspaceClientFactory.getInstance().getClient();
        lifecyclesClient = LogbookLifeCyclesClientFactory.getInstance().getClient();
        helper = new LogbookLifeCyclesClientHelper();
    }

    @Override
    public LogbookLifeCyclesClient getLifecyclesClient() {
        return lifecyclesClient;
    }

    @Override
    public LogbookLifeCyclesClientHelper getHelper() {
        return helper;
    }

    @Override
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

    @Override
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

    @Override
    public void reset() {
        input.clear();
        output.clear();
        helper.clear();
    }

    @Override
    public void close() {
        client.close();
        lifecyclesClient.close();
        partialClose();
    }

    /**
     * Close the HandlerIO, including temporary files and directories at the end of the step Workflow execution, but do
     * not close the WorkspaceClient
     */
    public void partialClose() {
        reset();
        memoryMap.clear();
        if (!FileUtil.deleteRecursive(localDirectory)) {
            LOGGER.warn("Cannot clear the temporary directory: " + localDirectory);
        }
    }

    @Override
    public List<Object> getInput() {
        return input;
    }

    @Override
    public Object getInput(int rank) {
        return input.get(rank);
    }

    @Override
    public List<ProcessingUri> getOutput() {
        return output;
    }

    @Override
    public ProcessingUri getOutput(int rank) {
        return output.get(rank);
    }

    @Override
    public HandlerIO addOuputResult(int rank, Object object) throws ProcessingException {
        return addOuputResult(rank, object, false);
    }

    @Override
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

    @Override
    public String getContainerName() {
        return containerName;
    }

    @Override
    public String getWorkerId() {
        return workerId;
    }

    @Override
    public File getLocalPathRoot() {
        return localDirectory;
    }

    @Override
    public File getNewLocalFile(String name) {
        File file = new File(localDirectory.getAbsolutePath() + "/" + name);
        file.getParentFile().mkdirs();
        return file;
    }

    @Override
    public boolean checkHandlerIO(int outputNumber, List<Class<?>> clasz) {
        if (getInput().size() != clasz.size() || getOutput().size() != outputNumber) {
            LOGGER.error("InputSize shoul be {} but is {} OR OutputSize should be {} but is {}",
                clasz.size(), getInput().size(), outputNumber, getOutput().size());
            return false;
        }
        for (int i = 0; i < getInput().size(); i++) {
            Object object = getInput(i);
            if (object == null || !clasz.get(i).isInstance(object)) {
                LOGGER.error("Input class should be {} but is {}",
                    clasz.get(i).getName(), object != null ? object.getClass().getName() : "Null object");
                return false;
            }
        }
        return true;
    }

    @Override
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
        try (FileInputStream inputStream = new FileInputStream(sourceFile)) {
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

    @Override
    public void transferInputStreamToWorkspace(String workspacePath, InputStream inputStream)
        throws ProcessingException {
        try {
            client.putObject(containerName, workspacePath, inputStream);
        } catch (ContentAddressableStorageServerException e) {
            throw new ProcessingException("Cannot write stream to workspace: " + containerName + "/" + workspacePath, e);
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
            try {
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

    @Override
    public File getFileFromWorkspace(String objectName)
        throws IOException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException {
        File file = getNewLocalFile(objectName);
        if (!file.exists()) {
            Response response = null;
            try {
                response = client.getObject(containerName, objectName);
                if (response != null) {
                    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                        StreamUtils.copy((InputStream) response.getEntity(), fileOutputStream);
                    }
                }
            } finally {
                client.consumeAnyEntityAndClose(response);
            }
        }
        return file;
    }

    @Override
    public InputStream getInputStreamFromWorkspace(String objectName)
        throws IOException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException {
        File file = getNewLocalFile(objectName);
        if (!file.exists()) {
            Response response = null;
            try {
                response = client.getObject(containerName, objectName);
                if (response != null) {
                    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                        StreamUtils.copy((InputStream) response.getEntity(), fileOutputStream);
                    }
                }
            } finally {
                client.consumeAnyEntityAndClose(response);
            }
        }
        return new FileInputStream(file);
    }

    @Override
    public Response getInputStreamNoCachedFromWorkspace(String objectName)
        throws ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException {
        return client.getObject(containerName, objectName);
    }

    @Override
    public void consumeAnyEntityAndClose(Response response) {
        client.consumeAnyEntityAndClose(response);
    }

    @Override
    public JsonNode getJsonFromWorkspace(String jsonFilePath) throws ProcessingException {
        Response response = null;
        InputStream is = null;
        try {
            response = client.getObject(containerName, jsonFilePath);
            is = (InputStream) response.getEntity();
            if (is != null) {
                return JsonHandler.getFromInputStream(is);
            } else {
                LOGGER.error("Json not found");
                throw new ProcessingException("Json not found");
            }
        } catch (InvalidParseOperationException e) {
            LOGGER.debug("Json wrong format", e);
            throw new ProcessingException(e);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.debug("Workspace Server Error", e);
            throw new ProcessingException(e);
        } finally {
            if (is != null) {
                StreamUtils.closeSilently(is);
            }
            WorkspaceClient.staticConsumeAnyEntityAndClose(response);
        }
    }

    @Override
    public boolean deleteLocalFile(String objectName) {
        File file = getNewLocalFile(objectName);
        if (file.exists()) {
            return file.delete();
        }
        return true;
    }
}
