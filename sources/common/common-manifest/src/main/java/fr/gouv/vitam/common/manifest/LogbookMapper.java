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
package fr.gouv.vitam.common.manifest;

import fr.gouv.culture.archivesdefrance.seda.v2.EventLogBookOgType;
import fr.gouv.culture.archivesdefrance.seda.v2.EventType;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.mapping.dip.TransformJsonTreeToListOfXmlElement;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import org.bson.Document;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogbookMapper {

    public static final String AGENT_IDENTIFIER = "AgentIdentifier";
    public static final String OBJECT_IDENTIFIER = "ObjectIdentifier";
    private static final String LOGBOOK_EVENT_OBJECT_IDENTIFIER = "obId";
    private static final String LOGBOOK_EVENT_AGENT_IDENTIFIER = "agId";

    public static EventType getEventTypeFromDocument(Document eventData) {
        EventType event = new EventType();
        setDataInEvent(eventData, event);
        return event;
    }

    public static EventLogBookOgType getEventOGTypeFromDocument(Document eventData) {
        EventLogBookOgType event = new EventLogBookOgType();
        setDataInEvent(eventData, event);
        return event;
    }

    private static <T extends EventType> void setDataInEvent(Document eventData, T event) {
        event.setEventIdentifier(eventData.getString(LogbookEvent.EV_ID));
        event.setEventTypeCode(eventData.getString(LogbookEvent.EV_TYPE_PROC));
        event.setEventType(eventData.getString(LogbookEvent.EV_TYPE));
        event.setEventDateTime(reformatEventDateTime(eventData.getString(LogbookEvent.EV_DATE_TIME)));
        event.setOutcome(eventData.getString(LogbookEvent.OUTCOME));
        event.setOutcomeDetail(eventData.getString(LogbookEvent.OUT_DETAIL));
        event.setOutcomeDetailMessage(eventData.getString(LogbookEvent.OUT_MESSG));
        event.setEventDetailData(eventData.getString(LogbookEvent.EV_DET_DATA));

        Map<String, List<String>> extensions = new HashMap<>();
        extensions.put(
            AGENT_IDENTIFIER,
            Collections.singletonList(eventData.getString(LOGBOOK_EVENT_AGENT_IDENTIFIER))
        );
        extensions.put(
            OBJECT_IDENTIFIER,
            Collections.singletonList(eventData.getString(LOGBOOK_EVENT_OBJECT_IDENTIFIER))
        );
        event.getAny().addAll(TransformJsonTreeToListOfXmlElement.mapJsonToElement(extensions));
    }

    private static String reformatEventDateTime(String persistedEvDateTime) {
        // Due to the bug #13013, there might be some Lfc events with evDateTime formatted without milliseconds
        // (2000-01-01T12:23:45) or seconds (2000-01-01T12:23).
        // This was caused by the default truncate behavior in the java method LocalDateTime.toString().
        // We can't patch LFCs (they are secured), but we must properly format the dateTime to be SEDA-conform.
        return LocalDateUtil.getFormattedDateTimeForMongo(LocalDateUtil.parseMongoFormattedDate(persistedEvDateTime));
    }
}
