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

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import org.bson.Document;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for Logbook LifeCycle item
 *
 * @param <T> template
 */
public class LogbookLifeCycle<T> extends VitamDocument<LogbookLifeCycle<T>> {

    private static final long serialVersionUID = 105654500015427902L;

    /**
     * Constructor
     */
    public LogbookLifeCycle() {}

    /**
     * Constructor from LogbookLifeCycleParameters
     *
     * @param parameters of type LogbookParameters
     * @throws IllegalArgumentException if argument is null
     */
    public LogbookLifeCycle(LogbookParameters parameters) {
        ParametersChecker.checkParameter("parameters", parameters);
        // Fill information using LogbookLifeCycleMongoDbName
        final Map<LogbookParameterName, String> map = parameters.getMapParameters();
        for (final LogbookLifeCycleMongoDbName name : LogbookLifeCycleMongoDbName.values()) {
            append(name.getDbname(), map.get(name.getLogbookParameterName()));
        }
        append(LogbookDocument.EVENTS, Collections.emptyList());
        checkId();
    }

    /**
     * Constructor for Codec
     *
     * @param content in format Document
     */
    public LogbookLifeCycle(Document content) {
        super(content);
    }

    @Override
    public VitamDocument<LogbookLifeCycle<T>> newInstance(JsonNode content) {
        return new LogbookLifeCycle<>(content);
    }

    /**
     * Constructor for Codec
     *
     * @param content in format String
     */
    public LogbookLifeCycle(String content) {
        super(content);
    }

    /**
     * Constructor for Codec
     *
     * @param content in format JsonNode
     */
    public LogbookLifeCycle(JsonNode content) {
        super(content);
    }

    static LogbookMongoDbName getIdName() {
        return LogbookMongoDbName.objectIdentifier;
    }

    /**
     * @return the ParameterName as id in collection
     */
    static LogbookParameterName getIdParameterName() {
        return LogbookParameterName.objectIdentifier;
    }

    @Override
    public final String getId() {
        return getString(getIdName().getDbname());
    }

    public List<Document> events() {
        return getList(LogbookDocument.EVENTS, Document.class);
    }
}
