/*
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
package fr.gouv.vitam.logbook.common.server.database.collections;

import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;

/**
 * Enum with all possible logbook life cycle parameters <br />
 * <br />
 * Use to set parameter value and to check emptiness or nullity
 */
public enum LogbookLifeCycleMongoDbName {
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
     * objectIdentifier
     */
    objectIdentifier("obId", LogbookParameterName.objectIdentifier),
    /**
     * eventDetailData
     */
    eventDetailData("evDetData", LogbookParameterName.eventDetailData);

    private final String dbname;
    private final LogbookParameterName parameter;

    private LogbookLifeCycleMongoDbName(String dbname, LogbookParameterName parameter) {
        this.dbname = dbname;
        this.parameter = parameter;
    }

    /**
     *
     * @return the corresponding dbname
     */
    public final String getDbname() {
        return dbname;
    }

    /**
     *
     * @return the corresponding {@link LogbookParameterName}
     */
    public final LogbookParameterName getLogbookParameterName() {
        return parameter;
    }

    /**
     *
     * @param name as {@link LogbookParameterName}
     * @return the corresponding {@link LogbookLifeCycleMongoDbName}
     */
    public static final LogbookLifeCycleMongoDbName getLogbookLifeCycleMongoDbName(LogbookParameterName name) {
        return LogbookLifeCycleMongoDbName.valueOf(name.toString());
    }

    /**
     *
     * @param name as db field name
     * @return the corresponding {@link LogbookLifeCycleMongoDbName}
     */
    public static final LogbookLifeCycleMongoDbName getFromDbname(String name) {
        switch (name) {
            case "agId":
                return LogbookLifeCycleMongoDbName.agentIdentifier;
            case "evDateTime":
                return LogbookLifeCycleMongoDbName.eventDateTime;
            case "evId":
                return LogbookLifeCycleMongoDbName.eventIdentifier;
            case "evParentId":
                return LogbookLifeCycleMongoDbName.parentEventIdentifier;
            case "evIdProc":
                return LogbookLifeCycleMongoDbName.eventIdentifierProcess;
            case "evType":
                return LogbookLifeCycleMongoDbName.eventType;
            case "evTypeProc":
                return LogbookLifeCycleMongoDbName.eventTypeProcess;
            case "obId":
                return LogbookLifeCycleMongoDbName.objectIdentifier;
            case "outDetail":
                return LogbookLifeCycleMongoDbName.outcomeDetail;
            case "outMessg":
                return LogbookLifeCycleMongoDbName.outcomeDetailMessage;
            case "outcome":
                return LogbookLifeCycleMongoDbName.outcome;
            case "evDetData":
                return LogbookLifeCycleMongoDbName.eventDetailData;
            default:
                throw new IllegalArgumentException("Unknown name: " + name);
        }
    }
}
