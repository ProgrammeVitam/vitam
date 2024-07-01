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

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MessagesTest {

    @Test
    public void testWithoutLocale() {
        final Messages messages = new Messages("messages");
        assertEquals(Locale.FRENCH, messages.getLocale());
        final String mesg1 = messages.getString("HelloWorld.START");
        assertEquals("Bonjour Vitam", mesg1);
        final String mesg2 = messages.getString("HelloWorld.KO", "value1", "value2");
        assertEquals("KO value1 on value2 !", mesg2);
        final String mesg3 = messages.getString("HelloWorld.OK", "éèàùôî");
        assertEquals("OK avec des accentués et d'apostrophe éèàùôî !", mesg3);
        final String mesg4 = messages.getString("HelloWorld.FATAL");
        assertEquals("!HelloWorld.FATAL!", mesg4);
        final String mesg5 = messages.getString("HelloWorld.FATAL", "value1", "value2");
        assertEquals("!HelloWorld.FATAL! value1 value2", mesg5);
        assertNotNull(messages.getAllMessages());
    }

    @Test
    public void testWithEmptyLocale() {
        final Messages messages = new Messages("messages", null);
        assertEquals(Locale.FRENCH, messages.getLocale());
        final String mesg1 = messages.getString("HelloWorld.START");
        assertEquals("Bonjour Vitam", mesg1);
        final String mesg2 = messages.getString("HelloWorld.KO", "value1", "value2");
        assertEquals("KO value1 on value2 !", mesg2);
        final String mesg3 = messages.getString("HelloWorld.OK", "éèàùôî");
        assertEquals("OK avec des accentués et d'apostrophe éèàùôî !", mesg3);
        assertNotNull(messages.getAllMessages());
    }

    @Test
    public void testWithLocale() {
        final Messages messages = new Messages("messages", Locale.FRENCH);
        assertEquals(Locale.FRENCH, messages.getLocale());
        final String mesg1 = messages.getString("HelloWorld.START");
        assertEquals("Bonjour Vitam", mesg1);
        final String mesg2 = messages.getString("HelloWorld.KO", "value1", "value2");
        assertEquals("KO value1 on value2 !", mesg2);
        final String mesg3 = messages.getString("HelloWorld.OK", "éèàùôî");
        assertEquals("OK avec des accentués et d'apostrophe éèàùôî !", mesg3);
        assertNotNull(messages.getAllMessages());
    }
}
