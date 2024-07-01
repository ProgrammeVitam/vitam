/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.i18n;

import fr.gouv.vitam.common.model.StatusCode;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class VitamLogbookMessagesTest {

    private static final String FIN = "FIN";
    private static final String STEP_OR_HANDLER = "StepOrHandler";
    private static final String TRANSACTION = "Transaction";
    private static final String LABEL = "LABEL";
    private static final String LFC = "LFC ";

    @Test
    public void testVitamMessagesTest() {
        assertTrue(VitamLogbookMessages.getLabelOp(STEP_OR_HANDLER).startsWith(LABEL));
        assertTrue(
            VitamLogbookMessages.getCodeOp(STEP_OR_HANDLER, StatusCode.STARTED).startsWith(StatusCode.STARTED.name())
        );
        Assert.assertEquals(
            VitamLogbookMessages.getCodeOp(STEP_OR_HANDLER, StatusCode.OK),
            "!" + STEP_OR_HANDLER + "." + StatusCode.OK + "!"
        );
        assertTrue(
            VitamLogbookMessages.getCodeOp(STEP_OR_HANDLER, StatusCode.WARNING).startsWith(StatusCode.WARNING.name())
        );
        assertTrue(
            VitamLogbookMessages.getCodeOp(STEP_OR_HANDLER, StatusCode.KO, FIN).startsWith(StatusCode.KO.name())
        );
        assertTrue(VitamLogbookMessages.getCodeOp(STEP_OR_HANDLER, StatusCode.KO, FIN).endsWith(FIN));
        assertTrue(
            VitamLogbookMessages.getCodeOp(STEP_OR_HANDLER, StatusCode.FATAL).startsWith(StatusCode.FATAL.name())
        );

        assertTrue(VitamLogbookMessages.getLabelLfc(STEP_OR_HANDLER).startsWith(LFC));
        assertTrue(
            VitamLogbookMessages.getCodeLfc(STEP_OR_HANDLER, StatusCode.STARTED).startsWith(
                LFC + StatusCode.STARTED.name()
            )
        );
        assertTrue(
            VitamLogbookMessages.getCodeLfc(STEP_OR_HANDLER, StatusCode.OK).startsWith(LFC + StatusCode.OK.name())
        );
        assertTrue(
            VitamLogbookMessages.getCodeLfc(STEP_OR_HANDLER, StatusCode.WARNING).startsWith(
                LFC + StatusCode.WARNING.name()
            )
        );
        assertTrue(
            VitamLogbookMessages.getCodeLfc(STEP_OR_HANDLER, StatusCode.KO, FIN).startsWith(LFC + StatusCode.KO.name())
        );
        assertTrue(VitamLogbookMessages.getCodeLfc(STEP_OR_HANDLER, StatusCode.KO, FIN).endsWith(FIN));
        assertTrue(
            VitamLogbookMessages.getCodeLfc(STEP_OR_HANDLER, StatusCode.FATAL).startsWith(LFC + StatusCode.FATAL.name())
        );

        assertTrue(VitamLogbookMessages.getLabelOp(STEP_OR_HANDLER, TRANSACTION).startsWith(LABEL + " " + TRANSACTION));
        assertTrue(
            VitamLogbookMessages.getCodeOp(STEP_OR_HANDLER, TRANSACTION, StatusCode.STARTED).startsWith(
                StatusCode.STARTED.name() + " " + TRANSACTION
            )
        );
        assertTrue(
            VitamLogbookMessages.getCodeOp(STEP_OR_HANDLER, TRANSACTION, StatusCode.OK).startsWith(
                StatusCode.OK.name() + " " + TRANSACTION
            )
        );
        assertTrue(
            VitamLogbookMessages.getCodeOp(STEP_OR_HANDLER, TRANSACTION, StatusCode.WARNING).startsWith(
                StatusCode.WARNING.name() + " " + TRANSACTION
            )
        );
        assertTrue(
            VitamLogbookMessages.getCodeOp(STEP_OR_HANDLER, TRANSACTION, StatusCode.KO, FIN).startsWith(
                StatusCode.KO.name() + " " + TRANSACTION
            )
        );
        assertTrue(VitamLogbookMessages.getCodeOp(STEP_OR_HANDLER, TRANSACTION, StatusCode.KO, FIN).endsWith(FIN));
        assertTrue(
            VitamLogbookMessages.getCodeOp(STEP_OR_HANDLER, TRANSACTION, StatusCode.FATAL).startsWith(
                StatusCode.FATAL.name() + " " + TRANSACTION
            )
        );

        assertTrue(
            VitamLogbookMessages.getLabelLfc(STEP_OR_HANDLER, TRANSACTION).startsWith(LFC + LABEL + " " + TRANSACTION)
        );
        assertTrue(
            VitamLogbookMessages.getCodeLfc(STEP_OR_HANDLER, TRANSACTION, StatusCode.STARTED).startsWith(
                LFC + StatusCode.STARTED.name() + " " + TRANSACTION
            )
        );
        assertTrue(
            VitamLogbookMessages.getCodeLfc(STEP_OR_HANDLER, TRANSACTION, StatusCode.OK).startsWith(
                LFC + StatusCode.OK.name() + " " + TRANSACTION
            )
        );
        assertTrue(
            VitamLogbookMessages.getCodeLfc(STEP_OR_HANDLER, TRANSACTION, StatusCode.WARNING).startsWith(
                LFC + StatusCode.WARNING.name() + " " + TRANSACTION
            )
        );
        assertTrue(
            VitamLogbookMessages.getCodeLfc(STEP_OR_HANDLER, TRANSACTION, StatusCode.KO, FIN).startsWith(
                LFC + StatusCode.KO.name() + " " + TRANSACTION
            )
        );
        assertTrue(VitamLogbookMessages.getCodeLfc(STEP_OR_HANDLER, TRANSACTION, StatusCode.KO, FIN).endsWith(FIN));
        assertTrue(
            VitamLogbookMessages.getCodeLfc(STEP_OR_HANDLER, TRANSACTION, StatusCode.FATAL).startsWith(
                LFC + StatusCode.FATAL.name() + " " + TRANSACTION
            )
        );

        assertNotNull(VitamLogbookMessages.getAllMessages());
    }

    @Test
    public void validPropertyFile() throws IOException, URISyntaxException {
        // For the moment valid only that all ' are doubles
        URL url = VitamLogbookMessages.class.getResource("/vitam-logbook-messages_fr.properties");
        assertNotNull(url);
        File file = new File(url.toURI());
        file = new File(file.getParentFile().getParentFile(), "classes/vitam-logbook-messages_fr.properties");
        InputStream input = new FileInputStream(file);
        boolean match = true;
        try (final InputStreamReader reader = new InputStreamReader(input)) {
            try (final BufferedReader buffered = new BufferedReader(reader)) {
                String line;
                while ((line = buffered.readLine()) != null) {
                    if (line.matches("#.*")) {
                        // skip comments
                        continue;
                    }

                    if (line.contains("'")) {
                        //this regex just check if one quote found, the next should be a quote
                        boolean submatch = line.matches("^([A-Z_.]*=([^']|(''))*)$");
                        if (!submatch) {
                            System.err.println("WRONG PROPERTY: " + line);
                        }
                        match &= submatch;
                    }
                }
            }
        }
        assertTrue(match);
    }
}
