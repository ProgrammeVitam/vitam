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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import fr.gouv.vitam.common.parameter.VitamParameter;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;

import java.util.List;
import java.util.Map;

/**
 * Class use to manage parameters for and on worker
 */
@JsonSerialize(using = WorkerParametersSerializer.class)
@JsonDeserialize(using = WorkerParametersDeserializer.class)
public interface WorkerParameters extends VitamParameter<WorkerParameterName> {
    /**
     * Put parameterValue on mapParameters with parameterName key <br />
     * <br />
     * If parameterKey already exists, then override it (no check)
     *
     * @param parameterName the key of the parameter to put on the parameter map
     * @param parameterValue the value to put on the parameter map
     * @return actual instance of WorkerParameter (fluent like)
     * @throws IllegalArgumentException if the parameterName is null or if parameterValue is null or empty
     */
    WorkerParameters putParameterValue(WorkerParameterName parameterName, String parameterValue);

    /**
     * Get the parameter according to the parameterName
     *
     * @param parameterName the wanted parameter
     * @return the value or null if not found
     * @throws IllegalArgumentException throws if parameterName is null
     */
    String getParameterValue(WorkerParameterName parameterName);

    /**
     * Set from map using String as Key
     *
     * @param map the map parameters to set
     * @return the current instance of WorkerParameters
     * @throws IllegalArgumentException if parameter key is unknown or if the map is null
     */
    WorkerParameters setMap(Map<String, String> map);

    /**
     * Get the current step parameter
     *
     * @return the current step value
     */
    String getCurrentStep();

    /**
     * Set the current step value
     *
     * @param currentStep the current step value
     * @return the current instance of WorkerParameters
     */
    WorkerParameters setCurrentStep(String currentStep);

    /**
     * Get the previous step parameter
     *
     * @return the previous step value
     */
    String getPreviousStep();

    /**
     * Set the previous step value
     *
     * @param previousStep the current step value
     * @return the current instance of WorkerParameters
     * @throws IllegalArgumentException if the previousStep is null or empty
     */
    WorkerParameters setPreviousStep(String previousStep);

    /**
     * Get container name parameter
     *
     * @return the container name value
     */
    String getContainerName();

    /**
     * Set the container name value
     *
     * @param containerName the container name value
     * @return the current instance of WorkerParameter
     * @throws IllegalArgumentException if the containerName is null or empty
     */
    WorkerParameters setContainerName(String containerName);

    /**
     * Get the object id parameter
     *
     * @return the object id value
     */
    String getObjectId();

    /**
     * Set the object id value
     *
     * @param objectId the object id value
     * @return the current instance of WorkerParameter
     * @throws IllegalArgumentException if the objectId is null or empty
     */
    WorkerParameters setObjectId(String objectId);

    /**
     * Get the object name parameter
     *
     * @return the object name value
     */
    String getObjectName();

    /**
     * Set the object name value
     *
     * @param objectName the object name value
     * @return the current instance of WorkerParameter
     * @throws IllegalArgumentException if the objectName is null or empty
     */
    WorkerParameters setObjectName(String objectName);

    /**
     * Get the object metadata parameter
     *
     * @return the object metadata value
     */
    JsonNode getObjectMetadata();

    /**
     * Set the object metadata value
     *
     * @param objectName the object metadata value
     * @return the current instance of WorkerParameter
     * @throws IllegalArgumentException if the objectName is null or empty
     */
    WorkerParameters setObjectMetadata(JsonNode objectName);

    /**
     * Get the object name parameter
     *
     * @return the object name value
     */
    List<String> getObjectNameList();

    /**
     * Set the object name value
     *
     * @param objectNameList the object name value
     * @return the current instance of WorkerParameter
     * @throws IllegalArgumentException if the objectName is null or empty
     */
    WorkerParameters setObjectNameList(List<String> objectNameList);

    /**
     * Get the object metadata parameter
     *
     * @return the object name value
     */
    List<JsonNode> getObjectMetadataList();

    /**
     * Set the object metadata value
     *
     * @param objectMetadataList the object metadataList value
     * @return the current instance of WorkerParameter
     * @throws IllegalStateException if the metadataList is null or empty
     */
    WorkerParameters setObjectMetadataList(List<JsonNode> objectMetadataList);

    /**
     * Get the metadata request parameter
     *
     * @return the metadata request value
     */
    String getMetadataRequest();

    /**
     * Set the request metadata request value
     *
     * @param metadataRequest the metadata request value
     * @return the current instance of WorkerParameter
     * @throws IllegalArgumentException if metadataRequest is null or empty
     */
    WorkerParameters setMetadataRequest(String metadataRequest);

    /**
     * Get the worker GUID parameter
     *
     * @return the worker GUID value
     */
    String getWorkerGUID();

    /**
     * Set the worker GUID value
     *
     * @param workerGUID the worker GUID value
     * @return the current instance of WorkerParameters
     * @throws IllegalArgumentException if workerGUID is null or empty
     */
    WorkerParameters setWorkerGUID(String workerGUID);

    /**
     * Get the process id parameter
     *
     * @return the process id value
     */
    String getProcessId();

    /**
     * Set the process id value
     *
     * @param processId the process id value
     * @return the current instance of WorkerParameters
     * @throws IllegalArgumentException if processId is null or empty
     */
    WorkerParameters setProcessId(String processId);

    /**
     * Get the url of metadata resource parameter
     *
     * @return the url of metadata resource value
     */
    String getUrlMetadata();

    /**
     * Set the url of metadata resource value
     *
     * @param urlMetadata the url of metadata resource value
     * @return the current instance of WorkerParameters
     * @throws IllegalArgumentException if urlMetadata is null or empty
     */
    WorkerParameters setUrlMetadata(String urlMetadata);

    /**
     * Get the url of workspace resource parameter
     *
     * @return the url of workspace resource value
     */
    String getUrlWorkspace();

    /**
     * Set the url of workspace resource value
     *
     * @param urlWorkspace the url of workspace resource value
     * @return the current instance of WorkerParameters
     * @throws IllegalArgumentException if urlWorspace is null or empty
     */
    WorkerParameters setUrlWorkspace(String urlWorkspace);

    /**
     * Get the logbook Type Process
     *
     * @return the logbook Type Process
     */
    LogbookTypeProcess getLogbookTypeProcess();

    /**
     * Set the logbook Type Process
     *
     * @param logbookTypeProcess the logbook Type Process
     * @return the current instance of WorkerParameters
     * @throws IllegalArgumentException if urlWorspace is null or empty
     */
    WorkerParameters setLogbookTypeProcess(LogbookTypeProcess logbookTypeProcess);

    /**
     * @return workflowIdentifier
     */
    String getWorkflowIdentifier();

    /**
     * Set the logbook Type Process
     *
     * @param workflowIdentifier
     * @return the current instance of WorkerParameters
     * @throws IllegalArgumentException if urlWorspace is null or empty
     */
    WorkerParameters setWorkflowIdentifier(String workflowIdentifier);

    /**
     * Set parameters from another WorkerParameters
     *
     * @param parameters the parameters to set
     * @return the current instance of WorkerParameters
     * @throws IllegalArgumentException if parameters is null
     */
    WorkerParameters setFromParameters(WorkerParameters parameters);

    /**
     * @return the current X-Request-Id
     */
    String getRequestId();

    /**
     * setRequestId.
     *
     * @param newRequestId
     * @return
     */
    WorkerParameters setRequestId(String newRequestId);

    /**
     * @return workflowStatusKo
     */
    String getWorkflowStatusKo();
}
