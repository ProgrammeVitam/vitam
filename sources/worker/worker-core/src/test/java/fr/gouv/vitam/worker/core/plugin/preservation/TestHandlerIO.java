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
package fr.gouv.vitam.worker.core.plugin.preservation;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCyclesClientHelper;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.WorkerspaceQueueException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.mockito.Mockito.mock;

public class TestHandlerIO implements HandlerIO {

    private List<Object> inputs = new ArrayList<>();
    private String currentObjectId;
    private Function<String, File> newLocalFileProvider;
    private InputStream inputStreamFromWorkspace;
    private Map<String, InputStream> inputStreamMap = new HashMap<>();
    private Map<String, File> transferedFileToWorkspaceMap = new HashMap<>();
    private Map<String, JsonNode> jsonFromWorkspace = new HashMap<>();
    private ProcessingUri output;
    private String containerName = "DEFAULT_CONTAINER_NAME";

    @Override
    public void addInIOParameters(List<IOParameter> list) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void addOutIOParameters(List<IOParameter> list) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void reset() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public List<Object> getInput() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object getInput(int rank) {
        return inputs.get(rank);
    }

    @Override
    public <T> T getInput(int rank, Class<T> type) {
        return type.cast(inputs.get(rank));
    }

    @Override
    public File getFile(int rank) {
        return (File) inputs.get(rank);
    }

    @Override
    public List<ProcessingUri> getOutput() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public ProcessingUri getOutput(int rank) {
        return output;
    }

    @Override
    public HandlerIO addOutputResult(int rank, Object object) throws ProcessingException {
        inputs.add(rank, object);
        return this;
    }

    @Override
    public HandlerIO addOutputResult(int rank, Object object, boolean asyncIO) throws ProcessingException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public HandlerIO addOutputResult(int rank, Object object, boolean deleteLocal, boolean asyncIO)
        throws ProcessingException {
        return addOutputResult(rank, object);
    }

    @Override
    public String getContainerName() {
        return containerName;
    }

    public TestHandlerIO setContainerName(String containerName) {
        this.containerName = containerName;
        return this;
    }

    @Override
    public String getWorkerId() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public File getNewLocalFile(String name) {
        return newLocalFileProvider.apply(name);
    }

    @Override
    public List<URI> getUriList(String containerName, String folderName) throws ProcessingException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean checkHandlerIO(int outputNumber, List<Class<?>> clasz) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isExistingFileInWorkspace(String objectName) {
        return inputStreamMap.containsKey(objectName) || this.transferedFileToWorkspaceMap.containsKey(objectName);
    }

    @Override
    public void transferFileToWorkspace(String workspacePath, File sourceFile, boolean toDelete, boolean asyncIO)
        throws ProcessingException {
        this.transferedFileToWorkspaceMap.put(workspacePath, sourceFile);
    }

    @Override
    public void transferAtomicFileToWorkspace(String workspacePath, File sourceFile) {
        this.transferedFileToWorkspaceMap.put(workspacePath, sourceFile);
    }

    @Override
    public void transferInputStreamToWorkspace(
        String workspacePath,
        InputStream inputStream,
        Path filePath,
        boolean asyncIO
    ) {
        this.inputStreamMap.put(workspacePath, inputStream);
    }

    @Override
    public File getFileFromWorkspace(String objectName)
        throws IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        return this.transferedFileToWorkspaceMap.get(objectName);
    }

    @Override
    public Map<String, Long> getFilesWithParamsFromWorkspace(String containerName, String folderName)
        throws ProcessingException {
        throw new VitamRuntimeException("Not implemented");
    }

    @Override
    public InputStream getInputStreamFromWorkspace(String objectName)
        throws IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        if (inputStreamMap.containsKey(objectName)) {
            return inputStreamMap.get(objectName);
        }
        if (transferedFileToWorkspaceMap.containsKey(objectName)) {
            return new FileInputStream(transferedFileToWorkspaceMap.get(objectName));
        } else {
            return this.inputStreamFromWorkspace;
        }
    }

    @Override
    public void consumeAnyEntityAndClose(Response response) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public JsonNode getJsonFromWorkspace(String jsonFilePath) throws ProcessingException {
        return jsonFromWorkspace.get(jsonFilePath);
    }

    @Override
    public LogbookLifeCyclesClient getLifecyclesClient() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public LogbookLifeCyclesClientHelper getHelper() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void transferJsonToWorkspace(
        String collectionName,
        String workspacePath,
        JsonNode jsonNode,
        boolean toDelete,
        boolean asyncIO
    ) throws ProcessingException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void unzipInputStreamOnWorkspace(
        String container,
        String folderName,
        String archiveMimeType,
        InputStream uploadedInputStream,
        boolean asyncIO
    ) throws ContentAddressableStorageException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void enableAsync(boolean asyncIo) throws WorkerspaceQueueException {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean removeFolder(String folderName) throws ContentAddressableStorageException {
        return true;
    }

    @Override
    public void setCurrentObjectId(String currentObjectId) {
        this.currentObjectId = currentObjectId;
    }

    @Override
    public void close() {
        throw new RuntimeException("Not implemented");
    }

    void setInputs(Object input) {
        inputs.add(input);
    }

    public void setNewLocalFile(File newLocalFile) {
        this.newLocalFileProvider = f -> newLocalFile;
    }

    public void setNewLocalFileProvider(Function<String, File> newLocalFileProvider) {
        this.newLocalFileProvider = newLocalFileProvider;
    }

    @Override
    public WorkspaceClientFactory getWorkspaceClientFactory() {
        return mock(WorkspaceClientFactory.class);
    }

    public void setInputStreamFromWorkspace(InputStream inputStreamFromWorkspace) {
        this.inputStreamFromWorkspace = inputStreamFromWorkspace;
    }

    public void setMapOfInputStreamFromWorkspace(String objectName, InputStream inputStreamFromWorkspaces) {
        this.inputStreamMap.put(objectName, inputStreamFromWorkspaces);
    }

    public File getTransferedFileToWorkspace(String name) {
        return transferedFileToWorkspaceMap.get(name);
    }

    public void setJsonFromWorkspace(String name, JsonNode jsonFromWorkspace) {
        this.jsonFromWorkspace.put(name, jsonFromWorkspace);
    }

    public void setOutputWithPath(String path) {
        this.output = new ProcessingUri("VALUE", path);
    }
}
