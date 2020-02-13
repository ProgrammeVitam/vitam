/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.functional.administration.rules.core;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * RulesManagerParser Manage the parse of a CSV file
 */

public class RulesManagerParser {

    static final String CREATION_DATE = "CreationDate";
    static final String UPDATE_DATE = "UpdateDate";

    /**
     * RulesManagerParser Constructor
     */
    RulesManagerParser() {

    }

    /**
     * readObjectsFromCsvWriteAsArrayNode : read objects from a csv file and write it as array node
     *
     * @param fileToParse rule file to read
     * @return ArrayNode json of rules date
     * @throws IOException                    when read file exception occurred
     * @throws InvalidParseOperationException when json parse exception occurred
     */
    public static ArrayNode readObjectsFromCsvWriteAsArrayNode(File fileToParse) throws IOException,
        InvalidParseOperationException {
        final CsvSchema bootstrap = CsvSchema.emptySchema().withHeader();
        final CsvMapper csvMapper = new CsvMapper();
        final MappingIterator<Map<?, ?>> mappingIterator =
            csvMapper.readerFor(Map.class).with(bootstrap).readValues(fileToParse);
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        final List<Map<?, ?>> data = mappingIterator.readAll();
        for (Map<?, ?> c : data) {
            final JsonNode node = JsonHandler.toJsonNode(c);
            //Trim withe space
            String ruleId = node.get("RuleId").asText();
            String escaped = ruleId.replaceAll("\"", " ");
            String trimmed = escaped.trim();
            final ObjectNode result = (ObjectNode) node;
            result.put("RuleId", trimmed);
            result.put(CREATION_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
            result.put(UPDATE_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));
            arrayNode.add(result);
        }
        return arrayNode;
    }
}
