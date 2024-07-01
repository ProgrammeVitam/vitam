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
package fr.gouv.vitam.storage.engine.server.storagelog.parameters;

/**
 * Enum with all possible logbook storage parameters <br>
 * <br>
 * Use to set parameter value and to check emptiness or nullity
 */
public enum StorageLogbookParameterName {
    /**
     * Set by the storage engine : date time of the event
     */
    eventDateTime,
    /**
     * X-Request-Id
     */
    xRequestId,
    /**
     * Application-Id
     */
    applicationId,
    /**
     * Tenant ID
     */
    tenantId,
    /**
     * Event type: should use one global Enum ActionType <br>
     * <br>
     * Could be for instance: "Unzip", "CheckSeda", ...
     */
    eventType,
    /**
     * Object Identifier.<br>
     * Identifier of the object to be written in the storage offer. <br>
     * For instance: Object GUID<br>
     */
    objectIdentifier,
    /**
     * Data category
     */
    dataCategory,
    /**
     * Object Group Identifier.<br>
     * Identifier of the object group corresponding to the object written in the
     * storage offer. <br>
     * For instance: Object Group GUID<br>
     */
    objectGroupIdentifier,
    /**
     * Digest of the written object.
     */
    digest,
    /**
     * digest Algorithm used for this object Could be for instance: "SHA-256",
     * "SHA-512", ...
     */
    digestAlgorithm,
    /**
     * Size in bytes of the object written on the storage offer
     */
    size,
    /**
     * List of the offers identifiers on which the object has been written
     */
    agentIdentifiers,
    /**
     * Requester of the storage order
     */
    agentIdentifierRequester,
    /**
     * Status between: "OK", "KO", "PENDING" <br>
     * <br>
     * One must use the StorageLogbookOutcome enum. <br>
     */
    outcome,
    /**
     * Object qualifier in the objectGroup
     */
    qualifier,
    /**
     * Object version in the objectGroup
     */
    version,
    /**
     * Context Id of the request
     */
    contextId,
    /**
     * Access Contract Id of the request
     */
    contractId,
    /**
     * Archive Units ID with contains the object
     */
    archivesId,
    /**
     * Object Size
     */
    objectSize,
}
