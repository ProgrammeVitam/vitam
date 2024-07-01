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
package fr.gouv.vitam.processing.common.parameter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Abstract implementation for all worker parameters
 */
@JsonSerialize(using = WorkerParametersSerializer.class)
@JsonDeserialize(using = WorkerParametersDeserializer.class)
abstract class AbstractWorkerParameters implements WorkerParameters {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AbstractWorkerParameters.class);
    private static final String ERROR_MESSAGE = "%s cannot be null";

    @JsonIgnore
    protected final Map<WorkerParameterName, String> mapParameters = new TreeMap<>();

    @JsonIgnore
    protected final Set<WorkerParameterName> mandatoryParameters;

    AbstractWorkerParameters(final Set<WorkerParameterName> mandatory) {
        mandatoryParameters = mandatory;
    }

    @JsonCreator
    protected AbstractWorkerParameters(Map<String, String> map) {
        mandatoryParameters = WorkerParametersFactory.getDefaultMandatory();
        setMap(map);
    }

    @JsonIgnore
    @Override
    public Set<WorkerParameterName> getMandatoriesParameters() {
        return Collections.unmodifiableSet(new HashSet<>(mandatoryParameters));
    }

    @JsonIgnore
    @Override
    public Map<WorkerParameterName, String> getMapParameters() {
        return Collections.unmodifiableMap(new HashMap<>(mapParameters));
    }

    @JsonIgnore
    @Override
    public WorkerParameters putParameterValue(WorkerParameterName parameterName, String parameterValue) {
        ParametersChecker.checkNullOrEmptyParameter(parameterName, parameterValue, getMandatoriesParameters());
        mapParameters.put(parameterName, parameterValue);
        return this;
    }

    @JsonIgnore
    @Override
    public String getParameterValue(WorkerParameterName parameterName) {
        ParametersChecker.checkParameter(String.format(ERROR_MESSAGE, "parameterName"), parameterName);
        return mapParameters.get(parameterName);
    }

    @JsonIgnore
    @Override
    public WorkerParameters setMap(Map<String, String> map) {
        ParametersChecker.checkParameter(String.format(ERROR_MESSAGE, "map"), map);
        for (final String key : map.keySet()) {
            if (WorkerParameterName.getEnums().contains(key)) {
                mapParameters.put(WorkerParameterName.valueOf(key), map.get(key));
            }
        }
        return this;
    }

    @JsonIgnore
    @Override
    public String getCurrentStep() {
        return mapParameters.get(WorkerParameterName.currentStep);
    }

    @JsonIgnore
    @Override
    public WorkerParameters setCurrentStep(String currentStep) {
        ParametersChecker.checkParameter(String.format(ERROR_MESSAGE, "currentStep"), currentStep);
        mapParameters.put(WorkerParameterName.currentStep, currentStep);
        return this;
    }

    @JsonIgnore
    @Override
    public String getPreviousStep() {
        return mapParameters.get(WorkerParameterName.previousStep);
    }

    @JsonIgnore
    @Override
    public WorkerParameters setPreviousStep(String previousStep) {
        mapParameters.put(WorkerParameterName.previousStep, previousStep);
        return this;
    }

    @JsonIgnore
    @Override
    public String getContainerName() {
        return mapParameters.get(WorkerParameterName.containerName);
    }

    @JsonIgnore
    @Override
    public WorkerParameters setContainerName(String containerName) {
        ParametersChecker.checkParameter(String.format(ERROR_MESSAGE, "containerName"), containerName);
        mapParameters.put(WorkerParameterName.containerName, containerName);
        return this;
    }

    @JsonIgnore
    @Override
    public String getObjectId() {
        return mapParameters.get(WorkerParameterName.objectId);
    }

    @JsonIgnore
    @Override
    public WorkerParameters setObjectId(String objectId) {
        ParametersChecker.checkParameter(String.format(ERROR_MESSAGE, "objectId"), objectId);
        mapParameters.put(WorkerParameterName.objectId, objectId);
        return this;
    }

    @JsonIgnore
    @Override
    public String getObjectName() {
        return mapParameters.get(WorkerParameterName.objectName);
    }

    @JsonIgnore
    @Override
    public WorkerParameters setObjectName(String objectName) {
        ParametersChecker.checkParameter(String.format(ERROR_MESSAGE, "objectName"), objectName);
        mapParameters.put(WorkerParameterName.objectName, objectName);
        return this;
    }

    @JsonIgnore
    @Override
    public JsonNode getObjectMetadata() {
        try {
            return JsonHandler.getFromString(mapParameters.get(WorkerParameterName.objectMetadata));
        } catch (InvalidParseOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @JsonIgnore
    @Override
    public WorkerParameters setObjectMetadata(JsonNode objectMetadata) {
        if (objectMetadata != null) {
            mapParameters.put(WorkerParameterName.objectMetadata, JsonHandler.unprettyPrint(objectMetadata));
        }
        return this;
    }

    @JsonIgnore
    @Override
    public List<String> getObjectNameList() {
        String objectList = mapParameters.get(WorkerParameterName.objectNameList);
        try {
            return JsonHandler.getFromString(objectList, List.class, String.class);
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new IllegalArgumentException(e);
        }
    }

    @JsonIgnore
    @Override
    public WorkerParameters setObjectNameList(List<String> objectNameList) {
        ParametersChecker.checkParameter(String.format(ERROR_MESSAGE, "objectNameList"), objectNameList);
        try {
            mapParameters.put(WorkerParameterName.objectNameList, JsonHandler.writeAsString(objectNameList));
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new IllegalArgumentException(e);
        }
        return this;
    }

    @JsonIgnore
    @Override
    public List<JsonNode> getObjectMetadataList() {
        String objectList = mapParameters.get(WorkerParameterName.objectMetadataList);
        if (objectList == null) {
            return null;
        }
        try {
            return JsonHandler.getFromString(objectList, List.class, JsonNode.class);
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new IllegalArgumentException(e);
        }
    }

    @JsonIgnore
    @Override
    public WorkerParameters setObjectMetadataList(List<JsonNode> objectMetaDataList) {
        ParametersChecker.checkParameter(String.format(ERROR_MESSAGE, "objectMetaDataList"), objectMetaDataList);
        try {
            mapParameters.put(WorkerParameterName.objectMetadataList, JsonHandler.writeAsString(objectMetaDataList));
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new IllegalArgumentException(e);
        }
        return this;
    }

    @JsonIgnore
    @Override
    public String getMetadataRequest() {
        return mapParameters.get(WorkerParameterName.metadataRequest);
    }

    @JsonIgnore
    @Override
    public WorkerParameters setMetadataRequest(String metadataRequest) {
        ParametersChecker.checkParameter(String.format(ERROR_MESSAGE, "metadataRequest"), metadataRequest);
        mapParameters.put(WorkerParameterName.metadataRequest, metadataRequest);
        return this;
    }

    @JsonIgnore
    @Override
    public String getWorkerGUID() {
        return mapParameters.get(WorkerParameterName.workerGUID);
    }

    @JsonIgnore
    @Override
    public WorkerParameters setWorkerGUID(String workerGUID) {
        ParametersChecker.checkParameter(String.format(ERROR_MESSAGE, "workerGUID"), workerGUID);
        mapParameters.put(WorkerParameterName.workerGUID, workerGUID);
        return this;
    }

    @JsonIgnore
    @Override
    public String getProcessId() {
        return mapParameters.get(WorkerParameterName.processId);
    }

    @JsonIgnore
    @Override
    public WorkerParameters setProcessId(String processId) {
        ParametersChecker.checkParameter(String.format(ERROR_MESSAGE, "processId"), processId);
        mapParameters.put(WorkerParameterName.processId, processId);
        return this;
    }

    @JsonIgnore
    @Override
    public String getUrlMetadata() {
        return mapParameters.get(WorkerParameterName.urlMetadata);
    }

    @JsonIgnore
    @Override
    public WorkerParameters setUrlMetadata(String urlMetadata) {
        ParametersChecker.checkParameter(String.format(ERROR_MESSAGE, "urlMetadata"), urlMetadata);
        mapParameters.put(WorkerParameterName.urlMetadata, urlMetadata);
        return this;
    }

    @JsonIgnore
    @Override
    public String getUrlWorkspace() {
        return mapParameters.get(WorkerParameterName.urlWorkspace);
    }

    @JsonIgnore
    @Override
    public WorkerParameters setUrlWorkspace(String urlWorkspace) {
        ParametersChecker.checkParameter(String.format(ERROR_MESSAGE, "urlWorkspace"), urlWorkspace);
        mapParameters.put(WorkerParameterName.urlWorkspace, urlWorkspace);
        return this;
    }

    @JsonIgnore
    @Override
    public LogbookTypeProcess getLogbookTypeProcess() {
        return LogbookTypeProcess.valueOf(mapParameters.get(WorkerParameterName.logBookTypeProcess));
    }

    @JsonIgnore
    @Override
    public WorkerParameters setLogbookTypeProcess(LogbookTypeProcess logbookTypeProcess) {
        ParametersChecker.checkParameter(String.format(ERROR_MESSAGE, "logBookTypeProcess"), logbookTypeProcess);
        mapParameters.put(WorkerParameterName.logBookTypeProcess, logbookTypeProcess.toString());
        return this;
    }

    @Override
    public String getWorkflowIdentifier() {
        return mapParameters.get(WorkerParameterName.workflowIdentifier);
    }

    @Override
    public WorkerParameters setWorkflowIdentifier(String workflowIdentifier) {
        ParametersChecker.checkParameter(String.format(ERROR_MESSAGE, "workflowIdentifier"), workflowIdentifier);
        mapParameters.put(WorkerParameterName.workflowIdentifier, workflowIdentifier);
        return this;
    }

    @Override
    public WorkerParameters setFromParameters(WorkerParameters parameters) {
        for (final WorkerParameterName item : WorkerParameterName.values()) {
            mapParameters.put(item, parameters.getParameterValue(item));
        }
        return this;
    }

    @Override
    public String toString() {
        try {
            return JsonHandler.writeAsString(mapParameters);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Cannot convert to String via JsonHandler", e);
            return mapParameters.toString();
        }
    }

    @JsonIgnore
    @Override
    public String getRequestId() {
        return mapParameters.get(WorkerParameterName.requestId);
    }

    @JsonIgnore
    @Override
    public WorkerParameters setRequestId(String newRequestId) {
        ParametersChecker.checkParameter(String.format(ERROR_MESSAGE, "newRequestId"), newRequestId);
        mapParameters.put(WorkerParameterName.requestId, newRequestId);
        return this;
    }

    @JsonIgnore
    @Override
    public String getWorkflowStatusKo() {
        return mapParameters.get(WorkerParameterName.workflowStatusKo);
    }
}
