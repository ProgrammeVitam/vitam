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
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import org.bson.Document;

/**
 * Logbook LifeCycle Unit item
 */
public class LogbookLifeCycleUnit extends LogbookLifeCycle<LogbookLifeCycleUnitParameters> {
    private static final long serialVersionUID = 5493682980103747369L;

    /**
     * use for jackson serialisation
     */
    public LogbookLifeCycleUnit() {
    }

    /**
     * Constructor from LogbookLifeCycleUnitParameters
     *
     * @param parameters LogbookLifeCycleUnitParameters
     * @throws IllegalArgumentException if argument is null
     */
    public LogbookLifeCycleUnit(LogbookLifeCycleUnitParameters parameters) {
        super(parameters);
    }


    /**
     * Constructor for Codec
     *
     * @param content of format Document to create LogbookLifeCycleUnit
     */
    public LogbookLifeCycleUnit(Document content) {
        super(content);
    }

    /**
     * Constructor for Codec
     *
     * @param content of format String to create LogbookLifeCycleUnit
     */
    public LogbookLifeCycleUnit(String content) {
        super(content);
    }

    /**
     * Constructor for Codec
     *
     * @param content of format JsonNode to create LogbookLifeCycleUnit
     */
    public LogbookLifeCycleUnit(JsonNode content) {
        super(content);
    }

    @Override
    public VitamDocument<LogbookLifeCycle<LogbookLifeCycleUnitParameters>> newInstance(
        JsonNode content) {
        return new LogbookLifeCycleUnit(content);
    }
}
