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
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static fr.gouv.vitam.processing.common.parameter.WorkerParameterName.containerName;
import static fr.gouv.vitam.processing.common.parameter.WorkerParameterName.objectMetadata;
import static fr.gouv.vitam.processing.common.parameter.WorkerParameterName.requestId;
import static fr.gouv.vitam.processing.common.parameter.WorkerParameterName.workflowStatusKo;

public class TestWorkerParameter implements WorkerParameters {

    public final Map<String, Object> params;

    public TestWorkerParameter(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public WorkerParameters putParameterValue(WorkerParameterName parameterName, String parameterValue) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public String getParameterValue(WorkerParameterName parameterName) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public WorkerParameters setMap(Map<String, String> map) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public String getCurrentStep() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public WorkerParameters setCurrentStep(String currentStep) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public String getPreviousStep() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public WorkerParameters setPreviousStep(String previousStep) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public String getContainerName() {
        return (String) this.params.get(containerName.name());
    }

    @Override
    public WorkerParameters setContainerName(String containerName) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public String getObjectId() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public WorkerParameters setObjectId(String objectId) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public String getObjectName() {
        return (String) params.get("objectName");
    }

    @Override
    public WorkerParameters setObjectName(String objectName) {
        params.put("objectName", objectName);
        return this;
    }

    @Override
    public JsonNode getObjectMetadata() {
        return (JsonNode) params.get(objectMetadata.name());
    }

    @Override
    public WorkerParameters setObjectMetadata(JsonNode objectName) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public List<String> getObjectNameList() {
        String objectList = (String) params.get(WorkerParameterName.objectNameList.name());
        try {
            return JsonHandler.getFromString(objectList, List.class, String.class);
        } catch (InvalidParseOperationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public WorkerParameters setObjectNameList(List<String> objectNameList) {
        try {
            params.put(WorkerParameterName.objectNameList.name(), JsonHandler.writeAsString(objectNameList));
        } catch (InvalidParseOperationException e) {
            throw new IllegalArgumentException(e);
        }
        return this;
    }

    @Override
    public List<JsonNode> getObjectMetadataList() {
        String objectList = (String) params.get(WorkerParameterName.objectMetadataList.name());
        if (objectList == null) {
            return null;
        }
        try {
            return JsonHandler.getFromString(objectList, List.class, JsonNode.class);
        } catch (InvalidParseOperationException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public WorkerParameters setObjectMetadataList(List<JsonNode> objectMetadataList) {
        try {
            params.put(WorkerParameterName.objectMetadataList.name(), JsonHandler.writeAsString(objectMetadataList));
        } catch (InvalidParseOperationException e) {
            throw new IllegalArgumentException(e);
        }
        return this;
    }

    @Override
    public String getMetadataRequest() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public WorkerParameters setMetadataRequest(String metadataRequest) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public String getWorkerGUID() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public WorkerParameters setWorkerGUID(String workerGUID) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public String getProcessId() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public WorkerParameters setProcessId(String processId) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public String getUrlMetadata() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public WorkerParameters setUrlMetadata(String urlMetadata) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public String getUrlWorkspace() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public WorkerParameters setUrlWorkspace(String urlWorkspace) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public LogbookTypeProcess getLogbookTypeProcess() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public WorkerParameters setLogbookTypeProcess(LogbookTypeProcess logbookTypeProcess) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public String getWorkflowIdentifier() {
        return "TEST WORKFLOW";
    }

    @Override
    public WorkerParameters setWorkflowIdentifier(String workflowIdentifier) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public WorkerParameters setFromParameters(WorkerParameters parameters) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public String getRequestId() {
        return (String) this.params.get(requestId.name());
    }

    @Override
    public WorkerParameters setRequestId(String newRequestId) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public String getWorkflowStatusKo() {
        return (String) this.params.get(workflowStatusKo.name());
    }

    @Override
    public Map<WorkerParameterName, String> getMapParameters() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public Set<WorkerParameterName> getMandatoriesParameters() {
        throw new IllegalStateException("Not implemented");
    }

    public static final class TestWorkerParameterBuilder {

        public Map<String, Object> params = new HashMap<>();

        private TestWorkerParameterBuilder() {}

        public static TestWorkerParameterBuilder workerParameterBuilder() {
            return new TestWorkerParameterBuilder();
        }

        public TestWorkerParameterBuilder withParams(Map<String, Object> params) {
            this.params = params;
            return this;
        }

        public TestWorkerParameterBuilder withRequestId(String requestId) {
            this.params.put(WorkerParameterName.requestId.name(), requestId);
            return this;
        }

        public TestWorkerParameterBuilder withContainerName(String containerName) {
            this.params.put(WorkerParameterName.containerName.name(), containerName);
            return this;
        }

        public TestWorkerParameterBuilder withObjectName(String objectName) {
            this.params.put(WorkerParameterName.objectName.name(), objectName);
            return this;
        }

        public TestWorkerParameterBuilder withObjectMetadata(JsonNode objectMetadata) {
            this.params.put(WorkerParameterName.objectMetadata.name(), objectMetadata);
            return this;
        }

        public TestWorkerParameterBuilder withWorkflowStatusKo(String status) {
            this.params.put(workflowStatusKo.name(), status);
            return this;
        }

        public TestWorkerParameter build() {
            return new TestWorkerParameter(this.params);
        }
    }
}
