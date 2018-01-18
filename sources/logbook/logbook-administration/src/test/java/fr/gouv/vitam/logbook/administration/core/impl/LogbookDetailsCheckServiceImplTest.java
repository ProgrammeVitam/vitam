package fr.gouv.vitam.logbook.administration.core.impl;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.administration.core.api.LogbookDetailsCheckService;
import fr.gouv.vitam.logbook.common.model.EventModel;
import fr.gouv.vitam.logbook.common.model.LogbookCheckResult;
import fr.gouv.vitam.logbook.common.model.LogbookEventType;
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
     * Vitam logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookDetailsCheckServiceImplTest.class);

    /**
     * Logbook's properties check service.
     */
    private LogbookDetailsCheckService logbookDetailsCheckService;

    @Test
    public void checkEvents() throws Exception {

        logbookDetailsCheckService = new LogbookDetailsCheckServiceImpl();
        List<LogbookCheckResult> logbookCheckResults;

        // check event "Action" conforme
        EventModel eventModel = new EventModel(LogbookEventType.ACTION, "aecaaaaabgheitkvabyhoalbapdzhniaaaaq",
            "", "aedqaaaabggsoscfaat22albarjmsvyaaaba", "aedqaaaabggsoscfaat22albarjmsvyaaaaq",
            "SANITY_CHECK_SIP", "STP_PREPARE_LC_TRACEABILITY",
            "OK", "SANITY_CHECK_SIP.OK");

        logbookCheckResults = logbookDetailsCheckService.checkEvent(eventModel);
        Assert.assertTrue(logbookCheckResults.isEmpty());

        // check event "Action" not conforme -> evDetails not conforme
        eventModel = new EventModel(LogbookEventType.ACTION, "aecaaaaabgheitkvabyhoalbapdzhniaaaaq",
            "", "aedqaaaabggsoscfaat22albarjmsvyaaaba", "aedqaaaabggsoscfaat22albarjmsvyaaaaq",
            "SANITY_CHECK_SIP", "STP_PREPARE_LC_TRACEABILITY",
            "OK", "PREPARE_LC_TRACEABILITY.OK");

        logbookCheckResults = logbookDetailsCheckService.checkEvent(eventModel);
        Assert.assertEquals(1, logbookCheckResults.size());
        Assert.assertEquals("aecaaaaabgheitkvabyhoalbapdzhniaaaaq", logbookCheckResults.get(0).getOperationId());
        Assert.assertEquals("SANITY_CHECK_SIP", logbookCheckResults.get(0).getCheckedProperty());
        Assert.assertTrue("The saved event outDetail value is : PREPARE_LC_TRACEABILITY.OK"
            .contains(logbookCheckResults.get(0).getSavedLogbookMsg()));
        Assert.assertTrue("The event outDetail value must be as : ^SANITY_CHECK_SIP(\\.(\\w+))*\\.OK$"
            .contains(logbookCheckResults.get(0).getExpectedLogbookMsg()));


        // check event "Step" not conforme -> outcome and evDetails not conforme
        eventModel = new EventModel(LogbookEventType.STEP, "aecaaaaabgheitkvabyhoalbapdzhniaaaaq",
            "", "aedqaaaabggsoscfaat22albarkwtviaaaaq", null,
            "STP_SANITY_CHECK_SIP", "",
            "WARNN", "PREPARE_LC_TRACEABILITY.FATAL");
        logbookCheckResults = logbookDetailsCheckService.checkEvent(eventModel);
        Assert.assertEquals(2, logbookCheckResults.size());
        Assert.assertEquals("aecaaaaabgheitkvabyhoalbapdzhniaaaaq", logbookCheckResults.get(0).getOperationId());
        Assert.assertEquals("STP_SANITY_CHECK_SIP", logbookCheckResults.get(0).getCheckedProperty());
        Assert.assertTrue("The saved event STP_SANITY_CHECK_SIP outcome value is : WARNN"
            .contains(logbookCheckResults.get(0).getSavedLogbookMsg()));
        Assert.assertTrue("The event outcome value must be as : STARTED, OK, WARNING, KO, FATAL"
            .contains(logbookCheckResults.get(0).getExpectedLogbookMsg()));

        Assert.assertTrue("The saved event outDetail value is : PREPARE_LC_TRACEABILITY.FATAL"
            .contains(logbookCheckResults.get(1).getSavedLogbookMsg()));
        Assert.assertTrue("The event outDetail value must be as : ^STP_SANITY_CHECK_SIP(\\.(\\w+))*\\.WARNN$"
            .contains(logbookCheckResults.get(1).getExpectedLogbookMsg()));


        // check event "Task" not conforme : Task/treatment with an incorrect evType
        eventModel = new EventModel(LogbookEventType.TASK, "aedqaaaabggsoscfaat22albarkwtiqaaaaq",
            "aeaqaaaabeghay2jabzuaalbarkwwzyaaabq", "aedqaaaabgghay2jabzuaalbarkwxnyaaaba",
            "aedqaaaabgghay2jabzuaalbarkwxnyaaaaq",
            "LFC.CHECK_MANIFEST.LFC_CREATION", "LFC.CHECK_UNIT_SCHEMA", "KO", "LFC.CHECK_MANIFEST.LFC_CREATION.OK");

        logbookCheckResults = logbookDetailsCheckService.checkEvent(eventModel);
        Assert.assertEquals(2, logbookCheckResults.size());
        Assert.assertEquals("aedqaaaabggsoscfaat22albarkwtiqaaaaq", logbookCheckResults.get(0).getOperationId());
        Assert.assertEquals("aeaqaaaabeghay2jabzuaalbarkwwzyaaabq", logbookCheckResults.get(0).getLfcId());
        Assert.assertEquals("LFC.CHECK_MANIFEST.LFC_CREATION", logbookCheckResults.get(0).getCheckedProperty());
        Assert.assertTrue("The saved event evType value is : LFC.CHECK_MANIFEST.LFC_CREATION"
            .contains(logbookCheckResults.get(0).getSavedLogbookMsg()));
        Assert.assertTrue("The event evType value must be as : LFC.CHECK_UNIT_SCHEMA.*"
            .contains(logbookCheckResults.get(0).getExpectedLogbookMsg()));

        Assert.assertTrue("The saved event outDetail value is : LFC.CHECK_MANIFEST.LFC_CREATION.OK"
            .contains(logbookCheckResults.get(1).getSavedLogbookMsg()));
        Assert.assertTrue(
            "The event outDetail value must be as : ^LFC.CHECK_MANIFEST.LFC_CREATION(\\.(\\w+))*\\.KO$"
                .contains(logbookCheckResults.get(1).getExpectedLogbookMsg()));
    }

    @Test
    public void checkLFCandOperation() throws Exception {
        logbookDetailsCheckService = new LogbookDetailsCheckServiceImpl();
        List<LogbookCheckResult> logbookCheckResults = new ArrayList<>();
        Map<String, EventModel> mapOpEvents = new HashMap<>();
        Map<String, EventModel> mapLfcEvents = new HashMap<>();

        // create logbook operation events map
        mapOpEvents.put("SANITY_CHECK_SIP",
            new EventModel(LogbookEventType.ACTION, "aedqaaaabggsoscfaat22albarkwtiqaaaaq",
                "aeaqaaaabeghay2jabzuaalbarkwwzyaaabq",
                "aedqaaaabggsoscfaat22albarkwtviaaaba", "aedqaaaabggsoscfaat22albarkwtviaaaaq",
                "STP_SANITY_CHECK_SIP", "STP_SANITY_CHECK_SIP",
                "OK", "STP_SANITY_CHECK_SIP.OK")
        );
        mapOpEvents.put("CHECK_HEADER",
            new EventModel(LogbookEventType.ACTION, "aedqaaaabggsoscfaat22albarkwtiqaaaaq",
                "aeaqaaaabeghay2jabzuaalbarkwwzyaaabq",
                "aedqaaaabghwqkjyabmlsalbarkwzxqaaaba", "aedqaaaabghwqkjyabmlsalbarkwwciaaaaq",
                "CHECK_HEADER", "CHECK_SEDA",
                "OK", "CHECK_HEADER.OK")
        );
        mapOpEvents.put("UNIT_METADATA_INDEXATION",
            new EventModel(LogbookEventType.ACTION, "aedqaaaabggsoscfaat22albarkwtiqaaaaq",
                "aeaqaaaabeghay2jabzuaalbarkwwzyaaabq",
                "aedqaaaabghwqkjyabmlsalbarkypbiaaaaq", "aedqaaaabghwqkjyabmlsalbarkxx4aaaaaq",
                "UNIT_METADATA_INDEXATION", "STP_UNIT_METADATA",
                "OK", "UNIT_METADATA_INDEXATION.OK")
        );
        mapOpEvents.put("UNIT_METADATA_STORAGE",
            new EventModel(LogbookEventType.ACTION, "aedqaaaabggsoscfaat22albarkwtiqaaaaq",
                "aeaqaaaabeghay2jabzuaalbarkwwzyaaabq",
                "aedqaaaabghwqkjyabmlsalbark3whiaaaaq", "aedqaaaabghwqkjyabmlsalbarkyscqaaaaq",
                "UNIT_METADATA_STORAGE", "STP_UNIT_STORING",
                "FATAL", "UNIT_METADATA_STORAGE.FATAL")
        );

        // create logbook lifecycles map
        mapLfcEvents.put("CHECK_UNIT_SCHEMA",
            new EventModel(LogbookEventType.ACTION, "aedqaaaabggsoscfaat22albarkwtiqaaaaq",
                "aeaqaaaabeghay2jabzuaalbarkwwzyaaabq", "aedqaaaabgghay2jabzuaalbarkw3vaaaaaq", null,
                "CHECK_UNIT_SCHEMA", "",
                "OK", "CHECK_UNIT_SCHEMA.OK")
        );
        mapLfcEvents.put("CHECK_CLASSIFICATION_LEVEL",
            new EventModel(LogbookEventType.ACTION, "aedqaaaabggsoscfaat22albarkwtiqaaaaq",
                "aeaqaaaabeghay2jabzuaalbarkwwzyaaabq", "aedqaaaabgghay2jabzuaalbarkw35qaaaaq", null,
                "CHECK_CLASSIFICATION_LEVEL", "",
                "OK", "CHECK_CLASSIFICATION_LEVEL.OK")
        );
        mapLfcEvents.put("UNIT_METADATA_INDEXATION",
            new EventModel(LogbookEventType.ACTION, "aedqaaaabggsoscfaat22albarkwtiqaaaaq",
                "aeaqaaaabeghay2jabzuaalbarkwwzyaaabq", "aedqaaaabgghay2jabzuaalbarkxxjaaaaaq", null,
                "UNIT_METADATA_INDEXATION", "",
                "OK", "UNIT_METADATA_INDEXATION.OK")
        );
        mapLfcEvents.put("UNIT_METADATA_STORAGE",
            new EventModel(LogbookEventType.ACTION, "aedqaaaabggsoscfaat22albarkwtiqaaaaq",
                "", "aedqaaaabgghay2jabzuaalbarkyr2qaaaaq", null,
                "UNIT_METADATA_STORAGE", "",
                "OK", "UNIT_METADATA_STORAGE.OK")
        );

        // Check coherence between logbook operation and lifecycles
        logbookCheckResults = logbookDetailsCheckService.checkLFCandOperation(mapOpEvents, mapLfcEvents);
        Assert.assertEquals(4, logbookCheckResults.size());

        // SANITY_CHECK_SIP is a System event -> it is skiped when check coherence between operation and lifecyles
        Assert.assertTrue(!logbookCheckResults.contains("SANITY_CHECK_SIP"));

        Assert.assertEquals("aedqaaaabggsoscfaat22albarkwtiqaaaaq", logbookCheckResults.get(0).getOperationId());
        Assert.assertEquals("aeaqaaaabeghay2jabzuaalbarkwwzyaaabq", logbookCheckResults.get(0).getLfcId());
        Assert.assertEquals("CHECK_UNIT_SCHEMA", logbookCheckResults.get(0).getCheckedProperty());
        Assert.assertTrue("The saved LFC event evType value CHECK_UNIT_SCHEMA, is not present in logbook operation"
            .contains(logbookCheckResults.get(0).getSavedLogbookMsg()));
        Assert.assertTrue("The logbook operation must contains the lifecycle event value evType"
            .contains(logbookCheckResults.get(0).getExpectedLogbookMsg()));

        Assert.assertEquals("CHECK_CLASSIFICATION_LEVEL", logbookCheckResults.get(1).getCheckedProperty());
        Assert.assertTrue(
            "The saved LFC event evType value CHECK_CLASSIFICATION_LEVEL, is not present in logbook operation"
                .contains(logbookCheckResults.get(1).getSavedLogbookMsg()));
        Assert.assertTrue(
            "The logbook operation must contains the lifecycle event value evType"
                .contains(logbookCheckResults.get(1).getExpectedLogbookMsg()));

        Assert.assertEquals("UNIT_METADATA_STORAGE", logbookCheckResults.get(2).getCheckedProperty());
        Assert.assertTrue("The saved LFC event outcome value OK, is not conforme in logbook operation"
            .contains(logbookCheckResults.get(2).getSavedLogbookMsg()));
        Assert.assertTrue(
            "The logbook operation must have the same event value outcome as in the lifecycle"
                .contains(logbookCheckResults.get(2).getExpectedLogbookMsg()));

        Assert.assertEquals("CHECK_HEADER", logbookCheckResults.get(3).getCheckedProperty());
        Assert.assertTrue("The saved logbook operation event evType value CHECK_HEADER, is not present in the lifecycles"
            .contains(logbookCheckResults.get(3).getSavedLogbookMsg()));
        Assert.assertTrue(
            "The logbook operation event evType, must be present in the lifecycles"
                .contains(logbookCheckResults.get(3).getExpectedLogbookMsg()));

    }
}
