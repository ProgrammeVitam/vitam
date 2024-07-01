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
package fr.gouv.vitam.ihmrecette.appserver.performance;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
public class ReportGenerator implements AutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReportGenerator.class);

    private static final String STARTED_SUFFIX = ".STARTED";

    private BufferedWriter writer;

    private boolean headerAlreadyGenerated;

    ReportGenerator(Path path) throws IOException {
        this.writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE);
        headerAlreadyGenerated = false;
    }

    void generateReport(String operationId, LogbookOperation logbookOperation) throws IOException {
        if (!headerAlreadyGenerated) {
            generateReportHeader(logbookOperation);
        }
        generateReportLine(operationId, logbookOperation);
    }

    private void generateReportHeader(LogbookOperation logbookOperation) throws IOException {
        Set<String> headers = new LinkedHashSet<>();
        headers.add("operationId");
        headers.add(logbookOperation.getEvType());

        List<LogbookEventOperation> events = logbookOperation.getEvents();
        events.iterator().forEachRemaining(event -> headers.add(event.getEvType().replace(STARTED_SUFFIX, "")));

        writer.write(String.join(",", headers));
        writer.newLine();
        headerAlreadyGenerated = true;
    }

    private void generateReportLine(String operationId, LogbookOperation logbookOperation) throws IOException {
        String referenceDate = logbookOperation.getEvDateTime();
        String firstEventType = logbookOperation.getEvType();

        List<LogbookEventOperation> events = logbookOperation.getEvents();

        Map<String, Interval> map = new LinkedHashMap<>();
        map.put(firstEventType, new Interval(referenceDate));

        for (LogbookEventOperation event : events) {
            String evType = event.getEvType();
            String evDateTime = event.getEvDateTime();

            // if step (step start event is logged with eventType.STARTED)
            if (evType.endsWith(STARTED_SUFFIX)) {
                evType = evType.replace(STARTED_SUFFIX, "");
                map.put(evType, new Interval(evDateTime));

                // update date reference
                referenceDate = evDateTime;
            } else {
                // if map contains key then it's a step end event
                if (map.containsKey(evType)) {
                    map.get(evType).setEndDate(evDateTime);
                } else {
                    // otherwise it's an action (only one event for actions)
                    Interval anInterval = new Interval(referenceDate);
                    anInterval.setEndDate(evDateTime);
                    map.put(evType, anInterval);

                    // update date reference
                    referenceDate = evDateTime;
                }
            }
        }

        writer.write(
            String.format(
                "%s,%s",
                operationId,
                map.values().stream().map(Interval::total).collect(Collectors.joining(","))
            )
        );
        writer.newLine();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    private static class Interval {

        private String startDate;
        private String endDate;

        Interval(String startDate) {
            this.startDate = startDate;
        }

        void setEndDate(String endDate) {
            this.endDate = endDate;
        }

        String total() {
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
