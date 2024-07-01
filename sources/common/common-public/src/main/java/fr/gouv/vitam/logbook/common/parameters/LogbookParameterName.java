/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.logbook.common.parameters;

/**
 * Enum with all possible logbook parameters <br />
 * <br />
 * Use to set parameter value and to check emptiness or nullity
 */
public enum LogbookParameterName {
    /**
     * Operation identifier: unique identifier created through GUIDFactory.newOperationIdGUID(tenant) at starting call
     * and reused in end call (ok or error).<br>
     * <br>
     * It is the identifier of one action/step within one process (workflow).
     */
    eventIdentifier,

    /**
     * Parent event identifier: created for every child event to link it to the parent event/opration/task
     * <br>
     * It is the identifier of the parent event if exists
     */
    parentEventIdentifier,

    /**
     * Event type: should use one global Enum ActionType <br>
     * <br>
     * Could be for instance: "Unzip", "CheckSeda", ...
     */
    eventType,
    /**
     * Set by the Logbook client: date time of the event
     */
    eventDateTime,
    /**
     * Process identifier: unique identifier for global operation workflow. <br>
     * Primary key for Operation
     */
    eventIdentifierProcess,
    /**
     * Event ProcessType: should use one global Enum ProcessType <br>
     * <br>
     * Could be for instance: "Ingest", "Audit", ...
     */
    eventTypeProcess,
    /**
     * Status between: "STARTED", "OK", "WARNING", "ERROR", "FATAL" <br>
     * <br>
     * One must use the StatusCode enum. <br>
     * <br>
     * Note that first call should be using "STARTED", while second should be one of the others.
     */
    outcome,
    /**
     * Vitam Code error as 404_nnnnnn where nnnnnn is the internal code error <br>
     * <br>
     * Can be null
     */
    outcomeDetail,
    /**
     * Message output, whatever the "outcome" status
     */
    outcomeDetailMessage,
    /**
     * Set by the Logbook client: Server identifier
     */
    agentIdentifier,
    /**
     * Name of the remote application <br>
     * <br>
     * Can be null
     */
    agentIdentifierApplication,
    /**
     * Hash of the personae certificate<br>
     * <br>
     * Can be null
     */
    agentIdentifierPersonae,
    /**
     * Session (X-Application-Id) from the remote application <br>
     * <br>
     * Can be null
     */
    agentIdentifierApplicationSession,
    /**
     * W-Request-Id from top request
     */
    eventIdentifierRequest,

    /**
     * Object Identifier of the "process". <br>
     * <br>
     * For instance: SIP GUID, but never ArchiveUnit GUID for an Operation<br>
     * For instance: ArchiveUnit GUID for a LifeCycle<br>
     * For instance: GOT GUID for a LifeCycle<br>
     * For instance: BDO GUID for a LifeCycle event of a distributed task<br>
     * <br>
     * Object Identifier <br>
     * One of objectIdentifierRequest and objectIdentifier can be null but not both
     */
    objectIdentifier,

    /**
     * Lifecycle Identifier of the "process". <br>
     * <br>
     * For instance: ArchiveUnit GUID for a LifeCycle.<br>
     * For instance: GOT GUID for a LifeCycle.<br>
     * <br>
     * Primary key for LifeCycle <br>
     * If null objectIdentifier is used to determine the LifeCycle to update.
     */
    lifeCycleIdentifier,

    /**
     * Object Identifier of the "process" using a request <br>
     * <br>
     * One of objectIdentifierRequest and objectIdentifier can be null but not both
     */
    objectIdentifierRequest,
    /**
     * External Object Identifier on the current "process <br>
     * <br>
     * For instance: from Ingest, in the manifest.xml, this field is "MessageIdentifier" <br>
     * <br>
     * Can be null
     */
    objectIdentifierIncome,
    /**
     * Master Data Identifier
     */
    masterData,

    /**
     * Master Data Identifier
     */
    rightsStatementIdentifier,

    /**
     * Master Data Identifier
     */
    agIdExt,
    /**
     * Event Detail Data on the current "process <br>
     * <br>
     * Can be null
     */
    eventDetailData,
    /**
     * events list
     */
    events,
}
