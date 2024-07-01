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

import java.util.HashSet;

/**
 * Enum with all possible worker parameters <br />
 * <br />
 * Use to set parameter value and to check emptiness nullity
 */
public enum WorkerParameterName {
    /**
     * Url of metadata resources
     */
    urlMetadata,
    /**
     * Url of workspace resources
     */
    urlWorkspace,
    /**
     * Unique id (GUID) of the workflow to be executed
     */
    processId,
    /**
     * Unique id of a step. The pattern of the id is :
     * {CONTAINER_NAME}_{WORKFLOW_ID}_{STEP_RANK_IN_THE_WORKFLOW}_{STEP_NAME}
     */
    stepUniqId,
    /**
     * Name of the container to be uploaded
     */
    containerName,
    /**
     * Name/path of the object to be processed
     */
    objectName,
    /**
     * Name/metadata of the object to be processed
     */
    objectMetadata,

    /**
     * List Name/path of the object to be processed
     */
    objectNameList,

    /**
     * List Name/metadata of the object to be processed
     */
    objectMetadataList,

    /**
     * Id of the object to be processed (not used, except in test classes)
     */
    objectId,
    /**
     * Type of the audit to be processed
     */
    auditType,
    /**
     * Id of the worker (GUID) (not used for now, except in test classes)
     */
    workerGUID,
    /**
     * Request to be executed by the metadata module (not used for now, except in test classes)
     */
    metadataRequest,
    /**
     * Current name of the step to be processed
     */
    currentStep,
    /**
     * Previous name of the step processed
     */
    previousStep,
    /**
     * If Current Workflow status is greater or equal ko, contains the status
     */
    workflowStatusKo,

    /**
     * The LogbookTypeProcess value used in logbookOperation
     */
    logBookTypeProcess,
    /**
     * Request to be executed by the logbook module
     */
    logbookRequest,
    /**
     * Start operation date
     */
    startDate,
    /**
     * End operation date
     */
    endDate,

    /**
     * hash of root
     */
    hashRoot,

    /**
     * the array of the different audit actions
     * there are three types of action: "check existence" , "check integrity", "correction"
     */
    auditActions,

    /**
     * Request identifier.
     */
    requestId,

    /**
     * Temporization delay (in seconds) for recent logbook lifecycle events
     */
    lifecycleTraceabilityTemporizationDelayInSeconds,

    /**
     * Max event count to select during a single logbook lifecycle traceability workflow.
     */
    lifecycleTraceabilityMaxEntries,

    /**
     * Boolean that indicate for Export DIP workflow if the access to an object must be (or not) logged
     */
    mustLogAccessOnObject,

    /**
     * Workflow identifier (used also as eventType)
     */
    workflowIdentifier,

    /**
     * Ingest operation id to clean up
     */
    ingestOperationIdToCleanup;

    public static HashSet<String> getEnums() {
        HashSet<String> values = new HashSet<String>();

        for (WorkerParameterName c : WorkerParameterName.values()) {
            values.add(c.name());
        }

        return values;
    }
}
