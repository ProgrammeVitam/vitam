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
package fr.gouv.vitam.ihmdemo.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used in order to make transformations on Json objects received from Vitam
 */
public final class JsonTransformer {

    private static final String MISSING_ID_ERROR_MSG = "Encountered Missing ID field in the given parents list.";
    private static final String MISSING_UP_ERROR_MSG = "Encountered Missing _up field in the given parents list.";
    private static final String INVALID_UP_FIELD_ERROR_MSG = "Encountered invalid _up field in the given parents list.";
    private static final String MISSING_UNIT_ID_ERROR_MSG = "The unit details is missing.";
    private static final String EVENT_DATE_TIME_FIELD = "evDateTime";
    private static final String EVENTS_FIELD = "events";
    private static final String START_EVENT_DATETIME_HEADER = "startEventDateTime";
    private static final String END_EVENT_DATETIME_HEADER = "endEventDateTime";
    private static final String EXECUTION_TIME_HEADER = "executionTime (ms)";
    private static final char SEMI_COLON_SEPARATOR = ';';
    private static final char RECORD_SEPARATOR = '\n';
    private static final String UNEXPECTED_EXCEPTION_DURING_CSV_FILE_GENERATION =
        "An unexpected error occurred during CSV file generation process";

    private JsonTransformer() {
        // empty constructor
    }

    /**
     * This method transforms ResultObjects so thr IHM could display results
     *
     * @param searchResult the Json to be transformed
     * @return the transformed JsonNode
     */
    public static JsonNode transformResultObjects(JsonNode searchResult) {
        ParametersChecker.checkParameter("Result cannot be empty", searchResult);
        final ObjectNode resultNode = JsonHandler.createObjectNode();
        long nbObjects = 0;
        final JsonNode result = searchResult.get("$results").get(0);
        final ArrayNode arrayNode = JsonHandler.createArrayNode();
        if (result != null) {
            final JsonNode qualifiers = result.get("#qualifiers");
            final List<JsonNode> versions = qualifiers.findValues("versions");
            final Map<String, Integer> usages = new HashMap<>();
            for (final JsonNode version : versions) {
                for (final JsonNode object : version) {
                    arrayNode.add(getDataObject(usages, object));
                    nbObjects++;
                }
            }
        }
        resultNode.put("nbObjects", nbObjects);
        resultNode.set("versions", arrayNode);
        return resultNode;
    }

    /**
     * Retrieve DataObject infos from object version.
     *
     * @param usages usage
     * @param object object
     * @return ObjectNode for DataObject version
     */
    private static ObjectNode getDataObject(final Map<String, Integer> usages, final JsonNode object) {
        final ObjectNode objectNode = JsonHandler.createObjectNode();
        objectNode.put("#id", object.get(UiConstants.ID.getResultCriteria()).asText());
        final String usage = object.get("DataObjectVersion").asText();
        final JsonNode finalInfo = object.get("FileInfo");
        if (usages.containsKey(usage)) {
            final Integer rank = usages.get(usage) + 1;
            objectNode.put("Rank", rank);
        } else {
            usages.put(usage, 0);
            objectNode.put("Rank", 0);
        }
        objectNode.put("DataObjectVersion", usage);
        if (object.get("Size") != null) {
            objectNode.put("Size", object.get("Size").asText());
        }
        if (finalInfo != null && finalInfo.get("LastModified") != null) {
            objectNode.put("LastModified", finalInfo.get("LastModified").asText());
        }
        if (
            object.get("FormatIdentification") != null &&
            object.get("FormatIdentification").get("FormatLitteral") != null
        ) {
            objectNode.put("FormatLitteral", object.get("FormatIdentification").get("FormatLitteral").asText());
        }
        if (finalInfo != null && finalInfo.get("Filename") != null) {
            objectNode.put("FileName", finalInfo.get("Filename").asText());
        }
        objectNode.set("metadatas", object);
        return objectNode;
    }

    /**
     * This method builds an ObjectNode based on a list of JsonNode object
     *
     * @param unitId achive unit
     * @param allParents list of JsonNode Objects used to build the referential
     * @return An ObjectNode where the key is the identifier and the value is the parent details (Title, Id, _up)
     * @throws VitamException if error when creating parent ObjectNode
     */
    public static ObjectNode buildAllParentsRef(String unitId, JsonNode allParents) throws VitamException {
        ParametersChecker.checkParameter("Result cannot be empty", allParents);

        boolean hasUnitId = false;

        final ObjectNode allParentsRef = JsonHandler.createObjectNode();
        for (final JsonNode currentParentNode : allParents) {
            if (!currentParentNode.has(UiConstants.ID.getResultCriteria())) {
                throw new VitamException(MISSING_ID_ERROR_MSG);
            }

            if (!currentParentNode.has(UiConstants.UNITUPS.getResultCriteria())) {
                throw new VitamException(MISSING_UP_ERROR_MSG);
            }

            if (!currentParentNode.get(UiConstants.UNITUPS.getResultCriteria()).isArray()) {
                throw new VitamException(INVALID_UP_FIELD_ERROR_MSG);
            }

            final String currentParentId = currentParentNode.get(UiConstants.ID.getResultCriteria()).asText();
            allParentsRef.set(currentParentId, currentParentNode);

            if (unitId.equalsIgnoreCase(currentParentId)) {
                hasUnitId = true;
            }
        }

        if (!hasUnitId) {
            throw new VitamException(MISSING_UNIT_ID_ERROR_MSG);
        }

        return allParentsRef;
    }

    /**
     * Generates execution time by step relative to a logbook operation
     *
     * @param logbookOperation logbook operation in JsonNode format
     * @return CSV report logbook
     * @throws VitamException if unexpected error in CSV file generation process
     * @throws IOException if error when write output stream
     * @throws Exception if error in others cases
     */
    public static ByteArrayOutputStream buildLogbookStatCsvFile(JsonNode logbookOperation)
        throws VitamException, IOException {
        final ByteArrayOutputStream csvOutputStream = new ByteArrayOutputStream();
        try (Writer csvWriter = new BufferedWriter(new OutputStreamWriter(csvOutputStream));) {
            // Total execution time
            final String startOperationTimeStr = logbookOperation.get(EVENT_DATE_TIME_FIELD).asText();

            final List<JsonNode> events = IteratorUtils.toList(logbookOperation.get(EVENTS_FIELD).iterator());

            // Last event
            final JsonNode lastEvent = events.get(events.size() - 1);
            final String endOperationTimeStr = lastEvent.get(EVENT_DATE_TIME_FIELD).asText();

            // Generate CSV report
            final CSVPrinter csvPrinter = new CSVPrinter(
                csvWriter,
                CSVFormat.newFormat(SEMI_COLON_SEPARATOR).withRecordSeparator(RECORD_SEPARATOR)
            );

            final List<String> header = IteratorUtils.toList(lastEvent.fieldNames());
            header.add(START_EVENT_DATETIME_HEADER);
            header.add(END_EVENT_DATETIME_HEADER);
            header.add(EXECUTION_TIME_HEADER);
            csvPrinter.printRecord(header);

            for (int i = 0; i < events.size() - 1; i += 2) {
                final JsonNode startEvent = events.get(i);
                final JsonNode endEvent = events.get(i + 1);
                final List<String> eventReportDetails = IteratorUtils.toList(endEvent.elements());
                final String startEventDateTimeStr = startEvent.get(EVENT_DATE_TIME_FIELD).asText();
                final String endEventDateTimeStr = endEvent.get(EVENT_DATE_TIME_FIELD).asText();

                eventReportDetails.add(startEventDateTimeStr);
                eventReportDetails.add(endEventDateTimeStr);
                eventReportDetails.add(calculateExecutionTime(startEventDateTimeStr, endEventDateTimeStr).toString());

                csvPrinter.printRecord(eventReportDetails);
            }

            // Last Event
            final List<String> lastEventDetails = IteratorUtils.toList(lastEvent.elements());
            lastEventDetails.add(startOperationTimeStr);
            lastEventDetails.add(endOperationTimeStr);
            lastEventDetails.add(calculateExecutionTime(startOperationTimeStr, endOperationTimeStr).toString());
            csvPrinter.printRecord(lastEventDetails);

            csvPrinter.flush();
            csvPrinter.close();
        } catch (final Exception e) {
            csvOutputStream.close();
            throw new VitamException(UNEXPECTED_EXCEPTION_DURING_CSV_FILE_GENERATION);
        }

        return csvOutputStream;
    }

    private static Long calculateExecutionTime(String startDateTimeStr, String endDateTimeStr) throws ParseException {
        final Date startDateTime = LocalDateUtil.getDate(startDateTimeStr);
        final Date endDateTime = LocalDateUtil.getDate(endDateTimeStr);
        return endDateTime.getTime() - startDateTime.getTime();
    }
}
