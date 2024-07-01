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

import fr.gouv.vitam.logbook.administration.core.api.LogbookDetailsCheckService;
import fr.gouv.vitam.logbook.common.model.coherence.EventModel;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookCheckError;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookEventType;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test the LogbookDetailsCheck services.
 */
public class LogbookDetailsCheckServiceImplTest {

    /**
     * Logbook's properties check service.
     */
    private LogbookDetailsCheckService logbookDetailsCheckService;

    @Test
    public void checkEvents() throws Exception {
        logbookDetailsCheckService = new LogbookDetailsCheckServiceImpl();
        List<LogbookCheckError> logbookCheckErrors;

        // check event "Action" conforme
        EventModel eventModel = new EventModel(
            LogbookEventType.ACTION,
            "aecaaaaabgheitkvabyhoalbapdzhniaaaaq",
            "",
            "aedqaaaabggsoscfaat22albarjmsvyaaaba",
            "aedqaaaabggsoscfaat22albarjmsvyaaaaq",
            "SANITY_CHECK_SIP",
            "STP_PREPARE_LC_TRACEABILITY",
            "OK",
            "SANITY_CHECK_SIP.OK"
        );

        logbookCheckErrors = logbookDetailsCheckService.checkEvent(eventModel);
        Assert.assertTrue(logbookCheckErrors.isEmpty());

        // check event "Action" not conforme -> evDetails not conforme
        eventModel = new EventModel(
            LogbookEventType.ACTION,
            "aecaaaaabgheitkvabyhoalbapdzhniaaaaq",
            "",
            "aedqaaaabggsoscfaat22albarjmsvyaaaba",
            "aedqaaaabggsoscfaat22albarjmsvyaaaaq",
            "SANITY_CHECK_SIP",
            "STP_PREPARE_LC_TRACEABILITY",
            "OK",
            "PREPARE_LC_TRACEABILITY.OK"
        );

        logbookCheckErrors = logbookDetailsCheckService.checkEvent(eventModel);
        Assert.assertEquals(1, logbookCheckErrors.size());
        Assert.assertEquals("aecaaaaabgheitkvabyhoalbapdzhniaaaaq", logbookCheckErrors.get(0).getOperationId());
        Assert.assertEquals("SANITY_CHECK_SIP", logbookCheckErrors.get(0).getCheckedProperty());
        Assert.assertTrue(
            "The saved event outDetail value is : PREPARE_LC_TRACEABILITY.OK".contains(
                    logbookCheckErrors.get(0).getSavedLogbookMsg()
                )
        );
        Assert.assertTrue(
            "The event outDetail value must be as : ^SANITY_CHECK_SIP(\\.(\\w+))*\\.OK$".contains(
                    logbookCheckErrors.get(0).getExpectedLogbookMsg()
                )
        );

        // check event "Step" not conforme -> outcome and evDetails not conforme
        eventModel = new EventModel(
            LogbookEventType.STEP,
            "aecaaaaabgheitkvabyhoalbapdzhniaaaaq",
            "",
            "aedqaaaabggsoscfaat22albarkwtviaaaaq",
            null,
            "STP_SANITY_CHECK_SIP",
            "",
            "WARNN",
            "PREPARE_LC_TRACEABILITY.FATAL"
        );
        logbookCheckErrors = logbookDetailsCheckService.checkEvent(eventModel);
        Assert.assertEquals(2, logbookCheckErrors.size());
        Assert.assertEquals("aecaaaaabgheitkvabyhoalbapdzhniaaaaq", logbookCheckErrors.get(0).getOperationId());
        Assert.assertEquals("STP_SANITY_CHECK_SIP", logbookCheckErrors.get(0).getCheckedProperty());
        Assert.assertTrue(
            "The saved event STP_SANITY_CHECK_SIP outcome value is : WARNN".contains(
                    logbookCheckErrors.get(0).getSavedLogbookMsg()
                )
        );
        Assert.assertTrue(
            "The event outcome value must be as : STARTED, OK, WARNING, KO, FATAL".contains(
                    logbookCheckErrors.get(0).getExpectedLogbookMsg()
                )
        );

        Assert.assertTrue(
            "The saved event outDetail value is : PREPARE_LC_TRACEABILITY.FATAL".contains(
                    logbookCheckErrors.get(1).getSavedLogbookMsg()
                )
        );
        Assert.assertTrue(
            "The event outDetail value must be as : ^STP_SANITY_CHECK_SIP(\\.(\\w+))*\\.WARNN$".contains(
                    logbookCheckErrors.get(1).getExpectedLogbookMsg()
                )
        );

        // check event "Task" not conforme : Task/treatment with an incorrect evType
        eventModel = new EventModel(
            LogbookEventType.TASK,
            "aedqaaaabggsoscfaat22albarkwtiqaaaaq",
            "aeaqaaaabeghay2jabzuaalbarkwwzyaaabq",
            "aedqaaaabgghay2jabzuaalbarkwxnyaaaba",
            "aedqaaaabgghay2jabzuaalbarkwxnyaaaaq",
            "LFC.CHECK_MANIFEST.LFC_CREATION",
            "LFC.CHECK_UNIT_SCHEMA",
            "KO",
            "LFC.CHECK_MANIFEST.LFC_CREATION.OK"
        );

        logbookCheckErrors = logbookDetailsCheckService.checkEvent(eventModel);
        Assert.assertEquals(2, logbookCheckErrors.size());
        Assert.assertEquals("aedqaaaabggsoscfaat22albarkwtiqaaaaq", logbookCheckErrors.get(0).getOperationId());
        Assert.assertEquals("aeaqaaaabeghay2jabzuaalbarkwwzyaaabq", logbookCheckErrors.get(0).getLfcId());
        Assert.assertEquals("LFC.CHECK_MANIFEST.LFC_CREATION", logbookCheckErrors.get(0).getCheckedProperty());
        Assert.assertTrue(
            "The saved event evType value is : LFC.CHECK_MANIFEST.LFC_CREATION".contains(
                    logbookCheckErrors.get(0).getSavedLogbookMsg()
                )
        );
        Assert.assertTrue(
            "The event evType value must be as : LFC.CHECK_UNIT_SCHEMA.*".contains(
                    logbookCheckErrors.get(0).getExpectedLogbookMsg()
                )
        );

        Assert.assertTrue(
            "The saved event outDetail value is : LFC.CHECK_MANIFEST.LFC_CREATION.OK".contains(
                    logbookCheckErrors.get(1).getSavedLogbookMsg()
                )
        );
        Assert.assertTrue(
            "The event outDetail value must be as : ^LFC.CHECK_MANIFEST.LFC_CREATION(\\.(\\w+))*\\.KO$".contains(
                    logbookCheckErrors.get(1).getExpectedLogbookMsg()
                )
        );
    }

    @Test
    public void checkLFCandOperation() throws Exception {
        logbookDetailsCheckService = new LogbookDetailsCheckServiceImpl();
        List<LogbookCheckError> logbookCheckErrors = new ArrayList<>();
        Map<String, EventModel> mapOpEvents = new HashMap<>();
        Map<String, EventModel> mapLfcEvents = new HashMap<>();

        // create logbook operation events map
        mapOpEvents.put(
            "SANITY_CHECK_SIP",
            new EventModel(
                LogbookEventType.ACTION,
                "aedqaaaabggsoscfaat22albarkwtiqaaaaq",
                "aeaqaaaabeghay2jabzuaalbarkwwzyaaabq",
                "aedqaaaabggsoscfaat22albarkwtviaaaba",
                "aedqaaaabggsoscfaat22albarkwtviaaaaq",
                "STP_SANITY_CHECK_SIP",
                "STP_SANITY_CHECK_SIP",
                "OK",
                "STP_SANITY_CHECK_SIP.OK"
            )
        );
        mapOpEvents.put(
            "CHECK_HEADER",
            new EventModel(
                LogbookEventType.ACTION,
                "aedqaaaabggsoscfaat22albarkwtiqaaaaq",
                "aeaqaaaabeghay2jabzuaalbarkwwzyaaabq",
                "aedqaaaabghwqkjyabmlsalbarkwzxqaaaba",
                "aedqaaaabghwqkjyabmlsalbarkwwciaaaaq",
                "CHECK_HEADER",
                "CHECK_SEDA",
                "OK",
                "CHECK_HEADER.OK"
            )
        );
        mapOpEvents.put(
            "UNIT_METADATA_INDEXATION",
            new EventModel(
                LogbookEventType.ACTION,
                "aedqaaaabggsoscfaat22albarkwtiqaaaaq",
                "aeaqaaaabeghay2jabzuaalbarkwwzyaaabq",
                "aedqaaaabghwqkjyabmlsalbarkypbiaaaaq",
                "aedqaaaabghwqkjyabmlsalbarkxx4aaaaaq",
                "UNIT_METADATA_INDEXATION",
                "STP_UNIT_METADATA",
                "OK",
                "UNIT_METADATA_INDEXATION.OK"
            )
        );
        mapOpEvents.put(
            "UNIT_METADATA_STORAGE",
            new EventModel(
                LogbookEventType.ACTION,
                "aedqaaaabggsoscfaat22albarkwtiqaaaaq",
                "aeaqaaaabeghay2jabzuaalbarkwwzyaaabq",
                "aedqaaaabghwqkjyabmlsalbark3whiaaaaq",
                "aedqaaaabghwqkjyabmlsalbarkyscqaaaaq",
                "UNIT_METADATA_STORAGE",
                "STP_UNIT_STORING",
                "FATAL",
                "UNIT_METADATA_STORAGE.FATAL"
            )
        );

        // create logbook lifecycles map
        mapLfcEvents.put(
            "CHECK_UNIT_SCHEMA",
            new EventModel(
                LogbookEventType.ACTION,
                "aedqaaaabggsoscfaat22albarkwtiqaaaaq",
                "aeaqaaaabeghay2jabzuaalbarkwwzyaaabq",
                "aedqaaaabgghay2jabzuaalbarkw3vaaaaaq",
                null,
                "CHECK_UNIT_SCHEMA",
                "",
                "OK",
                "CHECK_UNIT_SCHEMA.OK"
            )
        );
        mapLfcEvents.put(
            "CHECK_CLASSIFICATION_LEVEL",
            new EventModel(
                LogbookEventType.ACTION,
                "aedqaaaabggsoscfaat22albarkwtiqaaaaq",
                "aeaqaaaabeghay2jabzuaalbarkwwzyaaabq",
                "aedqaaaabgghay2jabzuaalbarkw35qaaaaq",
                null,
                "CHECK_CLASSIFICATION_LEVEL",
                "",
                "OK",
                "CHECK_CLASSIFICATION_LEVEL.OK"
            )
        );
        mapLfcEvents.put(
            "UNIT_METADATA_INDEXATION",
            new EventModel(
                LogbookEventType.ACTION,
                "aedqaaaabggsoscfaat22albarkwtiqaaaaq",
                "aeaqaaaabeghay2jabzuaalbarkwwzyaaabq",
                "aedqaaaabgghay2jabzuaalbarkxxjaaaaaq",
                null,
                "UNIT_METADATA_INDEXATION",
                "",
                "OK",
                "UNIT_METADATA_INDEXATION.OK"
            )
        );
        mapLfcEvents.put(
            "UNIT_METADATA_STORAGE",
            new EventModel(
                LogbookEventType.ACTION,
                "aedqaaaabggsoscfaat22albarkwtiqaaaaq",
                "",
                "aedqaaaabgghay2jabzuaalbarkyr2qaaaaq",
                null,
                "UNIT_METADATA_STORAGE",
                "",
                "OK",
                "UNIT_METADATA_STORAGE.OK"
            )
        );

        // Check coherence between logbook operation and lifecycles
        logbookCheckErrors = logbookDetailsCheckService.checkLFCandOperation(mapOpEvents, mapLfcEvents);
        Assert.assertEquals(5, logbookCheckErrors.size());

        Assert.assertEquals("aedqaaaabggsoscfaat22albarkwtiqaaaaq", logbookCheckErrors.get(0).getOperationId());
        Assert.assertEquals("aeaqaaaabeghay2jabzuaalbarkwwzyaaabq", logbookCheckErrors.get(0).getLfcId());
        Assert.assertEquals("CHECK_UNIT_SCHEMA", logbookCheckErrors.get(0).getCheckedProperty());
        Assert.assertTrue(
            "The saved LFC event evType value CHECK_UNIT_SCHEMA, is not present in logbook operation".contains(
                    logbookCheckErrors.get(0).getSavedLogbookMsg()
                )
        );
        Assert.assertTrue(
            "The logbook operation must contains the lifecycle event value evType".contains(
                    logbookCheckErrors.get(0).getExpectedLogbookMsg()
                )
        );

        Assert.assertEquals("CHECK_CLASSIFICATION_LEVEL", logbookCheckErrors.get(1).getCheckedProperty());
        Assert.assertTrue(
            "The saved LFC event evType value CHECK_CLASSIFICATION_LEVEL, is not present in logbook operation".contains(
                    logbookCheckErrors.get(1).getSavedLogbookMsg()
                )
        );
        Assert.assertTrue(
            "The logbook operation must contains the lifecycle event value evType".contains(
                    logbookCheckErrors.get(1).getExpectedLogbookMsg()
                )
        );

        Assert.assertEquals("UNIT_METADATA_STORAGE", logbookCheckErrors.get(2).getCheckedProperty());
        Assert.assertTrue(
            "The saved LFC event outcome value OK, is not conforme in logbook operation".contains(
                    logbookCheckErrors.get(2).getSavedLogbookMsg()
                )
        );
        Assert.assertTrue(
            "The logbook operation must have the same event value outcome as in the lifecycle".contains(
                    logbookCheckErrors.get(2).getExpectedLogbookMsg()
                )
        );

        Assert.assertEquals("CHECK_HEADER", logbookCheckErrors.get(3).getCheckedProperty());
        Assert.assertTrue(
            "The saved logbook operation event evType value CHECK_HEADER, is not present in the lifecycles".contains(
                    logbookCheckErrors.get(3).getSavedLogbookMsg()
                )
        );
        Assert.assertTrue(
            "The logbook operation event evType, must be present in the lifecycles".contains(
                    logbookCheckErrors.get(3).getExpectedLogbookMsg()
                )
        );
    }
}
