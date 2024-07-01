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

import fr.gouv.vitam.common.ParametersChecker;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Worker parameters factory </br>
 * Factory to get WorkerParameters object
 *
 * Example:
 *
 * <pre>
 *     {@code
 *      // Retrieve worker parameters with standard required fields
 *      WorkerParameters parameters = WorkerParametersFactory.newWorkerParameters();
 *
 *      // Retrieve worker parameters with standard required fields and specifics required fields
 *      Set<WorkerParameterName> specificMandatoryFields = new HashSet<>()
 *      // add specific fields
 *      specificMandatoryFields.add(WorkerParameterName.objectId);
 *      specificMandatoryFields.add(WorkerParameterName.objectName);
 *
 *      // Retrieve parameter object
 *      parameters = WorkerParametersFactory.newWorkerParameters(specificMandatoryFields);
 *     }
 * </pre>
 */

// FIXME P1 : see LogbookParametersFactory and create a common factory
// for all "vitam parameters" using generic (for example)
public class WorkerParametersFactory {

    private static final Set<WorkerParameterName> genericMandatories = new HashSet<>();

    static {
        genericMandatories.add(WorkerParameterName.urlMetadata);
        genericMandatories.add(WorkerParameterName.urlWorkspace);
        genericMandatories.add(WorkerParameterName.objectNameList);
        genericMandatories.add(WorkerParameterName.currentStep);
        genericMandatories.add(WorkerParameterName.containerName);
    }

    private WorkerParametersFactory() {
        // do nothing
    }

    /**
     * Get a new empty WorkerParameters object with specific mandatory
     *
     * @param mandatoryFieldsToAdd set of WorkerParameterName to add to the default mandatory fields, can be null
     * @return the new instance of WorkerParameters
     */
    static DefaultWorkerParameters newWorkerParameters(Set<WorkerParameterName> mandatoryFieldsToAdd) {
        return new DefaultWorkerParameters(initMandatoriesParameters(mandatoryFieldsToAdd));
    }

    /**
     * Get a new empty WorkerParameters object
     *
     * @return the new instance of WorkerParameters
     */
    public static DefaultWorkerParameters newWorkerParameters() {
        return new DefaultWorkerParameters(initMandatoriesParameters(null));
    }

    /**
     * Get a new WorkerParameters object
     *
     * @param processId unique id (GUID) of the workflow to be executed (can be null)
     * @param stepUniqId unique id of a step. The pattern of the id is :
     * {CONTAINER_NAME}_{WORKFLOW_ID}_{STEP_RANK_IN_THE_WORKFLOW}_{STEP_NAME}
     * @param containerName name of the container to be uploaded
     * @param currentStep current name of the step to be processed
     * @param objectNameList List name/path of the object to be processed
     * @param urlMetadata url of metadata resources
     * @param urlWorkspace url of workspace resources
     * @return the new instance of WorkerParameters
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public static DefaultWorkerParameters newWorkerParameters(
        String processId,
        String stepUniqId,
        String containerName,
        String currentStep,
        List<String> objectNameList,
        String urlMetadata,
        String urlWorkspace
    ) {
        ParametersChecker.checkParameter(
            "Parameters cannot be null or empty",
            processId,
            stepUniqId,
            containerName,
            currentStep,
            objectNameList,
            urlMetadata,
            urlWorkspace
        );
        final DefaultWorkerParameters parameters = new DefaultWorkerParameters(initMandatoriesParameters(null));
        parameters.putParameterValue(WorkerParameterName.processId, processId);
        parameters.putParameterValue(WorkerParameterName.stepUniqId, stepUniqId);
        parameters.putParameterValue(WorkerParameterName.containerName, containerName);
        parameters.putParameterValue(WorkerParameterName.currentStep, currentStep);
        parameters.setObjectNameList(objectNameList);
        parameters.putParameterValue(WorkerParameterName.urlMetadata, urlMetadata);
        parameters.putParameterValue(WorkerParameterName.urlWorkspace, urlWorkspace);
        return parameters;
    }

    /**
     * Get default mandatory fields
     *
     * @return the default mandatory fields set
     */
    static Set<WorkerParameterName> getDefaultMandatory() {
        return Collections.unmodifiableSet(new HashSet<>(genericMandatories));
    }

    /**
     * Initialize mandatory fields
     *
     * @param mandatoryFieldsToAdd the mandatory fields to add to default ones
     * @return the new Set of parameter names
     */
    private static Set<WorkerParameterName> initMandatoriesParameters(Set<WorkerParameterName> mandatoryFieldsToAdd) {
        final Set<WorkerParameterName> mandatory = new HashSet<>(genericMandatories);
        if (mandatoryFieldsToAdd != null) {
            mandatory.addAll(mandatoryFieldsToAdd);
        }
        return Collections.unmodifiableSet(mandatory);
    }
}
