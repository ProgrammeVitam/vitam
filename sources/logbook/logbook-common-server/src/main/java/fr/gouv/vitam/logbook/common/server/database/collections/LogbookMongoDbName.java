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
package fr.gouv.vitam.logbook.common.server.database.collections;

import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;

/**
 * Enum with all possible logbook parameters <br />
 * <br />
 * Use to set parameter value and to check emptiness or nullity
 */
public enum LogbookMongoDbName {
    /**
     * eventIdentifier
     */
    eventIdentifier("evId", LogbookParameterName.eventIdentifier),

    /**
     * parentEventIdentifier
     */
    parentEventIdentifier("evParentId", LogbookParameterName.parentEventIdentifier),

    /**
     * eventType
     */
    eventType("evType", LogbookParameterName.eventType),
    /**
     * eventDateTime
     */
    eventDateTime("evDateTime", LogbookParameterName.eventDateTime),
    /**
     * eventDetailData
     */
    eventDetailData("evDetData", LogbookParameterName.eventDetailData),
    /**
     * eventIdentifierProcess
     */
    eventIdentifierProcess("evIdProc", LogbookParameterName.eventIdentifierProcess),
    /**
     * eventTypeProcess
     */
    eventTypeProcess("evTypeProc", LogbookParameterName.eventTypeProcess),
    /**
     * outcome
     */
    outcome("outcome", LogbookParameterName.outcome),
    /**
     * outcomeDetail
     */
    outcomeDetail("outDetail", LogbookParameterName.outcomeDetail),
    /**
     * outcomeDetailMessage
     */
    outcomeDetailMessage("outMessg", LogbookParameterName.outcomeDetailMessage),
    /**
     * agentIdentifier
     */
    agentIdentifier("agId", LogbookParameterName.agentIdentifier),
    /**
     * agentIdentifierApplication
     */
    agentIdentifierApplication("agIdApp", LogbookParameterName.agentIdentifierApplication),
    /**
     * agentIdentifierApplication
     */
    agentIdentifierPersonae("agIdPers", LogbookParameterName.agentIdentifierPersonae),
    /**
     * agentIdentifierApplicationSession
     */
    agentIdentifierApplicationSession("evIdAppSession", LogbookParameterName.agentIdentifierApplicationSession),
    /**
     * eventIdentifierRequest
     */
    eventIdentifierRequest("evIdReq", LogbookParameterName.eventIdentifierRequest),

    /**
     * agencies details
     */
    agIdExt("agIdExt", LogbookParameterName.agIdExt),
    /**
     * rightsStatementIdentifier
     */
    rightsStatementIdentifier("rightsStatementIdentifier", LogbookParameterName.rightsStatementIdentifier),

    /**
     * objectIdentifier
     */
    objectIdentifier("obId", LogbookParameterName.objectIdentifier),
    /**
     * objectIdentifierRequest
     */
    objectIdentifierRequest("obIdReq", LogbookParameterName.objectIdentifierRequest),
    /**
     * objectIdentifierIncome
     */
    objectIdentifierIncome("obIdIn", LogbookParameterName.objectIdentifierIncome);

    private final String dbname;
    private final LogbookParameterName parameter;

    private LogbookMongoDbName(String dbname, LogbookParameterName parameter) {
        this.dbname = dbname;
        this.parameter = parameter;
    }

    /**
     * @return the corresponding dbname
     */
    public final String getDbname() {
        return dbname;
    }

    /**
     * @return the corresponding {@link LogbookParameterName}
     */
    public final LogbookParameterName getLogbookParameterName() {
        return parameter;
    }

    /**
     * @param name as {@link LogbookParameterName}
     * @return the corresponding {@link LogbookMongoDbName}
     */
    public static final LogbookMongoDbName getLogbookMongoDbName(LogbookParameterName name) {
        return LogbookMongoDbName.valueOf(name.toString());
    }

    /**
     * @param name as db field name
     * @return the corresponding {@link LogbookMongoDbName}
     */
    public static final LogbookMongoDbName getFromDbname(String name) {
        switch (name) {
            case "agId":
                return LogbookMongoDbName.agentIdentifier;
            case "agIdApp":
                return LogbookMongoDbName.agentIdentifierApplication;
            case "evIdAppSession":
                return LogbookMongoDbName.agentIdentifierApplicationSession;
            case "agIdExt":
                return LogbookMongoDbName.agIdExt;
            case "rightsStatementIdentifier":
                return LogbookMongoDbName.rightsStatementIdentifier;
            case "evDateTime":
                return LogbookMongoDbName.eventDateTime;
            case "evId":
                return LogbookMongoDbName.eventIdentifier;
            case "evParentId":
                return LogbookMongoDbName.parentEventIdentifier;
            case "evIdProc":
                return LogbookMongoDbName.eventIdentifierProcess;
            case "evDetData":
                return LogbookMongoDbName.eventDetailData;
            case "evIdReq":
                return LogbookMongoDbName.eventIdentifierRequest;
            case "evType":
                return LogbookMongoDbName.eventType;
            case "evTypeProc":
                return LogbookMongoDbName.eventTypeProcess;
            case "obId":
                return LogbookMongoDbName.objectIdentifier;
            case "obIdIn":
                return LogbookMongoDbName.objectIdentifierIncome;
            case "obIdReq":
                return LogbookMongoDbName.objectIdentifierRequest;
            case "outDetail":
                return LogbookMongoDbName.outcomeDetail;
            case "outMessg":
                return LogbookMongoDbName.outcomeDetailMessage;
            case "outcome":
                return LogbookMongoDbName.outcome;
            case "agIdPers":
                return LogbookMongoDbName.agentIdentifierPersonae;
            default:
                throw new IllegalArgumentException("Unknown name: " + name);
        }
    }
}
