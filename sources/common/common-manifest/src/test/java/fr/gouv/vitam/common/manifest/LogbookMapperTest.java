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

import fr.gouv.culture.archivesdefrance.seda.v2.EventType;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import org.bson.Document;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class LogbookMapperTest {

    @Test
    public void testLogbookLfcMapping() {
        Document event = new Document(
            Map.of(
                LogbookEvent.EV_ID,
                "aedqaaaaacecf726aa6gmamquaefteyaaaaq",
                LogbookEvent.EV_TYPE_PROC,
                "INGEST",
                LogbookEvent.EV_TYPE,
                "LFC.LFC_CREATION",
                LogbookEvent.EV_DATE_TIME,
                "2000-05-01T11:22:33.444",
                LogbookEvent.OUTCOME,
                "OK",
                LogbookEvent.OUT_DETAIL,
                "LFC.LFC_CREATION.OK",
                LogbookEvent.OUT_MESSG,
                "Success",
                LogbookEvent.EV_DET_DATA,
                "{}"
            )
        );

        EventType eventTypeFromDocument = LogbookMapper.getEventTypeFromDocument(event);
        assertThat(eventTypeFromDocument.getEventIdentifier()).isEqualTo("aedqaaaaacecf726aa6gmamquaefteyaaaaq");
        assertThat(eventTypeFromDocument.getEventTypeCode()).isEqualTo("INGEST");
        assertThat(eventTypeFromDocument.getEventType()).isEqualTo("LFC.LFC_CREATION");
        assertThat(eventTypeFromDocument.getEventDateTime()).isEqualTo("2000-05-01T11:22:33.444");
        assertThat(eventTypeFromDocument.getOutcome()).isEqualTo("OK");
        assertThat(eventTypeFromDocument.getOutcomeDetail()).isEqualTo("LFC.LFC_CREATION.OK");
        assertThat(eventTypeFromDocument.getOutcomeDetailMessage()).isEqualTo("Success");
        assertThat(eventTypeFromDocument.getEventDetailData()).isEqualTo("{}");
    }

    @Test
    public void testLogbookLfcMappingFixTruncatedDateTime() {
        Document event = new Document(
            Map.of(
                LogbookEvent.EV_ID,
                "aedqaaaaacecf726aa6gmamquaefteyaaaaq",
                LogbookEvent.EV_TYPE_PROC,
                "INGEST",
                LogbookEvent.EV_TYPE,
                "LFC.LFC_CREATION",
                LogbookEvent.EV_DATE_TIME,
                "2000-05-01T11:22",
                LogbookEvent.OUTCOME,
                "OK",
                LogbookEvent.OUT_DETAIL,
                "LFC.LFC_CREATION.OK",
                LogbookEvent.OUT_MESSG,
                "Success",
                LogbookEvent.EV_DET_DATA,
                "{}"
            )
        );

        EventType eventTypeFromDocument = LogbookMapper.getEventTypeFromDocument(event);
        assertThat(eventTypeFromDocument.getEventIdentifier()).isEqualTo("aedqaaaaacecf726aa6gmamquaefteyaaaaq");
        assertThat(eventTypeFromDocument.getEventTypeCode()).isEqualTo("INGEST");
        assertThat(eventTypeFromDocument.getEventType()).isEqualTo("LFC.LFC_CREATION");
        assertThat(eventTypeFromDocument.getEventDateTime()).isEqualTo("2000-05-01T11:22:00.000");
        assertThat(eventTypeFromDocument.getOutcome()).isEqualTo("OK");
        assertThat(eventTypeFromDocument.getOutcomeDetail()).isEqualTo("LFC.LFC_CREATION.OK");
        assertThat(eventTypeFromDocument.getOutcomeDetailMessage()).isEqualTo("Success");
        assertThat(eventTypeFromDocument.getEventDetailData()).isEqualTo("{}");
    }
}
