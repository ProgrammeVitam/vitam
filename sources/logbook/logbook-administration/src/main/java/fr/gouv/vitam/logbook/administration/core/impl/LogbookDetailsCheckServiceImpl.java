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
package fr.gouv.vitam.logbook.administration.core.impl;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.administration.core.api.LogbookDetailsCheckService;
import fr.gouv.vitam.logbook.common.model.coherence.EventModel;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookCheckError;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookEventName;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookEventType;
import fr.gouv.vitam.logbook.common.model.coherence.OutcomeStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Logbook details check service.<br>
 */
public class LogbookDetailsCheckServiceImpl implements LogbookDetailsCheckService {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookDetailsCheckServiceImpl.class);

    /**
     * SAVED_LOGBOOK_MSG
     */
    private final String SAVED_LOGBOOK_MSG = "The saved event %s value is : %s";
    private final String SAVED_LOGBOOK_OP_LFC_NOT_EXISTS_MSG =
        "The saved LFC event %s value %s, is not present in logbook operation";
    private final String SAVED_LOGBOOK_OP_LFC_NOT_CONFORME_MSG =
        "The saved LFC event %s value %s, is not conforme in logbook operation";
    private final String SAVED_LOGBOOK_OPERATION_EVENTS_NOT_EXIST_IN_LFC_MSG =
        "The saved logbook operation event %s value %s, is not present in the lifecycles";

    /**
     * EXPECTED_LOGBOOK_MSG
     */
    private final String EXPECTED_LOGBOOK_MSG = "The event %s value must be as : %s";
    private final String EXPECTED_LOGBOOK_OP_LFC_NOT_EXISTS_MSG =
        "The logbook operation must contains the lifecycle event value %s";
    private final String EXPECTED_LOGBOOK_OP_LFC_NOT_CONFORME_MSG =
        "The logbook operation must have the same event value %s as in the lifecycle";
    private final String EXPECTED_LOGBOOK_OPERATION_EVENTS_NOT_EXIST_IN_LFC_MSG =
        "The logbook operation event %s, must be present in the lifecycles";

    private final String DOT = ".";

    /**
     * Logbook events coherence check.
     *
     * @param event
     * @return
     */
    @Override
    public List<LogbookCheckError> checkEvent(EventModel event) {
        List<LogbookCheckError> logbookCheckErrors = new ArrayList<>();

        // check event evType coherence.
        if (LogbookEventType.TASK.equals(event.getLogbookEventType()) && event.getEvTypeParent() != null) {
            // the evType must have the following format : <evTypeParent.*>
            String evTypeFormat = event.getEvTypeParent() + DOT;
            if (!event.getEvType().startsWith(evTypeFormat)) {
                // construct LogbookCheckResult
                logbookCheckErrors.add(
                    new LogbookCheckError(
                        event.getOperationId(),
                        event.getLfcId(),
                        event.getEvType(),
                        String.format(SAVED_LOGBOOK_MSG, LogbookEventName.EVTYPE.getValue(), event.getEvType()),
                        String.format(EXPECTED_LOGBOOK_MSG, LogbookEventName.EVTYPE.getValue(), evTypeFormat + "*")
                    )
                );
            }
        }

        // check outcome coherence.
        boolean isOutcomeOk = Stream.of(OutcomeStatus.values())
            .map(String::valueOf)
            .anyMatch(s -> s.contains(event.getOutcome()));

        if (!isOutcomeOk) {
            logbookCheckErrors.add(
                new LogbookCheckError(
                    event.getOperationId(),
                    event.getLfcId(),
                    event.getEvType(),
                    String.format(
                        SAVED_LOGBOOK_MSG,
                        event.getEvType() + " " + LogbookEventName.OUTCOME.getValue(),
                        event.getOutcome()
                    ),
                    String.format(
                        EXPECTED_LOGBOOK_MSG,
                        LogbookEventName.OUTCOME.getValue(),
                        Stream.of(OutcomeStatus.values()).map(String::valueOf).collect(Collectors.joining(", "))
                    )
                )
            );
        }

        // Check event outcomeDetails coherence.
        // regex -> outcomeDetails must have the following format : <evType.*.outcome>
        String regex = "^" + event.getEvType() + "(\\.(\\w+))*\\." + event.getOutcome() + "$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(event.getOutDetail());

        if (!matcher.find()) {
            logbookCheckErrors.add(
                new LogbookCheckError(
                    event.getOperationId(),
                    event.getLfcId(),
                    event.getEvType(),
                    String.format(SAVED_LOGBOOK_MSG, LogbookEventName.OUTCOMEDETAILS.getValue(), event.getOutDetail()),
                    String.format(EXPECTED_LOGBOOK_MSG, LogbookEventName.OUTCOMEDETAILS.getValue(), regex)
                )
            );
        }

        return logbookCheckErrors;
    }

    /**
     * Check coherence between logbook operation and lifecycles.
     *
     * @param mapOpEvents
     * @param mapLfcEvents
     * @return
     */
    @Override
    public List<LogbookCheckError> checkLFCandOperation(
        Map<String, EventModel> mapOpEvents,
        Map<String, EventModel> mapLfcEvents
    ) {
        List<LogbookCheckError> logbookCheckErrors = new ArrayList<>();
        LOGGER.debug("Check coherence between logbook operation and Lifecycles");

        EventModel eventLfc;
        EventModel eventOp;
        // check if all lifecylces events exist on the logbook operation and vice versa.
        Set<String> treatedEvents = new HashSet<>();
        for (String evType : mapLfcEvents.keySet()) {
            eventLfc = mapLfcEvents.get(evType);
            eventOp = mapOpEvents.get(evType);
            if (eventOp == null) {
                // case when the lifecycle event is not present in the operation logbook
                logbookCheckErrors.add(
                    new LogbookCheckError(
                        eventLfc.getOperationId(),
                        eventLfc.getLfcId(),
                        eventLfc.getEvType(),
                        String.format(
                            SAVED_LOGBOOK_OP_LFC_NOT_EXISTS_MSG,
                            LogbookEventName.EVTYPE.getValue(),
                            eventLfc.getEvType()
                        ),
                        String.format(
                            EXPECTED_LOGBOOK_OP_LFC_NOT_EXISTS_MSG,
                            LogbookEventName.EVTYPE.getValue(),
                            eventLfc.getEvType()
                        )
                    )
                );
            } else if (!eventLfc.getOutcome().equals(eventOp.getOutcome())) {
                // case when the logbook operation event is not conform to the lifecycle event
                logbookCheckErrors.add(
                    new LogbookCheckError(
                        eventLfc.getOperationId(),
                        eventLfc.getLfcId(),
                        eventLfc.getEvType(),
                        String.format(
                            SAVED_LOGBOOK_OP_LFC_NOT_CONFORME_MSG,
                            LogbookEventName.OUTCOME.getValue(),
                            eventLfc.getOutcome()
                        ),
                        String.format(
                            EXPECTED_LOGBOOK_OP_LFC_NOT_CONFORME_MSG,
                            LogbookEventName.OUTCOME.getValue(),
                            eventLfc.getOutcome()
                        )
                    )
                );
            }
            // collect treated events
            treatedEvents.add(evType);
        }
        // delete the checked logbook operation events
        mapOpEvents.keySet().removeAll(treatedEvents);

        // treat non-conform logbook operation events
        if (!mapOpEvents.isEmpty()) {
            // case when logbook operation contains events without corespondances on Lifecycles.
            mapOpEvents
                .values()
                .forEach(
                    event ->
                        logbookCheckErrors.add(
                            new LogbookCheckError(
                                event.getOperationId(),
                                event.getLfcId(),
                                event.getEvType(),
                                String.format(
                                    SAVED_LOGBOOK_OPERATION_EVENTS_NOT_EXIST_IN_LFC_MSG,
                                    LogbookEventName.EVTYPE.getValue(),
                                    event.getEvType()
                                ),
                                String.format(
                                    EXPECTED_LOGBOOK_OPERATION_EVENTS_NOT_EXIST_IN_LFC_MSG,
                                    LogbookEventName.EVTYPE.getValue(),
                                    event.getEvType()
                                )
                            )
                        )
                );
        }

        return logbookCheckErrors;
    }
}
