/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.logbook.common.server.database.collections;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.BasicDBObject;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;

import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument.RIGHTS_STATEMENT_IDENTIFIER;

/**
 * Logbook Operation item
 */
public class LogbookOperation extends VitamDocument<LogbookOperation> {
    private static final long serialVersionUID = -8343195329673741173L;

    /**
     * ES Mapping
     */
    public static final String TYPEUNIQUE = "typeunique";

    /**
     * Events
     */
    public static final String EVENTS = "events";


    /**
     * Mapping of this Collection.
     */
    public static final String MAPPING = "{" + TYPEUNIQUE + ": {" +
        "properties : { " + LogbookOperation.EVENTS + " : { type : \"object\", enabled : true, " +
        "properties : { " + LogbookMongoDbName.eventDetailData.getDbname() +
        ": { type : \"object\",  enabled : true , " +
        "properties : { " +
        "evDetDataType: { type: \"string\", index: \"not_analyzed\" }, " +
        "LogType : { type : \"string\", index : \"not_analyzed\" }, " +
        "StartDate : { type : \"date\", index : \"analyzed\" }, " +
        "EndDate : { type : \"date\", index : \"analyzed\" }, " +
        "PreviousLogbookTraceabilityDate : { type : \"date\", index : \"analyzed\" }, " +
        "MinusOneMonthLogbookTraceabilityDate : { type : \"date\", index : \"analyzed\" }, " +
        "MinusOneYearLogbookTraceabilityDate : { type : \"date\", index : \"analyzed\" }, " +
        "Hash : { type : \"string\", index : \"not_analyzed\" }, " +
        "TimeStampToken : { type : \"string\", index : \"not_analyzed\" }, " +
        "NumberOfElement : { type : \"long\", index : \"analyzed\" }, " +
        "Size : { type : \"long\", index : \"analyzed\" }, " +
        "FileName : { type : \"string\", index : \"not_analyzed\" }, " +
        "EvDetailReq: { type: \"string\", index: \"not_analyzed\" }, " +
        "EvDateTimeReq: { type: \"date\", index: \"not_analyzed\" }, " +
        "ArchivalAgreement: { type: \"string\", index: \"not_analyzed\" }, " +
        "ServiceLevel: { type: \"string\", index: \"not_analyzed\" }" +
        "AccessStatus: { type: \"string\", index: \"not_analyzed\" }" +
        " } } " + // end eventDetailData
        ", evTypeProc : { type : \"string\", index : \"not_analyzed\" } " +
        ", evType : { type : \"string\", index : \"not_analyzed\" } " +
        ", outcome : { type : \"string\", index : \"not_analyzed\" } " +
        ", outDetail : { type : \"string\", index : \"not_analyzed\" } " +
        ", evDateTime : { type : \"date\", index : \"not_analyzed\" } " +
        " } } " + // end events
        ", evTypeProc : { type : \"string\", index : \"not_analyzed\" } " +
        ", evType : { type : \"string\", index : \"not_analyzed\" } " +
        ", outcome : { type : \"string\", index : \"not_analyzed\" } " +
        ", outDetail : { type : \"string\", index : \"not_analyzed\" } " +
        ", evDateTime : { type : \"date\", index : \"not_analyzed\" } " +
        " } } }";


    /**
     * Constructor from LogbookOperationParameters
     *
     * @param parameters to create logbook operation
     * @throws IllegalArgumentException if argument is null
     */
    public LogbookOperation(LogbookOperationParameters parameters) {
        ParametersChecker.checkParameter("parameters", parameters);
        // Fill information using LogbookMongoDbName
        final Map<LogbookParameterName, String> map = parameters.getMapParameters();
        for (final LogbookMongoDbName name : LogbookMongoDbName.values()) {
            append(name.getDbname(), map.get(name.getLogbookParameterName()));
        }
        append(LogbookDocument.EVENTS, Arrays.asList(new String[0]));
        append(RIGHTS_STATEMENT_IDENTIFIER, "");
        checkId();
    }

    /**
     * Constructor from LogbookOperationParameters for update purpose
     *
     * @param parameters the logbook parameters
     * @param forUpdate specifies if the parameters should not contain fields for update
     * @throws IllegalArgumentException if argument is null
     */
    public LogbookOperation(LogbookOperationParameters parameters, boolean forUpdate) {
        ParametersChecker.checkParameter("parameters", parameters);
        // Fill information using LogbookMongoDbName
        final Map<LogbookParameterName, String> map = parameters.getMapParameters();
        for (final LogbookMongoDbName name : LogbookMongoDbName.values()) {
            if (forUpdate) {
                switch (name) {
                    case agentIdentifierApplication:
                    case agentIdentifierApplicationSession:
                    case agIdExt:

                        break;
                    default:
                        append(name.getDbname(), map.get(name.getLogbookParameterName()));
                }
            } else {
                append(name.getDbname(), map.get(name.getLogbookParameterName()));
            }
        }

    }

    /**
     * Constructor for Codec
     *
     * @param content of format Document to create LogbookOperation
     */
    public LogbookOperation(Document content) {
        super(content);
    }

    /**
     * Constructor for Codec
     *
     * @param content of format String to create LogbookOperation
     */
    public LogbookOperation(String content) {
        super(content);
    }

    /**
     * Constructor for Codec
     *
     * @param content of format JsonNode to create LogbookOperation
     */
    public LogbookOperation(JsonNode content) {
        super(content);
    }

    @Override
    public VitamDocument<LogbookOperation> newInstance(JsonNode content) {
    	return new LogbookOperation(content);
    }

    static final LogbookMongoDbName getIdName() {
        return LogbookMongoDbName.eventIdentifierProcess;
    }

    /**
     *
     * @return the ParameterName as id in collection
     */
    public static final LogbookParameterName getIdParameterName() {
        return LogbookParameterName.eventIdentifierProcess;
    }

    @Override
    public String getId() {
        return getString(getIdName().getDbname());
    }

    /**
     *
     * @return the equivalent unique Operation
     */
    private LogbookOperationParameters getOperation(Bson object) {
        final LogbookOperationParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
        final Map<LogbookParameterName, String> map = parameters.getMapParameters();
        if (object instanceof BasicDBObject) {
            for (final LogbookMongoDbName name : LogbookMongoDbName.values()) {
                map.put(name.getLogbookParameterName(),
                    ((BasicDBObject) object).getString(name.getDbname()));
            }
        } else if (object instanceof Document) {
            for (final LogbookMongoDbName name : LogbookMongoDbName.values()) {
                map.put(name.getLogbookParameterName(),
                    ((Document) object).getString(name.getDbname()));
            }
        }
        return parameters;
    }

    /**
     * Get back LogbookOperationParameters
     *
     * @param all If true returns all items, if false return the first item and last item if any
     * @return the list of Operations
     */
    public List<LogbookOperationParameters> getOperations(boolean all) {
        @SuppressWarnings("unchecked")
        ArrayList<Document> events = (ArrayList<Document>) get(LogbookDocument.EVENTS);
        if (events == null) {
            events = new ArrayList<>();
        }
        final int nb = all ? events.size() : events.isEmpty() ? 1 : 2;
        final List<LogbookOperationParameters> list = new ArrayList<>(nb);
        list.add(getOperation(this));
        if (all) {
            for (final Document eventob : events) {
                list.add(getOperation(eventob));
            }
        } else {
            final Document eventob = events.get(events.size() - 1);
            list.add(getOperation(eventob));
        }
        return list;
    }

    /**
     * Initialize indexes for Collection
     */
    static final void addIndexes() {
        // TODO P1
    }

    /**
     * Drop indexes for Collection
     */
    static final void dropIndexes() {
        // TODO P1

    }
}
