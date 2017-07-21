/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.ihmrecette.appserver.performance;

import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.eventDateTime;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.eventType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;

/**
 *
 */
public class ReportGenerator implements AutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReportGenerator.class);

    private BufferedWriter writer;

    private boolean headerAlreadyGenerated;

    public ReportGenerator(Path path) throws IOException {
        this.writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE);
        headerAlreadyGenerated = false;
    }

    public void generateReport(String operationId, JsonNode logbookOperation) throws IOException, ParseException {
        if (!headerAlreadyGenerated) {
            generateReportHeader(logbookOperation);
        }
        generateReportLine(operationId, logbookOperation);
    }

    private void generateReportHeader(JsonNode logbookOperation) throws IOException {
        Set<String> headers = new LinkedHashSet<>();
        headers.add("operationId");
        headers.add(logbookOperation.get(eventType.getDbname()).asText());

        ArrayNode events = (ArrayNode) logbookOperation.get(LogbookDocument.EVENTS);
        events.iterator().forEachRemaining(event -> headers.add(event.get(eventType.getDbname()).asText()));

        writer.write(headers.stream().collect(Collectors.joining(",")));
        writer.newLine();
        headerAlreadyGenerated = true;
    }

    private void generateReportLine(String operationId, JsonNode logbookOperation) throws ParseException, IOException {
        String startOperation = logbookOperation.get(eventDateTime.getDbname()).asText();
        String firstEventType = logbookOperation.get(eventType.getDbname()).asText();

        ArrayNode events = (ArrayNode) logbookOperation.get(LogbookDocument.EVENTS);

        Map<String, Interval> map = new LinkedHashMap<>();
        map.put(firstEventType, new Interval(startOperation));

        events.iterator().forEachRemaining(event -> {
                String evType = event.get(eventType.getDbname()).asText();
                if (map.containsKey(evType)) {
                    String lastDate = event.get(eventDateTime.getDbname()).asText();
                    map.get(evType).setEndDate(lastDate);
                } else {
                    map.put(evType, new Interval(event.get(eventDateTime.getDbname()).asText()));
                }
            }
        );

        writer.write(String.format("%s,%s", operationId, map.entrySet().stream()
            .map(entry -> entry.getValue().total())
            .collect(Collectors.joining(","))));
        writer.newLine();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    private static class Interval {
        private String startDate;
        private String endDate;

        public Interval(String startDate) {
            this.startDate = startDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }

        public String total() {
            try {
                final Date startDateTime = LocalDateUtil.getDate(startDate);
                final Date endDateTime = LocalDateUtil.getDate(endDate);
                return String.valueOf(endDateTime.getTime() - startDateTime.getTime());
            } catch (DateTimeParseException | IllegalArgumentException | ParseException e) {
                LOGGER.error(String.format("unable to parse date: %s or %s", startDate, endDate));
                return "";
            }
        }
    }

}
